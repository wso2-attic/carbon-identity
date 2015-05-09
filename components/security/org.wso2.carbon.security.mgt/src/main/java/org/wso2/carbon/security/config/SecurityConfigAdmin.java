/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.security.config;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyEngine;
import org.apache.neethi.PolicyReference;
import org.apache.rampart.policy.RampartPolicyBuilder;
import org.apache.rampart.policy.RampartPolicyData;
import org.apache.rampart.policy.model.CryptoConfig;
import org.apache.rampart.policy.model.KerberosConfig;
import org.apache.rampart.policy.model.RampartConfig;
import org.apache.ws.secpolicy.WSSPolicyException;
import org.apache.ws.secpolicy.model.SecureConversationToken;
import org.apache.ws.secpolicy.model.Token;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.core.util.KeyStoreUtil;
import org.wso2.carbon.registry.core.Association;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.utils.Transaction;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.security.*;
import org.wso2.carbon.security.config.service.KerberosConfigData;
import org.wso2.carbon.security.config.service.SecurityConfigData;
import org.wso2.carbon.security.config.service.SecurityScenarioData;
import org.wso2.carbon.security.pox.POXSecurityHandler;
import org.wso2.carbon.security.util.*;
import org.wso2.carbon.user.core.AuthorizationManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.ServerException;
import org.wso2.carbon.utils.deployment.GhostDeployerUtils;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.security.KeyStore;
import java.util.*;


/**
 * Admin service for configuring Security scenarios
 */
public class SecurityConfigAdmin {

    public static final String USER = "rampart.config.user";
    private static Log log = LogFactory.getLog(SecurityConfigAdmin.class);
    private AxisConfiguration axisConfig = null;
    private CallbackHandler callback = null;
    private Registry registry = null;
    private UserRegistry govRegistry = null;
    private UserRealm realm = null;

    public SecurityConfigAdmin(AxisConfiguration config) throws SecurityConfigException {

        this.axisConfig = config;
        try {
            this.registry = SecurityServiceHolder.getRegistry();
            this.govRegistry = SecurityServiceHolder.getRegistryService().getGovernanceSystemRegistry();
        } catch (Exception e) {
            String msg = "Error when retrieving a registry instance";
            log.error(msg, e);
            throw new SecurityConfigException(msg, e);
        }
    }

    public SecurityConfigAdmin(AxisConfiguration config, Registry reg, CallbackHandler cb) {

        this.axisConfig = config;
        this.registry = reg;
        this.callback = cb;

        try {
            this.registry = SecurityServiceHolder.getRegistry();
            this.govRegistry = SecurityServiceHolder.getRegistryService().getGovernanceSystemRegistry(
                    ((UserRegistry) reg).getTenantId());
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            log.error("Error when obtaining the registry instance.", e);
        }
    }

    public SecurityConfigAdmin(UserRealm realm, Registry registry, AxisConfiguration config)
            throws SecurityConfigException {

        this.axisConfig = config;
        this.registry = registry;
        this.realm = realm;

        try {
            this.registry = SecurityServiceHolder.getRegistry();
        } catch (Exception e) {
            log.error("Error creating an PersistenceFactory instance", e);
            throw new SecurityConfigException("Error creating an PersistenceFactory instance", e);
        }
        try {
            this.govRegistry = SecurityServiceHolder.getRegistryService().getGovernanceSystemRegistry(
                    ((UserRegistry) registry).getTenantId());
        } catch (Exception e) {
            String error = "Error when obtaining the governance registry instance.";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }
    }

    /**
     * Get security scenario data
     *
     * @param sceneId Scenario Id
     * @return Security Scenario Data
     * @throws SecurityConfigException
     */
    public SecurityScenarioData getSecurityScenario(String sceneId) throws SecurityConfigException {

        SecurityScenarioData data = null;
        SecurityScenario scenario = SecurityScenarioDatabase.get(sceneId);
        if (scenario != null) {
            data = new SecurityScenarioData();
            data.setCategory(scenario.getCategory());
            data.setDescription(scenario.getDescription());
            data.setScenarioId(scenario.getScenarioId());
            data.setSummary(scenario.getSummary());
        }
        return data;
    }

    /**
     * Get current scenario
     *
     * @param serviceName Axis service name
     * @return Security Scenario Data
     * @throws SecurityConfigException
     */
    public SecurityScenarioData getCurrentScenario(String serviceName) throws SecurityConfigException {

        try {
            SecurityScenarioData data = null;
            AxisService service = axisConfig.getServiceForActivation(serviceName);
            if (service == null) {
                try {
                    service = GhostDeployerUtils.getTransitGhostServicesMap(axisConfig).get(serviceName);
                } catch (AxisFault axisFault) {
                    log.error("Error while reading Transit Ghosts map", axisFault);
                }
                if (service == null) {
                    throw new SecurityConfigException("AxisService is Null for service name : " + serviceName);
                }
            }
            String policyResourcePath = ServicePersistenceUtil.getResourcePath(service) + RegistryResources.POLICIES;
            if (!registry.resourceExists(policyResourcePath)) {
                return data;
            }
            /**
             * First check whether there's a custom policy engaged from registry. If it is not
             * the case, we check whether a default scenario is applied.
             */
            Parameter param = service.getParameter(SecurityConstants.SECURITY_POLICY_PATH);
            if (param != null) {
                data = new SecurityScenarioData();
                data.setPolicyRegistryPath((String) param.getValue());
                data.setScenarioId(SecurityConstants.POLICY_FROM_REG_SCENARIO);
            } else {
                SecurityScenario scenario = readCurrentScenario(serviceName);
                if (scenario != null) {
                    data = new SecurityScenarioData();
                    data.setCategory(scenario.getCategory());
                    data.setDescription(scenario.getDescription());
                    data.setScenarioId(scenario.getScenarioId());
                    data.setSummary(scenario.getSummary());
                }
            }
            return data;

        } catch (RegistryException e) {
            String error = "Error occurred while reading resource from config registry";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }
    }

