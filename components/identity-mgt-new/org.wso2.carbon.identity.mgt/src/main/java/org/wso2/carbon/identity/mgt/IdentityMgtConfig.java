/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.mgt.constants.IdentityMgtConstants;
import org.wso2.carbon.identity.mgt.mail.DefaultEmailSendingModule;
import org.wso2.carbon.identity.mgt.password.DefaultPasswordGenerator;
import org.wso2.carbon.identity.mgt.password.RandomPasswordGenerator;
import org.wso2.carbon.identity.mgt.policy.PolicyRegistry;
import org.wso2.carbon.identity.mgt.services.IdentityMgtService;
import org.wso2.carbon.identity.mgt.services.impl.IdentityMgtServiceImpl;
import org.wso2.carbon.identity.mgt.store.UserIdentityDataStore;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.*;

/**
 * encapsulates recovery config data
 */
public class IdentityMgtConfig {

    private static final Log log = LogFactory.getLog(IdentityMgtConfig.class);
    private static IdentityMgtConfig identityMgtConfig;
    private boolean accountLockEnable;
    IdentityMgtService identityMgtService = new IdentityMgtServiceImpl();
    private boolean enableAuthPolicy;
    private boolean authPolicyAccountExistCheck;
    private int authPolicyMaxLoginAttempts;
    private UserIdentityDataStore identityDataStore;
    private PolicyRegistry policyRegistry = new PolicyRegistry();

    public IdentityMgtConfig(RealmConfiguration configuration) {

        Properties properties = identityMgtService.addConfigurations(MultitenantConstants.SUPER_TENANT_ID);
        setConfigurations(configuration, properties);

    }

    public IdentityMgtConfig(RealmConfiguration configuration, int tenantId) {

        Properties properties = identityMgtService.getConfigurations(tenantId);
        setConfigurations(configuration, properties);

    }

    public void setConfigurations(RealmConfiguration configuration, Properties properties) {

        if (!properties.isEmpty()) {

            try {
                String authPolicyAccountExistCheck = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_EXIST);
                if (authPolicyAccountExistCheck != null) {
                    this.authPolicyAccountExistCheck = Boolean.parseBoolean(authPolicyAccountExistCheck.trim());
                }

                String enableAuthPolicy = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ENABLE);
                if (enableAuthPolicy != null) {
                    this.enableAuthPolicy = Boolean.parseBoolean(enableAuthPolicy.trim());
                }

                String accountLockEnable = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.ACCOUNT_LOCK_ENABLE);
                if (accountLockEnable != null) {
                    this.accountLockEnable = Boolean.parseBoolean(accountLockEnable.trim());
                }

                String maxLoginAttemptProperty = properties.
                        getProperty(IdentityMgtConstants.PropertyConfig.AUTH_POLICY_ACCOUNT_LOCKING_FAIL_ATTEMPTS);
                if (maxLoginAttemptProperty != null) {
                    this.authPolicyMaxLoginAttempts = Integer.valueOf(maxLoginAttemptProperty.trim());
                }

                if (this.authPolicyMaxLoginAttempts == 0) {
                    // default value is set
                    this.authPolicyMaxLoginAttempts = 10;
                }
            } catch (Exception e) {
                log.error("Error while loading identity mgt configurations", e);
            }
        }
    }

    /**
     * Gets instance
     * <p/>
     * As this is only called in start up syn and null check is not needed
     *
     * @param configuration a primary <code>RealmConfiguration</code>
     * @return <code>IdentityMgtConfig</code>
     */
    public static IdentityMgtConfig getInstance(RealmConfiguration configuration) {

        identityMgtConfig = new IdentityMgtConfig(configuration);
        return identityMgtConfig;
    }

    public static IdentityMgtConfig getInstance(RealmConfiguration configuration, int tenantId) {

        identityMgtConfig = new IdentityMgtConfig(configuration, tenantId);
        return identityMgtConfig;
    }

    public static IdentityMgtConfig getInstance() {
        return identityMgtConfig;
    }

    public boolean isAccountLockEnable() {
        return accountLockEnable;
    }

    public boolean isAuthPolicyAccountExistCheck() {
        return authPolicyAccountExistCheck;
    }

    public boolean isEnableAuthPolicy() {
        return enableAuthPolicy;
    }

    public int getAuthPolicyMaxLoginAttempts() {
        return authPolicyMaxLoginAttempts;
    }

    public UserIdentityDataStore getIdentityDataStore() {
        return identityDataStore;
    }

    public PolicyRegistry getPolicyRegistry() {
        return policyRegistry;
    }
}
