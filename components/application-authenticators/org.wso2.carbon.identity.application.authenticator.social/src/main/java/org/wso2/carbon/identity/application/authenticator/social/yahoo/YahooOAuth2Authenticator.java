/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.identity.application.authenticator.social.yahoo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 Authenticator for Yahoo.
 */
public class YahooOAuth2Authenticator extends OpenIDConnectAuthenticator {

    private static Log log = LogFactory.getLog(YahooOAuth2Authenticator.class);

    /**
     * Get authorization server endpoint.
     * @param authenticatorProperties Authenticator properties.
     * @return OAuth2 Endpoint
     */
    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {

        return authenticatorProperties.get(YahooOAuth2AuthenticatorConstants.YAHOO_OAUTH2_ENDPOINT);
    }

    /**
     * Get token access endpoint.
     * @param authenticatorProperties Authenticator properties.
     * @return
     */
    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {

        return authenticatorProperties.get(YahooOAuth2AuthenticatorConstants.YAHOO_TOKEN_ENDPOINT);
    }

    /**
     * This is overridden because of query string values are hard coded and input
     * value validations are not required.
     * @param request Servlet request.
     * @param response Servlet response.
     * @param context Authentication context.
     * @throws AuthenticationFailedException
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {

        try {

            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

            if (authenticatorProperties == null) {

                if (log.isDebugEnabled()) {
                    log.debug("Error while retrieving properties. Authenticator Properties cannot be null");
                }

                throw new AuthenticationFailedException("Error while retrieving properties. " +
                        "Authenticator Properties cannot be null");
            }

            String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
            String authorizationEP;

            if (getAuthorizationServerEndpoint(authenticatorProperties) != null) {
                authorizationEP = getAuthorizationServerEndpoint(authenticatorProperties);
            } else {
                authorizationEP = authenticatorProperties.get(OIDCAuthenticatorConstants.OAUTH2_AUTHZ_URL);
            }

            String callBackUrl = authenticatorProperties.get(YahooOAuth2AuthenticatorConstants.CALLBACK_URL);

            if (log.isDebugEnabled()) {
                log.debug("Yahoo callback url : " + callBackUrl);
            }

            if (callBackUrl == null || StringUtils.isEmpty(callBackUrl)) {
                callBackUrl = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH);
            }

            String state = context.getContextIdentifier() + "," + OIDCAuthenticatorConstants.LOGIN_TYPE;

            state = getState(state, authenticatorProperties);

            OAuthClientRequest authzRequest = OAuthClientRequest
                    .authorizationLocation(authorizationEP)
                    .setClientId(clientId)
                    .setRedirectURI(callBackUrl)
                    .setResponseType(OIDCAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE)
                    .setState(state).buildQueryMessage();

            String loginPage = authzRequest.getLocationUri();

            response.sendRedirect(loginPage);

        } catch (IOException e) {
            throw new AuthenticationFailedException("Exception while redirecting to the login page", e);
        } catch (OAuthSystemException e) {
            throw new AuthenticationFailedException("Exception while building authorization code request", e);
        }
    }

    /**
     * Get OAuth2 Scope
     * @param scope Scope
     * @param authenticatorProperties Authentication properties.
     * @return OAuth2 Scope
     */
    @Override
    protected String getScope(String scope, Map<String, String> authenticatorProperties) {

        return OIDCAuthenticatorConstants.OAUTH_OIDC_SCOPE;
    }

    /**
     * Get Authenticated User
     * @param token OAuth client response.
     * @return GUID of the authenticated user.
     */
    @Override
    protected String getAuthenticateUser(OAuthClientResponse token) {

        return token.getParam(YahooOAuth2AuthenticatorConstants.USER_GUID);
    }

