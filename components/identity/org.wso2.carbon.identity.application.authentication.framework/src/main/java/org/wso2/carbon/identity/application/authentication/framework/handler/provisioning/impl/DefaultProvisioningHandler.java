package org.wso2.carbon.identity.application.authentication.framework.handler.provisioning.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.core.util.PermissionUpdateUtil;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.provisioning.ProvisioningHandler;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.registry.core.service.RegistryService;
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
	
	public void handle(AuthenticationContext context, ExternalIdPConfig externalIdPConfig, 
			List<String> roles, String subject, Map<String, String> attributes)
			throws FrameworkException {

		RegistryService registryService = FrameworkServiceComponent.getRegistryService();
		RealmService realmService = FrameworkServiceComponent.getRealmService();
		String tenantDomain = MultitenantUtils.getTenantDomain(subject);
		
		try {
			int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
    		UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(registryService,
    				realmService, tenantDomain);
    		
			String userstoreDomain = getUserStoreDomain(externalIdPConfig, attributes, realm);

    		String username = MultitenantUtils.getTenantAwareUsername(subject);
    		
			UserStoreManager userstore = null;
			if(userstoreDomain != null && !userstoreDomain.isEmpty()) {
				userstore = realm.getUserStoreManager().getSecondaryUserStoreManager(userstoreDomain);

				if(userstore == null) {
					throw new FrameworkException("Specified user store is invalid");
				}
				
				// Remove userstore domain from username if the userstoreDomain is not primary
				if(realm.getUserStoreManager().getRealmConfiguration().isPrimary()) {
					username = UserCoreUtil.removeDomainFromName(username);
				}
			}
			
			
			String[] newRoles = roles.toArray(new String[0]);
			if(newRoles == null) {
				newRoles = new String[] {};
			}
    		
			if(log.isDebugEnabled()) {
            	log.debug("User "+username+" contains roles : " + Arrays.toString(newRoles) + " going to be provisioned");
            }

			// addingRoles = newRoles AND allExistingRoles
			Collection<String> addingRoles = new ArrayList<String>();
			Collections.addAll(addingRoles, newRoles);
			Collection<String> allExistingRoles = Arrays.asList(userstore.getRoleNames());
			addingRoles.retainAll(allExistingRoles);
    		
			if(userstore.isExistingUser(username)) {
				// Update user
				Collection<String> currentRolesList = Arrays.asList(userstore.getRoleListOfUser(username));
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
				if(userstore.getRealmConfiguration().isPrimary() && username.equals(realm.getRealmConfiguration().getAdminUserName())) {
    	    				if(log.isDebugEnabled()) {
    	    					log.debug("Federated user's username is equal to super admin's username of local IdP.");
    	    				}

    					// Whether superadmin login without superadmin role is permitted
    					if (deletingRoles.contains(realm.getRealmConfiguration().getAdminRoleName())) {
    						if(log.isDebugEnabled()) {
    							log.debug("Federated user doesn't have super admin role. Unable to sync roles, since super admin role cannot be unassingned from super admin user");
    						}
    						throw new FrameworkException("Federated user which having same username to super admin username of local IdP, trying login without having superadmin role assigned");
    					}
				}
				
	            if(log.isDebugEnabled()) {
	            	log.debug("Deleting roles : " + Arrays.toString(deletingRoles.toArray(new String[0])) + " and Adding roles : " + Arrays.toString(addingRoles.toArray(new String[0])));
	            }
				userstore.updateRoleListOfUser(username, deletingRoles.toArray(new String[0]), addingRoles.toArray(new String[0]));
	            if(log.isDebugEnabled()) {
	            	log.debug("Federated user: " + username + " is updated by authentication framework with roles : " + Arrays.toString(newRoles));
	            }
	            
	            // TODO : Update claims of user?
	            for (Map.Entry<String,String> entry : attributes.entrySet()) {
		            userstore.getClaimManager().getClaim(entry.getKey()).setValue(entry.getValue());
	            }
			}
			else {
				// Provision user
				// TODO : add claims (attributes) to default profile?
	    		userstore.addUser(username, generatePassword(username), addingRoles.toArray(new String[0]), attributes, null);
	            if(log.isDebugEnabled()) {
	            	log.debug("Federated user: " + username + " is provisioned by authentication framework with roles : " + Arrays.toString(addingRoles.toArray(new String[0])));
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
	 * @return
	 * @throws UserStoreException 
	 */
    private String getUserStoreDomain(ExternalIdPConfig externalIdPConfig, Map<String, String> attributes, UserRealm realm) throws FrameworkException, UserStoreException{
    	String userStoreDomainName = null;
    	
    	// Check for claim define user store dynamically and use the value of the claim
    	String userstoreClaim = externalIdPConfig.getProvisioningUserStoreClaimURI();
    	if(attributes.containsKey(userstoreClaim)) {
    		userStoreDomainName = attributes.get(userstoreClaim);
    		
    	}
    	// Else check for a default user store defined in the configuration
    	else {
    		userStoreDomainName = externalIdPConfig.getProvisioningUserStoreId();
    	}
    	
    	// If the any of above value is invalid, keep it empty to use primary userstore
    	if(realm.getUserStoreManager().getSecondaryUserStoreManager(userStoreDomainName) == null) {
    		throw new FrameworkException("Specified user store domain " + userStoreDomainName + " is not valid.");
    	}
    	
		return userStoreDomainName;
	}

	/**
     * Generates (random) password for user to be provisioned
     * @param username
     * @return
     */
    private String generatePassword(String username) {
    	return new BigInteger(130, random).toString(32);
	}
}
