/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.notification.mgt.json.bean;

import java.util.Properties;

/**
 * this class is used as the bean object for PEP endpoints.
 * PEP configuration builder builds a list of these and sends it to cacheClearingUserOperationListner
 */
@SuppressWarnings("unused")
public class JsonEndpointInfo {
    /**
     * Address of the endpoint
     */
    private String endpoint;
    /**
     * Name of the endpoint
     */
    private String endpointName;
    /**
     * Whether needs to perform basic authentication
     */
    private boolean authenticationRequired;
    /**
     * Credentials used for calling http endpoint
     */
    private String username;
    /**
     * Passwords are stored as char arrays
     */
    private char[] password;
    /**
     * Json message string
     */
    private String jsonConfigString;
    /**
     * Endpoint level config parameters
     */
    private Properties endpointProperties;

    public Properties getEndpointsParams() {
        return endpointProperties;
    }

    public void setEndpointProperties(Properties endpointsParams) {
        this.endpointProperties =
                endpointsParams;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getJsonConfigString() {
        return jsonConfigString;
    }

    public void setJsonConfigString(String jsonConfigString) {
        this.jsonConfigString = jsonConfigString;
    }

    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        if (password != null) {
            return password.clone();
        } else {
            return new char[0];
        }
    }

    public void setPassword(char[] password) {
        if (password != null) {
            this.password = password.clone();
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
