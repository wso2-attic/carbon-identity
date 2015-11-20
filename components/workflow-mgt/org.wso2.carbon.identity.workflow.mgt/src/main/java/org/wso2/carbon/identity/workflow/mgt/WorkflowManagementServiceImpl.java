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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.bean.Entity;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociation;
import org.wso2.carbon.identity.workflow.mgt.dao.AssociationDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.RequestEntityRelationshipDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestAssociationDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.Template;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowEvent;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.listener.WorkflowListener;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractTemplate;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * WorkflowService class provides all the common functionality for the basic workflows.
 */
public class WorkflowManagementServiceImpl implements WorkflowManagementService {
    
    public static final String DATE_FORMAT_FOR_FILTERING = "MM/dd/yyyy";
    public static final String HT_SERVICES_URL = "services/HumanTaskClientAPIAdmin";
    public static final String HT_PARAMETER_LIST_ELEMENT = "parametersList";
    public static final String HT_ITEM_NAME_ATTRIBUTE = "itemName";
    public static final String HT_REQUEST_ID_ATTRIBUTE_VALUE = "REQUEST ID";
    private static Log log = LogFactory.getLog(WorkflowManagementServiceImpl.class);

    WorkflowDAO workflowDAO = new WorkflowDAO();
    AssociationDAO associationDAO = new AssociationDAO();
    private RequestEntityRelationshipDAO requestEntityRelationshipDAO = new RequestEntityRelationshipDAO();
    private WorkflowRequestDAO workflowRequestDAO = new WorkflowRequestDAO();
    private WorkflowRequestAssociationDAO workflowRequestAssociationDAO = new WorkflowRequestAssociationDAO();


