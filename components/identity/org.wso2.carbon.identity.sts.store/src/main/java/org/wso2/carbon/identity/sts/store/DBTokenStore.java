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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.Token;
import org.apache.rahas.TokenStorage;
import org.apache.rahas.TrustException;
import org.wso2.carbon.identity.sts.store.dao.DBStsDAO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBTokenStore implements TokenStorage {

    private static Log log = LogFactory.getLog(DBTokenStore.class);
    private static int poolSize = 100;
    private static Map<String, Token> tokenMap = new ConcurrentHashMap<String, Token>();
    private DBStsDAO dbStsDAO;

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

    public void add(Token token) throws TrustException {
        // put the Token to cache.
        tokenMap.put(getTokenId(token), token);
    }

    public void update(Token token) throws TrustException {
        tokenMap.put(getTokenId(token), token);
    }

    public String[] getTokenIdentifiers() throws TrustException {
        List<String> tokenIds = new ArrayList<String>();

        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            tokenIds.add(entry.getKey());
        }

        return tokenIds.toArray(new String[tokenIds.size()]);
    }

    public Token[] getExpiredTokens() throws TrustException {
        List<Token> tokens = new ArrayList<Token>();
        Date now = new Date();
        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            Token tok = tokenMap.get(entry.getKey());
            if (tok.getExpires().getTime() < now.getTime()) {
                tokens.add(tok);
            }
        }
        return tokens.toArray(new Token[tokens.size()]);

    }

    public Token[] getValidTokens() throws TrustException {
        List<Token> tokens = new ArrayList<Token>();
        Date now = new Date();
        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            Token tok = tokenMap.get(entry.getKey());
            if (tok.getExpires().getTime() > now.getTime()) {
                tokens.add(tok);
            }
        }
        return tokens.toArray(new Token[tokens.size()]);
    }

    public Token[] getRenewedTokens() throws TrustException {

        List<Token> tokens = new ArrayList<Token>();
        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            Token tok = tokenMap.get(entry.getKey());
            if (Token.RENEWED == tok.getState()) {
                tokens.add(tok);
            }
        }
        return tokens.toArray(new Token[tokens.size()]);

    }

    public Token[] getCancelledTokens() throws TrustException {
        List<Token> tokens = new ArrayList<Token>();
        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            Token tok = tokenMap.get(entry.getKey());
            if (Token.CANCELLED == tok.getState()) {
                tokens.add(tok);
            }
        }
        return tokens.toArray(new Token[tokens.size()]);
    }

    public Token getToken(String id) throws TrustException {
        id = getTokenId(id);
        return tokenMap.get(id);
    }

    public void removeToken(String id) throws TrustException {
        id = getTokenId(id);
        tokenMap.remove(id);
    }

    public List<Token> getStorageTokens() throws TrustException {
        List<Token> tokens = new ArrayList<Token>();
        for (Map.Entry<String, Token> entry : tokenMap.entrySet()) {
            Token tok = tokenMap.get(entry.getKey());
            tokens.add(tok);

        }
        return tokens;
    }

    public void handlePersistence(List<?> persistingTokens) throws TrustException {
        // TODO
        // If we have distributed caching mechanism, we don't need to store
        // token immediately
        // in database. We can periodically take token from local cache and
        // store to database.
    }

    public void handlePersistenceOnShutdown() throws TrustException {
        // TODO
        // If we don't immediately persist token to database,
        // we have to persist before the server shut down.
    }
}
