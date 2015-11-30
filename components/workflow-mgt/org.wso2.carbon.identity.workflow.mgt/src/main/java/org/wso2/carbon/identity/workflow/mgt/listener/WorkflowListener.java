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
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociation;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.Template;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowEvent;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.List;

/**
 * Listener for Workflow Request Delete Process
 */
public interface WorkflowListener {

    /**
     * Trigger Before Listing Workflow Events
     *
     * @throws WorkflowException
     */
    void doPreListWorkflowEvents();

    /**
     * Trigger After Listing Workflow Events
     *
     * @throws WorkflowException
     */
    void doPostListWorkflowEvents(List<WorkflowEvent> result);

    /**
     * Trigger before delete the request
     *
     * @param workflowRequest
     * @throws WorkflowException
     */
    void doPreDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException;

    /**
     * Trigger after deleting the request
     *
     * @param workflowRequest
     * @throws WorkflowException
     */
    void doPostDeleteWorkflowRequest(WorkflowRequest workflowRequest) throws WorkflowException;

    /**
     * Trigger before delete the workflow
     *
     * @param workflow
     * @throws WorkflowException
     */
    void doPreDeleteWorkflow(Workflow workflow) throws WorkflowException;

    /**
     * Trigger after delete the workflow
     *
     * @param workflow
     * @throws WorkflowException
     */
    void doPostDeleteWorkflow(Workflow workflow) throws WorkflowException;

    /**
     * Trigger before listing workflow Impls
     *
     * @param templateId
     * @throws WorkflowException
     */
    void doPreListWorkflowImpls(String templateId) throws WorkflowException;

    /**
     * Trigger after listing workflow Impls
     *
     * @param templateId
     * @throws WorkflowException
     */
    void doPostListWorkflowImpls(String templateId, List<WorkflowImpl> result) throws WorkflowException;

    /**
     * Trigger before retrieving event
     *
     * @param id
     * @throws WorkflowException
     */
    void doPreGetEvent(String id);

    /**
     * Trigger after retrieving event
     *
     * @param id
     * @throws WorkflowException
     */
    void doPostGetEvent(String id, WorkflowEvent result);

    /**
     * Trigger before retrieving list of workflow templates
     *
     * @throws WorkflowException
     */
    void doPreListTemplates() throws WorkflowException;

    /**
     * Trigger after retrieving list of workflow templates
     *
     * @throws WorkflowException
     */
    void doPostListTemplates(List<Template> result) throws WorkflowException;

    /**
     * Trigger before retrieving workflow template
     *
     * @param templateId
     * @throws WorkflowException
     */
    void doPreGetTemplate(String templateId) throws WorkflowException;

    /**
     * Trigger after retrieving workflow template
     *
     * @param templateId
     * @throws WorkflowException
     */
    void doPostGetTemplate(String templateId, Template result) throws WorkflowException;

    /**
     * Trigger before retrieving workflow impl
     *
     * @param templateId
     * @param workflowImplId
     * @throws WorkflowException
     */
    void doPreGetWorkflowImpl(String templateId, String workflowImplId) throws WorkflowException;

    /**
     * Trigger after retrieving workflow impl
     *
     * @param templateId
     * @param workflowImplId
     * @throws WorkflowException
     */
    void doPostGetWorkflowImpl(String templateId, String workflowImplId, WorkflowImpl result) throws WorkflowException;

    /**
     * Trigger before adding a workflow
     *
     * @param workflowDTO
     * @param parameterList
     * @param tenantId
     * @throws WorkflowException
     */
    void doPreAddWorkflow(Workflow workflowDTO,
                          List<Parameter> parameterList, int tenantId) throws WorkflowException;

    /**
     * Trigger after adding a workflow
     *
     * @param workflowDTO
     * @param parameterList
     * @param tenantId
     * @throws WorkflowException
     */
    void doPostAddWorkflow(Workflow workflowDTO,
                           List<Parameter> parameterList, int tenantId) throws WorkflowException;

    /**
     * Trigger before retrieving a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    void doPreGetWorkflow(String workflowId) throws WorkflowException;

    /**
     * Trigger after retrieving a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    void doPostGetWorkflow(String workflowId, Workflow workflow) throws WorkflowException;

    /**
     * Trigger before retrieving parameters of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    void doPreGetWorkflowParameters(String workflowId) throws WorkflowException;

    /**
     * Trigger after retrieving parameters of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    void doPostGetWorkflowParameters(String workflowId, List<Parameter> result) throws WorkflowException;

    /**
     * Trigger before adding a association
     *
     * @param associationName
     * @param workflowId
     * @param eventId
     * @param condition
     * @throws WorkflowException
     */
    void doPreAddAssociation(String associationName, String workflowId, String eventId, String condition) throws
            WorkflowException;

    /**
     * Trigger after adding a association
     *
     * @param associationName
     * @param workflowId
     * @param eventId
     * @param condition
     * @throws WorkflowException
     */
    void doPostAddAssociation(String associationName, String workflowId, String eventId, String condition) throws
            WorkflowException;

    /**
     * Trigger before listing workflows of a tenant
     *
     * @param tenantId
     * @throws WorkflowException
     */
    void doPreListWorkflows(int tenantId) throws WorkflowException;

