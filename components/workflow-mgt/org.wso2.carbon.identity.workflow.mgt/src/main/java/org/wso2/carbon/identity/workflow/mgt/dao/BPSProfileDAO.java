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

import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileBean;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BPSProfileDAO {

    public void addProfile(String profileName, String host, String user, String password)
            throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.ADD_BPS_PROFILE_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
            prepStmt.setString(2, host);
            prepStmt.setString(3, user);
            prepStmt.setString(4, password);
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

    public Map<String, Object> getBPELProfileParams(String profileName) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        Map<String, Object> profileParams = new HashMap<>();
        String query = SQLConstants.GET_BPS_PROFILE_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                String hostName = rs.getString(SQLConstants.HOST_URL_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                String password = rs.getString(SQLConstants.PASSWORD_COLUMN);
                profileParams.put(WorkFlowConstants.TemplateConstants.HOST, hostName);
                profileParams.put(WorkFlowConstants.TemplateConstants.AUTH_USER, user);
                profileParams.put(WorkFlowConstants.TemplateConstants.AUTH_USER_PASSWORD, password);
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return profileParams;
    }

    public List<BPSProfileBean> listBPSProfiles() throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<BPSProfileBean> profiles = new ArrayList<>();
        String query = SQLConstants.LIST_BPS_PROFILES_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(SQLConstants.PROFILE_NAME_COLUMN);
                String hostName = rs.getString(SQLConstants.HOST_URL_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                BPSProfileBean profileBean = new BPSProfileBean();
                profileBean.setHost(hostName);
                profileBean.setProfileName(name);
                profileBean.setUsername(user);
                profiles.add(profileBean);
            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return profiles;
    }

    public void removeBPSProfile(String profileName) throws InternalWorkflowException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_BPS_PROFILES_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
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
}
