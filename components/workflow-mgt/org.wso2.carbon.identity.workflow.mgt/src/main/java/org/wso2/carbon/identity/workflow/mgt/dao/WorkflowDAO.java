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

package org.wso2.carbon.identity.workflow.mgt.dao;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.AbstractWorkflowTemplate;
import org.wso2.carbon.identity.workflow.mgt.AbstractWorkflowTemplateImpl;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowBean;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowDAO {

    /**
     * Stores Workflow executor service details
     */
    public void addWorkflow(String id, String name, String description, String templateId, String templateImpl)
            throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WORKFLOW_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, id);
            prepStmt.setString(2, name);
            prepStmt.setString(3, description);
            prepStmt.setString(4, templateId);
            prepStmt.setString(5, templateImpl);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void addWorkflowParams(String workflowId, Map<String, Object> values) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WORKFLOW_PARAMS_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, workflowId);
                prepStmt.setString(2, entry.getKey());
                prepStmt.setString(3, (String)entry.getValue());    //The values should be string
                prepStmt.executeUpdate();
            }
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public Map<String, Object> getWorkflowParams(String workflowId) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Map<String, Object> worlflowParams = new HashMap<>();
        String query = SQLConstants.GET_WORKFLOW_PARAMS;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String paramName = rs.getString(SQLConstants.PARAM_NAME_COLUMN);
                String paramValue = rs.getString(SQLConstants.PARAM_VALUE_COLUMN);
                if (StringUtils.isNotBlank(paramName)) {
                    worlflowParams.put(paramName, paramValue);
                }
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return worlflowParams;
    }

    /**
     * Stores the association of workflow executor service to a event type with the condition.
     */
    public void addAssociation(String workflowId, String eventId, String condition)
            throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ASSOCIATE_WF_TO_EVENT;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            prepStmt.setString(2, condition);
            prepStmt.setString(3, workflowId);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void removeAssociation(int id) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_ASSOCIATION_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, id);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void removeWorkflow(String id) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_WORKFLOW_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, id);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

//todo: updateWorkflow()

    public List<WorkflowBean> listWorkflows() throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<WorkflowBean> workflowList = new ArrayList<>();
        String query = SQLConstants.LIST_WORKFLOWS_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString(SQLConstants.ID_COLUMN);
                String name = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String description = rs.getString(SQLConstants.DESCRIPTION_COLUMN);
                String templateId = rs.getString(SQLConstants.TEMPLATE_ID_COLUMN);
                String templateImplId = rs.getString(SQLConstants.TEMPLATE_IMPL_ID_COLUMN);
                WorkflowBean workflowBean = new WorkflowBean();
                workflowBean.setWorkflowId(id);
                workflowBean.setWorkflowName(name);
                workflowBean.setWorkflowDescription(description);
                AbstractWorkflowTemplate template = WorkflowServiceDataHolder.getInstance().getTemplate(templateId);
                AbstractWorkflowTemplateImpl templateImplementation = WorkflowServiceDataHolder.getInstance()
                        .getTemplateImplementation(templateId, templateImplId);
                if (template != null && templateImplementation != null) {
                    workflowBean.setTemplateName(template.getFriendlyName());
                    workflowBean.setImplementationName(templateImplementation.getImplementationName());
                } else {
                    workflowBean.setTemplateName("");
                    workflowBean.setImplementationName("");
                }
                workflowList.add(workflowBean);
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return workflowList;
    }

    public List<WorkflowAssociation> getWorkflowsForRequest(String eventId)
            throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<WorkflowAssociation> associations = new ArrayList<>();
        String query = SQLConstants.GET_WORKFLOWS_FOR_EVENT_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String workflowId = rs.getString(SQLConstants.WORKFLOW_ID_COLUMN);
                String templateId = rs.getString(SQLConstants.TEMPLATE_ID_COLUMN);
                String templateImplId = rs.getString(SQLConstants.TEMPLATE_IMPL_ID_COLUMN);
                WorkflowAssociation association = new WorkflowAssociation();
                association.setWorkflowId(workflowId);
                association.setCondition(condition);
                association.setTemplateId(templateId);
                association.setImplId(templateImplId);
                associations.add(association);
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associations;
    }

}
