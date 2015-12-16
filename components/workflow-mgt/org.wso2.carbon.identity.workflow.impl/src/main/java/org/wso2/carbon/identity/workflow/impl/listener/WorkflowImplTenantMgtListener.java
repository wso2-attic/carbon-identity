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
package org.wso2.carbon.identity.workflow.impl.listener;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplException;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.UserCoreConstants;

public class WorkflowImplTenantMgtListener implements TenantMgtListener {

    private static Log log = LogFactory.getLog(WorkflowImplTenantMgtListener.class);

    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {
        String fullName = tenantInfoBean.getAdmin() + UserCoreConstants.TENANT_DOMAIN_COMBINER + tenantInfoBean.getTenantDomain() ;
        BPSProfile bpsProfileDTO = new BPSProfile();
        String url = IdentityUtil.getServerURL(WorkflowImplServiceDataHolder.getInstance()
                .getConfigurationContextService().getServerConfigContext().getServicePath(), true, true);
        try {
            bpsProfileDTO.setManagerHostURL(url);
            bpsProfileDTO.setWorkerHostURL(url);
            bpsProfileDTO.setUsername(fullName);
            bpsProfileDTO.setPassword(new char[0]);
            bpsProfileDTO.setProfileName(WFImplConstant.DEFAULT_BPS_PROFILE_NAME);

            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService()
                    .addBPSProfile(bpsProfileDTO, tenantInfoBean
                            .getTenantId());

        }catch (WorkflowImplException e) {
            //This is not thrown exception because this is not blocked to the other functionality. User can create
            // default profile by manually.
            String errorMsg = "Error occured while adding default bps profile, " + e.getMessage();
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
