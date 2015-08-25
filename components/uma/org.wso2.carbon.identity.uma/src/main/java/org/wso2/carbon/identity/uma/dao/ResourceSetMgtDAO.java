/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.uma.UMAConstants;
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

    // tables to store the resource set descriptions
    private static final String IDN_UMA_RESOURCE_SET = "IDN_UMA_RESOURCE_SET";
    private static final String IDN_UMA_RESOURCE_SET_METADATA = "IDN_UMA_RESOURCE_SET_METADATA";
    private static final String IDN_UMA_RESOURCE_SCOPE_ASSOCIATION = "IDN_UMA_RESOURCE_SCOPE_ASSOCIATION";
    private static final String IDN_OAUTH2_ACCESS_TOKEN = "IDN_OAUTH2_ACCESS_TOKEN";


    private static final String RESOURCE_SET_SCOPES = UMAConstants.OAuthResourceSetRegistration.RESOURCE_SET_SCOPES;

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

        String sql = SQLQueries.INSERT_RESOURCE_SET;
        String sqlMetaData = SQLQueries.INSERT_RESOURCE_SET_METADATA;
        String sqlAddScopes = SQLQueries.INSERT_RESOURCE_SET_SCOPE;

        if (StringUtils.isNotBlank(userStoreDomain)) {
            sql =sql.replaceAll(IDN_UMA_RESOURCE_SET,IDN_UMA_RESOURCE_SET+"_"+userStoreDomain);
            sqlMetaData = sqlMetaData.replaceAll(IDN_UMA_RESOURCE_SET_METADATA,IDN_UMA_RESOURCE_SET_METADATA+"_"+userStoreDomain);
            sqlAddScopes = sqlAddScopes.replaceAll(IDN_UMA_RESOURCE_SCOPE_ASSOCIATION,
                    IDN_UMA_RESOURCE_SCOPE_ASSOCIATION+"_"+userStoreDomain);
        }

        String resourceSetId =  resourceSetDO.getResourceSetId();
        String accessTokenId = resourceSetDO.getTokenId();

        try {
            // Insert resource set data
            preparedStatement = connection.prepareStatement(sql);

            preparedStatement.setString(1,resourceSetId);
            preparedStatement.setTimestamp(2,resourceSetDO.getTimeCreated());
            preparedStatement.setString(3, accessTokenId);

            preparedStatement.execute();

            // Insert Resource Set Metadata
            preparedStatement = connection.prepareStatement(sqlMetaData);
            Map<String,Object> metaData = resourceSetDO.getMetadata();

            for (Map.Entry<String,Object> metaDataEntry : metaData.entrySet()){
                // if the key is scopes we skip it since we will deal with them later
                if(StringUtils.equals(metaDataEntry.getKey(),RESOURCE_SET_SCOPES)){
                    continue;
                }

                preparedStatement.setString(1,resourceSetId);
                preparedStatement.setString(2,metaDataEntry.getKey());
                preparedStatement.setString(3, (String) metaDataEntry.getValue());

                preparedStatement.addBatch();

            }
            // execute to persist the metadata
            preparedStatement.executeBatch();


            // Insert Resource Set Scopes
            preparedStatement = connection.prepareStatement(sqlAddScopes);
            if (resourceSetDO.getScopes() != null && resourceSetDO.getScopes().length > 0){

                for (String scope : resourceSetDO.getScopes()){
                    preparedStatement.setString(1,resourceSetId);
                    preparedStatement.setString(2,scope);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }else{
                preparedStatement.setString(1,resourceSetId);
                preparedStatement.setString(2,"");
                preparedStatement.execute();
            }

        } catch (SQLIntegrityConstraintViolationException ex){

            String errorMsg = "ResourceSet with ID: "+resourceSetId+" and scopes: "+
                    UMAUtil.buildScopeString(resourceSetDO.getScopes())+" already exists";
            throw new IdentityUMAException(errorMsg, ex);

        }catch (DataTruncation e) {

            throw new IdentityUMAException("Invalid request", e);

        } catch (SQLException e) {
            log.error("Error when storing the resource set description for " + resourceSetDO.getName());
            throw new IdentityUMAException(e.getMessage());
        }

    }

