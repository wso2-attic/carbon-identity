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
import org.wso2.carbon.identity.workflow.impl.WorkflowImplService;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.dao.BPSProfileDAO;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.listener.WorkflowListener;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;


public class WorkflowListenerImpl implements WorkflowListener {
    @Override
    public void doPreDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {
        WorkflowImplService workflowImplService = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();
        workflowImplService.deleteHumanTask(workflowRequest);
    }

    @Override
    public void doPostDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {

    }

    @Override
    public void doPreDeleteWorkflow(Workflow workflow) throws WorkflowException {
        WorkflowImplService workflowImplService = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();
        if (workflowImplService == null) {
            throw new WorkflowException("Error when deleting the Workflow " + workflow.getWorkflowName());
        }
        workflowImplService.removeBPSPackage(workflow);
    }

    @Override
    public void doPostDeleteWorkflow(Workflow workflow) throws WorkflowException {

    }


}
