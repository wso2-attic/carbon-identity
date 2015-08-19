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
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class WorkflowAdminService {

    private static Log log = LogFactory.getLog(WorkflowAdminService.class);

    private WorkflowService osgiService = new WorkflowService();

    public WorkflowEventDTO[] listWorkflowEvents() {

        List<WorkflowEventDTO> events = osgiService.listWorkflowEvents();
        return events.toArray(new WorkflowEventDTO[events.size()]);
    }

    public TemplateBean[] listWorkflowTemplates() {

        List<TemplateBean> templates = osgiService.listWorkflowTemplates();
        return templates.toArray(new TemplateBean[templates.size()]);
    }

    public TemplateDTO getTemplateDTO(String templateName) {

        return osgiService.getTemplateDTO(templateName);
    }

    public TemplateImplDTO getTemplateImplDTO(String template, String implName) {

        return osgiService.getTemplateImplDTO(template, implName);
    }

    public void addBPSProfile(String profileName, String host, String user, String password, String callbackUser,
                              String callbackPassword) throws WorkflowException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            osgiService.addBPSProfile(profileName, host, user, password, callbackUser, callbackPassword, tenantId);
        } catch (WorkflowException e) {
            log.error("Server error when adding the profile " + profileName, e);
            throw new WorkflowException("Server error occurred when adding the BPS profile");
        }
    }

    public BPSProfileBean[] listBPSProfiles() throws WorkflowException {

        List<BPSProfileBean> bpsProfiles = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            bpsProfiles = osgiService.listBPSProfiles(tenantId);
        } catch (WorkflowException e) {
            log.error("Server error when listing BPS profiles", e);
            throw new WorkflowException("Server error occurred when listing BPS profiles");
        }
        if (CollectionUtils.isEmpty(bpsProfiles)) {
            return new BPSProfileBean[0];
        }
        return bpsProfiles.toArray(new BPSProfileBean[bpsProfiles.size()]);
    }

    public void removeBPSProfile(String profileName) throws WorkflowException {

        try {
            osgiService.removeBPSProfile(profileName);
        } catch (RuntimeWorkflowException e) {
            log.error("Error when removing workflow " + profileName, e);
            throw new WorkflowException(e.getMessage());
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing the profile " + profileName, e);
            throw new WorkflowException("Server error occurred when removing the BPS profile");
        }
    }

    public void addWorkflow(String name, String description, String templateId, String templateImpl,
                            Parameter[] templateParams, Parameter[] implParams) throws WorkflowException {

        String id = UUID.randomUUID().toString();
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            osgiService.addWorkflow(id, name, description, templateId, templateImpl, templateParams, implParams,
                    tenantId);
        } catch (RuntimeWorkflowException e) {
            log.error("Error when adding workflow " + name, e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding workflow " + name, e);
            throw new WorkflowException("Server error occurred when adding the workflow");
        }
    }

    public void addAssociation(String associationName, String workflowId, String eventId, String condition) throws
            WorkflowException {

        try {
            osgiService.addAssociation(associationName, workflowId, eventId, condition);
        } catch (RuntimeWorkflowException e) {
            log.error("Error when adding association " + associationName, e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding association of workflow " + workflowId + " with " + eventId, e);
            throw new WorkflowException("Server error occurred when associating the workflow with the event");
        }
    }

    public WorkflowBean[] listWorkflows() throws WorkflowException {

        List<WorkflowBean> workflows;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflows = osgiService.listWorkflows(tenantId);
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing workflows", e);
            throw new WorkflowException("Server error occurred when listing workflows");
        }
        if (CollectionUtils.isEmpty(workflows)) {
            return new WorkflowBean[0];
        }
        return workflows.toArray(new WorkflowBean[workflows.size()]);
    }

    public void removeWorkflow(String id) throws WorkflowException {

        try {
            osgiService.removeWorkflow(id);
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing workflow " + id, e);
            throw new WorkflowException("Server error occurred when removing workflow");
        }
    }

    public void removeAssociation(String associationId) throws WorkflowException {

        try {
            osgiService.removeAssociation(Integer.parseInt(associationId));
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing association " + associationId, e);
            throw new WorkflowException("Server error occurred when removing association");
        }
    }

    public AssociationDTO[] listAssociationsForWorkflow(String workflowId) throws WorkflowException {

        List<AssociationDTO> associations;
        try {
            associations = osgiService.getAssociationsForWorkflow(workflowId);
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
            associations = osgiService.listAllAssociations();
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

        return osgiService.getEvent(eventId);
    }

    /**
     * Returns array of requests initiated by a user.
     *
     * @param user
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsCreatedByUser(String user, String beginDate, String endDate, String
            dateCategory) throws WorkflowException {


        String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String fullyQualifiedUserName = UserCoreUtil.addTenantDomainToEntry(user, tenant);
        return osgiService.getRequestsFromFilter(fullyQualifiedUserName, beginDate, endDate, dateCategory);
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

        return osgiService.getRequestsFromFilter("", beginDate, endDate, dateCategory);
    }

    /**
     * Move Workflow request to DELETED state.
     *
     * @param requestId
     * @throws WorkflowException
     */
    public void deleteWorkflowRequest(String requestId) throws WorkflowException {

        osgiService.updateStatusOfRequest(requestId, WorkflowRequestStatus.DELETED.toString());
    }


}