    /**
     * Disable security policy
     *
     * @param serviceName Axis service name
     * @throws SecurityConfigException
     */
    public void disableSecurityOnService(String serviceName) throws SecurityConfigException {

        try {
            AxisService service = axisConfig.getServiceForActivation(serviceName);
            if (service == null) {
                throw new SecurityConfigException("AxisService is null for service name : " + serviceName);
            }
            String servicePath = ServicePersistenceUtil.getResourcePath(service);
            String policyResourcePath = ServicePersistenceUtil.getResourcePath(service) + RegistryResources.POLICIES;

            if (log.isDebugEnabled()) {
                log.debug("Removing security service policy : " + policyResourcePath);
            }
            if (!registry.resourceExists(policyResourcePath)) {
                return;
            }
            SecurityScenario scenario = readCurrentScenario(serviceName);
            if (scenario == null) {
                return;
            }

            String secPolicyPath = servicePath + RegistryResources.POLICIES + scenario.getWsuId();
            if (registry.resourceExists(secPolicyPath)) {
                registry.delete(secPolicyPath);
            }

            String[] moduleNames = scenario.getModules().toArray(new String[scenario.getModules().size()]);

            //Disengaging modules
            for (String moduleName : moduleNames) {
                AxisModule module = service.getAxisConfiguration().getModule(moduleName);
                service.disengageModule(module);

                String modPath = RegistryResources.MODULES + module.getName() + "/" + module.getVersion();
                registry.removeAssociation(servicePath, modPath, RegistryResources.Associations.ENGAGED_MODULES);
            }

            //Removing policy
            SecurityServiceAdmin admin = new SecurityServiceAdmin(axisConfig, registry);
            admin.removeSecurityPolicyFromAllBindings(service, scenario.getWsuId());

            String scenarioId = scenario.getScenarioId();
            String resourceUri = SecurityConstants.SECURITY_POLICY + "/" + scenarioId;

            //Removing persist data in registry
            try {
                boolean transactionStarted = Transaction.isStarted();
                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                registry.removeAssociation(resourceUri, servicePath,
                        SecurityConstants.ASSOCIATION_SERVICE_SECURING_POLICY);
                AuthorizationManager authorizationManager = realm.getAuthorizationManager();

                String[] roles = authorizationManager
                        .getAllowedRolesForResource(servicePath, UserCoreConstants.INVOKE_SERVICE_PERMISSION);
                for (int i = 0; i < roles.length; i++) {
                    authorizationManager
                            .clearRoleAuthorization(roles[i], servicePath, UserCoreConstants.INVOKE_SERVICE_PERMISSION);
                }

                Association[] keystores = registry
                        .getAssociations(RegistryConstants.CONFIG_REGISTRY_BASE_PATH + servicePath,
                                SecurityConstants.ASSOCIATION_PRIVATE_KEYSTORE);
                for (int i = 0; i < keystores.length; i++) {
                    registry.removeAssociation(RegistryConstants.CONFIG_REGISTRY_BASE_PATH + servicePath,
                            keystores[i].getDestinationPath(), SecurityConstants.ASSOCIATION_PRIVATE_KEYSTORE);
                }

                Association[] trustedkeystores = registry.getAssociations(RegistryConstants.CONFIG_REGISTRY_BASE_PATH +
                                servicePath, SecurityConstants.ASSOCIATION_TRUSTED_KEYSTORE);
                for (int i = 0; i < trustedkeystores.length; i++) {
                    registry.removeAssociation(RegistryConstants.CONFIG_REGISTRY_BASE_PATH + servicePath,
                            trustedkeystores[i].getDestinationPath(), SecurityConstants.ASSOCIATION_TRUSTED_KEYSTORE);
                }
                //Remove the policy path parameter if it is set..
                String paramPath = servicePath + RegistryResources.PARAMETERS + SecurityConstants.SECURITY_POLICY_PATH;
                if (registry.resourceExists(paramPath)) {
                    registry.delete(paramPath);
                }
                if (!transactionStarted) {
                    registry.commitTransaction();
                }
            } catch (RegistryException e) {
                try {
                    registry.rollbackTransaction();
                } catch (RegistryException ex) {
                    log.error("Error occurred while rolling back transaction.", ex);
                }
                String msg = "Error occurred while to removing data from registry for service " + serviceName;
                log.error(msg, e);
                throw new SecurityConfigException(msg, e);
            }

            Parameter param = new Parameter();
            param.setName(WSHandlerConstants.PW_CALLBACK_REF);
            service.removeParameter(param);

            Parameter param2 = new Parameter();
            param2.setName("disableREST"); // TODO Find the constant
            service.removeParameter(param2);

            Parameter pathParam = service.getParameter(SecurityConstants.SECURITY_POLICY_PATH);
            String policyPath = null;
            if (pathParam != null) {
                policyPath = (String) pathParam.getValue();
                service.removeParameter(pathParam);
            }

            //Unlock transports
            Policy policy = loadPolicy(scenarioId, policyPath);
            if (isHttpsTransportOnly(policy)) {
                try {
                    boolean transactionStarted = Transaction.isStarted();
                    if (!transactionStarted) {
                        registry.beginTransaction();
                    }
                    Resource resource = registry.get(servicePath);
                    resource.removeProperty(RegistryResources.ServiceProperties.IS_UT_ENABLED);
                    List<String> transports = getAllTransports();
                    setServiceTransports(serviceName, transports);

                    //Trigger the transport binding added event
                    AxisEvent event = new AxisEvent(CarbonConstants.AxisEvent.TRANSPORT_BINDING_ADDED, service);
                    axisConfig.notifyObservers(event, service);

                    resource.setProperty(RegistryResources.ServiceProperties.EXPOSED_ON_ALL_TANSPORTS,
                            Boolean.TRUE.toString());

                    for (String trans : transports) {
                        if (trans.endsWith("https")) {
                            continue;
                        }
                        String transPath = RegistryResources.TRANSPORTS + trans;
                        if (registry.resourceExists(transPath)) {
                            registry.addAssociation(servicePath, transPath,
                                    RegistryResources.Associations.EXPOSED_TRANSPORTS);
                        } else {
                            String msg = "Transport path " + transPath + " does not exist in the registry";
                            log.error(msg);
                            throw new SecurityConfigException(msg);
                        }
                    }

                    registry.put(resource.getPath(), resource);
                    if (!transactionStarted) {
                        registry.commitTransaction();
                    }

                } catch (RegistryException e) {
                    try {
                        registry.rollbackTransaction();
                    } catch (RegistryException ex) {
                        log.error("Error occurred while rolling back transaction.", ex);
                    }
                    String msg = "Service with name " + serviceName + " not found.";
                    log.error(msg, e);
                    throw new SecurityConfigException(msg, e);
                }
            }
        } catch (AxisFault e) {
            String error = "Error occurred while disabling security in service " + serviceName;
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        } catch(UserStoreException ex){
            String error = "Error occurred while removing roles from authorizationManager in service " + serviceName;
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        } catch(RegistryException ex){
            String error = "Error occurred while removing data from registry in service " + serviceName;
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        } catch(ServerException ex){
            String error = "Error occurred while removing security policy from all bindings in service " + serviceName;
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        }
    }

    /**
     * Read Kerberos configurations
     *
     * @param service Axis service name
     * @return Kerberos configuration data
     * @throws SecurityConfigException
     */
    private KerberosConfigData readKerberosConfigurations(AxisService service) throws SecurityConfigException {

        String kerberosPath = getKerberosConfigPath(service);
        try {
            if (!this.registry.resourceExists(kerberosPath)) {
                return null;
            }
            KerberosConfigData kerberosConfigData = new KerberosConfigData();

            String servicePrincipalResource = kerberosPath + "/" + KerberosConfig.SERVICE_PRINCIPLE_NAME;
            kerberosConfigData.setServicePrincipleName(getRegistryProperty(servicePrincipalResource,
                    KerberosConfig.SERVICE_PRINCIPLE_NAME));

            String servicePrincipalPasswordResource = kerberosPath + "/" + KerberosConfig.SERVICE_PRINCIPLE_PASSWORD;
            String encryptedString = getRegistryProperty(servicePrincipalPasswordResource,
                    KerberosConfig.SERVICE_PRINCIPLE_PASSWORD);

            CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
            try {
                kerberosConfigData.setServicePrinciplePassword
                        (new String(cryptoUtil.base64DecodeAndDecrypt(encryptedString)));
            } catch (CryptoException e) {
                String msg = "Unable to decode and decrypt password string.";
                log.error(msg, e);
            }
            return kerberosConfigData;

        } catch (RegistryException e) {
            String msg = "An error occurred while retrieving kerberos configuration data for service " +
                    service.getName();
            log.error(msg, e);
            throw new SecurityConfigException(msg);
        }
    }

    /**
     * Get registry property
     *
     * @param registryPath Registry path
     * @param name         Property name
     * @return Registry resource property
     * @throws RegistryException
     */
    private String getRegistryProperty(String registryPath, String name) throws RegistryException {

        Resource resource = this.registry.get(registryPath);
        if (resource != null) {
            String propertyValue = resource.getProperty(name);
            if (propertyValue != null) {
                return propertyValue;
            }
        }
        log.warn("Could not find registry value for property " + name + " in registry path " + registryPath);
        return null;
    }

    /**
     * Get Kerberos configuration path
     * @param service Axis service name
     * @return Kerberos configuration path
     */
    private String getKerberosConfigPath(AxisService service) {
        return getServicePath(service) + "/" + RampartConfigUtil.KERBEROS_CONFIG_RESOURCE;
    }

