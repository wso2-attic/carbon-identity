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
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;

import java.util.List;

public class WorkflowImplAdminService {

    private static Log log = LogFactory.getLog(WorkflowImplAdminService.class);
    private static final Log AUDIT_LOG = CarbonConstants.AUDIT_LOG;
    private static final String AUDIT_MESSAGE = "Initiator : %s | Action : %s | Target : %s | Data : { %s } | Result " +
            ":  %s ";
    private static final String AUDIT_SUCCESS = "Success";
    private static final String AUDIT_FAILED = "Failed";

    public void addBPSProfile(BPSProfile bpsProfileDTO) throws WorkflowImplException {

        String result = AUDIT_FAILED;
        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().addBPSProfile(bpsProfileDTO, tenantId);
            result = AUDIT_SUCCESS;
        } catch (WorkflowImplException e) {
            log.error("Server error when adding the profile " + bpsProfileDTO.getProfileName(), e);
            throw new WorkflowImplException("Server error occurred when adding the BPS profile");
        } finally {

            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Profile Name" + "\" : \"" + bpsProfileDTO.getProfileName()
                    + "\",\"" + "Manager Host URL" + "\" : \"" + bpsProfileDTO.getManagerHostURL()
                    + "\",\"" + "Worker Host URL" + "\" : \"" + bpsProfileDTO.getWorkerHostURL()
                    + "\",\"" + "User" + "\" : \"" + bpsProfileDTO.getUsername()
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Add BPS Profile",
                    "Workflow Impl Admin Service", auditData, result));
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
        String result = AUDIT_FAILED;
        try {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService()
                    .updateBPSProfile(bpsProfileDTO, tenantId);
            result = AUDIT_SUCCESS;
        } catch (WorkflowImplException e) {
            log.error("Server error when updating the BPS profile", e);
            throw new WorkflowImplException("Server error occurred when updating the BPS profile");
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Profile Name" + "\" : \"" + bpsProfileDTO.getProfileName()
                    + "\",\"" + "Manager Host URL" + "\" : \"" + bpsProfileDTO.getManagerHostURL()
                    + "\",\"" + "Worker Host URL" + "\" : \"" + bpsProfileDTO.getWorkerHostURL()
                    + "\",\"" + "User" + "\" : \"" + bpsProfileDTO.getUsername()
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Update BPS Profile",
                    "Workflow Impl Admin Service", auditData, result));
        }
    }

    public void removeBPSProfile(String profileName) throws WorkflowImplException {

        try {
            WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().removeBPSProfile(profileName);

            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Profile Name" + "\" : \"" + profileName + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Delete BPS Profile",
                    "Workflow Impl Admin Service", auditData, AUDIT_SUCCESS));
        } catch (WorkflowImplException e) {
            log.error("Error when removing workflow " + profileName, e);
            throw new WorkflowImplException(e.getMessage());
        }
    }

    /**
     * Removes BPS artifacts of a particular workflow.
     *
     * @param workflow Workflow request to be deleted.
     * @throws WorkflowImplException
     */
    public void removeBPSPackage(Workflow workflow) throws WorkflowImplException {
        String result = AUDIT_FAILED;
        try {
            WorkflowImplService workflowImplService =
                    WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();

            if (workflowImplService == null) {
                log.error("Error while initialising WorkflowImplService");
                throw new WorkflowImplException("Error when removing BPS artifacts of: " + workflow.getWorkflowName());
            }

            workflowImplService.removeBPSPackage(workflow);
            result = AUDIT_SUCCESS;
        } catch (WorkflowImplException e) {
            log.error("Error when removing BPS artifacts of: " + workflow.getWorkflowName(), e);
            throw new WorkflowImplException(e.getMessage());
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Workflow Name" + "\" : \"" + workflow.getWorkflowName()
                    + "\",\"" + "Template ID" + "\" : \"" + workflow.getTemplateId()
                    + "\",\"" + "Workflow Description" + "\" : \"" + workflow.getWorkflowDescription()
                    + "\",\"" + "Workflow ID" + "\" : \"" + workflow.getWorkflowId()
                    + "\",\"" + "Workflow Impl ID" + "\" : \"" + workflow.getWorkflowImplId()
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Remove BPS Package",
                    "Workflow Impl Admin Service", auditData, result));
        }
    }

}