    /**
     * Trigger after listing workflows of a tenant
     *
     * @param tenantId
     * @throws WorkflowException
     */
    void doPostListWorkflows(int tenantId, List<Workflow> result) throws WorkflowException;

    /**
     * Trigger before removing an association.
     *
     * @param associationId
     * @throws WorkflowException
     */
    void doPreRemoveAssociation(int associationId) throws WorkflowException;

    /**
     * Trigger after removing an association.
     *
     * @param associationId
     * @throws WorkflowException
     */
    void doPostRemoveAssociation(int associationId) throws WorkflowException;

    /**
     * Trigger before getting associations of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    void doPreGetAssociationsForWorkflow(String workflowId) throws WorkflowException;

    /**
     * Trigger before getting associations of a workflow
     *
     * @param workflowId
     * @throws WorkflowException
     */
    void doPostGetAssociationsForWorkflow(String workflowId, List<Association> result) throws WorkflowException;

    /**
     * Trigger before listing all associations
     *
     * @param tenantId
     * @throws WorkflowException
     */
    void doPreListAllAssociations(int tenantId) throws WorkflowException;

    /**
     * Trigger after listing all associations
     *
     * @param tenantId
     * @throws WorkflowException
     */
    void doPostListAllAssociations(int tenantId, List<Association> result) throws WorkflowException;

    /**
     * Trigger before changing state of an association
     *
     * @param associationId
     * @param isEnable
     * @throws WorkflowException
     */
    void doPreChangeAssociationState(String associationId, boolean isEnable) throws WorkflowException;

    /**
     * Trigger after changing state of an association
     *
     * @param associationId
     * @param isEnable
     * @throws WorkflowException
     */
    void doPostChangeAssociationState(String associationId, boolean isEnable) throws WorkflowException;

    /**
     * @param requestId
     * @param entities
     * @throws WorkflowException
     */
    void doPreAddRequestEntityRelationships(String requestId, Entity[] entities) throws WorkflowException;

    /**
     * @param requestId
     * @param entities
     * @throws WorkflowException
     */
    void doPostAddRequestEntityRelationships(String requestId, Entity[] entities) throws WorkflowException;

    /**
     * @param entity
     * @throws WorkflowException
     */
    void doPreEntityHasPendingWorkflows(Entity entity) throws WorkflowException;

    /**
     * @param entity
     * @throws WorkflowException
     */
    void doPostEntityHasPendingWorkflows(Entity entity) throws WorkflowException;

    /**
     * @param entity
     * @param requestType
     * @throws WorkflowException
     */
    void doPreEntityHasPendingWorkflowsOfType(Entity entity, String requestType) throws WorkflowException;

    /**
     * @param entity
     * @param requestType
     * @throws WorkflowException
     */
    void doPostEntityHasPendingWorkflowsOfType(Entity entity, String requestType) throws WorkflowException;

    /**
     * @param entity1
     * @param entity2
     * @throws WorkflowException
     */
    void doPreAreTwoEntitiesRelated(Entity entity1, Entity entity2) throws WorkflowException;

    /**
     * @param entity1
     * @param entity2
     * @throws WorkflowException
     */
    void doPostAreTwoEntitiesRelated(Entity entity1, Entity entity2) throws WorkflowException;

    /**
     * @param eventType
     * @throws WorkflowException
     */
    void doPreIsEventAssociated(String eventType) throws WorkflowException;

    /**
     * @param eventType
     * @throws WorkflowException
     */
    void doPostIsEventAssociated(String eventType) throws WorkflowException;

    /**
     * @param user
     * @param tenantId
     * @throws WorkflowException
     */
    void doPreGetRequestsCreatedByUser(String user, int tenantId) throws WorkflowException;

    /**
     * @param user
     * @param tenantId
     * @throws WorkflowException
     */
    void doPostGetRequestsCreatedByUser(String user, int tenantId, WorkflowRequest[] results) throws WorkflowException;

    /**
     * @param requestId
     * @throws WorkflowException
     */
    void doPreGetWorkflowsOfRequest(String requestId) throws WorkflowException;

    /**
     * @param requestId
     * @throws WorkflowException
     */
    void doPostGetWorkflowsOfRequest(String requestId, WorkflowRequestAssociation[] results) throws WorkflowException;

    /**
     * @param user
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @param tenantId
     * @param status
     * @throws WorkflowException
     */
    void doPreGetRequestsFromFilter(String user, String beginDate, String endDate, String
            dateCategory, int tenantId, String status) throws WorkflowException;

    /**
     * @param user
     * @param beginDate
     * @param endDate
     * @param dateCategory
     * @param tenantId
     * @param status
     * @throws WorkflowException
     */
    void doPostGetRequestsFromFilter(String user, String beginDate, String endDate, String
            dateCategory, int tenantId, String status, WorkflowRequest[] result) throws WorkflowException;

    /**
     * @param wfOperationType
     * @param wfStatus
     * @param entityType
     * @param tenantID
     * @param idFilter
     * @throws WorkflowException
     */
    void doPreListEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID, String
            idFilter) throws WorkflowException;

    /**
     * @param wfOperationType
     * @param wfStatus
     * @param entityType
     * @param tenantID
     * @param idFilter
     * @throws WorkflowException
     */
    void doPostListEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID, String
            idFilter, List<String> result) throws WorkflowException;

}
