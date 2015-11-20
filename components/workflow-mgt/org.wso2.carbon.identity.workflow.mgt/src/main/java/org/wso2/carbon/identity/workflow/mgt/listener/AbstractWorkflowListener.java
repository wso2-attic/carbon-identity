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

package org.wso2.carbon.identity.workflow.mgt.listener;

import org.wso2.carbon.identity.workflow.mgt.bean.Entity;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.List;

public abstract class AbstractWorkflowListener implements WorkflowListener {

    /**
     * Trigger Before Listing Workflow Events
     *
     * @throws WorkflowException
     */
    @Override
    public void doPreListWorkflowEvents() {

    }

    /**
     * Trigger After Listing Workflow Events
     *
     * @throws WorkflowException
     */
    @Override
    public void doPostListWorkflowEvents() {

    }

    /**
     * Trigger before delete the request
     *
     * @param workflowRequest
     * @throws WorkflowException
     */
    @Override
    public void doPreDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {

    }

    /**
     * Trigger after deleting the request
     *
     * @param workflowRequest
     * @throws WorkflowException
     */
    @Override
    public void doPostDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException {

    }

    /**
     * Trigger before delete the workflow
     *
     * @param workflow
     * @throws WorkflowException
     */
    @Override
    public void doPreDeleteWorkflow(Workflow workflow) throws WorkflowException {

    }

    /**
     * Trigger after delete the workflow
     *
     * @param workflow
     * @throws WorkflowException
     */
    @Override
    public void doPostDeleteWorkflow(Workflow workflow) throws WorkflowException {

    }

    /**
     * Trigger before listing workflow Impls
     *
     * @param templateId
     * @throws WorkflowException
     */
    @Override
    public void doPreListWorkflowImpls(String templateId) throws WorkflowException {

    }

    /**
     * Trigger after listing workflow Impls
     *
     * @param templateId
     * @throws WorkflowException
     */
    @Override
    public void doPostListWorkflowImpls(String templateId) throws WorkflowException {

    }

    /**
     * Trigger before retrieving event
     *
     * @param id
     * @throws WorkflowException
     */
    @Override
    public void doPreGetEvent(String id) {

    }

    /**
     * Trigger after retrieving event
     *
     * @param id
     * @throws WorkflowException
     */
    @Override
    public void doPostGetEvent(String id) {

    }

    /**
     * Trigger before retrieving list of workflow templates
     *
     * @throws WorkflowException
     */
    @Override
    public void doPreListTemplates() throws WorkflowException {

    }

    /**
     * Trigger after retrieving list of workflow templates
     *
     * @throws WorkflowException
     */
    @Override
    public void doPostListTemplates() throws WorkflowException {

    }

    /**
     * Trigger before retrieving workflow template
     *
     * @param templateId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetTemplate(String templateId) throws WorkflowException {

    }

    /**
     * Trigger after retrieving workflow template
     *
     * @param templateId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetTemplate(String templateId) throws WorkflowException {

    }

    /**
     * Trigger before retrieving workflow impl
     *
     * @param templateId
     * @param workflowImplId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetWorkflowImpl(String templateId, String workflowImplId) throws WorkflowException {

    }

    /**
     * Trigger after retrieving workflow impl
     *
     * @param templateId
     * @param workflowImplId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetWorkflowImpl(String templateId, String workflowImplId) throws WorkflowException {

    }

    /**
     * Trigger before adding a workflow
     *
     * @param workflowDTO
     * @param parameterList
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPreAddWorkflow(Workflow workflowDTO, List<Parameter> parameterList, int tenantId) throws
            WorkflowException {

    }

    /**
     * Trigger after adding a workflow
     *
     * @param workflowDTO
     * @param parameterList
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPostAddWorkflow(Workflow workflowDTO, List<Parameter> parameterList, int tenantId) throws
            WorkflowException {

    }

    /**
     * Trigger before retrieving a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetWorkflow(String workflowId) throws WorkflowException {

    }

    /**
     * Trigger after retrieving a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetWorkflow(String workflowId) throws WorkflowException {

    }

    /**
     * Trigger before retrieving parameters of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetWorkflowParameters(String workflowId) throws WorkflowException {

    }

    /**
     * Trigger after retrieving parameters of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetWorkflowParameters(String workflowId) throws WorkflowException {

    }

    /**
     * Trigger before adding a association
     *
     * @param associationName
     * @param workflowId
     * @param eventId
     * @param condition
     * @throws WorkflowException
     */
    @Override
    public void doPreAddAssociation(String associationName, String workflowId, String eventId, String condition)
            throws WorkflowException {

    }

    /**
     * Trigger after adding a association
     *
     * @param associationName
     * @param workflowId
     * @param eventId
     * @param condition
     * @throws WorkflowException
     */
    @Override
    public void doPostAddAssociation(String associationName, String workflowId, String eventId, String condition)
            throws WorkflowException {

    }

