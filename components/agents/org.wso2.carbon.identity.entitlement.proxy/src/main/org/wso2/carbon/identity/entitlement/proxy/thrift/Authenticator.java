/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.identity.entitlement.proxy.thrift;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.wso2.carbon.identity.entitlement.proxy.exception.EntitlementProxyException;
import org.wso2.carbon.identity.entitlement.proxy.generatedCode.AuthenticatorService;

public class Authenticator {

    private String userName;
    private String password;
    private String serverUrl;
    private String sessionId;

    public Authenticator(String userName, String password, String serverUrl) throws Exception {
        this.userName = userName;
        this.password = password;
        this.serverUrl = serverUrl;

        if (!authenticate()) {
            throw new Exception("Authentication Failed");
        }
    }

    private boolean authenticate() throws Exception {
        boolean isAuthenticated;
        try {
            THttpClient client = new THttpClient(serverUrl);
            TProtocol protocol = new TCompactProtocol(client);
            AuthenticatorService.Client authClient = new AuthenticatorService.Client(protocol);
            client.open();
            sessionId = authClient.authenticate(userName, password);
            client.close();
            isAuthenticated = true;
        } catch (TException e) {
            throw new EntitlementProxyException("Error while authenticating with ThriftAuthenticator", e);
        }
        return isAuthenticated;

    }

    public String getSessionId(boolean isExpired) throws Exception {
        if (isExpired) {
            authenticate();
        }
        return sessionId;
    }

}