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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtSystemConfig;
import org.wso2.carbon.identity.application.mgt.dao.IdentityProviderDAO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.listener.AbstractIdentityProviderMgtListener;

public class IdentityProviderMgtApplicationListener extends AbstractIdentityProviderMgtListener {

    private static final Log log = LogFactory.getLog(IdentityProviderMgtApplicationListener.class);

    @Override
    public boolean doPreUpdateIdP(String oldIdPName, IdentityProvider identityProvider) throws IdentityProviderManagementException {

        try {
            ApplicationBasicInfo[] applicationBasicInfos = ApplicationMgtSystemConfig.getInstance()
                    .getApplicationDAO().getAllApplicationBasicInfo();
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

            for (ApplicationBasicInfo applicationBasicInfo : applicationBasicInfos) {
                ServiceProvider serviceProvider = ApplicationMgtSystemConfig.getInstance().getApplicationDAO()
                        .getApplication(applicationBasicInfo.getApplicationName(), tenantDomain);
                LocalAndOutboundAuthenticationConfig localAndOutboundAuthConfig = serviceProvider
                        .getLocalAndOutBoundAuthenticationConfig();
                AuthenticationStep[] authSteps = localAndOutboundAuthConfig.getAuthenticationSteps();

                if (!identityProvider.isEnable()) {
                    for (AuthenticationStep authenticationStep : localAndOutboundAuthConfig.getAuthenticationSteps()) {
                        for (IdentityProvider idpProvider : authenticationStep.getFederatedIdentityProviders()) {
                            if (identityProvider.getIdentityProviderName()
                                    .equals(idpProvider.getIdentityProviderName())) {
                                throw new IdentityProviderManagementException(
                                        "Cannot disable identity provider, it is already being used.");
                            }
                        }
                    }
                }

                if (ApplicationConstants.AUTH_TYPE_FEDERATED
                        .equalsIgnoreCase(localAndOutboundAuthConfig.getAuthenticationType())) {

                    IdentityProvider fedIdp = authSteps[0].getFederatedIdentityProviders()[0];
                    if (StringUtils.equals(fedIdp.getIdentityProviderName(), identityProvider
                            .getIdentityProviderName())) {

                        String defualtAuthName = fedIdp
                                .getDefaultAuthenticatorConfig().getName();

                        String currentDefaultAuthName = identityProvider.getDefaultAuthenticatorConfig().getName();

                        if (!StringUtils.equals(currentDefaultAuthName, defualtAuthName)) {
                            FederatedAuthenticatorConfig currentDefaultAuthenticatorConfig = identityProvider
                                    .getDefaultAuthenticatorConfig();
                            fedIdp.setDefaultAuthenticatorConfig(currentDefaultAuthenticatorConfig);
                            ApplicationMgtSystemConfig.getInstance().getApplicationDAO()
                                    .updateApplication(serviceProvider);
                        }
                    }
                }
            }
        } catch (IdentityApplicationManagementException | IdentityException e) {
            throw new IdentityProviderManagementException("Error when updating default authenticator of service providers", e);
        }
        return true;
    }

    public int getDefaultOrderId(){
        return 10;
    }
}

