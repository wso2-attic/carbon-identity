/*
 * Copyright (c) 2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sts.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.Token;
import org.apache.rahas.TokenStorage;
import org.apache.rahas.TrustException;
import org.wso2.carbon.identity.sts.store.dao.DBStsDAO;
import org.wso2.carbon.identity.sts.store.util.STSStoreUtils;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.xml.stream.XMLStreamException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JDBCTokenStore implements TokenStorage {

    private static final Log log = LogFactory.getLog(JDBCTokenStore.class);
    private static int poolSize = 100;
    private static ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
    private DBStsDAO dbStsDAO;

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Getting existing cache if the cache available, else returns a newly created cache.
     * This logic handles by javax.cache implementation
     */
    public static Cache<String, SerializableToken> getTokenCache() {

        CacheManager manager = Caching.getCacheManagerFactory().getCacheManager(STSMgtConstants.TOKEN_CACHE_MANAGER);
        return manager.getCache(STSMgtConstants.TOKEN_CACHE_ID);
    }

    /**
     * "#" are used for internal references. If a token-id comes with that we
     * need to remove.
     *
     * @param token
     * @return
     */
    private String getTokenId(Token token) {
        if (token == null) {
            return "";
        }
        String tokenId = token.getId();
        if (tokenId != null && tokenId.startsWith("#")) {
            tokenId = tokenId.substring(1);
        }
        return tokenId;
    }

    /**
     * "#" are used for internal references. If a token-id comes with that we
     * need to remove
     *
     * @param tokenId
     * @return
     */
    private String getTokenId(String tokenId) {
        String tokenIdVal = tokenId;
        if (tokenId != null && tokenId.startsWith("#")) {
            tokenIdVal = tokenIdVal.substring(1);
        }
        return tokenIdVal;
    }

    @Override
    public void add(Token token) throws TrustException {
        //put the Token to cache.
        Cache<String, SerializableToken> tokenCache = getTokenCache();
        if (tokenCache != null) {
            tokenCache.put(getTokenId(token), STSStoreUtils.getSerializableToken(token));
            executorService.submit(new TokenPersisterTask(token));
        }
    }

    @Override
    public void update(Token token) throws TrustException {
        initDao();
        dbStsDAO.updateToken(token);
        //update the cache is that token present in cache
        Cache<String, SerializableToken> tokenCache = getTokenCache();

        if (tokenCache != null && tokenCache.containsKey(getTokenId(token))) {
            tokenCache.put(getTokenId(token), STSStoreUtils.getSerializableToken(token));
        }
    }

    @Override
    public String[] getTokenIdentifiers() throws TrustException {
        initDao();
        return dbStsDAO.getAllTokenKeys();
    }

    @Override
    public Token[] getExpiredTokens() throws TrustException {
        initDao();
        return dbStsDAO.getExpiredTokens(Token.EXPIRED);
    }

    @Override
    public Token[] getValidTokens() throws TrustException {
        initDao();
        return dbStsDAO.getValidTokens(new int[] { Token.ISSUED, Token.RENEWED });
    }

    @Override
    public Token[] getRenewedTokens() throws TrustException {
        initDao();
        return dbStsDAO.getRenewedTokens(Token.RENEWED);
    }

    @Override
    public Token[] getCancelledTokens() throws TrustException {
        initDao();
        return dbStsDAO.getCancelledTokens(Token.CANCELLED);
    }

    @Override
    public Token getToken(String id) throws TrustException {

        String tokenId = getTokenId(id);

        Cache<String, SerializableToken> tokenCache = getTokenCache();
        if (tokenCache != null && tokenCache.containsKey(tokenId)) {
            try {
                return STSStoreUtils.getToken((SerializableToken) tokenCache.get(tokenId));
            } catch (XMLStreamException e) {
                throw new TrustException("Failed to get Token from cache", e);
            }
        }
        initDao();
        Token token = dbStsDAO.getToken(tokenId);

        if (token == null) {
            log.debug("Token is not present in cache or database");
        }

        if (tokenCache != null && token != null) {
            tokenCache.put(tokenId, STSStoreUtils.getSerializableToken(token));
        }
        return token;
    }

    @Override
    public void removeToken(String id) throws TrustException {
        String tokenId = getTokenId(id);
        initDao();
        dbStsDAO.removeToken(tokenId);
        //remove token from cache and send cache invalidation msg
        Cache<String, SerializableToken> tokenCache = getTokenCache();
        if (tokenCache != null && tokenCache.containsKey(tokenId)) {
            tokenCache.remove(tokenId);
        }
    }

    @Override
    public List<Token> getStorageTokens() throws TrustException {
        initDao();
        return dbStsDAO.getTokens();
    }

    @Override
    public void handlePersistence(List<?> persistingTokens) throws TrustException {
        //TODO
        //If we have distributed caching mechanism, we don't need to store token immediately
        //in database. We can periodically take token from local cache and store to database.
    }

    @Override
    public void handlePersistenceOnShutdown() throws TrustException {
        //TODO
        // If we don't immediately persist token to database,
        // we have to persist before the server shut down.
    }

    private void initDao() {
        if (dbStsDAO == null) {
            this.dbStsDAO = new DBStsDAO();
        }
    }

    /**
     * This task used to persist the token.
     */
    protected static class TokenPersisterTask implements Runnable {

        private Token token;

        public TokenPersisterTask(Token token) {
            this.token = token;
        }

        @Override
        public void run() {
            try {
                persist();
            } catch (TrustException e) {
                log.error("Failed to persist token", e);
            }
        }

        private synchronized void persist() throws TrustException {
            try {
                new DBStsDAO().addToken(token);
            } catch (TrustException e) {
                throw new TrustException("Failed to persist token", e);
            }

        }
    }
}
