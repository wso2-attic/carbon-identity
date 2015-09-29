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
package org.wso2.carbon.identity.workflow.impl.listener;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.databinding.types.NCName;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryCategory;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryInput;
import org.wso2.carbon.humantask.stub.types.TStatus;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultRow;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultSet;
import org.wso2.carbon.humantask.stub.ui.task.client.api.HumanTaskClientAPIAdminStub;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalAccessFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalArgumentFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalOperationFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalStateFault;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.dao.BPSProfileDAO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.listener.WorkflowRequestDeleteListener;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;


public class WorkflowRequestDeleteListenerImpl implements WorkflowRequestDeleteListener{
    @Override
    public void doPreDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {

    }

    @Override
    public void doPostDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {
        deleteHumanTasks(workflowRequest);
    }

    /*
     * Delete human tasks associated with a given request
     *
     * @param requestId request id of the request to delete human tasks of
     * @throws WorkflowException
     */
    private void deleteHumanTasks(WorkflowRequest workflowRequest) throws WorkflowException {

        BPSProfileDAO bpsProfileDAO = new BPSProfileDAO();

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            List<BPSProfile> bpsProfiles = bpsProfileDAO.listBPSProfiles(tenantId);
            HumanTaskClientAPIAdminStub stub = null;
            TSimpleQueryInput input = new TSimpleQueryInput();
            TStatus reservedState = new TStatus();
            reservedState.setTStatus(WFImplConstant.HT_STATE_RESERVED);
            input.addStatus(reservedState);
            TStatus readyState = new TStatus();
            readyState.setTStatus(WFImplConstant.HT_STATE_READY);
            input.addStatus(readyState);
            input.setPageSize(100000);
            input.setPageNumber(0);
            input.setSimpleQueryCategory(TSimpleQueryCategory.ALL_TASKS);
            for (int i = 0; i < bpsProfiles.size(); i++) {
                String host = bpsProfiles.get(i).getWorkerHostURL();
                URL servicesUrl = new URL(new URL(host), WFImplConstant.HT_SERVICES_URL);
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
                        checkMatchingTaskAndDelete(workflowRequest.getRequestId(), stub, arr, j, child);
                    }

                }
            }
        } catch (MalformedURLException | XMLStreamException | IllegalOperationFault | IllegalAccessFault |
                RemoteException | IllegalStateFault | IllegalArgumentFault e) {
            throw new WorkflowException("Error while deleting the human tasks of the request.");
        }

    }




    /*
     *
     *
     * @param requestId Id of the deleting request
     * @param stub stub to call HumanTaskClientAPIAdmin
     * @param resultsList task list in the current human task engine
     * @param resultIndex index of the currently considering rask
     * @param taskElement currently considering task
     * @throws RemoteException
     */

    private void checkMatchingTaskAndDelete(String requestId, HumanTaskClientAPIAdminStub stub,
                                            TTaskSimpleQueryResultRow[] resultsList, int resultIndex, OMElementImpl
                                                    taskElement) throws RemoteException, IllegalStateFault,
            IllegalOperationFault, IllegalArgumentFault, IllegalAccessFault {
        if (taskElement.getLocalName().equals(WFImplConstant.HT_PARAMETER_LIST_ELEMENT)) {
            Iterator<OMElementImpl> parameters = taskElement.getChildElements();
            while (parameters.hasNext()) {
                OMElementImpl parameter = parameters.next();
                Iterator<OMAttribute> attributes = parameter.getAllAttributes();
                while (attributes.hasNext()) {
                    OMAttribute currentAttribute = attributes.next();
                    if (currentAttribute.getLocalName().equals(WFImplConstant.HT_ITEM_NAME_ATTRIBUTE) && currentAttribute
                            .getAttributeValue().equals(WFImplConstant.HT_REQUEST_ID_ATTRIBUTE_VALUE)) {
                        Iterator<OMElementImpl> itemValues = parameter.getChildElements();
                        if (itemValues.hasNext()) {
                            String taskRequestId = itemValues.next().getText();
                            if (taskRequestId.contains(",")) {
                                taskRequestId = taskRequestId.replaceAll(",", "");
                            }
                            if (taskRequestId.equals(requestId)) {
                                stub.skip(resultsList[resultIndex].getId());
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
