/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.scim.common.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SQLQueries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SCIMProviderDAO {

    public static final Log log = LogFactory.getLog(SCIMProviderDAO.class);

    public List<SCIMProviderDTO> getAllProviders(String consumerId) throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        List<SCIMProviderDTO> scimProviders;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.GET_ALL_PROVIDERS_SQL);
            prepStmt.setString(1, consumerId);

            rSet = prepStmt.executeQuery();

            scimProviders = new ArrayList<SCIMProviderDTO>();
            while (rSet.next()) {
                if (rSet.getString(1) != null && rSet.getString(1).length() > 0) {
                    SCIMProviderDTO providerDTO = new SCIMProviderDTO();
                    providerDTO.setProviderId(rSet.getString(1));
                    providerDTO.setUserName(rSet.getString(2));
                    //decrypt the password before returning the DTO
                    String encryptedPassword = rSet.getString(3);
                    byte[] bytes =
                            CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(encryptedPassword);
                    providerDTO.setPassword(new String(bytes));
                    providerDTO.setUserEPURL(rSet.getString(4));
                    providerDTO.setGroupEPURL(rSet.getString(5));
                    providerDTO.setBulkEPURL(rSet.getString(6));
                    scimProviders.add(providerDTO);
                }
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.GET_ALL_PROVIDERS_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading the SCIM Providers information from " +
                                            "the persistence store.");
        } catch (CryptoException e) {
             String errorMsg = "Error in decrypting the password.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return scimProviders;
    }

    public SCIMProviderDTO getProvider(String consumerId, String providerId)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        SCIMProviderDTO providerDTO = new SCIMProviderDTO();
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.GET_PROVIDER_SQL);
            prepStmt.setString(1, consumerId);
            prepStmt.setString(2, providerId);
            rSet = prepStmt.executeQuery();

            while (rSet.next()) {
                if (rSet.getString(1) != null && rSet.getString(1).length() > 0) {
                    providerDTO.setProviderId(providerId);
                    providerDTO.setUserName(rSet.getString(1));
                    //decrypt the password before returning the DTO
                    String encryptedPassword = rSet.getString(2);
                    byte[] bytes =
                            CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(encryptedPassword);
                    providerDTO.setPassword(new String(bytes));
                    providerDTO.setUserEPURL(rSet.getString(3));
                    providerDTO.setGroupEPURL(rSet.getString(4));
                    providerDTO.setBulkEPURL(rSet.getString(5));
                }
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.GET_PROVIDER_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading the SCIM Provider information from " +
                                            "the persistence store.");
        } catch (CryptoException e) {
            String errorMsg = "Error in decrypting the password.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return providerDTO;
    }

    public void addProvider(String consumerId, SCIMProviderDTO providerDTO)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sqlStmt = null;

        if (!isExistingProvider(consumerId, providerDTO.getProviderId())) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                sqlStmt = SQLQueries.ADD_PROVIDER_SQL;
                prepStmt = connection.prepareStatement(sqlStmt);
                prepStmt.setString(1, consumerId);
                prepStmt.setString(2, providerDTO.getProviderId());
                prepStmt.setString(3, providerDTO.getUserName());
                //encrypt the password before persisting
                String plainTextPassword = providerDTO.getPassword();
                String cipherTextPassword =
                        CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(
                                plainTextPassword.getBytes());
                prepStmt.setString(4, cipherTextPassword);
                prepStmt.setString(5, providerDTO.getUserEPURL());
                prepStmt.setString(6, providerDTO.getGroupEPURL());
                prepStmt.setString(7, providerDTO.getBulkEPURL());
                prepStmt.execute();

                connection.commit();

            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } catch (SQLException e) {
                log.error("Error when executing the SQL : " + sqlStmt);
                log.error(e.getMessage(), e);
                throw new IdentitySCIMException("Error when adding a new SCIM Provider.");
            } catch (CryptoException e) {
                String errorMsg = "Error when encrypting the password.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
            //add is scim consumer enabled to cache.
            SCIMProvisioningConfigManager.getInstance().addEnabledToCache(consumerId);

        } else {
            String errorMessage = "SCIM provider with given Consumer Id and Provider Id already exists.";
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public void deleteProvider(String consumerId, String providerId) throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        if (isExistingProvider(consumerId, providerId)) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                prepStmt = connection.prepareStatement(SQLQueries.DELETE_PROVIDER_SQL);
                prepStmt.setString(1, consumerId);
                prepStmt.setString(2, providerId);

                prepStmt.execute();
                connection.commit();

            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } catch (SQLException e) {
                log.error("Error when executing the SQL : " + SQLQueries.DELETE_GROUP_SQL);
                log.error(e.getMessage(), e);
                throw new IdentitySCIMException("Error deleting the SCIM Group.");
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
            //delete from cache
            SCIMProvisioningConfigManager.getInstance().removeEnabledFromCache(consumerId);
        } else {
            String errorMessage = "SCIM provider with given Consumer Id and Provider Id doesn't exists.";
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public void updateProvider(String consumerId, SCIMProviderDTO providerDTO)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        if (isExistingProvider(consumerId, providerDTO.getProviderId())) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                prepStmt = connection.prepareStatement(SQLQueries.UPDATE_PROVIDER_SQL);

                prepStmt.setString(1, providerDTO.getUserName());
                //encrypt the password before persisting
                //encrypt the password before persisting
                String plainTextPassword = providerDTO.getPassword();
                String cipherTextPassword =
                        CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(
                                plainTextPassword.getBytes());
                prepStmt.setString(2, cipherTextPassword);
                prepStmt.setString(3, providerDTO.getUserEPURL());
                prepStmt.setString(4, providerDTO.getGroupEPURL());
                prepStmt.setString(5, providerDTO.getBulkEPURL());
                prepStmt.setString(6, consumerId);
                prepStmt.setString(7, providerDTO.getProviderId());

                int count = prepStmt.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("No. of records updated for updating SCIM Provider : " + count);
                }
                connection.commit();

            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } catch (SQLException e) {
                log.error("Error when executing the SQL : " + SQLQueries.UPDATE_PROVIDER_SQL);
                log.error(e.getMessage(), e);
                throw new IdentitySCIMException("Error updating the SCIM Provider.");
            } catch (CryptoException e) {
                String errorMsg = "Error when encrypting the password.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            String errorMessage = "SCIM provider with given Consumer Id and Provider Id doesn't exists.";
            log.error(errorMessage);
            throw new IdentitySCIMException(errorMessage);
        }
    }

    public boolean isExistingProvider(String consumerId, String providerId)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isExistingProvider = false;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_EXISTING_PROVIDER_SQL);
            prepStmt.setString(1, consumerId);
            prepStmt.setString(2, providerId);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isExistingProvider = true;
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.CHECK_EXISTING_PROVIDER_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading provider information from " +
                                            "the persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isExistingProvider;
    }

    public boolean isExistingConsumer(String consumerId) throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isExistingProvider = false;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_EXISTING_CONSUMER_SQL);
            prepStmt.setString(1, consumerId);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isExistingProvider = true;
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.CHECK_EXISTING_CONSUMER_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading provider information from " +
                                            "the persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isExistingProvider;
    }

    public boolean isFirstStartup() throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isFirstStartup = true;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_FIRST_STARTUP_SQL);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isFirstStartup = false;
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.CHECK_FIRST_STARTUP_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading provider information from " +
                                            "the persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isFirstStartup;
    }
}
