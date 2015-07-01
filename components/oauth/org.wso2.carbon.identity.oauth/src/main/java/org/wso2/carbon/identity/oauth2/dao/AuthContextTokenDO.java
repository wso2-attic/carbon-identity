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

package org.wso2.carbon.identity.oauth2.dao;

import org.wso2.carbon.identity.oauth2.model.AuthzCodeDO;

/**
 *
 */
public class AuthContextTokenDO {

    private String authzCode;

    private String consumerKey;

    private String callbackUrl;

    private AuthzCodeDO authzCodeDO;

    private String tokenId;

    public AuthContextTokenDO(String authzCode, String consumerKey, String callbackUrl, AuthzCodeDO authzCodeDO) {
        this.authzCode = authzCode;
        this.consumerKey = consumerKey;
        this.callbackUrl = callbackUrl;
        this.authzCodeDO = authzCodeDO;
    }

    public AuthContextTokenDO(String authzCode) {
        this.authzCode = authzCode;
    }

    public AuthContextTokenDO(String authzCode, String tokenId) {
        this.authzCode = authzCode;
        this.tokenId = tokenId;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getAuthzCode() {
        return authzCode;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public AuthzCodeDO getAuthzCodeDO() {
        return authzCodeDO;
    }
}