    /**
     * Persist Kerberos configuration data
     *
     * @param service
     * @param kerberosConfigData
     * @throws SecurityConfigException
     */
    protected void persistsKerberosData(AxisService service, KerberosConfigData kerberosConfigData)
            throws SecurityConfigException {

        String kerberosPath = getKerberosConfigPath(service);
        try {

            //Checking whether registry path already exists
            if (registry.resourceExists(kerberosPath)) {
                registry.delete(kerberosPath);
            }

            org.wso2.carbon.registry.core.Collection collection = registry.newCollection();
            registry.put(kerberosPath, collection);

            String servicePrincipalResource = kerberosPath + "/" + KerberosConfig.SERVICE_PRINCIPLE_NAME;
            addRegistryResource(servicePrincipalResource, KerberosConfig.SERVICE_PRINCIPLE_NAME,
                    kerberosConfigData.getServicePrincipleName());

            String servicePrincipalPasswordResource = kerberosPath + "/" + KerberosConfig.SERVICE_PRINCIPLE_PASSWORD;
            addRegistryResource(servicePrincipalPasswordResource, KerberosConfig.SERVICE_PRINCIPLE_PASSWORD,
                    getEncryptedPassword(kerberosConfigData.getServicePrinciplePassword()));

        } catch (RegistryException e) {
            String error = "Error adding kerberos parameters to registry.";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }
    }

    /**
     * Add resource to registry
     *
     * @param registryPath Registry path
     * @param name         Property name
     * @param value        Property value
     * @throws RegistryException
     */
    private void addRegistryResource(String registryPath, String name, String value) throws RegistryException {

        org.wso2.carbon.registry.core.Resource resource = registry.newResource();
        resource.setProperty(name, value);
        registry.put(registryPath, resource);
    }

    /**
     * Get encrypted password
     *
     * @param password Password
     * @return Encrypted password
     * @throws SecurityConfigException
     */
    private String getEncryptedPassword(String password) throws SecurityConfigException {

        CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
        try {
            return cryptoUtil.encryptAndBase64Encode(password.getBytes());
        } catch (CryptoException e) {
            String msg = "Unable to encrypt and encode password string.";
            log.error(msg, e);
            throw new SecurityConfigException(msg, e);
        }
    }

    /**
     * Get registry service path
     *
     * @param service Axis service
     * @return Registry service path
     */
    private String getRegistryServicePath(AxisService service) {

        return RegistryResources.SERVICE_GROUPS + service.getAxisServiceGroup().getServiceGroupName() +
                RegistryResources.SERVICES + service.getName();
    }

    /**
     * Activate username token authentication
     *
     * @param serviceName Axis service name
     * @param userGroups User groups
     * @throws SecurityConfigException
     */
    public void activateUsernameTokenAuthentication(String serviceName, String[] userGroups)
            throws SecurityConfigException {
        // TODO Remove
    }

    /**
     * Apply Security
     *
     * @param serviceName            Axis service name
     * @param scenarioId             Scenario id
     * @param kerberosConfigurations Kerberos configurations
     * @throws SecurityConfigException
     */
    public void applySecurity(String serviceName, String scenarioId, KerberosConfigData kerberosConfigurations)
            throws SecurityConfigException {

        if (kerberosConfigurations == null) {
            log.error("Kerberos configurations provided are invalid.");
            throw new SecurityConfigException("Kerberos configuration parameters are null. " +
                    "Please specify valid kerberos configurations.");
        }

        AxisService service = axisConfig.getServiceForActivation(serviceName);
        if (service == null) {
            throw new SecurityConfigException("Service is null for service name : " + serviceName);
        }

        try {
            // Begin registry transaction
            boolean transactionStarted = Transaction.isStarted();
            if (!transactionStarted) {
                registry.beginTransaction();
            }

            // Disable security if already a policy is applied
            this.disableSecurityOnService(serviceName); //TODO fix the method

            boolean isRahasEngaged = false;
            applyPolicy(service, scenarioId, null, null, null, kerberosConfigurations);

            isRahasEngaged = engageModules(scenarioId, serviceName, service);

            if (!isRahasEngaged) {
                log.info("Rahas engaged to service - " + serviceName);
            }

            disableRESTCalls(serviceName, scenarioId);
            persistsKerberosData(service, kerberosConfigurations);
            getPOXCache().remove(serviceName);

            if (!transactionStarted) {
                registry.commitTransaction();
            }

        } catch (RegistryException e) {
            StringBuilder str = new StringBuilder("Error persisting security scenario ").
                    append(scenarioId).append(" for service ").append(serviceName);
            log.error(str.toString(), e);
            try {
                registry.rollbackTransaction();
            } catch (RegistryException ex) {
                log.error("An error occurred while rollback, registry.", ex);
            }
            throw new SecurityConfigException(str.toString(), e);
        }
    }

    /**
     * Apply Security
     *
     * @param serviceName   Axis service name
     * @param scenarioId    Scenario id
     * @param policyPath    Policy path
     * @param trustedStores Trusted stores
     * @param privateStore  Private key stores
     * @param userGroups    User groups
     * @throws SecurityConfigException
     */
    public void applySecurity(String serviceName, String scenarioId, String policyPath,
                              String[] trustedStores, String privateStore,
                              String[] userGroups) throws SecurityConfigException {

        // TODO: If this method is too time consuming, it is better to not start
        // transactions in
        // here. Most of the operations invoked in here, are already
        // transactional.

        AxisService service = axisConfig.getServiceForActivation(serviceName);
        if (service == null) {
            throw new SecurityConfigException("Service is null for service name : " + serviceName);
        }

        try {
            registry = SecurityServiceHolder.getRegistry();

            if (userGroups != null) {
                Arrays.sort(userGroups);
                if (Arrays.binarySearch(userGroups, CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME) > -1) {
                    log.error("Security breach. A user is attempting to enable anonymous for UT access");
                    throw new SecurityConfigException("Invalid data provided"); // obscure error message
                }
            }

            boolean registryTransactionStarted = Transaction.isStarted();
            if (!registryTransactionStarted) {
                registry.beginTransaction();         //is this really needed?
            }

            this.disableSecurityOnService(serviceName);

            // if the service is a ghost service, load the actual service
            if (GhostDeployerUtils.isGhostService(service)) {
                try {
                    service = GhostDeployerUtils.deployActualService(axisConfig, service);
                } catch (AxisFault axisFault) {
                    log.error("Error while loading actual service from Ghost", axisFault);
                }
            }

            boolean isRahasEngaged = false;
            applyPolicy(service, scenarioId, policyPath, trustedStores, privateStore);
            isRahasEngaged = engageModules(scenarioId, serviceName, service);
            disableRESTCalls(serviceName, scenarioId);
            persistData(service, scenarioId, privateStore, trustedStores, userGroups, isRahasEngaged);

            if (!registryTransactionStarted) {
                registry.commitTransaction();
            }
            // finally update the ghost file if GD is used..
            if (service.getFileName() != null) {
                updateSecScenarioInGhostFile(service.getFileName().getPath(), serviceName, scenarioId);
            }

            this.getPOXCache().remove(serviceName);
            Cache<String, String> cache = getPOXCache();
            if (cache != null) {
                cache.remove(serviceName);
            }

            //Adding the security scenario ID parameter to the axisService
            //This parameter can be used to get the applied security scenario
            //without reading the service meta data file.
            try {
                Parameter param = new Parameter();
                param.setName(SecurityConstants.SCENARIO_ID_PARAM_NAME);
                param.setValue(scenarioId);
                service.addParameter(param);
            } catch (AxisFault axisFault) {
                log.error("Error while adding Scenario ID parameter", axisFault);
            }

            try {
                AxisModule rahas = service.getAxisConfiguration().getModule("rahas");
                if (!SecurityConstants.USERNAME_TOKEN_SCENARIO_ID.equals(scenarioId)) {
                    service.disengageModule(rahas);
                    service.engageModule(rahas);
                }
            } catch (AxisFault e1) {
                String msg = "Failed to propagate changes immediately. It will take time to update nodes in cluster";
                log.error(msg, e1);
                throw new SecurityConfigException(msg, e1);
            }

        } catch (RegistryException e) {
            try {
                registry.rollbackTransaction();
            } catch (RegistryException ex) {
                log.error("Error while rolling back transaction.", ex);
            }
            String msg = "Error while accessing config registry.";
            log.error(msg, e);
            throw new SecurityConfigException(msg, e);
        }
    }

