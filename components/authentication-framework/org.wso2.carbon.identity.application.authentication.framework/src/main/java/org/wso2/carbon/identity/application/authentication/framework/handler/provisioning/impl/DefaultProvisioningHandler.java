package org.wso2.carbon.identity.application.authentication.framework.handler.provisioning.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.core.util.PermissionUpdateUtil;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.provisioning.ProvisioningHandler;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

public class DefaultProvisioningHandler implements ProvisioningHandler {

    private static Log log = LogFactory.getLog(DefaultProvisioningHandler.class);
    private static volatile DefaultProvisioningHandler instance;
    private SecureRandom random = new SecureRandom();

    public static DefaultProvisioningHandler getInstance() {
        if (instance == null) {
            synchronized (DefaultProvisioningHandler.class) {
                if (instance == null) {
                    instance = new DefaultProvisioningHandler();
                }
            }
        }
        return instance;
    }

    public void handle(List<String> roles, String subject, Map<String, String> attributes,
                       String provisioningUserStoreId, String tenantDomain) throws FrameworkException {

        RegistryService registryService = FrameworkServiceComponent.getRegistryService();
        RealmService realmService = FrameworkServiceComponent.getRealmService();

        try {
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(registryService,
                    realmService, tenantDomain);

            String userstoreDomain = getUserStoreDomain(provisioningUserStoreId, realm);

            String username = MultitenantUtils.getTenantAwareUsername(subject);

            UserStoreManager userstore = null;
            if (userstoreDomain != null && !userstoreDomain.isEmpty()) {
                userstore = realm.getUserStoreManager().getSecondaryUserStoreManager(
                        userstoreDomain);
            } else {
                userstore = realm.getUserStoreManager();
            }

            if (userstore == null) {
                throw new FrameworkException("Specified user store is invalid");
            }

            // Remove userstore domain from username if the userstoreDomain is not primary
            if (realm.getUserStoreManager().getRealmConfiguration().isPrimary()) {
                username = UserCoreUtil.removeDomainFromName(username);
            }

            String[] newRoles = new String[]{};

            if (roles != null) {
                roles = removeDomainFromNamesExcludeInternal(roles);
                newRoles = roles.toArray(new String[roles.size()]);
            }

            if (log.isDebugEnabled()) {
                log.debug("User " + username + " contains roles : " + Arrays.toString(newRoles)
                        + " going to be provisioned");
            }

            // addingRoles = newRoles AND allExistingRoles
            Collection<String> addingRoles = new ArrayList<String>();
            Collections.addAll(addingRoles, newRoles);
            Collection<String> allExistingRoles = removeDomainFromNamesExcludeInternal(
                    Arrays.asList(userstore.getRoleNames()));
            addingRoles.retainAll(allExistingRoles);

            Map<String, String> userClaims = new HashMap<String, String>();
            if (attributes != null && !attributes.isEmpty()) {
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    String claimURI = entry.getKey();
                    String claimValue = entry.getValue();
                    if (!(StringUtils.isEmpty(claimURI) || StringUtils.isEmpty(claimValue))) {
                        userClaims.put(claimURI, claimURI);
                    }
                }
            }

            if (userstore.isExistingUser(username)) {

                if (roles != null && roles.size() > 0) {
                    // Update user
                    Collection<String> currentRolesList = Arrays.asList(userstore
                            .getRoleListOfUser(username));
                    // addingRoles = (newRoles AND existingRoles) - currentRolesList)
                    addingRoles.removeAll(currentRolesList);

                    Collection<String> deletingRoles = new ArrayList<String>();
                    deletingRoles.addAll(currentRolesList);
                    // deletingRoles = currentRolesList - newRoles
                    deletingRoles.removeAll(Arrays.asList(newRoles));

                    // Exclude Internal/everyonerole from deleting role since its cannot be deleted
                    deletingRoles.remove(realm.getRealmConfiguration().getEveryOneRoleName());

                    // TODO : Does it need to check this?
                    // Check for case whether superadmin login
                    if (userstore.getRealmConfiguration().isPrimary()
                            && username.equals(realm.getRealmConfiguration().getAdminUserName())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Federated user's username is equal to super admin's username of local IdP.");
                        }

                        // Whether superadmin login without superadmin role is permitted
                        if (deletingRoles
                                .contains(realm.getRealmConfiguration().getAdminRoleName())) {
                            if (log.isDebugEnabled()) {
                                log.debug("Federated user doesn't have super admin role. Unable to sync roles, since super admin role cannot be unassingned from super admin user");
                            }
                            throw new FrameworkException(
                                    "Federated user which having same username to super admin username of local IdP, trying login without having superadmin role assigned");
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Deleting roles : "
                                + Arrays.toString(deletingRoles.toArray(new String[0]))
                                + " and Adding roles : "
                                + Arrays.toString(addingRoles.toArray(new String[0])));
                    }
                    userstore.updateRoleListOfUser(username, deletingRoles.toArray(new String[0]),
                            addingRoles.toArray(new String[0]));
                    if (log.isDebugEnabled()) {
                        log.debug("Federated user: " + username
                                + " is updated by authentication framework with roles : "
                                + Arrays.toString(newRoles));
                    }
                }

                if (!userClaims.isEmpty()) {
                    userstore.setUserClaimValues(username, userClaims, null);
                }

            } else {

                userstore.addUser(username, generatePassword(username), addingRoles.toArray(new String[0]),
                        userClaims, null);

                if (log.isDebugEnabled()) {
                    log.debug("Federated user: " + username
                            + " is provisioned by authentication framework with roles : "
                            + Arrays.toString(addingRoles.toArray(new String[0])));
                }
            }

