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

package org.wso2.carbon.identity.workflow.mgt;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.bean.AssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.ParameterDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.List;
import java.util.UUID;

public class WorkflowAdminService {

    private static Log log = LogFactory.getLog(WorkflowAdminService.class);

    private WorkflowService workflowService = new WorkflowService();

    public WorkflowEventDTO[] listWorkflowEvents() {

        List<WorkflowEventDTO> events = workflowService.listWorkflowEvents();
        return events.toArray(new WorkflowEventDTO[events.size()]);
    }

    public TemplateBean[] listWorkflowTemplates() {

        List<TemplateBean> templates = workflowService.listWorkflowTemplates();
        return templates.toArray(new TemplateBean[templates.size()]);
    }

    public TemplateDTO getTemplateDTO(String templateName) {

        return workflowService.getTemplateDTO(templateName);
    }

    public TemplateImplDTO getTemplateImplDTO(String template, String implName) {

        return workflowService.getTemplateImplDTO(template, implName);
    }

    public void addBPSProfile(BPSProfileDTO bpsProfileDTO) throws WorkflowException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            workflowService.addBPSProfile(bpsProfileDTO, tenantId);
        } catch (WorkflowException e) {
            log.error("Server error when adding the profile " + bpsProfileDTO.getProfileName(), e);
            throw new WorkflowException("Server error occurred when adding the BPS profile");
        }
    }

    public BPSProfileDTO[] listBPSProfiles() throws WorkflowException {

        List<BPSProfileDTO> bpsProfiles = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            bpsProfiles = workflowService.listBPSProfiles(tenantId);
        } catch (WorkflowException e) {
            log.error("Server error when listing BPS profiles", e);
            throw new WorkflowException("Server error occurred when listing BPS profiles");
        }
        if (CollectionUtils.isEmpty(bpsProfiles)) {
            return new BPSProfileDTO[0];
        }
        return bpsProfiles.toArray(new BPSProfileDTO[bpsProfiles.size()]);
    }

    /**
     * Reading BPS profile for given profile name and for current tenant
     *
     * @param bpsProfileName
     * @return
     * @throws WorkflowException
     */
    public BPSProfileDTO getBPSProfile(String bpsProfileName) throws WorkflowException {

        BPSProfileDTO bpsProfileDTO = null ;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            bpsProfileDTO = workflowService.getBPSProfile(bpsProfileName, tenantId);
        } catch (WorkflowException e) {
            log.error("Server error when reading a BPS profile", e);
            throw new WorkflowException("Server error occurred when reading a BPS profile");
        }
        return bpsProfileDTO;
    }

    /**
     * update BPS profile for given data
     *
     * @param bpsProfileDTO
     * @throws WorkflowException
     */
    public void updateBPSProfile(BPSProfileDTO bpsProfileDTO) throws WorkflowException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflowService.updateBPSProfile(bpsProfileDTO, tenantId);
        } catch (WorkflowException e) {
            log.error("Server error when updating the BPS profile", e);
            throw new WorkflowException("Server error occurred when updating the BPS profile");
        }
    }

    public void removeBPSProfile(String profileName) throws WorkflowException {

        try {
            workflowService.removeBPSProfile(profileName);
        } catch (RuntimeWorkflowException e) {
            log.error("Error when removing workflow " + profileName, e);
            throw new WorkflowException(e.getMessage());
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing the profile " + profileName, e);
            throw new WorkflowException("Server error occurred when removing the BPS profile");
        }
    }

    public void addWorkflow(WorkflowDTO workflowDTO, ParameterDTO[] templateParams, ParameterDTO[] implParams) throws WorkflowException {

        String id = UUID.randomUUID().toString();
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflowDTO.setWorkflowId(id);
            workflowService.addWorkflow(workflowDTO, templateParams, implParams, tenantId);

        } catch (RuntimeWorkflowException e) {
            log.error("Error when adding workflow " + workflowDTO.getWorkflowName(), e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding workflow " + workflowDTO.getWorkflowName(), e);
            throw new WorkflowException("Server error occurred when adding the workflow");
        }
    }

    public void addAssociation(String associationName, String workflowId, String eventId, String condition) throws
            WorkflowException {

        try {
            workflowService.addAssociation(associationName, workflowId, eventId, condition);
        } catch (RuntimeWorkflowException e) {
            log.error("Error when adding association " + associationName, e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding association of workflow " + workflowId + " with " + eventId, e);
            throw new WorkflowException("Server error occurred when associating the workflow with the event");
        }
    }


    public void changeAssociationState(String associationId, boolean isEnable)throws WorkflowException {
        try {
            workflowService.changeAssociationState(associationId, isEnable);
        } catch (RuntimeWorkflowException e) {
            log.error("Error when changing an association ", e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when changing state of association ", e);
            throw new WorkflowException("Server error occurred when changing the state of association");
        }

    }


    public WorkflowDTO[] listWorkflows() throws WorkflowException {

        List<WorkflowDTO> workflows;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflows = workflowService.listWorkflows(tenantId);
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing workflows", e);
            throw new WorkflowException("Server error occurred when listing workflows");
        }
        if (CollectionUtils.isEmpty(workflows)) {
            return new WorkflowDTO[0];
        }
        return workflows.toArray(new WorkflowDTO[workflows.size()]);
    }

    public void removeWorkflow(String id) throws WorkflowException {

        try {
            workflowService.removeWorkflow(id);
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing workflow " + id, e);
            throw new WorkflowException("Server error occurred when removing workflow");
        }
    }

    public void removeAssociation(String associationId) throws WorkflowException {

        try {
            workflowService.removeAssociation(Integer.parseInt(associationId));
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing association " + associationId, e);
            throw new WorkflowException("Server error occurred when removing association");
        }
    }

    public AssociationDTO[] listAssociationsForWorkflow(String workflowId) throws WorkflowException {

        List<AssociationDTO> associations;
        try {
            associations = workflowService.getAssociationsForWorkflow(workflowId);
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing associations for workflow id:" + workflowId, e);
            throw new WorkflowException("Server error when listing associations");
        }
        if (CollectionUtils.isEmpty(associations)) {
            return new AssociationDTO[0];
        }
        return associations.toArray(new AssociationDTO[associations.size()]);
    }

    public AssociationDTO[] listAllAssociations() throws WorkflowException {

        List<AssociationDTO> associations;
        try {
            associations = workflowService.listAllAssociations();
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing all associations", e);
            throw new WorkflowException("Server error when listing associations");
        }
        if (CollectionUtils.isEmpty(associations)) {
            return new AssociationDTO[0];
        }
        return associations.toArray(new AssociationDTO[associations.size()]);
    }

    public WorkflowEventDTO getEvent(String eventId) {

        return workflowService.getEvent(eventId);
    }

    /**
     * Returns array of requests initiated by a user.
     *
     * @param user
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsCreatedByUser(String user, String beginDate, String endDate, String
            dateCategory) throws WorkflowException {


        int tenant = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return workflowService.getRequestsFromFilter(user, beginDate, endDate, dateCategory, tenant);
    }

    /**
     * Return array of requests according to createdAt and updatedAt filter
     *
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsInFilter(String beginDate, String endDate, String
            dateCategory) throws WorkflowException {

        int tenant = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return workflowService.getRequestsFromFilter("", beginDate, endDate, dateCategory, tenant);
    }

    /**
     * Move Workflow request to DELETED state.
     *
     * @param requestId
     * @throws WorkflowException
     */
    public void deleteWorkflowRequest(String requestId) throws WorkflowException {

        workflowService.updateStatusOfRequest(requestId, WorkflowRequestStatus.DELETED.toString());
    }

    /**
     * Get workflows of a request.
     *
     * @param requestId
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestAssociationDTO[] getWorkflowsOfRequest(String requestId) throws WorkflowException {

        return workflowService.getWorkflowsOfRequest(requestId);
    }


}
