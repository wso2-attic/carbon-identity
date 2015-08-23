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

import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.mgt.bean.BPSProfileDTO;
import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BPSProfileDAO {

    public void addProfile(BPSProfileDTO bpsProfileDTO, int tenantId)
            throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.ADD_BPS_PROFILE_QUERY;
        String password = bpsProfileDTO.getPassword();
        String callbackPassword = bpsProfileDTO.getCallbackPassword();
        String profileName = bpsProfileDTO.getProfileName();
        String encryptPassword;
        String encryptCallBackPassword;

        try {
            CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
            encryptPassword = cryptoUtil.
                    encryptAndBase64Encode(password.getBytes(Charset.forName("UTF-8")));
            encryptCallBackPassword = cryptoUtil.
                    encryptAndBase64Encode(callbackPassword.getBytes(Charset.forName("UTF-8")));
        } catch (CryptoException e) {
            throw new InternalWorkflowException("Error while encrypting the passwords of BPS Profile" + " " +
                    profileName, e);
        }

        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, bpsProfileDTO.getProfileName());
            prepStmt.setString(2, bpsProfileDTO.getHost());
            prepStmt.setString(3, bpsProfileDTO.getUsername());
            prepStmt.setString(4, encryptPassword);
            prepStmt.setString(5, bpsProfileDTO.getCallbackUser());
            prepStmt.setString(6, encryptCallBackPassword);
            prepStmt.setInt(7, tenantId);
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

    public void updateProfile(BPSProfileDTO bpsProfile, int tenantId)
            throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String query = SQLConstants.UPDATE_BPS_PROFILE_QUERY;
        String password = bpsProfile.getPassword();
        String callbackPassword = bpsProfile.getCallbackPassword();
        String profileName = bpsProfile.getProfileName();
        String encryptPassword;
        String encryptCallBackPassword;

        try {
            CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
            encryptPassword = cryptoUtil.
                    encryptAndBase64Encode(password.getBytes(Charset.forName("UTF-8")));
            encryptCallBackPassword = cryptoUtil.
                    encryptAndBase64Encode(callbackPassword.getBytes(Charset.forName("UTF-8")));
        } catch (CryptoException e) {
            throw new InternalWorkflowException("Error while encrypting the passwords of BPS Profile" + " " +
                    profileName, e);
        }

        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, bpsProfile.getHost());
            prepStmt.setString(2, bpsProfile.getUsername());
            prepStmt.setString(3, encryptPassword);
            prepStmt.setString(4, bpsProfile.getCallbackUser());
            prepStmt.setString(5, encryptCallBackPassword);
            prepStmt.setInt(6, tenantId);
            prepStmt.setString(7, bpsProfile.getProfileName());

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

    public BPSProfileDTO getBPSProfile(String profileName, int tenantId, boolean isWithPasswords) throws InternalWorkflowException {

        BPSProfileDTO bpsProfileDTO = null ;
        
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        Map<String, Object> profileParams = new HashMap<>();
        String query = SQLConstants.GET_BPS_PROFILE_FOR_TENANT_QUERY;
        String decryptPassword;
        String decryptCallBackPassword;

        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                String hostName = rs.getString(SQLConstants.HOST_URL_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                String callbackUser = rs.getString(SQLConstants.CALLBACK_USER_COLUMN);

                bpsProfileDTO = new BPSProfileDTO();
                bpsProfileDTO.setProfileName(profileName);
                bpsProfileDTO.setHost(hostName);
                bpsProfileDTO.setUsername(user);
                bpsProfileDTO.setCallbackUser(callbackUser);

                if (isWithPasswords) {
                    String password = rs.getString(SQLConstants.PASSWORD_COLUMN);
                    String callbackPassword = rs.getString(SQLConstants.CALLBACK_PASSWORD_COLUMN);

                    try {
                        CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
                        byte[] decryptedPasswordBytes = cryptoUtil.base64DecodeAndDecrypt(password);
                        decryptPassword = new String(decryptedPasswordBytes, "UTF-8");
                        byte[] decryptedCallBackPasswordBytes = cryptoUtil.base64DecodeAndDecrypt(callbackPassword);
                        decryptCallBackPassword = new String(decryptedCallBackPasswordBytes, "UTF-8");

                    } catch (CryptoException | UnsupportedEncodingException e) {
                        throw new InternalWorkflowException("Error while decrypting the password for BPEL Profile"
                                + " " + profileName, e);
                    }
                    bpsProfileDTO.setPassword(decryptPassword);
                    bpsProfileDTO.setCallbackPassword(decryptCallBackPassword);
                }

            }
        } catch (IdentityException e) {
            throw new InternalWorkflowException("Error when connecting to the Identity Database.", e);
        } catch (SQLException e) {
            throw new InternalWorkflowException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return bpsProfileDTO;
    }

    public Map<String, Object> getBPELProfileParams(String profileName) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        Map<String, Object> profileParams = new HashMap<>();
        String query = SQLConstants.GET_BPS_PROFILE_QUERY;
        String decryptPassword;
        String decryptCallBackPassword;

        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                String hostName = rs.getString(SQLConstants.HOST_URL_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                String password = rs.getString(SQLConstants.PASSWORD_COLUMN);
                String callbackUser = rs.getString(SQLConstants.CALLBACK_USER_COLUMN);
                String callbackPassword = rs.getString(SQLConstants.CALLBACK_PASSWORD_COLUMN);

                try {
                    CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
                    byte[] decryptedPasswordBytes = cryptoUtil.base64DecodeAndDecrypt(password);
                    decryptPassword = new String(decryptedPasswordBytes, "UTF-8");
                    byte[] decryptedCallBackPasswordBytes = cryptoUtil.base64DecodeAndDecrypt(callbackPassword);
                    decryptCallBackPassword = new String(decryptedCallBackPasswordBytes, "UTF-8");

                } catch (CryptoException | UnsupportedEncodingException e) {
                    throw new InternalWorkflowException("Error while decrypting the password for BPEL Profile" + " " +
                            profileName, e);
                }

                profileParams.put(WorkFlowConstants.TemplateConstants.HOST, hostName);
                profileParams.put(WorkFlowConstants.TemplateConstants.AUTH_USER, user);
                profileParams.put(WorkFlowConstants.TemplateConstants.AUTH_USER_PASSWORD, decryptPassword);
                profileParams.put(WorkFlowConstants.TemplateConstants.CALLBACK_USER, callbackUser);
                profileParams.put(WorkFlowConstants.TemplateConstants.CALLBACK_USER_PASSWORD, decryptCallBackPassword);
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

    public List<BPSProfileDTO> listBPSProfiles(int tenantId) throws InternalWorkflowException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<BPSProfileDTO> profiles = new ArrayList<>();
        String query = SQLConstants.LIST_BPS_PROFILES_QUERY;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(SQLConstants.PROFILE_NAME_COLUMN);
                String hostName = rs.getString(SQLConstants.HOST_URL_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                String callbackUser = rs.getString(SQLConstants.CALLBACK_USER_COLUMN);
                BPSProfileDTO profileBean = new BPSProfileDTO();
                profileBean.setHost(hostName);
                profileBean.setProfileName(name);
                profileBean.setUsername(user);
                profileBean.setCallbackUser(callbackUser);
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
