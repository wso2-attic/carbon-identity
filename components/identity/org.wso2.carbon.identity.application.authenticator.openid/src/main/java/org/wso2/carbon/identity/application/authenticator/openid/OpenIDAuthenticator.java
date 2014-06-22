package org.wso2.carbon.identity.application.authenticator.openid;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.openid.exception.OpenIDException;
import org.wso2.carbon.identity.application.authenticator.openid.manager.DefaultOpenIDManager;
import org.wso2.carbon.identity.application.authenticator.openid.manager.OpenIDManager;
import org.wso2.carbon.identity.application.authenticator.openid.util.OpenIDConstants;

public class OpenIDAuthenticator extends AbstractApplicationAuthenticator implements
        FederatedApplicationAuthenticator {

    private static final long serialVersionUID = 2878862656196592256L;

    private static Log log = LogFactory.getLog(OpenIDAuthenticator.class);

    private static final String OPENID_MANAGER = "OpenIDManager";

    @Override
    public boolean canHandle(HttpServletRequest request) {

        if (log.isTraceEnabled()) {
            log.trace("Inside canHandle()");
        }

        String opeidMode = request.getParameter("openid.mode");
        if (opeidMode != null && !"checkid_immediate".equals(opeidMode)
                && !"checkid_setup".equals(opeidMode) && !"check_authentication".equals(opeidMode)) {
            return true;
        }

        return false;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
            HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        OpenIDManager manager = getNewOpenIDManagerInstance();

        if (context.getExternalIdP() != null || request.getParameter("claimed_id") != null) {
            try {
                
                Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();

                if (getOpenIDServerUrl() != null) {
                    // this is useful in case someone wants to overrode the default OpenID
                    // authenticator.
                    authenticatorProperties.put(OpenIDConstants.OPEN_ID_URL, getOpenIDServerUrl());
                }

                String loginPage = manager.doOpenIDLogin(request, response, context);
                String domain = request.getParameter("domain");

                if (domain != null) {
                    loginPage = loginPage + "&fidp=" + domain;
                }


                if (authenticatorProperties != null) {
                    String queryString = authenticatorProperties
                            .get(FrameworkConstants.QUERY_PARAMS);
                    if (queryString != null) {
                        if (!queryString.startsWith("&")) {
                            loginPage = loginPage + "&" + queryString;
                        } else {
                            loginPage = loginPage + queryString;
                        }
                    }
                }

                response.sendRedirect(loginPage);
            } catch (IOException e) {
                log.error("Error when sending to OpenID Provider", e);
                throw new AuthenticationFailedException(e.getMessage(), e);
            } catch (OpenIDException e) {
                log.error("Error when sending to OpenID Provider", e);
                throw new AuthenticationFailedException(e.getMessage(), e);
            }
        } else { // Claimed Identity
            String loginPage = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
            String queryParams = FrameworkUtils.getQueryStringWithFrameworkContextId(
                    context.getQueryParams(), context.getCallerSessionKey(),
                    context.getContextIdentifier());

            try {
                // TODO should be able to remove loginType=openid
                response.sendRedirect(loginPage + ("?" + queryParams + "&loginType=openid")
                        + "&authenticators=" + getName() + ":" + "LOCAL");
            } catch (IOException e) {
                log.error("Error when sending to the login page", e);
                throw new AuthenticationFailedException(e.getMessage(), e);
            }
        }

        return;
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
            HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        OpenIDManager manager = getNewOpenIDManagerInstance();

        try {
            manager.processOpenIDLoginResponse(request, response, context);
        } catch (OpenIDException e) {
            log.error("Error when processing response from OpenID Provider", e);
            throw new AuthenticationFailedException(e.getMessage(), e);
        }
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {

        if (log.isTraceEnabled()) {
            log.trace("Inside getContextIdentifier()");
        }

        return request.getParameter("sessionDataKey");
    }

    private OpenIDManager getNewOpenIDManagerInstance() {

        OpenIDManager openIDManager = null;
        String managerClassName = getAuthenticatorConfig().getParameterMap().get(OPENID_MANAGER);
        if (managerClassName != null) {
            try {
                // Bundle class loader will cache the loaded class and returned
                // the already loaded instance, hence calling this method
                // multiple times doesn't cost.
                Class clazz = Thread.currentThread().getContextClassLoader()
                        .loadClass(managerClassName);
                openIDManager = (OpenIDManager) clazz.newInstance();

            } catch (ClassNotFoundException e) {
                log.error("Error while instantiating the OpenIDManager ", e);
            } catch (InstantiationException e) {
                log.error("Error while instantiating the OpenIDManager ", e);
            } catch (IllegalAccessException e) {
                log.error("Error while instantiating the OpenIDManager ", e);
            }
        } else {
            openIDManager = new DefaultOpenIDManager();
        }

        return openIDManager;
    }

    @Override
    public String getClaimDialectURI() {
        return "http://axschema.org";
    }

    @Override
    public String getFriendlyName() {
        return "openid";
    }

    @Override
    public String getName() {
        return "OpenIDAuthenticator";
    }

    /**
     * 
     * @return
     */
    protected String getOpenIDServerUrl() {
        return null;
    }
}
