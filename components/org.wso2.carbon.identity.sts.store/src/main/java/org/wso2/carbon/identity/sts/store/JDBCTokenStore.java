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

package org.wso2.carbon.identity.sts.store;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.Token;
import org.apache.rahas.TokenStorage;
import org.apache.rahas.TrustException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.sts.store.dao.DBStsDAO;
import org.wso2.carbon.identity.sts.store.util.STSStoreUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class JDBCTokenStore implements TokenStorage {

    private DBStsDAO dbStsDAO;
    private static Log log = LogFactory.getLog(JDBCTokenStore.class);
    private static int poolSize = 100;
    
    private static ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

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
		if (tokenId != null && tokenId.startsWith("#")) {
			tokenId = tokenId.substring(1);
		}
		return tokenId;
	}

	
    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public void add(Token token) throws TrustException {
        //put the Token to cache.
    	Cache<String, SerializableToken> tokenCache = getTokenCache();
    	if(tokenCache != null) {
	        tokenCache.put(getTokenId(token), STSStoreUtils.getSerializableToken(token));
	        executorService.submit(new TokenPersisterTask(token));
    	}
    }

    public void update(Token token) throws TrustException {
        initDao();
        dbStsDAO.updateToken(token);
        //update the cache is that token present in cache
    	Cache<String, SerializableToken> tokenCache = getTokenCache();
    	
        if (tokenCache != null && tokenCache.containsKey(getTokenId(token))) {
            tokenCache.put(getTokenId(token), STSStoreUtils.getSerializableToken(token));
        }
    }

    public String[] getTokenIdentifiers() throws TrustException {
        initDao();
        return dbStsDAO.getAllTokenKeys();
    }

    public Token[] getExpiredTokens() throws TrustException {
        initDao();
        return dbStsDAO.getExpiredTokens(Token.EXPIRED);
    }

    public Token[] getValidTokens() throws TrustException {
        initDao();
        return dbStsDAO.getValidTokens(new int[]{Token.ISSUED, Token.RENEWED});
    }

    public Token[] getRenewedTokens() throws TrustException {
        initDao();
        return dbStsDAO.getRenewedTokens(Token.RENEWED);
    }

    public Token[] getCancelledTokens() throws TrustException {
        initDao();
        return dbStsDAO.getCancelledTokens(Token.CANCELLED);
    }

    public Token getToken(String id) throws TrustException {
    	
    	id = getTokenId(id);
    	
    	Cache<String, SerializableToken> tokenCache = getTokenCache();
        if (tokenCache != null && tokenCache.containsKey(id)) {
            try {
                return STSStoreUtils.getToken((SerializableToken) tokenCache.get(id));
            } catch (XMLStreamException e) {
               throw new TrustException("Failed to get Token from cache",e);
            }
        }
        initDao();
        Token token = dbStsDAO.getToken(id);

        if(token==null){
            log.debug("Token is not present in cache or database");
        }

        if (tokenCache != null && token!=null) {
            tokenCache.put(id, STSStoreUtils.getSerializableToken(token));
        }
        return token;
    }

    public void removeToken(String id) throws TrustException {
    	id = getTokenId(id);
        initDao();
        dbStsDAO.removeToken(id);
        //remove token from cache and send cache invalidation msg
    	Cache<String, SerializableToken> tokenCache = getTokenCache();
        if (tokenCache != null && tokenCache.containsKey(id)) {
        	tokenCache.remove(id);
        	// TODO ensure invalidate cache name is correct
//            CacheInvalidator cacheInvalidator =
//                    STSStoreComponent.getCacheInvalidator();
//            try {
//                cacheInvalidator.invalidateCache(STSConstants.KEY_ISSUER_CONFIG, id);
//            } catch (CacheException e) {
//                String msg = "Failed to invalidate token from cache";
//                log.error(msg, e);
//                throw new TrustException(msg, e);
//            }
        }
    }

    public List<Token> getStorageTokens() throws TrustException {
        initDao();
        return dbStsDAO.getTokens();
    }

    public void handlePersistence(List<?> persistingTokens) throws TrustException {
        //TODO
        //If we have distributed caching mechanism, we don't need to store token immediately
        //in database. We can periodically take token from local cache and store to database.
    }

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
	 * Getting existing cache if the cache available, else returns a newly created cache.
	 * This logic handles by javax.cache implementation
	 */
	public static Cache<String, SerializableToken> getTokenCache() {

		CacheManager manager = Caching.getCacheManagerFactory()
				.getCacheManager(STSMgtConstants.TOKEN_CACHE_MANAGER);
		Cache<String, SerializableToken> tokenCache = manager
				.getCache(STSMgtConstants.TOKEN_CACHE_ID);
		return tokenCache;

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
                log.error("Failed to persist token");
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
