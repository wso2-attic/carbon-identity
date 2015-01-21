/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.provisioning;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.OutboundProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.mgt.ApplicationInfoProvider;
import org.wso2.carbon.identity.provisioning.cache.ServiceProviderProvisioningConnectorCacheEntry;
import org.wso2.carbon.identity.provisioning.cache.ServiceProviderProvisioningConnectorCacheKey;
import org.wso2.carbon.identity.provisioning.cache.ServiceProviderProvisioningConnectorCache;
import org.wso2.carbon.identity.provisioning.dao.CacheBackedProvisioningMgtDAO;
import org.wso2.carbon.identity.provisioning.dao.ProvisioningManagementDAO;
import org.wso2.carbon.identity.provisioning.internal.IdentityProvisionServiceComponent;
import org.wso2.carbon.identity.provisioning.listener.DefaultInboundUserProvisioningListener;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 *
 *
 */
public class OutboundProvisioningManager {

    private static final Log log = LogFactory.getLog(OutboundProvisioningManager.class);
    private static CacheBackedProvisioningMgtDAO dao = new CacheBackedProvisioningMgtDAO(
            new ProvisioningManagementDAO());

    private static OutboundProvisioningManager provisioningManager = new OutboundProvisioningManager();

    private OutboundProvisioningManager() {

    }

    /**
     * 
     * @return
     */
    public static OutboundProvisioningManager getInstance() {
        return provisioningManager;
    }

