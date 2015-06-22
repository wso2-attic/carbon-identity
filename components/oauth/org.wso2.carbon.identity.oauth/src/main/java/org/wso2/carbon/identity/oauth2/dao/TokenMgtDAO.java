/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.dao;

import org.apache.commons.lang.ArrayUtils;
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

import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
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

    private boolean enablePersist = true;

    private static BlockingDeque<AccessContextTokenDO> accessContextTokenQueue = new LinkedBlockingDeque<>();

    private static BlockingDeque<AuthContextTokenDO> authContextTokenQueue = new LinkedBlockingDeque<>();

    private static final Log log = LogFactory.getLog(TokenMgtDAO.class);

    private static final String IDN_OAUTH2_ACCESS_TOKEN = "IDN_OAUTH2_ACCESS_TOKEN";

    static {

        final Log log = LogFactory.getLog(TokenMgtDAO.class);

        try {
            maxPoolSize =
                    Integer.parseInt(IdentityUtil.getProperty("JDBCPersistenceManager.SessionDataPersist.PoolSize"));
        } catch (NumberFormatException e) {
            if(log.isDebugEnabled()){
                log.debug("Error while parsing the integer", e);
            }
            log.warn("Session data persistence pool size is not configured. Using default value.");
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
            log.error("Error retrieving TokenPersistenceProcessor. Defaulting to PlainTextProcessor", e);
            persistenceProcessor = new PlainTextPersistenceProcessor();
        }

        if (IdentityUtil.getProperty("JDBCPersistenceManager.TokenPersist.Enable") != null) {
            enablePersist = Boolean.parseBoolean(IdentityUtil.getProperty("JDBCPersistenceManager.TokenPersist.Enable"));
        }
    }

    public void storeAuthorizationCode(String authzCode, String consumerKey, String callbackUrl,
                                       AuthzCodeDO authzCodeDO) throws IdentityOAuth2Exception {

        if (!enablePersist) {
            return;
        }

        if (maxPoolSize > 0) {
            authContextTokenQueue.push(new AuthContextTokenDO(authzCode, consumerKey, callbackUrl, authzCodeDO));
        } else {
            persistAuthorizationCode(authzCode, consumerKey, callbackUrl, authzCodeDO);
        }
    }

    public void persistAuthorizationCode(String authzCode, String consumerKey, String callbackUrl,
                                         AuthzCodeDO authzCodeDO) throws IdentityOAuth2Exception {

        if (!enablePersist) {
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
            throw new IdentityOAuth2Exception("Error when storing the authorization code for consumer key : "
                                              + consumerKey);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public void storeAccessToken(String accessToken, String consumerKey,
                                 AccessTokenDO accessTokenDO, Connection connection,
                                 String userStoreDomain) throws IdentityOAuth2Exception {

        if (!enablePersist) {
            return;
        }

        PreparedStatement prepStmt = null;
        ResultSet results = null;

        String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (userStoreDomain != null) {
            accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
        }

        String sql = "INSERT INTO " +
                     accessTokenStoreTable +
                " (ACCESS_TOKEN, REFRESH_TOKEN, CONSUMER_KEY, AUTHZ_USER, TIME_CREATED, REFRESH_TOKEN_TIME_CREATED, " +
                "VALIDITY_PERIOD, REFRESH_TOKEN_VALIDITY_PERIOD, TOKEN_SCOPE_HASH, TOKEN_STATE, USER_TYPE, TOKEN_ID) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        String sqlAddScopes = "INSERT INTO IDN_OAUTH2_ACCESS_TOKEN_SCOPE_ASSOCIATION (TOKEN_ID, TOKEN_SCOPE) " +
                "VALUES (?,?)";
        try {
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedAccessTokenIdentifier(accessToken));
            if (accessTokenDO.getRefreshToken() != null) {
                prepStmt.setString(2, persistenceProcessor.getProcessedRefreshToken(accessTokenDO.getRefreshToken()));
            } else {
                prepStmt.setString(2, accessTokenDO.getRefreshToken());
            }
            prepStmt.setString(3, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(4, accessTokenDO.getAuthzUser().toLowerCase());
            prepStmt.setTimestamp(5, accessTokenDO.getIssuedTime(), Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            prepStmt.setTimestamp(6, accessTokenDO.getRefreshTokenIssuedTime(), Calendar.getInstance(TimeZone
                    .getTimeZone("UTC")));
            prepStmt.setLong(7, accessTokenDO.getValidityPeriodInMillis());
            prepStmt.setLong(8, accessTokenDO.getRefreshTokenValidityPeriodInMillis());
            prepStmt.setString(9, OAuth2Util.hashScopes(accessTokenDO.getScope()));
            prepStmt.setString(10, accessTokenDO.getTokenState());
            prepStmt.setString(11, accessTokenDO.getTokenType());
            prepStmt.setString(12, accessTokenDO.getTokenId());
            prepStmt.execute();
            String accessTokenId = accessTokenDO.getTokenId();
            prepStmt = connection.prepareStatement(sqlAddScopes);
            for (String scope : accessTokenDO.getScope()){
                prepStmt.setString(1,accessTokenId);
                prepStmt.setString(2, scope);
                prepStmt.execute();
            }

            connection.commit();
        } catch (SQLIntegrityConstraintViolationException e) {
            String errorMsg = "Access Token for consumer key : " + consumerKey + ", user : " +
                              accessTokenDO.getAuthzUser().toLowerCase() + " and scope : " +
                              OAuth2Util.buildScopeString(accessTokenDO.getScope()) + "already exists";
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (DataTruncation e) {
            throw new IdentityOAuth2Exception("Invalid request", e);
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

        if (!enablePersist) {
            return;
        }
        if (maxPoolSize > 0) {
            accessContextTokenQueue.push(new AccessContextTokenDO(accessToken, consumerKey, accessTokenDO
                    , userStoreDomain));
        } else {
            persistAccessToken(accessToken, consumerKey, accessTokenDO, userStoreDomain);
        }
    }

    public boolean persistAccessToken(String accessToken, String consumerKey,
                                      AccessTokenDO accessTokenDO,
                                      String userStoreDomain) throws IdentityOAuth2Exception {

        if (!enablePersist) {
            return false;
        }
        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain);
            return true;
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception(
                    "Error occurred while getting a connection to Identity Data Persistent Storage", e);
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

        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        try {

            String sql;
            if (connection.getMetaData().getDriverName().contains("MySQL")
                || connection.getMetaData().getDriverName().contains("H2")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MYSQL;
            } else if (connection.getMetaData().getDriverName().contains("MS SQL")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_MSSQL;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_POSTGRESQL;
            } else {
                sql = SQLQueries.RETRIEVE_LATEST_ACCESS_TOKEN_BY_CLIENT_ID_USER_SCOPE_ORACLE;
            }

            if (StringUtils.isEmpty(scope)) {
                sql = sql.replace("TOKEN_SCOPE_HASH=?", "(TOKEN_SCOPE_HASH='' OR TOKEN_SCOPE_HASH IS NULL)");
            }

            if (StringUtils.isNotEmpty(userStoreDomain)) {
                //logic to store access token into different tables when multiple user stores are configured.
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName.toLowerCase());
            if (StringUtils.isNotEmpty(scope)) {
                prepStmt.setString(3, OAuth2Util.hashScopes(scope));
            }
            resultSet = prepStmt.executeQuery();
            connection.commit();
            
            if (resultSet.next()) {
                boolean returnToken = false;
                String tokenState = resultSet.getString(7);
                if (includeExpiredTokens) {
                    if (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(tokenState) ||
                            OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED.equals(tokenState)) {
                        returnToken = true;
                    }
                } else {
                    if (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(tokenState)) {
                        returnToken = true;
                    }
                }
                if (returnToken) {
                    String accessToken = persistenceProcessor.getPreprocessedAccessTokenIdentifier(
                            resultSet.getString(1));
                    String refreshToken = null;
                    if (resultSet.getString(2) != null) {
                        refreshToken = persistenceProcessor.getPreprocessedRefreshToken(resultSet.getString(2));
                    }
                    long issuedTime = resultSet.getTimestamp(3, Calendar.getInstance(TimeZone.getTimeZone("UTC")))
                            .getTime();
                    long refreshTokenIssuedTime = resultSet.getTimestamp(4, Calendar.getInstance(TimeZone.getTimeZone
                            ("UTC"))).getTime();
                    long validityPeriodInMillis = resultSet.getLong(5);
                    long refreshTokenValidityPeriodInMillis = resultSet.getLong(6);

                    String userType = resultSet.getString(8);
                    String tokenId = resultSet.getString(9);
                    // data loss at dividing the validity period but can be neglected
                    AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, userName, OAuth2Util.buildScopeArray
                            (scope), new Timestamp(issuedTime), new Timestamp(refreshTokenIssuedTime)
                            , validityPeriodInMillis, refreshTokenValidityPeriodInMillis, userType);
                    accessTokenDO.setAccessToken(accessToken);
                    accessTokenDO.setRefreshToken(refreshToken);
                    accessTokenDO.setTokenState(tokenState);
                    accessTokenDO.setTokenId(tokenId);
                    return accessTokenDO;
                }
            }
            return null;
        } catch (SQLException e) {
            String errorMsg = "Error occurred while trying to retrieve latest 'ACTIVE' " +
                              "access token for Client ID : " + consumerKey + ", User ID : " + userName +
                              " and  Scope : " + scope;
            if (includeExpiredTokens) {
                errorMsg = errorMsg.replace("ACTIVE", "ACTIVE or EXPIRED");
            }
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, prepStmt);
        }
    }

    public Set<AccessTokenDO> retrieveAccessTokens(String consumerKey, String userName,
                                                   String userStoreDomain, boolean includeExpired)
            throws IdentityOAuth2Exception {

        Connection connection;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while getting Identity persistence " +
                    "store connection", e);
        }

        PreparedStatement prepStmt = null;
        Map<String, AccessTokenDO> accessTokenDOMap = new HashMap<>();
        try {
            String sql = SQLQueries.RETRIEVE_ACTIVE_ACCESS_TOKEN_BY_CLIENT_ID_USER;
            if (includeExpired) {
                sql = SQLQueries.RETRIEVE_ACTIVE_EXPIRED_ACCESS_TOKEN_BY_CLIENT_ID_USER;
            }
            if (StringUtils.isNotEmpty(userStoreDomain)) {
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName);
            ResultSet resultSet = prepStmt.executeQuery();

            while (resultSet.next()) {
                String accessToken = persistenceProcessor.
                        getPreprocessedAccessTokenIdentifier(resultSet.getString(1));
                if(accessTokenDOMap.get(accessToken) == null) {
                    String refreshToken = persistenceProcessor.
                            getPreprocessedRefreshToken(resultSet.getString(2));
                    String[] scope = OAuth2Util.buildScopeArray(resultSet.getString(3));
                    Timestamp issuedTime = resultSet.getTimestamp(4, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                    Timestamp refreshTokenIssuedTime = resultSet.getTimestamp(5, Calendar.getInstance(TimeZone
                            .getTimeZone("UTC")));
                    long validityPeriodInMillis = resultSet.getLong(6);
                    long refreshTokenValidityPeriodMillis = resultSet.getLong(7);
                    String tokenType = resultSet.getString(8);
                    String tokenId = resultSet.getString(9);

                    AccessTokenDO dataDO = new AccessTokenDO(consumerKey, userName, scope, issuedTime,
                            refreshTokenIssuedTime, validityPeriodInMillis,
                            refreshTokenValidityPeriodMillis, tokenType);
                    dataDO.setAccessToken(accessToken);
                    dataDO.setRefreshToken(refreshToken);
                    dataDO.setTokenId(tokenId);
                    accessTokenDOMap.put(accessToken, dataDO);
                } else {
                    String scope = resultSet.getString(3).trim();
                    AccessTokenDO accessTokenDO = accessTokenDOMap.get(accessToken);
                    accessTokenDO.setScope((String[]) ArrayUtils.add(accessTokenDO.getScope(), scope));
                }
            }
            connection.commit();
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance";
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            String errorMsg = "Error occurred while retrieving 'ACTIVE' access tokens for " +
                              "Client ID : " + consumerKey + " and User ID : " + userName;
            if (includeExpired) {
                errorMsg = errorMsg.replace("ACTIVE", "ACTIVE or EXPIRED");
            }
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

        return new HashSet<>(accessTokenDOMap.values());
    }


    public AuthzCodeDO validateAuthorizationCode(String consumerKey, String authorizationKey, String userId)
            throws IdentityOAuth2Exception {
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
                if (resultSet.getString(6).equals(OAuthConstants.AuthorizationCodeState.ACTIVE)) {
                    String authorizedUser = resultSet.getString(1);
                    String scopeString = resultSet.getString(2);
                    String callbackUrl = resultSet.getString(3);
                    Timestamp issuedTime = resultSet.getTimestamp(4,
                                                                  Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                    long validityPeriod = resultSet.getLong(5);

                    return new AuthzCodeDO(authorizedUser,
                                           OAuth2Util.buildScopeArray(scopeString),
                                           issuedTime, validityPeriod, callbackUrl);
                } else {
                    String tokenId = resultSet.getString(7);
                    revokeToken(tokenId, userId);
                }
            }
            connection.commit();
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

    public void cleanUpAuthzCode(String authzCode) throws IdentityOAuth2Exception {
        if (maxPoolSize > 0) {
            authContextTokenQueue.push(new AuthContextTokenDO(authzCode));
        } else {
            removeAuthzCode(authzCode);
        }
    }

    public void expireAuthorizationCode(String authzCode, String tokenId) throws IdentityOAuth2Exception {
        if (maxPoolSize > 0) {
            authContextTokenQueue.push(new AuthContextTokenDO(authzCode, tokenId));
        } else {
            doExpireAuthorizationCode(authzCode, tokenId);
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

    public void doExpireAuthorizationCode(String authzCode, String tokenId) throws IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.EXPIRE_AUTHZ_CODE_AND_INSERT_CURRENT_TOKEN);
            prepStmt.setString(1, tokenId);
            prepStmt.setString(2, persistenceProcessor.getPreprocessedAuthzCode(authzCode));
            prepStmt.execute();
            connection.commit();
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.EXPIRE_AUTHZ_CODE_AND_INSERT_CURRENT_TOKEN, e);
            throw new IdentityOAuth2Exception("Error when expiring authorization code", e);
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
        String mySqlQuery;
        String oracleQuery;
        String msSqlQuery;
        String postgreSqlQuery;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromAccessToken(refreshToken);
            }
            String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }
            mySqlQuery = "SELECT ACCESS_TOKEN, AUTHZ_USER, " +
                         "TOKEN_SCOPE, TOKEN_STATE, REFRESH_TOKEN_TIME_CREATED, REFRESH_TOKEN_VALIDITY_PERIOD, " +
                         "TOKEN_ID FROM " + accessTokenStoreTable +
                         " WHERE CONSUMER_KEY = ? AND REFRESH_TOKEN = ? ORDER BY TIME_CREATED " +
                         "DESC LIMIT 1";

            oracleQuery = "SELECT * FROM (SELECT ACCESS_TOKEN, AUTHZ_USER, " +
                          "TOKEN_SCOPE, TOKEN_STATE, REFRESH_TOKEN_TIME_CREATED, REFRESH_TOKEN_VALIDITY_PERIOD, " +
                          "TOKEN_ID FROM " + accessTokenStoreTable +
                          " WHERE CONSUMER_KEY = ? AND REFRESH_TOKEN = ? ORDER BY TIME_CREATED " +
                          "DESC) WHERE ROWNUM < 2 ";

            msSqlQuery = "SELECT TOP 1 ACCESS_TOKEN, AUTHZ_USER, " +
                         "TOKEN_SCOPE, TOKEN_STATE, REFRESH_TOKEN_TIME_CREATED, TOKEN_ID FROM, " +
                         "REFRESH_TOKEN_VALIDITY_PERIOD, TOKEN_ID " + accessTokenStoreTable +
                         " WHERE CONSUMER_KEY = ? AND REFRESH_TOKEN = ? ORDER BY TIME_CREATED DESC";

            postgreSqlQuery = "SELECT * FROM (SELECT ACCESS_TOKEN, AUTHZ_USER, TOKEN_SCOPE, TOKEN_STATE, " +
                              "REFRESH_TOKEN_TIME_CREATED, REFRESH_TOKEN_VALIDITY_PERIOD, TOKEN_ID FROM " +
                              accessTokenStoreTable + " WHERE CONSUMER_KEY = ?" +
                              " AND REFRESH_TOKEN = ? AND ORDER BY TIME_CREATED DESC) AS TOKEN LIMIT " +
                              "1 ";

            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = mySqlQuery;
            } else if (connection.getMetaData().getDriverName().contains("MS SQL")) {
                sql = msSqlQuery;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = msSqlQuery;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = postgreSqlQuery;
            } else {
                sql = oracleQuery;
            }

            if (refreshToken == null) {
                sql = sql.replace("REFRESH_TOKEN = ?", "REFRESH_TOKEN IS NULL");
            }

            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            if (refreshToken != null) {
                prepStmt.setString(2, persistenceProcessor.getProcessedRefreshToken(refreshToken));
            }
            resultSet = prepStmt.executeQuery();

            if (resultSet.next()) {
                validationDataDO.setAccessToken(persistenceProcessor.getPreprocessedAccessTokenIdentifier(
                        resultSet.getString(1)));
                validationDataDO.setAuthorizedUser(resultSet.getString(2));
                validationDataDO.setScope(OAuth2Util.buildScopeArray(resultSet.getString(3)));
                validationDataDO.setRefreshTokenState(resultSet.getString(4));
                validationDataDO.setIssuedTime(resultSet.getTimestamp(5, Calendar.getInstance(TimeZone.getTimeZone
                        ("UTC"))));
                validationDataDO.setValidityPeriodInMillis(resultSet.getLong(6));
                validationDataDO.setTokenId(resultSet.getString(7));
            }
            connection.commit();
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

    public void cleanUpAccessToken(String accessToken) throws IdentityOAuth2Exception {
        if (maxPoolSize > 0) {
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
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
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
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromAccessToken(accessTokenIdentifier);
            }
            String sql = SQLQueries.RETRIEVE_ACTIVE_ACCESS_TOKEN;
            if (includeExpired) {
                sql = SQLQueries.RETRIEVE_ACTIVE_EXPIRED_ACCESS_TOKEN;
            }
            if (StringUtils.isNotEmpty(userStoreDomain)) {
                sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN, IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
            }
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, persistenceProcessor.getProcessedAccessTokenIdentifier(accessTokenIdentifier));
            resultSet = prepStmt.executeQuery();

            int iterateId = 0;
            List<String> scopes = new ArrayList<>();
            if (resultSet.next()) {
                if (iterateId == 0) {
                    String consumerKey = persistenceProcessor.getPreprocessedClientId(resultSet.getString(1));
                    String authorizedUser = resultSet.getString(2);
                    String[] scope = OAuth2Util.buildScopeArray(resultSet.getString(3));
                    Timestamp issuedTime = resultSet.getTimestamp(4,
                            Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                    Timestamp refreshTokenIssuedTime = resultSet.getTimestamp(5,
                            Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                    long validityPeriodInMillis = resultSet.getLong(6);
                    long refreshTokenValidityPeriodMillis = resultSet.getLong(7);
                    String tokenType = resultSet.getString(8);
                    String refreshToken = resultSet.getString(9);
                    String tokenId = resultSet.getString(10);
                    dataDO = new AccessTokenDO(consumerKey, authorizedUser, scope, issuedTime, refreshTokenIssuedTime,
                            validityPeriodInMillis, refreshTokenValidityPeriodMillis, tokenType);
                    dataDO.setAccessToken(accessTokenIdentifier);
                    dataDO.setRefreshToken(refreshToken);
                    dataDO.setTokenId(tokenId);
                }else{
                    scopes.add(resultSet.getString(3));
                }

                if (scopes.size() > 0){
                    dataDO.setScope((String[])ArrayUtils.add(scopes.toArray(), dataDO.getScope()));
                }

            }
            connection.commit();
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
     * @param tokenId
     * @param tokenState
     * @throws IdentityOAuth2Exception
     */
    public void setAccessTokenState(String tokenId, String tokenState, String tokenStateId,
                                    String userStoreDomain) throws IdentityOAuth2Exception {

	    Connection connection = null;
	    try {
		    connection = JDBCPersistenceManager.getInstance().getDBConnection();
	    } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while trying to get a Identity persistence store " +
                    "instance", e);

        }
	    try {
            setAccessTokenState(connection, tokenId, tokenState, tokenStateId, userStoreDomain);
            connection.commit();
	    } catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error while updating Access Token with ID : " +
                                              tokenId + " to Token State : " + tokenState, e);
        } finally {
		    IdentityDatabaseUtil.closeConnection(connection);
	    }
    }

	/**
	 *
	 * @param connection database connection
     * @param tokenId accesstoken
     * @param tokenState    state of the token need to be updated.
	 * @param tokenStateId  token state id.
	 * @param userStoreDomain   user store domain.
	 * @throws IdentityOAuth2Exception
	 */
    public void setAccessTokenState(Connection connection, String tokenId, String tokenState,
                                    String tokenStateId, String userStoreDomain)
			throws IdentityOAuth2Exception {
		PreparedStatement prepStmt = null;
		try {

			String sql = SQLQueries.UPDATE_TOKE_STATE;
			if (StringUtils.isNotEmpty(userStoreDomain)) {
				sql = sql.replace(IDN_OAUTH2_ACCESS_TOKEN,
				                  IDN_OAUTH2_ACCESS_TOKEN + "_" + userStoreDomain);
			}
			prepStmt = connection.prepareStatement(sql);
			prepStmt.setString(1, tokenState);
			prepStmt.setString(2, tokenStateId);
            prepStmt.setString(3, tokenId);
            prepStmt.executeUpdate();
		} catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error while updating Access Token with ID : " +
                                              tokenId + " to Token State : " + tokenState, e);
        } finally {
			IdentityDatabaseUtil.closeStatement(prepStmt);
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
            IdentityDatabaseUtil.closeAllConnections(connection, null, ps);
        }
    }


    /**
     * This method is to revoke specific tokens
     *
     * @param tokenId token that needs to be revoked
     * @throws IdentityOAuth2Exception if failed to revoke the access token
     */
    public void revokeToken(String tokenId, String userId) throws IdentityOAuth2Exception {

        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = OAuth2Util.getAccessTokenStoreTableFromUserId(userId);
            }
            String sqlQuery = SQLQueries.REVOKE_ACCESS_TOKEN_BY_TOKEN_ID.replace(
                    IDN_OAUTH2_ACCESS_TOKEN, accessTokenStoreTable);
            connection = IdentityDatabaseUtil.getDBConnection();
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, OAuthConstants.TokenStates.TOKEN_STATE_REVOKED);
            ps.setString(2, UUID.randomUUID().toString());
            ps.setString(3, tokenId);
            int count = ps.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Number of rows being updated : " + count);
            }
            connection.commit();
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollBack(connection);
            throw new IdentityOAuth2Exception("Error occurred while revoking Access Token with ID : " + tokenId, e);
        } catch (IdentityException e) {
            throw new IdentityOAuth2Exception("Error occurred while revoking Access Token with ID : " + tokenId, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, ps);
        }
    }

    /**
     * This method is to list the application authorized by OAuth resource owners
     *
     * @param authzUser username of the resource owner
     * @return set of distinct client IDs authorized by user until now
     * @throws IdentityOAuth2Exception if failed to update the access token
     */
    public Set<String> getAllTimeAuthorizedClientIds(String authzUser) throws IdentityOAuth2Exception {

        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        PreparedStatement ps = null;
        Connection connection = null;
        ResultSet rs = null;
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
                    IDN_OAUTH2_ACCESS_TOKEN, accessTokenStoreTable);
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, authzUser.toLowerCase());
            rs = ps.executeQuery();
            while (rs.next()) {
                String consumerKey = persistenceProcessor.getPreprocessedClientId(rs.getString(1));
                distinctConsumerKeys.add(consumerKey);
            }
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception(
                    "Error occurred while retrieving all distinct Client IDs authorized by " +
                            "User ID : " + authzUser + " until now", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, ps);
        }
        return distinctConsumerKeys;
    }

    public String findScopeOfResource(String resourceUri) throws IdentityOAuth2Exception {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

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
            connection.commit();
            return null;
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            String errorMsg = "Error getting scopes for resource - " + resourceUri + " : " + e.getMessage();
            log.error(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, ps);
        }
        return null;
    }

    public boolean validateScope(Connection connection, String accessToken, String resourceUri) {
        return false;
    }

	/**
	 * This method is used invalidate the existing token and generate a new toke within one DB transaction.
	 *
     * @param oldAccessTokenId     access token need to be updated.
     * @param tokenState      token state before generating new token.
	 * @param consumerKey     consumer key of the existing token
	 * @param tokenStateId    new token state id to be updated
	 * @param accessTokenDO   new access token details
	 * @param userStoreDomain user store domain which is related to this consumer
	 * @throws IdentityOAuth2Exception
	 */
    public void invalidateAndCreateNewToken(String oldAccessTokenId, String tokenState,
                                            String consumerKey, String tokenStateId,
	                                        AccessTokenDO accessTokenDO, String userStoreDomain)
			throws IdentityOAuth2Exception {
		Connection connection = null;
		try {
			// Get the connection
			connection = JDBCPersistenceManager.getInstance().getDBConnection();
			connection.setAutoCommit(false);

			// update existing token as inactive
            setAccessTokenState(connection, oldAccessTokenId, tokenState, tokenStateId, userStoreDomain);

            String newAccessToken = accessTokenDO.getAccessToken();
            // store new token in the DB
            storeAccessToken(newAccessToken, consumerKey, accessTokenDO, connection,
                             userStoreDomain);

            // update new access token against authorization code if token obtained via authorization code grant type
            updateTokenIdIfAutzCodeGrantType(oldAccessTokenId, accessTokenDO.getTokenId(), connection);

			// commit both transactions
			connection.commit();
		} catch (IdentityException e) {
			String errorMsg = "Error while getting an Identity Persistence Store instance.";
			log.error(errorMsg, e);
			throw new IdentityOAuth2Exception(errorMsg, e);
		} catch (SQLException e) {
			String errorMsg = "Error while regenerating Access Token :" + e.getMessage();
			log.error(errorMsg, e);
			throw new IdentityOAuth2Exception(errorMsg, e);
		} finally {
			IdentityDatabaseUtil.closeConnection(connection);
		}
	}

    private void updateTokenIdIfAutzCodeGrantType(String oldAccessTokenId, String newAccessTokenId, Connection
            connection) throws IdentityOAuth2Exception {
        PreparedStatement prepStmt = null;
        try {
            prepStmt = connection.prepareStatement(SQLQueries.UPDATE_NEW_TOKEN_AGAINST_AUTHZ_CODE);
            prepStmt.setString(1, newAccessTokenId);
            prepStmt.setString(2, oldAccessTokenId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            throw new IdentityOAuth2Exception("Error while updating Access Token against authorization code for " +
                                              "access token with ID : " + oldAccessTokenId, e);
        } finally {
            IdentityDatabaseUtil.closeStatement(prepStmt);
        }
    }

}