    /**
     * Get Subject Attributes
     * @param token
     * @return
     */
    protected Map<ClaimMapping, String> getSubjectAttributes(OAuthClientResponse token) {

        Map<ClaimMapping, String> claims = new HashMap<ClaimMapping, String>();

        try {

            String accessToken = token.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN);
            String userGUID = token.getParam(YahooOAuth2AuthenticatorConstants.USER_GUID);

            String url = IdentityApplicationConstants.YAHOO_USERINFO_URL
                    + userGUID
                    + YahooOAuth2AuthenticatorConstants.YAHOO_USER_DETAILS_JSON;

            String json = sendRequest(url, accessToken);

            if (!StringUtils.isNotBlank(json)) {
                log.info("Unable to fetch user claims. Proceeding without user claims");
                return claims;
            }

            Map<String, Object> jsonObject = JSONUtils.parseJSON(json);

            // Extract the inside profile JSON object.
            Map<String, Object> profile = JSONUtils.parseJSON(
                    jsonObject.entrySet().iterator().next().getValue().toString());

            if (profile == null) {
                log.info("Invalid user profile object. Proceeding without user claims");
                return claims;
            }

            for (Map.Entry<String, Object> data : profile.entrySet()) {

                String key = data.getKey();

                claims.put(ClaimMapping.build(key, key, null, false), profile.get(key).toString());

                if (log.isDebugEnabled()) {
                    log.debug("Adding claims from end-point data mapping : " + key + " - " +
                            profile.get(key).toString());
                }
            }

        } catch (Exception e) {
            log.error("Error occurred while accessing yahoo user info endpoint", e);
        }

