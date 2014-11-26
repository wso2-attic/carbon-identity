/*
 * Copyright 2005,2006 WSO2, Inc. http://www.wso2.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ws.security.util.UUIDGenerator;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.services.util.CarbonAuthenticationUtil;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityConstants.ServerConfig;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityClaimManager;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.relyingparty.dto.ClaimDTO;
import org.wso2.carbon.identity.relyingparty.dto.OpenIDAuthInfoDTO;
import org.wso2.carbon.identity.relyingparty.dto.OpenIDDTO;
import org.wso2.carbon.identity.relyingparty.dto.OpenIDSignInDTO;
import org.wso2.carbon.identity.relyingparty.internal.IdentityRPServiceComponent;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.mgt.UserMgtConstants;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import com.google.step2.Step2;

public class RelyingPartyService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(RelyingPartyService.class);
    private static final String GOOGLE_APPS_IDP_NAME = "GoogleApps";

    /**
     * @param openID
     * @return
     * @throws Exception
     */
    public OpenIDSignInDTO signInWithOpenID(OpenIDDTO openID) throws Exception {

        OpenIDSignInDTO dto = new OpenIDSignInDTO();
        MessageContext msgCtx = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgCtx
                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSess = request.getSession();
        String user = getUserNameFromOpenID(openID.getOpenID());
        String domainName = null;
        if (user != null) {
            domainName = MultitenantUtils.getDomainNameFromOpenId(openID.getOpenID());
        }
        UserRealm userRealm = IdentityTenantUtil.getRealm(domainName, user);
        Registry registry = IdentityTenantUtil.getRegistry(domainName, user);

        RealmService realmService = IdentityRPServiceComponent.getRealmService();

        String tenantAwareUserName = null;
        
        if (user!=null) {
        	tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(user);
        }
        
        if (user != null && userRealm.getUserStoreManager().isExistingUser(tenantAwareUserName)) {
            //this.onUserLogin(tenantAwareUserName, domainName, httpSess);
            dto.setAuthenticated(true);
            dto.setUserID(user);
            CarbonAuthenticationUtil.onSuccessAdminLogin(httpSess, tenantAwareUserName, realmService
                    .getTenantManager().getTenantId(domainName), domainName, "Info card login");
        } else {
            // external parties
            IdentityPersistenceManager persistentManager = IdentityPersistenceManager
                    .getPersistanceManager();
            if (persistentManager.hasSignedUpForOpenId(registry, userRealm, openID.getOpenID())) {
                user = persistentManager.getUserIdForOpenIDSignUp(registry, userRealm,
                        openID.getOpenID());
                if (userRealm.getUserStoreManager().isExistingUser(user)) {
                    domainName = MultitenantUtils.getTenantDomain(user);
                    CarbonAuthenticationUtil
                            .onSuccessAdminLogin(httpSess, user, realmService.getTenantManager()
                                    .getTenantId(domainName), domainName, "OpenID login");
                    dto.setAuthenticated(true);
                    dto.setUserID(user);
                }
            }
        }
        return dto;
    }

    /**
     * @return
     * @throws Exception
     */
    public OpenIDAuthInfoDTO getOpenIDAuthInfo(String dialect) throws Exception {
        IdentityClaimManager claimManager = null;
        Claim[] claims = null;
        List<String> required = null;
        List<String> optional = null;
        OpenIDAuthInfoDTO dto = null;
        UserRealm realm = null;

        claimManager = IdentityClaimManager.getInstance();
        realm = IdentityTenantUtil.getRealm(null, null);
        claims = claimManager.getAllSupportedClaims(dialect, realm);

        String openidRealm = (String) IdentityConfigParser.getInstance().getConfiguration()
                .get(IdentityConfigParser.OPENID_REALM);

        boolean requestClaimsFromIdP = Boolean
                .parseBoolean((String) IdentityConfigParser.getInstance().getConfiguration()
                        .get(IdentityConfigParser.REQUEST_CLAIMS_FROM_IDP));

        if (claims == null || claims.length == 0) {
            return null;
        }

        required = new ArrayList<String>();
        optional = new ArrayList<String>();

        for (Claim claim : claims) {
            if (claim.isRequired()) {
                required.add(claim.getClaimUri());
            } else {
                optional.add(claim.getClaimUri());
            }
        }

        dto = new OpenIDAuthInfoDTO();
        dto.setOptionalClaims(optional.toArray(new String[optional.size()]));
        dto.setRequiredClaims(required.toArray(new String[required.size()]));
        dto.setRequestTypes(new String[] { IdentityConstants.OpenId.SIMPLE_REGISTRATION });
        dto.setRealm(openidRealm);
        dto.setRequestClaimsFromIdP(requestClaimsFromIdP);
        return dto;
    }

    /**
     * @param openID
     * @return
     * @throws Exception
     */
    public static String getUserNameFromOpenID(String openID) throws Exception {

        String user = null;

        String openIDPattern = IdentityUtil.getProperty(ServerConfig.OPENID_USER_PATTERN);

        if (openID.length() > openIDPattern.length()) {
            user = openID.substring(openIDPattern.length());
            if (!openID.equals(openIDPattern + user)) {
                return null;
            }
        }

        return user;
    }

    /**
     * 
     * @param dto
     * @return
     * @throws IdentityException
     * @throws RegistryException
     */
    public boolean addOpenIdToProfile(OpenIDDTO dto) throws IdentityException, RegistryException {
        String openID = dto.getOpenID();
        String tenantDomain = MultitenantUtils.getDomainNameFromOpenId(openID);
        UserRealm realm = IdentityTenantUtil.getRealm(tenantDomain, dto.getUserName());

        try {
            if (realm.getUserStoreManager().isExistingUser(dto.getUserName())) {
                if (dto.getPassword() != null) {
                    boolean authenticated = realm.getUserStoreManager().authenticate(
                            dto.getUserName(), dto.getPassword());
                    if (authenticated) {
                        return doOpenIDSignUp(dto.getUserName(), dto.getOpenID());
                    }
                } else {
                    return doOpenIDSignUp(dto.getUserName(), dto.getOpenID());
                }
            }
        } catch (Exception e) {
            throw new IdentityException(e.getMessage(), e);
        }

        return false;
    }

    public void signInGAppUser(OpenIDDTO openId, String domainName) throws Exception {
        try {
            String email = null;
            String firstName = null;
            String lastName = null;
            for (ClaimDTO claim : openId.getClaims()) {
                if (claim.getClaimUri().equals(Step2.AxSchema.EMAIL.getUri())) {
                    email = claim.getClaimValue();
                } else if (claim.getClaimUri().equals(Step2.AxSchema.FIRST_NAME.getUri())) {
                    firstName = claim.getClaimValue();
                } else if (claim.getClaimUri().equals(Step2.AxSchema.LAST_NAME.getUri())) {
                    lastName = claim.getClaimValue();
                }
            }

            String username = MultitenantUtils.getTenantAwareUsername(email);
            if (username == null) {
                log.error("The username is null");
                throw new Exception("The username is null");
            }

            RealmService realmService = IdentityRPServiceComponent.getRealmService();
            UserRealm userRealm = null;
            Registry registry = null;
            int tenantId = realmService.getTenantManager().getTenantId(domainName);

            if (tenantId == -1) {
                String message = "Your google app domain "
                        + domainName
                        + " is not setup for Stratos. Please contact your Goole Apps administrator and ask him to setup Stratos via Google Apps Marketplace.";
                throw new Exception(message);
            }

            userRealm = IdentityTenantUtil.getRealm(domainName, null);
            if (!userRealm.getUserStoreManager().isExistingUser(username)) {
                if (!GOOGLE_APPS_IDP_NAME.equals(userRealm.getRealmConfiguration()
                        .getUserStoreProperties()
                        .get(UserCoreConstants.RealmConfig.PROPERTY_EXTERNAL_IDP))) {
                    throw new Exception(
                            "The domain you are trying to login already exist. If you can prove your rights to this domain please contact administrator.");
                }
                String password = UUIDGenerator.getUUID();
                UserStoreManager userStore = userRealm.getUserStoreManager();
                if (!userStore.isExistingRole(IdentityConstants.IDENTITY_DEFAULT_ROLE)) {
                    Permission permission = new Permission("/permission/admin/login",
                            UserMgtConstants.EXECUTE_ACTION);
                    userStore.addRole(IdentityConstants.IDENTITY_DEFAULT_ROLE, null,
                            new Permission[] { permission }, false);
                }
                userStore.addUser(username, password,
                        new String[] { IdentityConstants.IDENTITY_DEFAULT_ROLE }, null, null);
                IdentityPersistenceManager manager = IdentityPersistenceManager
                        .getPersistanceManager();
                registry = IdentityRPServiceComponent.getRegistryService().getConfigSystemRegistry(
                        tenantId);
                manager.doOpenIdSignUp(registry, userRealm, openId.getOpenID(), username);
                this.addUserClaims(username, firstName, lastName, email, tenantId);
            }

            // MessageContext msgCtx =
            // MessageContext.getCurrentMessageContext();
            //
            // HttpServletRequest request =
            // (HttpServletRequest) msgCtx.
            // getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            // HttpSession httpSess = request.getSession();
            // CarbonAuthenticationUtil.onSuccessAdminLogin(httpSess, username,
            // tenantId,
            // domainName,
            // "GApp login");

        } catch (Exception e) {
            log.error("An error occured while signing in gapp user " + e.getMessage());
            throw e;
        }
    }

    private void onUserLogin(String userName, String tenantDomain, HttpSession httpSess)
            throws Exception {
        httpSess.setAttribute(ServerConstants.USER_LOGGED_IN, userName);
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                .getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(tenantDomain);
        carbonContext.setUsername(userName);
        int tenantId = IdentityRPServiceComponent.getRealmService().getTenantManager()
                .getTenantId(tenantDomain);
        carbonContext.setTenantId(tenantId);
        carbonContext.setRegistry(RegistryType.SYSTEM_CONFIGURATION, IdentityRPServiceComponent
                .getRegistryService().getConfigSystemRegistry(tenantId));
        carbonContext.setRegistry(RegistryType.USER_CONFIGURATION, IdentityRPServiceComponent
                .getRegistryService().getConfigUserRegistry(userName, tenantId));
        carbonContext.setRegistry(RegistryType.USER_GOVERNANCE, IdentityRPServiceComponent
                .getRegistryService().getGovernanceUserRegistry(userName, tenantId));
        carbonContext.setRegistry(RegistryType.SYSTEM_GOVERNANCE, IdentityRPServiceComponent
                .getRegistryService().getGovernanceSystemRegistry(tenantId));
        carbonContext.setUserRealm(IdentityRPServiceComponent.getRegistryService()
                .getGovernanceUserRegistry(userName, tenantId).getUserRealm());
    }

    /**
     * 
     * @param userName
     * @param openId
     * @return
     * @throws IdentityException
     * @throws RegistryException
     */
    private boolean doOpenIDSignUp(String userName, String openId) throws Exception {
        IdentityPersistenceManager manager = IdentityPersistenceManager.getPersistanceManager();
        String domain = MultitenantUtils.getDomainNameFromOpenId(openId);
        Registry registry = IdentityTenantUtil.getRegistry(domain, userName);
        UserRealm realm = IdentityTenantUtil.getRealm(domain, userName);
        return manager.doOpenIdSignUp(registry, realm, openId, userName);
    }

    private void addUserClaims(String userName, String firstName, String lastName,
            String emailAddress, int tenantId) throws Exception {
        try {
            Map<String, String> claimsMap = new HashMap<String, String>();

            claimsMap.put(UserCoreConstants.ClaimTypeURIs.GIVEN_NAME, firstName);
            claimsMap.put(UserCoreConstants.ClaimTypeURIs.SURNAME, lastName);
            claimsMap.put(UserCoreConstants.ClaimTypeURIs.EMAIL_ADDRESS, emailAddress);

            // can be extended to store other user information.
            UserStoreManager userStoreManager = (UserStoreManager) IdentityRPServiceComponent
                    .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
            userStoreManager.setUserClaimValues(userName, claimsMap,
                    UserCoreConstants.DEFAULT_PROFILE);

        } catch (Exception e) {
            String msg = "Error in adding claims to the user.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }
}
