/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.config.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Encapsulates identity management config data.
 */
public class IdentityMgtConfigurationBuilder {

    private static final Log LOG = LogFactory.getLog(IdentityMgtConfigurationBuilder.class);
    private static final String CONFIG_FILE_NAME = "identity-mgt.properties";
    private static final String AUTH_POLICY_ACCOUNT_LOCK_PROPERTY = "Authentication.Policy.Check.Account.Lock";
    private static final String AUTH_POLICY_ACCOUNT_DISABLE_PROPERTY = "Authentication.Policy.Check.Account.Disable";
    private static volatile IdentityMgtConfigurationBuilder identityMgtConfigurationBuilder =
            new IdentityMgtConfigurationBuilder();
    private boolean authPolicyAccountLockCheck;
    private boolean authPolicyAccountDisableCheck;

    private IdentityMgtConfigurationBuilder() {

        InputStream inStream = null;

        File identityMgtProperties = new File(IdentityUtil.getIdentityConfigDirPath(), CONFIG_FILE_NAME);
        Properties properties = new Properties();
        if (identityMgtProperties.exists()) {
            try {
                inStream = new FileInputStream(identityMgtProperties);
                properties.load(inStream);
            } catch (IOException e) {
                LOG.error("Can not load identity-mgt properties file.", e);
            } finally {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        LOG.error("Error while closing stream.", e);
                    }
                }
            }
        }

        String authPolicyAccountLockCheckProperty = properties.getProperty(AUTH_POLICY_ACCOUNT_LOCK_PROPERTY);
        if (authPolicyAccountLockCheckProperty != null) {
            this.authPolicyAccountLockCheck = Boolean.parseBoolean(authPolicyAccountLockCheckProperty.trim());
        }

        String authPolicyAccountDisableCheckProperty = properties.getProperty(AUTH_POLICY_ACCOUNT_DISABLE_PROPERTY);
        if (authPolicyAccountDisableCheckProperty != null) {
            this.authPolicyAccountDisableCheck = Boolean.parseBoolean(authPolicyAccountDisableCheckProperty.trim());
        }
    }

    public static IdentityMgtConfigurationBuilder getInstance() {

        return identityMgtConfigurationBuilder;
    }

    public boolean isAuthPolicyAccountLockCheck() {

        return authPolicyAccountLockCheck;
    }

    public boolean isAuthPolicyAccountDisableCheck() {

        return authPolicyAccountDisableCheck;
    }
}