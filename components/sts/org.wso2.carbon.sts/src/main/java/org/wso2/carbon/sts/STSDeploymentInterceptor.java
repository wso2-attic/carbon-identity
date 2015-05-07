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
package org.wso2.carbon.sts;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.impl.AbstractIssuerConfig;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.apache.rahas.impl.TokenIssuerUtil;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.deployment.DeploymentInterceptor;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.core.util.KeyStoreUtil;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.security.keystore.service.KeyStoreData;
import org.wso2.carbon.security.util.RampartConfigUtil;
import org.wso2.carbon.security.util.ServerCrypto;
import org.wso2.carbon.sts.internal.STSServiceDataHolder;
import org.wso2.carbon.utils.ServerConstants;

import java.security.KeyStore;
import java.util.*;
import java.util.Map.Entry;

/**
 * This deployment interceptor will be called whenever STS service being deployed.
 *
 * @see AxisObserver
 */
public class STSDeploymentInterceptor implements AxisObserver {

    private static final Log log = LogFactory.getLog(DeploymentInterceptor.class);

    /**
     * Updates STS service during deployment
     *
     * @param config AxisConfiguration
     * @throws Exception
     */
    public static void updateSTSService(AxisConfiguration config) throws Exception {
        AxisService service = null;
        Registry configRegistry = null;
        Registry governRegistry = null;
        String keyPassword = null;
        KeyStoreAdmin admin = null;
        KeyStoreData[] keystores = null;
        String privateKeyAlias = null;
        String keyStoreName = null;
        String issuerName = null;
        ServerConfiguration serverConfig = null;

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        configRegistry = STSServiceDataHolder.getInstance().getRegistryService()
                .getConfigSystemRegistry(tenantId);
        governRegistry = STSServiceDataHolder.getInstance().getRegistryService()
                .getGovernanceSystemRegistry(tenantId);

        if (configRegistry == null || config.getService(ServerConstants.STS_NAME) == null) {
            if (log.isDebugEnabled()) {
                log.debug("configRegistry not set or STS service is unavailable");
            }
            return;
        }

        serverConfig = ServerConfiguration.getInstance();
        admin = new KeyStoreAdmin(tenantId, governRegistry);

        if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
            keyPassword = serverConfig.getFirstProperty("Security.KeyStore.KeyPassword");
            keystores = admin.getKeyStores(true);

            for (int i = 0; i < keystores.length; i++) {
                if (KeyStoreUtil.isPrimaryStore(keystores[i].getKeyStoreName())) {
                    keyStoreName = keystores[i].getKeyStoreName();
                    privateKeyAlias = KeyStoreUtil.getPrivateKeyAlias(KeyStoreManager.getInstance(
                            MultitenantConstants.SUPER_TENANT_ID)
                            .getKeyStore(keyStoreName));
                    break;
                }
            }
        } else {
            // this is not the proper way to find out the primary key store of the tenant. We need
            // check a better way  TODO
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if (tenantDomain == null) {
                tenantDomain = STSServiceDataHolder.getInstance().getRealmService().
                        getTenantManager().getDomain(tenantId);
            }

            if (tenantDomain != null) {
                // assuming domain always in this format -> example.com
                keyStoreName = tenantDomain.replace(".", "-") + ".jks";
                KeyStore keyStore = KeyStoreManager.getInstance(tenantId).getKeyStore(keyStoreName);
                if (keyStore != null) {
                    privateKeyAlias = KeyStoreUtil.getPrivateKeyAlias(keyStore);
                    keyPassword = KeyStoreManager.getInstance(tenantId).getKeyStorePassword(keyStoreName);
                } else {
                    log.warn("No key store is exist as " + keyStoreName + ". STS would be fail");
                }
            } else {
                throw new Exception("Tenant Domain can not be null");
            }

        }

        issuerName = serverConfig.getFirstProperty("HostName");

        if (issuerName == null) {
            // HostName not set :-( use wso2wsas-sts
            issuerName = ServerConstants.STS_NAME;
        }

