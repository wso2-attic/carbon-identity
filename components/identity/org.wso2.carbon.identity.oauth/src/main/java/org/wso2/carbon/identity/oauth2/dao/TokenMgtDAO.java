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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth.tokenprocessor.PlainTextPersistenceProcessor;
import org.wso2.carbon.identity.oauth.tokenprocessor.TokenPersistenceProcessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.AuthzCodeDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.*;
import java.util.*;
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

    private static BlockingDeque<AccessContextTokenDO> accessContextTokenQueue = new LinkedBlockingDeque<AccessContextTokenDO>();

    private static BlockingDeque<AuthContextTokenDO> authContextTokenQueue = new LinkedBlockingDeque<AuthContextTokenDO>();

    private static final Log log = LogFactory.getLog(TokenMgtDAO.class);

    static {

        final Log log = LogFactory.getLog(TokenMgtDAO.class);

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
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when storing the access code for consumer key : " + consumerKey);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(null, null, prepStmt);
        }
    }

    public void storeAccessToken(String accessToken, String consumerKey,
                                 AccessTokenDO accessTokenDO, String userStoreDomain) throws IdentityOAuth2Exception {

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
                                    String userStoreDomain) throws IdentityOAuth2Exception {

        if(!enablePersist){
            return false;
        }
        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            connection.setAutoCommit(false);
            storeAccessToken(accessToken, consumerKey, accessTokenDO, connection, userStoreDomain);
            return true;
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            //Could be due to failiing unique key constraint
            String errorMsg = "Error saving Access Token :" + e.getMessage();
            log.error(errorMsg, e);
            //throw e;
            //log.error(errorMsg, e);
            //throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
        return false;
    }

    public AccessTokenDO getValidAccessTokenIfExist(String consumerKey, String userName,
                                                               String userStoreDomain, String scope)
            throws IdentityOAuth2Exception {

        return getValidAccessTokenIfExist(consumerKey, userName, userStoreDomain,scope, false);
    }

    public AccessTokenDO getValidAccessTokenIfExist(String consumerKey, String userName,
                                                               String userStoreDomain, String scope, boolean includeExpiredTokens)
                throws IdentityOAuth2Exception {
                Connection connection = null;
                try {
                        connection = JDBCPersistenceManager.getInstance().getDBConnection();
                        return getValidAccessTokenIfExist(consumerKey, userName, connection, userStoreDomain, scope, includeExpiredTokens);
                    } catch (IdentityException e) {
                        String errorMsg = "Error when getting an Identity Persistence Store instance.";
                        log.error(errorMsg, e);
                        throw new IdentityOAuth2Exception(errorMsg, e);
                    } finally {
                        IdentityDatabaseUtil.closeAllConnections(connection, null, null);
                    }
            }

    public AccessTokenDO getValidAccessTokenIfExist(String consumerKey, String userName,
                                                               String userStoreDomain, boolean includeExpiredTokens)
            throws IdentityOAuth2Exception {
        Connection connection = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            return getValidAccessTokenIfExist(consumerKey, userName, connection, userStoreDomain, includeExpiredTokens);
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, null);
        }
    }

    public AccessTokenDO getValidAccessTokenIfExist(String consumerKey, String userName,
                                                               Connection connection,
                                                               String userStoreDomain, String scope) throws IdentityOAuth2Exception {
                return getValidAccessTokenIfExist(consumerKey, userName, connection, userStoreDomain, scope, false);
    }

    public AccessTokenDO getValidAccessTokenIfExist(String consumerKey, String userName,
                                                               Connection connection,
                                                               String userStoreDomain, String scope,
                                                               boolean includeExpiredTokens) throws IdentityOAuth2Exception {
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        long timestampSkew;
        long currentTime;
        long validityPeriod;
        long issuedTime;
        String accessToken;
        String refreshToken;
        String sql;
        String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";

        try {
            //logic to store access token into different tables when multiple user stores are configured.
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }

            String oracleSQL = null, mySQLSQL = null, msSQL = null,postgreSQL = null;
            if(includeExpiredTokens){
                oracleSQL =
                        "SELECT * FROM( " +
                                " SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                                " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                                " AND AUTHZ_USER = ? " +
                                " AND TOKEN_SCOPE = ? " +
                                " AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED')" +
                                " ORDER BY TIME_CREATED DESC) " +
                                " WHERE ROWNUM < 2 ";

                mySQLSQL = "SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, " +
                        " VALIDITY_PERIOD, TOKEN_STATE FROM " + accessTokenStoreTable  +
                        " WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND TOKEN_SCOPE = ? AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED') ORDER BY TIME_CREATED DESC " +
                        " LIMIT 1";

                msSQL = "SELECT TOP 1 ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE FROM IDN_OAUTH2_ACCESS_TOKEN " +
                        "WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND TOKEN_SCOPE = ? AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED') ORDER BY TIME_CREATED DESC";

                postgreSQL = "SELECT * FROM(SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                             " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                             " AND AUTHZ_USER = ? AND TOKEN_SCOPE = ? " +
                             " AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED') " +
                             " ORDER BY TIME_CREATED DESC) AS TOKEN " +
                             " LIMIT 1 ";
            } else {
                oracleSQL =
                        "SELECT * FROM( " +
                                " SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                                " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                                " AND AUTHZ_USER = ? " +
                                " AND TOKEN_SCOPE = ? " +
                                " AND TOKEN_STATE='ACTIVE' " +
                                " ORDER BY TIME_CREATED DESC) " +
                                " WHERE ROWNUM < 2 ";
                //We set USER_TYPE as user as login request use to generate only user tokens not application tokens
                mySQLSQL = "SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, " +
                        " VALIDITY_PERIOD, TOKEN_STATE FROM " + accessTokenStoreTable  +
                        " WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND TOKEN_SCOPE = ? AND TOKEN_STATE='ACTIVE' ORDER BY TIME_CREATED DESC " +
                        " LIMIT 1";

                msSQL = "SELECT TOP 1 ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE FROM IDN_OAUTH2_ACCESS_TOKEN " +
                        "WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND TOKEN_SCOPE = ? AND TOKEN_STATE='ACTIVE' ORDER BY TIME_CREATED DESC";
                postgreSQL = "SELECT * FROM(SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                                    " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                                    " AND AUTHZ_USER = ? " +
                                    " AND TOKEN_SCOPE = ? " +
                                    " AND TOKEN_STATE='ACTIVE' " +
                                    " ORDER BY TIME_CREATED DESC) AS TOKEN " +
                                    " LIMIT 1 ";
            }
            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = mySQLSQL;
            }
            else if(connection.getMetaData().getDriverName().contains("MS SQL")){
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = postgreSQL;
            } else {
                sql = oracleSQL;
            }
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName.toLowerCase());
            prepStmt.setString(3, scope);
            resultSet = prepStmt.executeQuery();

            //Read the latest ACCESS_TOKEN record for CONSUMER_KEY+AUTHZ_USER combination
            if (resultSet.next()) {
                issuedTime = resultSet.getTimestamp("TIME_CREATED",
                        Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                validityPeriod = resultSet.getLong("VALIDITY_PERIOD");
                //TODO revise the logic
                accessToken = persistenceProcessor.getPreprocessedAccessTokenIdentifier(resultSet.getString("ACCESS_TOKEN"));
                if(resultSet.getString("REFRESH_TOKEN") != null){
                    refreshToken = persistenceProcessor.getPreprocessedRefreshToken(resultSet.getString("REFRESH_TOKEN"));
                } else {
                    refreshToken = resultSet.getString("REFRESH_TOKEN");
                }
				// data loss at dividing the validity period but can be neglected
				AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, userName, null, new Timestamp(issuedTime),
						validityPeriod / 1000, null);
				accessTokenDO.setAccessToken(accessToken);
				accessTokenDO.setRefreshToken(refreshToken);
                if (includeExpiredTokens) {
                    return accessTokenDO;
                } else {
					if (OAuth2Util.getTokenExpireTimeMillis(accessTokenDO) > 0) {
                        return accessTokenDO;
                    }
                }
            }
            return null;

        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.VALIDATE_AUTHZ_CODE);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when validating an authorization code", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(null, resultSet, prepStmt);
        }
    }

    public AccessTokenDO getValidAccessTokenIfExist(String consumerKey, String userName,
                                                               Connection connection,
                                                               String userStoreDomain,
                                                               boolean includeExpiredTokens) throws IdentityOAuth2Exception {
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        long timestampSkew;
        long currentTime;
        long validityPeriod;
        long issuedTime;
        String accessToken;
        String refreshToken;
        String sql;
        String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";

        try {
            //logic to store access token into different tables when multiple user stores are configured.
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }

            String oracleSQL = null, mySQLSQL = null, msSQL = null, postgreSQL = null;
            if (includeExpiredTokens) {
                oracleSQL =
                        "SELECT * FROM( " +
                                " SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                                " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                                " AND AUTHZ_USER = ? " +
                                " AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED')" +
                                " ORDER BY TIME_CREATED DESC) " +
                                " WHERE ROWNUM < 2 ";

                mySQLSQL = "SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, " +
                        " VALIDITY_PERIOD, TOKEN_STATE FROM " + accessTokenStoreTable +
                        " WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED') ORDER BY TIME_CREATED DESC " +
                        " LIMIT 1";

                msSQL = "SELECT TOP 1 ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE FROM IDN_OAUTH2_ACCESS_TOKEN " +
                        "WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED') ORDER BY TIME_CREATED DESC";

                postgreSQL = "SELECT * FROM(SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                        " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                        " AND AUTHZ_USER = ? " +
                        " AND (TOKEN_STATE='ACTIVE' OR TOKEN_STATE='EXPIRED') " +
                        " ORDER BY TIME_CREATED DESC) AS TOKEN " +
                        " LIMIT 1 ";
            } else {
                oracleSQL =
                        "SELECT * FROM( " +
                                " SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                                " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                                " AND AUTHZ_USER = ? " +
                                " AND TOKEN_STATE='ACTIVE' " +
                                " ORDER BY TIME_CREATED DESC) " +
                                " WHERE ROWNUM < 2 ";
                //We set USER_TYPE as user as login request use to generate only user tokens not application tokens
                mySQLSQL = "SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, " +
                        " VALIDITY_PERIOD, TOKEN_STATE FROM " + accessTokenStoreTable +
                        " WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND TOKEN_STATE='ACTIVE' ORDER BY TIME_CREATED DESC " +
                        " LIMIT 1";
                msSQL = "SELECT TOP 1 ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE FROM IDN_OAUTH2_ACCESS_TOKEN " +
                        "WHERE CONSUMER_KEY = ? AND AUTHZ_USER = ? AND TOKEN_STATE='ACTIVE' ORDER BY TIME_CREATED DESC";
                postgreSQL = "SELECT * FROM(SELECT ACCESS_TOKEN, REFRESH_TOKEN, TIME_CREATED, VALIDITY_PERIOD, TOKEN_STATE " +
                        " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                        " AND AUTHZ_USER = ? " +
                        " AND TOKEN_STATE='ACTIVE' " +
                        " ORDER BY TIME_CREATED DESC) AS TOKEN " +
                        " LIMIT 1 ";
            }
            if (connection.getMetaData().getDriverName().contains("MySQL")
                    || connection.getMetaData().getDriverName().contains("H2")) {
                sql = mySQLSQL;
            } else if (connection.getMetaData().getDriverName().contains("MS SQL")) {
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("Microsoft")) {
                sql = msSQL;
            } else if (connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                sql = postgreSQL;
            } else {
                sql = oracleSQL;
            }
            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, userName.toLowerCase());
            resultSet = prepStmt.executeQuery();

            //Read the latest ACCESS_TOKEN record for CONSUMER_KEY+AUTHZ_USER combination
            if (resultSet.next()) {
                timestampSkew = OAuthServerConfiguration.getInstance().
                        getTimeStampSkewInSeconds() * 1000;
                currentTime = System.currentTimeMillis();
                issuedTime = resultSet.getTimestamp("TIME_CREATED",
                        Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                validityPeriod = resultSet.getLong("VALIDITY_PERIOD");
                //TODO revise the logic
                accessToken = persistenceProcessor.getPreprocessedAccessTokenIdentifier(resultSet.getString("ACCESS_TOKEN"));
                if (resultSet.getString("REFRESH_TOKEN") != null) {
                    refreshToken = persistenceProcessor.getPreprocessedRefreshToken(resultSet.getString("REFRESH_TOKEN"));
                } else {
                    refreshToken = resultSet.getString("REFRESH_TOKEN");
                }
				// data loss at dividing the validity period but can be neglected
				AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, userName, null, new Timestamp(issuedTime),
						validityPeriod / 1000, null);
				accessTokenDO.setAccessToken(accessToken);
                accessTokenDO.setRefreshToken(refreshToken);
                if (includeExpiredTokens) {
                    return accessTokenDO;
                } else {
                    if (((issuedTime + validityPeriod) - (currentTime + timestampSkew)) > 1000) {
                        return accessTokenDO;
                    }
                }
            }
            return null;

        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.VALIDATE_AUTHZ_CODE);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when validating an authorization code", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(null, resultSet, prepStmt);
        }
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
        String mySqlQuery;
        String oracleQuery;
        String msSqlQuery;
        String postgreSqlQuery;

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
            mySqlQuery = "SELECT ACCESS_TOKEN, AUTHZ_USER, " +
                    "TOKEN_SCOPE, TOKEN_STATE, TIME_CREATED FROM " + accessTokenStoreTable +
                    " WHERE CONSUMER_KEY = ? AND REFRESH_TOKEN = ? ORDER BY TIME_CREATED DESC LIMIT 1";

            oracleQuery = "SELECT * FROM (SELECT ACCESS_TOKEN, AUTHZ_USER, " +
                    "TOKEN_SCOPE, TOKEN_STATE, TIME_CREATED FROM " + accessTokenStoreTable +
                    " WHERE CONSUMER_KEY = ? AND REFRESH_TOKEN = ? ORDER BY TIME_CREATED DESC) WHERE ROWNUM < 2 ";

            msSqlQuery = "SELECT TOP 1 ACCESS_TOKEN, AUTHZ_USER, " +
                    "TOKEN_SCOPE, TOKEN_STATE, TIME_CREATED FROM " + accessTokenStoreTable +
                    " WHERE CONSUMER_KEY = ? AND REFRESH_TOKEN = ? ORDER BY TIME_CREATED DESC";

            postgreSqlQuery = "SELECT * FROM (SELECT ACCESS_TOKEN, AUTHZ_USER, TOKEN_SCOPE, TOKEN_STATE, TIME_CREATED" +
                    " FROM " + accessTokenStoreTable + " WHERE CONSUMER_KEY = ? " +
                    " AND REFRESH_TOKEN = ? " +
                    " ORDER BY TIME_CREATED DESC) AS TOKEN " +
                    " LIMIT 1 ";

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

    public AccessTokenDO retrieveAccessToken(String accessTokenIdentifier) throws IdentityOAuth2Exception {

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

            String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }
            String sql = "SELECT CONSUMER_KEY, AUTHZ_USER, " +
                    "TOKEN_SCOPE, TIME_CREATED,  VALIDITY_PERIOD, USER_TYPE, REFRESH_TOKEN " +
                    "FROM " + accessTokenStoreTable + " WHERE ACCESS_TOKEN = ? AND TOKEN_STATE=?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, persistenceProcessor.getProcessedAccessTokenIdentifier(accessTokenIdentifier));
            prepStmt.setString(2, OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
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
                dataDO = new AccessTokenDO(consumerKey, authorizedUser, scope, timestamp, validityPeriod, tokenType);
                dataDO.setRefreshToken(refreshToken);
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.VALIDATE_BEARER_TOKEN);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error when validating a bearer token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }

        return dataDO;
    }


    /**
     * Sets state of access token
     *
     * @param consumerKey
     * @param authorizedUser
     * @param tokenState
     * @throws IdentityOAuth2Exception
     */
    public void setAccessTokenState(String consumerKey, String authorizedUser, String tokenState,
                                    String tokenStateId, String userStoreDomain, String scope)
            throws IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            connection.setAutoCommit(false);

            String accessTokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
            if (userStoreDomain != null) {
                accessTokenStoreTable = accessTokenStoreTable + "_" + userStoreDomain;
            }
            String sql = "UPDATE " + accessTokenStoreTable +
                    " SET TOKEN_STATE = ? " +
                    ",TOKEN_STATE_ID = ? " +
                    "WHERE CONSUMER_KEY = ? " +
                    "AND AUTHZ_USER = ? " +
                    "AND TOKEN_SCOPE = ? " +
                    "AND TOKEN_STATE_ID = 'NONE' ";

            prepStmt = connection.prepareStatement(sql);
            prepStmt.setString(1, tokenState);
            prepStmt.setString(2, tokenStateId);
            prepStmt.setString(3, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(4, authorizedUser.toLowerCase());
            prepStmt.setString(5, scope);
            prepStmt.executeUpdate();
            connection.commit();
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentityOAuth2Exception(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.UPDATE_TOKE_STATE);
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error while updating token state", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    /**
     * This method is to revoke the tokens by OAuth clients
     *
     * @param token token that will be revoked
     * @param consumerKey consumerKey of the OAuth client to whom the token was issued
     * @throws IdentityOAuth2Exception if failed to update the access token
     */
    public void revokeTokensByClient(String token, String consumerKey) throws IdentityOAuth2Exception {

        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = OAuth2Util.getAccessTokenStoreTableFromAccessToken(token);
            }
            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(token);
            String sqlQuery = SQLQueries.REVOKE_ACCESS_TOKEN_BY_CLIENT.replace("IDN_OAUTH2_ACCESS_TOKEN", accessTokenStoreTable);
            connection = IdentityDatabaseUtil.getDBConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, OAuthConstants.TokenStates.TOKEN_STATE_REVOKED);
            ps.setString(2, UUID.randomUUID().toString());
            ps.setString(3, persistenceProcessor.getProcessedAccessTokenIdentifier(token));
            ps.setString(4, persistenceProcessor.getProcessedClientId(consumerKey));
            int count = ps.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Number of rows being updated : " + count);
            }
            connection.commit();
        } catch (SQLException e) {
            log.debug(e.getMessage(), e);
            IdentityDatabaseUtil.rollBack(connection);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (IdentityException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * This method is to revoke the tokens by OAuth resource owners
     *
     * @param apps apps for which the token will be revoked
     * @param authzUser  username of the resource owner
     * @throws org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception
     */
    public void revokeTokensByResourceOwner(String[] apps, String authzUser) throws IdentityOAuth2Exception {

        OAuthAppDAO appDAO = new OAuthAppDAO();
        authzUser = authzUser.toLowerCase();
        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        Connection dbConnection = null;
        try {
            dbConnection =  IdentityDatabaseUtil.getDBConnection();
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = OAuth2Util.getAccessTokenStoreTableFromUserId(authzUser);
            }
            OAuthAppDO[] oauthAppDOs =  getAppsAuthorizedByUser(authzUser);
            dbConnection.setAutoCommit(false);
            for (String app : apps) {
                for(OAuthAppDO appDO:oauthAppDOs){
                    appDO =  appDAO.getAppInformation(appDO.getOauthConsumerKey());
                    if(appDO.getApplicationName().equals(app)){
                        revokeTokenForApp(dbConnection, appDO.getOauthConsumerKey(), authzUser, accessTokenStoreTable);
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(appDO.getOauthConsumerKey(), authzUser);
                    }
                }
            }
            dbConnection.commit();
        } catch (SQLException e) {
            log.debug(e.getMessage(), e);
            IdentityDatabaseUtil.rollBack(dbConnection);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (IdentityException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (InvalidOAuthClientException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } finally {
            IdentityDatabaseUtil.closeConnection(dbConnection);
        }


    }

    private void revokeTokenForApp(Connection dbConnection, String consumerKey, String authzUser, String accessTokenStoreTable)
            throws IdentityOAuth2Exception, SQLException {

        String userStoreDomain = null;
        if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
            userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId(authzUser);
        }
        AccessTokenDO accessTokenDO = getValidAccessTokenIfExist(consumerKey, authzUser, userStoreDomain, true);
        if (accessTokenDO != null) {
            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(accessTokenDO.getAccessToken());
            String sqlQuery = SQLQueries.REVOKE_ALL_ACCESS_TOKEN_BY_RESOURCE_OWNER.replace("IDN_OAUTH2_ACCESS_TOKEN", accessTokenStoreTable);
            PreparedStatement ps = null;
            ps = dbConnection.prepareStatement(sqlQuery);
            ps.setString(1, OAuthConstants.TokenStates.TOKEN_STATE_REVOKED);
            ps.setString(2, UUID.randomUUID().toString());
            ps.setString(3, persistenceProcessor.getProcessedClientId(consumerKey));
            ps.setString(4, authzUser.toLowerCase());
//            ps.setString(5, persistenceProcessor.getProcessedAccessTokenIdentifier(accessTokenDO.getAccessToken()));
            int count = ps.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Number of rows being updated : " + count);
            }
        }
    }

    /**
     * This method is to list the application authorized by OAuth resource owners
     *
     * @param authzUser  username of the resource owner
     * @throws IdentityOAuth2Exception if failed to update the access token
     */
    public OAuthAppDO[] getAppsAuthorizedByUser(String authzUser) throws IdentityOAuth2Exception {

        List<OAuthAppDO> appDOs = new ArrayList<OAuthAppDO>();
        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        PreparedStatement ps = null;
        String userStoreDomain = null;
        Connection connection = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = OAuth2Util.getAccessTokenStoreTableFromUserId(authzUser);
            }
            String sqlQuery = SQLQueries.GET_DISTINCT_APPS_AUTHORIZED_BY_USER_ALL_TIME.replace("IDN_OAUTH2_ACCESS_TOKEN",accessTokenStoreTable);
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, authzUser.toLowerCase());
            ps.setString(2, OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
            ps.setString(3, OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED);
            ResultSet rs = ps.executeQuery();
            List<String> distinctConsumerKeys = new ArrayList<String>();
            while (rs.next()) {
                String consumerKey = persistenceProcessor.getPreprocessedClientId(rs.getString(1));
                distinctConsumerKeys.add(consumerKey);
            }
            if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                    OAuth2Util.checkUserNameAssertionEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId(authzUser);
            }
            for(String consumerKey:distinctConsumerKeys){
                AccessTokenDO accessTokenDO = getValidAccessTokenIfExist(consumerKey, authzUser, userStoreDomain, true);
                if (accessTokenDO != null) {
                    OAuthAppDO appDO = new OAuthAppDO();
                    appDO.setOauthConsumerKey(consumerKey);
                    appDOs.add(appDO);
                }
            }
        } catch (SQLException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (IdentityOAuth2Exception e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (IdentityException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return appDOs.toArray(new OAuthAppDO[appDOs.size()]);
    }

    /**
     * Get state of access token
     *
     * @param consumerKey
     * @param authorizedUser
     * @throws IdentityOAuth2Exception
     */
    public String getAccessTokenState(String consumerKey, String authorizedUser, String scope) throws IdentityOAuth2Exception {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String tokenState = null;
        try {
            connection = IdentityDatabaseUtil.getDBConnection();
            connection.setAutoCommit(false);
            prepStmt = connection.prepareStatement(SQLQueries.GET_TOKEN_STATE);
            prepStmt.setString(1, persistenceProcessor.getProcessedClientId(consumerKey));
            prepStmt.setString(2, authorizedUser.toLowerCase());
            prepStmt.setString(3, scope);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                tokenState = resultSet.getString("TOKEN_STATE");
            }
        } catch (IdentityException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (SQLException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage(), e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return tokenState;
    }

    public String findScopeOfResource(String resourceUri) throws IdentityOAuth2Exception {

        Connection connection = null;
        PreparedStatement ps;
        ResultSet rs;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            connection.setAutoCommit(false);
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
     * Get access token information
     *
     * @param accessToken access token
     * @return access token info obj
     * @throws IdentityOAuth2Exception if something went wrong when getting access token info
     */
    public AccessTokenDO getAccessTokenInfo(String accessToken) throws IdentityOAuth2Exception {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        AccessTokenDO accessTokenDO = null;
        String authorizedUser;
        String[] scope;
        Timestamp timestamp;
        long validityPeriod;
        String tokenState;
        String refreshToken;
        String consumerKey;
        String tokenType;
        try {
            String sqlQuery = "SELECT * FROM IDN_OAUTH2_ACCESS_TOKEN WHERE ACCESS_TOKEN=?";
            connection = IdentityDatabaseUtil.getDBConnection();
            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, accessToken);
            rs = ps.executeQuery();
            while(rs.next()) {
                consumerKey = persistenceProcessor.getPreprocessedClientId(rs.getString("CONSUMER_KEY"));
                authorizedUser = rs.getString("AUTHZ_USER");
                scope = OAuth2Util.buildScopeArray(rs.getString("TOKEN_SCOPE"));
                timestamp = rs.getTimestamp("TIME_CREATED",
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                validityPeriod = rs.getLong("VALIDITY_PERIOD");
                refreshToken = rs.getString("REFRESH_TOKEN");
                tokenState = rs.getString("TOKEN_STATE");
                accessToken = rs.getString("ACCESS_TOKEN");
                tokenType = rs.getString("USER_TYPE");
                accessTokenDO = new AccessTokenDO(consumerKey, authorizedUser, scope, timestamp, validityPeriod, tokenType);
                accessTokenDO.setRefreshToken(refreshToken);
                accessTokenDO.setTokenState(tokenState);
                accessTokenDO.setAccessToken(accessToken);
            }
        } catch (SQLException e) {
            String msg = "SQL error occurred while getting access token information " + e.getMessage();
            log.error(msg, e);
            throw new IdentityOAuth2Exception(msg, e);
        } catch (IdentityException e) {
            String msg = "Error occurred getting access token information due to failed to create database connection"+ e.getMessage();
            log.error(msg, e);
            throw new IdentityOAuth2Exception(msg, e);
        } finally {
            IdentityDatabaseUtil.closeResultSet(rs);
            IdentityDatabaseUtil.closeStatement(ps);
            IdentityDatabaseUtil.closeConnection(connection);
        }
        return accessTokenDO;
    }
}
