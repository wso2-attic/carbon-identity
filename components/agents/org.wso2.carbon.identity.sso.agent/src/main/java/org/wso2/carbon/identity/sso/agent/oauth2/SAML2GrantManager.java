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
import org.opensaml.xml.util.Base64;
import org.wso2.carbon.identity.sso.agent.SSOAgentConstants;
import org.wso2.carbon.identity.sso.agent.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.bean.LoggedInSessionBean;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentConfig;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class SAML2GrantManager {

    private SSOAgentConfig ssoAgentConfig = null;

    public SAML2GrantManager(SSOAgentConfig ssoAgentConfig) {
        this.ssoAgentConfig = ssoAgentConfig;
    }

    public void getAccessToken(HttpServletRequest request, HttpServletResponse response)
            throws SSOAgentException {


        String samlAssertionString = ((LoggedInSessionBean) request.getSession(false).
                getAttribute(SSOAgentConstants.SESSION_BEAN_NAME)).getSAML2SSO().
                getAssertionString();

        String clientLogin = ssoAgentConfig.getOAuth2().getClientId() + ":" +
                ssoAgentConfig.getOAuth2().getClientSecret();
        String queryParam = "grant_type=" + SSOAgentConstants.OAuth2.SAML2_BEARER_GRANT_TYPE + "&assertion=" +
                            URLEncoder.encode(Base64.encodeBytes(
                                    samlAssertionString.getBytes(Charset.forName("UTF-8"))).replaceAll("\n", ""));
        String additionalQueryParam = ssoAgentConfig.getRequestQueryParameters();
        if (additionalQueryParam != null) {
            queryParam = queryParam + additionalQueryParam;
        }
        String accessTokenResponse = executePost(queryParam,
                                                 Base64.encodeBytes(clientLogin.getBytes(Charset.forName("UTF-8")))
                                                       .replace("\n", ""));

        Gson gson = new Gson();
        LoggedInSessionBean.AccessTokenResponseBean accessTokenResp =
                gson.fromJson(accessTokenResponse, LoggedInSessionBean.AccessTokenResponseBean.class);

        ((LoggedInSessionBean) request.getSession(false).getAttribute(
                SSOAgentConstants.SESSION_BEAN_NAME)).getSAML2SSO()
                .setAccessTokenResponseBean(accessTokenResp);
    }

    protected String executePost(String urlParameters, String basicAuthHeader)
            throws SSOAgentException {

        URL url;
        HttpURLConnection connection = null;
        try {

            //Create connection
            url = new URL(ssoAgentConfig.getOAuth2().getTokenURL());
            if(ssoAgentConfig.getEnableSSLVerification()){
                connection = (HttpsURLConnection) url.openConnection();
            } else{
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization", "Basic " + basicAuthHeader);

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

        } catch (IOException e) {
            throw new SSOAgentException(
                    "Error occurred while executing SAML2 grant request to OAuth2 Token URL", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
