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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;

import java.util.Map;

/**
 * Contains authenticated user attributes and nonce value.
 */
public class AuthorizationGrantCacheEntry extends CacheEntry {

    private static final long serialVersionUID = -3043225645166013281L;

    private String codeId;

    private String tokenId;

    private Map<ClaimMapping, String> userAttributes;

    private String nonceValue;

    public AuthorizationGrantCacheEntry(Map<ClaimMapping, String> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public String getNonceValue() {
        return nonceValue;
    }

    public void setNonceValue(String nonceValue) {
        this.nonceValue = nonceValue;
    }

    public Map<ClaimMapping, String> getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(Map<ClaimMapping, String> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public String getCodeId(){
        return codeId;
    }

    public void setCodeId(String codeId){
        this.codeId = codeId;
    }

    public String getTokenId(){
        return tokenId;
    }

    public void setTokenId(String tokenId){
        this.tokenId = tokenId;
    }
}
