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
package org.wso2.carbon.identity.application.authenticator.requestpath.basicauth;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.RequestPathApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.requestpath.basicauth.internal.BasicAuthRequestPathAuthenticatorServiceComponent;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class BasicAuthRequestPathAuthenticator extends AbstractApplicationAuthenticator implements RequestPathApplicationAuthenticator {


    private static final long serialVersionUID = -3707836631281782935L;
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String BASIC_AUTH_SCHEMA = "Basic";
    private static final String AUTHENTICATOR_NAME = "BasicAuthRequestPathAuthenticator";
    private static Log log = LogFactory.getLog(BasicAuthRequestPathAuthenticator.class);

    @Override
    public boolean canHandle(HttpServletRequest request) {

        if (log.isTraceEnabled()) {
            log.trace("Inside canHandle()");
        }

        String headerValue = (String) request.getSession().getAttribute(AUTHORIZATION_HEADER_NAME);

        if (headerValue != null && !"".equals(headerValue.trim())) {
            String[] headerPart = headerValue.trim().split(" ");
            if (BASIC_AUTH_SCHEMA.equals(headerPart[0])) {
                return true;
            }
        } else if (request.getParameter("sectoken") != null) {
            return true;
        }

        return false;
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        // if this was set by the relevant servlet
        String headerValue = (String) request.getSession().getAttribute(AUTHORIZATION_HEADER_NAME);
        String credential = null;

        if (headerValue != null) {
            credential = headerValue.trim().split(" ")[1];
        } else {
            credential = request.getParameter("sectoken");
        }

        String credentials = new String(Base64.decode(credential));
        String username = credentials.substring(0, credentials.indexOf(":"));
        String password = credentials.substring(credentials.indexOf(":") + 1);
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new AuthenticationFailedException("username and password cannot be empty");
        }

        try {
            int tenantId = IdentityTenantUtil.getTenantIdOfUser(username);
            UserStoreManager userStoreManager = (UserStoreManager) BasicAuthRequestPathAuthenticatorServiceComponent.
                    getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
            boolean isAuthenticated = userStoreManager.authenticate(
                    MultitenantUtils.getTenantAwareUsername(username), password);
            if (!isAuthenticated) {
                throw new AuthenticationFailedException("Authentication Failed");
            }
            if (log.isDebugEnabled()) {
                log.debug("Authenticated user " + username);
            }

            Map<String, Object> authProperties = context.getProperties();
            String tenantDomain = MultitenantUtils.getTenantDomain(username);

            if (authProperties == null) {
                authProperties = new HashMap<String, Object>();
                context.setProperties(authProperties);
            }

            // TODO: user tenant domain has to be an attribute in the
            // AuthenticationContext
            authProperties.put("user-tenant-domain", tenantDomain);

            context.setSubject(AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier(
                    FrameworkUtils.prependUserStoreDomainToName(username)));
        } catch (IdentityRuntimeException e) {
            if(log.isDebugEnabled()){
                log.debug("BasicAuthentication failed while trying to get the tenant ID of the user " + username, e);
            }
            throw new AuthenticationFailedException(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthenticationFailedException("Authentication Failed");
        }
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return "basic-auth";
    }

    @Override
    public String getName() {
        return AUTHENTICATOR_NAME;
    }
}
