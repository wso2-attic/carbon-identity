/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth2.model;

import org.wso2.carbon.identity.oauth.cache.CacheEntry;

import java.sql.Timestamp;

public class AccessTokenDO extends CacheEntry {

    private static final long serialVersionUID = -8123522530178387354L;

    private String consumerKey;

    private String authzUser;

    private String[] scope;

    private String tokenState;

    private String refreshToken;

    private String accessToken;

    private Timestamp issuedTime;

    private long validityPeriodInMillis;

    private int tenantID;

    private String tokenType;
    private long validityPeriod;

    public AccessTokenDO(String consumerKey, String authzUser, String[] scope, Timestamp issuedTime, long validityPeriod, String tokenType) {
        this.consumerKey = consumerKey;
        this.authzUser = authzUser;
        this.scope = scope;
        this.issuedTime = issuedTime;
        this.validityPeriod = validityPeriod;
        this.validityPeriodInMillis = validityPeriod * 1000;
        this.tokenType = tokenType;
    }

    public int getTenantID() {
        return tenantID;
    }

    public void setTenantID(int tenantID) {
        this.tenantID = tenantID;
    }

    public String getConsumerKey() {
        return consumerKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public String getAuthzUser() {
        return authzUser;
    }

    public String[] getScope() {
        return scope;
    }

    public void setScope(String[] scope) {
        this.scope = scope;
    }

    public Timestamp getIssuedTime() {
        return issuedTime;
    }

    public void setIssuedTime(Timestamp issuedTime) {
        this.issuedTime = issuedTime;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public String getTokenState() {
        return tokenState;
    }

    public void setTokenState(String tokenState) {
        this.tokenState = tokenState;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getValidityPeriodInMillis() {
        return validityPeriodInMillis;
    }

    public void setValidityPeriodInMillis(long validityPeriodInMillis) {
        this.validityPeriodInMillis = validityPeriodInMillis;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
