/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.workflow.mgt.ws;

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.workflow.mgt.WorkFlowConstants;
import org.wso2.carbon.workflow.mgt.WorkFlowExecutor;
import org.wso2.carbon.workflow.mgt.WorkflowException;
import org.wso2.carbon.workflow.mgt.bean.WSServiceBean;
import org.wso2.carbon.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.workflow.mgt.bean.WorkflowParameter;
import org.wso2.carbon.workflow.mgt.dao.WorkflowServicesDAO;
import org.wso2.carbon.workflow.mgt.internal.WorkflowMgtServiceComponent;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WSWorkflowExecutor implements WorkFlowExecutor {


    @Override
    public boolean canHandle(WorkFlowRequest workFlowRequest) {
        try {
            WorkflowServicesDAO servicesDAO = new WorkflowServicesDAO();
            Map<WSServiceBean, String> servicesForRequester = servicesDAO.getEnabledServicesForRequester(workFlowRequest
                    .getRequesterId());
            for (Map.Entry<WSServiceBean, String> entry : servicesForRequester.entrySet()) {
                //todo cache?
                String request = buildWSRequest(workFlowRequest);
                //todo match condition
                //todo IF matches
                return true;
            }
        } catch (WorkflowException e) {
            //todo:
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException {
        String requestBody = buildWSRequest(workFlowRequest); //todo:cache from canHandle()?
        WorkflowServicesDAO servicesDAO = new WorkflowServicesDAO();
        Map<WSServiceBean, String> servicesForRequester = servicesDAO.getEnabledServicesForRequester(workFlowRequest
                .getRequesterId());
        for (Map.Entry<WSServiceBean, String> entry : servicesForRequester.entrySet()) {
            //todo check the request with condition
            //todo IF matches
            callService(requestBody, entry.getKey());
            return;
        }
    }

    private String buildWSRequest(WorkFlowRequest workFlowRequest) throws WorkflowException {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        WorkflowRequestBuilder requestBuilder = new WorkflowRequestBuilder(workFlowRequest.getUuid(),
                workFlowRequest.getRequesterId(), tenantDomain);
        for (WorkflowParameter parameter : workFlowRequest.getWorkflowParameters()) {
            if(parameter.isRequiredInWorkflow()){
                if(WorkFlowConstants.WF_PARAM_TYPE_PRIMITIVE.equals(parameter.getValueType()) || WorkFlowConstants
                        .WF_PARAM_TYPE_STRING.equals(parameter.getValueType())){
                    requestBuilder.addSingleValuedParam(parameter.getName(),parameter.getValue());
                } else if(WorkFlowConstants.WF_PARAM_TYPE_BASIC_LIST.equals(parameter.getValueType())){
                    requestBuilder.addListTypeParam(parameter.getName(), (java.util.List<Object>) parameter.getValue());
                } else if(WorkFlowConstants.WF_PARAM_TYPE_BASIC_MAP.equals(parameter.getValueType())){
                    requestBuilder.addMapTypeParam(parameter.getName(),
                            (java.util.Map<String, Object>) parameter.getValue());
                }
            }
        }
        return requestBuilder.buildRequest();
    }

    private void callService(String messagePayload, WSServiceBean service) throws WorkflowException {
        try {
            ServiceClient client = new ServiceClient(WorkflowMgtServiceComponent.getConfigurationContextService()
                    .getClientConfigContext(),null);
            Options options = new Options();
            options.setAction(service.getAction());
            options.setTo(new EndpointReference(service.getServiceEndpoint())); //todo
            options.setProperty(Constants.Configuration.MESSAGE_TYPE,
                    HTTPConstants.MEDIA_TYPE_APPLICATION_XML);

            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setUsername(service.getUserName());
            auth.setPassword(new String(service.getPassword()));
            auth.setPreemptiveAuthentication(true);
            List<String> authSchemes = new ArrayList<String>();
            authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
            auth.setAuthSchemes(authSchemes);
            options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
            options.setManageSession(true);
            client.setOptions(options);
            client.fireAndForget(AXIOMUtil.stringToOM(messagePayload));
        } catch (AxisFault e) {
            throw new WorkflowException("Error invoking service:"+service.getAlias(), e);
        } catch (XMLStreamException e) {
            throw new WorkflowException("Error with message payload: "+messagePayload, e);
        }
    }
}
