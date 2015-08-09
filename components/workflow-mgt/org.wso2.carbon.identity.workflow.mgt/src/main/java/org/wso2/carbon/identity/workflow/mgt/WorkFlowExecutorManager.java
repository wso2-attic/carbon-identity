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

package org.wso2.carbon.identity.workflow.mgt;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.workflow.mgt.dao.RequestEntityRelationshipDAO;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.extension.WorkflowRequestHandler;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkFlowRequest;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociationBean;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowDAO;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestBuilder;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowRequestStatus;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkFlowExecutorManager {

    private static WorkFlowExecutorManager instance = new WorkFlowExecutorManager();

    private static Log log = LogFactory.getLog(WorkFlowExecutorManager.class);

    private WorkFlowExecutorManager() {

    }

    public static WorkFlowExecutorManager getInstance() {

        return instance;
    }

    public void executeWorkflow(WorkFlowRequest workFlowRequest) throws WorkflowException {

        if (StringUtils.isBlank(workFlowRequest.getUuid())) {
            workFlowRequest.setUuid(UUID.randomUUID().toString());
        }
        OMElement xmlRequest = WorkflowRequestBuilder.buildXMLRequest(workFlowRequest);
        WorkflowDAO workflowDAO = new WorkflowDAO();
        List<WorkflowAssociationBean> associations =
                workflowDAO.getWorkflowsForRequest(workFlowRequest.getEventType(), workFlowRequest.getTenantId());
        if (CollectionUtils.isEmpty(associations)) {
            handleCallback(workFlowRequest, WorkflowRequestStatus.SKIPPED.toString(), null);
            return;
        }
        boolean workflowEngaged = false;
        boolean requestSaved = false;
        for (WorkflowAssociationBean association : associations) {
            try {
                AXIOMXPath axiomxPath = new AXIOMXPath(association.getCondition());
                if (axiomxPath.booleanValueOf(xmlRequest)) {
                    workflowEngaged = true;
                    if (!requestSaved) {
                        WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
                        String tenant = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        String currentUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
                        String fullyQualifiedName = UserCoreUtil.addTenantDomainToEntry(currentUser, tenant);
                        requestDAO.addWorkflowEntry(workFlowRequest, fullyQualifiedName);
                        requestSaved = true;
                    }
                    AbstractWorkflowTemplateImpl templateImplementation = WorkflowServiceDataHolder.getInstance()
                            .getTemplateImplementation(association.getTemplateId(), association.getImplId());
                    Map<String, Object> workflowParams = workflowDAO.getWorkflowParams(association.getWorkflowId());
                    templateImplementation.initializeExecutor(workflowParams);
                    templateImplementation.execute(workFlowRequest);
                }
            } catch (JaxenException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error when executing the xpath expression:" + association.getCondition() + " , on " +
                            xmlRequest, e);
                }
            }
        }

        if (!workflowEngaged) {
            handleCallback(workFlowRequest, WorkflowRequestStatus.SKIPPED.toString(), null);
        }
    }

    private void handleCallback(WorkFlowRequest request, String status, Map<String, Object> additionalParams)
            throws WorkflowException {

        if (request != null) {
            String eventId = request.getEventType();
            WorkflowRequestHandler requestHandler = WorkflowServiceDataHolder.getInstance().getRequestHandler(eventId);
            if (requestHandler == null) {
                throw new InternalWorkflowException("No request handlers registered for the id: " + eventId);
            }
            String request_id = request.getUuid();
            if (request.getTenantId() == MultitenantConstants.INVALID_TENANT_ID) {
                throw new InternalWorkflowException(
                        "Invalid tenant id for request " + eventId + " with id" + request_id);
            }
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                    .getThreadLocalCarbonContext();
            try {
                String tenantDomain = WorkflowServiceDataHolder.getInstance().getRealmService().getTenantManager()
                        .getDomain(request.getTenantId());
                carbonContext.setTenantId(request.getTenantId());
                carbonContext.setTenantDomain(tenantDomain);
                requestHandler.onWorkflowCompletion(status, request, additionalParams);
                RequestEntityRelationshipDAO requestEntityRelationshipDAO = new RequestEntityRelationshipDAO();
                if (WorkflowRequestStatus.APPROVED.toString().equals(status) || WorkflowRequestStatus.REJECTED
                        .toString().equals(status)) {
                    requestEntityRelationshipDAO.deleteRelationshipsOfRequest(request_id);
                }
            } catch (WorkflowException e) {
                throw e;
            } catch (UserStoreException e) {
                throw new InternalWorkflowException("Error when getting tenant domain for tenant id " + request
                        .getTenantId());
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    public void handleCallback(String uuid, String status, Map<String, Object> additionalParams)
            throws WorkflowException {

        WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
        WorkFlowRequest request = requestDAO.retrieveWorkflow(uuid);
        handleCallback(request, status, additionalParams);
    }
}
