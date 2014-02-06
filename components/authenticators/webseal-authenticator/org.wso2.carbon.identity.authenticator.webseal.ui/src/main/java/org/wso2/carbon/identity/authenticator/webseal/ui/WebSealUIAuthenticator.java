/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.authenticator.webseal.ui;

import org.apache.axiom.om.util.Base64;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;
import org.wso2.carbon.identity.authenticator.webseal.ui.client.WebSealAuthenticatorClient;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.ui.DefaultCarbonAuthenticator;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.URLDecoder;

/**
 * Webseal UI authenticator.
 */
public class WebSealUIAuthenticator extends DefaultCarbonAuthenticator {

    public static final String WEBSEAL_USER = "iv-user";
    protected static final Log log = LogFactory.getLog(WebSealUIAuthenticator.class);
    private static final int DEFAULT_PRIORITY_LEVEL = 10;
    public static final String AUTHENTICATOR_NAME = "WebSealUIAuthenticator";


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(HttpServletRequest request) {

        String ivuser = request.getHeader(WEBSEAL_USER);


        if(log.isDebugEnabled()){
            if(ivuser == null){
                log.debug("iv-user header is null. WebSealUIAuthenticator can not process this request");
            } else {
                log.debug("iv-user header is : " + ivuser);
            }
        }
        return ivuser != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null && authenticatorConfig.getPriority() > 0) {
            return authenticatorConfig.getPriority();
        }
        return DEFAULT_PRIORITY_LEVEL;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authenticate(HttpServletRequest request) throws AuthenticationException {

        String credentials = request.getHeader("Authorization");
        String userName = request.getHeader(WEBSEAL_USER);

        if (credentials != null) {
            credentials = credentials.trim();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Authorization header is empty");
            }
            throw new AuthenticationException("Autherization header is empty");
        }
        String rememberMe = request.getParameter("rememberMe");

        handleSecurity(credentials, rememberMe != null ,request);
        request.setAttribute("username", userName);
    }

    @Override
    public String doAuthentication(Object credentialsObj, boolean isRememberMe, ServiceClient serviceClient,
                                   HttpServletRequest request) throws AuthenticationException {
        String websealUser = null;
        String password = null;
        String credentials = (String)credentialsObj;

        try {
            if (credentials != null && credentials.startsWith("Basic ")) {
                credentials = new String(Base64.decode(credentials.substring(6)));
                int i = credentials.indexOf(':');
                if (i == -1) {
                    websealUser = credentials;
                } else {
                    websealUser = credentials.substring(0, i);
                }

                if (i != -1) {
                    password = credentials.substring(i + 1);
                    if (password != null && password.trim().equals("")) {
                        password = null;
                    }
                }
            }

            String userName = request.getHeader(WEBSEAL_USER);

            if (websealUser == null || password == null) {
                if (log.isDebugEnabled()) {
                    if (websealUser == null) {
                        log.debug("No valid webseal user name provided");
                    } else {
                        log.debug("WebSeal user name is : " + websealUser);
                    }
                    if (password == null) {
                        log.debug("No valid webseal user password provided");
                    }
                    if (userName == null) {
                        log.debug("No valid webseal authenticated user name provided");
                    }
                }
                throw new AuthenticationException("Invalid credentials");
            }

            if (userName != null) {
                 userName = URLDecoder.decode(userName, "UTF-8");
            }

            ServletContext servletContext = request.getSession().getServletContext();
            ConfigurationContext configContext = (ConfigurationContext) servletContext
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

            if (configContext == null) {
                log.error("Configuration context is null.");
            }
            // Obtain the back-end server URL from the request. If not obtain it
            // from the http
            // session and then from the ServletContext.
            HttpSession session = request.getSession();
            String backendServerURL = request.getParameter("backendURL");
            if (backendServerURL == null) {
                backendServerURL = CarbonUIUtil.getServerURL(servletContext, request.getSession());
            }

            // Back-end server URL is stored in the session, even if it is an
            // incorrect one. This
            // value will be displayed in the server URL text box. Usability
            // improvement.
            session.setAttribute(CarbonConstants.SERVER_URL, backendServerURL);
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN);            

            WebSealAuthenticatorClient client = new WebSealAuthenticatorClient(configContext,
                    backendServerURL,cookie, session);
            if(client.login(websealUser, password, userName)){
                return userName;
            }else{
                throw new AuthenticationException("Cannot login user "+userName);
            }
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error when sign-in for the user : " + websealUser, e);
            throw new AuthenticationException("Error when sign-in for the user : " + websealUser, e);
        }
    }

    @Override
    public boolean isDisabled() {
        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null) {
            return authenticatorConfig.isDisabled();
        }
        return true;
    }

   @Override
    public void unauthenticate(Object o) throws Exception {

        HttpServletRequest request = (HttpServletRequest) o;
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ConfigurationContext configContext = (ConfigurationContext) servletContext
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

        String backendServerURL = CarbonUIUtil.getServerURL(servletContext, session);
        try {
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN);
            WebSealAuthenticatorClient client = new WebSealAuthenticatorClient(configContext,
                                                                backendServerURL, cookie, session);
            client.logout(session);
        } catch (AuthenticationException e) {
            String msg = "Error occurred while logging out";
            log.error(msg, e);
            throw new Exception(msg, e);
        }    
    }
}
