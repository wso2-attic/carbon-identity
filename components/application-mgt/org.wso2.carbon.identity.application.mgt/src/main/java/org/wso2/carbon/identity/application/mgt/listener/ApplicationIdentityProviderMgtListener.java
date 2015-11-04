/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.mgt.listener;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtSystemConfig;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.listener.AbstractIdentityProviderMgtListener;

public class ApplicationIdentityProviderMgtListener extends AbstractIdentityProviderMgtListener {

    @Override
    public boolean doPreUpdateIdP(String oldIdPName, IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {

        try {
            ApplicationBasicInfo[] applicationBasicInfos = ApplicationMgtSystemConfig.getInstance()
                    .getApplicationDAO().getAllApplicationBasicInfo();

            for (ApplicationBasicInfo applicationBasicInfo : applicationBasicInfos) {
                ServiceProvider serviceProvider = ApplicationMgtSystemConfig.getInstance().getApplicationDAO()
                        .getApplication(applicationBasicInfo.getApplicationName(), tenantDomain);
                LocalAndOutboundAuthenticationConfig localAndOutboundAuthConfig = serviceProvider
                        .getLocalAndOutBoundAuthenticationConfig();
                AuthenticationStep[] authSteps = localAndOutboundAuthConfig.getAuthenticationSteps();

                // Check whether the identity provider is referred in a service provider
                if (!identityProvider.isEnable()) {
                    for (AuthenticationStep authenticationStep : localAndOutboundAuthConfig.getAuthenticationSteps()) {
                        for (IdentityProvider idpProvider : authenticationStep.getFederatedIdentityProviders()) {
                            if (identityProvider.getIdentityProviderName()
                                    .equals(idpProvider.getIdentityProviderName())) {
                                throw new IdentityProviderManagementException(
                                        "Error in disabling identity provider as it is referred by service providers.");
                            }
                        }
                    }
                }

                if (authSteps != null) {
                    if (ApplicationConstants.AUTH_TYPE_FEDERATED
                            .equalsIgnoreCase(localAndOutboundAuthConfig.getAuthenticationType())) {
                        // Update the service providers, with the default authenticator of the identity provider
                        IdentityProvider fedIdp = authSteps[0].getFederatedIdentityProviders()[0];
                        if (StringUtils.equals(fedIdp.getIdentityProviderName(), identityProvider
                                .getIdentityProviderName())) {

                            String defaultAuthName = fedIdp
                                    .getDefaultAuthenticatorConfig().getName();
                            String currentDefaultAuthName = identityProvider.getDefaultAuthenticatorConfig().getName();

                            if (!StringUtils.equals(currentDefaultAuthName, defaultAuthName)) {
                                FederatedAuthenticatorConfig currentDefaultAuthenticatorConfig = identityProvider
                                        .getDefaultAuthenticatorConfig();
                                fedIdp.setDefaultAuthenticatorConfig(currentDefaultAuthenticatorConfig);
                                fedIdp.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]
                                        {currentDefaultAuthenticatorConfig});
                                ApplicationMgtSystemConfig.getInstance().getApplicationDAO()
                                        .updateApplication(serviceProvider, tenantDomain);
                            }
                        }
                    } else if (authSteps.length >= 1) {
                        //Check whether the selected authenticator in multi step authentication, is enabled in the updated identity provider
                        FederatedAuthenticatorConfig[] idpFederatedConfig = identityProvider.getFederatedAuthenticatorConfigs();
                        for (AuthenticationStep authStep : authSteps) {

                            IdentityProvider[] federatedIdentityProviders = authStep.getFederatedIdentityProviders();

                            for (IdentityProvider federatedIdp : federatedIdentityProviders) {
                                FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = federatedIdp
                                        .getFederatedAuthenticatorConfigs();
                                String federatedConfigOption = federatedAuthenticatorConfigs[0].getName();

                                for (FederatedAuthenticatorConfig config : idpFederatedConfig) {
                                    if (StringUtils.equals(config.getName(), federatedConfigOption) && !config.isEnabled()) {
                                        throw new IdentityProviderManagementException(config.getName()
                                                + " is referred by service providers.");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityProviderManagementException("Error when updating default authenticator of service providers", e);
        }
        return true;
    }

    public int getDefaultOrderId() {
        return 10;
    }
}