            PermissionUpdateUtil.updatePermissionTree(tenantId);

        } catch (UserStoreException e) {
            throw new FrameworkException("Error while provisioning user : " + subject, e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new FrameworkException("Error while provisioning user : " + subject, e);
        } catch (CarbonException e) {
            throw new FrameworkException("Error while provisioning user : " + subject, e);
        }
    }

    /**
     * Compute the user store which user to be provisioned
     *
     * @return
     * @throws UserStoreException
     */
    private String getUserStoreDomain(String userStoreDomain, UserRealm realm)
            throws FrameworkException, UserStoreException {

        // If the any of above value is invalid, keep it empty to use primary userstore
        if (userStoreDomain != null
                && realm.getUserStoreManager().getSecondaryUserStoreManager(userStoreDomain) == null) {
            throw new FrameworkException("Specified user store domain " + userStoreDomain
                    + " is not valid.");
        }

        return userStoreDomain;
    }

    /**
     * Generates (random) password for user to be provisioned
     *
     * @param username
     * @return
     */
    protected String generatePassword(String username) {
        return new BigInteger(130, random).toString(32);
    }

    private String getUserStoreClaimValueFromMap(Map<ClaimMapping, String> claimMappingStringMap,
                                                 String userStoreClaimURI) {

        for (Map.Entry<ClaimMapping, String> entry : claimMappingStringMap.entrySet()) {
            if (entry.getKey().getRemoteClaim().getClaimUri().equals(userStoreClaimURI)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * remove user store domain from names except the domain 'Internal'
     * @param names
     * @return
     */
    private List<String> removeDomainFromNamesExcludeInternal(List<String> names){
        List<String> nameList = new ArrayList<String>();
        for(String name:names){
            String userStoreDomain = UserCoreUtil.extractDomainFromName(name);
            if(UserCoreConstants.INTERNAL_DOMAIN.equalsIgnoreCase(userStoreDomain)){
                nameList.add(name);
            }else{
                nameList.add(UserCoreUtil.removeDomainFromName(name));
            }
        }
        return nameList;
    }
}
