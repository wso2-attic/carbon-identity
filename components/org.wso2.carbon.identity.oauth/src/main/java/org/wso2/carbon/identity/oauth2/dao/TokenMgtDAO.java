/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth2.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.tokenprocessor.PlainTextPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.AuthzCodeDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Data Access Layer functionality for Token management in OAuth 2.0 implementation. This includes
 * storing and retrieving access tokens, authorization codes and refresh tokens.
 */
public class TokenMgtDAO {

    private static TokenPersistenceProcessor persistenceProcessor;

    private static int maxPoolSize = 100;

    private static int tokenPersistRetryCount = 5;

    private boolean enablePersist = true;

    private static BlockingDeque<AccessContextTokenDO> accessContextTokenQueue = new LinkedBlockingDeque<AccessContextTokenDO>();

    private static BlockingDeque<AuthContextTokenDO> authContextTokenQueue = new LinkedBlockingDeque<AuthContextTokenDO>();

    private static final Log log = LogFactory.getLog(TokenMgtDAO.class);

    private static final String IDN_OAUTH2_ACCESS_TOKEN = "IDN_OAUTH2_ACCESS_TOKEN";

    static {

        try {
            maxPoolSize =
                    Integer.parseInt(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.PoolSize"));
        } catch (Exception e) {
        }

        if (maxPoolSize > 0) {
            log.info("Thread pool size for session persistent consumer : " + maxPoolSize);

            ExecutorService threadPool = Executors.newFixedThreadPool(maxPoolSize);

            for (int i = 0; i < maxPoolSize; i++) {
                threadPool.execute(new TokenPersistenceTask(accessContextTokenQueue));
            }

            threadPool = Executors.newFixedThreadPool(maxPoolSize);

            for (int i = 0; i < maxPoolSize; i++) {
                threadPool.execute(new AuthPersistenceTask(authContextTokenQueue));
            }
        }

    }


    public TokenMgtDAO() {
        try {
            persistenceProcessor = OAuthServerConfiguration.getInstance().getPersistenceProcessor();
        } catch (IdentityOAuth2Exception e) {
            log.error("Error retrieving TokenPersistenceProcessor. Defaulting to PlainTextProcessor");
            persistenceProcessor = new PlainTextPersistenceProcessor();
        }

        if(IdentityUtil.getProperty("JDBCPersistenceManager.TokenPersist.Enable") != null){
            enablePersist= Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.TokenPersist.Enable"));
        }

        if(IdentityUtil.getProperty("OAuth.TokenPersistence.RetryCount") != null){
            tokenPersistRetryCount = Integer.parseInt(IdentityUtil.getProperty("OAuth.TokenPersistence.RetryCount"));
        }
    }

    public void storeAuthorizationCode(String authzCode, String consumerKey, String callbackUrl,
                                       AuthzCodeDO authzCodeDO) throws IdentityOAuth2Exception {

        if(!enablePersist){
            return;
        }

        if (maxPoolSize > 0){
            authContextTokenQueue.push(new AuthContextTokenDO(authzCode, consumerKey, callbackUrl, authzCodeDO));
        } else {
            persistAuthorizationCode(authzCode, consumerKey, callbackUrl, authzCodeDO);
        }
    }

    public void persistAuthorizationCode(String authzCode, String consumerKey, String callbackUrl,
                                       AuthzCodeDO authzCodeDO) throws IdentityOAuth2Exception {

        if(!enablePersist){
            return;
        }

        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.STORE_AUTHORIZATION_CODE);
            prepStmt.setString(1, persistenceProcessor.getProcessedAuthzCode(authzCode));
            prepStmt.setString(2, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(3, callbackUrl);
            prepStmt.setString(4, OAuth2Util.buildScopeString(authzCodeDO.getScope()));
            prepStmt.setString(5, authzCodeDO.getAuthorizedUser().toLowerCase());
            prepStmt.setTimestamp(6, authzCodeDO.getIssuedTime(),
                    Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            prepStmt.setLong(7, authzCodeDO.getValidityPeriod());
            prepStmt.execute();
            connection.commit();
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.STORE_AUTHORIZATION_CODE);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when storing the authorization code for consumer key : " + consumerKey);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void storeAccessToken(String accessToken, String consumerKey,
                                 AccessTokenDO accessTokenDO, Connection connection,
                                 String userStoreDomain) throws IdentityOAuth2Exception {

        if(!enablePersist){
            return;
        }

        storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain, 0);
    }

