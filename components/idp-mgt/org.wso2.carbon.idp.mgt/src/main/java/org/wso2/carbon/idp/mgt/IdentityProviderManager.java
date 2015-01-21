/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.idp.mgt;

import org.apache.axiom.om.util.Base64;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.common.ApplicationAuthenticatorService;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.ProvisioningConnectorService;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.idp.mgt.dao.CacheBackedIdPMgtDAO;
import org.wso2.carbon.idp.mgt.dao.FileBasedIdPMgtDAO;
import org.wso2.carbon.idp.mgt.dao.IdPManagementDAO;
import org.wso2.carbon.idp.mgt.internal.IdPManagementServiceComponent;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.security.KeyStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdentityProviderManager {

    private static Log log = LogFactory.getLog(IdentityProviderManager.class);

    private static CacheBackedIdPMgtDAO dao = new CacheBackedIdPMgtDAO(new IdPManagementDAO());

    private static volatile IdentityProviderManager instance = new IdentityProviderManager();

    /**
     * 
     * @return
     */
    public static IdentityProviderManager getInstance() {
        return instance;
    }

    private IdentityProviderManager() {

    }

    /**
     * Retrieves resident Identity provider for a given tenant
     * 
     * @param tenantDomain Tenant domain whose resident IdP is requested
     * @return <code>LocalIdentityProvider</code>
     * @throws IdentityApplicationManagementException Error when getting Resident Identity Providers
     */
    public IdentityProvider getResidentIdP(String tenantDomain)
            throws IdentityApplicationManagementException {

        String tenantContext = "";
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equalsIgnoreCase(tenantDomain)) {
            tenantContext = MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + tenantDomain + "/";
        }
        String hostName = ServerConfiguration.getInstance().getFirstProperty("HostName");
        String mgtTransport = CarbonUtils.getManagementTransport();
        AxisConfiguration axisConfiguration = IdPManagementServiceComponent
                .getConfigurationContextService().getServerConfigContext().getAxisConfiguration();
        int mgtTransportPort = CarbonUtils.getTransportProxyPort(axisConfiguration, mgtTransport);
        if (mgtTransportPort <= 0) {
            mgtTransportPort = CarbonUtils.getTransportPort(axisConfiguration, mgtTransport);
        }
        String serverUrl = mgtTransport + "://" + hostName + ":" + mgtTransportPort + "/";
        String stsUrl = serverUrl + "services/" + tenantContext + "wso2carbon-sts";
        String openIdUrl = serverUrl + "openid";
        String samlSSOUrl = serverUrl + "samlsso";
        String samlLogoutUrl = serverUrl + "samlsso";
        String authzUrl = serverUrl + "oauth2/authz";
        String tokenUrl = serverUrl + "oauth2/token";
        String userUrl = serverUrl + "oauth2/userinfo";
        String scimUserEndpoint = serverUrl + "wso2/scim/Users";
        String scimGroupsEndpoint = serverUrl + "wso2/scim/Groups";

        IdentityProvider identityProvider = dao.getIdPByName(null,
                IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME,
                getTenantIdOfDomain(tenantDomain), tenantDomain);
        if (identityProvider == null) {
            String message = "Could not find Resident Identity Provider for tenant " + tenantDomain;
            log.error(message);
            throw new IdentityApplicationManagementException(message);
        }

        int tenantId = -1;
        try {
            tenantId = IdPManagementServiceComponent.getRealmService().getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new IdentityApplicationManagementException(
                    "Exception occurred while retrieving Tenant ID from Tenant Domain " + tenantDomain, e);
        }
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantId);
        X509Certificate cert = null;
        try {
            if(!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)){
                // derive key store name
                String ksName = tenantDomain.trim().replace(".", "-");
                // derive JKS name
                String jksName = ksName + ".jks";
                KeyStore keyStore = keyStoreManager.getKeyStore(jksName);
                cert = (X509Certificate)keyStore.getCertificate(tenantDomain);
            } else {
                cert = keyStoreManager.getDefaultPrimaryCertificate();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IdentityApplicationManagementException(
                    "Error retrieving primary certificate for tenant : " + tenantDomain);
        }
        if(cert == null){
            throw new IdentityApplicationManagementException(
                    "Cannot find the primary certificate for tenant " + tenantDomain);
        }
        try {
            identityProvider.setCertificate(Base64.encode(cert.getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error(e.getMessage(), e);
            throw new IdentityApplicationManagementException(
                    "Error occurred while encoding primary certificate for tenant domain " + tenantDomain);
        }

        List<FederatedAuthenticatorConfig> fedAuthnCofigs = new ArrayList<FederatedAuthenticatorConfig>();
        List<Property> propertiesList = null;

        FederatedAuthenticatorConfig openIdFedAuthn = IdentityApplicationManagementUtil
                .getFederatedAuthenticator(identityProvider.getFederatedAuthenticatorConfigs(),
                        IdentityApplicationConstants.Authenticator.OpenID.NAME);
        if (openIdFedAuthn == null) {
            openIdFedAuthn = new FederatedAuthenticatorConfig();
            openIdFedAuthn.setName(IdentityApplicationConstants.Authenticator.OpenID.NAME);
        }
        propertiesList = new ArrayList<Property>(Arrays.asList(openIdFedAuthn.getProperties()));
        if (IdentityApplicationManagementUtil.getProperty(openIdFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.OpenID.OPEN_ID_URL) == null) {
            Property openIdUrlProp = new Property();
            openIdUrlProp.setName(IdentityApplicationConstants.Authenticator.OpenID.OPEN_ID_URL);
            openIdUrlProp.setValue(openIdUrl);
            propertiesList.add(openIdUrlProp);
        }
        openIdFedAuthn.setProperties(propertiesList.toArray(new Property[propertiesList.size()]));
        fedAuthnCofigs.add(openIdFedAuthn);

        FederatedAuthenticatorConfig saml2SSOFedAuthn = IdentityApplicationManagementUtil
                .getFederatedAuthenticator(identityProvider.getFederatedAuthenticatorConfigs(),
                        IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);
        if (saml2SSOFedAuthn == null) {
            saml2SSOFedAuthn = new FederatedAuthenticatorConfig();
            saml2SSOFedAuthn.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);
        }
        propertiesList = new ArrayList<Property>(Arrays.asList(saml2SSOFedAuthn.getProperties()));
        if (IdentityApplicationManagementUtil.getProperty(saml2SSOFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.SAML2SSO.SSO_URL) == null) {
            Property ssoUrlProp = new Property();
            ssoUrlProp.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.SSO_URL);
            ssoUrlProp.setValue(samlSSOUrl);
            propertiesList.add(ssoUrlProp);
        }
        if (IdentityApplicationManagementUtil.getProperty(saml2SSOFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.SAML2SSO.LOGOUT_REQ_URL) == null) {
            Property logoutReqUrlProp = new Property();
            logoutReqUrlProp
                    .setName(IdentityApplicationConstants.Authenticator.SAML2SSO.LOGOUT_REQ_URL);
            logoutReqUrlProp.setValue(samlLogoutUrl);
            propertiesList.add(logoutReqUrlProp);
        }
        if (IdentityApplicationManagementUtil.getProperty(saml2SSOFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID) == null) {
            Property idPEntityIdProp = new Property();
            idPEntityIdProp
                    .setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID);
            idPEntityIdProp.setValue(identityProvider.getHomeRealmId());
            propertiesList.add(idPEntityIdProp);
        }
        saml2SSOFedAuthn.setProperties(propertiesList.toArray(new Property[propertiesList.size()]));
        fedAuthnCofigs.add(saml2SSOFedAuthn);

        FederatedAuthenticatorConfig oidcFedAuthn = IdentityApplicationManagementUtil
                .getFederatedAuthenticator(identityProvider.getFederatedAuthenticatorConfigs(),
                        IdentityApplicationConstants.Authenticator.OIDC.NAME);
        if (oidcFedAuthn == null) {
            oidcFedAuthn = new FederatedAuthenticatorConfig();
            oidcFedAuthn.setName(IdentityApplicationConstants.Authenticator.OIDC.NAME);
        }
        propertiesList = new ArrayList<Property>(Arrays.asList(oidcFedAuthn.getProperties()));
        if (IdentityApplicationManagementUtil.getProperty(oidcFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL) == null) {
            Property authzUrlProp = new Property();
            authzUrlProp.setName(IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL);
            authzUrlProp.setValue(authzUrl);
            propertiesList.add(authzUrlProp);
        }
        if (IdentityApplicationManagementUtil.getProperty(oidcFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL) == null) {
            Property tokenUrlProp = new Property();
            tokenUrlProp.setName(IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL);
            tokenUrlProp.setValue(tokenUrl);
            propertiesList.add(tokenUrlProp);
        }
        if (IdentityApplicationManagementUtil.getProperty(oidcFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.OIDC.USER_INFO_URL) == null) {
            Property userInfoUrlProp = new Property();
            userInfoUrlProp.setName(IdentityApplicationConstants.Authenticator.OIDC.USER_INFO_URL);
            userInfoUrlProp.setValue(userUrl);
            propertiesList.add(userInfoUrlProp);
        }
        oidcFedAuthn.setProperties(propertiesList.toArray(new Property[propertiesList.size()]));
        fedAuthnCofigs.add(oidcFedAuthn);

        FederatedAuthenticatorConfig passiveSTSFedAuthn = IdentityApplicationManagementUtil
                .getFederatedAuthenticator(identityProvider.getFederatedAuthenticatorConfigs(),
                        IdentityApplicationConstants.Authenticator.PassiveSTS.NAME);
        if (passiveSTSFedAuthn == null) {
            passiveSTSFedAuthn = new FederatedAuthenticatorConfig();
            passiveSTSFedAuthn.setName(IdentityApplicationConstants.Authenticator.PassiveSTS.NAME);
        }
        propertiesList = new ArrayList<Property>(Arrays.asList(passiveSTSFedAuthn.getProperties()));
        if (IdentityApplicationManagementUtil.getProperty(saml2SSOFedAuthn.getProperties(),
                IdentityApplicationConstants.Authenticator.PassiveSTS.PASSIVE_STS_URL) == null) {
            Property passiveSTSUrlProp = new Property();
            passiveSTSUrlProp
                    .setName(IdentityApplicationConstants.Authenticator.PassiveSTS.PASSIVE_STS_URL);
            passiveSTSUrlProp.setValue(stsUrl);
            propertiesList.add(passiveSTSUrlProp);
        }

        passiveSTSFedAuthn
                .setProperties(propertiesList.toArray(new Property[propertiesList.size()]));
        fedAuthnCofigs.add(passiveSTSFedAuthn);
        identityProvider.setFederatedAuthenticatorConfigs(fedAuthnCofigs
                .toArray(new FederatedAuthenticatorConfig[fedAuthnCofigs.size()]));

        ProvisioningConnectorConfig scimProvConn = IdentityApplicationManagementUtil
                .getProvisioningConnector(identityProvider.getProvisioningConnectorConfigs(),
                        "scim");
        if (scimProvConn == null) {
            scimProvConn = new ProvisioningConnectorConfig();
            scimProvConn.setName("scim");
        }
        propertiesList = new ArrayList<Property>(Arrays.asList(scimProvConn
                .getProvisioningProperties()));
        if (IdentityApplicationManagementUtil.getProperty(scimProvConn.getProvisioningProperties(),
                "scimUserEndpoint") == null) {
            Property property = new Property();
            property.setName("scimUserEndpoint");
            property.setValue(scimUserEndpoint);
            propertiesList.add(property);
        }
        if (IdentityApplicationManagementUtil.getProperty(scimProvConn.getProvisioningProperties(),
                "scimUserEndpoint") == null) {
            Property property = new Property();
            property.setName("scimGroupEndpoint");
            property.setValue(scimGroupsEndpoint);
            propertiesList.add(property);
        }
        scimProvConn.setProvisioningProperties(propertiesList.toArray(new Property[propertiesList
                .size()]));
        identityProvider
                .setProvisioningConnectorConfigs(new ProvisioningConnectorConfig[] { scimProvConn });

        return identityProvider;
    }

    /**
     * Add Resident Identity provider for a given tenant
     * 
     * @param identityProvider <code>IdentityProvider</code>
     * @param tenantDomain Tenant domain whose resident IdP is requested
     * @throws IdentityApplicationManagementException Error when adding Resident Identity Provider
     */
    public void addResidentIdP(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (identityProvider.getHomeRealmId() == null
                || identityProvider.getHomeRealmId().equals("")) {
            String msg = "Invalid argument: Resident Identity Provider Home Realm Identifier value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        if (identityProvider.getFederatedAuthenticatorConfigs() == null) {
            identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[0]);
        }
        FederatedAuthenticatorConfig fedAuthnConfig = IdentityApplicationManagementUtil
                .getFederatedAuthenticator(identityProvider.getFederatedAuthenticatorConfigs(),
                        IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);
        if (fedAuthnConfig == null) {
            fedAuthnConfig = new FederatedAuthenticatorConfig();
            fedAuthnConfig.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);
        }
        if (fedAuthnConfig.getProperties() == null) {
            fedAuthnConfig.setProperties(new Property[0]);
        }
        
		boolean idPEntityIdAvailable = false;
		for (Property property : fedAuthnConfig.getProperties()) {
			if (IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID.equals(property.getName())) {
				idPEntityIdAvailable = true;
			}
		}
		if (!idPEntityIdAvailable) {
			Property property = new Property();
			property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID);
			property.setValue("localhost");
			if (fedAuthnConfig.getProperties().length > 0) {
				List<Property> properties = Arrays.asList(fedAuthnConfig.getProperties());
				properties.add(property);
				fedAuthnConfig.setProperties((Property[]) properties.toArray());
			} else {
				fedAuthnConfig.setProperties(new Property[] { property });
			}
		}

        FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = {fedAuthnConfig};
        identityProvider.setFederatedAuthenticatorConfigs(IdentityApplicationManagementUtil
                .concatArrays(identityProvider.getFederatedAuthenticatorConfigs(),federatedAuthenticatorConfigs));

        dao.addIdP(identityProvider, getTenantIdOfDomain(tenantDomain), tenantDomain);
    }

    /**
     * Update Resident Identity provider for a given tenant
     * 
     * @param identityProvider <code>IdentityProvider</code>
     * @param tenantDomain Tenant domain whose resident IdP is requested
     * @throws IdentityApplicationManagementException Error when updating Resident Identity Provider
     */
    public void updateResidentIdP(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (identityProvider.getHomeRealmId() == null
                || identityProvider.getHomeRealmId().equals("")) {
            String msg = "Invalid argument: Resident Identity Provider Home Realm Identifier value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        if (identityProvider.getFederatedAuthenticatorConfigs() == null) {
            identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[0]);
        }

        IdentityProvider currentIdP = IdentityProviderManager.getInstance().getIdPByName(
                IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME, tenantDomain, true);
        
		int tenantId = getTenantIdOfDomain(tenantDomain);
		validateUpdateOfIdPEntityId(currentIdP.getFederatedAuthenticatorConfigs(),
		                            identityProvider.getFederatedAuthenticatorConfigs(), tenantId, tenantDomain);
        
        dao.updateIdP(identityProvider, currentIdP, tenantId, tenantDomain);
    }

    /**
     * Retrieves registered Identity providers for a given tenant
     * 
     * @param tenantDomain Tenant domain whose IdP names are requested
     * @return Set of <code>IdentityProvider</code>. IdP names, primary IdP and home realm
     *         identifiers of each IdP
     * @throws IdentityApplicationManagementException Error when getting list of Identity Providers
     */
    public List<IdentityProvider> getIdPs(String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        return dao.getIdPs(null, tenantId, tenantDomain);

    }
    
    /**
     * Retrieves registered Enabled Identity providers for a given tenant
     * 
     * @param tenantDomain Tenant domain whose IdP names are requested
     * @return Set of <code>IdentityProvider</code>. IdP names, primary IdP and home realm
     *         identifiers of each IdP
     * @throws IdentityApplicationManagementException Error when getting list of Identity Providers
     */
    public List<IdentityProvider> getEnabledIdPs(String tenantDomain)
            throws IdentityApplicationManagementException {
    	List<IdentityProvider> enabledIdentityProviders = new ArrayList<IdentityProvider>();
        List<IdentityProvider> identityProviers = getIdPs(tenantDomain);
        
        for(IdentityProvider idp : identityProviers) {
        	if(idp.isEnable()) {
        		enabledIdentityProviders.add(idp);
        	}
        }
        return enabledIdentityProviders;
        
    }
    
    

    /**
     * 
     * @param idPName
     * @param tenantDomain
     * @param ignoreFileBasedIdps
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByName(String idPName, String tenantDomain,
            boolean ignoreFileBasedIdps) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        IdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId, tenantDomain);

        if (!ignoreFileBasedIdps) {

            if (identityProvider == null) {
                identityProvider = new FileBasedIdPMgtDAO().getIdPByName(idPName, tenantDomain);
            }

            if (identityProvider == null) {
                identityProvider = IdPManagementServiceComponent.getFileBasedIdPs().get(
                        IdentityApplicationConstants.DEFAULT_IDP_CONFIG);
            }
        }

        return identityProvider;
    }
    
    
    /**
     * 
     * @param idPName
     * @param tenantDomain
     * @param ignoreFileBasedIdps
     * @return
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getEnabledIdPByName(String idPName, String tenantDomain,
            boolean ignoreFileBasedIdps) throws IdentityApplicationManagementException {

    	IdentityProvider idp = getIdPByName(idPName, tenantDomain, ignoreFileBasedIdps);
    	if(idp != null && idp.isEnable()) {
    		return idp;
    	}
        return null;
    }

    /**
     * Retrieves Identity provider information about a given tenant by Identity Provider name
     * 
     * @param idPName Unique name of the Identity provider of whose information is requested
     * @param tenantDomain Tenant domain whose information is requested
     * @return <code>IdentityProvider</code> Identity Provider information
     * @throws IdentityApplicationManagementException Error when enetting Identity Provider
     *         information by IdP name
     */
    public IdentityProvider getIdPByName(String idPName, String tenantDomain)
            throws IdentityApplicationManagementException {
        return getIdPByName(idPName, tenantDomain, false);
    }

    /**
     *
     * @param property IDP authenticator property (E.g.: IdPEntityId)
     * @param value Value associated with given Property
     * @param tenantDomain
     * @return <code>IdentityProvider</code> Identity Provider information
     * @throws IdentityApplicationManagementException Error when getting Identity Provider
     *         information by authenticator property value
     */
    public IdentityProvider getIdPByAuthenticatorPropertyValue(String property, String value, String tenantDomain,
                                                               boolean ignoreFileBasedIdps)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (property == null || property.equals("") || value == null || value.equals("")) {
            String msg = "Invalid argument: Authenticator property or property value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        IdentityProvider identityProvider = dao.getIdPByAuthenticatorPropertyValue(
                null, property, value, tenantId, tenantDomain);

        if(identityProvider == null && !ignoreFileBasedIdps){
           identityProvider =  new FileBasedIdPMgtDAO()
                   .getIdPByAuthenticatorPropertyValue(property, value, tenantDomain);
        }

        return identityProvider;
    }

    /**
     * Retrieves Enabled Identity provider information about a given tenant by Identity Provider name
     * 
     * @param idPName Unique name of the Identity provider of whose information is requested
     * @param tenantDomain Tenant domain whose information is requested
     * @return <code>IdentityProvider</code> Identity Provider information
     * @throws IdentityApplicationManagementException Error when getting Identity Provider
     *         information by IdP name
     */
    public IdentityProvider getEnabledIdPByName(String idPName, String tenantDomain)
            throws IdentityApplicationManagementException {
       	
    	IdentityProvider idp = getIdPByName(idPName, tenantDomain);
    	if(idp != null && idp.isEnable()) {
    		return idp;
    	}
        return null; 
    }

    /**
     * Retrieves Identity provider information about a given tenant by realm identifier
     * 
     * @param realmId Unique realm identifier of the Identity provider of whose information is
     *        requested
     * @param tenantDomain Tenant domain whose information is requested
     * @throws IdentityApplicationManagementException Error when getting Identity Provider
     *         information by IdP home realm identifier
     */
    public IdentityProvider getIdPByRealmId(String realmId, String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (realmId == null || realmId.equals("")) {
            String msg = "Invalid argument: Identity Provider Home Realm Identifier value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        IdentityProvider identityProvider = dao.getIdPByRealmId(realmId, tenantId, tenantDomain);

        if (identityProvider == null) {
            identityProvider = new FileBasedIdPMgtDAO().getIdPByRealmId(realmId, tenantDomain);
        }

        return identityProvider;
    }
    
    /**
     * Retrieves Enabled Identity provider information about a given tenant by realm identifier
     * 
     * @param realmId Unique realm identifier of the Identity provider of whose information is
     *        requested
     * @param tenantDomain Tenant domain whose information is requested
     * @throws IdentityApplicationManagementException Error when getting Identity Provider
     *         information by IdP home realm identifier
     */
    public IdentityProvider getEnabledIdPByRealmId(String realmId, String tenantDomain)
            throws IdentityApplicationManagementException {

    	IdentityProvider idp = getIdPByRealmId(realmId, tenantDomain);
    	if(idp != null && idp.isEnable()) {
    		return idp;
    	}
        return null; 
    }
    
    
    

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique Name of the IdP to which the given IdP claim URIs need to be mapped
     * @param tenantDomain The tenant domain of whose local claim URIs to be mapped
     * @param idPClaimURIs IdP claim URIs which need to be mapped to tenant's local claim URIs
     * @throws IdentityApplicationManagementException Error when getting claim mappings
     */
    public Set<ClaimMapping> getMappedLocalClaims(String idPName, String tenantDomain,
            List<String> idPClaimURIs) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        IdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId, tenantDomain);

        if (identityProvider == null) {
            identityProvider = new FileBasedIdPMgtDAO().getIdPByName(idPName, tenantDomain);
        }

        if (identityProvider == null) {
            identityProvider = IdPManagementServiceComponent.getFileBasedIdPs().get(
                    IdentityApplicationConstants.DEFAULT_IDP_CONFIG);
        }

        ClaimConfig claimConfiguration = identityProvider.getClaimConfig();

        if (claimConfiguration != null) {

            ClaimMapping[] claimMappings = claimConfiguration.getClaimMappings();

            if (claimMappings != null && claimMappings.length > 0 && idPClaimURIs != null) {
                Set<ClaimMapping> returnSet = new HashSet<ClaimMapping>();
                for (String idpClaim : idPClaimURIs) {
                    for (ClaimMapping claimMapping : claimMappings) {
                        if (claimMapping.getRemoteClaim().getClaimUri().equals(idpClaim)) {
                            returnSet.add(claimMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }

        return new HashSet<ClaimMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique Name of the IdP to which the given IdP claim URIs need to be mapped
     * @param tenantDomain The tenant domain of whose local claim URIs to be mapped
     * @param idPClaimURIs IdP claim URIs which need to be mapped to tenant's local claim URIs
     * @throws IdentityApplicationManagementException Error when getting claim mappings
     */
    public Map<String, String> getMappedLocalClaimsMap(String idPName, String tenantDomain,
            List<String> idPClaimURIs) throws IdentityApplicationManagementException {

        Set<ClaimMapping> claimMappings = getMappedLocalClaims(idPName, tenantDomain, idPClaimURIs);
        Map<String, String> returnMap = new HashMap<String, String>();
        for (ClaimMapping claimMapping : claimMappings) {
            returnMap.put(claimMapping.getRemoteClaim().getClaimUri(), claimMapping.getLocalClaim()
                    .getClaimUri());
        }
        return returnMap;
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique Name of the IdP to which the given local claim URIs need to be mapped
     * @param tenantDomain The tenant domain of whose local claim URIs to be mapped
     * @param localClaimURIs Local claim URIs which need to be mapped to IdP's claim URIs
     * @throws IdentityApplicationManagementException Error when getting claim mappings
     */
    public Set<ClaimMapping> getMappedIdPClaims(String idPName, String tenantDomain,
            List<String> localClaimURIs) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        IdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId, tenantDomain);

        if (identityProvider == null) {
            identityProvider = new FileBasedIdPMgtDAO().getIdPByName(idPName, tenantDomain);
        }

        if (identityProvider == null) {
            identityProvider = IdPManagementServiceComponent.getFileBasedIdPs().get(
                    IdentityApplicationConstants.DEFAULT_IDP_CONFIG);
        }

        ClaimConfig claimConfiguration = identityProvider.getClaimConfig();

        if (claimConfiguration != null) {

            ClaimMapping[] claimMappings = claimConfiguration.getClaimMappings();

            if (claimMappings != null && claimMappings.length > 0 && localClaimURIs != null) {
                Set<ClaimMapping> returnSet = new HashSet<ClaimMapping>();
                for (String localClaimURI : localClaimURIs) {
                    for (ClaimMapping claimMapping : claimMappings) {
                        if (claimMapping.equals(localClaimURI)) {
                            returnSet.add(claimMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }
        return new HashSet<ClaimMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique Name of the IdP to which the given local claim URIs need to be mapped
     * @param tenantDomain The tenant domain of whose local claim URIs to be mapped
     * @param localClaimURIs Local claim URIs which need to be mapped to IdP's claim URIs
     * @throws IdentityApplicationManagementException Error when getting claim mappings
     */
    public Map<String, String> getMappedIdPClaimsMap(String idPName, String tenantDomain,
            List<String> localClaimURIs) throws IdentityApplicationManagementException {

        Set<ClaimMapping> claimMappings = getMappedIdPClaims(idPName, tenantDomain, localClaimURIs);
        Map<String, String> returnMap = new HashMap<String, String>();
        for (ClaimMapping claimMapping : claimMappings) {
            returnMap.put(claimMapping.getLocalClaim().getClaimUri(), claimMapping.getRemoteClaim()
                    .getClaimUri());
        }
        return returnMap;
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique name of the IdP to which the given IdP roles need to be mapped
     * @param tenantDomain The tenant domain of whose local roles to be mapped
     * @param idPRoles IdP roles which need to be mapped to local roles
     * @throws IdentityApplicationManagementException Error when getting role mappings
     */
    public Set<RoleMapping> getMappedLocalRoles(String idPName, String tenantDomain,
            String[] idPRoles) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);

        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            throw new IdentityApplicationManagementException(msg);
        }

        IdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId, tenantDomain);

        if (identityProvider == null) {
            identityProvider = new FileBasedIdPMgtDAO().getIdPByName(idPName, tenantDomain);
        }

        if (identityProvider == null) {
            identityProvider = IdPManagementServiceComponent.getFileBasedIdPs().get(
                    IdentityApplicationConstants.DEFAULT_IDP_CONFIG);
        }

        PermissionsAndRoleConfig roleConfiguration = identityProvider.getPermissionAndRoleConfig();

        if (roleConfiguration != null) {
            RoleMapping[] roleMappings = roleConfiguration.getRoleMappings();

            if (roleMappings != null && roleMappings.length > 0 && idPRoles != null) {
                Set<RoleMapping> returnSet = new HashSet<RoleMapping>();
                for (String idPRole : idPRoles) {
                    for (RoleMapping roleMapping : roleMappings) {
                        if (roleMapping.getRemoteRole().equals(idPRole)) {
                            returnSet.add(roleMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }
        return new HashSet<RoleMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique name of the IdP to which the given IdP roles need to be mapped
     * @param tenantDomain The tenant domain of whose local roles to be mapped
     * @param idPRoles IdP roles which need to be mapped to local roles
     * @throws IdentityApplicationManagementException Error when getting role mappings
     */
    public Map<String, LocalRole> getMappedLocalRolesMap(String idPName, String tenantDomain,
            String[] idPRoles) throws IdentityApplicationManagementException {

        Set<RoleMapping> roleMappings = getMappedLocalRoles(idPName, tenantDomain, idPRoles);
        Map<String, LocalRole> returnMap = new HashMap<String, LocalRole>();
        for (RoleMapping roleMapping : roleMappings) {
            returnMap.put(roleMapping.getRemoteRole(), roleMapping.getLocalRole());
        }
        return returnMap;
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique name of the IdP to which the given local roles need to be mapped
     * @param tenantDomain The tenant domain of whose local roles need to be mapped
     * @param localRoles Local roles which need to be mapped to IdP roles
     * @throws IdentityApplicationManagementException Error when getting role mappings
     */
    public Set<RoleMapping> getMappedIdPRoles(String idPName, String tenantDomain,
            LocalRole[] localRoles) throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        IdentityProvider identityProvider = dao.getIdPByName(null, idPName, tenantId, tenantDomain);

        if (identityProvider == null) {
            identityProvider = new FileBasedIdPMgtDAO().getIdPByName(idPName, tenantDomain);
        }

        if (identityProvider == null) {
            identityProvider = IdPManagementServiceComponent.getFileBasedIdPs().get(
                    IdentityApplicationConstants.DEFAULT_IDP_CONFIG);
        }

        PermissionsAndRoleConfig roleConfiguration = identityProvider.getPermissionAndRoleConfig();

        if (roleConfiguration != null) {
            RoleMapping[] roleMappings = roleConfiguration.getRoleMappings();

            if (roleMappings != null && roleMappings.length > 0 && localRoles != null) {
                Set<RoleMapping> returnSet = new HashSet<RoleMapping>();
                for (LocalRole localRole : localRoles) {
                    for (RoleMapping roleMapping : roleMappings) {
                        if (roleMapping.getLocalRole().equals(localRole)) {
                            returnSet.add(roleMapping);
                            break;
                        }
                    }
                }
                return returnSet;
            }
        }
        return new HashSet<RoleMapping>();
    }

    /**
     * Retrieves Identity provider information about a given tenant
     * 
     * @param idPName Unique name of the IdP to which the given local roles need to be mapped
     * @param tenantDomain The tenant domain of whose local roles need to be mapped
     * @param localRoles Local roles which need to be mapped to IdP roles
     * @throws IdentityApplicationManagementException Error when getting role mappings
     */
    public Map<LocalRole, String> getMappedIdPRolesMap(String idPName, String tenantDomain,
            LocalRole[] localRoles) throws IdentityApplicationManagementException {

        Set<RoleMapping> roleMappings = getMappedIdPRoles(idPName, tenantDomain, localRoles);
        Map<LocalRole, String> returnMap = new HashMap<LocalRole, String>();
        for (RoleMapping roleMapping : roleMappings) {
            returnMap.put(roleMapping.getLocalRole(), roleMapping.getRemoteRole());
        }
        return returnMap;
    }

    /**
     * Retrieves the primary Identity provider information for a given tenant
     * 
     * @param tenantDomain The tenant domain of whose primary IdP needs to be retrieved
     * @return primary Identity Provider name and home realm identifier
     * @throws IdentityApplicationManagementException Error when getting primary Identity Provider
     *         information
     */
    public IdentityProvider getPrimaryIdP(String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        IdentityProvider identityProvider = dao.getPrimaryIdP(null, tenantId, tenantDomain);
        if (identityProvider != null) {
            return identityProvider;
        }
        if (log.isDebugEnabled()) {
            log.debug("Primary Identity Provider not found for tenant " + tenantDomain);
        }
        return null;
    }

    /**
     * Adds an Identity Provider to the given tenant
     * 
     * @param identityProvider new Identity Provider information
     * @throws IdentityApplicationManagementException Error when adding Identity Provider
     *         information
     */
    public void addIdP(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);

        if (identityProvider.getIdentityProviderName() == null
                || identityProvider.getIdentityProviderName().equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (IdPManagementServiceComponent.getFileBasedIdPs().containsKey(identityProvider.getIdentityProviderName())
                && !identityProvider.getIdentityProviderName().startsWith(IdPManagementConstants.SHARED_IDP_PREFIX)) {
            //If an IDP with name starting with "SHARED_" is added from UI, It's blocked at the service class
            // before calling this method
            throw new IdentityApplicationManagementException("Identity provider with the name" + identityProvider
                    .getIdentityProviderName() + "exists in the file system.");
        }

        PermissionsAndRoleConfig roleConfiguration = identityProvider.getPermissionAndRoleConfig();

        if (roleConfiguration != null && roleConfiguration.getRoleMappings() != null) {
            for (RoleMapping mapping : roleConfiguration.getRoleMappings()) {
                UserStoreManager usm = null;
                try {
                    usm = IdPManagementServiceComponent.getRealmService()
                            .getTenantUserRealm(tenantId).getUserStoreManager();
                    String role = null;
                    if (mapping.getLocalRole().getUserStoreId() != null) {
                        role = mapping.getLocalRole().getUserStoreId()
                                + CarbonConstants.DOMAIN_SEPARATOR
                                + mapping.getLocalRole().getLocalRoleName();
                    }
                    if (usm.isExistingRole(mapping.getLocalRole().getLocalRoleName())) {
                        // perfect
                    } else if (usm.isExistingRole(mapping.getLocalRole().getLocalRoleName(), true)) {
                        // also fine
                    } else {
                        String msg = "Cannot find tenant role " + role + " for tenant "
                                + tenantDomain;
                        throw new IdentityApplicationManagementException(msg);
                    }
                } catch (UserStoreException e) {
                    String msg = "Error occurred while retrieving UserStoreManager for tenant "
                            + tenantDomain;
                    throw new IdentityApplicationManagementException(msg, e);
                }
            }
        }

        if (IdentityProviderManager.getInstance().getIdPByName(
                identityProvider.getIdentityProviderName(), tenantDomain, true) != null) {
            String msg = "An Identity Provider has already been registered with the name "
                    + identityProvider.getIdentityProviderName() + " for tenant " + tenantDomain;
            throw new IdentityApplicationManagementException(msg);
        }
        
        validateIdPEntityId(identityProvider.getFederatedAuthenticatorConfigs(), tenantId, tenantDomain);

        dao.addIdP(identityProvider, tenantId, tenantDomain);
    }

    /**
     * Deletes an Identity Provider from a given tenant
     * 
     * @param idPName Name of the IdP to be deleted
     * @throws IdentityApplicationManagementException Error when deleting Identity Provider
     *         information
     */
    public void deleteIdP(String idPName, String tenantDomain)
            throws IdentityApplicationManagementException {

        int tenantId = getTenantIdOfDomain(tenantDomain);
        if (idPName == null || idPName.equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }
        dao.deleteIdP(idPName, tenantId, tenantDomain);
    }

    /**
     * Updates a given Identity Provider information
     * 
     * @param oldIdPName existing Identity Provider name
     * @param newIdentityProvider new IdP information
     * @throws IdentityApplicationManagementException Error when updating Identity Provider
     *         information
     */
    public void updateIdP(String oldIdPName, IdentityProvider newIdentityProvider,
            String tenantDomain) throws IdentityApplicationManagementException {

        if (newIdentityProvider == null) {
            String msg = "Invalid argument: 'newIdentityProvider' is NULL\'";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (IdPManagementServiceComponent.getFileBasedIdPs().containsKey(
                newIdentityProvider.getIdentityProviderName())) {
            throw new IdentityApplicationManagementException(
                    "Identity provider with the same name exists in the file system.");
        }

        int tenantId = getTenantIdOfDomain(tenantDomain);

        if (oldIdPName == null || oldIdPName.equals("")) {
            String msg = "Invalid argument: Existing Identity Provider Name value is empty";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        IdentityProvider currentIdentityProvider = this
                .getIdPByName(oldIdPName, tenantDomain, true);
        if (currentIdentityProvider == null) {
            String msg = "Identity Provider with name " + oldIdPName + " does not exist";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (currentIdentityProvider.isPrimary() == true && newIdentityProvider.isPrimary() == false) {
            String msg = "Invalid argument: Cannot unset Identity Provider from primary. "
                    + "Alternatively set new Identity Provider to primary";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (newIdentityProvider.getIdentityProviderName() == null
                || newIdentityProvider.getIdentityProviderName().equals("")) {
            String msg = "Invalid argument: Identity Provider Name value is empty for \'newIdentityProvider\'";
            log.error(msg);
            throw new IdentityApplicationManagementException(msg);
        }

        if (newIdentityProvider.getPermissionAndRoleConfig() != null
                && newIdentityProvider.getPermissionAndRoleConfig().getRoleMappings() != null) {
            for (RoleMapping mapping : newIdentityProvider.getPermissionAndRoleConfig()
                    .getRoleMappings()) {
                UserStoreManager usm = null;
                try {
                    usm = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                            .getUserStoreManager();
                    String role = null;
                    if (mapping.getLocalRole().getUserStoreId() != null) {
                        role = mapping.getLocalRole().getUserStoreId()
                                + CarbonConstants.DOMAIN_SEPARATOR
                                + mapping.getLocalRole().getLocalRoleName();
                    } else {
                        role = mapping.getLocalRole().getLocalRoleName();
                    }
                    if (usm.isExistingRole(mapping.getLocalRole().getLocalRoleName())) {
                        // perfect
                    } else if (usm.isExistingRole(mapping.getLocalRole().getLocalRoleName(), true)) {
                        // also fine
                    } else {
                        String msg = "Cannot find tenant role " + role + " for tenant "
                                + tenantDomain;
                        throw new IdentityApplicationManagementException(msg);
                    }
                } catch (UserStoreException e) {
                    String msg = "Error occurred while retrieving UserStoreManager for tenant "
                            + tenantDomain;
                    log.error(msg);
                    throw new IdentityApplicationManagementException(msg);
                }
            }
        }
        
		validateUpdateOfIdPEntityId(currentIdentityProvider.getFederatedAuthenticatorConfigs(),
		                            newIdentityProvider.getFederatedAuthenticatorConfigs(),
		                            tenantId, tenantDomain);

        dao.updateIdP(newIdentityProvider, currentIdentityProvider, tenantId, tenantDomain);
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

    /**
     * Get the authenticators registered in the system.
     * 
     * @return <code>FederatedAuthenticatorConfig</code> array.
     * @throws IdentityApplicationManagementException Error when getting authenticators registered
     *         in the system
     */
    public FederatedAuthenticatorConfig[] getAllFederatedAuthenticators()
            throws IdentityApplicationManagementException {
        List<FederatedAuthenticatorConfig> appConfig = ApplicationAuthenticatorService
                .getInstance().getFederatedAuthenticators();
        if (appConfig != null && appConfig.size() > 0) {
            return appConfig.toArray(new FederatedAuthenticatorConfig[appConfig.size()]);
        }
        return null;
    }
    
    /**
     * Get the Provisioning Connectors registered in the system.
     * 
     * @return <code>ProvisioningConnectorConfig</code> array.
     * @throws IdentityApplicationManagementException
     */
    public ProvisioningConnectorConfig[] getAllProvisioningConnectors()
            throws IdentityApplicationManagementException {
        List<ProvisioningConnectorConfig> connectorConfigs = ProvisioningConnectorService
                .getInstance().getProvisioningConnectorConfigs();
        if (connectorConfigs != null && connectorConfigs.size() > 0) {
            return connectorConfigs.toArray(new ProvisioningConnectorConfig[connectorConfigs.size()]);
        }
        return null;
    }
    
	private boolean validateIdPEntityId(FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs,
	                                                          int tenantId, String tenantDomain)throws IdentityApplicationManagementException {
		if (federatedAuthenticatorConfigs != null) {
			for (FederatedAuthenticatorConfig authConfig : federatedAuthenticatorConfigs) {
				if (IdentityApplicationConstants.Authenticator.SAML2SSO.FED_AUTH_NAME.equals(authConfig.getName()) ||
				    IdentityApplicationConstants.Authenticator.SAML2SSO.NAME.equals(authConfig.getName())) {
					Property[] properties = authConfig.getProperties();
					if (properties != null) {
						for (Property property : properties) {
							if (IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID.equals(property.getName())) {
								if (dao.isSimilarIdPEntityIdsAvailble(property.getValue(), tenantId)) {
									String msg = "An Identity Provider Entity Id has already been registered with the name '" +
						                     property.getValue() + "' for tenant '" + tenantDomain + "'";
									throw new IdentityApplicationManagementException(msg);
								}
								return true;
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean validateUpdateOfIdPEntityId(FederatedAuthenticatorConfig[] currentFederatedAuthConfigs,
	                                            FederatedAuthenticatorConfig[] newFederatedAuthConfigs,
	                                            int tenantId, String tenantDomain) throws IdentityApplicationManagementException {
		String currentIdentityProviderEntityId = null;
		if (currentFederatedAuthConfigs != null) {
			for (FederatedAuthenticatorConfig fedAuthnConfig : currentFederatedAuthConfigs) {
				if (IdentityApplicationConstants.Authenticator.SAML2SSO.FED_AUTH_NAME.equals(fedAuthnConfig.getName()) ||
				    IdentityApplicationConstants.Authenticator.SAML2SSO.NAME.equals(fedAuthnConfig.getName())) {
					Property[] properties = fedAuthnConfig.getProperties();
					if (properties != null) {
						for (Property property : properties) {
							if (IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID.equals(property.getName())) {
								currentIdentityProviderEntityId = property.getValue();
								break;
							}
						}
					}
					break;
				}
			}
		}
		
		if(newFederatedAuthConfigs != null){
			for (FederatedAuthenticatorConfig fedAuthnConfig : newFederatedAuthConfigs) {
				if (IdentityApplicationConstants.Authenticator.SAML2SSO.FED_AUTH_NAME.equals(fedAuthnConfig.getName()) ||
				    IdentityApplicationConstants.Authenticator.SAML2SSO.NAME.equals(fedAuthnConfig.getName())) {
					Property[] properties = fedAuthnConfig.getProperties();
					if (properties != null) {
						for (Property property : properties) {
							if (IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID.equals(property.getName())) {
								if(currentIdentityProviderEntityId != null && currentIdentityProviderEntityId.equals(property.getValue())){
									return true;
								} else {
									if (dao.isSimilarIdPEntityIdsAvailble(property.getValue(), tenantId)) {
										String msg = "An Identity Provider Entity Id has already been registered with the name '" +
							                     property.getValue() + "' for tenant '" + tenantDomain + "'";
										throw new IdentityApplicationManagementException(msg);
									}
									return true;
								}
							}
						}
					}
					break;
				}
			}
		}

		return true;
	}

}
