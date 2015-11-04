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

package org.wso2.carbon.identity.workflow.impl.dao;

import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplException;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.mgt.util.SQLConstants;

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

    /**
     *
     *
     * @param bpsProfileDTO
     * @param tenantId
     * @throws WorkflowImplException
     */
    public void addProfile(BPSProfile bpsProfileDTO, int tenantId)
            throws WorkflowImplException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
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
            throw new WorkflowImplException("Error while encrypting the passwords of BPS Profile" + " " +
                                            profileName, e);
        }

        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, bpsProfileDTO.getProfileName());
            prepStmt.setString(2, bpsProfileDTO.getManagerHostURL());
            prepStmt.setString(3, bpsProfileDTO.getWorkerHostURL());
            prepStmt.setString(4, bpsProfileDTO.getUsername());
            prepStmt.setString(5, encryptPassword);
            prepStmt.setString(6, bpsProfileDTO.getCallbackUser());
            prepStmt.setString(7, encryptCallBackPassword);
            prepStmt.setInt(8, tenantId);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new WorkflowImplException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }


    /**
     *
     *
     * @param bpsProfile
     * @param tenantId
     * @throws WorkflowImplException
     */
    public void updateProfile(BPSProfile bpsProfile, int tenantId)
            throws WorkflowImplException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
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
            throw new WorkflowImplException("Error while encrypting the passwords of BPS Profile" + " " +
                                            profileName, e);
        }

        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, bpsProfile.getManagerHostURL());
            prepStmt.setString(2, bpsProfile.getWorkerHostURL());
            prepStmt.setString(3, bpsProfile.getUsername());
            prepStmt.setString(4, encryptPassword);
            prepStmt.setString(5, bpsProfile.getCallbackUser());
            prepStmt.setString(6, encryptCallBackPassword);
            prepStmt.setInt(7, tenantId);
            prepStmt.setString(8, bpsProfile.getProfileName());

            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new WorkflowImplException("Error when executing the sql query", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     *
     *
     * @param profileName
     * @param tenantId
     * @param isWithPasswords
     * @return
     * @throws WorkflowImplException
     */
    public BPSProfile getBPSProfile(String profileName, int tenantId, boolean isWithPasswords) throws
                                                                                               WorkflowImplException {

        BPSProfile bpsProfileDTO = null;

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        ;
        PreparedStatement prepStmt = null;
        ResultSet rs;
        Map<String, Object> profileParams = new HashMap<>();
        String query = SQLConstants.GET_BPS_PROFILE_FOR_TENANT_QUERY;
        String decryptPassword;
        String decryptCallBackPassword;

        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                String managerHostName = rs.getString(SQLConstants.HOST_URL_MANAGER_COLUMN);
                String workerHostName = rs.getString(SQLConstants.HOST_URL_WORKER_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                String callbackUser = rs.getString(SQLConstants.CALLBACK_USER_COLUMN);

                bpsProfileDTO = new BPSProfile();
                bpsProfileDTO.setProfileName(profileName);
                bpsProfileDTO.setManagerHostURL(managerHostName);
                bpsProfileDTO.setWorkerHostURL(workerHostName);
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
                        throw new WorkflowImplException("Error while decrypting the password for BPEL Profile"
                                                        + " " + profileName, e);
                    }
                    bpsProfileDTO.setPassword(decryptPassword);
                    bpsProfileDTO.setCallbackPassword(decryptCallBackPassword);
                }

            }
        } catch (SQLException e) {
            throw new WorkflowImplException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return bpsProfileDTO;
    }


    /**
     *
     *
     * @param tenantId
     * @return
     * @throws WorkflowImplException
     */
    public List<BPSProfile> listBPSProfiles(int tenantId) throws WorkflowImplException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet rs;
        List<BPSProfile> profiles = new ArrayList<>();
        String query = SQLConstants.LIST_BPS_PROFILES_QUERY;
        String decryptPassword;
        String decryptCallBackPassword;
        CryptoUtil cryptoUtil = CryptoUtil.getDefaultCryptoUtil();
        try {
            Object classCheckresult = null;
            try {
                classCheckresult = Class.forName("org.wso2.carbon.humantask.deployer.HumanTaskDeployer");
            } catch (ClassNotFoundException e) {

            }
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, tenantId);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(SQLConstants.PROFILE_NAME_COLUMN);
                if (classCheckresult == null && name.equals("embeded_bps")) {
                    continue;
                }
                String managerHostName = rs.getString(SQLConstants.HOST_URL_MANAGER_COLUMN);
                String workerHostName = rs.getString(SQLConstants.HOST_URL_WORKER_COLUMN);
                String user = rs.getString(SQLConstants.USERNAME_COLUMN);
                String callbackUser = rs.getString(SQLConstants.CALLBACK_USER_COLUMN);
                String password = rs.getString(SQLConstants.PASSWORD_COLUMN);
                String callbackPassword = rs.getString(SQLConstants.CALLBACK_PASSWORD_COLUMN);
                try {
                    byte[] decryptedPasswordBytes = cryptoUtil.base64DecodeAndDecrypt(password);
                    decryptPassword = new String(decryptedPasswordBytes, "UTF-8");
                    byte[] decryptedCallBackPasswordBytes = cryptoUtil.base64DecodeAndDecrypt(callbackPassword);
                    decryptCallBackPassword = new String(decryptedCallBackPasswordBytes, "UTF-8");

                } catch (CryptoException | UnsupportedEncodingException e) {
                    throw new WorkflowImplException("Error while decrypting the password for BPEL Profile" + " " +
                                                    name, e);
                }
                BPSProfile profileBean = new BPSProfile();
                profileBean.setManagerHostURL(managerHostName);
                profileBean.setWorkerHostURL(workerHostName);
                profileBean.setProfileName(name);
                profileBean.setUsername(user);
                profileBean.setPassword(decryptPassword);
                profileBean.setCallbackPassword(decryptCallBackPassword);
                profileBean.setCallbackUser(callbackUser);
                profiles.add(profileBean);
            }
        } catch (SQLException e) {
            throw new WorkflowImplException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return profiles;
    }


    /**
     *
     * @param profileName
     * @throws WorkflowImplException
     */
    public void removeBPSProfile(String profileName) throws WorkflowImplException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        String query = SQLConstants.DELETE_BPS_PROFILES_QUERY;
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, profileName);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new WorkflowImplException("Error when executing the sql.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }
}
