/**
 *
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.thrift.authentication.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.wso2.carbon.identity.thrift.authentication.client.exception.ThriftAuthenticationException;
import org.wso2.carbon.identity.thrift.authentication.client.internal.util.ThriftAuthenticationClientConstants;

/**
 * Abstract Thrift Authentication Client
 */
public abstract class AbstractThriftAuthenticationClient {

    private GenericKeyedObjectPool secureTransportPool;

    private static Log log = LogFactory.getLog(AbstractThriftAuthenticationClient.class);

    public AbstractThriftAuthenticationClient(GenericKeyedObjectPool secureTransportPool) {
        this.secureTransportPool = secureTransportPool;
    }

    public String authenticate(String userName, String password, ThriftAuthenticationClient.Protocol protocol, String host, int port) throws ThriftAuthenticationException {
        Object client = null;

        try {
            client = secureTransportPool.borrowObject(constructUrl(protocol, host, port));
            return authenticate(client, userName, password);
        } catch (ThriftAuthenticationException e) {
            throw new ThriftAuthenticationException(e.getCause().getMessage());
        } catch (Exception e) {
            throw new ThriftAuthenticationException("Error connecting to " + constructUrl(protocol, host, port), e);
        } finally {
            try {
                secureTransportPool.returnObject(constructUrl(protocol, host, port), client);
            } catch (Exception e) {
                secureTransportPool.clear(constructUrl(protocol, host, port));
            }
        }
    }

    private String constructUrl(ThriftAuthenticationClient.Protocol protocol, String host, int port) {
        return new StringBuilder().append(protocol.toString()).append("://").append(host.trim()).append(":").append(port).toString();
    }

    protected abstract String authenticate(Object client, String userName, String password)
            throws ThriftAuthenticationException;

}
