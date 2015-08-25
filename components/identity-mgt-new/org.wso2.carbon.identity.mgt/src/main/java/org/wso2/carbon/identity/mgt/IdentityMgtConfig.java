/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.identity.mgt.service.IdentityMgtService;
import org.wso2.carbon.identity.mgt.service.IdentityMgtServiceImpl;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 * encapsulates recovery config data
 */
public class IdentityMgtConfig {

    private static final Log log = LogFactory.getLog(IdentityMgtConfig.class);
    private boolean accountLockEnable;
    IdentityMgtService identityMgtService = new IdentityMgtServiceImpl();
    private boolean enableAuthPolicy;
    private boolean authPolicyAccountExistCheck;
    private int authPolicyMaxLoginAttempts;
    private int notificationExpireTime;

    public Properties addConfiguration(int tenantId) throws IdentityMgtException{

        Properties properties = null;
        try {
            properties = identityMgtService.addConfiguration(tenantId);
        } catch (IdentityMgtException ex) {
            String msg = "Error when adding configurations";
            log.error(msg);
            throw new IdentityMgtException(msg,ex);
        }
        return properties;
    }

    public Properties getConfiguration(int tenantId) throws IdentityMgtException{

        Properties properties = new Properties();
        Map<String, String> configMap = null;
        try {
            configMap = identityMgtService.getConfiguration(tenantId);
        } catch (IdentityMgtException ex) {
            String msg = "Error when retrieving configurations";
            log.error(msg);
            throw new IdentityMgtException(msg,ex);
        }
        Iterator<Map.Entry<String, String>> iterator = configMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> pair = iterator.next();
            properties.setProperty(pair.getKey(), pair.getValue());
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
        return properties;

    }

    public void setConfiguration(Properties properties) {

        if (!properties.isEmpty()) {

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

            String notificationExpireTimeProperty = properties.
                    getProperty(IdentityMgtConstants.PropertyConfig.NOTIFICATION_LINK_EXPIRE_TIME);
            if (notificationExpireTimeProperty != null) {
                this.notificationExpireTime = Integer.parseInt(notificationExpireTimeProperty.trim());
            }
        }
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

    public int getNotificationExpireTime() {
        return notificationExpireTime;
    }

}
