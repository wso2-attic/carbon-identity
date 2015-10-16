/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.application.authenticator.social.live;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WindowsLiveOAuth2Authenticator extends OpenIDConnectAuthenticator {

    private static final long serialVersionUID = -4154255583070524018L;
    public static final String ACCOUNT = "account";
    private String tokenEndpoint;
    private String oAuthEndpoint;
    private String userInfoEndpoint;

    private static Map<String, String> claimMap;

    static {

        claimMap = new HashMap<String, String>();
        claimMap.put("first_name", WindowsLiveOAuth2AuthenticatorConstants.GIVEN_NAME_CLAIM_URI);
        claimMap.put("last_name", WindowsLiveOAuth2AuthenticatorConstants.LAST_NAME_CLAIM_URI);
        claimMap.put("gender", WindowsLiveOAuth2AuthenticatorConstants.GENDER_CLAIM_URI);
        claimMap.put("emails", WindowsLiveOAuth2AuthenticatorConstants.EMAIL_ADD_CLAIM_URI);
        claimMap.put("locale", WindowsLiveOAuth2AuthenticatorConstants.LOCALITY_CLAIM_URI);
    }

    private static Log log = LogFactory.getLog(WindowsLiveOAuth2Authenticator.class);

    /**
     * initiate tokenEndpoint reading from application-authentication.xml
     */
    private void initTokenEndpoint() {
        this.tokenEndpoint = getAuthenticatorConfig().getParameterMap().get(WindowsLiveOAuth2AuthenticatorConstants
                .WINDOWS_LIVE_TOKEN_URL);
        if (StringUtils.isBlank(this.tokenEndpoint)) {
            this.tokenEndpoint = IdentityApplicationConstants.WINDOWS_LIVE_TOKEN_URL;
        }
    }

    /**
     * initiate oAuthEndpoint reading from application-authentication.xml
     */
    private void initOAuthEndpoint() {
        this.oAuthEndpoint = getAuthenticatorConfig().getParameterMap().get(WindowsLiveOAuth2AuthenticatorConstants
                .WINDOWS_LIVE_AUTHZ_URL);
        if (StringUtils.isBlank(this.oAuthEndpoint)) {
            this.oAuthEndpoint = IdentityApplicationConstants.WINDOWS_LIVE_OAUTH_URL;
        }
    }

    /**
     * initiate userInfoEndpoint reading from application-authentication.xml
     */
    private void initUserInfoEndPoint() {
        this.userInfoEndpoint = getAuthenticatorConfig().getParameterMap().get
                (WindowsLiveOAuth2AuthenticatorConstants.WINDOWS_LIVE_USER_INFO_URL);
        if (StringUtils.isBlank(this.userInfoEndpoint)) {
            this.userInfoEndpoint = IdentityApplicationConstants.WINDOWS_LIVE_USERINFO_URL;
        }
    }

    /**
     *
     * @return userInfoEndpoint
     */
    private String getUserInfoEndpoint(Map<String, String> authenticatorProperties) {
        if (StringUtils.isBlank(this.userInfoEndpoint)) {
            initUserInfoEndPoint();
        }
        return this.userInfoEndpoint;
    }

    /**
     *
     * @return oAuthEndpoint
     */
    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {
        if (StringUtils.isBlank(this.oAuthEndpoint)) {
            initOAuthEndpoint();
        }
        return this.oAuthEndpoint;
    }

    /**
     * @return
     */
    @Override
    protected String getCallbackUrl(Map<String, String> authenticatorProperties) {
        return authenticatorProperties.get(WindowsLiveOAuth2AuthenticatorConstants.CALLBACK_URL);
    }

    /**
     * @return
     */
    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {
        if (StringUtils.isBlank(this.tokenEndpoint)) {
            initTokenEndpoint();
        }
        return this.tokenEndpoint;
    }

    /**
     * @param state
     * @return
     */
    @Override
    protected String getState(String state, Map<String, String> authenticatorProperties) {
        return state;
    }

    /**
     * @return
     */
    @Override
    protected String getScope(String scope, Map<String, String> authenticatorProperties) {
        return "wl.contacts_emails"; // bingads.manage
    }

    /**
     * @return
     */
    @Override
    protected boolean requiredIDToken(Map<String, String> authenticatorProperties) {
        return false;
    }

    /**
     * @param token
     * @return
     */
    @Override
    protected String getAuthenticateUser(OAuthClientResponse token) {
        return token.getParam(WindowsLiveOAuth2AuthenticatorConstants.USER_ID);
    }

    /**
     * @param token
     * @return
     */
    @Override
    protected Map<ClaimMapping, String> getSubjectAttributes(OAuthClientResponse token) {

        Map<ClaimMapping, String> claims = new HashMap<ClaimMapping, String>();

        try {
            String json = sendRequest(getUserInfoEndpoint(null) + token.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN));
            if (StringUtils.isNotBlank(json)) {
                Map<String, Object> jsonObject = JSONUtils.parseJSON(json);

                if (jsonObject != null) {
                    for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                        String key = getClaimUri(entry.getKey());
                        String value = entry.getValue().toString();

                        if (value != null && value.startsWith("{") && value.endsWith("}")) {
                            Map<String, Object> children = JSONUtils.parseJSON(value);
                            if (WindowsLiveOAuth2AuthenticatorConstants.EMAIL_ADD_CLAIM_URI.equals(key)) {
                                value = (String) children.get(ACCOUNT);
                            }
                        }

                        if (key != null && StringUtils.isNotBlank(value)) {
                            claims.put(ClaimMapping.build(key, key, null, false), value);
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Adding claim mapping : " + key + " <> " + key + " : " + value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error(e);
        }

        return claims;
    }

    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<Property>();

        Property callbackUrl = new Property();
        callbackUrl.setDisplayName("Callback Url");
        callbackUrl.setName(WindowsLiveOAuth2AuthenticatorConstants.CALLBACK_URL);
        callbackUrl.setRequired(true);
        callbackUrl.setDescription("Enter value corresponding to callback url.");
        configProperties.add(callbackUrl);

        Property clientId = new Property();
        clientId.setName(OIDCAuthenticatorConstants.CLIENT_ID);
        clientId.setDisplayName("Client Id");
        clientId.setRequired(true);
        clientId.setDescription("Enter Microsoft Live client identifier value");
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName(OIDCAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setDisplayName("Client Secret");
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Microsoft Live client secret value");
        configProperties.add(clientSecret);

        return configProperties;
    }

    @Override
    public String getFriendlyName() {
        return WindowsLiveOAuth2AuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
    }

    @Override
    public String getName() {
        return WindowsLiveOAuth2AuthenticatorConstants.AUTHENTICATOR_NAME;
    }

    /**
     * @param fbKey
     * @return
     */
    protected String getClaimUri(String fbKey) {
        return claimMap.get(fbKey);
    }

    private String sendRequest(String url) throws IOException {
        if (url != null) {
            URLConnection urlConnection = new URL(url).openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), Charset.forName("utf-8")));
            StringBuilder b = new StringBuilder();
            String inputLine = in.readLine();
            while (inputLine != null) {
                b.append(inputLine).append("\n");
                inputLine = in.readLine();
            }
            in.close();
            return b.toString();
        } else {
            return StringUtils.EMPTY;
        }
    }
}
