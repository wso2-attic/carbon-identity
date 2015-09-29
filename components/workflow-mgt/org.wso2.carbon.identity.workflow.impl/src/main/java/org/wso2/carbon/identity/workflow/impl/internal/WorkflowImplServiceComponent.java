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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.impl.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.workflow.impl.ApprovalWorkflow;
import org.wso2.carbon.identity.workflow.impl.BPELDeployer;
import org.wso2.carbon.identity.workflow.impl.RequestExecutor;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplService;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplServiceImpl;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplTenantMgtListener;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.identity.workflow.mgt.workflow.TemplateInitializer;
import org.wso2.carbon.identity.workflow.mgt.workflow.WorkFlowExecutor;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URISyntaxException;

/**
 * @scr.component name="identity.workflow.impl" immediate="true"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="workflow.service"
 * interface="org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService"
 * cardinality="0..n" policy="dynamic" bind="setWorkflowManagementService"
 * unbind="unsetWorkflowManagementService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class WorkflowImplServiceComponent {

    private static Log log = LogFactory.getLog(WorkflowImplServiceComponent.class);

    protected void activate(ComponentContext context) {

        BundleContext bundleContext = context.getBundleContext();

        try {
            String metaDataXML =
                    readWorkflowImplParamMetaDataXML(WFImplConstant.WORKFLOW_IMPL_PARAMETER_METADATA_FILE_NAME);

            TemplateInitializer templateInitializer = new BPELDeployer();
            WorkFlowExecutor workFlowExecutor = new RequestExecutor();
            bundleContext
                    .registerService(AbstractWorkflow.class, new ApprovalWorkflow(templateInitializer,
                                                                                  workFlowExecutor, metaDataXML), null);

            WorkflowImplServiceDataHolder.getInstance().setWorkflowImplService(new WorkflowImplServiceImpl());

            WorkflowImplTenantMgtListener workflowTenantMgtListener = new WorkflowImplTenantMgtListener();
            ServiceRegistration tenantMgtListenerSR = bundleContext.registerService(
                    TenantMgtListener.class.getName(), workflowTenantMgtListener, null);
            if (tenantMgtListenerSR != null) {
                log.debug("Workflow Management - WorkflowTenantMgtListener registered");
            } else {
                log.error("Workflow Management - WorkflowTenantMgtListener could not be registered");
            }

            this.addDefaultBPSProfile();
        } catch (Throwable e) {
            log.error("Error occurred while activating WorkflowImplServiceComponent bundle, " + e.getMessage());
        }

    }


    protected void setWorkflowManagementService(WorkflowManagementService workflowManagementService) {

        WorkflowImplServiceDataHolder.getInstance().setWorkflowManagementService(workflowManagementService);
    }

    protected void unsetWorkflowManagementService(WorkflowManagementService workflowManagementService) {

        WorkflowImplServiceDataHolder.getInstance().setWorkflowManagementService(null);
    }


    protected void setRealmService(RealmService realmService) {

        WorkflowImplServiceDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        WorkflowImplServiceDataHolder.getInstance().setRealmService(null);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {

        WorkflowImplServiceDataHolder.getInstance().setConfigurationContextService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {

        WorkflowImplServiceDataHolder.getInstance().setConfigurationContextService(contextService);
    }


    private void addDefaultBPSProfile() {

        try {
            WorkflowImplService workflowImplService =
                    WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();
            BPSProfile currentBpsProfile = workflowImplService.getBPSProfile(WFConstant.DEFAULT_BPS_PROFILE,
                                                                             MultitenantConstants.SUPER_TENANT_ID);

            if (currentBpsProfile == null) {
                BPSProfile bpsProfileDTO = new BPSProfile();
                String hostName = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.HOST_NAME);
                String offset = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.PORTS_OFFSET);


                String userName =
                        WorkflowImplServiceDataHolder.getInstance().getRealmService().getBootstrapRealmConfiguration()
                                .getAdminUserName();
                String password =
                        WorkflowImplServiceDataHolder.getInstance().getRealmService().getBootstrapRealmConfiguration()
                                .getAdminPassword();

                if (hostName == null) {
                    hostName = NetworkUtils.getLocalHostname();
                }
                String url = "https://" + hostName + ":" + (9443 + Integer.parseInt(offset));

                bpsProfileDTO.setManagerHostURL(url);
                bpsProfileDTO.setWorkerHostURL(url);
                bpsProfileDTO.setUsername(userName);
                bpsProfileDTO.setPassword(password);
                bpsProfileDTO.setCallbackUser(userName);
                bpsProfileDTO.setCallbackPassword(password);
                bpsProfileDTO.setProfileName(WFConstant.DEFAULT_BPS_PROFILE);

                workflowImplService.addBPSProfile(bpsProfileDTO, MultitenantConstants.SUPER_TENANT_ID);
                if (log.isDebugEnabled()) {
                    log.info("Default BPS profile added to the DB");
                }
            }
        } catch (SocketException e) {
            //This is not thrown exception because this is not blocked to the other functionality. User can create
            // default profile by manually.
            String errorMsg = "Error while trying to read hostname, " + e.getMessage();
            log.error(errorMsg);
        } catch (WorkflowException e) {
            //This is not thrown exception because this is not blocked to the other functionality. User can create
            // default profile by manually.
            String errorMsg = "Error occured while adding default bps profile, " + e.getMessage();
            log.error(errorMsg);
        }
    }

    private String readWorkflowImplParamMetaDataXML(String fileName) throws WorkflowRuntimeException {
        String content = null;
        try {
            InputStream resourceAsStream = this.getClass().getClassLoader()
                    .getResourceAsStream(fileName);
            content = WorkflowManagementUtil.readFileFromResource(resourceAsStream);

        } catch (IOException e) {
            String errorMsg = "Error occurred while reading file from class path, " + e.getMessage();
            log.error(errorMsg);
            throw new WorkflowRuntimeException(errorMsg, e);
        } catch (URISyntaxException e) {
            String errorMsg = "Error occurred while reading file from class path, " + e.getMessage();
            log.error(errorMsg);
            throw new WorkflowRuntimeException(errorMsg, e);
        }
        return content;
    }


}
