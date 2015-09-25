/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.UUIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisBinding;
import org.apache.axis2.description.AxisEndpoint;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rahas.impl.AbstractIssuerConfig;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.apache.rahas.impl.TokenIssuerUtil;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.deployment.DeploymentInterceptor;
import org.wso2.carbon.core.persistence.PersistenceUtils;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.core.util.KeyStoreUtil;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.security.SecurityConfigException;
import org.wso2.carbon.security.SecurityConstants;
import org.wso2.carbon.security.SecurityScenario;
import org.wso2.carbon.security.SecurityScenarioDatabase;
import org.wso2.carbon.security.config.SecurityServiceAdmin;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.security.keystore.service.KeyStoreData;
import org.wso2.carbon.security.pox.POXSecurityHandler;
import org.wso2.carbon.security.util.RampartConfigUtil;
import org.wso2.carbon.security.util.ServerCrypto;
import org.wso2.carbon.security.util.ServicePasswordCallbackHandler;
import org.wso2.carbon.sts.internal.STSServiceDataHolder;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.ServerException;
import org.wso2.carbon.utils.deployment.GhostDeployerUtils;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * This deployment interceptor will be called whenever STS service being deployed.
 *
 * @see AxisObserver
 */
public class STSDeploymentInterceptor implements AxisObserver {

    private static final Log log = LogFactory.getLog(DeploymentInterceptor.class);

    public static final String HOST_NAME = "HostName";
    public static final String STS_TIME_TO_LIVE = "STSTimeToLive";
    public static final String SECURITY_DISABLE_TOKEN_STORE = "Security.DisableTokenStore";
    public static final String SECURITY_KEY_STORE_KEY_PASSWORD = "Security.KeyStore.KeyPassword";
    public static final String SECURITY_TOKEN_PERSISTER_CLASS = "Security.TokenPersister.Class";
    public static final String SECURITY_TOKEN_PERSISTER_STORAGE_PATH = "Security.TokenPersister.StoragePath";
    public static final String SECURITY_TOKEN_PERSISTER_IN_MEMORY_THRESHOLD =
            "Security.TokenPersister.InMemoryThreshold";
    public static final String SECURITY_TOKEN_PERSISTER_PROPERTIES_PROPERTY_NAME =
            "Security.TokenPersister.Properties.Property.Name";
    public static final String SECURITY_TOKEN_PERSISTER_PROPERTIES_PROPERTY_VALUE =
            "Security.TokenPersister.Properties.Property.Value";

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

        configRegistry = STSServiceDataHolder.getInstance().getRegistryService().getConfigSystemRegistry(tenantId);
        governRegistry = STSServiceDataHolder.getInstance().getRegistryService().getGovernanceSystemRegistry(tenantId);

        if (configRegistry == null || config.getService(ServerConstants.STS_NAME) == null) {
            if (log.isDebugEnabled()) {
                log.debug("configRegistry not set or STS service is unavailable");
            }
            return;
        }

        serverConfig = ServerConfiguration.getInstance();
        admin = new KeyStoreAdmin(tenantId, governRegistry);

        if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
            keyPassword = serverConfig.getFirstProperty(SECURITY_KEY_STORE_KEY_PASSWORD);
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

        issuerName = serverConfig.getFirstProperty(HOST_NAME);

        if (issuerName == null) {
            // HostName not set :-( use wso2wsas-sts
            issuerName = ServerConstants.STS_NAME;
        }

