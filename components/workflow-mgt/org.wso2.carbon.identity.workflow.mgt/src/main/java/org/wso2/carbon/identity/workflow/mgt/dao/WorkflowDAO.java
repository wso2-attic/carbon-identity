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
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractTemplate;
import org.wso2.carbon.identity.workflow.mgt.util.SQLConstants;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.internal.WorkflowServiceDataHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WorkflowDAO {

    /**
     * Stores Workflow executor service details
     */
    public void addWorkflow(Workflow workflow, int
            tenantId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WORKFLOW_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflow.getWorkflowId());
            prepStmt.setString(2, workflow.getWorkflowName());
            prepStmt.setString(3, workflow.getWorkflowDescription());
            prepStmt.setString(4, workflow.getTemplateId());
            prepStmt.setString(5, workflow.getWorkflowImplId());
            prepStmt.setInt(6, tenantId);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void updateWorkflow(Workflow workflow)
            throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        String query = SQLConstants.UPDATE_WORKFLOW_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflow.getWorkflowDescription());
            prepStmt.setString(2, workflow.getTemplateId());
            prepStmt.setString(3, workflow.getWorkflowImplId());
            prepStmt.setString(4, workflow.getWorkflowId());
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void updateWorkflowParams(List<Parameter> parameterList, String workflowId)throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        String query = SQLConstants.UPDATE_WORKFLOW_PARAMS_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            for (Parameter parameter : parameterList){
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, parameter.getParamName());
                prepStmt.setString(2, parameter.getParamValue());
                prepStmt.setString(3, parameter.getqName());
                prepStmt.setString(4, parameter.getHolder());
                prepStmt.setString(5, workflowId);

                prepStmt.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }
    public void removeWorkflowParams(String workflowId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_WORKFLOW_PARAMS_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void addWorkflowParams(List<Parameter> parameterList, String workflowId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WORKFLOW_PARAMS_QUERY;
        try {
            for (Parameter parameter : parameterList){
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, workflowId);
                prepStmt.setString(2, parameter.getParamName());
                prepStmt.setString(3, parameter.getParamValue());
                prepStmt.setString(4, parameter.getqName());
                prepStmt.setString(5, parameter.getHolder());

                prepStmt.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public List<Parameter> getWorkflowParams(String workflowId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Parameter> parameterList = new ArrayList<>();
        String query = SQLConstants.GET_WORKFLOW_PARAMS;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String paramName = rs.getString(SQLConstants.PARAM_NAME_COLUMN);
                String paramValue = rs.getString(SQLConstants.PARAM_VALUE_COLUMN);
                String paramQName = rs.getString(SQLConstants.PARAM_QNAME_COLUMN);
                String paramHolder = rs.getString(SQLConstants.PARAM_HOLDER_COLUMN);
                if (StringUtils.isNotBlank(paramName)) {
                    Parameter parameter = new Parameter(workflowId,paramName,paramValue,paramQName,paramHolder);
                    parameterList.add(parameter);
                }
            }
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return parameterList;
    }

    public Workflow getWorkflow(String workflowId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = SQLConstants.GET_WORKFLOW;

        Workflow workflow = null ;

        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String description = rs.getString(SQLConstants.DESCRIPTION_COLUMN);
                String templateId = rs.getString(SQLConstants.TEMPLATE_ID_COLUMN);
                String implId = rs.getString(SQLConstants.TEMPLATE_IMPL_ID_COLUMN);
                workflow = new Workflow();
                workflow.setWorkflowId(workflowId);
                workflow.setWorkflowName(workflowName);
                workflow.setWorkflowDescription(description);
                workflow.setTemplateId(templateId);
                workflow.setWorkflowImplId(implId);

                break ;
            }
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return workflow;
    }

    /**
     * Stores the association of workflow executor service to a event type with the condition.
     */
    public void addAssociation(String associationName, String workflowId, String eventId, String condition)
            throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ASSOCIATE_WF_TO_EVENT;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            prepStmt.setString(2, associationName);
            prepStmt.setString(3, condition);
            prepStmt.setString(4, workflowId);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }


    public void updateAssociation(Association associationDTO)
            throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        String query = SQLConstants.UPDATE_ASSOCIATE_WF_TO_EVENT;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, associationDTO.getEventId());
            prepStmt.setString(2, associationDTO.getAssociationName());
            prepStmt.setString(3, associationDTO.getCondition());
            prepStmt.setString(4, associationDTO.getWorkflowId());
            if(associationDTO.isEnabled()) {
                prepStmt.setString(5, "1");
            }else{
                prepStmt.setString(5, "0");
            }
            prepStmt.setString(6, associationDTO.getAssociationId());
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void removeAssociation(int id) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_ASSOCIATION_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, id);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void removeWorkflow(String id) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_WORKFLOW_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, id);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

