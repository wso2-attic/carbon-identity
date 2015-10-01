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
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.Association;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.Template;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.WorkflowWizard;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.WorkflowEvent;
import org.wso2.carbon.identity.workflow.mgt.stub.metadata.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequestAssociation;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceStub;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException;

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

    public WorkflowEvent[] listWorkflowEvents() throws RemoteException {

        WorkflowEvent[] workflowEvents = stub.listWorkflowEvents();
        if (workflowEvents == null) {
            workflowEvents = new WorkflowEvent[0];
        }
        return workflowEvents;
    }

    public Template[] listTemplates() throws RemoteException, WorkflowAdminServiceWorkflowException {

        Template[] templates = stub.listTemplates();
        if (templates == null) {
            templates = new Template[0];
        }
        return templates;
    }

    public WorkflowImpl[] listWorkflowImpls(String templateId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        WorkflowImpl[] workflows = stub.listWorkflowImpls(templateId);
        if (workflows == null) {
            workflows = new WorkflowImpl[0];
        }
        return workflows;
    }

    public Template getTemplate(String templateName) throws RemoteException, WorkflowAdminServiceWorkflowException {

        Template template = stub.getTemplate(templateName);
        return template;
    }

    public WorkflowImpl getWorkflowImp(String template, String implName)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        WorkflowImpl workflowImpl = stub.getWorkflowImpl(template, implName);
        return workflowImpl;
    }

    /**
     * Add new workflow
     *
     * @throws RemoteException
     * @throws WorkflowAdminServiceWorkflowException
     */
    public void addWorkflow(WorkflowWizard workflowWizard)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.addWorkflow(workflowWizard);

    }

    public WorkflowWizard getWorkflow(String workflowId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        return stub.getWorkflow(workflowId);
    }



    /**
     * Retrieve Workflows
     *
     * @return
     * @throws RemoteException
     * @throws WorkflowAdminServiceWorkflowException
     */
    public WorkflowWizard[] listWorkflows() throws RemoteException, WorkflowAdminServiceWorkflowException {

        WorkflowWizard[] workflows = stub.listWorkflows();
        if (workflows == null) {
            workflows = new WorkflowWizard[0];
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

    public WorkflowEvent getEvent(String id) throws RemoteException {

        return stub.getEvent(id);
    }

    public WorkflowRequest[] getRequestsCreatedByUser(String user, String beginDate, String endDate, String
            dateCategory, String status) throws RemoteException, WorkflowAdminServiceWorkflowException {
        //TODO ADD status as param
        WorkflowRequest[] request = stub.getRequestsCreatedByUser(user, beginDate, endDate, dateCategory,status);
        if (request == null) {
            request = new WorkflowRequest[0];
        }
        return request;

    }

    public WorkflowRequest[] getAllRequests(String beginDate, String endDate, String dateCategory, String status) throws
            RemoteException, WorkflowAdminServiceWorkflowException {

        //TODO ADD status as param
        WorkflowRequest[] requests = stub.getRequestsInFilter(beginDate, endDate, dateCategory,status);
        if (requests == null) {
            requests = new WorkflowRequest[0];
        }
        return requests;
    }

    public void deleteRequest(String requestId) throws WorkflowAdminServiceWorkflowException, RemoteException {
        stub.deleteWorkflowRequest(requestId);
    }

    public WorkflowRequestAssociation[] getWorkflowsOfRequest(String requestId) throws
            WorkflowAdminServiceWorkflowException, RemoteException {

        return stub.getWorkflowsOfRequest(requestId);
    }

}
