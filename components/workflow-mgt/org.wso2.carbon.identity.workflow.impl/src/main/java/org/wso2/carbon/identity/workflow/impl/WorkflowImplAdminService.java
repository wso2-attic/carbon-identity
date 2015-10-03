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

package org.wso2.carbon.identity.workflow.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;

import java.util.List;

public class WorkflowImplAdminService {

    private static Log log = LogFactory.getLog(WorkflowImplAdminService.class);

    public void addBPSProfile(BPSProfile bpsProfileDTO) throws WorkflowImplException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().addBPSProfile(bpsProfileDTO, tenantId);
        } catch (WorkflowImplException e) {
            log.error("Server error when adding the profile " + bpsProfileDTO.getProfileName(), e);
            throw new WorkflowImplException("Server error occurred when adding the BPS profile");
        }
    }

    public BPSProfile[] listBPSProfiles() throws WorkflowImplException {

        List<BPSProfile> bpsProfiles = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            bpsProfiles =
                    WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().listBPSProfiles(tenantId);
        } catch (WorkflowImplException e) {
            log.error("Server error when listing BPS profiles", e);
            throw new WorkflowImplException("Server error occurred when listing BPS profiles");
        }
        if (CollectionUtils.isEmpty(bpsProfiles)) {
            return new BPSProfile[0];
        }
        return bpsProfiles.toArray(new BPSProfile[bpsProfiles.size()]);
    }

    /**
     * Reading BPS profile for given profile name and for current tenant
     *
     * @param bpsProfileName
     * @return
     * @throws WorkflowImplException
     */
    public BPSProfile getBPSProfile(String bpsProfileName) throws WorkflowImplException {

        BPSProfile bpsProfileDTO = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            bpsProfileDTO = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService()
                    .getBPSProfile(bpsProfileName, tenantId);
        } catch (WorkflowImplException e) {
            log.error("Server error when reading a BPS profile", e);
            throw new WorkflowImplException("Server error occurred when reading a BPS profile");
        }
        return bpsProfileDTO;
    }

    /**
     * update BPS profile for given data
     *
     * @param bpsProfileDTO
     * @throws WorkflowImplException
     */
    public void updateBPSProfile(BPSProfile bpsProfileDTO) throws WorkflowImplException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService()
                    .updateBPSProfile(bpsProfileDTO, tenantId);
        } catch (WorkflowImplException e) {
            log.error("Server error when updating the BPS profile", e);
            throw new WorkflowImplException("Server error occurred when updating the BPS profile");
        }
    }

    public void removeBPSProfile(String profileName) throws WorkflowImplException {

        try {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().removeBPSProfile(profileName);
        } catch (WorkflowImplException e) {
            log.error("Error when removing workflow " + profileName, e);
            throw new WorkflowImplException(e.getMessage());
        }
    }

    /**
     * Removes BPS artifacts of a particular workflow.
     *
     * @param workflowRequest Workflow request to be deleted.
     * @throws WorkflowImplException
     */
    public void removeBPSPackage(Workflow workflowRequest) throws WorkflowImplException {

        try {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().removeBPSPackage(workflowRequest);
        } catch (WorkflowImplException e) {
            log.error("Error when removing BPS artifacts " + workflowRequest.getWorkflowName(), e);
            throw new WorkflowImplException(e.getMessage());
        }
    }

}
