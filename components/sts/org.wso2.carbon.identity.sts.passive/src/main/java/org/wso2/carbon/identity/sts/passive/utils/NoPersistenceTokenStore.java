/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.sts.passive.utils;

import org.apache.rahas.Token;
import org.apache.rahas.TokenStorage;
import org.apache.rahas.TrustException;

import java.util.List;

/**
 * Empty implementation of the TokenStorage. Can be usd when tokens are not needed to be persisted. e.g Passive STS
 */
public class NoPersistenceTokenStore implements TokenStorage {
    /**
     * Add the given token to the list.
     *
     * @param token The token to be added
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public void add(Token token) throws TrustException {

    }

    /**
     * Update an existing token.
     *
     * @param token
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public void update(Token token) throws TrustException {

    }

    /**
     * Return the list of all token identifiers.
     *
     * @return As array of token identifiers
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public String[] getTokenIdentifiers() throws TrustException {
        return new String[0];
    }

    /**
     * Return the list of <code>EXPIRED</code> tokens.
     * If there are no <code>EXPIRED</code> tokens <code>null</code> will be
     * returned
     *
     * @return An array of expired <code>Tokens</code>
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public Token[] getExpiredTokens() throws TrustException {
        return new Token[0];
    }

    /**
     * Return the list of ISSUED and RENEWED tokens.
     *
     * @return An array of ISSUED and RENEWED <code>Tokens</code>.
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public Token[] getValidTokens() throws TrustException {
        return new Token[0];
    }

    /**
     * Return the list of RENEWED tokens.
     *
     * @return An array of RENEWED <code>Tokens</code>
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public Token[] getRenewedTokens() throws TrustException {
        return new Token[0];
    }

    /**
     * Return the list of CANCELLED tokens
     *
     * @return An array of CANCELLED <code>Tokens</code>
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public Token[] getCancelledTokens() throws TrustException {
        return new Token[0];
    }

    /**
     * Returns the <code>Token</code> of the given id
     *
     * @param id
     * @return The requested <code>Token</code> identified by the give id
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public Token getToken(String id) throws TrustException {
        return null;
    }

    /**
     * Removes the given token from token storage.
     *
     * @param id Token id to remove.
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public void removeToken(String id) throws TrustException {

    }

    /**
     * Retrieves the tokens in volatile memory if any.
     *
     * @return
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public List<Token> getStorageTokens() throws TrustException {
        return null;
    }

    /**
     * Implements how persistence of volatile tokens should be handled
     * while managing concurrency issues.
     *
     * @param persistingTokens
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public void handlePersistence(List<?> persistingTokens) throws TrustException {

    }

    /**
     * Implements how volatile tokens should be persisted upon server shutdown.
     *
     * @throws org.apache.rahas.TrustException
     */
    @Override
    public void handlePersistenceOnShutdown() throws TrustException {

    }
}
