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


package org.wso2.carbon.identity.workflow.impl.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.databinding.types.NCName;
import org.apache.axis2.databinding.types.URI;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryInput;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultSet;
import org.wso2.carbon.humantask.stub.ui.task.client.api.HumanTaskClientAPIAdminStub;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalAccessFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalArgumentFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalOperationFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalStateFault;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;

import javax.xml.stream.XMLStreamException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class HumanTaskClientAPIAdminClient {

    private static final Log log = LogFactory.getLog(HumanTaskClientAPIAdminClient.class);

    private HumanTaskClientAPIAdminStub stub;

    public HumanTaskClientAPIAdminClient(String bpsURL, String username, char[] password) throws AxisFault {
        stub = new HumanTaskClientAPIAdminStub(bpsURL + WFImplConstant.HT_SERVICES_URL);
        ServiceClient serviceClient = stub._getServiceClient();
        Options options = serviceClient.getOptions();
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(username);
        auth.setPassword(new String(password));
        auth.setPreemptiveAuthentication(true);
        List<String> authSchemes = new ArrayList<>();
        authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
        auth.setAuthSchemes(authSchemes);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
        serviceClient.setOptions(options);
    }

    public HumanTaskClientAPIAdminClient(String bpsURL, String username) throws AxisFault {

        stub = new HumanTaskClientAPIAdminStub(bpsURL + WFImplConstant.HT_SERVICES_URL);
        ServiceClient serviceClient = stub._getServiceClient();
        OMElement mutualSSLHeader;
        try {
            String headerString = WFImplConstant.MUTUAL_SSL_HEADER.replaceAll("\\$username", username);
            mutualSSLHeader = AXIOMUtil.stringToOM(headerString);
            serviceClient.addHeader(mutualSSLHeader);
        } catch (XMLStreamException e) {
            throw new AxisFault("Error while creating mutualSSLHeader XML Element.", e);
        }
        Options options = serviceClient.getOptions();
        serviceClient.setOptions(options);
    }

    /**
     * Lists the tasks matching the provided simple query object.
     *
     * @param queryInput : The simple query object with the filtering criteria.
     * @return : The result set
     * @throws java.rmi.RemoteException :
     * @throws org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalArgumentFault
     *                                  :
     * @throws org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalStateFault
     *                                  :
     */
    public TTaskSimpleQueryResultSet simpleQuery(TSimpleQueryInput queryInput)
            throws RemoteException, IllegalArgumentFault, IllegalStateFault {
        try {
            return stub.simpleQuery(queryInput);
        } catch (RemoteException | IllegalStateFault | IllegalArgumentFault e) {
            log.error("Error occurred while performing taskListQuery operation", e);
            throw e;
        }
    }

    /**
     * Loads the task input.
     *
     * @param taskId : The id of the task/.
     * @return : The task input OMElement.
     * @throws RemoteException        :
     * @throws IllegalStateFault      :
     * @throws IllegalOperationFault:
     * @throws IllegalAccessFault:
     * @throws IllegalArgumentFault:
     * @throws javax.xml.stream.XMLStreamException
     * 
     */
    public OMElement getInput(URI taskId)
            throws RemoteException, IllegalStateFault, IllegalOperationFault, IllegalAccessFault,
            IllegalArgumentFault, XMLStreamException {
        String errMsg = "Error occurred while performing loadTaskInput operation";
        try {
            String input = (String) stub.getInput(taskId, new NCName(""));
            return AXIOMUtil.stringToOM(input);
        } catch (RemoteException | IllegalStateFault | IllegalOperationFault | IllegalArgumentFault | IllegalAccessFault | XMLStreamException e) {
            log.error(errMsg, e);
            throw e;
        }
    }

    /**
     * The skip operation.
     * @param taskId : The task id.
     * @throws IllegalArgumentFault :
     * @throws IllegalOperationFault  :
     * @throws IllegalAccessFault :
     * @throws IllegalStateFault :
     * @throws RemoteException :
     */
    public void skip(URI taskId)
            throws IllegalArgumentFault, IllegalOperationFault, IllegalAccessFault,
            IllegalStateFault, RemoteException {
        String errMsg = "Error occurred while performing skip operation";
        try {
            stub.skip(taskId);
        } catch (RemoteException | IllegalStateFault | IllegalOperationFault | IllegalArgumentFault | IllegalAccessFault e) {
            log.error(errMsg, e);
            throw e;
        }
    }


}
