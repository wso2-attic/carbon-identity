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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;
import org.wso2.carbon.identity.uma.model.ResourceSetDO;
import org.wso2.carbon.identity.uma.util.UMAUtil;

import java.sql.*;
import java.util.*;

/**
 * Data Access functionality for OAuth 2.0 Resource Set Registration implementation.
 * This includes storing, retrieving and updating resource set descriptions introduced
 * by the resource owner to the Authorization Server
 */
public class ResourceSetMgtDAO {

    private static final Log log = LogFactory.getLog(ResourceSetMgtDAO.class);

    // table to store the resource set descriptions
    private static final String IDN_UMA_PROTECTION_RESOURCE_SET = "IDN_UMA_RESOURCE_SET";

    private static final String DEFAULT_TIME_ZONE = "UTC";
    // thread pool size
    private int maxPoolSize = 0;


    // handlers call this method to persist the tokens
    public void saveResourceSetDescription
            (ResourceSetDO newResourceSetDO,
             String userStoreDomain) throws IdentityUMAException {

        if (maxPoolSize > 0){
            // persist using threads TODO
        }else{
            persistResourceSet(newResourceSetDO, userStoreDomain);
        }
    }


    // intermediate to do actions for existing ResourceSetDOs
    private void persistResourceSet
            (ResourceSetDO newResourceSetDO,String userStoreDomain)
            throws IdentityUMAException {

        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            // persist the resource set description
            saveResourceSet(newResourceSetDO, connection, userStoreDomain);

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
     * @param connection
     * @param userStoreDomain
     * @throws IdentityUMAException
     */
    private void saveResourceSet
            (ResourceSetDO resourceSetDO, Connection connection, String userStoreDomain)
            throws IdentityUMAException {

        PreparedStatement preparedStatement;

        // get the SQL with the table name corrected
        String sql = getCorrectedSQL(SQLQueries.INSERT_RESOURCE_SET,userStoreDomain);

        try {

            preparedStatement = connection.prepareStatement(sql);

            // fill in the params for the query
            preparedStatement.setString(1, resourceSetDO.getResourceSetId());
            preparedStatement.setString(2,resourceSetDO.getName());
            preparedStatement.setString(3, resourceSetDO.getURI());
            preparedStatement.setString(4, resourceSetDO.getType());
            preparedStatement.setString(5, resourceSetDO.getIconURI());
            preparedStatement.setString(6, resourceSetDO.getAuthorizedUser());
            preparedStatement.setString(7, resourceSetDO.getConsumerKey());
            preparedStatement.setString(8, UMAUtil.buildScopeString(resourceSetDO.getScopes()));
            preparedStatement.setTimestamp
                    (9, resourceSetDO.getCreatedTime(), Calendar.getInstance(TimeZone.getTimeZone(DEFAULT_TIME_ZONE)));

            preparedStatement.execute();

        } catch (SQLIntegrityConstraintViolationException ex){

            String errorMsg = "Access Token for consumer key : " + resourceSetDO.getConsumerKey() + ", user : " +
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



    public Set<ResourceSetDO> retrieveResourceSets(String consumerKey, String userStoreDomain) throws IdentityUMAException {

        Connection connection;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
        } catch (IdentityException e) {

            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);
        }

        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        Map<String, ResourceSetDO> resourceSetDOMap = new HashMap<>();

        String sql = getCorrectedSQL(SQLQueries.GET_ALL_RESOURCE_SETS, userStoreDomain);

        try {

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1,consumerKey);

            resultSet = prepStmt.executeQuery();

            while (resultSet.next()){

                String resourceSetId = resultSet.getString(1);

                if (resourceSetDOMap.get(resourceSetId) == null) {

                    String name = resultSet.getString(2);
                    String URI = resultSet.getString(3);
                    String type = resultSet.getString(4);
                    String iconURI = resultSet.getString(5);
                    String authorizedUser = resultSet.getString(6);
                    String resourceSetConsumerKey = resultSet.getString(7);
                    String[] resourceSetScope = UMAUtil.buildScopeArray(resultSet.getString(8));
                    Timestamp createdTime = resultSet.getTimestamp(9);

                    ResourceSetDO resourceSetDO = new ResourceSetDO(name, URI, type, resourceSetScope, iconURI);
                    resourceSetDO.setIconURI(iconURI);
                    resourceSetDO.setAuthorizedUser(authorizedUser);
                    resourceSetDO.setConsumerKey(resourceSetConsumerKey);
                    resourceSetDO.setCreatedTime(createdTime);

                    resourceSetDOMap.put(resourceSetId,resourceSetDO);
                }
            }

            connection.commit();
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving registred resource sets for " +
                    "Client ID : " + consumerKey;
            throw new IdentityUMAException(errorMsg, e);
        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection,resultSet,prepStmt);
        }

