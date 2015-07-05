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

import org.apache.amber.oauth2.client.response.OAuthClientResponse;
import org.apache.amber.oauth2.common.utils.JSONUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;

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

    private static Map<String, String> claimMap;

    static {

        claimMap = new HashMap<String, String>();
        claimMap.put("first_name", "http://wso2.org/claims/givenname");
        claimMap.put("last_name", "http://wso2.org/claims/lastname");
        claimMap.put("gender", "http://wso2.org/claims/gender");
        claimMap.put("emails", "http://wso2.org/claims/emailaddress");
        claimMap.put("locale", "http://wso2.org/claims/locality");
    }

    private static Log log = LogFactory.getLog(WindowsLiveOAuth2Authenticator.class);

    /**
     * @return
     */
    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {
        return "https://login.live.com/oauth20_authorize.srf";
    }

    /**
     * @return
     */
    @Override
    protected String getCallbackUrl(Map<String, String> authenticatorProperties) {
        return authenticatorProperties.get("windows-live-callback-url");
    }

    /**
     * @return
     */
    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {
        return "https://login.live.com/oauth20_token.srf";
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
        return token.getParam("user_id");
    }

    /**
     * @param token
     * @return
     */
    @Override
    protected Map<ClaimMapping, String> getSubjectAttributes(OAuthClientResponse token) {

        Map<ClaimMapping, String> claims = new HashMap<ClaimMapping, String>();

        try {
            String json = sendRequest("https://apis.live.net/v5.0/me?access_token="
                    + token.getParam(OIDCAuthenticatorConstants.ACCESS_TOKEN));

            Map<String, Object> jsonObject = JSONUtils.parseJSON(json);

            if (jsonObject != null) {
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    String key = getClaimUri(entry.getKey());
                    String value = entry.getValue().toString();

                    if (value != null && value.startsWith("{") && value.endsWith("}")) {
                        Map<String, Object> children = JSONUtils.parseJSON(value);
                        if ("http://wso2.org/claims/emailaddress".equals(key)) {
                            value = (String) children.get("account");
                        }
                    }

                    if (key != null && value != null && value.trim().length() > 0) {
                        claims.put(ClaimMapping.build(key, key, null, false), value);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Adding claim mapping : " + key + " <> " + key + " : " + value);
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
        callbackUrl.setName("windows-live-callback-url");
        callbackUrl.setRequired(true);
        callbackUrl.setDescription("Enter value corresponding to callback url.");

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

        configProperties.add(callbackUrl);

        return configProperties;
    }

    @Override
    public String getFriendlyName() {
        return "microsoft(hotmail, msn, live)";
    }

    @Override
    public String getName() {
        return "MicrosoftWindowsLive";
    }

    /**
     * @param fbKey
     * @return
     */
    protected String getClaimUri(String fbKey) {
        return claimMap.get(fbKey);
    }

    private String sendRequest(String url) throws IOException {
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
    }
}
