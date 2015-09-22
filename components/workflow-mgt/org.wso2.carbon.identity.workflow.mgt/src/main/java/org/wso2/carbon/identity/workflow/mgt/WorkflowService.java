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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.databinding.types.NCName;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultRow;
import org.wso2.carbon.humantask.stub.ui.task.client.api.HumanTaskClientAPIAdminStub;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalAccessFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalArgumentFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalOperationFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalStateFault;
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.ParameterDTO;;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociationBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowDTO;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryCategory;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryInput;
import org.wso2.carbon.humantask.stub.types.TStatus;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultSet;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplate;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.bean.AssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.Entity;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowEventDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.dao.RequestEntityRelationshipDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestAssociationDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.dao.BPSProfileDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowDAO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
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

    public static final String DATE_FORMAT_FOR_FILTERING = "MM/dd/yyyy";
    public static final String HT_SERVICES_URL = "services/HumanTaskClientAPIAdmin";
    public static final String HT_PARAMETER_LIST_ELEMENT = "parametersList";
    public static final String HT_ITEM_NAME_ATTRIBUTE = "itemName";
    public static final String HT_REQUEST_ID_ATTRIBUTE_VALUE = "REQUEST ID";
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
                    ParameterDTO[] parameterDTOs = new ParameterDTO[requestHandler.getParamDefinitions().size()];
                    int i = 0;
                    for (Map.Entry<String, String> paramEntry : requestHandler.getParamDefinitions().entrySet()) {
                        ParameterDTO parameterDTO = new ParameterDTO();
                        parameterDTO.setParamName(paramEntry.getKey());
                        parameterDTO.setParamValue(paramEntry.getValue());
                        parameterDTOs[i] = parameterDTO;
                        i++;
                    }
                    eventDTO.setParameterDTOs(parameterDTOs);
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
                ParameterDTO[] parameterDTOs = new ParameterDTO[requestHandler.getParamDefinitions().size()];
                int i = 0;
                for (Map.Entry<String, String> paramEntry : requestHandler.getParamDefinitions().entrySet()) {
                    ParameterDTO parameterDTO = new ParameterDTO();
                    parameterDTO.setParamName(paramEntry.getKey());
                    parameterDTO.setParamValue(paramEntry.getValue());
                    parameterDTOs[i] = parameterDTO;
                    i++;
                }
                eventDTO.setParameterDTOs(parameterDTOs);
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

    public void addBPSProfile(BPSProfileDTO bpsProfileDTO, int tenantId)
            throws InternalWorkflowException {

        bpsProfileDAO.addProfile(bpsProfileDTO, tenantId);
    }

    public List<BPSProfileDTO> listBPSProfiles(int tenantId) throws WorkflowException {

        return bpsProfileDAO.listBPSProfiles(tenantId);
    }

    public void removeBPSProfile(String profileName) throws WorkflowException {

        bpsProfileDAO.removeBPSProfile(profileName);
    }

    public void addWorkflow(WorkflowDTO workflowDTO,
                            ParameterDTO[] templateParams, ParameterDTO[] implParams, int tenantId) throws WorkflowException {
        Map<String, Object> paramMap = new HashMap<>();
        if (templateParams != null) {
            for (ParameterDTO param : templateParams) {
                paramMap.put(param.getParamName(), param.getParamValue());
            }
        }
        if (implParams != null) {
            for (ParameterDTO param : implParams) {
                paramMap.put(param.getParamName(), param.getParamValue());
            }
        }
        paramMap.put(WorkFlowConstants.TemplateConstants.WORKFLOW_NAME, workflowDTO.getWorkflowName());

        AbstractWorkflowTemplateImpl templateImplementation =
                WorkflowServiceDataHolder.getInstance().getTemplateImplementation(workflowDTO.getTemplateName(), workflowDTO.getImplementationName());
        //deploying the template
        templateImplementation.deploy(paramMap);

        //add workflow to the database
        workflowDAO.addWorkflow(workflowDTO, tenantId);
        workflowDAO.addWorkflowParams(workflowDTO.getWorkflowId(), paramMap);

        //Creating a role for the workflow
        WorkflowManagementUtil.createAppRole(workflowDTO.getWorkflowName());
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

    public List<WorkflowDTO> listWorkflows(int tenantId) throws WorkflowException {

        return workflowDAO.listWorkflows(tenantId);
    }

    public void removeWorkflow(String id) throws WorkflowException {
        WorkflowDTO workflow = workflowDAO.getWorkflow(id);
        //Deleting the role that is created for per workflow
        WorkflowManagementUtil.deleteWorkflowRole(workflow.getWorkflowName());
        workflowDAO.removeWorkflow(id);
    }

    public void removeAssociation(int associationId) throws WorkflowException {

        workflowDAO.removeAssociation(associationId);
    }

    public Map<String, Object> getBPSProfileParams(String profileName) throws WorkflowException {

        return bpsProfileDAO.getBPELProfileParams(profileName);
    }

    public BPSProfileDTO getBPSProfile(String profileName, int tenantId) throws WorkflowException {

        return bpsProfileDAO.getBPSProfile(profileName, tenantId, false);
    }

    public void updateBPSProfile(BPSProfileDTO bpsProfileDTO, int tenantId) throws WorkflowException {
        BPSProfileDTO currentBpsProfile =  bpsProfileDAO.getBPSProfile(bpsProfileDTO.getProfileName(), tenantId,true);
        if(bpsProfileDTO.getPassword()==null || bpsProfileDTO.getPassword().isEmpty()){
            bpsProfileDTO.setPassword(currentBpsProfile.getPassword());
        }
        if(bpsProfileDTO.getCallbackPassword()==null || bpsProfileDTO.getCallbackPassword().isEmpty()){
            bpsProfileDTO.setCallbackPassword(currentBpsProfile.getCallbackPassword());
        }
        bpsProfileDAO.updateProfile(bpsProfileDTO, tenantId);
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

    public void changeAssociationState(String associationId, boolean isEnable) throws WorkflowException {

        AssociationDTO association = workflowDAO.getAssociation(associationId);
        association.setEnabled(isEnable);
        workflowDAO.updateAssociation(association);
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

        List<WorkflowAssociationBean> associations = workflowDAO.getWorkflowAssociationsForRequest(eventType, CarbonContext
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
     * @param user User to get requests of, empty String to retrieve requests of all users
     * @param tenantId tenant id of currently logged in user
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsCreatedByUser(String user, int tenantId) throws WorkflowException {

        return workflowRequestDAO.getRequestsOfUser(user, tenantId);
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
            String loggedUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            if (!loggedUser.equals(workflowRequestDAO.retrieveCreatedUserOfRequest(requestId))) {
                throw  new WorkflowException("User not authorized to delete this request");
            }
            deleteHumanTasks(requestId);
            workflowRequestDAO.updateStatusOfRequest(requestId, newState);
            workflowRequestAssociationDAO.updateStatusOfRelationshipsOfRequest(requestId, WorkFlowConstants
                    .HT_STATE_SKIPPED);
        }
        requestEntityRelationshipDAO.deleteRelationshipsOfRequest(requestId);
    }

    /**
     * get requests list according to createdUser, createdTime, and lastUpdatedTime
     *
     * @param user User to get requests of, empty String to retrieve requests of all users
     * @param beginDate lower limit of date range to filter
     * @param endDate upper limit of date range to filter
     * @param dateCategory filter by created time or last updated time ?
     * @param tenantId tenant id of currently logged in user
     * @return
     * @throws WorkflowException
     */
    public WorkflowRequestDTO[] getRequestsFromFilter(String user, String beginDate, String endDate, String
            dateCategory, int tenantId, String status) throws WorkflowException {

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_FILTERING);
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
     * @return
     * @throws InternalWorkflowException
     */
    public List<String> listEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID) throws
            InternalWorkflowException {
        return requestEntityRelationshipDAO.getEntityNamesOfRequest(wfOperationType, wfStatus, entityType, tenantID);
    }

    /**
     * Delete human tasks associated with a given request
     *
     * @param requestId request id of the request to delete human tasks of
     * @throws WorkflowException
     */
    private void deleteHumanTasks(String requestId) throws WorkflowException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            List<BPSProfileDTO> bpsProfiles = bpsProfileDAO.listBPSProfiles(tenantId);
            HumanTaskClientAPIAdminStub stub = null;
            TSimpleQueryInput input = new TSimpleQueryInput();
            TStatus reservedState = new TStatus();
            reservedState.setTStatus(WorkFlowConstants.HT_STATE_RESERVED);
            input.addStatus(reservedState);
            TStatus readyState = new TStatus();
            readyState.setTStatus(WorkFlowConstants.HT_STATE_READY);
            input.addStatus(readyState);
            input.setPageSize(100000);
            input.setPageNumber(0);
            input.setSimpleQueryCategory(TSimpleQueryCategory.ALL_TASKS);
            for (int i = 0; i < bpsProfiles.size(); i++) {
                String host = bpsProfiles.get(i).getHost();
                URL servicesUrl = new URL(new URL(host), HT_SERVICES_URL);
                stub = new HumanTaskClientAPIAdminStub(servicesUrl.toString());
                ServiceClient client = stub._getServiceClient();
                authenticate(client, bpsProfiles.get(i).getUsername(), bpsProfiles.get(i).getPassword());
                TTaskSimpleQueryResultSet results = stub.simpleQuery(input);
                TTaskSimpleQueryResultRow[] arr = results.getRow();
                for (int j = 0; j < arr.length; j++) {
                    Object task = stub.getInput(arr[j].getId(),new NCName(""));
                    InputStream stream = new ByteArrayInputStream(task.toString().getBytes(StandardCharsets.UTF_8));
                    OMElement taskXML = new StAXOMBuilder(stream).getDocumentElement();
                    Iterator<OMElementImpl> iterator = taskXML.getChildElements();
                    while (iterator.hasNext()) {
                        OMElementImpl child = iterator.next();
                        checkMatchingTaskAndDelete(requestId, stub, arr, j, child);
                    }

                }
            }
        } catch (MalformedURLException | XMLStreamException | IllegalOperationFault | IllegalAccessFault |
                RemoteException | IllegalStateFault | IllegalArgumentFault e) {
            throw new WorkflowException("Error while deleting the human tasks of the request.");
        }

    }

    /**
     *
     *
     * @param requestId Id of the deleting request
     * @param stub stub to call HumanTaskClientAPIAdmin
     * @param resultsList task list in the current human task engine
     * @param resultIndex index of the currently considering rask
     * @param taskElement currently considering task
     * @throws RemoteException
     * @throws IllegalStateFault
     * @throws IllegalOperationFault
     * @throws IllegalArgumentFault
     * @throws IllegalAccessFault
     */
    private void checkMatchingTaskAndDelete(String requestId, HumanTaskClientAPIAdminStub stub,
                                            TTaskSimpleQueryResultRow[] resultsList, int resultIndex, OMElementImpl
                                                    taskElement) throws RemoteException, IllegalStateFault,
            IllegalOperationFault, IllegalArgumentFault, IllegalAccessFault {
        if (taskElement.getLocalName().equals(HT_PARAMETER_LIST_ELEMENT)) {
            Iterator<OMElementImpl> parameters = taskElement.getChildElements();
            while (parameters.hasNext()) {
                OMElementImpl parameter = parameters.next();
                Iterator<OMAttribute> attributes = parameter.getAllAttributes();
                while (attributes.hasNext()) {
                    OMAttribute currentAttribute = attributes.next();
                    if (currentAttribute.getLocalName().equals(HT_ITEM_NAME_ATTRIBUTE) && currentAttribute
                            .getAttributeValue().equals(HT_REQUEST_ID_ATTRIBUTE_VALUE)) {
                        Iterator<OMElementImpl> itemValues = parameter.getChildElements();
                        if (itemValues.hasNext()) {
                            String taskRequestId = itemValues.next().getText();
                            if (taskRequestId.contains(",")) {
                                taskRequestId = taskRequestId.replaceAll(",", "");
                            }
                            if (taskRequestId.equals(requestId)) {
                                //stub.skip(resultsList[resultIndex].getId());
                            }
                        }

                    }
                }
            }
        }
    }

    private void authenticate(ServiceClient client, String accessUsername, String accessPassword) throws WorkflowException {

        if (accessUsername != null && accessPassword != null) {
            Options option = client.getOptions();
            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setUsername(accessUsername);
            auth.setPassword(accessPassword);
            auth.setPreemptiveAuthentication(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
            option.setManageSession(true);

        } else {
            throw new WorkflowException("Authentication username or password not set");
        }
    }
}
