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
package org.wso2.carbon.identity.authenticator.iwa.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.rampart.RampartMessageData;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;
import org.wso2.carbon.identity.authenticator.iwa.stub.client.IWAAuthenticatorStub;
import org.wso2.carbon.identity.base.IdentityBaseUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.ui.DefaultCarbonAuthenticator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class IWAUIAuthenticator extends DefaultCarbonAuthenticator {

    public static final String NEGOTIATE = "Negotiate";
    protected static final Log log = LogFactory.getLog(IWAUIAuthenticator.class);
    private static final int DEFAULT_PRIORITY_LEVEL = 10;
    private static final String AUTHENTICATOR_NAME = "IWAUIAuthenticator";

    /**
     * {@inheritDoc}
     */
    public boolean canHandle(HttpServletRequest request) {
        if (NEGOTIATE.equalsIgnoreCase(request.getAuthType()) && request.getRemoteUser() != null) {
            if (log.isDebugEnabled()) {
                log.debug("IWA request received for url: "
                        + request.getRequestURL());
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration
                .getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = authenticatorsConfiguration
                .getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null && authenticatorConfig.getPriority() > 0) {
            return authenticatorConfig.getPriority();
        }
        return DEFAULT_PRIORITY_LEVEL;
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

    /**
     * {@inheritDoc}
     */
    public void authenticate(HttpServletRequest request) throws AuthenticationException {
        String userName = request.getRemoteUser();
        userName = userName.substring(userName.indexOf("\\") + 1);

        if (log.isDebugEnabled()) {
            log.debug("Authenticate request received : Authtype - " + request.getAuthType() +
                      ", User - " + userName);
        }

        ServletContext servletContext = request.getSession().getServletContext();
        HttpSession session = request.getSession();
        String backendServerURL = request.getParameter("backendURL");
        if (backendServerURL == null) {
            backendServerURL = CarbonUIUtil.getServerURL(servletContext, request.getSession());
        }

        session.setAttribute(CarbonConstants.SERVER_URL, backendServerURL);
        String rememberMe = request.getParameter("rememberMe");

        handleSecurity(userName, (rememberMe != null), request);
        request.setAttribute("username", userName);
    }

    /**
     * {@inheritDoc}
     */
    public String doAuthentication(Object credentials, boolean isRememberMe, ServiceClient client,
            HttpServletRequest request) throws AuthenticationException {
        try {

            String user = (String) credentials;

            if (user == null) {
                throw new AuthenticationException("Invalid Credentials.");
            }

            ServletContext servletContext = request.getSession().getServletContext();
            ConfigurationContext configContext = (ConfigurationContext) servletContext
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

            if (configContext == null) {
                log.error("Configuration context is null.");
            }

            HttpSession session = request.getSession();
            String backendServerURL = request.getParameter("backendURL");
            if (backendServerURL == null) {
                backendServerURL = CarbonUIUtil.getServerURL(servletContext, request.getSession());
            }

            // Back-end server URL is stored in the session, even if it is an incorrect one. This
            // value will be displayed in the server URL text box. Usability improvement.
            session.setAttribute(CarbonConstants.SERVER_URL, backendServerURL);

            if (getIWAClient(request).login(user, request.getRemoteAddr())) {
                setAdminCookie(session, client, null);
            }

            return user;

        } catch (Exception e) {
            throw new AuthenticationException(
                    "System error occured while trying to authenticate the user", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDisabled() {
        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration
                .getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig = authenticatorsConfiguration
                .getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null) {
            return authenticatorConfig.isDisabled();
        }
        // By default we keep this disabled.
        return true;
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isAdminCookieSet() {
        return true;
    }

    /**
     * IWA does not need to login page for authentication. It will use the Windows logged in users
     * credentials.
     */
    public boolean skipLoginPage() {
        return true;
    }

    /**
     * 
     * @param request
     * @return
     * @throws AxisFault
     */
    private IWAAuthenticatorStub getIWAClient(HttpServletRequest request)
            throws AxisFault, IdentityException {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        String backendServerURL = request.getParameter("backendURL");
        if (backendServerURL == null) {
            backendServerURL = CarbonUIUtil.getServerURL(servletContext, request.getSession());
        }

        ConfigurationContext configContext = (ConfigurationContext) servletContext
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

        String serviceEPR = backendServerURL + "IWAAuthenticator";
        IWAAuthenticatorStub stub = new IWAAuthenticatorStub(configContext, serviceEPR);
        ServiceClient client = stub._getServiceClient();
        client.engageModule("rampart");
        Policy rampartConfig = IdentityBaseUtil.getDefaultRampartConfig();
        Policy signOnly = IdentityBaseUtil.getSignOnlyPolicy();
        Policy mergedPolicy = signOnly.merge(rampartConfig);
        Options options = client.getOptions();
        options.setProperty(RampartMessageData.KEY_RAMPART_POLICY, mergedPolicy);
        options.setManageSession(true);
        return stub;
    }
}