    /**
     * Apply Security
     *
     * @param service       Axis service
     * @param scenarioId    Scenario id
     * @param policyPath    Policy path
     * @param trustedStores Trusted stores
     * @param privateStore  Private key stores
     * @throws SecurityConfigException
     */
    protected void applyPolicy(AxisService service, String scenarioId, String policyPath, String[] trustedStores,
                               String privateStore) throws SecurityConfigException {

        applyPolicy(service, scenarioId, policyPath, trustedStores, privateStore, null);
    }

    /**
     * Apply security
     *
     * @param service        Axis service
     * @param scenarioId     Scenario id
     * @param policyPath     Policy path
     * @param trustedStores  Trusted stores
     * @param privateStore   Private key stores
     * @param kerberosConfig Kerberos configuration data
     * @throws SecurityConfigException
     */
    protected void applyPolicy(AxisService service, String scenarioId, String policyPath, String[] trustedStores,
            String privateStore, KerberosConfigData kerberosConfig) throws SecurityConfigException {

        String serviceGroupId = service.getAxisServiceGroup().getServiceGroupName();
        try {
            String registryServicePath = getRegistryServicePath(service);
            String serviceXPath = ServicePersistenceUtil.getResourcePath(service);

            CallbackHandler handler;
            if (callback == null) {
                handler = new ServicePasswordCallbackHandler(null, serviceGroupId, service.getName(), serviceXPath,
                        registryServicePath, registry, realm);
            } else {
                handler = this.callback;
            }

            Parameter param = new Parameter();
            param.setName(WSHandlerConstants.PW_CALLBACK_REF);
            param.setValue(handler);
            service.addParameter(param);

            Properties props = getServerCryptoProperties(privateStore, trustedStores);
            RampartConfig rampartConfig = new RampartConfig();
            // rampartConfig.setTokenStoreClass(SimpleTokenStore.class.getName());
            populateRampartConfig(rampartConfig, props, kerberosConfig);
            Policy policy = loadPolicy(scenarioId, policyPath);

            if (rampartConfig != null) {
                policy.addAssertion(rampartConfig);
            }

            //If the policy is from registry, add the policy path as a service parameter
            if (policyPath != null && scenarioId.equals(SecurityConstants.POLICY_FROM_REG_SCENARIO)) {
                Parameter pathParam = new Parameter(SecurityConstants.SECURITY_POLICY_PATH, policyPath);
                service.addParameter(pathParam);
                writeParameterIntoRegistry(pathParam, registryServicePath);
            }

            if (isHttpsTransportOnly(policy)) {
                setServiceTransports(service.getName(), getHttpsTransports());
                try {

                    boolean transactionStarted = Transaction.isStarted();
                    if (!transactionStarted) {
                        registry.beginTransaction();
                    }

                    Resource resource = registry.get(registryServicePath);
                    resource.setProperty(RegistryResources.ServiceProperties.EXPOSED_ON_ALL_TANSPORTS,
                            Boolean.FALSE.toString());
                    resource.setProperty(RegistryResources.ServiceProperties.IS_UT_ENABLED, Boolean.TRUE.toString());

                    Association[] exposedTransports = registry
                            .getAssociations(registryServicePath, RegistryResources.Associations.EXPOSED_TRANSPORTS);
                    boolean isExists = false;
                    // TODO : Handle generally as axis2 parameters
                    for (Association assoc : exposedTransports) {
                        String transport = assoc.getDestinationPath();
                        if (transport.endsWith("https")) {
                            isExists = true;
                            continue;
                        }
                        if (registry.resourceExists(transport)) {
                            registry.removeAssociation(registryServicePath, transport,
                                    RegistryResources.Associations.EXPOSED_TRANSPORTS);
                        } else {
                            String msg = "Transport resource " + transport + " not available in Registry";
                            log.error(msg);
                            throw new SecurityConfigException(msg);
                        }
                    }

                    if (!isExists) {
                        String transportResourcePath = RegistryResources.TRANSPORTS + "https" + "/listener";
                        if (registry.resourceExists(transportResourcePath)) {
                            registry.addAssociation(registryServicePath, transportResourcePath,
                                    RegistryResources.Associations.EXPOSED_TRANSPORTS);
                        } else {
                            String msg = "Transport resource " + transportResourcePath + " not available in Registry";
                            log.error(msg);
                            throw new SecurityConfigException(msg);
                        }
                    }

                    registry.put(resource.getPath(), resource);
                    if (!transactionStarted) {
                        registry.commitTransaction();
                    }

                } catch (RegistryException e) {
                    try {
                        registry.rollbackTransaction();
                    } catch (RegistryException e1) {
                        log.error("Error while rolling back transaction.", e);
                    }
                    String msg = "Service with name " + service.getName() + " not found.";
                    log.error(msg, e);
                    throw new SecurityConfigException(msg, e);
                }
            } else {
                setServiceTransports(service.getName(), getAllTransports());
            }

            SecurityServiceAdmin secAdmin = new SecurityServiceAdmin(axisConfig, registry);
            secAdmin.addSecurityPolicyToAllBindings(service, policy);

        } catch (ServerException e) {
            try {
                registry.rollbackTransaction();
            } catch (RegistryException ex) {
                log.error("Error while rolling back transaction.", ex);
            }
            String error = "Error occurred while adding security policy to all bindings.";
            throw new SecurityConfigException(error, e);
        } catch (RegistryException e) {
            try {
                registry.rollbackTransaction();
            } catch (RegistryException ex) {
                log.error("Error while rolling back transaction.", ex);
            }
            String error = "Error occurred while accessing registry.";
            throw new SecurityConfigException(error, e);
        } catch (AxisFault e) {
            try {
                registry.rollbackTransaction();
            } catch (RegistryException e1) {
                log.error("Error while rolling back transaction.", e);
            }
            String error = "Error occurred while applying security.";
            throw new SecurityConfigException(error, e);
        } catch (Exception e) {
            try {
                registry.rollbackTransaction();
            } catch (RegistryException ex) {
                log.error("Error while rolling back transaction.", ex);
            }
            String error = "Error occurred while applying security";
            throw new SecurityConfigException(error, e);
        }
    }

    /**
     * Write parameters to registry
     *
     * @param parameter
     * @param servicePath
     * @throws Exception
     */
    private void writeParameterIntoRegistry(Parameter parameter, String servicePath) throws Exception {

        boolean transactionStarted = Transaction.isStarted();
        if (!transactionStarted) {
            registry.beginTransaction();
        }
        String paramName = parameter.getName();
        if (paramName != null && paramName.trim().length() != 0) {
            if (parameter.getParameterElement() == null && parameter.getValue() != null
                    && parameter.getValue() instanceof String) {
                parameter = new Parameter();
                parameter.setName(paramName.trim());
                parameter.setValue(parameter.getValue());
                parameter.setLocked(parameter.isLocked());
            }
            if (parameter.getParameterElement() != null) {
                Resource paramResource = registry.newResource();
                paramResource.setContent(parameter.getParameterElement().toString());
                paramResource.addProperty(RegistryResources.NAME, parameter.getName());
                registry.put(servicePath + RegistryResources.PARAMETERS + parameter.getName(), paramResource);
                paramResource.discard();
            }
        }
        if (!transactionStarted) {
            registry.commitTransaction();
        }
    }

