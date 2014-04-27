/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.config.dto;

import java.io.Serializable;
import java.util.Map;

import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfiguration;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FacebookFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.JustInTimeProvisioningConfiguration;
import org.wso2.carbon.identity.application.common.model.OpenIDConnectFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.OpenIDFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.PassiveSTSFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfiguration;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.SAMLFederatedAuthenticator;

public class ExternalIdPConfig implements Serializable {

    private static final long serialVersionUID = -8973637869824303767L;

    private FederatedIdentityProvider identityProvider;
    private SAMLFederatedAuthenticator samlAuthenticator;
    private OpenIDConnectFederatedAuthenticator oidcAuthenticator;
    private OpenIDFederatedAuthenticator openidAuthenticator;
    private PassiveSTSFederatedAuthenticator passiveSTSAuthenticator;
    private ClaimConfiguration claimConfiguration;
    private FacebookFederatedAuthenticator facebookAuthenticator;
    private PermissionsAndRoleConfiguration roleConfiguration;
    private JustInTimeProvisioningConfiguration justInTimeProConfig;

    private Map<String, String> parameterMap;

    public ExternalIdPConfig() {
    }

    /**
     * 
     * @param identityProvider
     */
    public ExternalIdPConfig(FederatedIdentityProvider identityProvider) {
        this.identityProvider = identityProvider;

        FederatedAuthenticator[] federatedAuthenticators = identityProvider
                .getFederatedAuthenticators();

        if (federatedAuthenticators != null && federatedAuthenticators.length > 0) {
            for (FederatedAuthenticator federatedAuthenticator : federatedAuthenticators) {
                if (federatedAuthenticator instanceof OpenIDFederatedAuthenticator) {
                    openidAuthenticator = (OpenIDFederatedAuthenticator) federatedAuthenticator;
                } else if (federatedAuthenticator instanceof SAMLFederatedAuthenticator) {
                    samlAuthenticator = (SAMLFederatedAuthenticator) federatedAuthenticator;
                } else if (federatedAuthenticator instanceof PassiveSTSFederatedAuthenticator) {
                    passiveSTSAuthenticator = (PassiveSTSFederatedAuthenticator) federatedAuthenticator;
                } else if (federatedAuthenticator instanceof OpenIDConnectFederatedAuthenticator) {
                    oidcAuthenticator = (OpenIDConnectFederatedAuthenticator) federatedAuthenticator;
                } else if (federatedAuthenticator instanceof FacebookFederatedAuthenticator) {
                    facebookAuthenticator = (FacebookFederatedAuthenticator) federatedAuthenticator;
                }
            }
        }

        claimConfiguration = identityProvider.getClaimConfiguration();
        roleConfiguration = identityProvider.getPermissionAndRoleConfiguration();
        justInTimeProConfig = identityProvider.getJustInTimeProvisioningConfiguration();
    }

    /**
     * 
     * @return
     */
    public String getIdPName() {
        return identityProvider.getIdentityProviderName();
    }

    /**
     * 
     * @return
     */
    public String getPublicCert() {
        return identityProvider.getCertificate();
    }

    /**
     * 
     * @return
     */
    public boolean isPrimary() {
        return identityProvider.isPrimary();
    }

    /**
     * 
     * @return
     */
    public boolean isSAML2SSOEnabled() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.isEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public String getIssuer() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.getIdpEntityId();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getSSOUrl() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.getSaml2SSOUrl();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public boolean isAuthnRequestSigned() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.isAuthnRequestSigned();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isLogoutEnabled() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.isLogoutEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public String getLogoutRequestUrl() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.getLogoutRequestUrl();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public boolean isLogoutRequestSigned() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.isLogoutRequestSigned();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isAuthnResponseSigned() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.isLogoutRequestSigned();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isOIDCEnabled() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.isEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public String getClientId() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.getClientId();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getClientSecret() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.getClientSecret();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getAuthzEndpointUrl() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.getAuthzEndpointUrl();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getTokenEndpointUrl() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.getTokenEndpointUrl();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getName() {
        return identityProvider.getIdentityProviderName();
    }

    /**
     * 
     * @return
     */
    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

