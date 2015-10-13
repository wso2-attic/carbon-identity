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

    @Override
    protected String getCallBackURL(Map<String, String> authenticatorProperties) {
      return  authenticatorProperties.get(GoogleOAuth2AuthenticationConstant.CALLBACK_URL);
    }
    @Override
    protected String getQueryString(Map<String, String> authenticatorProperties) {
        return authenticatorProperties.get(GoogleOAuth2AuthenticationConstant.ADDITIONAL_QUERY_PARAMS);
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
     * @param context
     * @param jsonObject
     * @return
     */
    @Override
    protected String getAuthenticateUser(AuthenticationContext context, Map<String, Object> jsonObject) {
        if (jsonObject.get(OIDCAuthenticatorConstants.Claim.EMAIL) == null) {
            return (String) jsonObject.get("sub");
        } else {
            return (String) jsonObject.get(OIDCAuthenticatorConstants.Claim.EMAIL);
        }
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
            String json = sendRequest(IdentityApplicationConstants.GOOGLE_USERINFO_URL,
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

        Property scope = new Property();
        userInfoEndpoint.setDisplayName("Additional Query Parameters");
        userInfoEndpoint.setName("AdditionalQueryParameters");
        userInfoEndpoint.setValue("");
        userInfoEndpoint.setDescription("Additional query parameters. e.g: paramName1=value1");
        configProperties.add(scope);

        return configProperties;
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
