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
package org.wso2.carbon.identity.authenticator.webseal;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;
import org.wso2.carbon.core.services.authentication.CarbonServerAuthenticator;
import org.wso2.carbon.core.services.util.CarbonAuthenticationUtil;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.authenticator.webseal.internal.WebSealAuthBEDataHolder;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.AuthenticationObserver;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class WebSealAuthenticator  extends AbstractAdmin implements CarbonServerAuthenticator {

    private static final int DEFAULT_PRIORITY_LEVEL = 10;
    private static final String AUTHENTICATOR_NAME = "WebSealUIAuthenticator";
    private static final String DEFAULT_DELEGATION_ROLE = "delegated-admin";
    private static final Log log = LogFactory.getLog(WebSealAuthenticator.class);

    /**
     * role name
     */
    private String delegatedRoleName = null;

    /**
     * Supports delegated identity logins. A third party system authenticates
     * the authenticatedUser - had to accept it's assertion the system needs to
     * authenticates it self to carbon. Also the only the users who belong the
     * roles having delegate-identity permission could perform this action.
     * 
     * @param username
     *            User who could delegate logins for other users.
     * @param password
     *            Password of the user who could delegate logins for other
     *            users.
     * @param authenticatedUser
     *            The user who's being authenticated by a third party system.
     * @param remoteAddress
     *            RemoteAddress
     * @return true if auth succeeded, false otherwise
     * @throws AuthenticationException  throws, if fails
     */
    public boolean login(String username, String password, String authenticatedUser,
            String remoteAddress) throws AuthenticationException {

        HttpSession httpSess = getHttpSession();
        try {
            if ((username == null) || (password == null) || (remoteAddress == null)
                    || username.trim().equals("") || password.trim().equals("")
                    || remoteAddress.trim().equals("")) {
                CarbonAuthenticationUtil.onFailedAdminLogin(httpSess, username, -1, remoteAddress,
                        "Failed to login. Username/ Password/ Remote address is empty");
                return false;
            }

            RegistryService registryService = WebSealAuthBEDataHolder.getInstance().getRegistryService();
            RealmService realmService = WebSealAuthBEDataHolder.getInstance().getRealmService();

            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            username = MultitenantUtils.getTenantAwareUsername(username);

            UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(registryService,
                    realmService, tenantDomain);

            boolean isAuthenticated = realm.getUserStoreManager().authenticate(username, password);

            if (!isAuthenticated) {
                CarbonAuthenticationUtil.onFailedAdminLogin(httpSess, username, -1, remoteAddress,
                        "User " + username + " is not authenticated");
                return false;
            }

            // If we are to trust the user who delegates identity - he should be
            // in a role called delegated-admin.
            
            String[] roles = realm.getUserStoreManager().getRoleListOfUser(username);
            boolean  isDelegateToAuthorized = false;
            
            if(roles != null){
                if(delegatedRoleName == null){
                    delegatedRoleName = getDelegationRoleName();
                }
                for(String role : roles){
                    if(role.equals(delegatedRoleName)){
                        isDelegateToAuthorized = true;
                        break;
                    }
                }
            }

            // authenticatedUser user should have the permission to login to the
            // system.
            boolean isLoginToAuthorized = realm.getAuthorizationManager().isUserAuthorized(
                authenticatedUser, "/permission/admin/login", CarbonConstants.UI_PERMISSION_ACTION);

            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);

            if(!isDelegateToAuthorized){
                CarbonAuthenticationUtil.onFailedAdminLogin(httpSess, username, -1, remoteAddress,
                        "User " + username + " is not authorized for identity delegation on behalf of "
                                                                            + authenticatedUser);
                log.warn("User " + username + " is not authorized for identity delegation on behalf of "
                                                                            + authenticatedUser);                
                return false;
            }

            if (isLoginToAuthorized) {
                CarbonAuthenticationUtil.onSuccessAdminLogin(httpSess, authenticatedUser, tenantId,
                        tenantDomain, remoteAddress);
                handleAuthenticationCompleted(tenantId, true);
                log.info("Identity delegation by " + username + " on behalf of " + authenticatedUser);
                return true;
            } else {
                CarbonAuthenticationUtil.onFailedAdminLogin(httpSess, username, -1, remoteAddress,
                        "User "  + authenticatedUser + " is not authorized to login using delegation");
                handleAuthenticationCompleted(tenantId, false);
                log.warn("User "  + authenticatedUser + " is not authorized to login using delegation");
                return false;
            }
        } catch (Exception e) {
            String msg = "System error while Authenticating/Authorizing User with identity delegation";
            log.error(msg, e);
            return false;
        }
    }

    public void logout() {
        
        String loggedInUser;
        String delegatedBy;
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat date = new SimpleDateFormat("'['yyyy-MM-dd HH:mm:ss,SSSS']'");
        HttpSession session = getHttpSession();

        if (session != null) {
            loggedInUser = (String) session.getAttribute(ServerConstants.USER_LOGGED_IN);
            delegatedBy = (String) session.getAttribute("DELEGATED_BY");
            if (delegatedBy == null) {
                log.info("'" + loggedInUser + "' logged out at " + date.format(currentTime));
            } else {
                log.info("'" + loggedInUser + "' logged out at " + date.format(currentTime)
                         + " delegated by " + delegatedBy);
            }
            session.invalidate();
        }
    }
    

    @Override
    public boolean isHandle(MessageContext messageContext) {
        return true;
    }

    @Override
    public boolean isAuthenticated(MessageContext messageContext) {
        HttpServletRequest request = (HttpServletRequest) messageContext
                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession();
        String userLoggedIn = (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
        return (userLoggedIn != null);
    }

    @Override
    public boolean authenticateWithRememberMe(MessageContext messageContext) {
        return false;
    }

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

    @Override
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    private void handleAuthenticationCompleted(int tenantId, boolean isSuccessful) throws Exception {
        BundleContext bundleContext = WebSealAuthBEDataHolder.getInstance().getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker = new ServiceTracker(bundleContext,
                    AuthenticationObserver.class.getName(), null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).completedAuthentication(tenantId,
                            isSuccessful);
                }
            }
            tracker.close();
        }
    }

    private String getDelegationRoleName(){

        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null && authenticatorConfig.getParameters() != null) {
            String role = authenticatorConfig.getParameters().get("DelegationRole");
            if(role != null && role.trim().length() > 0){
                return role;
            }
        }
        return DEFAULT_DELEGATION_ROLE;
    }
}
