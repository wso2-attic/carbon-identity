/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.thrift.authentication.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.thrift.TException;
import org.wso2.carbon.identity.thrift.authentication.client.exception.ThriftAuthenticationException;
import org.wso2.carbon.identity.thrift.authentication.client.internal.generatedCode.AuthenticationException;
import org.wso2.carbon.identity.thrift.authentication.client.internal.generatedCode.AuthenticatorService;

/**
 * Implementation of the ThriftAuthenticator that authenticates based on the provided client
 */
public class ThriftAuthenticationClient extends AbstractThriftAuthenticationClient {

    private static Log log = LogFactory.getLog(ThriftAuthenticationClient.class);

    public enum Protocol {
        SSL, HTTPS
    }

    public ThriftAuthenticationClient(GenericKeyedObjectPool secureTransportPool) {
        super(secureTransportPool);
    }

    @Override
    protected String authenticate(Object client, String userName, String password)
            throws ThriftAuthenticationException {
        try {
            return ((AuthenticatorService.Client) client).authenticate(userName, password);
        } catch (AuthenticationException e) {
            throw new ThriftAuthenticationException("Thrift Authentication Exception", e);
        } catch (TException e) {
            throw new ThriftAuthenticationException("Thrift exception", e);
        }
    }
}
