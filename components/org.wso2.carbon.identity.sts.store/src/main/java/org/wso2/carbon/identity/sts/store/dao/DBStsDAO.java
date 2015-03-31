/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.sts.store.dao;


import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.Token;
import org.apache.rahas.TrustException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.sts.store.DBQueries;
import org.wso2.carbon.identity.sts.store.util.STSStoreUtils;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This utility used to manage the DB operations for token store.
 */
public class DBStsDAO {

    private static final Log log = LogFactory.getLog(DBStsDAO.class);

    /**
     * This is for adding token to DB.
     *
     * @param token Token
     */
    public void addToken(Token token) throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = DBQueries.ADD_TOKEN;

        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, token.getId());
            byte[] tokenByteContainer = getTokenContent(token);
            InputStream tokenInputStream = new ByteArrayInputStream(tokenByteContainer);
            prepStmt.setBinaryStream(2, tokenInputStream, tokenByteContainer.length);
            prepStmt.setTimestamp(3, new Timestamp(token.getCreated().getTime()));
            prepStmt.setTimestamp(4, new Timestamp(token.getExpires().getTime()));
            prepStmt.setInt(5, token.getState());
            prepStmt.execute();
            connection.commit();

        } catch (Exception e) {
            STSStoreUtils.removeTokenFromCache(token.getId());
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException e1) {
                String msg1 = "Failed to rollback connection";
                log.error(msg1, e1);
            }
            String msg = "Failed to add token";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }

    }

    private Connection getDBConnection() throws IdentityException {
        return IdentityDatabaseUtil.getDBConnection();
    }

    /**
     * This is for updating the token in DB
     *
     * @param token Token
     */
    public void updateToken(Token token) throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String query = DBQueries.UPDATE_TOKEN;

        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            //  prepStmt.setObject(1, token);
            byte[] tokenByteContainer = getTokenContent(token);
            InputStream tokenInputStream = new ByteArrayInputStream(tokenByteContainer);
            prepStmt.setBinaryStream(1, tokenInputStream, tokenByteContainer.length);
            prepStmt.setTimestamp(2, new Timestamp(token.getCreated().getTime()));
            prepStmt.setTimestamp(3, new Timestamp(token.getExpires().getTime()));
            prepStmt.setInt(4, token.getState());
            prepStmt.setString(5, token.getId());
            prepStmt.executeUpdate();
            connection.commit();

        } catch (Exception e) {
            STSStoreUtils.removeTokenFromCache(token.getId());
            String msg = "Failed to update token ";
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    String msg1 = "Failed to rollback connection";
                    log.error(msg1, e1);
                }
            }
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }

    }

    /**
     * This is for removing token
     *
     * @param tokenId tokenId
     */
    public void removeToken(String tokenId) throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = DBQueries.REMOVE_TOKEN;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, tokenId);
            prepStmt.executeUpdate();

        } catch (Exception e) {
            String msg = "Failed to remove token";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }

    }

    /**
     * This is for get all the token keys
     *
     * @return arrays of keys
     */
    public String[] getAllTokenKeys() throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<String> keyList;
        String keys[] = new String[0];
        String query = DBQueries.ALL_TOKEN_KEYS;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();

            keyList = new ArrayList<String>();
            if (rs != null) {
                while (rs.next()) {
                    keyList.add(rs.getString("TOKEN_ID"));
                }
            }
            if (keyList.size() > 0) {
                keys = keyList.toArray(new String[keyList.size()]);
            }
        } catch (Exception e) {
            String msg = "Failed to get all tokens";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
        return keys;
    }

    /**
     * This is to get Token from token id
     *
     * @param tokenId tokenId
     * @return Token
     */
    public Token getToken(String tokenId) throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Token token = null;

        String query = DBQueries.GET_TOKEN;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, tokenId);
            rs = prepStmt.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Blob tokenContent = rs.getBlob("TOKEN_CONTENT");
                    byte[] tokenContentBytes = tokenContent.getBytes(1, (int) tokenContent.length());
                    token = getToken(tokenContentBytes);
                }
            }
        } catch (Exception e) {
            String msg = "Failed to get token";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
        return token;
    }

    private Token getToken(byte tokenContentBytes[]) throws TrustException {
        Token token;
        try {
            ByteArrayInputStream tokenContentByteArray = new ByteArrayInputStream(tokenContentBytes);
            ObjectInputStream tokenContentObject = new ObjectInputStream(tokenContentByteArray);
            Object tokenObj = tokenContentObject.readObject();
            token = (Token) tokenObj;
        } catch (Exception e) {
            String msg = "Failed to convert blob content to Token object ";
            log.error(msg, e);
            throw new TrustException(msg, e);
        }
        return token;
    }

    /**
     * This is to get all tokens from token store
     *
     * @return List of Tokens
     */
    public List<Token> getTokens() throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Token> tokens = new ArrayList<Token>();

        String query = DBQueries.GET_ALL_TOKENS;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    tokens.add(getToken((byte[]) rs.getObject("TOKEN_CONTENT")));
                }
            }
            return tokens;
        } catch (Exception e) {
            String msg = "Failed to get all  tokens";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
    }

    /**
     * This is to get valid tokens
     *
     * @param status Token.ISSUED, Token.RENEWED
     * @return Arrays of Tokens
     * @throws TrustException if failed to get valid tokens
     */
    public Token[] getValidTokens(int status[]) throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Token> tokens = new ArrayList<Token>();

        String query = DBQueries.VALID_TOKENS;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, Token.ISSUED);
            prepStmt.setInt(2, Token.RENEWED);
            rs = prepStmt.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Token token = getToken((byte[]) rs.getObject("TOKEN_CONTENT"));
                    tokens.add(token);
                }
            }
            return tokens.toArray(new Token[tokens.size()]);
        } catch (Exception e) {
            String msg = "Failed to get valid tokens";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
    }

    /**
     * This is to get expired tokens from token store
     *
     * @param status state id of expired tokens
     * @return Token[]
     * @throws TrustException if failed to get expired tokens
     */
    public Token[] getExpiredTokens(int status) throws TrustException {
        return getTokens(status);
    }

    /**
     * This is to get the renewed tokens from token store
     *
     * @param status state id of the renewed token
     * @return Token[]
     * @throws TrustException if failed to get renewed tokens
     */
    public Token[] getRenewedTokens(int status) throws TrustException {
        return getTokens(status);
    }

    /**
     * This is to get canceled tokens from token store
     *
     * @param status state id of cancel token
     * @return Token[]
     * @throws TrustException if failed to get canceled tokens
     */
    public Token[] getCancelledTokens(int status) throws TrustException {
        return getTokens(status);
    }

    private Token[] getTokens(int status) throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        List<Token> tokens = new ArrayList<Token>();

        String query = DBQueries.GET_TOKENS_BY_STATE;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, status);
            rs = prepStmt.executeQuery();

            if (rs != null) {
                while (rs.next()) {
                    Token token = getToken((byte[]) rs.getObject("TOKEN_CONTENT"));
                    tokens.add(token);
                }
            }
            return tokens.toArray(new Token[tokens.size()]);
        } catch (Exception e) {
            String msg = "Failed to get token";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
    }

    /**
     * This is to check token store empty or not
     *
     * @return boolean
     * @throws TrustException if failed to check the tokens availability
     */
    public boolean isTokensExist() throws TrustException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        boolean tokenExist = false;

        String query = DBQueries.TOKENS_EXISTS;
        try {
            connection = getDBConnection();
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();

            if (rs != null) {
                if (rs.next()) {
                    return true;
                }
            }

        } catch (Exception e) {
            String msg = "Failed to check token exist";
            log.error(msg, e);
            throw new TrustException(msg, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
        return tokenExist;
    }

    private byte[] getTokenContent(Token token) throws IOException {
        ByteArrayOutputStream tokenArrayStream = new ByteArrayOutputStream();
        ObjectOutputStream tokenObjectStream = null;
        byte[] tokenBytes = null;
        try {
            tokenObjectStream = new ObjectOutputStream(tokenArrayStream);
            tokenObjectStream.writeObject(token);
            tokenObjectStream.flush();
            tokenBytes = tokenArrayStream.toByteArray();
        } finally {
            if (tokenObjectStream != null) {
                tokenObjectStream.close();
            }
        }
        return tokenBytes;
    }


}
