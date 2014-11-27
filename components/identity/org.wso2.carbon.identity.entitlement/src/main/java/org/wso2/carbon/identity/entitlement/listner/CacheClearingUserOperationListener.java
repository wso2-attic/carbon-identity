/*
 *  Copyright (c) Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.entitlement.listner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.EntitlementException;
import org.wso2.carbon.identity.entitlement.cache.EntitlementPolicyInvalidationCache;
import org.wso2.carbon.identity.entitlement.internal.EntitlementServiceComponent;
import org.wso2.carbon.identity.entitlement.pdp.EntitlementEngine;
import org.wso2.carbon.identity.entitlement.pip.AbstractPIPAttributeFinder;
import org.wso2.carbon.identity.entitlement.pip.CarbonAttributeFinder;
import org.wso2.carbon.identity.entitlement.pip.PIPAttributeFinder;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * This listener is registered as a user operation listener. Whenever a user operation takes place
 * this listener fires and responsible for clearing caches within entitlement engine as
 * well as sending notifications to registered pep endpoints
 */
public class CacheClearingUserOperationListener implements UserOperationEventListener {

    private static Log log = LogFactory.getLog(CacheClearingUserOperationListener.class);

    @Override
    public int getExecutionOrderId() {
        return 3;
    }

    @Override
    public boolean doPreAuthenticate(String s, Object o, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAuthenticate(String s, boolean b, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreAddUser(String s, Object o, String[] strings,
                                Map<String, String> stringStringMap, String s2,
                                UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAddUser(String s, Object o, String[] strings,
                                 Map<String, String> stringStringMap, String s2,
                                 UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateCredential(String s, Object o, Object o2,
                                         UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredential(String s, Object o, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreUpdateCredentialByAdmin(String s, Object o,
                                                UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateCredentialByAdmin(String s, Object o,
                                                 UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreDeleteUser(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

	/**
	 * on post operations all internal caches are cleared and notifications to pep endpoints are sent
	 * @param s
	 * @param userStoreManager
	 * @return
	 * @throws UserStoreException
	 */
    @Override
    public boolean doPostDeleteUser(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValue(String s, String s2, String s3, String s4,
                                          UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValue(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPreSetUserClaimValues(String s, Map<String, String> stringStringMap, String s2,
                                           UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostSetUserClaimValues(String s, Map<String, String> stringStringMap,
                                            String s2, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String s, String[] strings, String s2,
                                              UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValues(String s, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String s, String s2, String s3,
                                             UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteUserClaimValue(String s, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreAddRole(String s, String[] strings, Permission[] permissions,
                                UserStoreManager userStoreManager) throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostAddRole(String s, String[] strings, Permission[] permissions,
                                 UserStoreManager userStoreManager) throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreDeleteRole(String s, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostDeleteRole(String s, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreUpdateRoleName(String s, String s2, UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleName(String s, String s2, UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreUpdateUserListOfRole(String s, String[] strings, String[] strings2,
                                             UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateUserListOfRole(String s, String[] strings, String[] strings2,
                                              UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String s, String[] strings, String[] strings2,
                                             UserStoreManager userStoreManager)
            throws UserStoreException {
        return true;
    }

    @Override
    public boolean doPostUpdateRoleListOfUser(String s, String[] strings, String[] strings2,
                                              UserStoreManager userStoreManager)
            throws UserStoreException {

        try {
            clearCache();
        } catch (EntitlementException e) {
            log.error("Error while clearing entitlement cache", e);
        }
        return true;
    }

    /***
     * this method is reponsible for clearing all 3 major caches of entitlement engine
     * including  PIP_ATTRIBUTE_CACHE , PDP_DECISION_INVALIDATION_CACHE, ENTITLEMENT_POLICY_INVALIDATION_CACHE
     * @throws EntitlementException
     */
    public void clearCarbonAttributeCache() throws EntitlementException {

        CarbonAttributeFinder finder = EntitlementEngine.getInstance().getCarbonAttributeFinder();
        if (finder != null) {
            finder.clearAttributeCache();
            // we need invalidate policy cache as well. Decision cache is cleared within clearAttributeCache.
            clearPolicyCache();
        } else {
            throw new EntitlementException(
                    "Can not clear attribute cache - Carbon Attribute Finder is not initialized");
        }

        Map<PIPAttributeFinder, Properties> designators =
                EntitlementServiceComponent.getEntitlementConfig()
                        .getDesignators();
        if (designators != null && !designators.isEmpty()) {
            Set<PIPAttributeFinder> pipAttributeFinders = designators.keySet();
            for (PIPAttributeFinder pipAttributeFinder : pipAttributeFinders) {
                if (pipAttributeFinder instanceof AbstractPIPAttributeFinder) {
                    pipAttributeFinder.clearCache();
                }
            }
        }
    }

    /**
     * clears internal cache and sends cache clearing notifications to endpoints
     * @throws EntitlementException
     */
    public void clearCache() throws EntitlementException {
        clearCarbonAttributeCache();
    }

    /**
     * clears policy cache
     * @throws EntitlementException
     */
    public void clearPolicyCache() throws EntitlementException {
        EntitlementPolicyInvalidationCache.getInstance().invalidateCache();
    }

}
