/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.social.google;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleOAuth2Authenticator extends OpenIDConnectAuthenticator {

    private static final long serialVersionUID = -4154255583070524018L;

    private static Log log = LogFactory.getLog(GoogleOAuth2Authenticator.class);

    /**
     * Get Authorization Server Endpoint
     *
     * @param authenticatorProperties
     * @return
     */
    @Override
    protected String getAuthorizationServerEndpoint(
            Map<String, String> authenticatorProperties) {

        return authenticatorProperties.get(GoogleOAuth2AuthenticationConstant.GOOGLE_OAUTH_ENDPOINT);
    }

    /**
     * Get Token Endpoint
     *
     * @param authenticatorProperties
     * @return
     */
    @Override
    protected String getTokenEndpoint(
            Map<String, String> authenticatorProperties) {

        return authenticatorProperties.get(GoogleOAuth2AuthenticationConstant.GOOGLE_TOKEN_ENDPOINT);
    }

    /**
     * This is override because of query string values hard coded and input
     * values validations are not required.
     *
     * @param request
     * @param response
     * @param context
     * @throws AuthenticationFailedException
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        try {
            Map<String, String> authenticatorProperties = context
                    .getAuthenticatorProperties();
            if (authenticatorProperties != null) {
                String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
                String authorizationEP;
                if (getAuthorizationServerEndpoint(authenticatorProperties) != null) {
                    authorizationEP = getAuthorizationServerEndpoint(authenticatorProperties);
                } else {
                    authorizationEP = authenticatorProperties.get(OIDCAuthenticatorConstants.OAUTH2_AUTHZ_URL);
                }

                String callBackUrl = authenticatorProperties.get(GoogleOAuth2AuthenticationConstant.CALLBACK_URL);

                if (log.isDebugEnabled()) {
                    log.debug("Google-callback-url : " + callBackUrl);
                }

                if (callBackUrl == null) {
                    callBackUrl = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH, false);
                }

                String state = context.getContextIdentifier() + "," + OIDCAuthenticatorConstants.LOGIN_TYPE;

                state = getState(state, authenticatorProperties);

                OAuthClientRequest authzRequest;

                // This is the query string need to send in order to get email and
                // profile
                String queryString = GoogleOAuth2AuthenticationConstant.QUERY_STRING;

                authzRequest = OAuthClientRequest
                        .authorizationLocation(authorizationEP)
                        .setClientId(clientId)
                        .setRedirectURI(callBackUrl)
                        .setResponseType(
                                OIDCAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE)
                        .setState(state).buildQueryMessage();

                String loginPage = authzRequest.getLocationUri();
                String domain = request.getParameter("domain");

                if (domain != null) {
                    loginPage = loginPage + "&fidp=" + domain;
                }

                if (queryString != null) {
                    if (!queryString.startsWith("&")) {
                        loginPage = loginPage + "&" + queryString;
                    } else {
                        loginPage = loginPage + queryString;
                    }
                }
                response.sendRedirect(loginPage);

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Error while retrieving properties. Authenticator Properties cannot be null");
                }
                throw new AuthenticationFailedException(
                        "Error while retrieving properties. Authenticator Properties cannot be null");
            }
        } catch (IOException e) {
            throw new AuthenticationFailedException("Exception while sending to the login page", e);
        } catch (OAuthSystemException e) {
            throw new AuthenticationFailedException("Exception while building authorization code request", e);
        }
    }

    /**
     * Get Scope
     *
     * @param scope
     * @param authenticatorProperties
     * @return
     */
    @Override
    protected String getScope(String scope,
                              Map<String, String> authenticatorProperties) {
        return OIDCAuthenticatorConstants.OAUTH_OIDC_SCOPE;
    }

    /**
     * Get Authenticated User
     *
     * @param token
     * @return
     */
    @Override
    protected String getAuthenticateUser(OAuthClientResponse token) {
        return token.getParam(OIDCAuthenticatorConstants.Claim.EMAIL);
    }

    /**
     * Get Subject Attributes
     *
     * @param token
     * @return
     */
    @Override
    protected Map<ClaimMapping, String> getSubjectAttributes(
            OAuthClientResponse token) {

        Map<ClaimMapping, String> claims = new HashMap<ClaimMapping, String>();

        try {

                String json = sendRequest(token.getParam(GoogleOAuth2AuthenticationConstant.GOOGLE_USERINFO_ENDPOINT),
                        token.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN));
            if (StringUtils.isNotBlank(json)) {
                Map<String, Object> jsonObject = JSONUtils.parseJSON(json);

                if (jsonObject != null) {
                    for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                        claims.put(ClaimMapping.build(entry.getKey(),
                                entry.getKey(), null, false), entry.getValue()
                                .toString());
                        if (log.isDebugEnabled()) {
                            log.debug("Adding claim from end-point data mapping : " + entry.getKey() + " - " +
                                    entry.getValue());
                        }

                    }
                }

            }
        }catch (Exception e) {
            log.error("Error occurred while accessing google user info endpoint", e);
        }

        return claims;
    }

    /**
     * Get Configuration Properties
     *
     * @return
     */
    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<Property>();

        Property clientId = new Property();
        clientId.setName(OIDCAuthenticatorConstants.CLIENT_ID);
        clientId.setDisplayName("Client Id");
        clientId.setRequired(true);
        clientId.setDescription("Enter Google IDP client identifier value");
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName(OIDCAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setDisplayName("Client Secret");
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Google IDP client secret value");
        configProperties.add(clientSecret);

        Property callbackUrl = new Property();
        callbackUrl.setDisplayName("Callback Url");
        callbackUrl.setName(GoogleOAuth2AuthenticationConstant.CALLBACK_URL);
        callbackUrl.setRequired(true);
        callbackUrl.setDescription("Enter value corresponding to callback url.");
        configProperties.add(callbackUrl);

        Property oauthEndpoint = new Property();
        oauthEndpoint.setDisplayName("Google Oauth Endpoint");
        oauthEndpoint.setName(GoogleOAuth2AuthenticationConstant.GOOGLE_OAUTH_ENDPOINT);
        oauthEndpoint.setValue(IdentityApplicationConstants.GOOGLE_OAUTH_URL);
        oauthEndpoint.setDescription("Enter value corresponding to google oauth endpoint.");
        configProperties.add(oauthEndpoint);

        Property tokenEndpoint = new Property();
        tokenEndpoint.setDisplayName("Google Token Endpoint");
        tokenEndpoint.setName(GoogleOAuth2AuthenticationConstant.GOOGLE_TOKEN_ENDPOINT);
        tokenEndpoint.setValue(IdentityApplicationConstants.GOOGLE_TOKEN_URL);
        tokenEndpoint.setDescription("Enter value corresponding to google token endpoint.");
        configProperties.add(tokenEndpoint);

        Property userInfoEndpoint = new Property();
        userInfoEndpoint.setDisplayName("Google User Info Endpoint");
        userInfoEndpoint.setName(GoogleOAuth2AuthenticationConstant.GOOGLE_USERINFO_ENDPOINT);
        userInfoEndpoint.setValue(IdentityApplicationConstants.GOOGLE_USERINFO_URL);
        userInfoEndpoint.setDescription("Enter value corresponding to google user info endpoint.");
        configProperties.add(userInfoEndpoint);

        return configProperties;
    }

    /**
     * this method are overridden for extra claim request to google end-point
     *
     * @param request
     * @param response
     * @param context
     * @throws AuthenticationFailedException
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
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

            String callBackUrl = authenticatorProperties.get(GoogleOAuth2AuthenticationConstant.CALLBACK_URL);

            log.debug("callBackUrl : " + callBackUrl);

            if (callBackUrl == null) {
                callBackUrl = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH, false);
            }

            @SuppressWarnings({"unchecked"})
            Map<String, String> paramValueMap = (Map<String, String>) context.getProperty("oidc:param.map");

            if (paramValueMap != null
                    && paramValueMap.containsKey("redirect_uri")) {
                callBackUrl = paramValueMap.get("redirect_uri");
            }

            OAuthAuthzResponse authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = authzResponse.getCode();

            OAuthClientRequest accessRequest = null;
            accessRequest = getAccessRequest(tokenEndPoint, clientId, clientSecret, callBackUrl, code);

            // create OAuth client that uses custom http client under the hood
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthClientResponse oAuthResponse = null;
            oAuthResponse = getOAuthResponse(accessRequest, oAuthClient, oAuthResponse);
            // TODO : return access token and id token to framework
            String accessToken = "";
            String idToken = "";
            if (oAuthResponse != null) {
                accessToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN);
                idToken = oAuthResponse.getParam(OIDCAuthenticatorConstants.ID_TOKEN);
            }

            if (accessToken != null && (idToken != null || !requiredIDToken(authenticatorProperties))) {

                context.setProperty(OIDCAuthenticatorConstants.ACCESS_TOKEN, accessToken);

                if (idToken != null) {
                    context.setProperty(OIDCAuthenticatorConstants.ID_TOKEN, idToken);

                    String base64Body = idToken.split("\\.")[1];
                    byte[] decoded = Base64.decodeBase64(base64Body.getBytes());
                    String json = new String(decoded, Charset.forName("utf-8"));

                    if (log.isDebugEnabled()) {
                        log.debug("Id token json string : " + json);
                    }

                    Map<String, Object> jsonObject = JSONUtils.parseJSON(json);

                    if (jsonObject != null) {
                        Map<ClaimMapping, String> claims = getSubjectAttributes(oAuthResponse);

                        String authenticatedUser = (String) jsonObject.get(OIDCAuthenticatorConstants.Claim.EMAIL);
                        AuthenticatedUser authenticatedUserObj = AuthenticatedUser
                                .createFederateAuthenticatedUserFromSubjectIdentifier(authenticatedUser);
                        authenticatedUserObj.setUserAttributes(claims);
                        context.setSubject(authenticatedUserObj);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Decoded json object is null");
                        }
                        throw new AuthenticationFailedException("Decoded json object is null");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Authentication Failed");
                    }
                    throw new AuthenticationFailedException("Authentication Failed");
                }

            } else {
                throw new AuthenticationFailedException("Authentication Failed");
            }
        } catch (OAuthProblemException e) {
            throw new AuthenticationFailedException("Error occurred while acquiring access token", e);
        }
    }

    private OAuthClientResponse getOAuthResponse(OAuthClientRequest accessRequest, OAuthClient oAuthClient, OAuthClientResponse oAuthResponse) throws AuthenticationFailedException {
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

    private OAuthClientRequest getAccessRequest(String tokenEndPoint, String clientId, String clientSecret
            , String callBackUrl, String code)
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
     * Get Friendly Name
     *
     * @return
     */
    @Override
    public String getFriendlyName() {
        return GoogleOAuth2AuthenticationConstant.GOOGLE_CONNECTOR_FRIENDLY_NAME;
    }

    /**
     * GetName
     *
     * @return
     */
    @Override
    public String getName() {
        return GoogleOAuth2AuthenticationConstant.GOOGLE_CONNECTOR_NAME;
    }

    /**
     * extra request sending to google user info end-point
     *
     * @param url
     * @param accessToken
     * @return
     * @throws IOException
     */
    private String sendRequest(String url, String accessToken)
            throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("claim url: " + url + " & accessToken : " + accessToken);
        }
        if (url != null) {
            URL obj = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) obj.openConnection();

            urlConnection.setRequestMethod("GET");
            // add request header
            urlConnection.setRequestProperty("Authorization", "Bearer " + accessToken);
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder b = new StringBuilder();
            String inputLine = in.readLine();
            while (inputLine != null) {
                b.append(inputLine).append("\n");
                inputLine = in.readLine();
            }
            in.close();

            if (log.isDebugEnabled()) {
                log.debug("response: " + b.toString());
            }
        return b.toString();
    }
        else{
            return StringUtils.EMPTY;
        }
    }
}
