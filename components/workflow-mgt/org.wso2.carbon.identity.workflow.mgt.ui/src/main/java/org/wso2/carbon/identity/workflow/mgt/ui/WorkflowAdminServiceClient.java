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

package org.wso2.carbon.identity.workflow.mgt.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.mgt.dto.xsd.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.xsd.Template;
import org.wso2.carbon.identity.workflow.mgt.dto.xsd.Workflow;
import org.wso2.carbon.identity.workflow.mgt.dto.xsd.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceStub;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventDTO;

import java.rmi.RemoteException;

public class WorkflowAdminServiceClient {

    private WorkflowAdminServiceStub stub;
    private static final Log log = LogFactory.getLog(WorkflowAdminServiceClient.class);

    /**
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws AxisFault
     */
    public WorkflowAdminServiceClient(String cookie, String backendServerURL,
                                      ConfigurationContext configCtx) throws AxisFault {

        String serviceURL = backendServerURL + "WorkflowAdminService";
        stub = new WorkflowAdminServiceStub(configCtx, serviceURL);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public WorkflowEventDTO[] listWorkflowEvents() throws RemoteException {

        WorkflowEventDTO[] workflowEvents = stub.listWorkflowEvents();
        if (workflowEvents == null) {
            workflowEvents = new WorkflowEventDTO[0];
        }
        return workflowEvents;
    }

    public Template[] listTemplates() throws RemoteException {

        Template[] templates = stub.listWorkflowTemplates();
        if (templates == null) {
            templates = new Template[0];
        }
        return templates;
    }

    public Template getTemplate(String templateName) throws RemoteException {

        Template template = stub.getTemplate(templateName);
        return template;
    }

    public WorkflowImpl getWorkflowImp(String template, String implName) throws RemoteException {

        WorkflowImpl workflowImpl = stub.getTemplateImpl(template, implName);
        return workflowImpl;
    }

    /**
     * Add new workflow
     *
     * @throws RemoteException
     * @throws WorkflowAdminServiceWorkflowException
     */
    public void addWorkflow(Workflow workflow, Parameter[] parameters)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.addWorkflow(workflow, parameters);

    }



    /**
     * Retrieve Workflows
     *
     * @return
     * @throws RemoteException
     * @throws WorkflowAdminServiceWorkflowException
     */
    public Workflow[] listWorkflows() throws RemoteException, WorkflowAdminServiceWorkflowException {

        Workflow[] workflows = stub.listWorkflows();
        if (workflows == null) {
            workflows = new Workflow[0];
        }
        return workflows;
    }

    public void deleteWorkflow(String workflowId) throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.removeWorkflow(workflowId);
    }

    public Association[] listAssociationsForWorkflow(String workflowId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {
        Association[] associationsForWorkflow = stub.listAssociations(workflowId);
        if (associationsForWorkflow == null) {
            associationsForWorkflow = new Association[0];
        }
        return associationsForWorkflow;
    }

    public Association[] listAllAssociations() throws RemoteException, WorkflowAdminServiceWorkflowException {

        Association[] associations = stub.listAllAssociations();
        if (associations == null) {
            associations = new Association[0];
        }
        return associations;
    }

    public void deleteAssociation(String associationId) throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.removeAssociation(associationId);
    }

    public void addAssociation(String workflowId, String associationName, String eventId, String condition)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.addAssociation(associationName, workflowId, eventId, condition);
    }

    /**
     * Enable association to allow to execute
     *
     * @param associationId
     * @throws RemoteException
     * @throws WorkflowAdminServiceWorkflowException
     */
    public void enableAssociation(String associationId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.changeAssociationState(associationId, true);
    }

    /**
     * Disable association to avoid with execution of the workflows
     *
     * @param associationId
     * @throws RemoteException
     * @throws WorkflowAdminServiceWorkflowException
     */
    public void disableAssociation(String associationId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.changeAssociationState(associationId,false);
    }

    public WorkflowEventDTO getEvent(String id) throws RemoteException {

        return stub.getEvent(id);
    }

    public WorkflowRequestDTO[] getRequestsCreatedByUser(String user, String beginDate, String endDate, String
            dateCategory) throws RemoteException, WorkflowAdminServiceWorkflowException {

        WorkflowRequestDTO[] requestDTOs = stub.getRequestsCreatedByUser(user, beginDate, endDate, dateCategory);
        if (requestDTOs == null) {
            requestDTOs = new WorkflowRequestDTO[0];
        }
        return requestDTOs;

    }

    public WorkflowRequestDTO[] getAllRequests(String beginDate, String endDate, String dateCategory) throws
            RemoteException, WorkflowAdminServiceWorkflowException {

        WorkflowRequestDTO[] requestDTOs = stub.getRequestsInFilter(beginDate, endDate, dateCategory);
        if (requestDTOs == null) {
            requestDTOs = new WorkflowRequestDTO[0];
        }
        return requestDTOs;
    }

    public void deleteRequest(String requestId) throws WorkflowAdminServiceWorkflowException, RemoteException {
        stub.deleteWorkflowRequest(requestId);
    }

    public WorkflowRequestAssociationDTO[] getWorkflowsOfRequest(String requestId) throws
            WorkflowAdminServiceWorkflowException, RemoteException {

        return stub.getWorkflowsOfRequest(requestId);
    }

}