        if (privateKeyAlias != null) {
            service = config.getService(ServerConstants.STS_NAME);

            String cryptoProvider = ServerCrypto.class.getName();

            Properties props = RampartConfigUtil.getServerCryptoProperties(
                    new String[] { keyStoreName }, keyStoreName, privateKeyAlias);

            SAMLTokenIssuerConfig stsSamlConfig = new SAMLTokenIssuerConfig(issuerName, cryptoProvider, props);
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
            String ttl = serverConfig.getFirstProperty(STS_TIME_TO_LIVE);

            if (StringUtils.isNotBlank(ttl)) {
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
            String tokenStoreDisabled = serverConfig.getFirstProperty(SECURITY_DISABLE_TOKEN_STORE);
            if (tokenStoreDisabled != null) {
                stsSamlConfig.setTokenStoreDisabled(Boolean.parseBoolean(tokenStoreDisabled));
            }
            //Set persister configuration reading from carbon.xml
            String persisterClassName = serverConfig.getFirstProperty(SECURITY_TOKEN_PERSISTER_CLASS);
            String persistingFilePath = serverConfig.getFirstProperty(SECURITY_TOKEN_PERSISTER_STORAGE_PATH);
            String inMemoryThreshold = serverConfig.getFirstProperty(SECURITY_TOKEN_PERSISTER_IN_MEMORY_THRESHOLD);

            if (persisterClassName != null) {
                stsSamlConfig.setPersisterClassName(persisterClassName);
            }
            Map<String, String> propertyMap = new HashMap<>();
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
                    SECURITY_TOKEN_PERSISTER_PROPERTIES_PROPERTY_NAME);
            String[] persisterPropertyValues = serverConfig.getProperties(
                    SECURITY_TOKEN_PERSISTER_PROPERTIES_PROPERTY_VALUE);
            if (!ArrayUtils.isEmpty(persisterPropertyNames) && !ArrayUtils.isEmpty(persisterPropertyValues) &&
                persisterPropertyNames.length == persisterPropertyValues.length) {
                for (int i = 0; i < persisterPropertyNames.length; i++) {
                    propertyMap.put(persisterPropertyNames[i], persisterPropertyValues[i]);
                }
            }
            if (!propertyMap.isEmpty()) {
                stsSamlConfig.setPersisterPropertyMap(propertyMap);
            }

            try {
                // remove param is exists
                Parameter param = service.getParameter(SAMLTokenIssuerConfig.SAML_ISSUER_CONFIG.getLocalPart());
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
    @Override
    public void serviceUpdate(AxisEvent event, AxisService service) {
        if (event.getEventType() == AxisEvent.SERVICE_DEPLOY
            && ServerConstants.STS_NAME.equals(service.getName())) {
            try {
                applyPolicy(service);
                updateSTSService(service.getAxisConfiguration());
            } catch (Exception e) {
                log.error("Error while updating " + ServerConstants.STS_NAME
                          + " in STSDeploymentInterceptor", e);
            }
        }
    }

    private void applyPolicy(AxisService service) throws SecurityConfigException {
        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            Registry configRegistry = STSServiceDataHolder.getInstance().getRegistryService()
                    .getConfigSystemRegistry(tenantId);
            String servicePath = getRegistryServicePath(service);
            String policyResourcePath = servicePath + RegistryResources.POLICIES;
            if (configRegistry.resourceExists(policyResourcePath)) {
                Resource resource = configRegistry.get(policyResourcePath);
                if (resource instanceof Collection) {
                    for (String policyPath : ((Collection) resource).getChildren()) {
                        Resource res = configRegistry.get(policyPath);
                        Policy policy = loadPolicy(res);
                        applyPolicy(service,policy);
                        //addSecurityPolicyToAllBindings(service,policy);
                        //service.getPolicySubject().attachPolicy(policy);
                    }
                }
            }
        } catch (RegistryException e) {
            log.error("Error occurred while persisting policy", e);
        } catch (XMLStreamException e) {
            log.error("Error occurred while persisting policy", e);
        }
    }

    private Policy loadPolicy(Resource resource) throws RegistryException, XMLStreamException {

        InputStream in = resource.getContentStream();
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
        StAXOMBuilder builder = new StAXOMBuilder(parser);

        OMElement policyElement = builder.getDocumentElement();
        return PolicyEngine.getPolicy(policyElement);

    }

    private String getRegistryServicePath(AxisService service) {

        StringBuilder pathValue = new StringBuilder();
        return (pathValue
                .append(RegistryResources.SERVICE_GROUPS)
                .append(service.getAxisServiceGroup().getServiceGroupName())
                .append(RegistryResources.SERVICES)
                .append(service.getName())).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(AxisConfiguration arg0) {
        // Nothing to implement
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moduleUpdate(AxisEvent arg0, AxisModule arg1) {
        // Nothing to implement
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup group) {
        // Nothing to implement
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addParameter(Parameter arg0) throws AxisFault {
        // Nothing to implement

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deserializeParameters(OMElement arg0) throws AxisFault {
        // Nothing to implement
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter getParameter(String arg0) {
        // Nothing to return
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList getParameters() {
        // Nothing to return
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParameterLocked(String arg0) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeParameter(Parameter arg0) throws AxisFault {
        // Nothing to implement
    }

    public void addSecurityPolicyToAllBindings(AxisService axisService, Policy policy)
            throws ServerException {
        try {
            if (policy.getId() == null) {
                // Generate an ID
                policy.setId(UUIDGenerator.getUUID());
            }
            Map endPointMap = axisService.getEndpoints();
            for (Object o : endPointMap.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                AxisEndpoint point = (AxisEndpoint) entry.getValue();
                AxisBinding binding = point.getBinding();
                String bindingName = binding.getName().getLocalPart();

                //only UTOverTransport is allowed for HTTP
                if (bindingName.endsWith("HttpBinding") &&
                        (!policy.getAttributes().containsValue("UTOverTransport"))) {
                    continue;
                }
                binding.getPolicySubject().attachPolicy(policy);
                // Add the new policy to the registry
            }
        } catch (Exception e) {
            log.error("Error in adding security policy to all bindings", e);
            throw new ServerException("addPoliciesToService", e);
        }
    }

    private void applyPolicy (AxisService service, Policy policy) throws SecurityConfigException {

        if(service == null || policy == null){
            throw new SecurityConfigException("Error while applying policy to service. Service and policy must be " +
                    "present to apply policy");
        }

        UserRealm userRealm = (UserRealm) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getUserRealm();

        UserRegistry govRegistry = (UserRegistry) PrivilegedCarbonContext
                .getThreadLocalCarbonContext().getRegistry(RegistryType.SYSTEM_GOVERNANCE);
        String policyId = policy.getId();
        SecurityScenario securityScenario = SecurityScenarioDatabase.getByWsuId(policyId);
        //this.disableSecurityOnService(service.getName());
        disableRESTCalls(service, service.getName(), securityScenario.getScenarioId());

        if (GhostDeployerUtils.isGhostService(service)) {
            try {
                service = GhostDeployerUtils.deployActualService(service.getAxisConfiguration(), service);
            } catch (AxisFault axisFault) {
                log.error("Error while loading actual service from Ghost", axisFault);
            }
        }
        // Engage required modules.
        engageModules(securityScenario.getScenarioId(), service.getName(), service);

        try {
            String serviceGroupId = service.getAxisServiceGroup().getServiceGroupName();
            CallbackHandler handler;
            // This will break kerberos from management console UI
             handler = new ServicePasswordCallbackHandler(null, serviceGroupId, service.getName(),govRegistry, userRealm);
            Parameter param = new Parameter();
            param.setName(WSHandlerConstants.PW_CALLBACK_REF);
            param.setValue(handler);
            service.addParameter(param);

            this.getPOXCache().remove(service.getName());
            Cache<String, String> cache = getPOXCache();
            if (cache != null) {
                cache.remove(service.getName());
            }

            //Adding the security scenario ID parameter to the axisService
            //This parameter can be used to get the applied security scenario
            //without reading the service meta data file.
            try {
                Parameter params = new Parameter();
                params.setName(SecurityConstants.SCENARIO_ID_PARAM_NAME);
                params.setValue(securityScenario.getScenarioId());
                service.addParameter(params);
            } catch (AxisFault axisFault) {
                log.error("Error while adding Scenario ID parameter", axisFault);
            }

            addSecurityPolicyToAllBindings(service, policy);
        } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
            throw new SecurityConfigException("Error occurred while creating callback handler", e);
        } catch (AxisFault e) {
            throw new SecurityConfigException("Error occurred while adding callback parameter", e);
        } catch (ServerException e) {
            throw new SecurityConfigException("Error while adding policy to bindings", e);
        }
    }

    protected void disableRESTCalls(AxisService service, String serviceName, String scenrioId)
            throws SecurityConfigException {

        if (scenrioId.equals(SecurityConstants.USERNAME_TOKEN_SCENARIO_ID)) {
            return;
        }
        try {
            if (service == null) {
                throw new SecurityConfigException("nullService");
            }

            Parameter param = new Parameter();
            param.setName("disableREST"); // TODO Find the constant
            param.setValue(Boolean.TRUE.toString());
            service.addParameter(param);

        } catch (AxisFault e) {
            log.error(e);
            throw new SecurityConfigException("disablingREST", e);
        }

    }

    protected void engageModules(String scenarioId, String serviceName, AxisService axisService)
            throws SecurityConfigException {
        SecurityScenario securityScenario = SecurityScenarioDatabase.get(scenarioId);
        String[] moduleNames = (String[]) securityScenario.getModules()
                .toArray(new String[securityScenario.getModules().size()]);
        // handle each module required
        try {

            for (String modName : moduleNames) {
                AxisModule module = axisService.getAxisConfiguration().getModule(modName);
                // engage at axis2
                axisService.disengageModule(module);
                axisService.engageModule(module);
            }

        } catch (AxisFault e) {
            log.error(e);
            throw new SecurityConfigException("Error in engaging modules", e);
        }
    }

    private Cache<String, String> getPOXCache() {
        CacheManager manager = Caching.getCacheManagerFactory().getCacheManager(POXSecurityHandler.POX_CACHE_MANAGER);
        Cache<String, String> cache = manager.getCache(POXSecurityHandler.POX_ENABLED);
        return cache;
    }

}