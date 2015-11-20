/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth2.internal;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.dao.OAuthConsumerDAO;

public class OAuthApplicationMgtListener extends AbstractApplicationMgtListener {

    public static final String OAUTH2 = "oauth2";
    public static final String OAUTH2_CONSUMER_SECRET = "oauthConsumerSecret";

    @Override
    public int getDefaultOrderId() {
        return 11;
    }

    public boolean doPreCreateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {
        removeClientSecret(serviceProvider);
        return true;
    }

    public boolean doPostGetApplication(ServiceProvider serviceProvider, String serviceProviderName, String tenantDomain)
            throws IdentityApplicationManagementException {
        try {
            addClientSecret(serviceProvider, serviceProviderName, tenantDomain);
        } catch (IdentityOAuthAdminException e) {
            throw new IdentityApplicationManagementException("Injecting client secret failed.", e);
        }
        return true;
    }

    public boolean doPostGetApplicationByClientId(ServiceProvider serviceProvider, String clientId, String clientType,
                                                  String tenantDomain) throws IdentityApplicationManagementException {

        try {
            addClientSecret(serviceProvider, clientType, tenantDomain);
        } catch (IdentityOAuthAdminException e) {
            throw new IdentityApplicationManagementException("Injecting client secret failed.", e);
        }
        return true;
    }

    private void removeClientSecret(ServiceProvider serviceProvider) {
        InboundAuthenticationConfig inboundAuthenticationConfig = serviceProvider.getInboundAuthenticationConfig();
        if (inboundAuthenticationConfig != null) {
            InboundAuthenticationRequestConfig[] inboundRequestConfigs = inboundAuthenticationConfig.
                    getInboundAuthenticationRequestConfigs();
            if (inboundRequestConfigs != null) {
                for (InboundAuthenticationRequestConfig inboundRequestConfig : inboundRequestConfigs) {
                    if (inboundRequestConfig.getInboundAuthType().equals(OAUTH2)) {
                        Property[] props = inboundRequestConfig.getProperties();
                        for (Property prop : props) {
                            if (prop.getName().equalsIgnoreCase(OAUTH2_CONSUMER_SECRET)) {
                                prop.setValue("");
                                continue;   //we are interested only on this property
                            } else {
                                //ignore
                            }
                        }
                        continue;// we are interested only on oauth2 config. Only one will be present.
                    } else {
                        //ignore
                    }
                }
            } else {

            }
        } else {
            //nothing to do
        }
    }

    private ServiceProvider addClientSecret(ServiceProvider serviceProvider, String clientType,
                                            String tenantDomain) throws IdentityOAuthAdminException {

        InboundAuthenticationConfig inboundAuthenticationConfig = serviceProvider.getInboundAuthenticationConfig();
        if (inboundAuthenticationConfig != null) {
            InboundAuthenticationRequestConfig[] inboundRequestConfigs = inboundAuthenticationConfig.
                    getInboundAuthenticationRequestConfigs();
            if (inboundRequestConfigs != null) {
                for (InboundAuthenticationRequestConfig inboundRequestConfig : inboundRequestConfigs) {
                    if (inboundRequestConfig.getInboundAuthType().equals(OAUTH2)) {
                        Property[] props = inboundRequestConfig.getProperties();
                        for (Property prop : props) {
                            if (prop.getName().equalsIgnoreCase(OAUTH2_CONSUMER_SECRET)) {
                                prop.setValue(getClientSecret(inboundRequestConfig.getInboundAuthKey()));
                                continue;   //we are interested only on this property
                            } else {
                                //ignore
                            }
                        }
                        continue;// we are interested only on oauth2 config. Only one will be present.
                    } else {
                        //ignore
                    }
                }
            } else {

            }
        } else {
            //nothing to do
        }
        return serviceProvider;
    }

    private String getClientSecret(String inboundAuthKey) throws IdentityOAuthAdminException {
        OAuthConsumerDAO dao = new OAuthConsumerDAO();
        return dao.getOAuthConsumerSecret(inboundAuthKey);
    }
}