        return claims;
    }

    /**
     * Get configuration properties.
     * @return Properties list.
     */
    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<Property>();

        Property clientId = new Property();
        clientId.setName(OIDCAuthenticatorConstants.CLIENT_ID);
        clientId.setDisplayName("Client Id");
        clientId.setRequired(true);
        clientId.setDescription("Enter Yahoo IDP client identifier value");
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName(OIDCAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setDisplayName("Client Secret");
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Yahoo IDP client secret value");
        configProperties.add(clientSecret);

        Property callbackUrl = new Property();
        callbackUrl.setDisplayName("Callback URL");
        callbackUrl.setName(YahooOAuth2AuthenticatorConstants.CALLBACK_URL);
        callbackUrl.setDescription("Enter value corresponding to callback url.");
        configProperties.add(callbackUrl);

        Property oauthEndpoint = new Property();
        oauthEndpoint.setDisplayName("Yahoo Oauth2 Endpoint");
        oauthEndpoint.setName(YahooOAuth2AuthenticatorConstants.YAHOO_OAUTH2_ENDPOINT);
        oauthEndpoint.setValue(IdentityApplicationConstants.YAHOO_OAUTH2_URL);
        oauthEndpoint.setDescription("Enter value corresponding to Yahoo oauth2 endpoint.");
        configProperties.add(oauthEndpoint);

        Property tokenEndpoint = new Property();
        tokenEndpoint.setDisplayName("Yahoo Token Endpoint");
        tokenEndpoint.setName(YahooOAuth2AuthenticatorConstants.YAHOO_TOKEN_ENDPOINT);
        tokenEndpoint.setValue(IdentityApplicationConstants.YAHOO_TOKEN_URL);
        tokenEndpoint.setDescription("Enter value corresponding to Yahoo token endpoint.");
        configProperties.add(tokenEndpoint);

        return configProperties;
    }

    /**
     * This method is overridden to get user claims from the Yahoo.
     * @param request Servlet request.
     * @param response Servlet response.
     * @param context Authentication context.
     * @throws AuthenticationFailedException
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {

        try {

            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

            String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
            String clientSecret = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_SECRET);
            String tokenEndPoint;

            if (getTokenEndpoint(authenticatorProperties) != null) {
                tokenEndPoint = getTokenEndpoint(authenticatorProperties);
            } else {
                tokenEndPoint = authenticatorProperties.get(OIDCAuthenticatorConstants.OAUTH2_TOKEN_URL);
            }

            String callBackUrl = authenticatorProperties.get(YahooOAuth2AuthenticatorConstants.CALLBACK_URL);

            if (log.isDebugEnabled()) {
                log.debug("Tenant Domain: " + context.getTenantDomain());
                log.debug("Service provider name: " + context.getServiceProviderName());
                log.debug("Token endpoint: " + tokenEndPoint);
                log.debug("Callback URL: " + callBackUrl);
            }

            if (callBackUrl == null) {
                callBackUrl = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH);
            }

            OAuthAuthzResponse authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = authzResponse.getCode();

            OAuthClientRequest accessRequest = getAccessRequest(
                    tokenEndPoint, clientId, clientSecret, callBackUrl, code);

            // Create OAuth client that uses custom http client under the hood.
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthClientResponse oAuthResponse = null;
            oAuthResponse = getOAuthResponse(accessRequest, oAuthClient, oAuthResponse);

            if (oAuthResponse == null) {
                throw new AuthenticationFailedException("Null authentication response");
            }

            String accessToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN);
            String refreshToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.REFRESH_TOKEN);
            String userGUID = oAuthResponse.getParam(YahooOAuth2AuthenticatorConstants.USER_GUID);

            if (accessToken != null && userGUID != null) {

                context.setProperty(OIDCAuthenticatorConstants.ACCESS_TOKEN, accessToken);
                context.setProperty(OIDCAuthenticatorConstants.REFRESH_TOKEN, refreshToken);

                Map<ClaimMapping, String> claims = getSubjectAttributes(oAuthResponse);
                AuthenticatedUser authenticatedUserObj = AuthenticatedUser
                        .createFederateAuthenticatedUserFromSubjectIdentifier(userGUID);
                authenticatedUserObj.setUserAttributes(claims);

                context.setSubject(authenticatedUserObj);

            } else {
                throw new AuthenticationFailedException("Authentication Failed");
            }

        } catch (OAuthProblemException e) {
            throw new AuthenticationFailedException("Error occurred while acquiring access token", e);
        }
    }

    private OAuthClientResponse getOAuthResponse(OAuthClientRequest accessRequest,
                                                 OAuthClient oAuthClient,
                                                 OAuthClientResponse oAuthResponse)
            throws AuthenticationFailedException {

        OAuthClientResponse oAuthClientResponse = oAuthResponse;

        try {
            oAuthClientResponse = oAuthClient.accessToken(accessRequest);
        } catch (OAuthSystemException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception while requesting access token", e);
            }
            throw new AuthenticationFailedException("Exception while requesting access token", e);

        } catch (OAuthProblemException e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception while requesting access token", e);
            }
        }

        return oAuthClientResponse;
    }

    private OAuthClientRequest getAccessRequest(String tokenEndPoint,
                                                String clientId,
                                                String clientSecret,
                                                String callBackUrl,
                                                String code)
            throws AuthenticationFailedException {

        OAuthClientRequest accessRequest = null;

        try {
            accessRequest = OAuthClientRequest.tokenLocation(tokenEndPoint)
                    .setGrantType(GrantType.AUTHORIZATION_CODE).setClientId(clientId).setClientSecret(clientSecret)
                    .setRedirectURI(callBackUrl).setCode(code).buildBodyMessage();
        } catch (OAuthSystemException e) {
            throw new AuthenticationFailedException("Exception while building request for request access token", e);
        }

        return accessRequest;
    }

    /**
     * Get Friendly Name.
     * @return Friendly name.
     */
    @Override
    public String getFriendlyName() {

        return YahooOAuth2AuthenticatorConstants.YAHOO_CONNECTOR_FRIENDLY_NAME;
    }

    /**
     * Get connector name.
     * @return Connector name.
     */
    @Override
    public String getName() {

        return YahooOAuth2AuthenticatorConstants.YAHOO_CONNECTOR_NAME;
    }

    /**
     * Request user claims from user info endpoint.
     * @param url User info endpoint.
     * @param accessToken Access token.
     * @return Response string.
     * @throws IOException
     */
    private String sendRequest(String url, String accessToken)
            throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("Claim URL: " + url + " & Access-Token : " + accessToken);
        }

        if (url == null) {
            return StringUtils.EMPTY;
        }

        URL obj = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) obj.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);

        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder builder = new StringBuilder();

        String inputLine = reader.readLine();

        while (inputLine != null) {
            builder.append(inputLine).append("\n");
            inputLine = reader.readLine();
        }

        reader.close();

        if (log.isDebugEnabled()) {
            log.debug("response: " + builder.toString());
        }

        return builder.toString();
    }
}