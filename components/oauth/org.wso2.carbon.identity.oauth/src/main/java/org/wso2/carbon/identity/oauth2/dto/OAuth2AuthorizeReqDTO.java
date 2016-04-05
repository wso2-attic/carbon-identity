/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.dto;

import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;

import java.util.LinkedHashSet;
import java.util.Properties;

public class OAuth2AuthorizeReqDTO {
    private String consumerKey;
    private String[] scopes;
    private String responseType;
    private String callbackUrl;
    private AuthenticatedUser user;
    private String password;
    private LinkedHashSet acrValues;
    private String nonce;
    private String tenantDomain;
    private Properties properties = new Properties();

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String[] getScopes() {
        return scopes.clone();
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes.clone();
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public LinkedHashSet getACRValues() {
        return acrValues;
    }

    public void setACRValues(LinkedHashSet acrValues) {
        this.acrValues = acrValues;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getNonce() {
        return this.nonce;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public void addProperty(Object propName, Object propValue) {
        properties.put(propName, propValue);
    }

    public Object getProperty(Object propName) {
        return properties.get(propName);
    }

}
