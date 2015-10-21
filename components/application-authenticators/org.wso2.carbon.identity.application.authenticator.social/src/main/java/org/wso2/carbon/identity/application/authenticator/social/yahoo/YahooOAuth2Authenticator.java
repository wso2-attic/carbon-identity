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

import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OAuth 2.0 Authenticator for Yahoo.
 */
public class YahooOAuth2Authenticator extends OpenIDConnectAuthenticator {

    private static final long serialVersionUID = -4290245763061524219L;

    private String oAuthEndpoint;
    private String tokenEndpoint;
    private String userInfoURL;

    /**
     * Initialize the Yahoo OAuth endpoint url.
     */
    private void initOAuthEndpoint() {

        oAuthEndpoint = getAuthenticatorConfig()
                .getParameterMap()
                .get(YahooOAuth2AuthenticatorConstants.YAHOO_OAUTHZ_ENDPOINT);

        if(oAuthEndpoint == null) {
            oAuthEndpoint = IdentityApplicationConstants.YAHOO_OAUTH2_URL;
        }
    }

    /**
     * Initialize the Yahoo token endpoint url.
     */
    private void initTokenEndpoint() {

        tokenEndpoint =  getAuthenticatorConfig()
                .getParameterMap()
                .get(YahooOAuth2AuthenticatorConstants.YAHOO_TOKEN_ENDPOINT);

        if(tokenEndpoint == null) {
            tokenEndpoint = IdentityApplicationConstants.YAHOO_TOKEN_URL;
        }
    }

    /**
     * Initialize the Yahoo user info url.
     */
    private void initUserInfoURL() {

        userInfoURL = getAuthenticatorConfig()
                .getParameterMap()
                .get(YahooOAuth2AuthenticatorConstants.YAHOO_USERINFO_ENDPOINT);

        if(userInfoURL == null) {
            userInfoURL = IdentityApplicationConstants.YAHOO_USERINFO_URL;
        }
    }

    /**
     * Get authorization server endpoint.
     * @param authenticatorProperties Authenticator properties.
     * @return OAuth2 Endpoint
     */
    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {

        if(oAuthEndpoint == null) {
            initOAuthEndpoint();
        }

        return oAuthEndpoint;
    }

    /**
     * Get token access endpoint.
     * @param authenticatorProperties Authenticator properties.
     * @return
     */
    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {

        if(tokenEndpoint == null) {
            initTokenEndpoint();
        }

        return tokenEndpoint;
    }

    /**
     * Get the user info endpoint url.
     * @return User info endpoint url.
     */
    private String getUserInfoURL() {

        if(userInfoURL == null) {
            initUserInfoURL();
        }

        return userInfoURL;
    }

    /**
     * Get OAuth2 Scope
     * @param scope Scope
     * @param authenticatorProperties Authentication properties.
     * @return OAuth2 Scope
     */
    @Override
    protected String getScope(String scope, Map<String, String> authenticatorProperties) {

        return YahooOAuth2AuthenticatorConstants.YAHOO_SCOPE;
    }

    /**
     * Get Authenticated User
     * @param token OAuth client response.
     * @return GUID of the authenticated user.
     */
    @Override
    protected String getAuthenticateUser(AuthenticationContext context, Map<String, Object> jsonObject, OAuthClientResponse token) {

        return token.getParam(YahooOAuth2AuthenticatorConstants.USER_GUID);
    }

    /**
     * Get the user info endpoint.
     * @param token OAuth client response.
     * @return User info endpoint.
     */
    @Override
    protected String getUserInfoEndpoint(OAuthClientResponse token, Map<String, String> authenticatorProperties) {

        String userGUID = token.getParam(YahooOAuth2AuthenticatorConstants.USER_GUID);
        String url = getUserInfoURL()
                + userGUID
                + YahooOAuth2AuthenticatorConstants.YAHOO_USER_DETAILS_JSON;

        return url;
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
     * Always return false as there is no ID token in Yahoo OAuth.
     * @param authenticatorProperties Authenticator properties.
     * @return False
     */
    @Override
    protected boolean requiredIDToken(Map<String, String> authenticatorProperties) {
        return false;
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
        callbackUrl.setName(IdentityApplicationConstants.OAuth2.CALLBACK_URL);
        callbackUrl.setDescription("Enter value corresponding to callback url.");
        configProperties.add(callbackUrl);

        return configProperties;
    }
}