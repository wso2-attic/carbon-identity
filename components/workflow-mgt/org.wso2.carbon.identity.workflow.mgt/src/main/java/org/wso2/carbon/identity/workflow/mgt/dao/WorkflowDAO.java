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
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.Workflow;
import org.wso2.carbon.identity.workflow.mgt.template.AbstractTemplate;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.identity.workflow.mgt.bean.AssociationDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociationBean;
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
    public void addWorkflow(Workflow workflowDTO, int
            tenantId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WORKFLOW_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowDTO.getWorkflowId());
            prepStmt.setString(2, workflowDTO.getWorkflowName());
            prepStmt.setString(3, workflowDTO.getWorkflowDescription());
            prepStmt.setString(4, workflowDTO.getTemplateId());
            prepStmt.setString(5, workflowDTO.getWorkflowImplId());
            prepStmt.setInt(6, tenantId);
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

    public void addWorkflowParams(List<Parameter> parameterList) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ADD_WORKFLOW_PARAMS_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            for (Parameter parameter : parameterList){
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, parameter.getWorkflowId());
                prepStmt.setString(2, parameter.getParamName());
                prepStmt.setString(3, parameter.getParamValue());
                prepStmt.setString(4, parameter.getQName());
                prepStmt.setString(4, parameter.getHolder());

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

    public List<Parameter> getWorkflowParams(String workflowId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Parameter> parameterList = new ArrayList<>();
        String query = SQLConstants.GET_WORKFLOW_PARAMS;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
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
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return parameterList;
    }

    public Workflow getWorkflow(String workflowId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = SQLConstants.GET_WORKFLOW;

        Workflow workflowDTO = new Workflow();

        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String description = rs.getString(SQLConstants.DESCRIPTION_COLUMN);
                String templateId = rs.getString(SQLConstants.TEMPLATE_ID_COLUMN);
                String implId = rs.getString(SQLConstants.TEMPLATE_IMPL_ID_COLUMN);

                workflowDTO.setWorkflowId(workflowId);
                workflowDTO.setWorkflowName(workflowName);
                workflowDTO.setWorkflowDescription(description);
                workflowDTO.setTemplateId(templateId);
                workflowDTO.setWorkflowImplId(implId);

                break ;
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return workflowDTO;
    }

    /**
     * Stores the association of workflow executor service to a event type with the condition.
     */
    public void addAssociation(String associationName, String workflowId, String eventId, String condition)
            throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.ASSOCIATE_WF_TO_EVENT;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            prepStmt.setString(2, associationName);
            prepStmt.setString(3, condition);
            prepStmt.setString(4, workflowId);
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


    public void updateAssociation(AssociationDTO associationDTO)
            throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        String query = SQLConstants.UPDATE_ASSOCIATE_WF_TO_EVENT;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
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

    public List<Workflow> listWorkflows(int tenantId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Workflow> workflowList = new ArrayList<>();
        String query = SQLConstants.LIST_WORKFLOWS_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
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
                AbstractTemplate template = WorkflowServiceDataHolder.getInstance().getTemplates().get(templateId);
                AbstractWorkflow abstractWorkflow = WorkflowServiceDataHolder.getInstance()
                        .getWorkflowImpls().get(templateImplId);
                if (template != null && abstractWorkflow != null) {
                    workflowDTO.setTemplateId(template.getTemplateId());
                    workflowDTO.setWorkflowImplId(abstractWorkflow.getWorkflowImplId());
                }
                workflowList.add(workflowDTO);
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

    public List<WorkflowAssociationBean> getWorkflowAssociationsForRequest(String eventId, int tenantId)
            throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<WorkflowAssociationBean> associations = new ArrayList<>();
        String query = SQLConstants.GET_ASSOCIATIONS_FOR_EVENT_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, eventId);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String workflowId = rs.getString(SQLConstants.WORKFLOW_ID_COLUMN);
                String templateId = rs.getString(SQLConstants.TEMPLATE_ID_COLUMN);
                String templateImplId = rs.getString(SQLConstants.TEMPLATE_IMPL_ID_COLUMN);
                WorkflowAssociationBean association = new WorkflowAssociationBean();
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

    public List<AssociationDTO> listAssociationsForWorkflow(String workflowId)
            throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<AssociationDTO> associations = new ArrayList<>();
        String query = SQLConstants.GET_ASSOCIATIONS_FOR_WORKFLOW_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, workflowId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String eventId = rs.getString(SQLConstants.EVENT_ID_COLUMN);
                String associationId = String.valueOf(rs.getInt(SQLConstants.ID_COLUMN));
                String associationName = rs.getString(SQLConstants.ASSOCIATION_NAME_COLUMN);
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                AssociationDTO associationDTO = new AssociationDTO();
                associationDTO.setCondition(condition);
                associationDTO.setAssociationId(associationId);
                associationDTO.setEventId(eventId);
                associationDTO.setAssociationName(associationName);
                associationDTO.setWorkflowName(workflowName);
                associations.add(associationDTO);
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

    public List<AssociationDTO> listAssociations() throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<AssociationDTO> associations = new ArrayList<>();
        String query = SQLConstants.GET_ALL_ASSOCIATIONS_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String condition = rs.getString(SQLConstants.CONDITION_COLUMN);
                String eventId = rs.getString(SQLConstants.EVENT_ID_COLUMN);
                String associationId = String.valueOf(rs.getInt(SQLConstants.ID_COLUMN));
                String associationName = rs.getString(SQLConstants.ASSOCIATION_NAME_COLUMN);
                String workflowName = rs.getString(SQLConstants.WF_NAME_COLUMN);
                String isEnable = rs.getString(SQLConstants.ASSOCIATION_IS_ENABLED);
                AssociationDTO associationDTO = new AssociationDTO();
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
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associations;
    }


    public AssociationDTO getAssociation(String associationId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        AssociationDTO associationDTO = null ;
        String query = SQLConstants.GET_ASSOCIATION_FOR_ASSOC_ID_QUERY;
        try {

            connection = IdentityDatabaseUtil.getDBConnection();
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
                associationDTO = new AssociationDTO();
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


        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return associationDTO;
    }



}