        return new HashSet<>(resourceSetDOMap.values());
    }

    /**
     *  Retrieve all the resource set IDs registred for a particular consumerKey
     * @param consumerKey
     * @param userStoreDomain
     * @return
     * @throws IdentityUMAException
     */
    public List<String> retrieveResourceSetIDs(String consumerKey, String userStoreDomain) throws IdentityUMAException {
        Connection connection;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
        } catch (IdentityException e) {

            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);
        }

        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        List<String> resourceSetIdList = new ArrayList<>();


        String sql = getCorrectedSQL(SQLQueries.GET_ALL_RESOURCE_SET_IDS, userStoreDomain);

        try {
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1,consumerKey);

            resultSet = prepStmt.executeQuery();

            while (resultSet.next()){
                String resourceSetId = resultSet.getString(1);

                if (StringUtils.isNotEmpty(resourceSetId) && !resourceSetIdList.contains(resourceSetId)){
                    resourceSetIdList.add(resourceSetId);
                }
            }

            connection.commit();
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving registered resource set IDs " +
                    "Client ID : " + consumerKey;

            throw new IdentityUMAException(errorMsg, e);
        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }

        return resourceSetIdList;
    }

    /**
     *
     * @param resourceSetId
     * @param consumerKey
     * @param userStoreDomain
     * @return
     * @throws IdentityUMAException
     */
    public ResourceSetDO retrieveResourceSet(String resourceSetId, String consumerKey, String userStoreDomain) throws IdentityUMAException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        ResourceSetDO resourceSetDO = null;

        String sql = getCorrectedSQL(SQLQueries.GET_RESOURCE_SET_FROM_ID,userStoreDomain);

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1,resourceSetId);

            resultSet = prepStmt.executeQuery();

            if (resultSet.next()){
                    String name = resultSet.getString(2);
                    String URI = resultSet.getString(3);
                    String type = resultSet.getString(4);
                    String iconURI = resultSet.getString(5);
                    String authorizedUser = resultSet.getString(6);
                    String resourceSetConsumerKey = resultSet.getString(7);
                    String[] resourceSetScope = UMAUtil.buildScopeArray(resultSet.getString(8));
                    Timestamp createdTime = resultSet.getTimestamp(9);

                    resourceSetDO = new ResourceSetDO(name, URI, type, resourceSetScope, iconURI);
                    resourceSetDO.setResourceSetId(resourceSetId);
                    resourceSetDO.setIconURI(iconURI);
                    resourceSetDO.setAuthorizedUser(authorizedUser);
                    resourceSetDO.setConsumerKey(resourceSetConsumerKey);
                    resourceSetDO.setCreatedTime(createdTime);
            }

            connection.commit();
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving resource set with ID : " +resourceSetId
                    + " registered with Client ID : " + consumerKey;
            throw new IdentityUMAException(errorMsg, e);
        } catch (IdentityException e) {
            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }

        return resourceSetDO;
    }



    public boolean removeResourceSet(String resourceSetId, String consumerKey, String userStoreDomain) throws IdentityUMAException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sql = getCorrectedSQL(SQLQueries.DELETE_RESOURCE_SET_FROM_ID,userStoreDomain);

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, resourceSetId);
            int rowsAffected = prepStmt.executeUpdate();
            connection.commit();

            return rowsAffected > 0 ?  true : false;

        } catch (IdentityException e) {

            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);

        } catch (SQLException e) {

            String errorMsg = "Error occurred while deleting resource set with ID : " +resourceSetId
                    + " registered with Client ID : " + consumerKey;
            throw new IdentityUMAException(errorMsg, e);

        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

    }


    public boolean updateResourceSet
            (String resourceSetId,ResourceSetDO newResourceSetDO, String consumerKey, String userStoreDomain) throws IdentityUMAException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sql = getCorrectedSQL(SQLQueries.UPDATE_RESOURCE_SET,userStoreDomain);

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            prepStmt = connection.prepareStatement(sql);

            String name = newResourceSetDO.getName();
            String URI = newResourceSetDO.getURI();
            String type = newResourceSetDO.getType();
            String iconURI = newResourceSetDO.getIconURI();
            String scopes = UMAUtil.buildScopeString(newResourceSetDO.getScopes());

            prepStmt.setString(1,name);
            prepStmt.setString(2,URI);
            prepStmt.setString(3,type);
            prepStmt.setString(4,iconURI);
            prepStmt.setString(5,scopes);
            prepStmt.setString(6,resourceSetId);

            int rowsAffected = prepStmt.executeUpdate();
            connection.commit();

            return rowsAffected > 0 ?  true : false;

        } catch (IdentityException e) {

            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);

        } catch (SQLException e) {

            String errorMsg = "Error occurred while update resource set with ID : " +resourceSetId
                    + " registered with Client ID : " + consumerKey;
            throw new IdentityUMAException(errorMsg, e);

        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }




    /**
     *  Util method to correct the table name in the query if a userStoreDomain is defined
     * @param sql Original SQL String
     * @param userStoreDomain
     * @return corrected SQL string ie. SQL with the table name corrected
     */
    private String getCorrectedSQL(String sql, String userStoreDomain){

        String resourceSetRegTable = IDN_UMA_PROTECTION_RESOURCE_SET;

        if (userStoreDomain != null && StringUtils.isNotEmpty(userStoreDomain)){
            resourceSetRegTable = resourceSetRegTable + "_" + userStoreDomain;
        }

        return sql.replaceAll("\\$resourceSetRegTable", resourceSetRegTable);
    }
}