    /**
     * Engaging modules
     *
     * @param scenarioId    Scenario id
     * @param serviceName   Axis service name
     * @param axisService   Axis service
     * @return  Is rahas engaged
     * @throws SecurityConfigException
     */
    protected boolean engageModules(String scenarioId, String serviceName, AxisService axisService)
            throws SecurityConfigException {

        boolean isRahasEngaged = false;
        SecurityScenario securityScenario = SecurityScenarioDatabase.get(scenarioId);
        String[] moduleNames = (String[]) securityScenario.modules.
                toArray(new String[securityScenario.modules.size()]);

        String servicePath = getServicePath(axisService);

        // Handle each module required
        try {
            try {
                boolean transactionStarted = Transaction.isStarted();
                if (!transactionStarted) {
                    registry.beginTransaction();
                }
                Association[] assocs = registry.getAssociations(servicePath,
                        RegistryResources.Associations.ENGAGED_MODULES);
                for (String modName : moduleNames) {
                    AxisModule module = axisService.getAxisConfiguration().getModule(modName);
                    String path = RegistryResources.MODULES + modName + "/" + module.getVersion();
                    boolean isFound = false;
                    for (Association tempAssoc : assocs) {
                        if (tempAssoc.getDestinationPath().equals(path)) {
                            isFound = true;
                            break;
                        }
                    }

                    if (!isFound) {
                        if (registry.resourceExists(path)) {
                            registry.addAssociation(servicePath, path, RegistryResources.Associations.ENGAGED_MODULES);
                        }
                    }
                    // Engage at axis2
                    axisService.disengageModule(module);
                    axisService.engageModule(module);
                    if (modName.equalsIgnoreCase("rahas")) {
                        isRahasEngaged = true;
                    }
                }
                if (!transactionStarted) {
                    registry.commitTransaction();
                }
            } catch (RegistryException e) {
                try {
                    registry.rollbackTransaction();
                } catch (RegistryException ex) {
                    log.error("Error occurred while rolling back transaction", ex);
                }
                String msg = "Error occurred while engaging module.";
                log.error(msg, e);
                throw new AxisFault(msg, e);
            }
        } catch (AxisFault e) {
            String msg = "Error occurred while engaging module.";
            log.error(msg, e);
            throw new SecurityConfigException(msg, e);
        }
        return isRahasEngaged;
    }

    /**
     * Disable REST calls
     *
     * @param serviceName Axis service name
     * @param scenrioId   Scenario id
     * @throws SecurityConfigException
     */
    protected void disableRESTCalls(String serviceName, String scenrioId) throws SecurityConfigException {

        if (scenrioId.equals(SecurityConstants.USERNAME_TOKEN_SCENARIO_ID)) {
            return;
        }
        try {
            AxisService service = axisConfig.getServiceForActivation(serviceName);
            if (service == null) {
                throw new SecurityConfigException("Service is null for service name " + serviceName);
            }

            Parameter param = new Parameter();
            param.setName("disableREST"); // TODO Find the constant
            param.setValue(Boolean.TRUE.toString());
            service.addParameter(param);

        } catch (AxisFault e) {
            String error = "Error occurred while disabling REST calls";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }

    }

    /**
     * Get Service path
     *
     * @param service Axis service
     * @return Service path
     */
    private String getServicePath(AxisService service) {

        String servicePath = RegistryResources.SERVICE_GROUPS + service.getAxisServiceGroup().getServiceGroupName() +
                RegistryResources.SERVICES + service.getName();
        return servicePath;
    }

    /**
     * Persist data
     *
     * @param service        Axis service
     * @param scenrioId      Scenario id
     * @param privateStore   Private key stores
     * @param trustedStores  Trusted stores
     * @param userGroups     User groups
     * @param isRahasEngaged Is rahas engaged
     * @throws SecurityConfigException
     */
    protected void persistData(AxisService service, String scenrioId, String privateStore,String[] trustedStores,
                               String[] userGroups, boolean isRahasEngaged) throws SecurityConfigException {

        try {
            String servicePath = getServicePath(service);

            if (privateStore != null) {
                String ksPath = SecurityConstants.KEY_STORES + "/" + privateStore;
                if (govRegistry.resourceExists(ksPath)) {
                    registry.addAssociation(servicePath, RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                            ksPath, SecurityConstants.ASSOCIATION_PRIVATE_KEYSTORE);
                } else if (KeyStoreUtil.isPrimaryStore(privateStore)) {
                    registry.addAssociation(servicePath, RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                    RegistryResources.SecurityManagement.PRIMARY_KEYSTORE_PHANTOM_RESOURCE,
                            SecurityConstants.ASSOCIATION_PRIVATE_KEYSTORE);
                } else {
                    throw new SecurityConfigException("Missing key store " + privateStore);
                }
            }

            if (trustedStores != null) {
                for (String storeName : trustedStores) {
                    String ksPath = SecurityConstants.KEY_STORES + "/" + storeName;
                    if (govRegistry.resourceExists(ksPath)) {
                        registry.addAssociation(servicePath, RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                ksPath, SecurityConstants.ASSOCIATION_TRUSTED_KEYSTORE);
                    } else if (KeyStoreUtil.isPrimaryStore(storeName)) {
                        registry.addAssociation(servicePath, RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                        RegistryResources.SecurityManagement.PRIMARY_KEYSTORE_PHANTOM_RESOURCE,
                                SecurityConstants.ASSOCIATION_TRUSTED_KEYSTORE);
                    } else {
                        throw new SecurityConfigException("Missing key store" + storeName);
                    }
                }
            } else {
                trustedStores = new String[0];
            }

            if (userGroups != null) {
                AuthorizationManager acAdmin = realm.getAuthorizationManager();

                for (int i = 0; i < userGroups.length; i++) {
                    String value = userGroups[i];
                    acAdmin.authorizeRole(value, servicePath,
                            UserCoreConstants.INVOKE_SERVICE_PERMISSION);
                }
            }

            if (isRahasEngaged) {
                setRahasParameters(service, privateStore);
            } else {
                removeRahasParameters(service);
            }

        }catch(RegistryException ex){
            String error = "Error occurred while accessing registry.";
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        }catch(UserStoreException ex){
            String error = "Error occurred while persisting role to authorization manager.";
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        }catch(AxisFault ex){
            String error = "Error occurred while persisting data.";
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        }
    }

    /**
     * Load the security policy from Registry according to the scenarioId. If the scenario is
     * "policyFromRegistry", it gets the policy from the given path.
     *
     * @param scenarioId - String id of the scenario
     * @param policyPath - regIdentifier:path
     *                   regIdentifier (conf: or gov:) is used to identify the registry
     *                   path is the policy path in the selected registry
     * @return - Policy object
     * @throws SecurityConfigException - Error while loading policy
     */
    public Policy loadPolicy(String scenarioId, String policyPath) throws SecurityConfigException {

        try {
            Registry registryToLoad = registry;
            String resourceUri = SecurityConstants.SECURITY_POLICY + "/" + scenarioId;
            if (policyPath != null && scenarioId.equals(SecurityConstants.POLICY_FROM_REG_SCENARIO)) {
                resourceUri = policyPath.substring(policyPath.lastIndexOf(':') + 1);
                String regIdentifier = policyPath.substring(0, policyPath.lastIndexOf(':'));
                if (SecurityConstants.GOVERNANCE_REGISTRY_IDENTIFIER.equals(regIdentifier)) {
                    registryToLoad = govRegistry;
                }
            }
            Resource resource = registryToLoad.get(resourceUri);
            InputStream in = resource.getContentStream();

            XMLStreamReader parser;
            parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
            StAXOMBuilder builder = new StAXOMBuilder(parser);

            OMElement policyElement = builder.getDocumentElement();
            if (policyPath != null && scenarioId.equals(SecurityConstants.POLICY_FROM_REG_SCENARIO)) {
                OMAttribute att = policyElement.getAttribute(SecurityConstants.POLICY_ID_QNAME);
                if (att != null) {
                    att.setAttributeValue(SecurityConstants.POLICY_FROM_REG_SCENARIO);
                }
            }
            return PolicyEngine.getPolicy(policyElement);

        } catch (RegistryException e) {
            String error = "Error occurred while accessing registry.";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        } catch (XMLStreamException ex) {
            String error = "Error occurred while building XML stream.";
            log.error(error, ex);
            throw new SecurityConfigException(error, ex);
        }
    }

