/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.common.model;

import org.apache.axiom.om.OMElement;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IdentityProvider implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 3348487050533568857L;

    private static final Log log = LogFactory.getLog(IdentityProvider.class);

    private String identityProviderName;
    private String identityProviderDescription;
    private String alias;
    private boolean primary;
    private boolean federationHub;
    private String homeRealmId;
    private String provisioningRole;
    private String displayName;
    private boolean enable;
    private FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = new FederatedAuthenticatorConfig[0];
    private FederatedAuthenticatorConfig defaultAuthenticatorConfig;
    private ProvisioningConnectorConfig[] provisioningConnectorConfigs = new ProvisioningConnectorConfig[0];
    private ProvisioningConnectorConfig defaultProvisioningConnectorConfig;
    private ClaimConfig claimConfig;
    private String certificate;
    private PermissionsAndRoleConfig permissionAndRoleConfig;
    private JustInTimeProvisioningConfig justInTimeProvisioningConfig;

    public static IdentityProvider build(OMElement identityProviderOM) {
        IdentityProvider identityProvider = new IdentityProvider();

        Iterator<?> iter = identityProviderOM.getChildElements();

        while (iter.hasNext()) {
            OMElement element = (OMElement) (iter.next());
            String elementName = element.getLocalName();

            if (elementName.equals("IdentityProviderName")) {
                if (element.getText() != null) {
                    identityProvider.setIdentityProviderName(element.getText());
                } else {
                    log.error("Identity provider not loaded from the file system. Identity provider name must be " +
                            "not null.");
                    return null;
                }
            } else if ("IdentityProviderDescription".equals(elementName)) {
                identityProvider.setIdentityProviderDescription(element.getText());
            } else if ("Alias".equals(elementName)) {
                identityProvider.setAlias(element.getText());
            } else if ("IsPrimary".equals(elementName)) {
                if (element.getText() != null && element.getText().trim().length() > 0) {
                    identityProvider.setPrimary(Boolean.parseBoolean(element.getText()));
                }
            } else if ("IsEnabled".equals(elementName)) {
                if (element.getText() != null && element.getText().trim().length() > 0) {
                    identityProvider.setEnable((Boolean.parseBoolean(element.getText())));
                }
            } else if ("IsFederationHub".equals(elementName)) {
                if (element.getText() != null && element.getText().trim().length() > 0) {
                    identityProvider.setFederationHub(Boolean.parseBoolean(element.getText()));
                }
            } else if ("HomeRealmId".equals(elementName)) {
                identityProvider.setHomeRealmId(element.getText());
            } else if ("ProvisioningRole".equals(elementName)) {
                identityProvider.setProvisioningRole(element.getText());
            } else if ("FederatedAuthenticatorConfigs".equals(elementName)) {

                Iterator<?> federatedAuthenticatorConfigsIter = element.getChildElements();

                if (federatedAuthenticatorConfigsIter == null) {
                    continue;
                }

                List<FederatedAuthenticatorConfig> federatedAuthenticatorConfigsArrList;
                federatedAuthenticatorConfigsArrList = new ArrayList<FederatedAuthenticatorConfig>();

                while (federatedAuthenticatorConfigsIter.hasNext()) {
                    OMElement federatedAuthenticatorConfigsElement = (OMElement) (federatedAuthenticatorConfigsIter
                            .next());
                    FederatedAuthenticatorConfig fedAuthConfig;
                    fedAuthConfig = FederatedAuthenticatorConfig
                            .build(federatedAuthenticatorConfigsElement);
                    if (fedAuthConfig != null) {
                        federatedAuthenticatorConfigsArrList.add(fedAuthConfig);
                    }
                }

                if (federatedAuthenticatorConfigsArrList.size() > 0) {
                    FederatedAuthenticatorConfig[] federatedAuthenticatorConfigsArr;
                    federatedAuthenticatorConfigsArr = federatedAuthenticatorConfigsArrList
                            .toArray(new FederatedAuthenticatorConfig[0]);
                    identityProvider
                            .setFederatedAuthenticatorConfigs(federatedAuthenticatorConfigsArr);
                }
            } else if ("DefaultAuthenticatorConfig".equals(elementName)) {
                identityProvider.setDefaultAuthenticatorConfig(FederatedAuthenticatorConfig
                        .build(element.getFirstElement()));
            } else if ("ProvisioningConnectorConfigs".equals(elementName)) {

                Iterator<?> provisioningConnectorConfigsIter = element.getChildElements();

                if (provisioningConnectorConfigsIter == null) {
                    continue;
                }

                List<ProvisioningConnectorConfig> provisioningConnectorConfigsArrList;
                provisioningConnectorConfigsArrList = new ArrayList<ProvisioningConnectorConfig>();

                while (provisioningConnectorConfigsIter.hasNext()) {
                    OMElement provisioningConnectorConfigsElement = (OMElement) (provisioningConnectorConfigsIter
                            .next());
                    ProvisioningConnectorConfig proConConfig = null;
                    try {
                        proConConfig = ProvisioningConnectorConfig
                                .build(provisioningConnectorConfigsElement);
                    } catch (IdentityApplicationManagementException e) {
                        log.error("Error while building provisioningConnectorConfig for IDP " + identityProvider
                                .getIdentityProviderName() + ". Cause : " + e.getMessage() + ". Building rest of the " +
                                "IDP configs");
                    }
                    if (proConConfig != null) {
                        provisioningConnectorConfigsArrList.add(proConConfig);
                    }
                }

                if (CollectionUtils.isNotEmpty(provisioningConnectorConfigsArrList)) {
                    ProvisioningConnectorConfig[] provisioningConnectorConfigsArr;
                    provisioningConnectorConfigsArr = provisioningConnectorConfigsArrList
                            .toArray(new ProvisioningConnectorConfig[0]);
                    identityProvider
                            .setProvisioningConnectorConfigs(provisioningConnectorConfigsArr);
                }
            } else if ("DefaultProvisioningConnectorConfig".equals(elementName)) {
                try {
                    identityProvider.setDefaultProvisioningConnectorConfig(ProvisioningConnectorConfig
                            .build(element));
                } catch (IdentityApplicationManagementException e) {
                    log.error("Error while building default provisioningConnectorConfig for IDP " + identityProvider
                            .getIdentityProviderName() + ". Cause : " + e.getMessage() + ". Building rest of the " +
                            "IDP configs");
                }
            } else if ("ClaimConfig".equals(elementName)) {
                identityProvider.setClaimConfig(ClaimConfig.build(element));
            } else if ("Certificate".equals(elementName)) {
                identityProvider.setCertificate(element.getText());
            } else if ("PermissionAndRoleConfig".equals(elementName)) {
                identityProvider
                        .setPermissionAndRoleConfig(PermissionsAndRoleConfig.build(element));
            } else if ("JustInTimeProvisioningConfig".equals(elementName)) {
                identityProvider.setJustInTimeProvisioningConfig(JustInTimeProvisioningConfig
                        .build(element));
            }

        }

        return identityProvider;
    }

    /**
     * @return
     */
    public FederatedAuthenticatorConfig[] getFederatedAuthenticatorConfigs() {
        return federatedAuthenticatorConfigs;
    }

    /**
     * @param federatedAuthenticatorConfigs
     */
    public void setFederatedAuthenticatorConfigs(
            FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs) {

        if (federatedAuthenticatorConfigs == null) {
            return;
        }
        Set<FederatedAuthenticatorConfig> propertySet =
                new HashSet<FederatedAuthenticatorConfig>(Arrays.asList(federatedAuthenticatorConfigs));
        this.federatedAuthenticatorConfigs = propertySet.toArray(new FederatedAuthenticatorConfig[propertySet.size()]);
    }

    /**
     * @return
     */
    public FederatedAuthenticatorConfig getDefaultAuthenticatorConfig() {
        return defaultAuthenticatorConfig;
    }

    /**
     * @param defaultAuthenticatorConfig
     */
    public void setDefaultAuthenticatorConfig(
            FederatedAuthenticatorConfig defaultAuthenticatorConfig) {
        this.defaultAuthenticatorConfig = defaultAuthenticatorConfig;
    }

    /**
     * @return
     */
    public String getIdentityProviderName() {
        return identityProviderName;
    }

    /**
     * @param identityProviderName
     */
    public void setIdentityProviderName(String identityProviderName) {
        this.identityProviderName = identityProviderName;
    }

    /**
     * @return
     */
    public String getIdentityProviderDescription() {
        return identityProviderDescription;
    }

    /**
     * @param identityProviderDescription
     */
    public void setIdentityProviderDescription(String identityProviderDescription) {
        this.identityProviderDescription = identityProviderDescription;
    }

    /**
     * @return
     */
    public ProvisioningConnectorConfig getDefaultProvisioningConnectorConfig() {
        return defaultProvisioningConnectorConfig;
    }

    /**
     * @param defaultProvisioningConnectorConfig
     */
    public void setDefaultProvisioningConnectorConfig(
            ProvisioningConnectorConfig defaultProvisioningConnectorConfig) {
        this.defaultProvisioningConnectorConfig = defaultProvisioningConnectorConfig;
    }

    /**
     * @return
     */
    public ProvisioningConnectorConfig[] getProvisioningConnectorConfigs() {
        return provisioningConnectorConfigs;
    }

    /**
     * @param provisioningConnectorConfigs
     */
    public void setProvisioningConnectorConfigs(
            ProvisioningConnectorConfig[] provisioningConnectorConfigs) {
        if (provisioningConnectorConfigs == null) {
            return;
        }
        Set<ProvisioningConnectorConfig> propertySet =
                new HashSet<ProvisioningConnectorConfig>(Arrays.asList(provisioningConnectorConfigs));
        this.provisioningConnectorConfigs = propertySet.toArray(new ProvisioningConnectorConfig[propertySet.size()]);
    }

    /**
     * @return
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * @param primary
     */
    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * @return
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * @param certificate
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * @return
     */
    public ClaimConfig getClaimConfig() {
        return claimConfig;
    }

    /**
     * @param claimConfig
     */
    public void setClaimConfig(ClaimConfig claimConfig) {
        this.claimConfig = claimConfig;
    }

    /**
     * @return
     */
    public PermissionsAndRoleConfig getPermissionAndRoleConfig() {
        return permissionAndRoleConfig;
    }

    /**
     * @param permissionAndRoleConfig
     */
    public void setPermissionAndRoleConfig(PermissionsAndRoleConfig permissionAndRoleConfig) {
        this.permissionAndRoleConfig = permissionAndRoleConfig;
    }

    /**
     * @return
     */
    public String getHomeRealmId() {
        return homeRealmId;
    }

    /**
     * @param homeRealmId
     */
    public void setHomeRealmId(String homeRealmId) {
        this.homeRealmId = homeRealmId;
    }

    /**
     * @return
     */
    public JustInTimeProvisioningConfig getJustInTimeProvisioningConfig() {
        return justInTimeProvisioningConfig;
    }

    /**
     * @param justTimeProvisioningConfiguration
     */
    public void setJustInTimeProvisioningConfig(
            JustInTimeProvisioningConfig justInTimeProvisioningConfig) {
        this.justInTimeProvisioningConfig = justInTimeProvisioningConfig;
    }

    /**
     * This represents a federation hub identity provider.
     *
     * @return
     */
    public boolean isFederationHub() {
        return federationHub;
    }

    /**
     * @param federationHub
     */
    public void setFederationHub(boolean federationHub) {
        this.federationHub = federationHub;
    }

    /**
     * This represents a provisioning role of identity provider.
     *
     * @return
     */
    public String getProvisioningRole() {
        return provisioningRole;
    }

    /**
     * @param provisioningRole
     */
    public void setProvisioningRole(String provisioningRole) {
        this.provisioningRole = provisioningRole;
    }

    /**
     * This represents whether the idp enable.
     *
     * @return
     */
    public boolean isEnable() {
        return enable;
    }

    /**
     * @param enable
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    /**
     * This represents a display name of identity provider.
     *
     * @return
     */
    public String getDisplayName() {
        return displayName;
    }

    /*
     * <IdentityProvider> <IdentityProviderName></IdentityProviderName>
     * <IdentityProviderDescription></IdentityProviderDescription> <Alias></Alias>
     * <IsPrimary></IsPrimary> <IsFederationHub></IsFederationHub><HomeRealmId></HomeRealmId>
     * <ProvisioningRole></ProvisioningRole>
     * <FederatedAuthenticatorConfigs></FederatedAuthenticatorConfigs>
     * <DefaultAuthenticatorConfig></DefaultAuthenticatorConfig>
     * <ProvisioningConnectorConfigs></ProvisioningConnectorConfigs>
     * <DefaultProvisioningConnectorConfig></DefaultProvisioningConnectorConfig>
     * <ClaimConfig></ClaimConfig> <Certificate></Certificate>
     * <PermissionAndRoleConfig></PermissionAndRoleConfig>
     * <JustInTimeProvisioningConfig></JustInTimeProvisioningConfig> </IdentityProvider>
     */

    /**
     * @param displayName
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityProvider)) return false;

        IdentityProvider that = (IdentityProvider) o;

        if (identityProviderName != null ? !identityProviderName.equals(that.identityProviderName) :
                that.identityProviderName != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identityProviderName != null ? identityProviderName.hashCode() : 0;
    }
}