/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used store the SSO Service Provider information used in the System level, i.e. across
 * all tenants. This class publishes an OSGi Service and anyone can use that service to publish Service
 * Provider info.
 * This class is used in Stratos when SSO is enabled to the complete system.
 */
public enum SSOServiceProviderConfigManager {

    INSTANCE;

    private static Log log = LogFactory.getLog(SSOServiceProviderConfigManager.class);

    // This map is used to store the service provider info against the issuer name
    private ConcurrentHashMap<String, SAMLSSOServiceProviderDO> serviceProviderMap;

    private SSOServiceProviderConfigManager() {
        this.serviceProviderMap = new ConcurrentHashMap<String, SAMLSSOServiceProviderDO>();
    }

    public static SSOServiceProviderConfigManager getInstance() {

        return INSTANCE;
    }

    /**
     * Add Service Providers to the list
     *
     * @param issuerName issuer name used by the service provider
     * @param spDO       SAMLSSOServiceProviderDO bean representing the Service Provider
     */
    public void addServiceProvider(String issuerName, SAMLSSOServiceProviderDO spDO) {
        if (serviceProviderMap.containsKey(issuerName)) {
            log.warn("Duplicate Service Providers detected.");
            return;
        } else {
            serviceProviderMap.put(issuerName, spDO);
            if (log.isDebugEnabled()) {
                log.debug("A Service Provider is added to the Service Provider Map with the " +
                        "issuer name : " + issuerName);
            }
        }
    }

    /**
     * Get the Service Provider with the given issuer name
     *
     * @param issuerName issuer name
     * @return SAMLSSOServiceProviderDO bean representing the Service Provider
     */
    public SAMLSSOServiceProviderDO getServiceProvider(String issuerName) {
        return serviceProviderMap.get(issuerName);
    }

    /**
     * Get all the SAMLSSOServiceProviderDO objects which are registered through the OSGi service.
     *
     * @return Enumeration of SAMLSSOServiceProviderDO objects
     */
    public Enumeration<SAMLSSOServiceProviderDO> getAllServiceProviders() {
        return serviceProviderMap.elements();
    }

}