    /**
     * Populate rampart configuration
     *
     * @param rampartConfig Rampart configuration
     * @param props         Rampart properties
     * @throws SecurityConfigException
     */
    public void populateRampartConfig(RampartConfig rampartConfig, Properties props)
            throws SecurityConfigException {

        populateRampartConfig(rampartConfig, props, null);
    }

    /**
     * Populate rampart configuration
     *
     * @param rampartConfig          Rampart configuration
     * @param props                  Rampart properties
     * @param kerberosConfigurations Kerberos configurations
     * @throws SecurityConfigException
     */
    public void populateRampartConfig(RampartConfig rampartConfig, Properties props,
                                      KerberosConfigData kerberosConfigurations) throws SecurityConfigException {

        if (rampartConfig != null) {

            if (kerberosConfigurations != null) {

                Properties kerberosProperties = new Properties();
                kerberosProperties.setProperty(KerberosConfig.SERVICE_PRINCIPLE_NAME,
                        kerberosConfigurations.getServicePrincipleName());

                KerberosConfig kerberosConfig = new KerberosConfig();
                kerberosConfig.setProp(kerberosProperties);

                // Set system wide kerberos configurations

                String carbonSecurityConfigurationPath = RampartConfigUtil.getCarbonSecurityConfigurationPath();
                if (carbonSecurityConfigurationPath != null) {

                    String krbFile = carbonSecurityConfigurationPath + File.separatorChar
                            + KerberosConfigData.KERBEROS_CONFIG_FILE_NAME;

                    File krbFileObject = new File(krbFile);

                    if (!krbFileObject.exists()) {
                        throw new SecurityConfigException("Kerberos configuration file not found at " + krbFile);
                    }

                    log.info("Setting " + KerberosConfigData.KERBEROS_CONFIG_FILE_SYSTEM_PROPERTY +
                            " to kerberos configuration file " + krbFile);

                    System.setProperty(KerberosConfigData.KERBEROS_CONFIG_FILE_SYSTEM_PROPERTY, krbFile);

                } else {
                    throw new SecurityConfigException("Could not retrieve carbon home");
                }

                rampartConfig.setKerberosConfig(kerberosConfig);

            } else {

                if (!props.isEmpty()) {
                    // Encryption crypto config
                    {
                        CryptoConfig encrCryptoConfig = new CryptoConfig();
                        encrCryptoConfig.setProvider(ServerCrypto.class.getName());
                        encrCryptoConfig.setProp(props);
                        encrCryptoConfig.setCacheEnabled(true);
                        encrCryptoConfig.setCryptoKey(ServerCrypto.PROP_ID_PRIVATE_STORE);
                        rampartConfig.setEncrCryptoConfig(encrCryptoConfig);
                    }

                    {
                        CryptoConfig signatureCryptoConfig = new CryptoConfig();
                        signatureCryptoConfig.setProvider(ServerCrypto.class.getName());
                        signatureCryptoConfig.setProp(props);
                        signatureCryptoConfig.setCacheEnabled(true);
                        signatureCryptoConfig.setCryptoKey(ServerCrypto.PROP_ID_PRIVATE_STORE);
                        rampartConfig.setSigCryptoConfig(signatureCryptoConfig);
                    }
                }

                rampartConfig.setEncryptionUser(WSHandlerConstants.USE_REQ_SIG_CERT);
                rampartConfig.setUser(props.getProperty(SecurityConstants.USER));

                // Get ttl and timeskew params from axis2 xml
                int ttl = RampartConfig.DEFAULT_TIMESTAMP_TTL;
                int timeSkew = RampartConfig.DEFAULT_TIMESTAMP_MAX_SKEW;

                rampartConfig.setTimestampTTL(Integer.toString(ttl));
                rampartConfig.setTimestampMaxSkew(Integer.toString(timeSkew));
                //adding this class as default one.
                //rampartConfig.setTokenStoreClass("org.wso2.carbon.identity.sts.store.DBTokenStore");

                //this will check for TokenStoreClassName property under Security in carbon.xml
                //if it is not found, default token store class will be set
                String tokenStoreClassName = ServerConfiguration.getInstance().getFirstProperty("Security.TokenStoreClassName");
                if (tokenStoreClassName == null) {
                    rampartConfig.setTokenStoreClass(SecurityTokenStore.class.getName());
                } else {
                    rampartConfig.setTokenStoreClass(tokenStoreClassName);
                }
            }
        }
    }

    /**
     * Get server crypto properties
     *
     * @param privateStore      Private key stores
     * @param trustedCertStores Trusted cert stores
     * @return
     * @throws Exception
     */
    public Properties getServerCryptoProperties(String privateStore, String[] trustedCertStores)
            throws Exception {

        Properties props = new Properties();
        int tenantId = ((UserRegistry) registry).getTenantId();

        if (trustedCertStores != null && trustedCertStores.length > 0) {
            StringBuilder trustString = new StringBuilder();
            for (String trustedCertStore : trustedCertStores) {
                trustString.append(trustedCertStore).append(",");
            }

            if (trustedCertStores.length != 0) {
                props.setProperty(ServerCrypto.PROP_ID_TRUST_STORES, trustString.toString());
            }
        }

        if (privateStore != null) {
            props.setProperty(ServerCrypto.PROP_ID_PRIVATE_STORE, privateStore);

            KeyStoreManager keyMan = KeyStoreManager.getInstance(tenantId);
            KeyStore ks = keyMan.getKeyStore(privateStore);

            String privKeyAlias = KeyStoreUtil.getPrivateKeyAlias(ks);
            props.setProperty(ServerCrypto.PROP_ID_DEFAULT_ALIAS, privKeyAlias);
            props.setProperty(USER, privKeyAlias);
        }

        if (privateStore != null || (trustedCertStores != null && trustedCertStores.length > 0)) {
            //Set the tenant-ID in the properties
            props.setProperty(ServerCrypto.PROP_ID_TENANT_ID,
                    new Integer(tenantId).toString());
        }

        return props;
    }

    /**
     * Expose this service only via the specified transport
     *
     * @param serviceId          service name
     * @param transportProtocols transport protocols to expose
     * @throws AxisFault                                        axisfault
     * @throws org.wso2.carbon.security.SecurityConfigException ex
     */
    public void setServiceTransports(String serviceId, List<String> transportProtocols)
            throws SecurityConfigException, AxisFault {

        AxisService axisService = axisConfig.getServiceForActivation(serviceId);
        if (axisService == null) {
            throw new SecurityConfigException("Service is null for service id : " + serviceId);
        }

        ArrayList<String> transports = new ArrayList<String>();
        for (int i = 0; i < transportProtocols.size(); i++) {
            transports.add(transportProtocols.get(i));
        }
        axisService.setExposedTransports(transports);

        if (log.isDebugEnabled()) {
            log.debug("Successfully add selected transport bindings to service " + serviceId);
        }
    }

    /**
     * Check the policy to see whether the service should only be exposed in
     * HTTPS
     *
     * @param policy service policy
     * @return returns true if the service should only be exposed in HTTPS
     * @throws org.wso2.carbon.security.SecurityConfigException ex
     */
    public boolean isHttpsTransportOnly(Policy policy) throws SecurityConfigException {

        // When there is a transport binding sec policy assertion,
        // the service should be exposed only via HTTPS
        boolean httpsRequired = false;

        try {
            Iterator alternatives = policy.getAlternatives();
            if (alternatives.hasNext()) {
                List it = (List) alternatives.next();

                RampartPolicyData rampartPolicyData = RampartPolicyBuilder.build(it);
                if (rampartPolicyData.isTransportBinding()) {
                    httpsRequired = true;
                } else if (rampartPolicyData.isSymmetricBinding()) {
                    Token encrToken = rampartPolicyData.getEncryptionToken();
                    if (encrToken instanceof SecureConversationToken) {
                        Policy bsPol = ((SecureConversationToken) encrToken).getBootstrapPolicy();
                        Iterator alts = bsPol.getAlternatives();
                        if (alts.hasNext()) {
                        }
                        List bsIt = (List) alts.next();
                        RampartPolicyData bsRampartPolicyData = RampartPolicyBuilder.build(bsIt);
                        httpsRequired = bsRampartPolicyData.isTransportBinding();
                    }
                }
            }
        } catch (WSSPolicyException e) {
            String error = "Error occurred while build rampart policy";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }

        return httpsRequired;
    }

