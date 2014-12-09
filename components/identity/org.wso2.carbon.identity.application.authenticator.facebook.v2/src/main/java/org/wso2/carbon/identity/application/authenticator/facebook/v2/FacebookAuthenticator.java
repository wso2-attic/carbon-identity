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

package org.wso2.carbon.identity.application.authenticator.facebook.v2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Object;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.client.response.OAuthAuthzResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.utils.JSONUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.ui.CarbonUIUtil;

public class FacebookAuthenticator extends AbstractApplicationAuthenticator implements
                                                                           FederatedApplicationAuthenticator {

    private static final long serialVersionUID = 1L;
    private static final Log LOGGER = LogFactory.getLog(FacebookAuthenticator.class);

    @Override
    public boolean canHandle(HttpServletRequest request) {

        LOGGER.trace("Inside FacebookAuthenticator.canHandle()");

        // Check commonauth got an OIDC response
        if (request.getParameter(FacebookAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE) != null &&
            request.getParameter(FacebookAuthenticatorConstants.OAUTH2_PARAM_STATE) != null  &&
              FacebookAuthenticatorConstants.FACEBOOK_LOGIN_TYPE.equals(getLoginType(request))) {
            return true;
        }

        return false;
    }
    
    @Override
	protected void initiateAuthenticationRequest(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException {
		
		try {
            Map<String,String> authenticatorProperties = context.getAuthenticatorProperties();
            String clientId = authenticatorProperties.get(FacebookAuthenticatorConstants.CLIENT_ID);
            String authorizationEP = FacebookAuthenticatorConstants.FB_AUTHZ_URL;
            String scope = FacebookAuthenticatorConstants.SCOPE;

            String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
            callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");

            String state = context.getContextIdentifier() + "," + FacebookAuthenticatorConstants.FACEBOOK_LOGIN_TYPE;

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
            throw new AuthenticationFailedException(e.getMessage(), e);
        } catch (OAuthSystemException e) {
            LOGGER.error("Exception while building authorization code request.", e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
        return;
	}

    private String getClientID(Map<String, String> authenticatorProperties, String clientId) {
        return authenticatorProperties.get(clientId);
    }

    @Override
	protected void processAuthenticationResponse(HttpServletRequest request,
			HttpServletResponse response, AuthenticationContext context)
			throws AuthenticationFailedException {
		
		 LOGGER.trace("Inside FacebookAuthenticator.authenticate()");
		
		try {
            Map<String,String> authenticatorProperties = context.getAuthenticatorProperties();
		    String clientId =  authenticatorProperties.get(FacebookAuthenticatorConstants.CLIENT_ID);
		    String clientSecret = authenticatorProperties.get(FacebookAuthenticatorConstants.CLIENT_SECRET);
            String userIdentifierField = authenticatorProperties.get(FacebookAuthenticatorConstants.USER_IDENTIFIER_FIELD);

		    String tokenEndPoint = FacebookAuthenticatorConstants.FB_TOKEN_URL;
		    String fbauthUserInfoUrl = FacebookAuthenticatorConstants.FB_USER_INFO_URL;
		
		    String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
		    callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");
		
		    String code = getAuthorizationCode(request);
		    String token = getToken(tokenEndPoint, clientId, clientSecret, callbackurl, code);

            if(StringUtils.isEmpty(userIdentifierField)) {
                throw new AuthenticatorException("User identifier field is not found in Facebook authenticator " +
                        "configuration");
            }
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Using user identifier field: %s", userIdentifierField));
            }

            Map<String,Object> userIdJson = getUserIdJson(fbauthUserInfoUrl, token, userIdentifierField);
            setSubject(context, userIdJson, userIdentifierField);

		    Map<String,Object> userInfoJson = getUserInfoJson(fbauthUserInfoUrl, token);
            buildClaims(context,userInfoJson);
		} catch (AuthenticatorException e) {
		    LOGGER.error("Failed to process Facebook Connect response.", e);
		    throw new AuthenticationFailedException(e.getMessage(), e);
		}
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
                LOGGER.debug("URL: " + tokenRequest.getLocationUri());
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

    private String getUserInfoString(String fbauthUserInfoUrl, String token)
            throws AuthenticatorException {
         return getUserInfoString(fbauthUserInfoUrl, token, null);
    }

    private String getUserInfoString(String fbauthUserInfoUrl, String token, String fields)
                                                                             throws AuthenticatorException {

        String userInfoString = null;
        try {
            if(StringUtils.isEmpty(fields)) {
                userInfoString = sendRequest(String.format("%s?%s",fbauthUserInfoUrl, token));
            }
            else {
                userInfoString = sendRequest(String.format("%s?fields=%s&%s",fbauthUserInfoUrl, fields, token));
            }
        } catch (MalformedURLException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("URL: " + fbauthUserInfoUrl + token, e);
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

    private Map<String,Object> getUserIdJson(String fbAuthUserInfoUrl, String token, String userIdentifierField)
            throws AuthenticatorException {
        Map<String, Object> jsonObject;
        String userIdString = getUserInfoString(fbAuthUserInfoUrl, token, userIdentifierField);
        try {
            jsonObject = JSONUtils.parseJSON(userIdString);
        } catch (JSONException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("userIdString: " + userIdString, e);
            }
            throw new AuthenticatorException("Could not parse user information", e);
        }
        return jsonObject;
    }

    private void setSubject(AuthenticationContext context, Map<String,Object> jsonObject, String userIdentifierField) throws AuthenticatorException {
        String authenticatedUserId = (String) jsonObject.get(userIdentifierField);
        if(StringUtils.isEmpty(authenticatedUserId)) {
            throw new AuthenticatorException("Authenticated user identifier is empty");
        }
        context.setSubject(authenticatedUserId);
    }

    private Map<String,Object> getUserInfoJson(String fbauthUserInfoUrl, String token)
                                                                      throws AuthenticatorException {
        Map<String, Object> jsonObject;
        String userInfoString = getUserInfoString(fbauthUserInfoUrl, token);
        try {
            jsonObject = JSONUtils.parseJSON(userInfoString);
        } catch (JSONException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("UserInfoString: " + userInfoString, e);
            }
            throw new AuthenticatorException("Could not parse user information.", e);
        }
        return jsonObject;
    }

    public void buildClaims(AuthenticationContext context, Map<String, Object> jsonObject) throws AuthenticatorException{
        if(jsonObject != null) {
            Map<ClaimMapping, String> claims = new HashMap<ClaimMapping, String>();

            for(Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                claims.put(ClaimMapping.build(entry.getKey(), entry.getKey(), null,
                        false), entry.getValue().toString());
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Adding claim mapping: " + entry.getKey() + " <> " + entry.getKey() + " : " + entry.getValue());
                }

            }
            context.setSubjectAttributes(claims);
        } else {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Decoded json object is null");
            }
            throw new AuthenticatorException("Decoded json object is null");
        }
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        LOGGER.trace("Inside FacebookAuthenticator.getContextIdentifier()");
        String state = request.getParameter(FacebookAuthenticatorConstants.OAUTH2_PARAM_STATE);
        if (state != null) {
            return state.split(",")[0];
        } else {
            return null;
        }
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

    private String getLoginType(HttpServletRequest request) {
        String state = request.getParameter(FacebookAuthenticatorConstants.OAUTH2_PARAM_STATE);
        if (state != null) {
            return state.split(",")[1];
        } else {
            return null;
        }
    }

	@Override
	public String getFriendlyName() {
		//return "facebook";
	    return "facebook-v2";
	}

	@Override
	public String getName() {
		//return FacebookAuthenticatorConstants.AUTHENTICATOR_NAME;
		return "FacebookAuthenticator-v2";
	}
	
	@Override
    public List<Property> getConfigurationProperties() {
        List configProperties = new ArrayList();

        Property clientId = new Property();
        clientId.setName("ClientId");
        clientId.setDisplayName("Client Id");
        clientId.setRequired(true);
        clientId.setDescription("Enter Facebook client identifier value");
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName("ClientSecret");
        clientSecret.setDisplayName("Client Secret");
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Facebook client secret value");
        configProperties.add(clientSecret);

        Property userIdentifier = new Property();
        userIdentifier.setName(FacebookAuthenticatorConstants.USER_IDENTIFIER_FIELD);
        userIdentifier.setDisplayName("User Identifier Field");
        userIdentifier.setDescription("Enter Facebook user identifier field");
        userIdentifier.setDefaultValue("id");
        userIdentifier.setRequired(true);
        configProperties.add(userIdentifier);

        return configProperties;
    }
}
