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

package org.wso2.carbon.identity.workflow.impl;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.impl.util.WorkflowRequestBuilder;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.workflow.WorkFlowExecutor;

import java.util.ArrayList;
import java.util.List;

public class RequestExecutor implements WorkFlowExecutor {

    private static Log log = LogFactory.getLog(RequestExecutor.class);
    private static final String EXECUTOR_NAME = "BPELExecutor";
    /*private static final Set<String> REQUIRED_PARAMS;

    static {
        REQUIRED_PARAMS = new HashSet<>();
        REQUIRED_PARAMS.add(WFConstant.TemplateConstants.HOST);
        REQUIRED_PARAMS.add(WFConstant.TemplateConstants.WORKFLOW_NAME);
        REQUIRED_PARAMS.add(WFConstant.TemplateConstants.SERVICE_ACTION);
        REQUIRED_PARAMS.add(WFConstant.TemplateConstants.AUTH_USER);
        REQUIRED_PARAMS.add(WFConstant.TemplateConstants.AUTH_USER_PASSWORD);
    }*/

    private List<Parameter> parameterList;
    private BPSProfile bpsProfile ;

    @Override
    public boolean canHandle(WorkflowRequest workFlowRequest) {
        //Since this is handled by manager level
        return true;
    }

    @Override
    public void initialize(List<Parameter> parameterList) throws WorkflowException {

        this.parameterList = parameterList;

        Parameter bpsProfileParameter = Parameter
                .getParameter(parameterList, WFImplConstant.ParameterName.BPS_PROFILE, WFConstant.ParameterHolder
                        .WORKFLOW_IMPL);
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        if(bpsProfileParameter != null) {
            String bpsProfileName = bpsProfileParameter.getParamValue();
            try {
                bpsProfile = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().getBPSProfile
                        (bpsProfileName, tenantId);
            } catch (WorkflowImplException e) {
                String errorMsg = "Error occurred while reading bps profile, " + e.getMessage();
                log.error(errorMsg);
                throw new WorkflowException(errorMsg,e);
            }
        }
    }

    @Override
    public void execute(WorkflowRequest workFlowRequest) throws WorkflowException {

        validateExecutionParams();
        OMElement requestBody = WorkflowRequestBuilder.buildXMLRequest(workFlowRequest, this.parameterList);
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
        if (parameterList == null) {
            throw new InternalWorkflowException("Init params for the BPELExecutor is null.");
        }
        /*for (String requiredParam : REQUIRED_PARAMS) {
            if (!initParams.containsKey(requiredParam) || initParams.get(requiredParam) == null) {
                throw new InternalWorkflowException("Init params doesn't contain the required parameter " +
                                                    ":" + requiredParam);
            }
        }*/
    }

    private void callService(OMElement messagePayload) throws AxisFault {

        ServiceClient client = new ServiceClient(WorkflowImplServiceDataHolder.getInstance()
                                                         .getConfigurationContextService()
                                                         .getClientConfigContext(), null);
        Options options = new Options();
        options.setAction(WFImplConstant.DEFAULT_APPROVAL_BPEL_SOAP_ACTION);

        String host = bpsProfile.getHost();
        String serviceName = Parameter.getParameter(parameterList,WFConstant.ParameterName.WORKFLOW_NAME,WFConstant.ParameterHolder.WORKFLOW_IMPL).getParamValue();

        String endpoint;
        if (host.endsWith("/")) {
            endpoint = host + "services/" + serviceName + WFConstant.TemplateConstants.SERVICE_SUFFIX;
        } else {
            endpoint = host + "/services/" + serviceName + WFConstant.TemplateConstants.SERVICE_SUFFIX;
        }

        options.setTo(new EndpointReference(endpoint));

        options.setProperty(org.apache.axis2.Constants.Configuration.MESSAGE_TYPE, SOAP11Constants.SOAP_11_CONTENT_TYPE);

        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(bpsProfile.getUsername());
        auth.setPassword(bpsProfile.getPassword());
        auth.setPreemptiveAuthentication(true);
        List<String> authSchemes = new ArrayList<>();
        authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
        auth.setAuthSchemes(authSchemes);
        options.setProperty(HTTPConstants.AUTHENTICATE, auth);

        options.setManageSession(true);

        client.setOptions(options);
        client.fireAndForget(messagePayload);

    }

}