    /**
     * Get "https" transports in the AxisConfig
     *
     * @return list
     */
    public List<String> getHttpsTransports() {

        List<String> httpsTransports = new ArrayList<String>();
        for (Iterator iter = axisConfig.getTransportsIn().keySet().iterator(); iter.hasNext(); ) {
            String transport = (String) iter.next();
            if (transport.toLowerCase().indexOf(SecurityConstants.HTTPS_TRANSPORT) != -1) {
                httpsTransports.add(transport);
            }
        }
        return httpsTransports;
    }

    /**
     * Get all transports in AxisConfig
     *
     * @return list of all transports
     */
    public List<String> getAllTransports() {

        List<String> allTransports = new ArrayList<String>();
        for (Iterator iter = axisConfig.getTransportsIn().keySet().iterator(); iter.hasNext(); ) {
            String transport = (String) iter.next();
            allTransports.add(transport);
        }
        return allTransports;
    }

    /**
     * Get security configuration data
     *
     * @param serviceName Axis service name
     * @param scenarioId  Scenario id
     * @param policyPath  Policy path
     * @return  Security configuration data
     * @throws SecurityConfigException
     */
    public SecurityConfigData getSecurityConfigData(String serviceName, String scenarioId,
                                                    String policyPath) throws SecurityConfigException {
        SecurityConfigData data = null;
        try {
            if (scenarioId == null) {
                return data;
            }
            AxisService service = axisConfig.getServiceForActivation(serviceName);

            /**
             * Scenario ID can either be a default one (out of 15) or "policyFromRegistry", which
             * means the current scenario refers to a custom policy from registry. If that is the
             * case, we can't read the current scenario from the WSU ID. Therefore, we don't
             * check the scenario ID. In default cases, we check it.
             */
            if (scenarioId.equals(SecurityConstants.POLICY_FROM_REG_SCENARIO)) {
                Parameter param = service.getParameter(SecurityConstants.SECURITY_POLICY_PATH);
                if (param == null || !policyPath.equals(param.getValue())) {
                    return data;
                }
            } else {
                SecurityScenario scenario = readCurrentScenario(serviceName);
                if (scenario == null || !scenario.getScenarioId().equals(scenarioId)) {
                    return data;
                }
            }

            data = new SecurityConfigData();

            String servicePath = getServicePath(service);

            AuthorizationManager acReader = realm.getAuthorizationManager();
            String[] roles = acReader.getAllowedRolesForResource(servicePath,
                    UserCoreConstants.INVOKE_SERVICE_PERMISSION);
            data.setUserGroups(roles);

            Association[] pvtStores = registry.getAssociations(servicePath,
                    SecurityConstants.ASSOCIATION_PRIVATE_KEYSTORE);
            if (pvtStores.length > 0) {
                String temp = pvtStores[0].getDestinationPath();
                if (temp.startsWith("//")) {
                    temp = temp.substring(1);
                }
                if (temp.equals(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                        RegistryResources.SecurityManagement.PRIMARY_KEYSTORE_PHANTOM_RESOURCE)) {
                    ServerConfiguration config = ServerConfiguration.getInstance();
                    String file = new File(config.getFirstProperty(RegistryResources
                            .SecurityManagement.SERVER_PRIMARY_KEYSTORE_FILE)).getAbsolutePath();
                    String name = KeyStoreUtil.getKeyStoreFileName(file);
                    data.setPrivateStore(name);
                } else {
                    temp = temp.substring(temp.lastIndexOf("/") + 1);
                    data.setPrivateStore(temp);
                }
            }

            Association[] tstedStores = registry.getAssociations(servicePath,
                    SecurityConstants.ASSOCIATION_TRUSTED_KEYSTORE);
            String[] trustedStores = new String[tstedStores.length];
            for (int i = 0; i < tstedStores.length; i++) {
                String temp = tstedStores[i].getDestinationPath();
                if (temp.startsWith("//")) {
                    temp = temp.substring(1);
                }
                if (temp.equals(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                        RegistryResources.SecurityManagement.PRIMARY_KEYSTORE_PHANTOM_RESOURCE)) {
                    ServerConfiguration config = ServerConfiguration.getInstance();
                    String file = new File(config.getFirstProperty(RegistryResources
                            .SecurityManagement.SERVER_PRIMARY_KEYSTORE_FILE)).getAbsolutePath();
                    String name = KeyStoreUtil.getKeyStoreFileName(file);
                    trustedStores[i] = name;
                } else {
                    temp = temp.substring(temp.lastIndexOf("/") + 1);
                    trustedStores[i] = temp;
                }
            }
            data.setTrustedKeyStores(trustedStores);

            KerberosConfigData kerberosData = this.readKerberosConfigurations(service);
            data.setKerberosConfigurations(kerberosData);
            return data;

        } catch (RegistryException e) {
            String error = "Error occurred while accessing registry.";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        } catch (UserStoreException e) {
            String error = "Error occurred while accessing roles from authorization manager.";
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }
    }

    /**
     * Read current scenario
     *
     * @param serviceName Axis service name
     * @return  Current security scenario
     * @throws SecurityConfigException
     */
    public SecurityScenario readCurrentScenario(String serviceName) throws SecurityConfigException {
        SecurityScenario scenario = null;

        try {
            AxisService service = axisConfig.getServiceForActivation(serviceName);
            if (service == null) {
                // try to find it from the transit ghost map
                try {
                    service = GhostDeployerUtils
                            .getTransitGhostServicesMap(axisConfig).get(serviceName);
                } catch (AxisFault axisFault) {
                    log.error("Error while reading Transit Ghosts map", axisFault);
                }
                if (service == null) {
                    throw new SecurityConfigException("AxisService is null for service name : " + serviceName);
                }
            }

            String policyResourcePath = getServicePath(service) + RegistryResources.POLICIES;
            if (!registry.resourceExists(policyResourcePath)) {
                return scenario;
            }

            Map endPointMap = service.getEndpoints();
            for (Object o : endPointMap.entrySet()) {
                scenario = null;

                Map.Entry entry = (Map.Entry) o;
                AxisEndpoint point = (AxisEndpoint) entry.getValue();
                AxisBinding binding = point.getBinding();
                java.util.Collection policies = binding.getPolicySubject()
                        .getAttachedPolicyComponents();
                Iterator policyComponents = policies.iterator();
                String policyId = "";
                while (policyComponents.hasNext()) {
                    PolicyComponent currentPolicyComponent = (PolicyComponent) policyComponents
                            .next();
                    if (currentPolicyComponent instanceof Policy) {
                        policyId = ((Policy) currentPolicyComponent).getId();
                    } else if (currentPolicyComponent instanceof PolicyReference) {
                        policyId = ((PolicyReference) currentPolicyComponent).getURI().substring(1);
                    }

                    // Check whether this is a security scenario
                    scenario = SecurityScenarioDatabase.getByWsuId(policyId);
                }

                // If a scenario is NOT applied to at least one non HTTP
                // binding,
                // we consider the service unsecured.
                if ((scenario == null)
                        && (!binding.getName().getLocalPart().contains("HttpBinding"))) {
                    break;
                }
            }

            // If the binding level policies are not present, check whether there is a policy attached
            // at the service level. This is a fix for Securing Proxy Services.
            if (scenario == null) {
                java.util.Collection policies = service.getPolicySubject()
                        .getAttachedPolicyComponents();
                Iterator policyComponents = policies.iterator();
                String policyId = "";
                while (policyComponents.hasNext()) {
                    PolicyComponent currentPolicyComponent = (PolicyComponent) policyComponents
                            .next();
                    if (currentPolicyComponent instanceof Policy) {
                        policyId = ((Policy) currentPolicyComponent).getId();
                    } else if (currentPolicyComponent instanceof PolicyReference) {
                        policyId = ((PolicyReference) currentPolicyComponent).getURI().substring(1);
                    }
                    // Check whether this is a security scenario
                    scenario = SecurityScenarioDatabase.getByWsuId(policyId);
                }
            }
            return scenario;

        } catch (Exception e) {
            String error = "Error while reading Security Scenario for service " + serviceName;
            log.error(error, e);
            throw new SecurityConfigException(error, e);
        }

    }