    private void storeAccessToken(String accessToken, String consumerKey, AccessTokenDO accessTokenDO,
                                 Connection connection, String userStoreDomain, int retryAttempt)
            throws IdentityOAuth2Exception {

        PreparedStatement prepStmt = null;
        String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (userStoreDomain != null) {
            accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
        }
        String sql = "INSERT INTO " +
                accessTokenStoreTable +
                " (ACCESS_TOKEN, REFRESH_TOKEN, CONSUMER_KEY, AUTHZ_USER, TIME_CREATED, " +
                "VALIDITY_PERIOD, TOKEN_SCOPE, TOKEN_STATE, USER_TYPE) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";
        try {
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedAccessTokenIdentifier(accessToken));
            if(accessTokenDO.getRefreshToken() != null){
                prepStmt.setString(2, persistenceProcessor.getProcessedRefreshToken(accessTokenDO.getRefreshToken()));
            } else {
                prepStmt.setString(2, accessTokenDO.getRefreshToken());
            }
            prepStmt.setString(3, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(4, accessTokenDO.getAuthzUser().toLowerCase());
            prepStmt.setTimestamp(5, accessTokenDO.getIssuedTime(), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            prepStmt.setLong(6, accessTokenDO.getValidityPeriod() * 1000);
            prepStmt.setString(7, OAuth2Util.buildScopeString(accessTokenDO.getScope()));
            prepStmt.setString(8, accessTokenDO.getTokenState());
            prepStmt.setString(9, accessTokenDO.getTokenType());
            prepStmt.execute();
            connection.commit();
            if(retryAttempt > 0) {
                log.info("Successfully recovered 'CON_APP_KEY' constraint violation with the attempt : " +
                        retryAttempt);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            if (retryAttempt >= tokenPersistRetryCount) {
                log.error("'CON_APP_KEY' constrain violation retry count exceeds above the maximum count - " +
                        tokenPersistRetryCount);
                String errorMsg = "Access Token for consumer key : " + consumerKey + ", user : " +
                        accessTokenDO.getAuthzUser().toLowerCase() + " and scope : " +
                        OAuth2Util.buildScopeString(accessTokenDO.getScope()) + "already exists";
                throw new IdentityOAuth2Exception(errorMsg, e);
            }

            IdentityDatabaseUtil.closeAllConnections(null, null, prepStmt);
            recoverFromConAppKeyConstraintViolation(accessToken, consumerKey, accessTokenDO,
                    connection, userStoreDomain, retryAttempt + 1);
        } catch (DataTruncation e) {
            throw new IdentityOAuth2Exception("Invalid request",e);
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception(
                    "Error when storing the access token for consumer key : " + consumerKey, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(null, null, prepStmt);
        }
    }

    public void storeAccessToken(String accessToken, String consumerKey,
                                 AccessTokenDO accessTokenDO, String userStoreDomain)
            throws IdentityException {

        if(!enablePersist){
            return;
        }
        if (maxPoolSize > 0){
            accessContextTokenQueue.push(new AccessContextTokenDO(accessToken, consumerKey, accessTokenDO, userStoreDomain));
        } else {
            persistAccessToken(accessToken, consumerKey, accessTokenDO, userStoreDomain);
        }
    }

    public boolean persistAccessToken(String accessToken, String consumerKey,
                                    AccessTokenDO accessTokenDO,
                                    String userStoreDomain) throws IdentityException {

        if(!enablePersist){
            return false;
        }
        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain);
            return true;
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
    }

    public AccessTokenDO retrieveLatestAccessToken(String consumerKey, String userName,
                                                   String userStoreDomain, String scope,
                                                   boolean includeExpiredTokens)
            throws IdentityOAuth2Exception {

        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while getting Identity persistence " +
                    "store connection", e);
        }

        try {

            String sql = null;
            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MYSQL;
            }
            else if(connection.getMetaData().getDatabaseProductName().contains("DB2")){
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_DB2;
            }
            else if(connection.getMetaData().getDriverName().contains("MS SQL")){
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_POSTGRESQL;
            } else {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_ORACLE;
            }

            if(StringUtils.isEmpty(scope)){
                sql = sql.replace("TOKEN_SCOPE=?","(TOKEN_SCOPE='' OR TOKEN_SCOPE IS NULL)");
            }

            if(StringUtils.isNotEmpty(userStoreDomain)){
                //logic to store access token into different tables when multiple user stores are configured.
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN,IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }

            PreparedStatement prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName.toLowerCase());
            if(StringUtils.isNotEmpty(scope)){
                prepStmt.setString(3, scope);
            }
            ResultSet resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                boolean returnToken = false;
                String tokenState = resultSet.getString(5);
                if(includeExpiredTokens){
                    if(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(tokenState) ||
                            OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED.equals(tokenState)){
                        returnToken = true;
                    }
                } else {
                    if(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(tokenState)){
                        returnToken = true;
                    }
                }
                if(returnToken) {
                    String accessToken = persistenceProcessor.getPreprocessedAccessTokenIdentifier(resultSet.getString(1));
                    String refreshToken = null;
                    if (resultSet.getString(2) != null) {
                        refreshToken = persistenceProcessor.getPreprocessedRefreshToken(resultSet.getString(2));
                    }
                    long issuedTime = resultSet.getTimestamp(3,
                            Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                    long validityPeriod = resultSet.getLong(4);
                    String userType = resultSet.getString(6);
                    // data loss at dividing the validity period but can be neglected
                    AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, userName,
                            OAuth2Util.buildScopeArray(scope), new Timestamp(issuedTime),
                            validityPeriod / 1000, userType);
                    accessTokenDO.setAccessToken(accessToken);
                    accessTokenDO.setRefreshToken(refreshToken);
                    accessTokenDO.setTokenState(tokenState);
                    return accessTokenDO;
                }
            }
            return null;
        } catch (SQLException e) {
            String errorMsg = "Error occurred while trying to retrieve latest 'ACTIVE' " +
                    "access token for Client ID : " + consumerKey + ", User ID : " + userName +
                    " and  Scope : " + scope;
            if(includeExpiredTokens){
                errorMsg = errorMsg.replace("ACTIVE","ACTIVE or EXPIRED");
            }
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
    }

    public Set<AccessTokenDO> retrieveAccessTokens(String consumerKey, String userName,
                                                    String userStoreDomain, boolean includeExpired)
            throws IdentityOAuth2Exception{

        Set<AccessTokenDO> accessTokenDOs = new HashSet<AccessTokenDO>();

        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while getting Identity persistence " +
                    "store connection", e);
        }

        PreparedStatement prepStmt = null;
        try {
            String sql = SQLQueries.RETRIEVE_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER;
            if(includeExpired){
                sql = SQLQueries.RETRIEVE_ACTIVE_EXPIRED_ACCESS_TOKEN_BY_CLIENT_ID_USER;
            }
            if(StringUtils.isNotEmpty(userStoreDomain)) {
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName);
            ResultSet resultSet = prepStmt.executeQuery();

            while (resultSet.next()) {
                String accessToken = persistenceProcessor.
                        getPreprocessedAccessTokenIdentifier(resultSet.getString(1));
                String refreshToken = persistenceProcessor.
                        getPreprocessedRefreshToken(resultSet.getString(2));
                String[] scope = OAuth2Util.buildScopeArray(resultSet.getString(3));
                Timestamp timestamp = resultSet.getTimestamp(4,
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                long validityPeriod = resultSet.getLong(5);
                String tokenType = resultSet.getString(6);

                AccessTokenDO dataDO = new AccessTokenDO(consumerKey, userName, scope, timestamp,
                        validityPeriod / 1000, tokenType);
                dataDO.setAccessToken(accessToken);
                dataDO.setRefreshToken(refreshToken);

                accessTokenDOs.add(dataDO);
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance";
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving 'ACTIVE' access tokens for " +
                    "Client ID : " + consumerKey + " and User ID : " + userName;
            if(includeExpired){
                errorMsg = errorMsg.replace("ACTIVE","ACTIVE or EXPIRED");
            }
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return accessTokenDOs;
    }

    public AuthzCodeDO validateAuthorizationCode(String consumerKey, String authorizationKey) throws IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.VALIDATE_AUTHZ_CODE);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, persistenceProcessor.getProcessedAuthzCode(authorizationKey));
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                String authorizedUser = resultSet.getString(1);
                String scopeString = resultSet.getString(2);
                String callbackUrl = resultSet.getString(3);
                Timestamp issuedTime = resultSet.getTimestamp(4,
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                long validityPeriod = resultSet.getLong(5);

                return new AuthzCodeDO(authorizedUser,
                        OAuth2Util.buildScopeArray(scopeString),
                        issuedTime, validityPeriod, callbackUrl);
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.VALIDATE_AUTHZ_CODE);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when validating an authorization code", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

        return null;
    }

    public void cleanUpAuthzCode(String authzCode) throws IdentityOAuth2Exception{
        if (maxPoolSize > 0){
            authContextTokenQueue.push(new AuthContextTokenDO(authzCode, null, null, null));
        } else {
            removeAuthzCode(authzCode);
        }
    }

    public void removeAuthzCode(String authzCode) throws IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.REMOVE_AUTHZ_CODE);
            prepStmt.setString(1, persistenceProcessor.getPreprocessedAuthzCode(authzCode));

            prepStmt.execute();
            connection.commit();
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.REMOVE_AUTHZ_CODE);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when cleaning up an authorization code", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public RefreshTokenValidationDataDO validateRefreshToken(String consumerKey, String refreshToken)
            throws IdentityOAuth2Exception {

        RefreshTokenValidationDataDO validationDataDO = new RefreshTokenValidationDataDO();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String userStoreDomain = null;
        String sql = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            if(OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromAccessToken(refreshToken);
            }
            String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }

            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = SQLQueries.VALIDATE_REFRESH_TOKEN_MYSQL.replaceAll("\\$accessTokenStoreTable", accessTokenStoreTable);
            } else if(connection.getMetaData().getDatabaseProductName().contains("DB2")){
                sql = SQLQueries.VALIDATE_REFRESH_TOKEN_DB2.replaceAll("\\$accessTokenStoreTable", accessTokenStoreTable);
            } else if (connection.getMetaData().getDriverName().contains("MS SQL")) {
                sql = SQLQueries.VALIDATE_REFRESH_TOKEN_MSSQL.replaceAll("\\$accessTokenStoreTable", accessTokenStoreTable);
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = SQLQueries.VALIDATE_REFRESH_TOKEN_MSSQL.replaceAll("\\$accessTokenStoreTable", accessTokenStoreTable);
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = SQLQueries.VALIDATE_REFRESH_TOKEN_POSTGRESQL.replaceAll("\\$accessTokenStoreTable", accessTokenStoreTable);
            } else {
                sql = SQLQueries.VALIDATE_REFRESH_TOKEN_ORACLE.replaceAll("\\$accessTokenStoreTable", accessTokenStoreTable);
            }

            if(refreshToken == null){
                sql = sql.replace("REFRESH_TOKEN = ?","REFRESH_TOKEN IS NULL");
            }

            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            if(refreshToken != null){
                prepStmt.setString(2, persistenceProcessor.getProcessedRefreshToken(refreshToken));
            }
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                validationDataDO.setAccessToken(persistenceProcessor.getPreprocessedAccessTokenIdentifier(resultSet.getString(1)));
                validationDataDO.setAuthorizedUser(resultSet.getString(2));
                validationDataDO.setScope(OAuth2Util.buildScopeArray(resultSet.getString(3)));
                validationDataDO.setRefreshTokenState(resultSet.getString(4));
                Timestamp timestamp = resultSet.getTimestamp(5,Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                validationDataDO.setIssuedAt(timestamp.getTime());
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when validating a refresh token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

        return validationDataDO;
    }

    public void cleanUpAccessToken(String accessToken) throws IdentityOAuth2Exception{
        if (maxPoolSize > 0){
            accessContextTokenQueue.push(new AccessContextTokenDO(accessToken, null, null, null));
        } else {
            removeAccessToken(accessToken);
        }
    }

    public void removeAccessToken(String accessToken) throws IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String userStoreDomain = null;
        String sql = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            if(OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromAccessToken(accessToken);
            }

            String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }
            sql = "DELETE FROM " + accessTokenStoreTable + " WHERE ACCESS_TOKEN = ? ";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, persistenceProcessor.getProcessedAccessTokenIdentifier(accessToken));
            prepStmt.execute();
            connection.commit();

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when cleaning up an access token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public AccessTokenDO retrieveAccessToken(String accessTokenIdentifier, boolean includeExpired)
            throws IdentityOAuth2Exception {

        AccessTokenDO dataDO = null;
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String userStoreDomain = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();

            //select the user store domain when multiple user stores are configured.
            if(OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromAccessToken(accessTokenIdentifier);
            }
            String sql = SQLQueries.RETRIEVE_ACTIVE_ACCESS_TOKEN;
            if(includeExpired){
                sql = SQLQueries.RETRIEVE_ACTIVE_EXPIRED_ACCESS_TOKEN;
            }
            if(StringUtils.isNotEmpty(userStoreDomain)) {
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, persistenceProcessor.getProcessedAccessTokenIdentifier(accessTokenIdentifier));
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                String consumerKey = persistenceProcessor.getPreprocessedClientId(resultSet.getString(1));
                String authorizedUser = resultSet.getString(2);
                String[] scope = OAuth2Util.buildScopeArray(resultSet.getString(3));
                Timestamp timestamp = resultSet.getTimestamp(4,
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                long validityPeriod = resultSet.getLong(5);
                String tokenType = resultSet.getString(6);
                String refreshToken = resultSet.getString(7);
                dataDO = new AccessTokenDO(consumerKey, authorizedUser, scope, timestamp, validityPeriod / 1000, tokenType);
                dataDO.setAccessToken(accessTokenIdentifier);
                dataDO.setRefreshToken(refreshToken);
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance";
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error when retrieving Access Token : " + accessTokenIdentifier, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

        return dataDO;
    }


    /**
     * Sets state of access token
     *
     * @param accessToken
     * @param tokenState
     * @throws IdentityOAuth2Exception
     */
    public void setAccessTokenState(String accessToken, String tokenState, String tokenStateId,
                                    String userStoreDomain) throws IdentityOAuth2Exception {

        Connection connection;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while trying to get a Identity " +
                    "persistence store instance");
        }

        try {
            setAccessTokenState(accessToken, tokenState, tokenStateId, userStoreDomain, connection);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
    }

    private void setAccessTokenState(String accessToken, String tokenState, String tokenStateId,
                                     String userStoreDomain, Connection connection) throws IdentityOAuth2Exception {

        PreparedStatement prepStmt = null;
        try {
            String sql = SQLQueries.UPDATE_TOKE_STATE;
            if (StringUtils.isNotEmpty(userStoreDomain)) {
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, tokenState);
            prepStmt.setString(2, tokenStateId);
            prepStmt.setString(3, persistenceProcessor.getProcessedAccessTokenIdentifier(accessToken));
            prepStmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error while updating Access Token : " +
                    accessToken + " to Token State : " + tokenState, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(null, null, prepStmt);
        }
    }

    /**
     * This method is to revoke specific tokens
     *
     * @param token token that needs to be revoked
     * @throws IdentityOAuth2Exception if failed to revoke the access token
     */
    public void revokeToken(String token) throws IdentityOAuth2Exception {

        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = OAuth2Util.getAccessTokenStoreTableFromAccessToken(token);
            }
            String sqlQuery = SQLQueries.REVOKE_ACCESS_TOKEN.replace(
                    IDN_OAUTH2_ACCESS_TOKEN, accessTokenStoreTable);
            connection = IdentityDatabaseUtil.getDBConnection();
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, OAuthConstants.TokenStates.TOKEN_STATE_REVOKED);
            ps.setString(2, UUID.randomUUID().toString());
            ps.setString(3, persistenceProcessor.getProcessedAccessTokenIdentifier(token));
            int count = ps.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Number of rows being updated : " + count);
            }
            connection.commit();
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollBack(connection);
            throw new IdentityOAuth2Exception("Error occurred while revoking Access Token : " + token, e);
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while revoking Access Token : " + token, e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * This method is to list the application authorized by OAuth resource owners
     *
     * @param authzUser  username of the resource owner
     * @return set of distinct client IDs authorized by user until now
     * @throws IdentityOAuth2Exception if failed to update the access token
     */
    public Set<String> getAllTimeAuthorizedClientIds(String authzUser) throws IdentityOAuth2Exception {

        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        PreparedStatement ps = null;
        Connection connection = null;
        Set<String> distinctConsumerKeys = new HashSet<String>();
        try {
            try {
                connection = IdentityDatabaseUtil.getDBConnection();
            } catch (IdentityException e) {
                throw new IdentityOAuth2Exception(
                        "Error occurred while trying to get an Identity persistence store", e);
            }
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = OAuth2Util.getAccessTokenStoreTableFromUserId(authzUser);
            }
            String sqlQuery = SQLQueries.GET_DISTINCT_APPS_AUTHORIZED_BY_USER_ALL_TIME.replace(
                    IDN_OAUTH2_ACCESS_TOKEN,accessTokenStoreTable);
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, authzUser.toLowerCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String consumerKey = persistenceProcessor.getPreprocessedClientId(rs.getString(1));
                distinctConsumerKeys.add(consumerKey);
            }
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception(
                    "Error occurred while retrieving all distinct Client IDs authorized by " +
                            "User ID : " + authzUser + " until now", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return distinctConsumerKeys;
    }

    public String findScopeOfResource(String resourceUri) throws IdentityOAuth2Exception {

        Connection connection = null;
        PreparedStatement ps;
        ResultSet rs;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            String sql = "SELECT IOS.SCOPE_KEY " +
                         "FROM IDN_OAUTH2_SCOPE IOS, IDN_OAUTH2_RESOURCE_SCOPE IORS " +
                         "WHERE RESOURCE_PATH = ? " +
                         "AND IORS.SCOPE_ID = IOS.SCOPE_ID";

            ps = connection.prepareStatement(sql);
            ps.setString(1, resourceUri);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("SCOPE_KEY");
            }
            return null;
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            String errorMsg = "Error getting scopes for resource - " + resourceUri + " : " + e.getMessage();
            log.error(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
        return null;
    }

    public boolean validateScope(Connection connection, String accessToken, String resourceUri){
        return false;
    }

    /**
     * Revoke the OAuth Consent which is recorded in the IDN_OPENID_USER_RPS table against the user for a particular
     * Application
     * @param username - Username of the Consent owner
     * @param applicationName - Name of the OAuth App
     * @throws org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception - If an unexpected error occurs.
     */
    public void revokeOAuthConsentByApplicationAndUser(String username, String applicationName) throws IdentityOAuth2Exception {

        if(username == null || applicationName == null){
            log.error("Could not remove consent of user " + username + " for application " + applicationName);
            return;
        }

        Connection connection = null;
        PreparedStatement ps;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            connection.setAutoCommit(false);

            String sql = "DELETE FROM IDN_OPENID_USER_RPS " +
                    "WHERE USER_NAME = ? " +
                    "AND RP_URL = ?";

            ps = connection.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, applicationName);
            ps.execute();
            connection.commit();

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            String errorMsg = "Error deleting OAuth consent of Application " + applicationName + " and User " + username
                    + e.getMessage();
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
    }

    private void recoverFromConAppKeyConstraintViolation(String accessToken, String consumerKey, AccessTokenDO
            accessTokenDO, Connection connection, String userStoreDomain, int retryAttempt)
            throws IdentityOAuth2Exception {

        log.warn("Retry attempt to recover 'CON_APP_KEY' constraint violation : " + retryAttempt);

        AccessTokenDO latestNonActiveToken = retrieveLatestToken(consumerKey, accessTokenDO.getAuthzUser(),
                userStoreDomain, OAuth2Util.buildScopeString(accessTokenDO.getScope()), false, connection);

        AccessTokenDO latestActiveToken = retrieveLatestToken(consumerKey, accessTokenDO.getAuthzUser(),
                userStoreDomain, OAuth2Util.buildScopeString(accessTokenDO.getScope()), true, connection);

        if (latestActiveToken != null) {
            if (latestNonActiveToken == null ||
                    latestActiveToken.getIssuedTime().after(latestNonActiveToken.getIssuedTime())) {
                if (maxPoolSize == 0) {
                    // In here we can use existing token since we have a synchronised communication
                    accessTokenDO.setAccessToken(latestActiveToken.getAccessToken());
                    accessTokenDO.setIssuedTime(latestActiveToken.getIssuedTime());
                    accessTokenDO.setRefreshToken(latestActiveToken.getRefreshToken());
                    accessTokenDO.setValidityPeriod(latestActiveToken.getValidityPeriod());
                    accessTokenDO.setValidityPeriodInMillis(latestActiveToken.getValidityPeriod() * 1000);
                    log.info("Successfully recovered 'CON_APP_KEY' constraint violation with the attempt : " +
                            retryAttempt);
                } else {
                    // In here we have to use new token since we have asynchronous communication. User already
                    // received that token

                    // Inactivate latest active token.
                    setAccessTokenState(latestActiveToken.getAccessToken(), "INACTIVE", UUID.randomUUID().toString(),
                            userStoreDomain, connection);

                    // Update token issued time & try to store it again.
                    accessTokenDO.setIssuedTime(new Timestamp(new Date().getTime()));
                    storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain,
                            retryAttempt);
                }
            } else {
                // Inactivate latest active token.
                setAccessTokenState(latestActiveToken.getAccessToken(), "INACTIVE", UUID.randomUUID().toString(),
                        userStoreDomain, connection);

                // Update token issued time & try to store it again.
                accessTokenDO.setIssuedTime(new Timestamp(new Date().getTime()));
                storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain, retryAttempt);
            }
        } else {
            // In this case another process already updated the latest active token to inactive.

            // Update token issued time & try to store it again.
            accessTokenDO.setIssuedTime(new Timestamp(new Date().getTime()));
            storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain, retryAttempt);
        }
    }


    private AccessTokenDO retrieveLatestToken(String consumerKey, String userName, String userStoreDomain, String scope,
                                             boolean active, Connection connection) throws IdentityOAuth2Exception {

        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        try {

            String sql;
            if (active) {
                if (connection.getMetaData().getDriverName().contains("MySQL")
                        || connection.getMetaData().getDriverName().contains("H2")
                        || connection.getMetaData().getDatabaseProductName().contains("DB2")) {
                    sql = SQLQueries.RETRIEVE_LATEST_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MYSQL;
                } else if (connection.getMetaData().getDriverName().contains("MS SQL")) {
                    sql = SQLQueries.RETRIEVE_LATEST_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
                } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                    sql = SQLQueries.RETRIEVE_LATEST_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
                } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                    sql = SQLQueries.RETRIEVE_LATEST_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_POSTGRESQL;
                } else {
                    sql = SQLQueries.RETRIEVE_LATEST_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_ORACLE;
                }
            } else {
                if (connection.getMetaData().getDriverName().contains("MySQL")
                        || connection.getMetaData().getDriverName().contains("H2")
                        || connection.getMetaData().getDatabaseProductName().contains("DB2")) {
                    sql = SQLQueries.RETRIEVE_LATEST_NON_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MYSQL;
                } else if (connection.getMetaData().getDriverName().contains("MS SQL")) {
                    sql = SQLQueries.RETRIEVE_LATEST_NON_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
                } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                    sql = SQLQueries.RETRIEVE_LATEST_NON_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
                } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                    sql = SQLQueries.RETRIEVE_LATEST_NON_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_POSTGRESQL;
                } else {
                    sql = SQLQueries.RETRIEVE_LATEST_NON_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_ORACLE;
                }
            }

            if (StringUtils.isEmpty(scope)) {
                sql = sql.replace("TOKEN_SCOPE=?", "(TOKEN_SCOPE='' OR TOKEN_SCOPE IS NULL)");
            }

            if (StringUtils.isNotEmpty(userStoreDomain)) {
                //logic to store access token into different tables when multiple user stores are configured.
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName.toLowerCase());
            if (StringUtils.isNotEmpty(scope)) {
                prepStmt.setString(3, scope);
            }
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                String accessToken = persistenceProcessor.getPreprocessedAccessTokenIdentifier(resultSet.getString(1));
                String refreshToken = null;
                if (resultSet.getString(2) != null) {
                    refreshToken = persistenceProcessor.getPreprocessedRefreshToken(resultSet.getString(2));
                }
                long issuedTime = resultSet.getTimestamp(3,
                        Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                long validityPeriod = resultSet.getLong(4);
                String userType = resultSet.getString(5);
                // data loss at dividing the validity period but can be neglected
                AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, userName,
                        OAuth2Util.buildScopeArray(scope), new Timestamp(issuedTime),
                        validityPeriod / 1000, userType);
                accessTokenDO.setAccessToken(accessToken);
                accessTokenDO.setRefreshToken(refreshToken);
                return accessTokenDO;
            }
            return null;
        } catch (SQLException e) {
            String errorMsg = "Error occurred while trying to retrieve latest 'ACTIVE' " +
                    "access token for Client ID : " + consumerKey + ", User ID : " + userName +
                    " and  Scope : " + scope;
            if (!active) {
                errorMsg = errorMsg.replace("ACTIVE", "NON ACTIVE");
            }
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(null, resultSet, prepStmt);
        }
    }
}