//todo: updateWorkflow()

    public List<Workflow> listWorkflows(int tenantId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Workflow> workflowList = new ArrayList<>();
        String query = SQLConstants.LIST_WORKFLOWS_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString(SQLConstants.ID_COLUMN);
                String name = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String description = rs.getString(SQLConstants.DESCRIPTION_COLUMN);
                String templateId = rs.getString(SQLConstants.TEMPLATE_ID_COLUMN);
                String templateImplId = rs.getString(SQLConstants.TEMPLATE_IMPL_ID_COLUMN);
                Workflow workflowDTO = new Workflow();
                workflowDTO.setWorkflowId(id);
                workflowDTO.setWorkflowName(name);
                workflowDTO.setWorkflowDescription(description);
                workflowDTO.setTemplateId(templateId);
                workflowDTO.setWorkflowImplId(templateImplId);
                workflowList.add(workflowDTO);
            }
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return workflowList;
    }

    public List<WorkflowAssociation> getWorkflowAssociationsForRequest(String eventId, int tenantId)
            throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<WorkflowAssociation> associations = new ArrayList<>();
        String query = SQLConstants.GET_ASSOCIATIONS_FOR_EVENT_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            prepStmt.setInt(2, tenantId);
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
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associations;
    }

    public List<Association> listAssociationsForWorkflow(String workflowId)
            throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<Association> associations = new ArrayList<>();
        String query = SQLConstants.GET_ASSOCIATIONS_FOR_WORKFLOW_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String eventId = rs.getString(SQLConstants.EVENT_ID_COLUMN);
                String associationId = String.valueOf(rs.getInt(SQLConstants.ID_COLUMN));
                String associationName = rs.getString(SQLConstants.ASSOCIATION_NAME_COLUMN);
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                Association associationDTO = new Association();
                associationDTO.setCondition(condition);
                associationDTO.setAssociationId(associationId);
                associationDTO.setEventId(eventId);
                associationDTO.setAssociationName(associationName);
                associationDTO.setWorkflowName(workflowName);
                associations.add(associationDTO);
            }
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associations;
    }

    public List<Association> listAssociations() throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<Association> associations = new ArrayList<>();
        String query = SQLConstants.GET_ALL_ASSOCIATIONS_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String eventId = rs.getString(SQLConstants.EVENT_ID_COLUMN);
                String associationId = String.valueOf(rs.getInt(SQLConstants.ID_COLUMN));
                String associationName = rs.getString(SQLConstants.ASSOCIATION_NAME_COLUMN);
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String isEnable = rs.getString(SQLConstants.ASSOCIATION_IS_ENABLED);
                Association associationDTO = new Association();
                associationDTO.setCondition(condition);
                associationDTO.setAssociationId(associationId);
                associationDTO.setEventId(eventId);
                associationDTO.setAssociationName(associationName);
                associationDTO.setWorkflowName(workflowName);
                associations.add(associationDTO);
                if(isEnable.equals("1")){
                    associationDTO.setEnabled(true);
                }else{
                    associationDTO.setEnabled(false);
                }
            }
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associations;
    }


    public Association getAssociation(String associationId) throws InternalWorkflowException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs;
        Association associationDTO = null ;
        String query = SQLConstants.GET_ASSOCIATION_FOR_ASSOC_ID_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, associationId);

            rs = prepStmt.executeQuery();

            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String eventId = rs.getString(SQLConstants.EVENT_ID_COLUMN);
                String associationName = rs.getString(SQLConstants.ASSOCIATION_NAME_COLUMN);
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String workflowId = rs.getString(SQLConstants.WORKFLOW_ID_COLUMN);
                String isEnable = rs.getString(SQLConstants.ASSOCIATION_IS_ENABLED);
                associationDTO = new Association();
                associationDTO.setCondition(condition);
                associationDTO.setAssociationId(associationId);
                associationDTO.setEventId(eventId);
                associationDTO.setWorkflowId(workflowId);
                associationDTO.setAssociationName(associationName);
                associationDTO.setWorkflowName(workflowName);

                if(isEnable.equals("1")){
                    associationDTO.setEnabled(true);
                }else{
                    associationDTO.setEnabled(false);
                }
            }


        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associationDTO;
    }



}
