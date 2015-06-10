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
import org.wso2.carbon.identity.workflow.mgt.stub.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.EventBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateBean;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDeploymentDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WorkflowBean;

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

    public EventBean[] listWorkflowEvents() throws RemoteException {

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

    public void deployTemplate(TemplateDeploymentDTO deploymentDTO)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

        stub.deployTemplate(deploymentDTO);
    }

    public void addBPSProfile(String profileName, String host, String user, String password)
            throws RemoteException, WorkflowAdminServiceWorkflowException {

//        String[] splittedPw = password.split("(?!^)");
        stub.addBPSProfile(profileName, host, user, password);
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
}