    /**
     * 
     * @param parameterMap
     */
    public void setParameterMap(Map<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    /**
     * 
     * @return
     */
    public String getDomain() {
        return identityProvider.getHomeRealmId();
    }

    /**
     * 
     * @return
     */
    public String getSpEntityId() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.getSpEntityId();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public ClaimMapping[] getClaimMappings() {
        if (claimConfiguration != null) {
            return claimConfiguration.getClaimMappings();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String[] getRoles() {
        if (roleConfiguration != null) {
            return roleConfiguration.getIdpRoles();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public RoleMapping[] getRoleMappings() {
        if (roleConfiguration != null) {
            return roleConfiguration.getRoleMappings();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public Claim[] getClaims() {
        if (claimConfiguration != null) {
            return claimConfiguration.getIdpClaims();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getSsoUrl() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.getSaml2SSOUrl();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getPassiveSTSRealm() {
        if (passiveSTSAuthenticator != null) {
            return passiveSTSAuthenticator.getPassiveSTSRealm();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getPassiveSTSUrl() {
        if (passiveSTSAuthenticator != null) {
            return passiveSTSAuthenticator.getPassiveSTSUrl();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getProvisioningUserStoreId() {
        if (justInTimeProConfig != null) {
            return justInTimeProConfig.getProvisioningUserStore();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getProvisioningUserStoreClaimURI() {
        if (justInTimeProConfig != null) {
            return justInTimeProConfig.getUserStoreClaimUri();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getTokenEndpointAlias() {
        return identityProvider.getAlias();
    }

    /**
     * 
     * @return
     */
    public boolean isOIDCDefault() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.isEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isOIDCUserIdInClaims() {
        if (oidcAuthenticator != null) {
            return oidcAuthenticator.isUsetIdInClaim();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isOpenIDDefault() {

        if (identityProvider.getDefaultAuthenticator() != null && openidAuthenticator != null) {
            return identityProvider.getDefaultAuthenticator().getName() == openidAuthenticator
                    .getName();
        }

        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isOpenIDEnabled() {
        if (openidAuthenticator != null) {
            return openidAuthenticator.isEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isOpenIDUserIdInClaims() {
        if (openidAuthenticator != null) {
            return openidAuthenticator.isUsetIdInClaim();
        }
        return false;
    }
    
    /**
     * @return
     */
    public String getOpenIDURL() {
    	if (openidAuthenticator != null) {
            return openidAuthenticator.getOpenIDServerUrl();
        }
    	
    	return null;
    }

    /**
     * 
     * @return
     */
    public boolean isPassiveSTSDefault() {
        if (identityProvider.getDefaultAuthenticator() != null && passiveSTSAuthenticator != null) {
            return identityProvider.getDefaultAuthenticator().getName() == passiveSTSAuthenticator
                    .getName();
        }

        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isPassiveSTSEnabled() {
        if (passiveSTSAuthenticator != null) {
            return passiveSTSAuthenticator.isEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isPassiveSTSUserIdInClaims() {
        if (passiveSTSAuthenticator != null) {
            return passiveSTSAuthenticator.isUsetIdInClaim();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isProvisioningEnabled() {
        if (justInTimeProConfig != null) {
            return justInTimeProConfig.isProvisioningEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isSAML2SSODefault() {
        if (identityProvider.getDefaultAuthenticator() != null && samlAuthenticator != null) {
            return identityProvider.getDefaultAuthenticator().getName() == samlAuthenticator
                    .getName();
        }

        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isSAML2SSOUserIdInClaims() {
        if (samlAuthenticator != null) {
            return samlAuthenticator.isUsetIdInClaim();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isFbauthEnabled() {
        if (facebookAuthenticator != null) {
            return facebookAuthenticator.isEnabled();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public boolean isFbauthDefault() {
        if (identityProvider.getDefaultAuthenticator() != null && facebookAuthenticator != null) {
            return identityProvider.getDefaultAuthenticator().getName() == facebookAuthenticator
                    .getName();
        }

        return false;
    }

    /**
     * 
     * @return
     */
    public String getFbauthClientId() {
        if (facebookAuthenticator != null) {
            return facebookAuthenticator.getClientId();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public String getFbauthClientSecret() {
        if (facebookAuthenticator != null) {
            return facebookAuthenticator.getClientSecret();
        }
        return null;
    }

    /**
     * 
     * @return
     */
    public boolean isFbauthUserIdInClaims() {
        if (facebookAuthenticator != null) {
            return facebookAuthenticator.isUsetIdInClaim();
        }
        return false;
    }

    /**
     * 
     * @return
     */
    public String getRoleClaimUri() {
        if (identityProvider.getClaimConfiguration() != null) {
            return identityProvider.getClaimConfiguration().getRoleClaimURI();
        }
        return null;
    }

}
