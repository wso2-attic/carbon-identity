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
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.Template;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.dto.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkflowManagementAdminService {

    private static Log log = LogFactory.getLog(WorkflowManagementAdminService.class);

    public WorkflowEventDTO[] listWorkflowEvents() {

        List<WorkflowEventDTO> events = WorkflowServiceDataHolder.getInstance().getWorkflowService().listWorkflowEvents();
        return events.toArray(new WorkflowEventDTO[events.size()]);
    }

    public Template[] listWorkflowTemplates() {
        List<Template> templates = WorkflowServiceDataHolder.getInstance().getWorkflowService().listTemplates();
        return templates.toArray(new Template[templates.size()]);
    }

    public Template getTemplateDTO(String templateName) {

        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getTemplate(templateName);
    }

    public WorkflowImpl getTemplateImplDTO(String templateId, String implementationId) {

        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getWorkflowImpl(implementationId);
    }



    public void addWorkflow(Workflow workflowDTO, Parameter[] parameters) throws WorkflowException {

        String id = UUID.randomUUID().toString();
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflowDTO.setWorkflowId(id);
            WorkflowServiceDataHolder.getInstance().getWorkflowService().addWorkflow(workflowDTO, Arrays.asList(parameters) , tenantId);

        } catch (WorkflowRuntimeException e) {
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
            WorkflowServiceDataHolder.getInstance().getWorkflowService().addAssociation(associationName, workflowId, eventId, condition);
        } catch (WorkflowRuntimeException e) {
            log.error("Error when adding association " + associationName, e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding association of workflow " + workflowId + " with " + eventId, e);
            throw new WorkflowException("Server error occurred when associating the workflow with the event");
        }
    }


    public void changeAssociationState(String associationId, boolean isEnable)throws WorkflowException {
        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService().changeAssociationState(associationId, isEnable);
        } catch (WorkflowRuntimeException e) {
            log.error("Error when changing an association ", e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when changing state of association ", e);
            throw new WorkflowException("Server error occurred when changing the state of association");
        }

    }


    public Workflow[] listWorkflows() throws WorkflowException {

        List<Workflow> workflows;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflows = WorkflowServiceDataHolder.getInstance().getWorkflowService().listWorkflows(tenantId);
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing workflows", e);
            throw new WorkflowException("Server error occurred when listing workflows");
        }
        if (CollectionUtils.isEmpty(workflows)) {
            return new Workflow[0];
        }
        return workflows.toArray(new Workflow[workflows.size()]);
    }

    public void removeWorkflow(String id) throws WorkflowException {

        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService().removeWorkflow(id);
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing workflow " + id, e);
            throw new WorkflowException("Server error occurred when removing workflow");
        }
    }

    public void removeAssociation(String associationId) throws WorkflowException {

        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService().removeAssociation(Integer.parseInt(associationId));
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing association " + associationId, e);
            throw new WorkflowException("Server error occurred when removing association");
        }
    }

    public AssociationDTO[] listAssociationsForWorkflow(String workflowId) throws WorkflowException {

        List<AssociationDTO> associations;
        try {
            associations = WorkflowServiceDataHolder.getInstance().getWorkflowService().getAssociationsForWorkflow(workflowId);
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
            associations = WorkflowServiceDataHolder.getInstance().getWorkflowService().listAllAssociations();
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

        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getEvent(eventId);
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
        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getRequestsFromFilter(user, beginDate, endDate, dateCategory, tenant);
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
        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getRequestsFromFilter("", beginDate, endDate, dateCategory, tenant);
    }

    /**
     * Move Workflow request to DELETED state.
     *
     * @param requestId
     * @throws WorkflowException
     */
    public void deleteWorkflowRequest(String requestId) throws WorkflowException {

        WorkflowServiceDataHolder.getInstance().getWorkflowService().updateStatusOfRequest(requestId, WorkflowRequestStatus.DELETED.toString());
    }

    /**
     * Get workflows of a request.
     *
     * @param requestId
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestAssociationDTO[] getWorkflowsOfRequest(String requestId) throws WorkflowException {

        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getWorkflowsOfRequest(requestId);
    }


}
