/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.iwa.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ietf.jgss.GSSException;
import org.wso2.carbon.identity.application.authenticator.iwa.*;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.PrivilegedActionException;

/**
 * This class handles the IWA login requests. The implementation is based on the NegotiateSecurityFilter class.
 */
public class IWAServelet extends HttpServlet {

    private transient Authenticator authenticator;
    private static Log log = LogFactory.getLog(IWAServelet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String commonAuthURL = IdentityUtil.getServerURL(IWAConstants.COMMON_AUTH_EP, false, true);
        String param = request.getParameter(IWAConstants.IWA_PARAM_STATE);
        if (param == null) {
            throw new IllegalArgumentException(IWAConstants.IWA_PARAM_STATE + " parameter is null.");
        }
        commonAuthURL += "?" + IWAConstants.IWA_PARAM_STATE + "=" + URLEncoder.encode(param, IWAConstants.UTF_8) +
                         "&" + IWAAuthenticator.IWA_PROCESSED + "=1";

        String header = request.getHeader(GSSConstants.AUTHORIZATION_HEADER);
        // authenticate user
        if (header != null) {
            // log the user in using the token
            String token = header.substring("Negotiate".length()+1);
            if (token.startsWith(GSSConstants.NTLM_PROLOG)){
                //todo prompt to type user name and password.
                //todo else make redirect to basic auth
                log.warn("NTLM token found ");
                sendUnauthorized(response, true);
                return;
            }
            final byte [] gss = Base64.decode(token);
            String name;
            try {

                name = authenticator.authnticateSp(gss);
            } catch (GSSException e) {
                log.warn("error logging in user.", e);
                sendUnauthorized(response, true);
                return;
            }
            if (name.equals(null)) {
                log.warn("error logging in user.");
                sendUnauthorized(response, true);
                return;
            }

            if (log.isDebugEnabled()) {
                log.debug("logged in user: " + name + ")");
            }
                HttpSession session = request.getSession(true);
                if (session == null) {
                    throw new ServletException("Expected HttpSession");
                }
            session.setAttribute(IWAConstants.SUBJECT_ATTRIBUTE, name);

            log.info("Successfully logged in user: " + name);

            response.sendRedirect(commonAuthURL);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("authorization required");
        }
        //added headers for nego auth
        sendUnauthorized(response, false);
    }

    /**
     * Send response as unauthorized
     *
     * @param response
     * @param close    whether to close the connection or to keep it alive
     */
    private void sendUnauthorized(HttpServletResponse response, boolean close) {
        try {
            //IWAServiceDataHolder.getInstance().getProviders().sendUnauthorized(response);
            if (close) {
                response.setHeader(GSSConstants.AUTHENTICATE_HEADER, GSSConstants.NEGOTIATE_HEADER);
                response.addHeader(IWAConstants.HTTP_CONNECTION_HEADER, IWAConstants.CONNECTION_CLOSE);
            } else {
                response.setHeader(GSSConstants.AUTHENTICATE_HEADER, GSSConstants.NEGOTIATE_HEADER);
                response.addHeader(IWAConstants.HTTP_CONNECTION_HEADER, IWAConstants.CONNECTION_KEEP_ALIVE);
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.flushBuffer();
        } catch (IOException e) {
            log.error("Error when sending unauthorized response." + e);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //todo remove these 2 files and check if it works
        //todo what if this file do  affect the normal steps
        ConfigurationSetup.setSystemProperties("login.conf", "krb5.conf");

        try {

            //todo get user names and password from user mgt file
            this.authenticator=new Authenticator("TomCat", "WSO2!@#");

        } catch (LoginException | PrivilegedActionException | GSSException e) {
            log.error("Error when creating gss credentials ." + e);
            e.printStackTrace();
        }

    }

}
