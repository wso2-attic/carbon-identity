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
 */

package org.wso2.carbon.identity.entitlement.filter;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.identity.entitlement.filter.exception.EntitlementCacheUpdateServletDataHolder;
import org.wso2.carbon.identity.entitlement.filter.exception.EntitlementCacheUpdateServletException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class EntitlementCacheUpdateServlet extends HttpServlet {

    private static final Log log = LogFactory.getLog(EntitlementCacheUpdateServlet.class);


    private static final String USERNAME_STRING = "username";
    private static final String PSWD_STRING = "password";
    private static final String NULL_STRING = "null";
    private static final String ADMIN = "AuthenticationAdmin";
    private static final String UPDATE_CACHE = "/updateCacheAuth.do";
    private static final String SUBJECT_SCOPE = "subjectScope";
    private static final String UPDATE_CACHE_HTML = "/updateCache.html";

    @Override
    public void init(ServletConfig config) throws EntitlementCacheUpdateServletException {

        EntitlementCacheUpdateServletDataHolder.getInstance().setServletConfig(config);
        try {
            EntitlementCacheUpdateServletDataHolder.getInstance().setConfigCtx(ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(null, null));
        } catch (AxisFault e) {
            log.error("Error while initializing Configuration Context", e);
            throw new EntitlementCacheUpdateServletException("Error while initializing Configuration Context", e);

        }

        EntitlementCacheUpdateServletDataHolder.getInstance().setHttpsPort(config.getInitParameter(EntitlementConstants.HTTPS_PORT));
        EntitlementCacheUpdateServletDataHolder.getInstance().setAuthentication(config.getInitParameter(EntitlementConstants.AUTHENTICATION));
        EntitlementCacheUpdateServletDataHolder.getInstance().setRemoteServiceUrl(config.getServletContext().getInitParameter(EntitlementConstants.REMOTE_SERVICE_URL));
        EntitlementCacheUpdateServletDataHolder.getInstance().setRemoteServiceUserName(config.getServletContext().getInitParameter(EntitlementConstants.USERNAME));
        EntitlementCacheUpdateServletDataHolder.getInstance().setRemoteServicePassword(config.getServletContext().getInitParameter(EntitlementConstants.PASSWORD));
        EntitlementCacheUpdateServletDataHolder.getInstance().setAuthenticationPage(config.getInitParameter(EntitlementConstants.AUTHENTICATION_PAGE));
        EntitlementCacheUpdateServletDataHolder.getInstance().setAuthenticationPageURL(config.getInitParameter(EntitlementConstants.AUTHENTICATION_PAGE_URL));


    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws EntitlementCacheUpdateServletException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws EntitlementCacheUpdateServletException {

        if (!req.isSecure()) {
            redirectToHTTPS(req, resp);
        } else if (req.getParameter(USERNAME_STRING) != null && req.getParameter(PSWD_STRING) != null
                && !NULL_STRING.equals(req.getParameter(USERNAME_STRING)) && !NULL_STRING.equals(req.getParameter(PSWD_STRING)
        )) {
            doAuthentication(req, resp);
        } else {
            if (req.getParameter(USERNAME_STRING) == null) {
                log.info("\'username\' parameter not available in request. Redirecting to " +
                        EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPageURL());
            }
            if (req.getParameter(PSWD_STRING) == null) {
                log.info("\'password\' parameter not available in request. Redirecting to " +
                        EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPageURL());
            }
            if (req.getParameter(USERNAME_STRING) != null && NULL_STRING.equals(req.getParameter(USERNAME_STRING))) {
                log.info("\'username\' is empty in request. Redirecting to " + EntitlementCacheUpdateServletDataHolder
                        .getInstance().getAuthenticationPageURL());
            }
            if (req.getParameter(PSWD_STRING) != null && NULL_STRING.equals(req.getParameter(PSWD_STRING))) {
                log.info("\'password\' is empty in request. Redirecting to " +
                        EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPageURL());
            }
            showAuthPage(req, resp);
        }
    }

    private boolean authenticate(String userName, String password, String remoteIp)
            throws EntitlementCacheUpdateServletException {

        boolean isAuthenticated = false;
        String authentication = EntitlementCacheUpdateServletDataHolder.getInstance().getAuthentication();
        String remoteServiceUrl = EntitlementCacheUpdateServletDataHolder.getInstance().getRemoteServiceUrl();
        ConfigurationContext configCtx = EntitlementCacheUpdateServletDataHolder.getInstance().getConfigCtx();
        String authCookie = EntitlementCacheUpdateServletDataHolder.getInstance().getAuthCookie();
        String remoteServiceUserName = EntitlementCacheUpdateServletDataHolder.getInstance().getRemoteServiceUserName();
        String remoteServicePassword = EntitlementCacheUpdateServletDataHolder.getInstance().getRemoteServicePassword();

        if (authentication.equals(EntitlementConstants.WSO2_IS)) {

            AuthenticationAdminStub authStub;
            String authenticationAdminServiceURL = remoteServiceUrl + ADMIN;
            try {
                authStub = new AuthenticationAdminStub(configCtx, authenticationAdminServiceURL);
                ServiceClient client = authStub._getServiceClient();
                Options options = client.getOptions();
                options.setManageSession(true);
                options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, authCookie);
                isAuthenticated = authStub.login(userName, password, remoteIp);
                EntitlementCacheUpdateServletDataHolder.getInstance().setAuthCookie((String) authStub._getServiceClient()
                        .getServiceContext()
                        .getProperty(HTTPConstants.COOKIE_STRING));
            } catch (LoginAuthenticationExceptionException e) {
                log.info(userName + " not authenticated to perform entitlement query to perform cache update");
                if (log.isDebugEnabled()) {
                    log.debug("Login Authentication Exception Occurred  ", e);
                }
            } catch (Exception e) {
                throw new EntitlementCacheUpdateServletException("Error while trying to authenticate" +
                        " with AuthenticationAdmin", e);
            }

        } else if (authentication.equals(EntitlementConstants.WEB_APP)) {

            if (userName.equals(remoteServiceUserName) && password.equals(remoteServicePassword)) {
                isAuthenticated = true;
            }

        } else {

            throw new EntitlementCacheUpdateServletException(authentication + " is an invalid"
                    + " configuration for authentication parameter in web.xml. Valid configurations are"
                    + " \'" + EntitlementConstants.WEB_APP + "\' and \'" + EntitlementConstants.WSO2_IS + "\'");

        }
        return isAuthenticated;
    }

    private String convertStreamToString(InputStream is) {
        try {
            return new Scanner(is).useDelimiter("\\A").next();
        } catch (NoSuchElementException e) {
            if (log.isDebugEnabled()) {
                log.debug("No such element: ", e);
            }
            return "";
        }
    }

    private void redirectToHTTPS(HttpServletRequest req, HttpServletResponse resp) throws EntitlementCacheUpdateServletException {
        String serverName = req.getServerName();
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();
        String redirectURL = "https://" + serverName + ":" + EntitlementCacheUpdateServletDataHolder.getInstance().getHttpsPort() +
                contextPath
                + servletPath;
        try {
            resp.sendRedirect(redirectURL);
        } catch (IOException e) {
            log.error("Error while redirecting request to come over HTTPS", e);
            throw new EntitlementCacheUpdateServletException("Error while redirecting request to come over HTTPS", e);
        }
    }

    private void doAuthentication(HttpServletRequest req, HttpServletResponse resp) throws EntitlementCacheUpdateServletException {
        String username = req.getParameter(USERNAME_STRING);
        String password = req.getParameter(PSWD_STRING);
        String remoteIp = req.getServerName();

        if (authenticate(username, password, remoteIp)) {

            RequestDispatcher requestDispatcher = req.getRequestDispatcher(UPDATE_CACHE);
            String subjectScope = EntitlementCacheUpdateServletDataHolder.getInstance().getServletConfig().getServletContext()
                    .getInitParameter(SUBJECT_SCOPE);
            String subjectAttributeName = EntitlementCacheUpdateServletDataHolder.getInstance().getServletConfig().getServletContext()
                    .getInitParameter("subjectAttributeName");

            if (subjectScope.equals(EntitlementConstants.REQUEST_PARAM)) {

                requestDispatcher = req.getRequestDispatcher(UPDATE_CACHE + "?" + subjectAttributeName + "=" + username);

            } else if (subjectScope.equals(EntitlementConstants.REQUEST_ATTIBUTE)) {

                req.setAttribute(subjectAttributeName, username);

            } else if (subjectScope.equals(EntitlementConstants.SESSION)) {

                req.getSession().setAttribute(subjectAttributeName, username);

            } else {

                resp.setHeader("Authorization", Base64Utils.encode((username + ":" + password).getBytes(Charset.forName("UTF-8"))));
            }

            try {
                requestDispatcher.forward(req, resp);
            } catch (Exception e) {
                log.error("Error occurred while dispatching request to /updateCacheAuth.do", e);
                throw new EntitlementCacheUpdateServletException("Error occurred while dispatching request to /updateCacheAuth.do", e);
            }

        } else {
            showAuthPage(req, resp);
        }
    }

    private void showAuthPage(HttpServletRequest req, HttpServletResponse resp) throws EntitlementCacheUpdateServletException {
        if ("default".equals(EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPage())) {

            InputStream is = getClass().getResourceAsStream(UPDATE_CACHE_HTML);
            String updateCache = convertStreamToString(is);
            try {
                resp.getWriter().print(updateCache);
            } catch (IOException e) {
                log.error("Error occurred while writing /updateCache.html page to OutputStream");
                throw new EntitlementCacheUpdateServletException("Error occurred while writing"
                        + " /updateCache.html page to OutputStream" + e);
            }
        } else if ("custom".equals(EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPage())) {

            try {
                req.getRequestDispatcher(EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPageURL()).forward(req, resp);
            } catch (Exception e) {
                log.error("Error occurred while dispatching request to " + EntitlementCacheUpdateServletDataHolder
                                .getInstance().getAuthenticationPageURL(),
                        e);
                throw new EntitlementCacheUpdateServletException("Error occurred while dispatching"
                        + " request to " + EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPageURL(), e);
            }

        } else {

            throw new EntitlementCacheUpdateServletException(EntitlementCacheUpdateServletDataHolder.getInstance().getAuthenticationPage()
                    + " is an " +
                    "invalid"
                    + " configuration for authenticationPage parameter in web.xml. Valid"
                    + " configurations are 'default' and 'custom'");

        }
    }

}
