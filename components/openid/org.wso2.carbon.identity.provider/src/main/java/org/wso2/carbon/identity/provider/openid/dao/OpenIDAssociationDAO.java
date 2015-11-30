/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid.dao;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.provider.openid.OpenIDServerConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The DAO used to access the identity database. This basically store, load and
 * remove associations from the database.
 *
 * @author WSO2 Inc.
 */
public class OpenIDAssociationDAO {

    private static final Log log = LogFactory.getLog(OpenIDAssociationDAO.class);
    private String associationStore;

    private static OpenIDAssociationDAO privateStore;
    private static OpenIDAssociationDAO sharedStore;

    static {
        privateStore = new OpenIDAssociationDAO(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE);
        sharedStore = new OpenIDAssociationDAO(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_SHARED);
    }

    /**
     * Create the DAO with the identity database connection.
     *
     * @param dbConnection
     * @param privateAssociations if this DAO stores private associations
     */
    private OpenIDAssociationDAO(String storeType) {
        associationStore = storeType;
    }

    public static OpenIDAssociationDAO getInstance(String storeType){
        if(storeType.equals(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE)){
            return privateStore;
        } else{
            return sharedStore;
        }
    }

    /**
     * Tries to store the association in the identity database. But if the entry
     * already exists this operation doesn't do anything useful.
     *
     * @param association
     */
    public void storeAssociation(Association association) {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        try {

//            if (!isAssociationExist(connection, association.getHandle())) {
                prepStmt = connection.prepareStatement(OpenIDSQLQueries.STORE_ASSOCIATION);
                prepStmt.setString(1, association.getHandle());
                prepStmt.setString(2, association.getType());
                prepStmt.setTimestamp(3, new java.sql.Timestamp(association.getExpiry().getTime()));
                prepStmt.setString(4, Base64.encode(association.getMacKey().getEncoded()));
                prepStmt.setString(5, associationStore);
                prepStmt.execute();
                connection.commit();
                if(log.isDebugEnabled()) {
                    log.debug("Association " + association.getHandle() + " successfully stored in the database");
                }
//            } else {
//                log.debug("Association " + association.getHandle() + " already exist in the database.");
//            }
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to store the association " + association.getHandle(), e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * Loads the association in the identity database.
     *
     * @param handle
     * @return <code>Association</code>
     */
    public Association loadAssociation(String handle) {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        ResultSet results = null;

        try {
            prepStmt = connection.prepareStatement(OpenIDSQLQueries.LOAD_ASSOCIATION);
            prepStmt.setString(1, handle);
            results = prepStmt.executeQuery();

            if (results.next()) {
                log.debug("Loading association " + handle);
                return buildAssociationObject(results);
            }
            connection.commit();
        } catch (SQLException e) {
            log.error("Failed to load the association " + handle, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, results, prepStmt);
        }
        if(log.isDebugEnabled()) {
            log.debug("Failed to load the association " + handle + " from the database");
        }
        return null;
    }

    /**
     * Tries to remove the association from the database. But if the entry
     * doesn't exist, then this method throws an exception.
     *
     * @param handle
     */
    public void removeAssociation(String handle) {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;

        try {

            if (isAssociationExist(connection, handle)) {
                prepStmt = connection.prepareStatement(OpenIDSQLQueries.REMOVE_ASSOCIATION);
                prepStmt.setString(1, handle);
                prepStmt.execute();
                connection.commit();
                if(log.isDebugEnabled()) {
                    log.debug("Association " + handle + " successfully removed from the database");
                }
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("Association " + handle + " does not exist in the database");
                }
            }

        } catch (SQLException e) {
            log.error("Failed to remove the association " + handle, e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * Check if the entry exist in the database
     *
     * @param connection
     * @return boolean
     * @throws SQLException
     */
    private boolean isAssociationExist(Connection connection, String handle) {

        PreparedStatement prepStmt = null;
        ResultSet results = null;
        boolean result = false;

        try {
            prepStmt = connection.prepareStatement(OpenIDSQLQueries.CHECK_ASSOCIATION_ENTRY_EXIST);
            prepStmt.setString(1, handle);
            results = prepStmt.executeQuery();

            if (results.next()) {
                result = true;
                log.debug("Association " + handle + " found");
            }

        } catch (SQLException e) {
            log.error("Failed to load the association " + handle + ". Error while accessing the database. ", e);
        } finally {
            IdentityDatabaseUtil.closeResultSet(results);
            IdentityDatabaseUtil.closeStatement(prepStmt);
        }
        log.debug("Association " + handle + " not found");
        return result;
    }

    /**
     * Builds the Association object
     *
     * @param results
     * @return <code>Association</code>
     * @throws SQLException
     */
    private synchronized Association buildAssociationObject(ResultSet results) {

        Association assoc = null;
        String assocHandle = null;

        try {

            assocHandle = results.getString(1);
            String assocType = results.getString(2);
            java.util.Date expireIn = new java.util.Date(results.getTimestamp(3).getTime());
            String macKey = results.getString(4);
            String assocStore = results.getString(5);

            // we check if params are missing
            if (assocHandle == null || assocType == null || expireIn == null || macKey == null || assocStore == null) {
                log.error("Required data missing. Cannot build the Association object");
                return null;
            }

            // Here we check if we are loading the correct associations
            if (associationStore.equals(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE) &&
                assocStore.equals(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_SHARED)) {
                log.error(
                        "Invalid association data found. Tried to load a Private Association but found a Shared Association");
                return null;
            } else if (associationStore.equals(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_SHARED) &&
                       assocStore.equals(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE)) {
                log.error(
                        "Invalid association data found. Tried to load a Shared Association but found a Private Association");
                return null;
            }

            // Checks for association handle
            if (Association.TYPE_HMAC_SHA1.equals(assocType)) {
                assoc = Association.createHmacSha1(assocHandle, Base64.decode(macKey), expireIn);

            } else if (Association.TYPE_HMAC_SHA256.equals(assocType)) {
                assoc = Association.createHmacSha256(assocHandle, Base64.decode(macKey), expireIn);

            } else {
                log.error("Invalid association type " + assocType + " loaded from database");
                return null;
            }

        } catch (SQLException e) {
            log.error("Failed to build the Association for " + assocHandle + ". Error while accessing the database.",
                      e);
        } finally {
            IdentityDatabaseUtil.closeResultSet(results);
        }

        log.debug("Association " + assocHandle + " loaded successfully from the database.");
        return assoc;
    }

}
