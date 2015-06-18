/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.common.internal;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent;
import org.wso2.carbon.identity.scim.common.config.SCIMProviderDAO;
import org.wso2.carbon.identity.scim.common.config.SCIMProviderDTO;
import org.wso2.carbon.identity.scim.common.config.SCIMProvisioningConfigManager;
import org.wso2.carbon.identity.scim.common.listener.SCIMUserOperationListener;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonUtils;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.charon.core.config.SCIMConfig;
import org.wso2.charon.core.config.SCIMConfigConstants;
import org.wso2.charon.core.config.SCIMConfigProcessor;
import org.wso2.charon.core.config.SCIMConsumer;
import org.wso2.charon.core.config.SCIMProvider;
import org.wso2.charon.core.config.SCIMUserSchemaExtensionBuilder;
import org.wso2.charon.core.exceptions.CharonException;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @scr.component name="identity.scim.common" immediate="true"
 * @scr.reference name="identityCoreInitializedEventService"
 * interface="org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent" cardinality="1..1"
 * policy="dynamic" bind="setIdentityCoreInitializedEventService" unbind="unsetIdentityCoreInitializedEventService"
 */
public class SCIMCommonComponent {
    private static Log logger = LogFactory.getLog(SCIMCommonComponent.class);

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    protected void activate(ComponentContext ctx) {
        try {
            String filePath = CarbonUtils.getCarbonConfigDirPath() + File.separator +
                              SCIMConfigConstants.PROVISIONING_CONFIG_NAME;

            SCIMConfigProcessor scimConfigProcessor = new SCIMConfigProcessor();
            SCIMConfig scimConfig = scimConfigProcessor.buildConfigFromFile(filePath);

            SCIMProvisioningConfigManager configManager = SCIMProvisioningConfigManager.getInstance();
            configManager.setSCIMConfig(scimConfig);
            //if it is the first startup
            SCIMProviderDAO providerDAO = new SCIMProviderDAO();
            if (providerDAO.isFirstStartup()) {

                SCIMConfigPersister persister = new SCIMConfigPersister(scimConfig);
                //persist scim config in DB by a separate thread
                executorService.submit(persister);
            }
            // reading user schema extension
            if (Boolean.parseBoolean(scimConfig.getAdditionalPropertyValue("user-schema-extension-enabled"))) {
                String schemaFilePath =
                        CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        SCIMConfigConstants.SCIM_SCHEMA_EXTENSION_CONFIG;
                SCIMUserSchemaExtensionBuilder.getInstance().buildUserSchemaExtension(schemaFilePath);
            }

            //register UserOperationEventListener implementation
            SCIMUserOperationListener scimUserOperationListener = new SCIMUserOperationListener();
            ctx.getBundleContext().registerService(UserOperationEventListener.class.getName(),
                                                   scimUserOperationListener, null);

            SCIMCommonUtils.init();

            if (logger.isDebugEnabled()) {
                logger.debug("SCIM Common component activated successfully.");
            }
        } catch (CharonException | IdentitySCIMException e) {
            logger.error("Error in reading information from identity tables at SCIMCommonComponentStartup.", e);
        }
    }

    protected void unsetIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    protected void setIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    protected class SCIMConfigPersister implements Runnable {
        private SCIMConfig scimConfig;

        public SCIMConfigPersister(SCIMConfig scimConfiguration) {
            scimConfig = scimConfiguration;
        }

        @Override
        public void run() {
            if (scimConfig != null) {
                //get scim consumer processed
                Map<String, SCIMConsumer> consumerMap = scimConfig.getConsumersMap();

                if (MapUtils.isNotEmpty(consumerMap)) {
                    for (String consumerId : consumerMap.keySet()) {
                        SCIMConsumer consumer = scimConfig.getConsumerProcessed(consumerId);
                        Map<String, SCIMProvider> providersMap = consumer.getScimProviders();
                        if (MapUtils.isNotEmpty(providersMap)) {
                            for (SCIMProvider scimProvider : providersMap.values()) {
                                SCIMProviderDTO scimDTO = new SCIMProviderDTO();
                                scimDTO.setProviderId(scimProvider.getId());
                                scimDTO.setUserName(scimProvider.getProperty(
                                        SCIMConfigConstants.ELEMENT_NAME_USERNAME));
                                scimDTO.setPassword(scimProvider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD));
                                scimDTO.setUserEPURL(scimProvider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT));
                                scimDTO.setGroupEPURL(scimProvider.getProperty(SCIMConfigConstants.ELEMENT_NAME_GROUP_ENDPOINT));

                                SCIMProviderDAO scimProviderDAO = new SCIMProviderDAO();
                                try {
                                    scimProviderDAO.addProvider(consumerId, scimDTO);
                                } catch (IdentitySCIMException e) {
                                    logger.error("Error in persisting scim provider: " +
                                                 scimProvider.getId() + " for scim consumer: " + consumerId, e);
                                }
                            }
                        }
                        //get provider
                        //create DTO and persist
                    }
                }

            }
        }
    }
}