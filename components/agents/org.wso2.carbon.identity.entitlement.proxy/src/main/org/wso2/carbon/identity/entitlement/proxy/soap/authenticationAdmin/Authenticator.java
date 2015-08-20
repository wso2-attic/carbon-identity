/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 *
 */

package org.wso2.carbon.identity.entitlement.proxy.soap.authenticationAdmin;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.identity.entitlement.proxy.exception.EntitlementProxyException;

import java.util.Map;

public class Authenticator {

    private String userName;
    private String password;
    private String serverUrl;
    private String cookie;

    public Authenticator(String userName, String password, String serverUrl) throws Exception {
        this.userName = userName;
        this.password = password;
        this.serverUrl = serverUrl;

        if (!authenticate()) {
            throw new EntitlementProxyException("Authentication Failed");
        }
    }

    private boolean authenticate() throws Exception {
        ConfigurationContext configurationContext;
        configurationContext = ConfigurationContextFactory.createDefaultConfigurationContext();
        Map<String, TransportOutDescription> transportsOut =configurationContext
                .getAxisConfiguration().getTransportsOut();
        for (TransportOutDescription transportOutDescription : transportsOut.values()) {
            transportOutDescription.getSender().init(configurationContext, transportOutDescription);
        }
        AuthenticationAdminStub authAdmin = new AuthenticationAdminStub(configurationContext,
                serverUrl);
        boolean isAuthenticated = authAdmin.login(userName, password, "localhost");
        cookie = (String) authAdmin._getServiceClient().getServiceContext()
                .getProperty(HTTPConstants.COOKIE_STRING);
        authAdmin._getServiceClient().cleanupTransport();
        return isAuthenticated;

    }

    public String getCookie(boolean isExpired) throws Exception {
        if (isExpired) {
            authenticate();
        }
        return cookie;
    }

}
