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

import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.bean.Entity;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.Template;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowImpl;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowEvent;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequestAssociation;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.List;


public interface WorkflowManagementService {

    List<WorkflowEvent> listWorkflowEvents();

    public List<WorkflowImpl> listWorkflowImpls(String templateId) throws WorkflowException;

    WorkflowEvent getEvent(String id);
    List<Template> listTemplates() throws WorkflowException;
    Template getTemplate(String templateId) throws WorkflowException;

    public WorkflowImpl getWorkflowImpl(String templateId, String workflowImplId) throws WorkflowException;

    void addWorkflow(Workflow workflowDTO,
                     List<Parameter> parameterList, int tenantId) throws WorkflowException;
    public Workflow getWorkflow(String workflowId) throws WorkflowException ;
    public List<Parameter> getWorkflowParameters(String workflowId) throws WorkflowException ;

    void addAssociation(String associationName, String workflowId, String eventId, String condition) throws
                                                                                                     WorkflowException;

    List<Workflow> listWorkflows(int tenantId) throws WorkflowException;

    void removeWorkflow(String id) throws WorkflowException;

    void removeAssociation(int associationId) throws WorkflowException;



    List<Association> getAssociationsForWorkflow(String workflowId) throws WorkflowException;

    List<Association> listAllAssociations() throws WorkflowException;

    void changeAssociationState(String associationId, boolean isEnable) throws WorkflowException;

    void addRequestEntityRelationships(String requestId, Entity[] entities) throws InternalWorkflowException;

    boolean entityHasPendingWorkflows(Entity entity) throws InternalWorkflowException;

    boolean entityHasPendingWorkflowsOfType(Entity entity, String requestType) throws
                                                                               InternalWorkflowException;

    boolean areTwoEntitiesRelated(Entity entity1, Entity entity2) throws
                                                                  InternalWorkflowException;

    boolean eventEngagedWithWorkflows(String eventType) throws InternalWorkflowException;

    WorkflowRequest[] getRequestsCreatedByUser(String user, int tenantId) throws WorkflowException;

    WorkflowRequestAssociation[] getWorkflowsOfRequest(String requestId) throws WorkflowException;

    void updateStatusOfRequest(String requestId, String newState) throws WorkflowException;

    WorkflowRequest[] getRequestsFromFilter(String user, String beginDate, String endDate, String
            dateCategory, int tenantId) throws WorkflowException;

    List<String> listEntityNames(String wfOperationType, String wfStatus, String entityType, int tenantID) throws
                                                                                                           InternalWorkflowException;
}
