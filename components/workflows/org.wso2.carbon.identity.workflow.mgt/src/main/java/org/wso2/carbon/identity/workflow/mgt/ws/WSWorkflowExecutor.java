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

package org.wso2.carbon.identity.workflow.mgt.ws;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.WorkFlowExecutor;
import org.wso2.carbon.identity.workflow.mgt.WorkflowDataType;
import org.wso2.carbon.identity.workflow.mgt.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.bean.WSServiceAssociation;
import org.wso2.carbon.identity.workflow.mgt.bean.WSServiceBean;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowParameter;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowServicesDAO;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowMgtServiceComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class WSWorkflowExecutor implements WorkFlowExecutor {

    private static final String EXECUTOR_NAME = "WS_Executor";
    private static Log log = LogFactory.getLog(WSWorkflowExecutor.class);

    @Override
    public boolean canHandle(WorkFlowRequest workFlowRequest) {
        try {
            OMElement requestBody = buildWSRequest(workFlowRequest); //todo:cache for later use?
            WorkflowServicesDAO servicesDAO = new WorkflowServicesDAO();
            List<WSServiceAssociation> servicesForEvent = servicesDAO.getSubscribedServicesForEvent(workFlowRequest
                    .getEventType());
            for (WSServiceAssociation association : servicesForEvent) {
                try {
                    AXIOMXPath axiomxPath = new AXIOMXPath(association.getCondition());
                    if (axiomxPath.booleanValueOf(requestBody)) {
                        return true;
                    }
                } catch (JaxenException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Error when executing the xpath expression:" + association.getCondition() + " , on " +
                                requestBody, e);
                    }
                }
            }
        } catch (WorkflowException e) {
            log.error("Unable to build the WS request for the request.", e);
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void execute(WorkFlowRequest workFlowRequest) throws WorkflowException {
        OMElement requestBody = buildWSRequest(workFlowRequest); //todo:cache from canHandle()?
        WorkflowServicesDAO servicesDAO = new WorkflowServicesDAO();
        List<WSServiceAssociation> servicesForEvent = servicesDAO.getSubscribedServicesForEvent(workFlowRequest
                .getEventType());

        Collections.sort(servicesForEvent, new Comparator<WSServiceAssociation>() {
            @Override
            public int compare(WSServiceAssociation o1, WSServiceAssociation o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });

        for (WSServiceAssociation association : servicesForEvent) {
            try {
                AXIOMXPath axiomxPath = new AXIOMXPath(association.getCondition());
                if (axiomxPath.booleanValueOf(requestBody)) {
                    callService(requestBody, association.getService());
                    return;
                }
            } catch (JaxenException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error when executing the xpath expression:" + association.getCondition() + " , on " +
                            requestBody, e);
                }
            }
        }
    }

    @Override
    public String getName() {
        return EXECUTOR_NAME;
    }

    private void callService(OMElement messagePayload, WSServiceBean service) throws WorkflowException {
        try {
            ServiceClient client = new ServiceClient(WorkflowMgtServiceComponent.getConfigurationContextService()
                    .getClientConfigContext(), null);
            Options options = new Options();
            options.setAction(service.getWsAction());
            options.setTo(new EndpointReference(service.getServiceEndpoint()));
            options.setProperty(Constants.Configuration.MESSAGE_TYPE, HTTPConstants.MEDIA_TYPE_APPLICATION_XML);

            HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
            auth.setUsername(service.getUserName());
            auth.setPassword(service.getPassword());
            auth.setPreemptiveAuthentication(true);
            List<String> authSchemes = new ArrayList<String>();
            authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
            auth.setAuthSchemes(authSchemes);
            options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);

            options.setManageSession(true);
            client.setOptions(options);
            client.fireAndForget(messagePayload);
        } catch (AxisFault e) {
            throw new WorkflowException("Error invoking service:" + service.getAlias(), e);
        }
    }

    protected OMElement buildWSRequest(WorkFlowRequest workFlowRequest) throws WorkflowException {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        WorkflowRequestBuilder requestBuilder = new WorkflowRequestBuilder(workFlowRequest.getUuid(),
                workFlowRequest.getEventType(), tenantDomain);

        for (WorkflowParameter parameter : workFlowRequest.getWorkflowParameters()) {
            if (parameter.isRequiredInWorkflow()) {
                switch (parameter.getValueType()) {
                    case WorkflowDataType.BOOLEAN_TYPE:
                    case WorkflowDataType.STRING_TYPE:
                    case WorkflowDataType.INTEGER_TYPE:
                    case WorkflowDataType.DOUBLE_TYPE:
                        requestBuilder.addSingleValuedParam(parameter.getName(), parameter.getValue());
                        break;
                    case WorkflowDataType.STRING_LIST_TYPE:
                    case WorkflowDataType.DOUBLE_LIST_TYPE:
                    case WorkflowDataType.INTEGER_LIST_TYPE:
                    case WorkflowDataType.BOOLEAN_LIST_TYPE:
                        requestBuilder.addListTypeParam(parameter.getName(), (List<Object>) parameter.getValue());
                        break;
                    case WorkflowDataType.STRING_STRING_MAP_TYPE:
                        requestBuilder.addMapTypeParam(parameter.getName(), (Map<String, Object>) parameter.getValue());
                        break;
                    //ignoring the other types
                }
            }
        }
        return requestBuilder.buildRequest();
    }

    @Override
    public String toString() {
        return "WSWorkflowExecutor{priority:" + getPriority() + "}";
    }
}
