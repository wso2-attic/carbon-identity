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

package org.wso2.carbon.identity.sts.store.util;

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.rahas.Token;
import org.apache.rahas.TrustException;
import org.wso2.carbon.identity.sts.store.SerializableToken;

import javax.xml.stream.XMLStreamException;

public class STSStoreUtils {

    private STSStoreUtils() {
    }

    /**
     * This method used to  get Serialized object to cache
     *
     * @param token Token
     * @return SerializableToken
     */
    public static SerializableToken getSerializableToken(Token token) {

        SerializableToken serializableToken = new SerializableToken();
        serializableToken.setId(token.getId());
        serializableToken.setState(token.getState());
        serializableToken.setToken(token.getToken().toString());
        if (token.getPreviousToken() != null) {
            serializableToken.setPreviousToken(token.getPreviousToken().toString());
        }
        if (token.getAttachedReference() != null) {
            serializableToken.setAttachedReference(token.getAttachedReference().toString());
        }
        if (token.getUnattachedReference() != null) {
            serializableToken.setUnattachedReference(token.getUnattachedReference().toString());
        }
        if (token.getProperties() != null) {

            serializableToken.setProperties(token.getProperties());
        }

        serializableToken.setSecret(token.getSecret());
        serializableToken.setCreated(token.getCreated());
        serializableToken.setExpires(token.getExpires());
        serializableToken.setIssuerAddress(token.getIssuerAddress());
        serializableToken.setPersistenceEnabled(token.isPersistenceEnabled());
        serializableToken.setChanged(token.isChanged());

        return serializableToken;

    }

    /**
     * This method used to get Token from SerializableToken
     *
     * @param serializableToken SerializableToken
     * @return Token
     * @throws TrustException     if failed to get Token from SerializableToken
     * @throws XMLStreamException if failed to convert SerializableToken to Token
     */
    public static Token getToken(SerializableToken serializableToken) throws TrustException, XMLStreamException {

        Token token1 = new Token(serializableToken.getId(), AXIOMUtil.stringToOM(serializableToken.getToken()),
                                 serializableToken.getCreated(), serializableToken.getExpires());

        token1.setState(serializableToken.getState());
        if (serializableToken.getPreviousToken() != null) {

            token1.setPreviousToken(AXIOMUtil.stringToOM(serializableToken.getPreviousToken()));
        }
        if (serializableToken.getAttachedReference() != null) {

            token1.setAttachedReference(AXIOMUtil.stringToOM(serializableToken.getAttachedReference()));
        }
        if (serializableToken.getUnattachedReference() != null) {

            token1.setUnattachedReference(AXIOMUtil.stringToOM(serializableToken.getUnattachedReference()));
        }
        token1.setProperties(serializableToken.getProperties());
        token1.setSecret(serializableToken.getSecret());
        token1.setIssuerAddress(serializableToken.getIssuerAddress());
        token1.setPersistenceEnabled(serializableToken.isPersistenceEnabled());
        token1.setChanged(serializableToken.isChanged());

        return token1;
    }
}
