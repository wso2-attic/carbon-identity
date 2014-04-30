/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authenticator.facebook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.client.response.OAuthAuthzResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.utils.JSONUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.ui.CarbonUIUtil;

public class FacebookAuthenticator extends AbstractApplicationAuthenticator implements
                                                                           FederatedApplicationAuthenticator {

    private static final long serialVersionUID = 1L;
    private static final Log LOGGER = LogFactory.getLog(FacebookAuthenticator.class);

    public boolean canHandle(HttpServletRequest request) {

        LOGGER.trace("Inside FacebookAuthenticator.canHandle()");

        // From login page asking for Facebook login
        if (request.getParameter(FacebookAuthenticatorConstants.LOGIN_TYPE) != null &&
            FacebookAuthenticatorConstants.FACEBOOK_LOGIN_TYPE.equals(request.getParameter(FacebookAuthenticatorConstants.LOGIN_TYPE))) {
            return true;
        }

        // Check commonauth got an OIDC response
        if (request.getParameter(FacebookAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE) != null &&
            request.getParameter(FacebookAuthenticatorConstants.OAUTH2_PARAM_STATE) != null) {
            return true;
        }

        return false;
    }

    @Override
    public AuthenticatorStatus authenticate(HttpServletRequest request,
                                            HttpServletResponse response,
                                            AuthenticationContext context) {

        LOGGER.trace("Inside FacebookAuthenticator.authenticate()");

        if (request.getParameter(FacebookAuthenticatorConstants.LOGIN_TYPE) != null &&
            FacebookAuthenticatorConstants.FACEBOOK_LOGIN_TYPE.equals(request.getParameter(FacebookAuthenticatorConstants.LOGIN_TYPE))) {
            sendInitialRequest(request, response, null);
            return AuthenticatorStatus.CONTINUE;
        }

        try {
            ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
            String clientId = externalIdPConfig.getFbauthClientId();
            String clientSecret = externalIdPConfig.getFbauthClientSecret();
            String tokenEndPoint = FacebookAuthenticatorConstants.FB_TOKEN_URL;
            String fbauthUserInfoUrl = FacebookAuthenticatorConstants.FB_USER_INFO_URL;

            String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
            callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");

            String code = getAuthorizationCode(request);
            String token = getToken(tokenEndPoint, clientId, clientSecret, callbackurl, code);
            String authenticatedUser = getUserName(fbauthUserInfoUrl, token);
            request.getSession().setAttribute(FacebookAuthenticatorConstants.USERNAME,
                                              authenticatedUser);
            return AuthenticatorStatus.PASS;
        } catch (AuthenticatorException e) {
            LOGGER.error("Failed to process Facebook Connect response.", e);
        }
        return AuthenticatorStatus.FAIL;
    }

    private String getAuthorizationCode(HttpServletRequest request) throws AuthenticatorException {
        OAuthAuthzResponse authzResponse;
        try {
            authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            return authzResponse.getCode();
        } catch (OAuthProblemException e) {
            throw new AuthenticatorException("Exception while reading authorization code.", e);
        }
    }

    private String getToken(String tokenEndPoint, String clientId, String clientSecret,
                            String callbackurl, String code) throws AuthenticatorException {
        OAuthClientRequest tokenRequest = null;

        String token = null;

        try {
            tokenRequest =
                           buidTokenRequest(tokenEndPoint, clientId, clientSecret, callbackurl,
                                            code);

            token = sendRequest(tokenRequest.getLocationUri());
            if (token.startsWith("{")) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Received token: " + token + " for code: " + code);

                }
                throw new AuthenticatorException("Received access token is invalid.");
            }
        } catch (MalformedURLException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("URL : " + tokenRequest.getLocationUri());
            }
            throw new AuthenticatorException(
                                             "MalformedURLException while sending access token request.",
                                             e);

        } catch (IOException e) {
            throw new AuthenticatorException("IOException while sending access token request.", e);
        }
        return token;
    }

    private OAuthClientRequest buidTokenRequest(String tokenEndPoint, String clientId,
                                                String clientSecret, String callbackurl, String code)
                                                                                                     throws AuthenticatorException {
        OAuthClientRequest tokenRequest = null;
        try {
            tokenRequest =
                           OAuthClientRequest.tokenLocation(tokenEndPoint).setClientId(clientId)
                                             .setClientSecret(clientSecret)
                                             .setRedirectURI(callbackurl).setCode(code)
                                             .buildQueryMessage();
        } catch (OAuthSystemException e) {
            throw new AuthenticatorException("Exception while building access token request.", e);
        }
        return tokenRequest;
    }

    private String getUserInformation(String fbauthUserInfoUrl, String token)
                                                                             throws AuthenticatorException {

        String userInfoString = null;
        try {
            userInfoString = sendRequest(fbauthUserInfoUrl + "?" + token);
        } catch (MalformedURLException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("URL : " + fbauthUserInfoUrl + token, e);
            }
            throw new AuthenticatorException(
                                             "MalformedURLException while sending user information request.",
                                             e);
        } catch (IOException e) {
            throw new AuthenticatorException(
                                             "IOException while sending sending user information request.",
                                             e);
        }
        return userInfoString;
    }

    private String getUserName(String fbauthUserInfoUrl, String token)
                                                                      throws AuthenticatorException {
        String userName = null;
        String userInfoString = getUserInformation(fbauthUserInfoUrl, token);
        try {
            Map<String, Object> jsonObject = JSONUtils.parseJSON(userInfoString);
            userName = (String) jsonObject.get(FacebookAuthenticatorConstants.USERNAME);
        } catch (JSONException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("UserInfoString : " + userInfoString, e);
            }
            throw new AuthenticatorException("Exception while parsing User Information.", e);
        }
        return userName;
    }

    @Override
    public AuthenticatorStatus logout(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationContext context,
                                      AuthenticatorStateInfo stateInfo) {
        LOGGER.trace("Inside FacebookAuthenticator.logout()");
        return AuthenticatorStatus.PASS;
    }

    @Override
    public void sendInitialRequest(HttpServletRequest request, HttpServletResponse response,
                                   AuthenticationContext context) {

        LOGGER.trace("Inside FacebookAuthenticator.sendInitialRequest()");

        try {
            ExternalIdPConfig externalIdPConfig = context.getExternalIdP();
            String clientId = externalIdPConfig.getFbauthClientId();
            String authorizationEP = FacebookAuthenticatorConstants.FB_AUTHZ_URL;
            String scope = FacebookAuthenticatorConstants.SCOPE;

            String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
            callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");

            String state = context.getContextIdentifier();

            OAuthClientRequest authzRequest =
                                              OAuthClientRequest.authorizationLocation(authorizationEP)
                                                                .setClientId(clientId)
                                                                .setRedirectURI(callbackurl)
                                                                .setResponseType(FacebookAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE)
                                                                .setScope(scope).setState(state)
                                                                .buildQueryMessage();
            response.sendRedirect(authzRequest.getLocationUri());
        } catch (IOException e) {
            LOGGER.error("Exception while sending to the login page.", e);
        } catch (OAuthSystemException e) {
            LOGGER.error("Exception while building authorization code request.", e);
        }
        return;
    }

    @Override
    public String getAuthenticatedSubject(HttpServletRequest request) {
        LOGGER.trace("Inside FacebookAuthenticator.getAuthenticatedSubject()");
        String authenticatedUser =
                                   (String) request.getSession()
                                                   .getAttribute(FacebookAuthenticatorConstants.USERNAME);
        return authenticatedUser;
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        LOGGER.trace("Inside FacebookAuthenticator.getContextIdentifier()");
        return request.getParameter(FacebookAuthenticatorConstants.OAUTH2_PARAM_STATE);
    }

    @Override
    public String getAuthenticatorName() {
        LOGGER.trace("Inside FacebookAuthenticator.getAuthenticatorName()");
        return FacebookAuthenticatorConstants.AUTHENTICATOR_NAME;
    }

    @Override
    public Map<String, String> getResponseAttributes(HttpServletRequest request, AuthenticationContext context) {
        LOGGER.trace("Inside FacebookAuthenticator.getResponseAttributes()");
        return null;
    }

    private String sendRequest(String url) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        BufferedReader in =
                            new BufferedReader(
                                               new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder b = new StringBuilder();
        String inputLine = in.readLine();
        while (inputLine != null) {
            b.append(inputLine).append("\n");
            inputLine = in.readLine();
        }
        in.close();
        return b.toString();
    }

    @Override
    public AuthenticatorStateInfo getStateInfo(HttpServletRequest request) {
        return null;
    }
}
