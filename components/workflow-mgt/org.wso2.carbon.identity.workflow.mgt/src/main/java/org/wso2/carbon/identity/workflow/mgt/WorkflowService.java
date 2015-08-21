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
import org.wso2.carbon.identity.workflow.mgt.bean.AssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.bean.Entity;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociationBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.dao.RequestEntityRelationshipDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestAssociationDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplate;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.dao.BPSProfileDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowDAO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WorkflowService {

    private static Log log = LogFactory.getLog(WorkflowService.class);

    WorkflowDAO workflowDAO = new WorkflowDAO();
    BPSProfileDAO bpsProfileDAO = new BPSProfileDAO();
    RequestEntityRelationshipDAO requestEntityRelationshipDAO = new RequestEntityRelationshipDAO();
    WorkflowRequestDAO workflowRequestDAO = new WorkflowRequestDAO();
    WorkflowRequestAssociationDAO workflowRequestAssociationDAO = new WorkflowRequestAssociationDAO();

    public List<WorkflowEventDTO> listWorkflowEvents() {

        List<WorkflowRequestHandler> workflowRequestHandlers =
                WorkflowServiceDataHolder.getInstance().listRequestHandlers();
        List<WorkflowEventDTO> eventList = new ArrayList<>();
        if (workflowRequestHandlers != null) {
            for (WorkflowRequestHandler requestHandler : workflowRequestHandlers) {
                WorkflowEventDTO eventDTO = new WorkflowEventDTO();
                eventDTO.setEventId(requestHandler.getEventId());
                eventDTO.setEventFriendlyName(requestHandler.getFriendlyName());
                eventDTO.setEventDescription(requestHandler.getDescription());
                eventDTO.setEventCategory(requestHandler.getCategory());
                //note: parameters are not set at here in list operation. It's set only at get operation
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
                    eventDTO.setParameters(parameters);
                }
                eventList.add(eventDTO);
            }
        }
        return eventList;
    }

    public WorkflowEventDTO getEvent(String id) {

        WorkflowRequestHandler requestHandler = WorkflowServiceDataHolder.getInstance().getRequestHandler(id);
        if (requestHandler != null) {
            WorkflowEventDTO eventDTO = new WorkflowEventDTO();
            eventDTO.setEventId(requestHandler.getEventId());
            eventDTO.setEventFriendlyName(requestHandler.getFriendlyName());
            eventDTO.setEventDescription(requestHandler.getDescription());
            eventDTO.setEventCategory(requestHandler.getCategory());
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
                eventDTO.setParameters(parameters);
            }
            return eventDTO;
        }
        return null;
    }

    public List<TemplateBean> listWorkflowTemplates() {

        List<AbstractWorkflowTemplate> templateList =
                WorkflowServiceDataHolder.getInstance().listTemplates();
        List<TemplateBean> templateBeans = new ArrayList<>();
        if (templateList != null) {
            for (AbstractWorkflowTemplate template : templateList) {
                TemplateBean templateBean = new TemplateBean();
                templateBean.setId(template.getTemplateId());
                templateBean.setName(template.getFriendlyName());
                templateBean.setDescription(template.getDescription());
                templateBeans.add(templateBean);
            }
        }
        return templateBeans;
    }

    public TemplateDTO getTemplateDTO(String templateId) {

        AbstractWorkflowTemplate template = WorkflowServiceDataHolder.getInstance().getTemplate(templateId);
        TemplateDTO templateDTO = new TemplateDTO();
        templateDTO.setId(template.getTemplateId());
        templateDTO.setName(template.getFriendlyName());
        templateDTO.setDescription(template.getDescription());
        TemplateImplDTO[] templateImplDTOs = new TemplateImplDTO[template.getImplementations().size()];
        for (int i = 0; i < template.getImplementations().size(); i++) {
            TemplateImplDTO implDTO = new TemplateImplDTO();
            implDTO.setTemplateId(templateId);
            implDTO.setImplementationId(template.getImplementations().get(i).getImplementationId());
            implDTO.setImplementationName(template.getImplementations().get(i).getImplementationName());
            //not sending any params here because we are giving the complete Impl list here, they will be sent
            // when requesting individual templateImpl
            implDTO.setImplementationParams(new TemplateParameterDef[0]);
            templateImplDTOs[i] = implDTO;
        }
        templateDTO.setParameters(template.getParamDefinitions());
        templateDTO.setImplementations(templateImplDTOs);
        return templateDTO;
    }

    public TemplateImplDTO getTemplateImplDTO(String template, String implName) {

        AbstractWorkflowTemplate workflowTemplate = WorkflowServiceDataHolder.getInstance().getTemplate(template);
        if (template != null) {
            AbstractWorkflowTemplateImpl templateImpl = workflowTemplate.getImplementation(implName);
            if (templateImpl != null) {
                TemplateImplDTO implDTO = new TemplateImplDTO();
                implDTO.setImplementationId(templateImpl.getTemplateId());
                implDTO.setImplementationName(templateImpl.getImplementationName());
                implDTO.setTemplateId(templateImpl.getTemplateId());
                implDTO.setImplementationParams(templateImpl.getImplParamDefinitions());
                return implDTO;
            }
        }
        return null;
    }

    public void addBPSProfile(String profileName, String host, String user, String password, String callBackUser,
                              String callbackPassword, int tenantId)
            throws InternalWorkflowException {

        bpsProfileDAO.addProfile(profileName, host, user, password, callBackUser, callbackPassword, tenantId);
    }

    public List<BPSProfileBean> listBPSProfiles(int tenantId) throws WorkflowException {

        return bpsProfileDAO.listBPSProfiles(tenantId);
    }

    public void removeBPSProfile(String profileName) throws WorkflowException {

        bpsProfileDAO.removeBPSProfile(profileName);
    }

    public void addWorkflow(String id, String name, String description, String templateId, String templateImpl,
                            Parameter[] templateParams, Parameter[] implParams, int tenantId) throws WorkflowException {

        workflowDAO.addWorkflow(id, name, description, templateId, templateImpl, tenantId);
        Map<String, Object> paramMap = new HashMap<>();
        if (templateParams != null) {
            for (Parameter param : templateParams) {
                paramMap.put(param.getParamName(), param.getParamValue());
            }
        }
        if (implParams != null) {
            for (Parameter param : implParams) {
                paramMap.put(param.getParamName(), param.getParamValue());
            }
        }
        paramMap.put(WorkFlowConstants.TemplateConstants.WORKFLOW_NAME, name);
        workflowDAO.addWorkflowParams(id, paramMap);
        AbstractWorkflowTemplateImpl templateImplementation =
                WorkflowServiceDataHolder.getInstance().getTemplateImplementation(templateId, templateImpl);
        //deploying the template
        templateImplementation.deploy(paramMap);
    }

    public void addAssociation(String associationName, String workflowId, String eventId, String condition) throws
            WorkflowException {

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
            workflowDAO.addAssociation(associationName, workflowId, eventId, condition);
        } catch (XPathExpressionException e) {
            log.error("The condition:" + condition + " is not an valid xpath expression.", e);
            throw new RuntimeWorkflowException("The condition is not a valid xpath expression.");
        }
    }

    public List<WorkflowBean> listWorkflows(int tenantId) throws WorkflowException {

        return workflowDAO.listWorkflows(tenantId);
    }

    public void removeWorkflow(String id) throws WorkflowException {

        workflowDAO.removeWorkflow(id);
    }

    public void removeAssociation(int associationId) throws WorkflowException {

        workflowDAO.removeAssociation(associationId);
    }

    public Map<String, Object> getBPSProfileParams(String profileName) throws WorkflowException {

        return bpsProfileDAO.getBPELProfileParams(profileName);
    }

    public List<AssociationDTO> getAssociationsForWorkflow(String workflowId) throws WorkflowException {

        List<AssociationDTO> associations = workflowDAO.listAssociationsForWorkflow(workflowId);
        for (Iterator<AssociationDTO> iterator = associations.iterator(); iterator.hasNext(); ) {
            AssociationDTO association = iterator.next();
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

    public List<AssociationDTO> listAllAssociations() throws WorkflowException {

        List<AssociationDTO> associations = workflowDAO.listAssociations();
        for (Iterator<AssociationDTO> iterator = associations.iterator(); iterator.hasNext(); ) {
            AssociationDTO association = iterator.next();
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

    /**
     * Add a new relationship between a workflow request and an entity.
     *
     * @param requestId
     * @param entities
     * @throws InternalWorkflowException
     */
    public void addRequestEntityRelationships(String requestId, Entity[] entities) throws InternalWorkflowException {

        for (int i = 0; i < entities.length; i++) {
            requestEntityRelationshipDAO.addRelationship(entities[i], requestId);
        }
    }

    /**
     * Check if a given entity has any pending workflow requests associated with it.
     *
     * @param entity
     * @return
     * @throws InternalWorkflowException
     */
    public boolean entityHasPendingWorkflows(Entity entity) throws InternalWorkflowException {
        return requestEntityRelationshipDAO.entityHasPendingWorkflows(entity);
    }

    /**
     * Check if a given entity as any pending workflows of a given type associated with it.
     *
     * @param entity
     * @param requestType
     * @return
     * @throws InternalWorkflowException
     */
    public boolean entityHasPendingWorkflowsOfType(Entity entity, String requestType) throws
            InternalWorkflowException {
        return requestEntityRelationshipDAO.entityHasPendingWorkflowsOfType(entity, requestType);
    }

    /**
     * Check if there are any requests the associated with both entities.
     *
     * @param entity1
     * @param entity2
     * @return
     * @throws InternalWorkflowException
     */
    public boolean areTwoEntitiesRelated(Entity entity1, Entity entity2) throws
            InternalWorkflowException {
        return requestEntityRelationshipDAO.twoEntitiesAreRelated(entity1, entity2);
    }

    /**
     * Check if an operation is engaged with a workflow or not.
     *
     * @param eventType
     * @return
     * @throws InternalWorkflowException
     */
    public boolean eventEngagedWithWorkflows(String eventType) throws InternalWorkflowException {

        List<WorkflowAssociationBean> associations = workflowDAO.getWorkflowsForRequest(eventType, CarbonContext
                .getThreadLocalCarbonContext().getTenantId());
        if (associations.size() > 0) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * Returns array of requests initiated by a user.
     *
     * @param user
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsCreatedByUser(String user) throws WorkflowException {

        return workflowRequestDAO.getRequestsOfUser(user);
    }

    /**
     * Get list of workflows of a request
     *
     * @param requestId
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestAssociationDTO[] getWorkflowsOfRequest(String requestId) throws WorkflowException {

        return workflowRequestAssociationDAO.getWorkflowsOfRequest(requestId);
    }

    /**
     * Update state of a existing workflow request
     *
     * @param requestId
     * @param newState
     * @throws WorkflowException
     */
    public void updateStatusOfRequest(String requestId, String newState) throws WorkflowException {
        if (WorkflowRequestStatus.DELETED.toString().equals(newState)) {
            workflowRequestDAO.updateStatusOfRequest(requestId, newState);
        }
        requestEntityRelationshipDAO.deleteRelationshipsOfRequest(requestId);
    }

    /**
     * get requests list according to createdUser, createdTime, and lastUpdatedTime
     *
     * @param user
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsFromFilter(String user, String beginDate, String endDate, String
            dateCategory) throws WorkflowException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Timestamp beginTime;
        Timestamp endTime;

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
        if (StringUtils.isBlank(user)) {
            return workflowRequestDAO.getRequestsFilteredByTime(beginTime, endTime, dateCategory);
        } else {
            return workflowRequestDAO.getRequestsOfUserFilteredByTime(user, beginTime, endTime, dateCategory);
        }

    }
}
