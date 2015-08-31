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
package org.wso2.carbon.identity.workflow.mgt.listener;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.workflow.mgt.WorkflowService;
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileDTO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.utils.NetworkUtils;

import java.net.SocketException;

public class WorkflowTenantMgtListener implements TenantMgtListener{

    private static Log log = LogFactory.getLog(WorkflowTenantMgtListener.class);

    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {
        WorkflowService workflowService = WorkflowServiceDataHolder.getInstance().getWorkflowService();

        BPSProfileDTO bpsProfileDTO = new BPSProfileDTO();
        String hostName = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.HOST_NAME);
        String offset = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.PORTS_OFFSET);
        String userName = WorkflowServiceDataHolder.getInstance().getRealmService().getBootstrapRealmConfiguration()
                .getAdminUserName();
        String password = WorkflowServiceDataHolder.getInstance().getRealmService().getBootstrapRealmConfiguration()
                .getAdminPassword();
        try {
            if (hostName == null) {
                hostName = NetworkUtils.getLocalHostname();
            }
            String url = "https://" + hostName + ":" + (9443 + Integer.parseInt(offset));

            bpsProfileDTO.setHost(url);
            bpsProfileDTO.setUsername(tenantInfoBean.getAdmin());
            bpsProfileDTO.setPassword(tenantInfoBean.getAdminPassword());
            bpsProfileDTO.setCallbackUser(tenantInfoBean.getAdmin());
            bpsProfileDTO.setCallbackPassword(tenantInfoBean.getAdminPassword());
            bpsProfileDTO.setProfileName(WorkFlowConstants.DEFAULT_BPS_PROFILE);

            workflowService.addBPSProfile(bpsProfileDTO,tenantInfoBean.getTenantId() );

        } catch (SocketException e) {
            //This is not thrown exception because this is not blocked to the other functionality. User can create default profile by manually.
            String errorMsg = "Error while trying to read hostname, " + e.getMessage() ;
            log.error(errorMsg);
        } catch (InternalWorkflowException e) {
            //This is not thrown exception because this is not blocked to the other functionality. User can create default profile by manually.
            String errorMsg = "Error occured while adding default bps profile, " + e.getMessage() ;
            log.error(errorMsg);
        } catch (WorkflowException e) {
            //This is not thrown exception because this is not blocked to the other functionality. User can create default profile by manually.
            String errorMsg = "Error occured while adding default bps profile, " + e.getMessage() ;
            log.error(errorMsg);
        }

    }

    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {

    }

    @Override
    public void onTenantDelete(int i) {

    }

    @Override
    public void onTenantRename(int i, String s, String s1) throws StratosException {

    }

    @Override
    public void onTenantInitialActivation(int i) throws StratosException {

    }

    @Override
    public void onTenantActivation(int i) throws StratosException {

    }

    @Override
    public void onTenantDeactivation(int i) throws StratosException {

    }

    @Override
    public void onSubscriptionPlanChange(int i, String s, String s1) throws StratosException {

    }

    @Override
    public int getListenerOrder() {
        return 0;
    }

    @Override
    public void onPreDelete(int i) throws StratosException {

    }
}
