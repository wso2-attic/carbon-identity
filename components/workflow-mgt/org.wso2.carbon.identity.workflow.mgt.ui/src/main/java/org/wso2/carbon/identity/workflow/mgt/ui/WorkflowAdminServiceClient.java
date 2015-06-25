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
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceStub;
import org.wso2.carbon.identity.workflow.mgt.stub.WorkflowAdminServiceWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.AssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO;
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

        return stub.listWorkflowEvents();
    }

    public TemplateBean[] listTemplates() throws RemoteException {

        return stub.listWorkflowTemplates();
    }

    public TemplateDTO getTemplate(String templateName) throws RemoteException {

        return stub.getTemplateDTO(templateName);
    }

    public TemplateImplDTO getTemplateImpDTO(String template, String implName) throws RemoteException {

        return stub.getTemplateImplDTO(template, implName);
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

        return stub.listBPSProfiles();
    }

    public void deleteBPSProfile(String profileName) throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.removeBPSProfile(profileName);
    }

    public WorkflowBean[] listWorkflows() throws RemoteException, WorkflowAdminServiceWorkflowException {

        return stub.listWorkflows();
    }

    public void deleteWorkflow(String workflowId) throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.removeWorkflow(workflowId);
    }

    public AssociationDTO[] listAssociationsForWorkflow(String workflowId)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        return stub.listAssociationsForWorkflow(workflowId);
    }

    public AssociationDTO[] listAllAssociations() throws RemoteException, WorkflowAdminServiceWorkflowException {

        return stub.listAllAssociations();
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

}