    /**
     * Trigger before listing workflows of a tenant
     *
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPreListWorkflows(int tenantId) throws WorkflowException {

    }

    /**
     * Trigger after listing workflows of a tenant
     *
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPostListWorkflows(int tenantId) throws WorkflowException {

    }

    /**
     * Trigger before removing an association.
     *
     * @param associationId
     * @throws WorkflowException
     */
    @Override
    public void doPreRemoveAssociation(int associationId) throws WorkflowException {

    }

    /**
     * Trigger after removing an association.
     *
     * @param associationId
     * @throws WorkflowException
     */
    @Override
    public void doPostRemoveAssociation(int associationId) throws WorkflowException {

    }

    /**
     * Trigger before getting associations of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetAssociationsForWorkflow(String workflowId) throws WorkflowException {

    }

    /**
     * Trigger before getting associations of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetAssociationsForWorkflow(String workflowId) throws WorkflowException {

    }

    /**
     * Trigger before listing all associations
     *
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPreListAllAssociations(int tenantId) throws WorkflowException {

    }

    /**
     * Trigger after listing all associations
     *
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPostListAllAssociations(int tenantId) throws WorkflowException {

    }

    /**
     * Trigger before changing state of an association
     *
     * @param associationId
     * @param isEnable
     * @throws WorkflowException
     */
    @Override
    public void doPreChangeAssociationState(String associationId, boolean isEnable) throws WorkflowException {

    }

    /**
     * Trigger after changing state of an association
     *
     * @param associationId
     * @param isEnable
     * @throws WorkflowException
     */
    @Override
    public void doPostChangeAssociationState(String associationId, boolean isEnable) throws WorkflowException {

    }

    /**
     * @param requestId
     * @param entities
     * @throws WorkflowException
     */
    @Override
    public void doPreAddRequestEntityRelationships(String requestId, Entity[] entities) throws WorkflowException {

    }

    /**
     * @param requestId
     * @param entities
     * @throws WorkflowException
     */
    @Override
    public void doPostAddRequestEntityRelationships(String requestId, Entity[] entities) throws WorkflowException {

    }

    /**
     * @param entity
     * @throws WorkflowException
     */
    @Override
    public void doPreEntityHasPendingWorkflows(Entity entity) throws WorkflowException {

    }

    /**
     * @param entity
     * @throws WorkflowException
     */
    @Override
    public void doPostEntityHasPendingWorkflows(Entity entity) throws WorkflowException {

    }

    /**
     * @param entity
     * @param requestType
     * @throws WorkflowException
     */
    @Override
    public void doPreEntityHasPendingWorkflowsOfType(Entity entity, String requestType) throws WorkflowException {

    }

    /**
     * @param entity
     * @param requestType
     * @throws WorkflowException
     */
    @Override
    public void doPostEntityHasPendingWorkflowsOfType(Entity entity, String requestType) throws WorkflowException {

    }

    /**
     * @param entity1
     * @param entity2
     * @throws WorkflowException
     */
    @Override
    public void doPreAreTwoEntitiesRelated(Entity entity1, Entity entity2) throws WorkflowException {

    }

    /**
     * @param entity1
     * @param entity2
     * @throws WorkflowException
     */
    @Override
    public void doPostAreTwoEntitiesRelated(Entity entity1, Entity entity2) throws WorkflowException {

    }

    /**
     * @param eventType
     * @throws WorkflowException
     */
    @Override
    public void doPreIsEventAssociated(String eventType) throws WorkflowException {

    }

    /**
     * @param eventType
     * @throws WorkflowException
     */
    @Override
    public void doPostIsEventAssociated(String eventType) throws WorkflowException {

    }

    /**
     * @param user
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetRequestsCreatedByUser(String user, int tenantId) throws WorkflowException {

    }

    /**
     * @param user
     * @param tenantId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetRequestsCreatedByUser(String user, int tenantId) throws WorkflowException {

    }

    /**
     * @param requestId
     * @throws WorkflowException
     */
    @Override
    public void doPreGetWorkflowsOfRequest(String requestId) throws WorkflowException {

    }

    /**
     * @param requestId
     * @throws WorkflowException
     */
    @Override
    public void doPostGetWorkflowsOfRequest(String requestId) throws WorkflowException {

    }

    /**
     * @param user
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @param tenantId
     * @param status
     * @throws WorkflowException
     */
    @Override
    public void doPreGetRequestsFromFilter(String user, String beginDate, String endDate, String dateCategory, int
            tenantId, String status) throws WorkflowException {

    }

    /**
     * @param user
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @param tenantId
     * @param status
     * @throws WorkflowException
     */
    @Override
    public void doPostGetRequestsFromFilter(String user, String beginDate, String endDate, String dateCategory, int
            tenantId, String status) throws WorkflowException {

    }

    /**
     * @param wfOperationType
     * @param wfStatus
     * @param entityType
     * @param tenantID
     * @param idFilter
     * @throws WorkflowException
     */
    @Override
    public void doPreListEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID, String
            idFilter) throws WorkflowException {

    }

    /**
     * @param wfOperationType
     * @param wfStatus
     * @param entityType
     * @param tenantID
     * @param idFilter
     * @throws WorkflowException
     */
    @Override
    public void doPostListEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID,
                                      String idFilter) throws WorkflowException {

    }
}
