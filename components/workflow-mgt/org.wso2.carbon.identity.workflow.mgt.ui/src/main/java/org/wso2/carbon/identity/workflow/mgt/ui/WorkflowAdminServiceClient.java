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
import org.wso2.carbon.identity.workflow.mgt.stub.bean.ServiceAssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateDeploymentDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.TemplateImplDTO;
import org.wso2.carbon.identity.workflow.mgt.stub.bean.WSServiceBean;
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

    public void addExistingService(String alias, String serviceEP, String wsAction, String username, String password)
            throws RemoteException, WorkflowAdminServiceWorkflowException {
        WSServiceBean serviceBean = new WSServiceBean();
        serviceBean.setAlias(alias);
        serviceBean.setServiceEndpoint(serviceEP);
        serviceBean.setWsAction(wsAction);
        serviceBean.setUserName(username);
        serviceBean.setPassword(password);
        stub.addWSService(serviceBean);
    }

    public void associateServiceToEvent(String serviceAlias, String eventType, String condition, int priority)
            throws RemoteException, WorkflowAdminServiceWorkflowException {
        stub.associateWSServiceToEvent(serviceAlias, eventType, condition, priority);
    }

    public WorkflowEventDTO[] listWorkflowEvents() throws RemoteException {
        return stub.listWorkflowEvents();
    }

    public ServiceAssociationDTO[] listServices() throws RemoteException, WorkflowAdminServiceWorkflowException {
        return stub.listWSServices();
    }

    public void removeService(String alias, String event) throws RemoteException,
            WorkflowAdminServiceWorkflowException {
        stub.removeWSService(alias, event);
    }

    public String[] listTemplates() throws RemoteException {
        return stub.listWorkflowTemplates();
    }

    public TemplateDTO getTemplate(String templateName) throws RemoteException {
        return stub.getTemplateDTO(templateName);
    }

    public TemplateImplDTO getTemplateImpDTO(String template, String implName) throws RemoteException {
        return stub.getTemplateImplDTO(template, implName);
    }

    public void deployTemplate(TemplateDeploymentDTO deploymentDTO) throws RemoteException {
        stub.deployTemplate(deploymentDTO);
    }

    public void addBPSProfile(String profileName, String host, String user, String password) throws RemoteException {
        String[] splittedPw = password.split("(?!^)");
        stub.addBPSProfile(profileName, host, user, splittedPw);
    }

    public BPSProfileBean[] listBPSProfiles() throws RemoteException {
        return stub.getBPSProfiles();
    }

    public void deleteBPSProfile(String profileName) throws RemoteException {
        stub.deleteBPSProfile(profileName);
    }
}
