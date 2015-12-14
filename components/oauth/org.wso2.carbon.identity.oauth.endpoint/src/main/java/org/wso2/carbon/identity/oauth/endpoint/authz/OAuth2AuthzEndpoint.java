/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.authz;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.CommonAuthenticationHandler;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.authentication.framework.model.CommonAuthRequestWrapper;
import org.wso2.carbon.identity.application.authentication.framework.model.CommonAuthResponseWrapper;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheEntry;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheKey;
import org.wso2.carbon.identity.oauth.cache.SessionDataCache;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheEntry;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.endpoint.OAuthRequestWrapper;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.oauth.endpoint.util.OpenIDConnectUserRPStore;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.CarbonOAuthAuthzRequest;
import org.wso2.carbon.identity.oauth2.model.OAuth2Parameters;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Path("/authorize")
public class OAuth2AuthzEndpoint {

    private static final Log log = LogFactory.getLog(OAuth2AuthzEndpoint.class);
    public static final String APPROVE = "approve";
    private boolean isCacheAvailable = false;

    @GET
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/html")
    public Response authorize(@Context HttpServletRequest request, @Context HttpServletResponse response)
            throws URISyntaxException {

        // Setting super-tenant carbon context
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        String clientId = request.getParameter("client_id");

        String sessionDataKeyFromLogin = getSessionDataKey(request);
        String sessionDataKeyFromConsent = request.getParameter(OAuthConstants.SESSION_DATA_KEY_CONSENT);
        SessionDataCacheKey cacheKey = null;
        SessionDataCacheEntry resultFromLogin = null;
        SessionDataCacheEntry resultFromConsent = null;

        Object flowStatus = request.getAttribute(FrameworkConstants.RequestParams.FLOW_STATUS);
        String isToCommonOauth = request.getParameter(FrameworkConstants.RequestParams.TO_COMMONAUTH);

        if ("true".equals(isToCommonOauth) && flowStatus == null) {
            try {
                return sendRequestToFramework(request, response);
            } catch (ServletException | IOException e) {
                log.error("Error occurred while sending request to authentication framework.");
                return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
            }
        }

        if (StringUtils.isNotEmpty(sessionDataKeyFromLogin)) {
            cacheKey = new SessionDataCacheKey(sessionDataKeyFromLogin);
            resultFromLogin = SessionDataCache.getInstance().getValueFromCache(cacheKey);
        }
        if (StringUtils.isNotEmpty(sessionDataKeyFromConsent)) {
            cacheKey = new SessionDataCacheKey(sessionDataKeyFromConsent);
            resultFromConsent = SessionDataCache.getInstance().getValueFromCache(cacheKey);
            SessionDataCache.getInstance().clearCacheEntry(cacheKey);
        }
        if (resultFromLogin != null && resultFromConsent != null) {

            if (log.isDebugEnabled()) {
                log.debug("Invalid authorization request.\'SessionDataKey\' found in request as parameter and " +
                        "attribute, and both have non NULL objects in cache");
            }
            return Response.status(HttpServletResponse.SC_FOUND).location(new URI(
                    EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid authorization request",
                            null, null))).build();

        } else if (clientId == null && resultFromLogin == null && resultFromConsent == null) {

            if (log.isDebugEnabled()) {
                log.debug("Invalid authorization request.\'SessionDataKey\' not found in request as parameter or " +
                        "attribute, and client_id parameter cannot be found in request");
            }
            return Response.status(HttpServletResponse.SC_FOUND).location(new URI(
                    EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid authorization request",
                            null, null))).build();

        } else if (sessionDataKeyFromLogin != null && resultFromLogin == null) {
            if (log.isDebugEnabled()) {
                log.debug("Session data not found in SessionDataCache for " + sessionDataKeyFromLogin);
            }
            return Response.status(HttpServletResponse.SC_FOUND).location(new URI(
                    EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.ACCESS_DENIED, "Session Timed Out", null, null)))
                    .build();

        } else if (sessionDataKeyFromConsent != null && resultFromConsent == null) {

            if (resultFromLogin == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Session data not found in SessionDataCache for " + sessionDataKeyFromConsent);
                }
                return Response.status(HttpServletResponse.SC_FOUND).location(new URI(
                        EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.ACCESS_DENIED, "Session Timed Out", null, null)))
                        .build();
            } else {
                sessionDataKeyFromConsent = null;
            }

        }
        SessionDataCacheEntry sessionDataCacheEntry = null;

        try {

            if (clientId != null && sessionDataKeyFromLogin == null && sessionDataKeyFromConsent == null) {
                // Authz request from client
                String redirectURL = handleOAuthAuthorizationRequest(clientId, request);

                Object attribute = request.getAttribute(FrameworkConstants.RequestParams.FLOW_STATUS);
                if (attribute != null && attribute == AuthenticatorFlowStatus.SUCCESS_COMPLETED) {
                    try {
                        return sendRequestToFramework(request, response,
                                (String) request.getAttribute(FrameworkConstants.SESSION_DATA_KEY),
                                FrameworkConstants.OAUTH2);
                    } catch (ServletException | IOException e ) {
                       log.error("Error occurred while sending request to authentication framework.");
                    }
                    return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
                } else {
                    return Response.status(HttpServletResponse.SC_FOUND).location(new URI(redirectURL)).build();
                }

            } else if (resultFromLogin != null) { // Authentication response

                sessionDataCacheEntry = resultFromLogin;
                OAuth2Parameters oauth2Params = sessionDataCacheEntry.getoAuth2Parameters();
                AuthenticationResult authnResult = getAuthenticationResult(request, sessionDataKeyFromLogin);
                if (authnResult != null) {
                    removeAuthenticationResult(request, sessionDataKeyFromLogin);

                    String redirectURL = null;
                    if (authnResult.isAuthenticated()) {
                        AuthenticatedUser authenticatedUser = authnResult.getSubject();
                        if (authenticatedUser.getUserAttributes() != null) {
                            authenticatedUser.setUserAttributes(new ConcurrentHashMap<ClaimMapping, String>(
                                    authenticatedUser.getUserAttributes()));
                        }
                        sessionDataCacheEntry.setLoggedInUser(authenticatedUser);
                        sessionDataCacheEntry.setAuthenticatedIdPs(authnResult.getAuthenticatedIdPs());
                        SessionDataCache.getInstance().addToCache(cacheKey, sessionDataCacheEntry);
                        redirectURL = doUserAuthz(request, sessionDataKeyFromLogin, sessionDataCacheEntry);
                        return Response.status(HttpServletResponse.SC_FOUND).location(new URI(redirectURL)).build();

                    } else {

                        OAuthProblemException oauthException = OAuthProblemException.error(
                                OAuth2ErrorCodes.ACCESS_DENIED, "Authentication required");
                        redirectURL = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                                .error(oauthException).location(oauth2Params.getRedirectURI())
                                .setState(oauth2Params.getState()).buildQueryMessage()
                                .getLocationUri();

                    }
                    return Response.status(HttpServletResponse.SC_FOUND).location(new URI(redirectURL)).build();

                } else {

                    String appName = sessionDataCacheEntry.getoAuth2Parameters().getApplicationName();

                    if (log.isDebugEnabled()) {
                        log.debug("Invalid authorization request. \'sessionDataKey\' attribute found but " +
                                "corresponding AuthenticationResult does not exist in the cache.");
                    }
                    return Response.status(HttpServletResponse.SC_FOUND).location(new URI(EndpointUtil
                            .getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid authorization request",
                                    appName, null))).build();

                }

            } else if (resultFromConsent != null) { // Consent submission

                sessionDataCacheEntry = ((SessionDataCacheEntry) resultFromConsent);
                OAuth2Parameters oauth2Params = sessionDataCacheEntry.getoAuth2Parameters();
                String consent = request.getParameter("consent");
                if (consent != null) {

                    if (OAuthConstants.Consent.DENY.equals(consent)) {
                        OpenIDConnectUserRPStore.getInstance().putUserRPToStore(resultFromConsent.getLoggedInUser().
                                getAuthenticatedSubjectIdentifier(), resultFromConsent.getoAuth2Parameters().
                                getApplicationName(), false);
                        // return an error if user denied
                        String denyResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                                .setError(OAuth2ErrorCodes.ACCESS_DENIED)
                                .location(oauth2Params.getRedirectURI()).setState(oauth2Params.getState())
                                .buildQueryMessage().getLocationUri();
                        return Response.status(HttpServletResponse.SC_FOUND).location(new URI(denyResponse)).build();
                    }

                    String redirectURL = handleUserConsent(request, consent, oauth2Params, sessionDataCacheEntry);

                    String authenticatedIdPs = sessionDataCacheEntry.getAuthenticatedIdPs();

                    if (authenticatedIdPs != null && !authenticatedIdPs.isEmpty()) {
                        try {
                            redirectURL = redirectURL + "&AuthenticatedIdPs=" + URLEncoder.encode(authenticatedIdPs
                                    , "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            //this exception should not occur
                            log.error("Error while encoding the url", e);
                        }
                    }

                    return Response.status(HttpServletResponse.SC_FOUND).location(new URI(redirectURL)).build();
                } else {
                    String appName = sessionDataCacheEntry.getoAuth2Parameters().getApplicationName();

                    if (log.isDebugEnabled()) {
                        log.debug("Invalid authorization request. \'sessionDataKey\' parameter found but \'consent\' " +
                                "parameter could not be found in request");
                    }
                    return Response.status(HttpServletResponse.SC_FOUND).location(new URI(
                            EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, "Invalid authorization " +
                                    "request", appName, null)))
                            .build();
                }

            } else { // Invalid request
                if (log.isDebugEnabled()) {
                    log.debug("Invalid authorization request");
                }

                return Response.status(HttpServletResponse.SC_FOUND).location(new URI(EndpointUtil.getErrorPageURL
                        (OAuth2ErrorCodes.INVALID_REQUEST, "Invalid authorization request", null, null))).build();
            }

        } catch (OAuthProblemException e) {

            if (log.isDebugEnabled()) {
                log.debug(e.getError(), e);
            }
            return Response.status(HttpServletResponse.SC_FOUND).location(new URI(
                    EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, e.getMessage(), null, null)))
                    .build();

        } catch (OAuthSystemException e) {

            String redirectUri = null;
            if (sessionDataCacheEntry != null) {
                redirectUri = sessionDataCacheEntry.getoAuth2Parameters().getRedirectURI();
            }
            if (log.isDebugEnabled()) {
                log.debug("Server error occurred while performing authorization", e);
            }
            if (StringUtils.isNotEmpty(redirectUri)) {
                return Response.status(HttpServletResponse.SC_FOUND).location(new URI(EndpointUtil.getErrorPageURL
                        (OAuth2ErrorCodes.SERVER_ERROR, "Server error occurred while performing authorization", null,
                                redirectUri))).build();
            } else {
                return Response.status(HttpServletResponse.SC_FOUND).location(new URI(EndpointUtil.getErrorPageURL
                        (OAuth2ErrorCodes.SERVER_ERROR, "Server error occurred while performing authorization", null,
                                null))).build();
            }

        } finally {
            if (sessionDataKeyFromConsent != null) {
                /*
                 * TODO Cache retaining is a temporary fix. Remove after Google fixes
                 * http://code.google.com/p/gdata-issues/issues/detail?id=6628
                 */
                String retainCache = System.getProperty("retainCache");

                if (retainCache == null) {
                    clearCacheEntry(sessionDataKeyFromConsent);
                }
            }

            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Remove authentication result from request
     * @param req
     */
    private void removeAuthenticationResult(HttpServletRequest req, String sessionDataKey) {

        if(isCacheAvailable){
            FrameworkUtils.removeAuthenticationResultFromCache(sessionDataKey);
        }else {
            req.removeAttribute(FrameworkConstants.RequestAttribute.AUTH_RESULT);
        }
    }


    /**
     * In federated and multi steps scenario there is a redirection from commonauth to samlsso so have to get
     * session data key from query parameter
     *
     * @param req Http servlet request
     * @return Session data key
     */
    private String getSessionDataKey(HttpServletRequest req) {
        String sessionDataKey = (String) req.getAttribute(OAuthConstants.SESSION_DATA_KEY);
        if (sessionDataKey == null) {
            sessionDataKey = req.getParameter(OAuthConstants.SESSION_DATA_KEY);
        }
        return sessionDataKey;
    }

    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/html")
    public Response authorizePost(@Context HttpServletRequest request,@Context HttpServletResponse response,  MultivaluedMap paramMap)
            throws URISyntaxException {
        HttpServletRequestWrapper httpRequest = new OAuthRequestWrapper(request, paramMap);
        return authorize(httpRequest, response);
    }

    /**
     * @param consent
     * @param sessionDataCacheEntry
     * @return
     * @throws OAuthSystemException
     */
    private String handleUserConsent(HttpServletRequest request, String consent, OAuth2Parameters oauth2Params,
                                     SessionDataCacheEntry sessionDataCacheEntry) throws OAuthSystemException {

        String applicationName = sessionDataCacheEntry.getoAuth2Parameters().getApplicationName();
        String loggedInUser = sessionDataCacheEntry.getLoggedInUser().getAuthenticatedSubjectIdentifier();

        boolean skipConsent = EndpointUtil.getOAuthServerConfiguration().getOpenIDConnectSkipeUserConsentConfig();
        if (!skipConsent) {
            boolean approvedAlways =
                    OAuthConstants.Consent.APPROVE_ALWAYS.equals(consent) ? true : false;
            if (approvedAlways) {
                OpenIDConnectUserRPStore.getInstance().putUserRPToStore(loggedInUser, applicationName, approvedAlways);
            }
        }

        OAuthResponse oauthResponse = null;

        // authorizing the request
        OAuth2AuthorizeRespDTO authzRespDTO = authorize(oauth2Params, sessionDataCacheEntry);

        if (authzRespDTO != null && authzRespDTO.getErrorCode() == null) {
            OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse
                    .authorizationResponse(request, HttpServletResponse.SC_FOUND);
            // all went okay
            if (StringUtils.isNotBlank(authzRespDTO.getAuthorizationCode())){
                builder.setCode(authzRespDTO.getAuthorizationCode());
                addUserAttributesToCache(sessionDataCacheEntry, authzRespDTO.getAuthorizationCode(), authzRespDTO.getCodeId());
            }
            if (StringUtils.isNotBlank(authzRespDTO.getAccessToken())){
                builder.setAccessToken(authzRespDTO.getAccessToken());
                builder.setExpiresIn(authzRespDTO.getValidityPeriod());
                builder.setParam(OAuth.OAUTH_TOKEN_TYPE, "Bearer");
            }
            if (StringUtils.isNotBlank(authzRespDTO.getIdToken())){
                builder.setParam("id_token", authzRespDTO.getIdToken());
            }
            if (StringUtils.isNotBlank(oauth2Params.getState())) {
                builder.setParam(OAuth.OAUTH_STATE, oauth2Params.getState());
            }
            String redirectURL = authzRespDTO.getCallbackURI();
            oauthResponse = builder.location(redirectURL).buildQueryMessage();

        } else if (authzRespDTO != null && authzRespDTO.getErrorCode() != null) {
            // Authorization failure due to various reasons
            String errorMsg;
            if (authzRespDTO.getErrorMsg() != null) {
                errorMsg = authzRespDTO.getErrorMsg();
            } else {
                errorMsg = "Error occurred while processing the request";
            }
            OAuthProblemException oauthProblemException = OAuthProblemException.error(
                    authzRespDTO.getErrorCode(), errorMsg);
            oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND).error(oauthProblemException)
                    .location(oauth2Params.getRedirectURI()).setState(oauth2Params.getState())
                    .buildQueryMessage();
        } else {
            // Authorization failure due to various reasons
            String errorCode = OAuth2ErrorCodes.SERVER_ERROR;
            String errorMsg = "Error occurred while processing the request";
            OAuthProblemException oauthProblemException = OAuthProblemException.error(
                    errorCode, errorMsg);
            oauthResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND).error(oauthProblemException)
                    .location(oauth2Params.getRedirectURI()).setState(oauth2Params.getState())
                    .buildQueryMessage();
        }

        return oauthResponse.getLocationUri();
    }

    private void addUserAttributesToCache(SessionDataCacheEntry sessionDataCacheEntry, String code, String codeId) {
        AuthorizationGrantCacheKey authorizationGrantCacheKey = new AuthorizationGrantCacheKey(code);
        AuthorizationGrantCacheEntry authorizationGrantCacheEntry = new AuthorizationGrantCacheEntry(
                sessionDataCacheEntry.getLoggedInUser().getUserAttributes());
        authorizationGrantCacheEntry.setNonceValue(sessionDataCacheEntry.getoAuth2Parameters().getNonce());
        authorizationGrantCacheEntry.setCodeId(codeId);
        AuthorizationGrantCache.getInstance().addToCacheByCode(authorizationGrantCacheKey, authorizationGrantCacheEntry);
    }

    /**
     * http://tools.ietf.org/html/rfc6749#section-4.1.2
     * <p/>
     * 4.1.2.1. Error Response
     * <p/>
     * If the request fails due to a missing, invalid, or mismatching
     * redirection URI, or if the client identifier is missing or invalid,
     * the authorization server SHOULD inform the resource owner of the
     * error and MUST NOT automatically redirect the user-agent to the
     * invalid redirection URI.
     * <p/>
     * If the resource owner denies the access request or if the request
     * fails for reasons other than a missing or invalid redirection URI,
     * the authorization server informs the client by adding the following
     * parameters to the query component of the redirection URI using the
     * "application/x-www-form-urlencoded" format
     *
     * @param clientId
     * @param req
     * @return
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     */
    private String handleOAuthAuthorizationRequest(String clientId, HttpServletRequest req)
            throws OAuthSystemException, OAuthProblemException {

        OAuth2ClientValidationResponseDTO clientDTO = null;
        String redirectUri = req.getParameter("redirect_uri");
        if (StringUtils.isBlank(clientId)) {
            if (log.isDebugEnabled()) {
                log.debug("Client Id is not present in the authorization request");
            }
            return EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, "Client Id is not present in the " +
                    "authorization request", null, null);
        } else if (StringUtils.isBlank(redirectUri)) {
            if (log.isDebugEnabled()) {
                log.debug("Redirect URI is not present in the authorization request");
            }
            return EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, "Redirect URI is not present in the" +
                    " authorization request", null, null);
        } else {
            clientDTO = validateClient(clientId, redirectUri);
        }

        if (!clientDTO.isValidClient()) {
            return EndpointUtil.getErrorPageURL(clientDTO.getErrorCode(), clientDTO.getErrorMsg(), null, null);
        }

        // Now the client is valid, redirect him to the authorization page.
        OAuthAuthzRequest oauthRequest = new CarbonOAuthAuthzRequest(req);

        OAuth2Parameters params = new OAuth2Parameters();
        params.setClientId(clientId);
        params.setRedirectURI(clientDTO.getCallbackURL());
        params.setResponseType(oauthRequest.getResponseType());
        params.setScopes(oauthRequest.getScopes());
        if (params.getScopes() == null) { // to avoid null pointers
            Set<String> scopeSet = new HashSet<String>();
            scopeSet.add("");
            params.setScopes(scopeSet);
        }
        params.setState(oauthRequest.getState());
        params.setApplicationName(clientDTO.getApplicationName());

        // OpenID Connect specific request parameters
        params.setNonce(oauthRequest.getParam(OAuthConstants.OAuth20Params.NONCE));
        params.setDisplay(oauthRequest.getParam(OAuthConstants.OAuth20Params.DISPLAY));
        params.setIDTokenHint(oauthRequest.getParam(OAuthConstants.OAuth20Params.ID_TOKEN_HINT));
        params.setLoginHint(oauthRequest.getParam(OAuthConstants.OAuth20Params.LOGIN_HINT));
        if (StringUtils.isNotBlank(oauthRequest.getParam("acr_values")) && !"null".equals(oauthRequest.getParam
                ("acr_values"))) {
            String[] acrValues = oauthRequest.getParam("acr_values").split(" ");
            LinkedHashSet list = new LinkedHashSet();
            for (String acrValue : acrValues) {
                list.add(acrValue);
            }
            params.setACRValues(list);
        }
        String prompt = oauthRequest.getParam(OAuthConstants.OAuth20Params.PROMPT);
        params.setPrompt(prompt);

        /**
         * The prompt parameter can be used by the Client to make sure
         * that the End-User is still present for the current session or
         * to bring attention to the request. If this parameter contains
         * none with any other value, an error is returned
         *
         * http://openid.net/specs/openid-connect-messages-
         * 1_0-14.html#anchor6
         *
         * prompt : none
         * The Authorization Server MUST NOT display any authentication or
         * consent user interface pages. An error is returned if the
         * End-User is not already authenticated or the Client does not have
         * pre-configured consent for the requested scopes. This can be used
         * as a method to check for existing authentication and/or consent.
         *
         * prompt : login
         * The Authorization Server MUST prompt the End-User for
         * reauthentication.
         *
         * Error : login_required
         * The Authorization Server requires End-User authentication. This
         * error MAY be returned when the prompt parameter in the
         * Authorization Request is set to none to request that the
         * Authorization Server should not display any user interfaces to
         * the End-User, but the Authorization Request cannot be completed
         * without displaying a user interface for user authentication.
         *
         */

        boolean forceAuthenticate = false;
        boolean checkAuthentication = false;

        // values {none, login, consent, select_profile}
        boolean contains_none = (OAuthConstants.Prompt.NONE).equals(prompt);
        String[] prompts = null;
        if (StringUtils.isNotBlank(prompt)) {
            prompts = prompt.trim().split("\\s");
            contains_none = (OAuthConstants.Prompt.NONE).equals(prompt);
            if (prompts.length > 1 && contains_none) {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid prompt variable combination. The value 'none' cannot be used with others " +
                            "prompts. Prompt: " + prompt);
                }
                return OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                        .setError(OAuth2ErrorCodes.INVALID_REQUEST)
                        .setErrorDescription("Invalid prompt variable combination. The value \'none\' cannot be used " +
                                "with others prompts.").location(params.getRedirectURI())
                        .setState(params.getState()).buildQueryMessage().getLocationUri();
            }
        }

        if ((OAuthConstants.Prompt.LOGIN).equals(prompt)) { // prompt for authentication
            checkAuthentication = false;
            forceAuthenticate = true;
        } else if (contains_none) {
            checkAuthentication = true;
            forceAuthenticate = false;
        } else if ((OAuthConstants.Prompt.CONSENT).equals(prompt)) {
            checkAuthentication = false;
            forceAuthenticate = false;
        }

        String sessionDataKey = UUIDGenerator.generateUUID();
        SessionDataCacheKey cacheKey = new SessionDataCacheKey(sessionDataKey);
        SessionDataCacheEntry sessionDataCacheEntryNew = new SessionDataCacheEntry();
        sessionDataCacheEntryNew.setoAuth2Parameters(params);
        sessionDataCacheEntryNew.setQueryString(req.getQueryString());

        if (req.getParameterMap() != null) {
            sessionDataCacheEntryNew.setParamMap(new ConcurrentHashMap<String, String[]>(req.getParameterMap()));
        }
        SessionDataCache.getInstance().addToCache(cacheKey, sessionDataCacheEntryNew);

        try {
            req.setAttribute(FrameworkConstants.RequestParams.FLOW_STATUS, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
            req.setAttribute(FrameworkConstants.SESSION_DATA_KEY, sessionDataKey);
            return EndpointUtil.getLoginPageURL(clientId, sessionDataKey, forceAuthenticate,
                    checkAuthentication, oauthRequest.getScopes(), req.getParameterMap());

        } catch (IdentityOAuth2Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while retrieving the login page url.", e);
            }
            throw new OAuthSystemException("Error when encoding login page URL");
        }
    }

    /**
     * Validates the client using the oauth2 service
     *
     * @param clientId
     * @param callbackURL
     * @return
     */
    private OAuth2ClientValidationResponseDTO validateClient(String clientId, String callbackURL) {
        return EndpointUtil.getOAuth2Service().validateClientInfo(clientId, callbackURL);
    }

    /**
     * prompt : none
     * The Authorization Server MUST NOT display any authentication
     * or consent user interface pages. An error is returned if the
     * End-User is not already authenticated or the Client does not
     * have pre-configured consent for the requested scopes. This
     * can be used as a method to check for existing authentication
     * and/or consent.
     * <p/>
     * prompt : consent
     * The Authorization Server MUST prompt the End-User for consent before
     * returning information to the Client.
     * <p/>
     * prompt Error : consent_required
     * The Authorization Server requires End-User consent. This
     * error MAY be returned when the prompt parameter in the
     * Authorization Request is set to none to request that the
     * Authorization Server should not display any user
     * interfaces to the End-User, but the Authorization Request
     * cannot be completed without displaying a user interface
     * for End-User consent.
     *
     * @param sessionDataCacheEntry
     * @return
     * @throws OAuthSystemException
     */
    private String doUserAuthz(HttpServletRequest request, String sessionDataKey,
                               SessionDataCacheEntry sessionDataCacheEntry)
            throws OAuthSystemException {

        OAuth2Parameters oauth2Params = sessionDataCacheEntry.getoAuth2Parameters();
        String loggedInUser = sessionDataCacheEntry.getLoggedInUser().getAuthenticatedSubjectIdentifier();

        boolean skipConsent = EndpointUtil.getOAuthServerConfiguration().getOpenIDConnectSkipeUserConsentConfig();

        // load the users approved applications to skip consent
        String appName = oauth2Params.getApplicationName();
        boolean hasUserApproved = OpenIDConnectUserRPStore.getInstance().hasUserApproved(loggedInUser, appName);
        String consentUrl;
        String errorResponse = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND)
                .setError(OAuth2ErrorCodes.ACCESS_DENIED)
                .location(oauth2Params.getRedirectURI())
                .setState(oauth2Params.getState()).buildQueryMessage()
                .getLocationUri();

        consentUrl = EndpointUtil.getUserConsentURL(oauth2Params, loggedInUser, sessionDataKey,
                OAuth2Util.isOIDCAuthzRequest(oauth2Params.getScopes()) ? true : false);

        //Skip the consent page if User has provided approve always or skip consent from file
        if ((OAuthConstants.Prompt.CONSENT).equals(oauth2Params.getPrompt())) {
            return consentUrl;

        } else if ((OAuthConstants.Prompt.NONE).equals(oauth2Params.getPrompt())) {
            //Returning error if the user has not previous session
            if (sessionDataCacheEntry.getLoggedInUser() == null) {
                return errorResponse;
            } else {
                if (skipConsent || hasUserApproved) {
                    return handleUserConsent(request, APPROVE, oauth2Params, sessionDataCacheEntry);
                } else {
                    return errorResponse;
                }
            }

        } else if (((OAuthConstants.Prompt.LOGIN).equals(oauth2Params.getPrompt()) || StringUtils.isBlank(oauth2Params.getPrompt()))) {
            if (skipConsent || hasUserApproved) {
                return handleUserConsent(request, APPROVE, oauth2Params, sessionDataCacheEntry);
            } else {
                return consentUrl;
            }
        } else {
            return StringUtils.EMPTY;
        }

    }

    /**
     * Here we set the authenticated user to the session data
     *
     * @param oauth2Params
     * @return
     */
    private OAuth2AuthorizeRespDTO authorize(OAuth2Parameters oauth2Params
            , SessionDataCacheEntry sessionDataCacheEntry) {

        OAuth2AuthorizeReqDTO authzReqDTO = new OAuth2AuthorizeReqDTO();
        authzReqDTO.setCallbackUrl(oauth2Params.getRedirectURI());
        authzReqDTO.setConsumerKey(oauth2Params.getClientId());
        authzReqDTO.setResponseType(oauth2Params.getResponseType());
        authzReqDTO.setScopes(oauth2Params.getScopes().toArray(new String[oauth2Params.getScopes().size()]));
        authzReqDTO.setUsername(sessionDataCacheEntry.getLoggedInUser().getAuthenticatedSubjectIdentifier());
        authzReqDTO.setACRValues(oauth2Params.getACRValues());
        authzReqDTO.setNonce(oauth2Params.getNonce());
        return EndpointUtil.getOAuth2Service().authorize(authzReqDTO);
    }

    private void clearCacheEntry(String sessionDataKey) {
        if (sessionDataKey != null) {
            SessionDataCacheKey cacheKey = new SessionDataCacheKey(sessionDataKey);
            SessionDataCacheEntry result = SessionDataCache.getInstance().getValueFromCache(cacheKey);
            if (result != null) {
                SessionDataCache.getInstance().clearCacheEntry(cacheKey);
            }
        }
    }

    /**
     * Get authentication result
     * When using federated or multiple steps authenticators, there is a redirection from commonauth to samlsso,
     * So in that case we cannot use request attribute and have to get the result from cache
     *
     * @param req Http servlet request
     * @param sessionDataKey Session data key
     * @return
     */
    private AuthenticationResult getAuthenticationResult(HttpServletRequest req, String sessionDataKey) {

        AuthenticationResult result = getAuthenticationResultFromRequest(req);
        if (result == null) {
            isCacheAvailable = true;
            result = getAuthenticationResultFromCache(sessionDataKey);
        }
        return result;
    }

    private AuthenticationResult getAuthenticationResultFromCache(String sessionDataKey) {
        AuthenticationResult authResult = null;
        AuthenticationResultCacheEntry authResultCacheEntry = FrameworkUtils
                .getAuthenticationResultFromCache(sessionDataKey);
        if (authResultCacheEntry != null) {
            authResult = authResultCacheEntry.getResult();
        } else {
            log.error("Cannot find AuthenticationResult from the cache");
        }
        return authResult;
    }

    /**
     * Get authentication result from request
     *
     * @param request  Http servlet request
     * @return
     */
    private AuthenticationResult getAuthenticationResultFromRequest(HttpServletRequest request) {

        return (AuthenticationResult) request.getAttribute(FrameworkConstants.RequestAttribute.AUTH_RESULT);
    }

    /**
     * In SAML there is no redirection from authentication endpoint to  commonauth and it send a post request to samlsso
     * servlet and sending the request to authentication framework from here, this overload method not sending
     * sessionDataKey and type to commonauth that's why overloaded the method here
     *
     * @param request Http servlet request
     * @param response Http servlet response
     * @throws ServletException
     * @throws IOException
     */
    private Response sendRequestToFramework(HttpServletRequest request,
            HttpServletResponse response) throws ServletException,IOException,URISyntaxException {

        CommonAuthenticationHandler commonAuthenticationHandler = new CommonAuthenticationHandler();

        CommonAuthResponseWrapper responseWrapper = new CommonAuthResponseWrapper(response);
        commonAuthenticationHandler.doGet(request, responseWrapper);

        Object attribute = request.getAttribute(FrameworkConstants.RequestParams.FLOW_STATUS);
        if (attribute != null) {
            if (attribute == AuthenticatorFlowStatus.INCOMPLETE) {
                if (responseWrapper.getRedirectURL()
                        .contains(ConfigurationFacade.getInstance().getAuthenticationEndpointURL())) {
                    response.sendRedirect(responseWrapper.getRedirectURL());
                } else {
                    response.sendRedirect(responseWrapper.getRedirectURL());
                }
            } else {
                return authorize(request, response);
            }
        } else {
            request.setAttribute(FrameworkConstants.RequestParams.FLOW_STATUS, AuthenticatorFlowStatus.UNKNOWN);
            return authorize(request, response);
        }
        return null;
    }

    /**
     * This method use to call authentication framework directly via API other than using HTTP redirects.
     * Sending wrapper request object to doGet method since other original request doesn't exist required parameters
     * Doesn't check SUCCESS_COMPLETED since taking decision with INCOMPLETE status
     *
     *
     * @param request  Http Request
     * @param response Http Response
     * @param sessionDataKey Session data key
     * @param type authenticator type
     * @throws ServletException
     * @throws IOException
     */
    private Response sendRequestToFramework(HttpServletRequest request, HttpServletResponse response,
            String sessionDataKey, String type) throws ServletException, IOException, URISyntaxException {

        CommonAuthenticationHandler commonAuthenticationHandler = new CommonAuthenticationHandler();

        CommonAuthRequestWrapper requestWrapper = new CommonAuthRequestWrapper(request);
        requestWrapper.setParameter(FrameworkConstants.SESSION_DATA_KEY, sessionDataKey);
        requestWrapper.setParameter(FrameworkConstants.RequestParams.TYPE, type);

        CommonAuthResponseWrapper responseWrapper = new CommonAuthResponseWrapper(response);
        commonAuthenticationHandler.doGet(requestWrapper, responseWrapper);

        Object attribute = request.getAttribute(FrameworkConstants.RequestParams.FLOW_STATUS);
        if (attribute != null) {
            if (attribute == AuthenticatorFlowStatus.INCOMPLETE) {

                if (responseWrapper.getRedirectURL()
                        .contains(ConfigurationFacade.getInstance().getAuthenticationEndpointURL())) {
                    response.sendRedirect(responseWrapper.getRedirectURL());
                } else {
                    response.sendRedirect(responseWrapper.getRedirectURL());
                }
            } else {
                return authorize(requestWrapper, responseWrapper);
            }
        } else {
            requestWrapper.setAttribute(FrameworkConstants.RequestParams.FLOW_STATUS, AuthenticatorFlowStatus.UNKNOWN);
            return authorize(requestWrapper, responseWrapper);
        }
        return null;
    }

}