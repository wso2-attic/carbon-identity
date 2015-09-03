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

package org.wso2.carbon.identity.workflow.mgt.template.impl;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.template.WorkFlowExecutor;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultBPELExecutor implements WorkFlowExecutor {

    private static final String EXECUTOR_NAME = "DefaultBPELExecutor";
    private static final Set<String> REQUIRED_PARAMS;

    static {
        REQUIRED_PARAMS = new HashSet<>();
        REQUIRED_PARAMS.add(WorkFlowConstants.TemplateConstants.HOST);
        REQUIRED_PARAMS.add(WorkFlowConstants.TemplateConstants.WORKFLOW_NAME);
        REQUIRED_PARAMS.add(WorkFlowConstants.TemplateConstants.SERVICE_ACTION);
        REQUIRED_PARAMS.add(WorkFlowConstants.TemplateConstants.AUTH_USER);
        REQUIRED_PARAMS.add(WorkFlowConstants.TemplateConstants.AUTH_USER_PASSWORD);
    }

    private Map<String, Object> initParams;

    @Override
    public boolean canHandle(WorkFlowRequest workFlowRequest) {
        //Since this is handled by manager level
        return true;
    }

    @Override
    public void initialize(Map<String, Object> params) {

        this.initParams = params;
    }

    @Override
    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException {

        validateExecutionParams();
        OMElement requestBody = WorkflowRequestBuilder.buildXMLRequest(workFlowRequest, this.initParams);
        try {
            callService(requestBody);
        } catch (AxisFault axisFault) {
            throw new InternalWorkflowException("Error invoking service for request: " +
                    workFlowRequest.getUuid(), axisFault);
        }
    }

    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }


    private void validateExecutionParams() throws InternalWorkflowException {
        //Since missing a parameter should lead to a exception scenario here, throwing a exception with what is
        // missing and returning void for successful cases.
        if (initParams == null) {
            throw new InternalWorkflowException("Init params for the DefaultBPELExecutor is null.");
        }
        for (String requiredParam : REQUIRED_PARAMS) {
            if (!initParams.containsKey(requiredParam) || initParams.get(requiredParam) == null) {
                throw new InternalWorkflowException("Init params doesn't contain the required parameter " +
                        ":" + requiredParam);
            }
        }
    }

    private void callService(OMElement messagePayload) throws AxisFault {

        ServiceClient client = new ServiceClient(WorkflowServiceDataHolder.getInstance()
                .getConfigurationContextService().getClientConfigContext(), null);
        Options options = new Options();
        options.setAction((String) initParams.get(WorkFlowConstants.TemplateConstants.SERVICE_ACTION));
        String endpoint;
        String host = (String) initParams.get(WorkFlowConstants.TemplateConstants.HOST);
        String serviceName = (String) initParams.get(WorkFlowConstants.TemplateConstants.WORKFLOW_NAME);
        if (host.endsWith("/")) {
            endpoint = host + "services/" + serviceName + WorkFlowConstants.TemplateConstants.SERVICE_SUFFIX;
        } else {
            endpoint = host + "/services/" + serviceName + WorkFlowConstants.TemplateConstants.SERVICE_SUFFIX;
        }

        options.setTo(new EndpointReference(endpoint));

        options.setProperty(Constants.Configuration.MESSAGE_TYPE, SOAP11Constants.SOAP_11_CONTENT_TYPE);

        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername((String) initParams.get(WorkFlowConstants.TemplateConstants.AUTH_USER));
        auth.setPassword((String) initParams.get(WorkFlowConstants.TemplateConstants.AUTH_USER_PASSWORD));
        auth.setPreemptiveAuthentication(true);
        List<String> authSchemes = new ArrayList<>();
        authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
        auth.setAuthSchemes(authSchemes);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);

        options.setManageSession(true);

        client.setOptions(options);
        client.fireAndForget(messagePayload);

    }

}
