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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociation;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.Template;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowEvent;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowWizard;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractTemplate;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class WorkflowManagementAdminService {

    private static Log log = LogFactory.getLog(WorkflowManagementAdminService.class);
    private static final Log AUDIT_LOG = CarbonConstants.AUDIT_LOG;
    private static final String AUDIT_MESSAGE = "Initiator : %s | Action : %s | Target : %s | Data : { %s } | Result : %s ";
    private static final String AUDIT_SUCCESS = "Success";
    private static final String AUDIT_FAILED = "Failed";


    private WorkflowWizard getWorkflow(org.wso2.carbon.identity.workflow.mgt.bean.Workflow workflowBean)
            throws WorkflowException {
        try {

            WorkflowWizard workflow = null;

            if (workflowBean != null) {

                workflow = new WorkflowWizard();

                workflow.setWorkflowId(workflowBean.getWorkflowId());
                workflow.setWorkflowName(workflowBean.getWorkflowName());
                workflow.setWorkflowDescription(workflowBean.getWorkflowDescription());

                AbstractTemplate abstractTemplate =
                        WorkflowServiceDataHolder.getInstance().getTemplates().get(workflowBean.getTemplateId());

                Template template = new Template();
                template.setTemplateId(abstractTemplate.getTemplateId());
                template.setName(abstractTemplate.getName());
                template.setDescription(abstractTemplate.getDescription());

                template.setParametersMetaData(abstractTemplate.getParametersMetaData());

                workflow.setTemplate(template);


                AbstractWorkflow abstractWorkflow =
                        WorkflowServiceDataHolder.getInstance().getWorkflowImpls()
                                .get(workflowBean.getTemplateId()).get(workflowBean.getWorkflowImplId());

                WorkflowImpl workflowimpl = new WorkflowImpl();
                workflowimpl.setWorkflowImplId(abstractWorkflow.getWorkflowImplId());
                workflowimpl.setWorkflowImplName(abstractWorkflow.getWorkflowImplName());
                workflowimpl.setTemplateId(abstractWorkflow.getTemplateId());
                workflowimpl.setParametersMetaData(abstractWorkflow.getParametersMetaData());

                workflow.setWorkflowImpl(workflowimpl);

                List<Parameter> workflowParams = WorkflowServiceDataHolder.getInstance().getWorkflowService()
                        .getWorkflowParameters(workflowBean.getWorkflowId());
                List<Parameter> templateParams = new ArrayList<>();
                List<Parameter> workflowImplParams = new ArrayList<>();
                for (Parameter parameter : workflowParams) {
                    if (parameter.getHolder().equals(WFConstant.ParameterHolder.TEMPLATE)) {
                        templateParams.add(parameter);
                    } else if (parameter.getHolder().equals(WFConstant.ParameterHolder.WORKFLOW_IMPL)) {
                        workflowImplParams.add(parameter);
                    }
                }
                workflow.setTemplateParameters(templateParams.toArray(new Parameter[templateParams.size()]));
                workflow.setWorkflowImplParameters(workflowImplParams
                                                           .toArray(new Parameter[workflowImplParams.size()]));

            }
            return workflow;
        } catch (InternalWorkflowException e) {
            String errorMsg =
                    "Error occurred while reading workflow object details for given workflow id, " + e.getMessage();
            log.error(errorMsg);
            throw new WorkflowException(errorMsg, e);
        }

    }

    public WorkflowWizard getWorkflow(String workflowId) throws WorkflowException {
        org.wso2.carbon.identity.workflow.mgt.bean.Workflow workflowBean =
                WorkflowServiceDataHolder.getInstance().getWorkflowService().getWorkflow(workflowId);
        return getWorkflow(workflowBean);
    }


    public WorkflowEvent[] listWorkflowEvents() {

        List<WorkflowEvent> events = WorkflowServiceDataHolder.getInstance().getWorkflowService().listWorkflowEvents();
        return events.toArray(new WorkflowEvent[events.size()]);
    }

    public Template[] listTemplates() throws WorkflowException {
        List<Template> templates = WorkflowServiceDataHolder.getInstance().getWorkflowService().listTemplates();
        return templates.toArray(new Template[templates.size()]);
    }

    public Template getTemplate(String templateId) throws WorkflowException {
        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getTemplate(templateId);
    }

    public WorkflowImpl getWorkflowImpl(String templateId, String implementationId) throws WorkflowException {
        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getWorkflowImpl(templateId,
                                                                                            implementationId);
    }

    public WorkflowImpl[] listWorkflowImpls(String templateId) throws WorkflowException {
        List<WorkflowImpl> workflowList =
                WorkflowServiceDataHolder.getInstance().getWorkflowService().listWorkflowImpls(templateId);
        return workflowList.toArray(new WorkflowImpl[workflowList.size()]);
    }


    public void addWorkflow(WorkflowWizard workflow) throws WorkflowException {

        String result = AUDIT_FAILED;
        String id = workflow.getWorkflowId();
        if (StringUtils.isBlank(id)) {
            id = UUID.randomUUID().toString();
        }
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            org.wso2.carbon.identity.workflow.mgt.bean.Workflow workflowBean = new org.wso2.carbon.identity.workflow
                    .mgt.bean.Workflow();
            workflowBean.setWorkflowId(id);
            workflowBean.setWorkflowName(workflow.getWorkflowName());
            workflowBean.setWorkflowDescription(workflow.getWorkflowDescription());
            String templateId = workflow.getTemplateId() == null ? workflow.getTemplate().getTemplateId() :
                                workflow.getTemplateId();
            if (templateId == null) {
                throw new WorkflowException("template id can't be empty");
            }
            workflowBean.setTemplateId(templateId);
            String workflowImplId =
                    workflow.getWorkflowImplId() == null ? workflow.getWorkflowImpl().getWorkflowImplId() :
                    workflow.getWorkflowImplId();
            if (workflowImplId == null) {
                throw new WorkflowException("workflowimpl id can't be empty");
            }
            workflowBean.setWorkflowImplId(workflowImplId);

            List<Parameter> parameterList = new ArrayList<>();
            if (workflow.getTemplateParameters() != null) {
                parameterList.addAll(Arrays.asList(workflow.getTemplateParameters()));
            }
            if (workflow.getWorkflowImplParameters() != null) {
                parameterList.addAll(Arrays.asList(workflow.getWorkflowImplParameters()));
            }

            WorkflowServiceDataHolder.getInstance().getWorkflowService()
                    .addWorkflow(workflowBean, parameterList, tenantId);
            result = AUDIT_SUCCESS;
        } catch (WorkflowRuntimeException e) {
            log.error("Error when adding workflow " + workflow.getWorkflowName(), e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding workflow " + workflow.getWorkflowName(), e);
            throw new WorkflowException("Server error occurred when adding the workflow");
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Workflow Name" + "\" : \"" + workflow.getWorkflowName()
                    + "\",\"" + "Workflow Impl ID" + "\" : \"" + workflow.getWorkflowImplId()
                    + "\",\"" + "Workerflow ID" + "\" : \"" + workflow.getWorkflowId()
                    + "\",\"" + "Workflow Description" + "\" : \"" + workflow.getWorkflowDescription()
                    + "\",\"" + "Template ID" + "\" : \"" + workflow.getTemplateId()
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Add Workflow",
                    "Workflow Management Admin Service", auditData, result));
        }
    }

    public void addAssociation(String associationName, String workflowId, String eventId, String condition) throws
                                                                                                            WorkflowException {

        String result = AUDIT_FAILED;
        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService()
                    .addAssociation(associationName, workflowId, eventId, condition);
            result = AUDIT_SUCCESS;
        } catch (WorkflowRuntimeException e) {
            log.error("Error when adding association " + associationName, e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when adding association of workflow " + workflowId + " with " + eventId, e);
            throw new WorkflowException("Server error occurred when associating the workflow with the event");
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Association Name" + "\" : \"" + associationName
                    + "\",\"" + "Workflow ID" + "\" : \"" + workflowId
                    + "\",\"" + "Event ID" + "\" : \"" + eventId
                    + "\",\"" + "Condition" + "\" : \"" + condition
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Add Association",
                    "Workflow Management Admin Service", auditData, result));
        }
    }


    public void changeAssociationState(String associationId, boolean isEnable) throws WorkflowException {

        String result = AUDIT_FAILED;
        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService()
                    .changeAssociationState(associationId, isEnable);
            result = AUDIT_SUCCESS;
        } catch (WorkflowRuntimeException e) {
            log.error("Error when changing an association ", e);
            throw new WorkflowException(e.getMessage());
        } catch (WorkflowException e) {
            log.error("Server error when changing state of association ", e);
            throw new WorkflowException("Server error occurred when changing the state of association");
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Association ID" + "\" : \"" + associationId
                    + "\",\"" + "Resulting State" + "\" : \"" + isEnable
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Change Association State",
                    "Workflow Management Admin Service", auditData, result));
        }

    }


    public WorkflowWizard[] listWorkflows() throws WorkflowException {

        List<WorkflowWizard> workflowWizards = new ArrayList<>();
        List<Workflow> workflowBeans = null;
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        try {
            workflowBeans = WorkflowServiceDataHolder.getInstance().getWorkflowService().listWorkflows(tenantId);
            for (Workflow workflow : workflowBeans) {
                WorkflowWizard workflowTmp = getWorkflow(workflow);
                workflowWizards.add(workflowTmp);
            }
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing workflows", e);
            throw new WorkflowException("Server error occurred when listing workflows");
        }
        return workflowWizards.toArray(new WorkflowWizard[workflowWizards.size()]);
    }

    public void removeWorkflow(String id) throws WorkflowException {

        String result = AUDIT_FAILED;
        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService().removeWorkflow(id);
            result = AUDIT_SUCCESS;
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing workflow " + id, e);
            throw new WorkflowException("Server error occurred when removing workflow");
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Workflow ID" + "\" : \"" + id
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Remove workflow",
                    "Workflow Management Admin Service", auditData, result));

        }
    }

    public void removeAssociation(String associationId) throws WorkflowException {

        String result = AUDIT_FAILED;
        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService()
                    .removeAssociation(Integer.parseInt(associationId));
            result = AUDIT_SUCCESS;
        } catch (InternalWorkflowException e) {
            log.error("Server error when removing association " + associationId, e);
            throw new WorkflowException("Server error occurred when removing association");
        } finally {

            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Association ID" + "\" : \"" + associationId
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Remove Association",
                    "Workflow Management Admin Service", auditData, result));
        }
    }

    public Association[] listAssociations(String workflowId) throws WorkflowException {

        List<Association> associations;
        try {
            associations =
                    WorkflowServiceDataHolder.getInstance().getWorkflowService().getAssociationsForWorkflow(workflowId);
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing associations for workflow id:" + workflowId, e);
            throw new WorkflowException("Server error when listing associations");
        }
        if (CollectionUtils.isEmpty(associations)) {
            return new Association[0];
        }
        return associations.toArray(new Association[associations.size()]);
    }

    public Association[] listAllAssociations() throws WorkflowException {

        List<Association> associations;
        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            associations = WorkflowServiceDataHolder.getInstance().getWorkflowService().listAllAssociations(tenantId);
        } catch (InternalWorkflowException e) {
            log.error("Server error when listing all associations", e);
            throw new WorkflowException("Server error when listing associations");
        }
        if (CollectionUtils.isEmpty(associations)) {
            return new Association[0];
        }
        return associations.toArray(new Association[associations.size()]);
    }


    //TODO:Below method should refactor


    public WorkflowEvent getEvent(String eventId) {

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
    public WorkflowRequest[] getRequestsCreatedByUser(String user, String beginDate, String endDate, String
            dateCategory, String status) throws WorkflowException {


        int tenant = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return WorkflowServiceDataHolder.getInstance().getWorkflowService()
                .getRequestsFromFilter(user, beginDate, endDate, dateCategory, tenant, status);
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
    public WorkflowRequest[] getRequestsInFilter(String beginDate, String endDate, String
            dateCategory, String status) throws WorkflowException {

        int tenant = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return WorkflowServiceDataHolder.getInstance().getWorkflowService()
                .getRequestsFromFilter("", beginDate, endDate, dateCategory, tenant, status);
    }

    /**
     * Move Workflow request to DELETED state.
     *
     * @param requestId
     * @throws WorkflowException
     */
    public void deleteWorkflowRequest(String requestId) throws WorkflowException {

        String result = AUDIT_FAILED;
        try {
            WorkflowServiceDataHolder.getInstance().getWorkflowService()
                    .deleteWorkflowRequest(requestId);
            result = AUDIT_SUCCESS;
        } finally {
            String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
            String auditData = "\"" + "Request ID" + "\" : \"" + requestId
                    + "\"";
            AUDIT_LOG.info(String.format( AUDIT_MESSAGE,loggedInUser, "Remove workflow request",
                    "Workflow Management Admin Service", auditData, result));
        }
    }

    /**
     * Get workflows of a request.
     *
     * @param requestId
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestAssociation[] getWorkflowsOfRequest(String requestId) throws WorkflowException {

        return WorkflowServiceDataHolder.getInstance().getWorkflowService().getWorkflowsOfRequest(requestId);
    }


}
