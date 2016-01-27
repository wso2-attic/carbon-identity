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
import org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.workflow.impl.ApprovalWorkflow;
import org.wso2.carbon.identity.workflow.impl.BPELDeployer;
import org.wso2.carbon.identity.workflow.impl.RequestExecutor;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplService;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplServiceImpl;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplAuditLogger;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplServiceListener;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplTenantMgtListener;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplValidationListener;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowListenerImpl;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.listener.WorkflowListener;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * @scr.component name="org.wso2.carbon.identity.workflow.impl" immediate="true"
 * @scr.reference name="org.wso2.carbon.user.core.service.realmservice" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="org.wso2.carbon.identity.workflow.mgt.workflowservice"
 * interface="org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService"
 * cardinality="1..1" policy="dynamic" bind="setWorkflowManagementService"
 * unbind="unsetWorkflowManagementService"
 * @scr.reference name="identityCoreInitializedEventService"
 * interface="org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent" cardinality="1..1"
 * policy="dynamic" bind="setIdentityCoreInitializedEventService" unbind="unsetIdentityCoreInitializedEventService"
 * @scr.reference name="org.wso2.carbon.utils.contextservice"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="org.wso2.carbon.identity.workflow.impl.listener.workflowimplservicelistner"
 * interface="org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplServiceListener"
 * cardinality="0..n" policy="dynamic"
 * bind="setWorkflowImplServiceListener"
 * unbind="unsetWorkflowImplServiceListener"
 */
public class WorkflowImplServiceComponent {

    private static Log log = LogFactory.getLog(WorkflowImplServiceComponent.class);

    protected void activate(ComponentContext context) {

        BundleContext bundleContext = context.getBundleContext();

        try {
            String metaDataXML =
                    readWorkflowImplParamMetaDataXML(WFImplConstant.WORKFLOW_IMPL_PARAMETER_METADATA_FILE_NAME);

            bundleContext.registerService(AbstractWorkflow.class, new ApprovalWorkflow(BPELDeployer.class,
                    RequestExecutor.class, metaDataXML), null);
            bundleContext.registerService(WorkflowListener.class, new WorkflowListenerImpl(), null);
            bundleContext.registerService(WorkflowImplServiceListener.class, new WorkflowImplAuditLogger(), null);
            bundleContext.registerService(WorkflowImplServiceListener.class, new WorkflowImplValidationListener(),
                    null);
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
            BPSProfile currentBpsProfile = workflowImplService.getBPSProfile(WFImplConstant.DEFAULT_BPS_PROFILE_NAME,
                    MultitenantConstants.SUPER_TENANT_ID);
            String url = IdentityUtil.getServerURL(WorkflowImplServiceDataHolder.getInstance()
                    .getConfigurationContextService().getServerConfigContext().getServicePath(), true, true);
            String userName = WorkflowImplServiceDataHolder.getInstance().getRealmService()
                    .getBootstrapRealmConfiguration().getAdminUserName();
            if (currentBpsProfile == null || !currentBpsProfile.getWorkerHostURL().equals(url) || !currentBpsProfile
                    .getUsername().equals(userName)) {
                BPSProfile bpsProfileDTO = new BPSProfile();
                bpsProfileDTO.setManagerHostURL(url);
                bpsProfileDTO.setWorkerHostURL(url);
                bpsProfileDTO.setUsername(userName);
                bpsProfileDTO.setPassword(new char[0]);
                bpsProfileDTO.setProfileName(WFImplConstant.DEFAULT_BPS_PROFILE_NAME);
                if (currentBpsProfile == null) {
                    workflowImplService.addBPSProfile(bpsProfileDTO, MultitenantConstants.SUPER_TENANT_ID);
                    log.info("Default BPS profile added to the DB.");
                } else {
                    workflowImplService.updateBPSProfile(bpsProfileDTO, MultitenantConstants.SUPER_TENANT_ID);
                    log.info("Default BPS profile updated.");
                }

            }
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

    protected void unsetIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    protected void setIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    protected void setWorkflowImplServiceListener(WorkflowImplServiceListener workflowListener) {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList().add(workflowListener);
    }

    protected void unsetWorkflowImplServiceListener(WorkflowImplServiceListener workflowListener) {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList().remove(workflowListener);
    }

}