//
//
//    public Set<ResourceSetDO> retrieveResourceSets(String consumerKey, String userStoreDomain) throws IdentityUMAException {
//
//        Connection connection;
//
//        try {
//            connection = JDBCPersistenceManager.getInstance().getDBConnection();
//        } catch (IdentityException e) {
//
//            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
//                    "store connection", e);
//        }
//
//        PreparedStatement prepStmt = null;
//        ResultSet resultSet = null;
//        Map<String, ResourceSetDO> resourceSetDOMap = new HashMap<>();
//
//        String sql = addUserStoreDomainToSQL(SQLQueries.GET_ALL_RESOURCE_SETS, userStoreDomain);
//
//        try {
//
//            prepStmt = connection.prepareStatement(sql);
//            prepStmt.setString(1,consumerKey);
//
//            resultSet = prepStmt.executeQuery();
//
//            while (resultSet.next()){
//
//                String resourceSetId = resultSet.getString(1);
//
//                if (resourceSetDOMap.get(resourceSetId) == null) {
//
//                    String name = resultSet.getString(2);
//                    String URI = resultSet.getString(3);
//                    String type = resultSet.getString(4);
//                    String iconURI = resultSet.getString(5);
//                    String authorizedUser = resultSet.getString(6);
//                    String resourceSetConsumerKey = resultSet.getString(7);
//                    String[] resourceSetScope = UMAUtil.buildScopeArray(resultSet.getString(8));
//                    Timestamp createdTime = resultSet.getTimestamp(9);
//
//                    ResourceSetDO resourceSetDO = new ResourceSetDO(name, URI, type, resourceSetScope, iconURI);
//                    resourceSetDO.setIconURI(iconURI);
//                    resourceSetDO.setAuthorizedUser(authorizedUser);
//                    resourceSetDO.setConsumerKey(resourceSetConsumerKey);
//                    resourceSetDO.setCreatedTime(createdTime);
//
//                    resourceSetDOMap.put(resourceSetId,resourceSetDO);
//                }
//            }
//
//            connection.commit();
//        } catch (SQLException e) {
//            String errorMsg = "Error occurred while retrieving registred resource sets for " +
//                    "Client ID : " + consumerKey;
//            throw new IdentityUMAException(errorMsg, e);
//        }finally {
//            IdentityDatabaseUtil.closeAllConnections(connection,resultSet,prepStmt);
//        }
//
//        return new HashSet<>(resourceSetDOMap.values());
//    }

    /**
     *  Retrieve all the resource set IDs registred for a particular consumerKey
     * @param userStoreDomain
     * @param accessTokenId
     * @return
     * @throws IdentityUMAException
     */
    public List<String> retrieveResourceSetIDs(String accessTokenId, String userStoreDomain) throws IdentityUMAException {
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

        String sql = SQLQueries.GET_RESOURCE_SET_IDS;
        if (StringUtils.isNotBlank(userStoreDomain)){
            sql = sql.replaceAll(IDN_UMA_RESOURCE_SET, IDN_UMA_RESOURCE_SET+"_"+userStoreDomain)
                    .replaceAll(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN+"_"+userStoreDomain);
        }


        try {
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1,accessTokenId);

            resultSet = prepStmt.executeQuery();

            while (resultSet.next()){
                String resourceSetId = resultSet.getString(1);
                if (StringUtils.isNotEmpty(resourceSetId) && !resourceSetIdList.contains(resourceSetId)){
                    resourceSetIdList.add(resourceSetId);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving registered resource set IDs for User:";
            throw new IdentityUMAException(errorMsg, e);
        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }

        return resourceSetIdList;
    }

    /**
     *
     * @param resourceSetId
     * @param userStoreDomain
     * @return
     * @throws IdentityUMAException
     */
    public ResourceSetDO retrieveResourceSet(String resourceSetId,String accessTokenId,String userStoreDomain) throws IdentityUMAException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        ResourceSetDO resourceSetDO = null;
        List<String> resourceScopes = new ArrayList<>();
        Map<String,Object> metadataMap= new HashMap<>();


        String sql = SQLQueries.GET_RESOURCE_SET_FROM_ID;
        if (StringUtils.isNotBlank(userStoreDomain)){
            sql = sql.replaceAll(IDN_UMA_RESOURCE_SET, IDN_UMA_RESOURCE_SET+"_"+userStoreDomain)
                    .replaceAll(IDN_UMA_RESOURCE_SET_METADATA,IDN_UMA_RESOURCE_SET_METADATA+"_"+userStoreDomain)
                    .replaceAll(IDN_UMA_RESOURCE_SCOPE_ASSOCIATION,
                            IDN_UMA_RESOURCE_SCOPE_ASSOCIATION + "_" + userStoreDomain);
        }


        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1,resourceSetId);
            prepStmt.setString(2,accessTokenId);

            resultSet = prepStmt.executeQuery();

            while(resultSet.next()){
                if (resourceSetDO == null){

                    String resourceSetID = resultSet.getString(1);
                    Timestamp timeCreated = resultSet.getTimestamp(2);
                    String tokenId = resultSet.getString(3);

                    String propertyKey = resultSet.getString(4);
                    String propertyValue = resultSet.getString(5);

                    String scope = resultSet.getString(6);

                    resourceSetDO = new ResourceSetDO();
                    resourceSetDO.setResourceSetId(resourceSetID);
                    resourceSetDO.setTimeCreated(timeCreated);
                    resourceSetDO.setTokenId(tokenId);

                    // add metadata
                    if (StringUtils.isNotBlank(propertyKey)){
                        metadataMap.put(propertyKey,propertyValue);
                    }

                    if (StringUtils.isNotBlank(scope)){
                        resourceScopes.add(scope);
                    }

                }else{
                    String propertyKey = resultSet.getString(4);
                    String propertyValue = resultSet.getString(5);
                    String scope = resultSet.getString(6);

                    if (StringUtils.isNotBlank(propertyKey) && !metadataMap.containsKey(propertyKey)){
                        metadataMap.put(propertyKey,propertyValue);
                    }

                    if (StringUtils.isNotBlank(scope) && !resourceScopes.contains(scope)){
                        resourceScopes.add(scope);
                    }
                }
            }

            if (resourceSetDO != null){
                resourceSetDO.setMetadata(metadataMap);
                resourceSetDO.setScopes(resourceScopes.toArray(new String[resourceScopes.size()]));
            }

            connection.commit();
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving resource set with ID : " +resourceSetId
                    + " registered with Client ID : <FILL>";
            throw new IdentityUMAException(errorMsg, e);
        } catch (IdentityException e) {
            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }

        return resourceSetDO;
    }



    public boolean removeResourceSet(String resourceSetId, String accessTokenId, String userStoreDomain) throws IdentityUMAException {

        Connection connection = null;
        PreparedStatement prepStmt = null;

        String sql = SQLQueries.DELETE_RESOURCE_SET_BY_ID;

        if (StringUtils.isNotBlank(userStoreDomain)){
            sql = sql.replaceAll(IDN_UMA_RESOURCE_SET, IDN_UMA_RESOURCE_SET+"_"+userStoreDomain)
                    .replaceAll(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN+"_"+userStoreDomain);
        }

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, resourceSetId);
            prepStmt.setString(2, accessTokenId);

            int rowsAffected = prepStmt.executeUpdate();
            connection.commit();

            return rowsAffected > 0;

        } catch (IdentityException e) {

            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);

        } catch (SQLException e) {

            String errorMsg = "Error occurred while deleting resource set with ID : " +resourceSetId;
            throw new IdentityUMAException(errorMsg, e);

        }finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

    }


    /**
     * Method to update(replace) a previously registered resourceSet with a new one
     * @param resourceSetId
     * @param newResourceSetDO
     * @param userStoreDomain
     * @return
     * @throws IdentityUMAException
     */
    public boolean updateResourceSet
            (String resourceSetId,ResourceSetDO newResourceSetDO, String userStoreDomain) throws IdentityUMAException {

        boolean isUpdateSuccessful = false;
        try {
            String accessTokenId = newResourceSetDO.getTokenId();
            // delete the old resource set
            boolean isDeleteSuccess = removeResourceSet(resourceSetId,accessTokenId,userStoreDomain);

            // if the delete is successful the resourceSet existed and the user trying to update it has authorization
            // to do so
            if (isDeleteSuccess){
                // we replace the previous resource set by inserting a new resource set with the same resource set Id
                saveResourceSetDescription(newResourceSetDO,userStoreDomain);
                isUpdateSuccessful = true;
            }
            return isUpdateSuccessful;
        } catch (IdentityException e) {

            throw new IdentityUMAException("Error occurred while getting Identity persistence " +
                    "store connection", e);

        }

    }


}
