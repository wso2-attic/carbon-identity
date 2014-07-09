/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.certificateauthority.utils;

import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.certificateauthority.CaConfigurations;
import org.wso2.carbon.identity.certificateauthority.CaException;

import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CAUtils {

    public static X509Certificate getConfiguredCaCert() throws Exception {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        return getConfiguredCaCert(tenantId);
    }

    public static X509Certificate getConfiguredCaCert(int tenantId) throws Exception {
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantId);
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        String keyStore = getKeyStoreName();
        String alias = getAlias();

        if (keyStore == null) {
            if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
                return keyStoreManager.getDefaultPrimaryCertificate();
            } else {
                String ksName = tenantDomain.trim().replace(".", "-");
                String jksName = ksName + ".jks";
                return (X509Certificate) keyStoreManager.getKeyStore(jksName).getCertificate(tenantDomain);
            }
        }
        return (X509Certificate) keyStoreManager.getKeyStore(keyStore).getCertificate(alias);
    }

    public static PrivateKey getConfiguredPrivateKey() throws Exception {
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        return getConfiguredPrivateKey(tenantID);
    }

    public static PrivateKey getConfiguredPrivateKey(int tenantId) throws Exception {
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantId);
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        String keystore = getKeyStoreName();
        String alias = getAlias();

        if (keystore == null) {
            if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
                return keyStoreManager.getDefaultPrivateKey();
            } else {
                String ksName = tenantDomain.trim().replace(".", "-");
                String jksName = ksName + ".jks";
                return (PrivateKey) keyStoreManager.getPrivateKey(jksName, tenantDomain);
            }
        }
        Key privateKey = keyStoreManager.getPrivateKey(keystore, alias);
        return (PrivateKey) privateKey;
    }

    public static String getKeyStoreName() throws CaException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        return CaConfigurations.getKeyStoreName(tenantId);
    }

    public static String getAlias() throws CaException {
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        return CaConfigurations.getAlias(tenantId);
    }

    public static String getServerURL() {
        return "http://" + ServerUtils.getServerHost() + ":" + ServerUtils.getHttpPort();
    }
}