        if (privateKeyAlias != null) {
            service = config.getService(ServerConstants.STS_NAME);

            String cryptoProvider = ServerCrypto.class.getName();

            Properties props = RampartConfigUtil.getServerCryptoProperties(
                    new String[]{keyStoreName}, keyStoreName, privateKeyAlias);

            SAMLTokenIssuerConfig stsSamlConfig = new SAMLTokenIssuerConfig(issuerName,
                    cryptoProvider, props);
            stsSamlConfig.setIssuerKeyAlias(privateKeyAlias);
            stsSamlConfig.setIssuerKeyPassword(keyPassword);
            stsSamlConfig.setAddRequestedAttachedRef(true);
            stsSamlConfig.setAddRequestedUnattachedRef(true);
            stsSamlConfig.setKeyComputation(2);
            stsSamlConfig.setProofKeyType(TokenIssuerUtil.BINARY_SECRET);

            String resourcePath = null;
            resourcePath = RegistryResources.SERVICE_GROUPS + ServerConstants.STS_NAME
                    + RegistryResources.SERVICES + ServerConstants.STS_NAME + "/trustedServices";
            if (configRegistry.resourceExists(resourcePath)) {
                Resource trustedService = null;
                Properties properties = null;
                Iterator iterator = null;
                trustedService = configRegistry.get(resourcePath);
                properties = trustedService.getProperties();
                if (properties != null && !properties.isEmpty()) {
                    iterator = properties.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Entry entry = (Entry) iterator.next();
                        if (RegistryUtils.isHiddenProperty(entry.getKey().toString())) {
                            continue;
                        }
                        stsSamlConfig.addTrustedServiceEndpointAddress((String) entry.getKey(),
                                (String) ((List) entry.getValue()).get(0));
                    }
                }
            }

            //Set the TTL value read from the carbon.xml
            String ttl = serverConfig.getFirstProperty("STSTimeToLive");

            if (ttl != null && ttl.length() > 0) {
                try {
                    stsSamlConfig.setTtl(Long.parseLong(ttl));
                    if (log.isDebugEnabled()) {
                        log.debug("STSTimeToLive read from carbon.xml " + ttl);
                    }
                } catch (NumberFormatException e) {
                    log.error("Error while reading STSTimeToLive from carbon.xml", e);
                }
            }
            //set if token store is disabled
            String tokenStoreDisabled = serverConfig.getFirstProperty("Security.DisableTokenStore");
            if (tokenStoreDisabled != null) {
                stsSamlConfig.setTokenStoreDisabled(Boolean.parseBoolean(tokenStoreDisabled));
            }
            //Set persister configuration reading from carbon.xml
            String persisterClassName = serverConfig.getFirstProperty(
                    "Security.TokenPersister.Class");
            String persistingFilePath = serverConfig.getFirstProperty(
                    "Security.TokenPersister.StoragePath");
            String inMemoryThreshold = serverConfig.getFirstProperty(
                    "Security.TokenPersister.InMemoryThreshold");

            if (persisterClassName != null) {
                stsSamlConfig.setPersisterClassName(persisterClassName);
            }
            Map<String, String> propertyMap = new HashMap<String, String>();
            if (persistingFilePath != null) {
                propertyMap.put(AbstractIssuerConfig.LOCAL_PROPERTY_STORAGE_PATH, persistingFilePath);
            }
            if (inMemoryThreshold != null) {
                propertyMap.put(AbstractIssuerConfig.LOCAL_PROPERTY_THRESHOLD, inMemoryThreshold);
            }
            if (log.isDebugEnabled()) {
                if (persisterClassName != null && inMemoryThreshold == null) {
                    log.debug("Although persister is defined, threshold not defined.");
                }
            }

            //allow defining any additional properties related to token persister.
            String[] persisterPropertyNames = serverConfig.getProperties(
                    "Security.TokenPersister.Properties.Property.Name");
            String[] persisterPropertyValues = serverConfig.getProperties(
                    "Security.TokenPersister.Properties.Property.Value");
            if (persisterPropertyNames.length != 0 && persisterPropertyValues.length != 0) {
                if (persisterPropertyNames.length == persisterPropertyValues.length) {
                    for (int i = 0; i < persisterPropertyNames.length; i++) {
                        propertyMap.put(persisterPropertyNames[i], persisterPropertyValues[i]);
                    }
                }
            }
            if (propertyMap.size() != 0) {
                stsSamlConfig.setPersisterPropertyMap(propertyMap);
            }

            try {
                // remove param is exists
                Parameter param = service.getParameter(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG
                        .getLocalPart());
                if (param == null) {
                    // Add new parameter
                    service.addParameter(stsSamlConfig.getParameter());
                }
            } catch (AxisFault e) {
                log.error("Error while updating " + ServerConstants.STS_NAME
                        + " in STSDeploymentInterceptor", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void serviceUpdate(AxisEvent event, AxisService service) {
        if (event.getEventType() == AxisEvent.SERVICE_DEPLOY
                && ServerConstants.STS_NAME.equals(service.getName())) {
            try {
                updateSTSService(service.getAxisConfiguration());
            } catch (Exception e) {
                log.error("Error while updating " + ServerConstants.STS_NAME
                        + " in STSDeploymentInterceptor", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(AxisConfiguration arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public void moduleUpdate(AxisEvent arg0, AxisModule arg1) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup group) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public void addParameter(Parameter arg0) throws AxisFault {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    public void deserializeParameters(OMElement arg0) throws AxisFault {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    public Parameter getParameter(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList getParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isParameterLocked(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removeParameter(Parameter arg0) throws AxisFault {
        // TODO Auto-generated method stub
    }
}