/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 *
 */

package org.wso2.carbon.identity.sso.agent.oauth2;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.xml.util.Base64;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentSessionBean;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class SAML2GrantAccessTokenRequestor {

    private static final Log log = LogFactory.getLog(SAML2GrantAccessTokenRequestor.class);
    public static final String SAML2_BEARER_ASSERTION = "grant_type=urn:ietf:params:oauth:grant-type:saml2-bearer&assertion=";

    private SAML2GrantAccessTokenRequestor() {
    }

    public static void getAccessToken(HttpServletRequest request) throws SSOAgentException {


        String samlAssertionString = ((SSOAgentSessionBean) request.getSession().getAttribute(
                SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean().getSAMLAssertionString();

        try {

            String consumerKey = SSOAgentConfigs.getOAuth2ClientId();
            String consumerSecret = SSOAgentConfigs.getOAuth2ClientSecret();
            String tokenEndpoint = SSOAgentConfigs.getTokenEndpoint();
            String keySecret = consumerKey+":"+consumerSecret;

            String accessTokenResponse = executePost(tokenEndpoint,
                    SAML2_BEARER_ASSERTION + URLEncoder.encode(Base64
                            .encodeBytes(samlAssertionString.getBytes(Charset.forName("UTF-8"))).replaceAll("\n", "")),
                    Base64.encodeBytes(keySecret.getBytes(Charset.forName
                            ("UTF-8")))
                    .replace("\n",
                            ""));

            Gson gson = new Gson();
            SSOAgentSessionBean.AccessTokenResponseBean accessTokenResp =
                    gson.fromJson(accessTokenResponse, SSOAgentSessionBean.AccessTokenResponseBean.class);

            ((SSOAgentSessionBean) request.getSession().getAttribute(
                    SSOAgentConfigs.getSessionBeanName())).getSAMLSSOSessionBean()
                    .setAccessTokenResponseBean(accessTokenResp);

        } catch (Exception e) {
            throw new SSOAgentException("Error while retrieving OAuth2 access token using SAML2 grant type", e);
        }
    }

    public static String executePost(String targetURL, String urlParameters, String clientCredentials) throws SSOAgentException {

        URL url;
        HttpURLConnection connection = null;
        try {

            //Create connection
            url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization", "Basic " + clientCredentials);

            connection.setRequestProperty("Content-Length", "" +
                    Integer.toString(urlParameters.getBytes(Charset.forName("UTF-8")).length));

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();

        } catch (Exception e) {

            throw new SSOAgentException("Exception while executiong post", e);
        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
