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
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequestAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowRequestDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceStub;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.AssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateParameterDef;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowEventDTO;

import java.rmi.RemoteException;
import java.util.List;

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

    public TemplateBean[] listTemplates() throws RemoteException {

        TemplateBean[] templates = stub.listWorkflowTemplates();
        if (templates == null) {
            templates = new TemplateBean[0];
        }
        return templates;
    }

    public TemplateDTO getTemplate(String templateName) throws RemoteException {

        TemplateDTO templateDTO = stub.getTemplateDTO(templateName);
        if (templateDTO != null) {
            if (templateDTO.getParameters() == null) {
                templateDTO.setParameters(new TemplateParameterDef[0]);
            }
            if (templateDTO.getImplementations() == null) {
                templateDTO.setImplementations(new TemplateImplDTO[0]);
            }
        }
        return templateDTO;
    }

    public TemplateImplDTO getTemplateImpDTO(String template, String implName) throws RemoteException {

        TemplateImplDTO templateImplDTO = stub.getTemplateImplDTO(template, implName);
        if (templateImplDTO != null) {
            if (templateImplDTO.getImplementationParams() == null) {
                templateImplDTO.setImplementationParams(new TemplateParameterDef[0]);
            }
        }
        return templateImplDTO;
    }

    public void addWorkflow(String workflowName, String description, String templateName, String templateImplName,
                            List<Parameter> templateParams, List<Parameter> templateImplParams)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.addWorkflow(workflowName, description, templateName, templateImplName, templateParams.toArray(new
                        Parameter[templateParams.size()]),
                templateImplParams.toArray(new Parameter[templateImplParams.size()]));

    }

    public void addBPSProfile(String profileName, String host, String user, String password, String callbackUser,
                              String callbackPassword)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

//        String[] splittedPw = password.split("(?!^)");
        stub.addBPSProfile(profileName, host, user, password, callbackUser, callbackPassword);
    }

    public BPSProfileBean[] listBPSProfiles() throws RemoteException, WorkflowAdminServiceWorkflowException {

        BPSProfileBean[] bpsProfiles = stub.listBPSProfiles();
        if (bpsProfiles == null) {
            bpsProfiles = new BPSProfileBean[0];
        }
        return bpsProfiles;
    }

    public void deleteBPSProfile(String profileName) throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.removeBPSProfile(profileName);
    }

    public WorkflowBean[] listWorkflows() throws RemoteException, WorkflowAdminServiceWorkflowException {

        WorkflowBean[] workflows = stub.listWorkflows();
        if (workflows == null) {
            workflows = new WorkflowBean[0];
        }
        return workflows;
    }

    public void deleteWorkflow(String workflowId) throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.removeWorkflow(workflowId);
    }

    public AssociationDTO[] listAssociationsForWorkflow(String workflowId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        AssociationDTO[] associationsForWorkflow = stub.listAssociationsForWorkflow(workflowId);
        if (associationsForWorkflow == null) {
            associationsForWorkflow = new AssociationDTO[0];
        }
        return associationsForWorkflow;
    }

    public AssociationDTO[] listAllAssociations() throws RemoteException, WorkflowAdminServiceWorkflowException {

        AssociationDTO[] associations = stub.listAllAssociations();
        if (associations == null) {
            associations = new AssociationDTO[0];
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