    /**
     * TODO: Need to cache the output from this method.
     * 
     * @return
     * @throws UserStoreException
     */
    private Map<String, RuntimeProvisioningConfig> getOutboundProvisioningConnectors(
            ServiceProvider serviceProvider, String tenantDomainName) throws UserStoreException {

        Map<String, RuntimeProvisioningConfig> connectors = new HashMap<String, RuntimeProvisioningConfig>();

        // maintain the provisioning connector cache in the super tenant.
        // at the time of provisioning there may not be an authenticated user in the system -
        // specially in the case of in-bound provisioning.

        String tenantDomain = null;
        int tenantId = -1234;
        ServiceProviderProvisioningConnectorCacheKey key = null;
        ServiceProviderProvisioningConnectorCacheEntry entry = null;

        if (CarbonContext.getThreadLocalCarbonContext() != null) {
            tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        }

        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            // reading from the cache
            key = new ServiceProviderProvisioningConnectorCacheKey(serviceProvider.getApplicationName(), tenantDomain);

            entry = (ServiceProviderProvisioningConnectorCacheEntry) ServiceProviderProvisioningConnectorCache.getInstance()
                    .getValueFromCache(key);

            // cache hit
            if (entry != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Provisioning cache HIT for " + serviceProvider + " of "
                            + tenantDomainName);
                }
                return entry.getConnectors();
            }

        } finally {
            PrivilegedCarbonContext.endTenantFlow();

            if (tenantDomain != null) {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
            }
        }

        // NOW build the Map

        // a list of registered provisioning connector factories.
        Map<String, AbstractProvisioningConnectorFactory> registeredConnectorFactories = IdentityProvisionServiceComponent
                .getConnectorFactories();

        // get all registered list of out-bound provisioning connectors registered for the local
        // service provider.
        OutboundProvisioningConfig outboundProvisioningConfiguration = serviceProvider
                .getOutboundProvisioningConfig();

        if (outboundProvisioningConfiguration == null) {
            if (log.isDebugEnabled()) {
                log.debug("No outbound provisioning configuration defined for local service provider.");
            }
            // no out-bound provisioning configuration defined for local service provider.return an
            // empty list.
            return new HashMap<String, RuntimeProvisioningConfig>();
        }

        // get the list of registered provisioning identity providers in out-bound provisioning
        // configuration.
        IdentityProvider[] provisionningIdPList = outboundProvisioningConfiguration
                .getProvisioningIdentityProviders();

        if (provisionningIdPList != null && provisionningIdPList.length > 0) {
            // we have a set of provisioning identity providers registered in our system.

            for (IdentityProvider fIdP : provisionningIdPList) {
                // iterate through the provisioning identity provider list to find out the default
                // provisioning connector of each of the,

                try {

                    AbstractOutboundProvisioningConnector connector;

                    ProvisioningConnectorConfig defaultConnector = fIdP
                            .getDefaultProvisioningConnectorConfig();
                    if (defaultConnector != null) {
                        // if no default provisioning connector defined for this identity provider,
                        // we can safely ignore it - need not to worry about provisioning.

                        String connectorType = fIdP.getDefaultProvisioningConnectorConfig()
                                .getName();

                        boolean enableJitProvisioning = false;

                        if (fIdP.getJustInTimeProvisioningConfig() != null
                                && fIdP.getJustInTimeProvisioningConfig().isProvisioningEnabled()) {
                            enableJitProvisioning = true;
                        }

                        connector = getOutboundProvisioningConnector(fIdP,
                                registeredConnectorFactories, tenantDomainName,
                                enableJitProvisioning);
                        // add to the provisioning connectors list. there will be one item for each
                        // provisioning identity provider found in the out-bound provisioning
                        // configuration of the local service provider.
                        if (connector != null) {
                            RuntimeProvisioningConfig proConfig = new RuntimeProvisioningConfig();
                            proConfig
                                    .setProvisioningConnectorEntry(new SimpleEntry<String, AbstractOutboundProvisioningConnector>(
                                            connectorType, connector));
                            proConfig.setBlocking(defaultConnector.isBlocking());
                            connectors.put(fIdP.getIdentityProviderName(), proConfig);
                        }
                    }

                } catch (IdentityApplicationManagementException e) {
                    throw new UserStoreException("Error while retrieving idp configuration for "
                            + fIdP.getIdentityProviderName(), e);
                }
            }
        }

        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            entry = new ServiceProviderProvisioningConnectorCacheEntry();
            entry.setConnectors(connectors);
            ServiceProviderProvisioningConnectorCache.getInstance().addToCache(key, entry);

        } finally {
            PrivilegedCarbonContext.endTenantFlow();

            if (tenantDomain != null) {

                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Entry added successfully ");
        }

        return connectors;
    }

    /**
     * 
     * @param fIdP
     * @param registeredConnectorFactories
     * @param tenantDomainName
     * @param enableJitProvisioning
     * @return
     * @throws IdentityApplicationManagementException
     * @throws UserStoreException
     */
    private AbstractOutboundProvisioningConnector getOutboundProvisioningConnector(
            IdentityProvider fIdP,
            Map<String, AbstractProvisioningConnectorFactory> registeredConnectorFactories,
            String tenantDomainName, boolean enableJitProvisioning)
            throws IdentityApplicationManagementException, UserStoreException {

        String idpName = fIdP.getIdentityProviderName();

        // name of the default provisioning connector.
        String connectorType = fIdP.getDefaultProvisioningConnectorConfig().getName();

        // get identity provider configuration.
        fIdP = IdentityProviderManager.getInstance().getEnabledIdPByName(idpName, tenantDomainName);

        if (fIdP == null) {
            // This is an exceptional situation. If service provider has connected to an
            // identity provider, that identity provider must be present in the system.
            // If not its an exception.
            throw new UserStoreException(
                    "Provisioning identity provider not available in the system. Idp Name : "
                            + idpName);
        }

        // get a list of provisioning connectors associated with the provisioning
        // identity provider.
        ProvisioningConnectorConfig[] provisioningConfigs = fIdP.getProvisioningConnectorConfigs();

        if (provisioningConfigs != null && provisioningConfigs.length > 0) {

            for (ProvisioningConnectorConfig defaultProvisioningConfig : provisioningConfigs) {

                if (!connectorType.equals(defaultProvisioningConfig.getName())
                        || !defaultProvisioningConfig.isEnabled()) {
                    // we need to find the provisioning connector selected by the service provider.
                    continue;
                }

                // this is how we match the configuration to the runtime. the provisioning
                // connector factory should be registered with the system, with the exact
                // name available in the corresponding configuration.
                AbstractProvisioningConnectorFactory factory = registeredConnectorFactories
                        .get(connectorType);

                // get the provisioning properties associated with a given provisioning
                // connector.
                Property[] provisioningProperties = defaultProvisioningConfig
                        .getProvisioningProperties();

                if (enableJitProvisioning) {
                    Property jitEnabled = new Property();
                    jitEnabled.setName(IdentityProvisioningConstants.JIT_PROVISIONING_ENABLED);
                    jitEnabled.setValue("1");
                    provisioningProperties = IdentityApplicationManagementUtil.concatArrays(
                            provisioningProperties, new Property[] { jitEnabled });
                }

                // get the runtime provisioning connector associate the provisioning
                // identity provider. any given time, a given provisioning identity provider
                // can only be associated with a single provisioning connector.
                return factory.getConnector(idpName, provisioningProperties, tenantDomainName);
            }
        }

        return null;
    }

    /**
     * 
     * @param provisioningEntity
     * @param serviceProviderIdentifier
     * @param inboundClaimDialect
     * @param tenantDomainName
     * @param jitProvisioning
     * @throws IdentityProvisioningException
     */
    public void provision(ProvisioningEntity provisioningEntity, String serviceProviderIdentifier,
            String inboundClaimDialect, String tenantDomainName, boolean jitProvisioning)
            throws IdentityProvisioningException {

        try {

            // get details about the service provider.any in-bound provisioning request via
            // the SOAP based API (or the management console) - or SCIM API with HTTP Basic
            // Authentication is considered as coming from the local service provider.
            ServiceProvider serviceProvider = ApplicationInfoProvider.getInstance()
                    .getServiceProvider(serviceProviderIdentifier, tenantDomainName);

            if (serviceProvider == null) {
                throw new IdentityProvisioningException("Invalid service provider name : "
                        + serviceProviderIdentifier);
            }

            ClaimMapping[] spClaimMappings = null;

            // if we know the serviceProviderClaimDialect - we do not need to find it again.
            if (inboundClaimDialect == null && serviceProvider.getClaimConfig() != null) {
                spClaimMappings = serviceProvider.getClaimConfig().getClaimMappings();
            }

            // get all the provisioning connectors associated with local service provider for
            // out-bound provisioning.
            // TODO: stop loading connectors all the time.
            Map<String, RuntimeProvisioningConfig> connectors = getOutboundProvisioningConnectors(
                    serviceProvider, tenantDomainName);

            ProvisioningEntity outboundProEntity;

            ExecutorService executors = null;

            if (connectors.size() > 0) {
                executors = Executors.newFixedThreadPool(connectors.size());
            }

            for (Iterator<Entry<String, RuntimeProvisioningConfig>> iterator = connectors
                    .entrySet().iterator(); iterator.hasNext();) {

                Entry<String, RuntimeProvisioningConfig> entry = iterator.next();

                Entry<String, AbstractOutboundProvisioningConnector> connectorEntry = entry
                        .getValue().getProvisioningConnectorEntry();

                AbstractOutboundProvisioningConnector connector = connectorEntry.getValue();
                String connectorType = connectorEntry.getKey();
                String idPName = entry.getKey();

                IdentityProvider provisioningIdp = IdentityProviderManager.getInstance()
                        .getIdPByName(idPName, tenantDomainName);

                if (provisioningIdp == null) {
                    // this is an exception if we cannot find the provisioning identity provider
                    // by its name.
                    throw new IdentityProvisioningException("Invalid identity provider name : "
                            + idPName);
                }

                String outboundClaimDialect = connector.getClaimDialectUri();

                if (outboundClaimDialect == null
                        && (provisioningIdp.getClaimConfig() == null || provisioningIdp
                                .getClaimConfig().isLocalClaimDialect())) {
                    outboundClaimDialect = DefaultInboundUserProvisioningListener.WSO2_CARBON_DIALECT;
                }

                ClaimMapping[] idpClaimMappings = null;

                if (provisioningIdp.getClaimConfig() != null) {
                    idpClaimMappings = provisioningIdp.getClaimConfig().getClaimMappings();
                }

                // TODO: this should happen asynchronously in a different thread.
                // create a new provisioning entity object for each provisioning identity
                // provider.

                Map<ClaimMapping, List<String>> mapppedClaims;

                // get mapped claims.
                mapppedClaims = getMappedClaims(inboundClaimDialect, outboundClaimDialect,
                        provisioningEntity, spClaimMappings, idpClaimMappings, tenantDomainName);

                if (provisioningIdp.getPermissionAndRoleConfig() != null) {
                    // update with mapped user groups.
                    updateProvisioningUserWithMappedRoles(provisioningEntity, provisioningIdp
                            .getPermissionAndRoleConfig().getRoleMappings());
                }

                // check whether we already have the provisioned identifier - if
                // so set it.
                ProvisionedIdentifier provisionedIdentifier;

                provisionedIdentifier = getProvisionedEntityIdentifier(idPName, connectorType,
                        provisioningEntity, tenantDomainName);

                ProvisioningOperation provisioningOp = provisioningEntity.getOperation();

                if (provisionedIdentifier == null || provisionedIdentifier.getIdentifier() == null) {
                    provisioningOp = ProvisioningOperation.POST;
                }

                String[] provisionByRoleList = new String[0];

                if (provisioningIdp.getProvisioningRole() != null) {
                    provisionByRoleList = provisioningIdp.getProvisioningRole().split(",");
                }

                // see whether the given provisioning entity satisfies the conditions to be
                // provisioned.

                if (!canUserBeProvisioned(provisioningEntity, provisionByRoleList, tenantDomainName)) {
                    
                    if (!canUserBeDeProvisioned(provisionedIdentifier)) {
                        continue;
                    } else {
                        // This is used when user removed from the provisioning role
                        provisioningOp = ProvisioningOperation.DELETE;
                    }

                }

                outboundProEntity = new ProvisioningEntity(provisioningEntity.getEntityType(),
                        provisioningEntity.getEntityName(), provisioningOp, mapppedClaims);

                outboundProEntity.setIdentifier(provisionedIdentifier);
                outboundProEntity.setJitProvisioning(jitProvisioning);

                ProvisioningThread proThread = new ProvisioningThread(outboundProEntity,
                        tenantDomainName, connector, connectorType, idPName, dao);

                if (!entry.getValue().isBlocking()) {
                    executors.execute(proThread);
                } else {
                    proThread.run();
                }

            }

            if (executors != null) {
                executors.shutdown();
            }

        } catch (Exception e) {
            log.error("Error while out-bound provisioning.", e);
        }

    }

    /**
     * 
     * @param provisioningEntity
     * @param idPRoleMapping
     */
    private void updateProvisioningUserWithMappedRoles(ProvisioningEntity provisioningEntity,
            RoleMapping[] idPRoleMapping) {

        if (provisioningEntity.getEntityType() != ProvisioningEntityType.USER
                || idPRoleMapping == null || idPRoleMapping.length == 0) {
            return;
        }

        List<String> userGroups = getGroupNames(provisioningEntity.getAttributes());

        if (userGroups == null || userGroups.size() == 0) {
            return;
        }

        Map<String, String> mappedRoles = new HashMap<String, String>();

        for (RoleMapping mapping : idPRoleMapping) {
            mappedRoles.put(mapping.getLocalRole().getLocalRoleName(), mapping.getRemoteRole());
        }

        List<String> mappedUserGroups = new ArrayList<String>();

        for (Iterator<String> iterator = userGroups.iterator(); iterator.hasNext();) {
            String userGroup = iterator.next();
            String mappedGroup = null;
            if ((mappedGroup = mappedRoles.get(userGroup)) != null) {
                mappedUserGroups.add(mappedGroup);
            }
        }

        ProvisioningUtil.setClaimValue(IdentityProvisioningConstants.GROUP_CLAIM_URI,
                provisioningEntity.getAttributes(), mappedUserGroups);

    }

    /**
     * 
     * @param inboundClaimDialect
     * @param outboundClaimDialect
     * @param provisioningEntity
     * @param spClaimMappings
     * @param idpClaimMappings
     * @return
     * @throws IdentityApplicationManagementException
     */
    private Map<ClaimMapping, List<String>> getMappedClaims(String inboundClaimDialect,
            String outboundClaimDialect, ProvisioningEntity provisioningEntity,
            ClaimMapping[] spClaimMappings, ClaimMapping[] idpClaimMappings, String tenantDomainName)
            throws IdentityApplicationManagementException {

        // if we have any in-bound attributes - need to convert those into out-bound
        // attributes in a form understood by the external provisioning providers.
        Map<String, String> inboundAttributes = provisioningEntity.getInboundAttributes();

        //Create copy of provisioningEntity attributes to add connector specific outbound claim value mappings
        Map<ClaimMapping, List<String>> outboundClaimValueMappings =new HashMap<ClaimMapping, List<String>>(provisioningEntity.getAttributes());
        if (outboundClaimDialect != null) {
            // out-bound claim dialect is not provisioning provider specific. Its
            // specific to the connector.

            if (inboundClaimDialect == null) {
                // in-bound claim dialect is service provider specific.
                // we have read the claim mapping from service provider claim
                // configuration.
                return IdentityApplicationManagementUtil.getMappedClaims(outboundClaimDialect,
                        inboundAttributes, spClaimMappings, outboundClaimValueMappings,
                        tenantDomainName);
            } else {
                // in-bound claim dialect is not service provider specific.
                // its been supplied by the corresponding in-bound provisioning servlet
                // or listener.
                return IdentityApplicationManagementUtil.getMappedClaims(outboundClaimDialect,
                        inboundAttributes, inboundClaimDialect, outboundClaimValueMappings,
                        tenantDomainName);
            }
        } else {
            // out-bound claim dialect is provisioning provider specific.
            // we have read the claim mapping from identity provider claim
            // configuration

            if (inboundClaimDialect == null) {
                // in-bound claim dialect is service provider specific.
                // we have read the claim mapping from service provider claim
                // configuration.
                return IdentityApplicationManagementUtil.getMappedClaims(idpClaimMappings,
                        inboundAttributes, spClaimMappings, outboundClaimValueMappings);
            } else {
                // in-bound claim dialect is not service provider specific.
                // its been supplied by the corresponding in-bound provisioning servlet
                // or listener.
                return IdentityApplicationManagementUtil.getMappedClaims(idpClaimMappings,
                        inboundAttributes, inboundClaimDialect, outboundClaimValueMappings,
                        tenantDomainName);
            }
        }
    }

    /**
     * 
     * @param attributeMap
     * @return
     */
    protected List<String> getGroupNames(Map<ClaimMapping, List<String>> attributeMap) {
        return ProvisioningUtil.getClaimValues(attributeMap,
                IdentityProvisioningConstants.GROUP_CLAIM_URI, null);
    }

    /**
     * 
     * @param attributeMap
     * @return
     */
    private String getUserName(Map<ClaimMapping, List<String>> attributeMap) {
        List<String> userList = ProvisioningUtil.getClaimValues(attributeMap,
                IdentityProvisioningConstants.USERNAME_CLAIM_URI, null);

        if (userList != null && userList.size() > 0) {
            return userList.get(0);
        }

        return null;
    }

    /**
     * 
     * @param provisioningEntity
     * @param provisionByRoleList
     * @param tenantDomain
     * @return
     * @throws CarbonException
     * @throws UserStoreException
     */
    protected boolean canUserBeProvisioned(ProvisioningEntity provisioningEntity,
            String[] provisionByRoleList, String tenantDomain) throws UserStoreException,
            CarbonException {

        if (provisioningEntity.getEntityType() != ProvisioningEntityType.USER
                || provisionByRoleList == null || provisionByRoleList.length == 0) {
            // we apply restrictions only for users.
            // if service provider's out-bound provisioning configuration does not define any roles
            // to be provisioned then we apply no restrictions.
            return true;
        }

        String userName = getUserName(provisioningEntity.getAttributes());
        List<String> roleListOfUser = getUserRoles(userName, tenantDomain);

        for (String provisionByRole : provisionByRoleList) {
            if (roleListOfUser.contains(provisionByRole)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 
     * @param provisionedIdentifier
     * @return
     * @throws CarbonException
     * @throws UserStoreException
     */
    protected boolean canUserBeDeProvisioned(ProvisionedIdentifier provisionedIdentifier)
            throws UserStoreException, CarbonException, IdentityApplicationManagementException {

        // check whether we already have the provisioned identifier.current idp is not eligible to
        // provisioning.
        if (provisionedIdentifier != null && provisionedIdentifier.getIdentifier() != null) {
            return true;
        }

        return false;
    }

    /**
     * 
     * @param userName
     * @param tenantDomain
     * @return
     * @throws CarbonException
     * @throws UserStoreException
     */
    private List<String> getUserRoles(String userName, String tenantDomain) throws CarbonException,
            UserStoreException {

        RegistryService registryService = IdentityProvisionServiceComponent.getRegistryService();
        RealmService realmService = IdentityProvisionServiceComponent.getRealmService();

        UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(registryService,
                realmService, tenantDomain);

        UserStoreManager userstore = null;
        userstore = realm.getUserStoreManager();
        String[] newRoles = userstore.getRoleListOfUser(userName);
        return Arrays.asList(newRoles);
    }

    /**
     * 
     * @param idpName
     * @param connectorType
     * @param provisioningEntity
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    private ProvisionedIdentifier getProvisionedEntityIdentifier(String idpName,
            String connectorType, ProvisioningEntity provisioningEntity, String tenantDomain)
            throws IdentityApplicationManagementException {
        int tenantId = getTenantIdOfDomain(tenantDomain);
        return dao.getProvisionedIdentifier(idpName, connectorType, provisioningEntity, tenantId, tenantDomain);
    }

    /**
     * Get the tenant id of the given tenant domain.
     * 
     * @param tenantDomain Tenant Domain
     * @return Tenant Id of domain user belongs to.
     * @throws IdentityApplicationManagementException Error when getting tenant id from tenant
     *         domain
     */
    private static int getTenantIdOfDomain(String tenantDomain)
            throws IdentityApplicationManagementException {

        try {
            return IdPManagementUtil.getTenantIdOfDomain(tenantDomain);
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            String msg = "Error occurred while getting Tenant Id from Tenant domain "
                    + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        }
    }
}