    @Override
    public Workflow getWorkflow(String workflowId) throws WorkflowException {
        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetWorkflow(workflowId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetWorkflow in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        Workflow workflowBean = workflowDAO.getWorkflow(workflowId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetWorkflow(workflowId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetWorkflow in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        return workflowBean;
    }

    @Override
    public List<Parameter> getWorkflowParameters(String workflowId) throws WorkflowException {
        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetWorkflowParameters(workflowId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetWorkflowParameters in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        List<Parameter> workflowParams = workflowDAO.getWorkflowParams(workflowId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetWorkflowParameters(workflowId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetWorkflowParameters in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return workflowParams;
    }




    @Override
    public List<WorkflowEvent> listWorkflowEvents() {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
                workflowListener.doPreListWorkflowEvents();
        }
        List<WorkflowRequestHandler> workflowRequestHandlers =
                WorkflowServiceDataHolder.getInstance().listRequestHandlers();
        List<WorkflowEvent> eventList = new ArrayList<>();
        if (workflowRequestHandlers != null) {
            for (WorkflowRequestHandler requestHandler : workflowRequestHandlers) {
                WorkflowEvent event = new WorkflowEvent();
                event.setEventId(requestHandler.getEventId());
                event.setEventFriendlyName(requestHandler.getFriendlyName());
                event.setEventDescription(requestHandler.getDescription());
                event.setEventCategory(requestHandler.getCategory());
                //note: parameters are not set at here in list operation. It's set only at get operation
                if (requestHandler.getParamDefinitions() != null) {
                    Parameter[] parameterDTOs = new Parameter[requestHandler.getParamDefinitions().size()];
                    int i = 0;
                    for (Map.Entry<String, String> paramEntry : requestHandler.getParamDefinitions().entrySet()) {
                        Parameter parameterDTO = new Parameter();
                        parameterDTO.setParamName(paramEntry.getKey());
                        parameterDTO.setParamValue(paramEntry.getValue());
                        parameterDTOs[i] = parameterDTO;
                        i++;
                    }
                    event.setParameters(parameterDTOs);
                }
                eventList.add(event);
            }
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            workflowListener.doPostListWorkflowEvents();
        }


        return eventList;
    }

    @Override
    public WorkflowEvent getEvent(String id) {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            workflowListener.doPreGetEvent(id);
        }
        WorkflowRequestHandler requestHandler = WorkflowServiceDataHolder.getInstance().getRequestHandler(id);
        if (requestHandler != null) {
            WorkflowEvent event = new WorkflowEvent();
            event.setEventId(requestHandler.getEventId());
            event.setEventFriendlyName(requestHandler.getFriendlyName());
            event.setEventDescription(requestHandler.getDescription());
            event.setEventCategory(requestHandler.getCategory());
            if (requestHandler.getParamDefinitions() != null) {
                Parameter[] parameters = new Parameter[requestHandler.getParamDefinitions().size()];
                int i = 0;
                for (Map.Entry<String, String> paramEntry : requestHandler.getParamDefinitions().entrySet()) {
                    Parameter parameter = new Parameter();
                    parameter.setParamName(paramEntry.getKey());
                    parameter.setParamValue(paramEntry.getValue());
                    parameters[i] = parameter;
                    i++;
                }
                event.setParameters(parameters);
            }

            for (WorkflowListener workflowListener : workflowListenerList) {
                workflowListener.doPostGetEvent(id);
            }
            return event;
        }
        return null;
    }

    @Override
    public List<Template> listTemplates() throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreListTemplates();
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreListTemplates in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        Map<String, AbstractTemplate> templateMap = WorkflowServiceDataHolder.getInstance().getTemplates();
        List<AbstractTemplate> templateList = new ArrayList<>(templateMap.values());
        List<Template> templates = new ArrayList<Template>();
        if (templateList != null) {
            for (AbstractTemplate abstractTemplate : templateList) {
                Template template = new Template();
                template.setTemplateId(abstractTemplate.getTemplateId());
                template.setName(abstractTemplate.getName());
                template.setDescription(abstractTemplate.getDescription());
                template.setParametersMetaData(abstractTemplate.getParametersMetaData());
                templates.add(template);
            }
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostListTemplates();
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostListTemplates in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }


        return templates;
    }

    @Override
    public List<WorkflowImpl> listWorkflowImpls(String templateId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreListWorkflowImpls(templateId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreListWorkflowImpls in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        Map<String, AbstractWorkflow> abstractWorkflowMap =
                WorkflowServiceDataHolder.getInstance().getWorkflowImpls().get(templateId);
        List<WorkflowImpl> workflowList = new ArrayList<>();
        if (abstractWorkflowMap != null) {
            List<AbstractWorkflow> abstractWorkflowList = new ArrayList<>(abstractWorkflowMap.values());
            for (AbstractWorkflow abstractWorkflow : abstractWorkflowList) {
                WorkflowImpl workflow = new WorkflowImpl();
                workflow.setWorkflowImplId(abstractWorkflow.getWorkflowImplId());
                workflow.setWorkflowImplName(abstractWorkflow.getWorkflowImplName());
                workflow.setParametersMetaData(abstractWorkflow.getParametersMetaData());
                workflow.setTemplateId(abstractWorkflow.getTemplateId());
                workflowList.add(workflow);
            }
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostListWorkflowImpls(templateId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostListWorkflowImpls in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        return workflowList;
    }

    @Override
    public Template getTemplate(String templateId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetTemplate(templateId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetTemplate in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        AbstractTemplate abstractTemplate = WorkflowServiceDataHolder.getInstance().getTemplates().get(templateId);
        Template template = null;
        if (abstractTemplate != null) {
            template = new Template();
            template.setTemplateId(abstractTemplate.getTemplateId());
            template.setName(abstractTemplate.getName());
            template.setDescription(abstractTemplate.getDescription());
            template.setParametersMetaData(abstractTemplate.getParametersMetaData());
        }

        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetTemplate(templateId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetTemplate in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return template;
    }


    @Override
    public WorkflowImpl getWorkflowImpl(String templateId, String workflowImplId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetWorkflowImpl(templateId, workflowImplId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetWorkflowImpl in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        WorkflowImpl workflowImpl = null;
        Map<String, AbstractWorkflow> abstractWorkflowMap =
                WorkflowServiceDataHolder.getInstance().getWorkflowImpls().get(templateId);
        if (abstractWorkflowMap != null) {
            AbstractWorkflow tmp = abstractWorkflowMap.get(workflowImplId);
            if (tmp != null) {
                workflowImpl = new WorkflowImpl();
                workflowImpl.setWorkflowImplId(tmp.getWorkflowImplId());
                workflowImpl.setWorkflowImplName(tmp.getWorkflowImplName());
                workflowImpl.setParametersMetaData(tmp.getParametersMetaData());
                workflowImpl.setTemplateId(tmp.getTemplateId());
            }
        }

        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetWorkflowImpl(templateId, workflowImplId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetWorkflowImpl in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return workflowImpl;
    }

    @Override
    public void addWorkflow(Workflow workflow,
                            List<Parameter> parameterList, int tenantId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreAddWorkflow(workflow, parameterList, tenantId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreAddWorkflow in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        //TODO:Workspace Name may contain spaces , so we need to remove spaces and prepare process for that
        Parameter workflowNameParameter =
                new Parameter(workflow.getWorkflowId(), WFConstant.ParameterName.WORKFLOW_NAME,
                        workflow.getWorkflowName(), WFConstant.ParameterName.WORKFLOW_NAME,
                        WFConstant.ParameterHolder.WORKFLOW_IMPL);

        if (!parameterList.contains(workflowNameParameter)) {
            parameterList.add(workflowNameParameter);
        } else {
            workflowNameParameter = parameterList.get(parameterList.indexOf(workflowNameParameter));
        }
        if (!workflowNameParameter.getParamValue().equals(workflow.getWorkflowName())) {
            workflowNameParameter.setParamValue(workflow.getWorkflowName());
            //TODO:Since the user has changed the workflow name, we have to undeploy bpel package that is already
            // deployed using previous workflow name.
        }

        AbstractWorkflow abstractWorkflow =
                WorkflowServiceDataHolder.getInstance().getWorkflowImpls().get(workflow.getTemplateId())
                        .get(workflow.getWorkflowImplId());
        //deploying the template
        abstractWorkflow.deploy(parameterList);

        //add workflow to the database
        if (workflowDAO.getWorkflow(workflow.getWorkflowId()) == null) {
            workflowDAO.addWorkflow(workflow, tenantId);
            WorkflowManagementUtil.createAppRole(StringUtils.deleteWhitespace(workflow.getWorkflowName()));
        } else {
            workflowDAO.removeWorkflowParams(workflow.getWorkflowId());
            workflowDAO.updateWorkflow(workflow);
        }
        workflowDAO.addWorkflowParams(parameterList, workflow.getWorkflowId(), tenantId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostAddWorkflow(workflow, parameterList, tenantId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostAddWorkflow in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

    }

    @Override
    public void addAssociation(String associationName, String workflowId, String eventId, String condition) throws
                                                                                                            WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreAddAssociation(associationName, workflowId, eventId, condition);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreAddAssociation in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        if (StringUtils.isBlank(workflowId)) {
            log.error("Null or empty string given as workflow id to be associated to event.");
            throw new InternalWorkflowException("Service alias cannot be null");
        }
        if (StringUtils.isBlank(eventId)) {
            log.error("Null or empty string given as 'event' to be associated with the service.");
            throw new InternalWorkflowException("Event type cannot be null");
        }

        if (StringUtils.isBlank(condition)) {
            log.error("Null or empty string given as condition expression when associating " + workflowId +
                    " to event " + eventId);
            throw new InternalWorkflowException("Condition cannot be null");
        }

        //check for xpath syntax errors
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            xpath.compile(condition);
            associationDAO.addAssociation(associationName, workflowId, eventId, condition);
        } catch (XPathExpressionException e) {
            log.error("The condition:" + condition + " is not an valid xpath expression.", e);
            throw new WorkflowRuntimeException("The condition is not a valid xpath expression.");
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostAddAssociation(associationName, workflowId, eventId, condition);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostAddAssociation in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
    }

    @Override
    public List<Workflow> listWorkflows(int tenantId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreListWorkflows(tenantId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreListWorkflows in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        List<Workflow> workflowList = workflowDAO.listWorkflows(tenantId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostListWorkflows(tenantId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostListWorkflows in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return workflowList;
    }

    @Override
    public void removeWorkflow(String workflowId) throws WorkflowException {
        Workflow workflow = workflowDAO.getWorkflow(workflowId);
        //Deleting the role that is created for per workflow
        if (workflow != null) {

            List<WorkflowListener> workflowListenerList =
                    WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();

            for (WorkflowListener workflowListener : workflowListenerList) {
                try {
                    workflowListener.doPreDeleteWorkflow(workflow);
                } catch (WorkflowException e) {
                    throw new WorkflowException(
                            "Error occurred while calling doPreDeleteWorkflow in WorkflowListener ," +
                            workflowListener.getClass().getName(), e);
                }
            }

            WorkflowManagementUtil.deleteWorkflowRole(StringUtils.deleteWhitespace(workflow.getWorkflowName()));
            workflowDAO.removeWorkflowParams(workflowId);
            workflowDAO.removeWorkflow(workflowId);

            for (WorkflowListener workflowListener : workflowListenerList) {
                try {
                    workflowListener.doPostDeleteWorkflow(workflow);
                } catch (WorkflowException e) {
                    throw new WorkflowException(
                            "Error occurred while calling doPreDeleteWorkflow in WorkflowListener ," +
                            workflowListener.getClass().getName(), e);
                }
            }

        }
    }

    @Override
    public void removeAssociation(int associationId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreRemoveAssociation(associationId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreRemoveAssociation in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        associationDAO.removeAssociation(associationId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostRemoveAssociation(associationId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostRemoveAssociation in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

    }


    @Override
    public List<Association> getAssociationsForWorkflow(String workflowId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetAssociationsForWorkflow(workflowId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetAssociationsForWorkflow in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        List<Association> associations = associationDAO.listAssociationsForWorkflow(workflowId);
        for (Iterator<Association> iterator = associations.iterator(); iterator.hasNext(); ) {
            Association association = iterator.next();
            WorkflowRequestHandler requestHandler =
                    WorkflowServiceDataHolder.getInstance().getRequestHandler(association.getEventId());
            if (requestHandler != null) {
                association.setEventName(requestHandler.getFriendlyName());
            } else {
                //invalid reference, probably event id is renamed or removed
                iterator.remove();
            }
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetAssociationsForWorkflow(workflowId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetAssociationsForWorkflow in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return associations;
    }

    @Override
    public List<Association> listAllAssociations(int tenantId) throws WorkflowException {

        List<Association> associations = associationDAO.listAssociations(tenantId);
        for (Iterator<Association> iterator = associations.iterator(); iterator.hasNext(); ) {
            Association association = iterator.next();
            WorkflowRequestHandler requestHandler =
                    WorkflowServiceDataHolder.getInstance().getRequestHandler(association.getEventId());
            if (requestHandler != null) {
                association.setEventName(requestHandler.getFriendlyName());
            } else {
                //invalid reference, probably event id is renamed or removed
                iterator.remove();
            }
        }
        return associations;
    }

    @Override
    public void changeAssociationState(String associationId, boolean isEnable) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreChangeAssociationState(associationId,isEnable);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreChangeAssociationState in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        Association association = associationDAO.getAssociation(associationId);
        association.setEnabled(isEnable);
        associationDAO.updateAssociation(association);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostChangeAssociationState(associationId, isEnable);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostChangeAssociationState in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }


    }


    /**
     * Add a new relationship between a workflow request and an entity.
     *
     * @param requestId
     * @param entities
     * @throws InternalWorkflowException
     */
    @Override
    public void addRequestEntityRelationships(String requestId, Entity[] entities) throws InternalWorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreAddRequestEntityRelationships(requestId, entities);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreAddRequestEntityRelationships in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        for (int i = 0; i < entities.length; i++) {
            requestEntityRelationshipDAO.addRelationship(entities[i], requestId);
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostAddRequestEntityRelationships(requestId, entities);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPostAddRequestEntityRelationships in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

    }

    /**
     * Check if a given entity has any pending workflow requests associated with it.
     *
     * @param entity
     * @return
     * @throws InternalWorkflowException
     */
    @Override
    public boolean entityHasPendingWorkflows(Entity entity) throws InternalWorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreEntityHasPendingWorkflows(entity);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreEntityHasPendingWorkflows in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        boolean hasPendingWorkflows = requestEntityRelationshipDAO.entityHasPendingWorkflows(entity);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostEntityHasPendingWorkflows(entity);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPostEntityHasPendingWorkflows in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return hasPendingWorkflows;
    }

    /**
     * Check if a given entity as any pending workflows of a given type associated with it.
     *
     * @param entity
     * @param requestType
     * @return
     * @throws InternalWorkflowException
     */
    @Override
    public boolean entityHasPendingWorkflowsOfType(Entity entity, String requestType) throws
                                                                                      InternalWorkflowException {
        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreEntityHasPendingWorkflowsOfType(entity, requestType);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreEntityHasPendingWorkflowsOfType in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        boolean hasPendingWorkflows = requestEntityRelationshipDAO.entityHasPendingWorkflowsOfType(entity, requestType);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostEntityHasPendingWorkflowsOfType(entity, requestType);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPostEntityHasPendingWorkflowsOfType in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return hasPendingWorkflows;
    }

    /**
     * Check if there are any requests the associated with both entities.
     *
     * @param entity1
     * @param entity2
     * @return
     * @throws InternalWorkflowException
     */
    @Override
    public boolean areTwoEntitiesRelated(Entity entity1, Entity entity2) throws
                                                                         InternalWorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreAreTwoEntitiesRelated(entity1, entity2);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreAreTwoEntitiesRelated in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        boolean twoEntitiesRelated = requestEntityRelationshipDAO.twoEntitiesAreRelated(entity1, entity2);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostAreTwoEntitiesRelated(entity1, entity2);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPostAreTwoEntitiesRelated in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return twoEntitiesRelated;
    }

    /**
     * Check if an operation is engaged with a workflow or not.
     *
     * @param eventType
     * @return
     * @throws InternalWorkflowException
     */
    @Override
    public boolean isEventAssociated(String eventType) throws InternalWorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreIsEventAssociated(eventType);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreIsEventAssociated in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        List<WorkflowAssociation> associations = workflowRequestAssociationDAO.getWorkflowAssociationsForRequest(eventType, CarbonContext
                .getThreadLocalCarbonContext().getTenantId());
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreIsEventAssociated(eventType);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreIsEventAssociated in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        if (associations.size() > 0) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Returns array of requests initiated by a user.
     *
     * @param user     User to get requests of, empty String to retrieve requests of all users
     * @param tenantId tenant id of currently logged in user
     * @return
     * @throws WorkflowException
     */
    @Override
    public WorkflowRequest[] getRequestsCreatedByUser(String user, int tenantId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetRequestsCreatedByUser(user, tenantId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetRequestsCreatedByUser in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        WorkflowRequest[] requests = workflowRequestDAO.getRequestsOfUser(user, tenantId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetRequestsCreatedByUser(user, tenantId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetRequestsCreatedByUser in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return requests;
    }

    /**
     * Get list of workflows of a request
     *
     * @param requestId
     * @return
     * @throws WorkflowException
     */
    @Override
    public WorkflowRequestAssociation[] getWorkflowsOfRequest(String requestId) throws WorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetWorkflowsOfRequest(requestId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetWorkflowsOfRequest in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        WorkflowRequestAssociation[] requestAssociations = workflowRequestAssociationDAO.getWorkflowsOfRequest(requestId);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetWorkflowsOfRequest(requestId);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetWorkflowsOfRequest in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        return requestAssociations;
    }


    @Override
    public void deleteWorkflowRequest(String requestId) throws WorkflowException {
        String loggedUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String createdUser = workflowRequestDAO.retrieveCreatedUserOfRequest(requestId);
        if (!loggedUser.equals(createdUser)) {
            throw new WorkflowException("User not authorized to delete this request");
        }
        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();

        WorkflowRequest workflowRequest = new WorkflowRequest();
        workflowRequest.setRequestId(requestId);
        workflowRequest.setCreatedBy(createdUser);

        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreDeleteWorkflowRequest(workflowRequest);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreDeleteWorkflowRequest in WorkflowListener ," +
                        workflowListener.getClass().getName(), e);
            }
        }

        workflowRequestDAO.updateStatusOfRequest(requestId, WorkflowRequestStatus.DELETED.toString());
        workflowRequestAssociationDAO
                .updateStatusOfRelationshipsOfPendingRequest(requestId, WFConstant.HT_STATE_SKIPPED);
        requestEntityRelationshipDAO.deleteRelationshipsOfRequest(requestId);

        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostDeleteWorkflowRequest(workflowRequest);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostDeleteWorkflowRequest in WorkflowListener ," +
                        workflowListener.getClass().getName(), e);
            }
        }
    }

    /**
     * get requests list according to createdUser, createdTime, and lastUpdatedTime
     *
     * @param user         User to get requests of, empty String to retrieve requests of all users
     * @param beginDate    lower limit of date range to filter
     * @param endDate      upper limit of date range to filter
     * @param dateCategory filter by created time or last updated time ?
     * @param tenantId     tenant id of currently logged in user
     * @return
     * @throws WorkflowException
     */
    @Override
    public WorkflowRequest[] getRequestsFromFilter(String user, String beginDate, String endDate, String
            dateCategory, int tenantId, String status) throws WorkflowException {

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_FILTERING);
        Timestamp beginTime;
        Timestamp endTime;

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreGetRequestsFromFilter(user, beginDate, endDate, dateCategory, tenantId, status);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPreGetRequestsFromFilter in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        try {
            Date parsedBeginDate = dateFormat.parse(beginDate);
            beginTime = new java.sql.Timestamp(parsedBeginDate.getTime());
        } catch (ParseException e) {
            long millis = 0;
            Date parsedBeginDate = new Date(millis);
            beginTime = new java.sql.Timestamp(parsedBeginDate.getTime());
        }
        try {
            Date parsedEndDate = dateFormat.parse(endDate);
            endTime = new java.sql.Timestamp(parsedEndDate.getTime());
        } catch (ParseException e) {
            Date parsedEndDate = new Date();
            endTime = new java.sql.Timestamp(parsedEndDate.getTime());
        }
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostGetRequestsFromFilter(user, beginDate, endDate, dateCategory, tenantId, status);
            } catch (WorkflowException e) {
                throw new WorkflowException(
                        "Error occurred while calling doPostGetRequestsFromFilter in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }

        if (StringUtils.isBlank(user)) {
            return workflowRequestDAO.getRequestsFilteredByTime(beginTime, endTime, dateCategory, tenantId, status);
        } else {
            return workflowRequestDAO.getRequestsOfUserFilteredByTime(user, beginTime, endTime, dateCategory,
                                                                      tenantId, status);
        }

    }

    /**
     * Retrieve List of associated Entity-types of the workflow requests.
     *
     * @param wfOperationType Operation Type of the Work-flow.
     * @param wfStatus        Current Status of the Work-flow.
     * @param entityType      Entity Type of the Work-flow.
     * @param tenantID        Tenant ID of the currently Logged user.
     * @param idFilter        Entity ID filter to search
     * @return
     * @throws InternalWorkflowException
     */
    @Override
    public List<String> listEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID,
                                        String idFilter) throws InternalWorkflowException {

        List<WorkflowListener> workflowListenerList =
                WorkflowServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPreListEntityNames(wfOperationType, wfStatus, entityType, tenantID, idFilter);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPreListEntityNames in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        List<String> requestEntities = requestEntityRelationshipDAO.getEntityNamesOfRequest(wfOperationType, wfStatus,
                entityType, idFilter, tenantID);
        for (WorkflowListener workflowListener : workflowListenerList) {
            try {
                workflowListener.doPostListEntityNames(wfOperationType, wfStatus, entityType, tenantID, idFilter);
            } catch (WorkflowException e) {
                throw new InternalWorkflowException(
                        "Error occurred while calling doPostListEntityNames in WorkflowListener ," +
                                workflowListener.getClass().getName(), e);
            }
        }
        return requestEntities;
    }


}
