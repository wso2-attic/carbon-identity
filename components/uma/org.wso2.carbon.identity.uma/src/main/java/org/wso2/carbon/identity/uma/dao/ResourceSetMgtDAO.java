/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;
import org.wso2.carbon.identity.uma.model.ResourceSetDO;
import org.wso2.carbon.identity.uma.util.UMAUtil;

import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Data Access functionality for OAuth 2.0 Resource Set Registration implementation.
 * This includes storing, retrieving and updating resource set descriptions introduced
 * by the resource owner to the Authorization Server
 */
public class ResourceSetMgtDAO {

    private static final Log log = LogFactory.getLog(ResourceSetMgtDAO.class);

    // table to store the resource set descriptions
    private static final String IDN_UMA_PROTECTION_RESOURCE_SET_REG = "IDN_UMA_RESOURCE_SET_DESCRIPTIONS";

    private static final String IDN_UMA_PROTECTION_RESOURCE_SET_SCOPES = "IDN_UMA_RESOURCE_SET_SCOPES";

    private static final String DEFAULT_TIME_ZONE = "UTC";
    // thread pool size
    private int maxPoolSize = 0;


    // handlers call this method to persist the tokens
    public void saveResourceSetDescription
            (ResourceSetDO newResourceSetDO, ResourceSetDO existingResourceDO, String consumerKey,
             String userStoreDomain) throws IdentityUMAException {

        if (maxPoolSize > 0){
            // persist using threads TODO
        }else{
            persistResourceSet(newResourceSetDO, existingResourceDO, consumerKey, userStoreDomain);
        }
    }


    // intermediate to do actions for existing ResourceSetDOs
    private void persistResourceSet
            (ResourceSetDO newResourceSetDO,ResourceSetDO existingResourceSetDO, String consumerKey, String userStoreDomain)
            throws IdentityUMAException {

        Connection connection = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            // validate the resource set and then proceed to persist

            // persist the resource set description
            saveResourceSet(newResourceSetDO, consumerKey, connection, userStoreDomain);

            connection.commit();

        } catch (IdentityException | SQLException e) {
                throw new IdentityUMAException
                        (e.getMessage());
        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
    }


    /**
     * Persists the resource set description to the database
     * @param resourceSetDO
     * @param consumerKey
     * @param connection
     * @param userStoreDomain
     * @throws IdentityUMAException
     */
    private void saveResourceSet
            (ResourceSetDO resourceSetDO, String consumerKey, Connection connection, String userStoreDomain)
            throws IdentityUMAException {

        PreparedStatement preparedStatement;
        String resourceSetRegTable = IDN_UMA_PROTECTION_RESOURCE_SET_REG;

        // if the userStore value is not null calculate the table name using the user store domain name
        if (userStoreDomain != null){
            resourceSetRegTable = resourceSetRegTable + "_" + userStoreDomain;
        }

        String sql = "INSERT INTO " +
                resourceSetRegTable +
                " (RESOURCE_SET_ID, NAME, URI, TYPE, ICON_URI, " +
                "AUTHZ_USER, CONSUMER_KEY, RESOURCE_SET_SCOPE, TIME_CREATED) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        try {

            preparedStatement = connection.prepareStatement(sql);

            // fill in the params for the query
            preparedStatement.setString(1, resourceSetDO.getResourceSetId());
            preparedStatement.setString(2,resourceSetDO.getName());
            preparedStatement.setString(3, resourceSetDO.getURI());
            preparedStatement.setString(4, resourceSetDO.getType());
            preparedStatement.setString(5, resourceSetDO.getIconURI());
            preparedStatement.setString(6, resourceSetDO.getAuthorizedUser());
            preparedStatement.setString(7, consumerKey);
            preparedStatement.setString(8, UMAUtil.buildScopeString(resourceSetDO.getScopes()));
            preparedStatement.setTimestamp
                    (9, resourceSetDO.getCreatedTime(), Calendar.getInstance(TimeZone.getTimeZone(DEFAULT_TIME_ZONE)));

            // persist the resource set description
            preparedStatement.execute();

//            // now persist the scopes associated with the resource set description
//            preparedStatement = connection.prepareStatement(sqlAddScopes);
//            if (resourceSetDO.getScopes() != null && resourceSetDO.getScopes().length > 0) {
//                for (String scope : resourceSetDO.getScopes()) {
//                    preparedStatement.setString(1, resourceSetDO.getResourceSetId());
//                    preparedStatement.setString(2, scope);
//                    preparedStatement.execute();
//                }
//            } else {
//                preparedStatement.setString(1, resourceSetDO.getResourceSetId());
//                preparedStatement.setString(2, "");
//                preparedStatement.execute();
//            }

        } catch (SQLIntegrityConstraintViolationException ex){

            String errorMsg = "Access Token for consumer key : " + consumerKey + ", user : " +
                    resourceSetDO.getAuthorizedUser() + " and scope : " +
                    UMAUtil.buildScopeString(resourceSetDO.getScopes()) + "already exists";
            throw new IdentityUMAException(errorMsg, ex);

        }catch (DataTruncation e) {

            throw new IdentityUMAException("Invalid request", e);

        } catch (SQLException e) {
            log.error("Error when storing the resource set description for " + resourceSetDO.getName());
            throw new IdentityUMAException(e.getMessage());
        }


    }







}