    /**
     * If the given service is in ghost state, force the actual service deployment. This method
     * is useful when we want to make sure the actual service is in the system before we do some
     * operation on the service.
     *
     * @param serviceName - name of the service
     */
    public void forceActualServiceDeployment(String serviceName) {

        AxisService service = axisConfig.getServiceForActivation(serviceName);
        if (service == null) {
            // try to find it from the transit ghost map
            try {
                service = GhostDeployerUtils
                        .getTransitGhostServicesMap(axisConfig).get(serviceName);
            } catch (AxisFault axisFault) {
                log.error("Error while reading Transit Ghosts map", axisFault);
            }
        }
        if (service != null && GhostDeployerUtils.isGhostService(service)) {
            // if the service is a ghost service, load the actual service
            try {
                GhostDeployerUtils.deployActualService(axisConfig, service);
            } catch (AxisFault axisFault) {
                log.error("Error while loading actual service from Ghost", axisFault);
            }
        }
    }

    /**
     * Set Rahas parameters
     *
     * @param axisService     Axis service
     * @param privateKeyStore Private key store
     * @throws RegistryException
     * @throws AxisFault
     */
    private void setRahasParameters(AxisService axisService, String privateKeyStore)
            throws RegistryException, AxisFault {

        Properties cryptoProps = new Properties();

        String serviceName = axisService.getName();
        String servicePath = getServicePath(axisService);

        Resource resource = registry.get(servicePath);
        Association[] pvtStores = registry.getAssociations(servicePath,
                SecurityConstants.ASSOCIATION_PRIVATE_KEYSTORE);
        Association[] tstedStores = registry.getAssociations(servicePath,
                SecurityConstants.ASSOCIATION_TRUSTED_KEYSTORE);

        if (pvtStores != null && pvtStores.length > 0) {
            String keyAlias = null;
            ServerConfiguration serverConfig = ServerConfiguration.getInstance();
            keyAlias = serverConfig.getFirstProperty("Security.KeyStore.KeyAlias");
            cryptoProps.setProperty(ServerCrypto.PROP_ID_PRIVATE_STORE, privateKeyStore);
            cryptoProps.setProperty(ServerCrypto.PROP_ID_DEFAULT_ALIAS, keyAlias);
        }
        StringBuffer trustStores = new StringBuffer();

        for (Association assoc : tstedStores) {
            String tstedStore = assoc.getDestinationPath();
            String name = tstedStore.substring(tstedStore.lastIndexOf("/"));
            trustStores.append(name).append(",");
        }
        cryptoProps.setProperty(ServerCrypto.PROP_ID_TRUST_STORES, trustStores.toString());

        try {
            setServiceParameterElement(serviceName, RahasUtil.getSCTIssuerConfigParameter(
                    ServerCrypto.class.getName(), cryptoProps, -1, null, true, true));
            setServiceParameterElement(serviceName, RahasUtil.getTokenCancelerConfigParameter());
            resource.setProperty(SecurityConstants.PROP_RAHAS_SCT_ISSUER, "true");
            registry.put(servicePath, resource);

        } catch (Exception e) {
            throw new AxisFault("Could not configure Rahas parameters", e);
        }
    }

    /**
     * Remove Rahas parameters
     *
     * @param axisService Axis service
     * @throws AxisFault
     */
    private void removeRahasParameters(AxisService axisService) throws RegistryException {

        String servicePath = getServicePath(axisService);
        if (registry.resourceExists(servicePath)) {
            Resource resource = registry.get(servicePath);
            if (resource.getProperty(SecurityConstants.PROP_RAHAS_SCT_ISSUER) != null) {
                resource.removeProperty(SecurityConstants.PROP_RAHAS_SCT_ISSUER);
                registry.put(servicePath, resource);
            }
        }
    }

    /**
     * Set service parameter element
     *
     * @param serviceName Axis service name
     * @param parameter   Parameter
     * @throws AxisFault
     */
    private void setServiceParameterElement(String serviceName, Parameter parameter) throws AxisFault {

        AxisService axisService = axisConfig.getServiceForActivation(serviceName);
        if (axisService == null) {
            throw new AxisFault("Invalid service name '" + serviceName + "'");
        }

        Parameter p = axisService.getParameter(parameter.getName());
        if (p != null) {
            if (!p.isLocked()) {
                axisService.addParameter(parameter);
            }
        } else {
            axisService.addParameter(parameter);
        }

    }

    /**
     * Opens the relevant ghost file and adds the security scenario id as a parameter in the
     * service. If the scenario id is null, remove existing security attribute from the file.
     *
     * @param servicePath - Absolute path fo the service file
     * @param serviceName - AxisService name
     * @param scenarioId  - Security scenario Id
     */
    private void updateSecScenarioInGhostFile(String servicePath,
                                              String serviceName, String scenarioId) {

        File ghostFile = GhostDeployerUtils.getGhostFile(servicePath, axisConfig);
        if (ghostFile != null && ghostFile.exists()) {
            FileInputStream ghostInStream = null;
            FileOutputStream ghostOutStream = null;
            try {
                // read the ghost file and create an OMElement
                ghostInStream = new FileInputStream(ghostFile);
                OMElement ghostSGElement = new StAXOMBuilder(ghostInStream).getDocumentElement();
                // iterate through all services
                Iterator itr = ghostSGElement
                        .getChildrenWithLocalName(CarbonConstants.GHOST_SERVICE);
                while (itr.hasNext()) {
                    OMElement serviceElm = (OMElement) itr.next();
                    String nameFromFile = serviceElm
                            .getAttributeValue(new QName(CarbonConstants.GHOST_ATTR_NAME));
                    if (nameFromFile != null && nameFromFile.equals(serviceName)) {
                        OMAttribute secAtt = serviceElm.getAttribute(new QName(CarbonConstants
                                .GHOST_ATTR_SECURITY_SCENARIO));
                        // if the correct service is found, update the security scenario attribute
                        if (scenarioId == null) {
                            if (secAtt != null) {
                                serviceElm.removeAttribute(secAtt);
                            } else {
                                return;
                            }
                        } else {
                            if (secAtt == null) {
                                serviceElm.addAttribute(CarbonConstants
                                        .GHOST_ATTR_SECURITY_SCENARIO, scenarioId, null);
                            } else {
                                secAtt.setAttributeValue(scenarioId);
                            }
                        }
                    }
                }
                // serialize the modified OMElement
                ghostOutStream = new FileOutputStream(ghostFile);
                ghostSGElement.serialize(ghostOutStream);
                ghostOutStream.flush();
            } catch (Exception e) {
                log.error("Error while reading ghost file for service : " + servicePath);
            } finally {
                try {
                    if (ghostInStream != null) {
                        ghostInStream.close();
                    }
                    if (ghostOutStream != null) {
                        ghostOutStream.close();
                    }
                } catch (IOException e) {
                    log.error("Error while closing the file output stream", e);
                }
            }
        }
    }

    /**
     * Get POX Cache
     * @return POX cache
     */
    private Cache<String, String> getPOXCache() {

        CacheManager manager = Caching.getCacheManagerFactory().getCacheManager(POXSecurityHandler.POX_CACHE_MANAGER);
        Cache<String, String> cache = manager.getCache(POXSecurityHandler.POX_ENABLED);
        return cache;
    }

}
