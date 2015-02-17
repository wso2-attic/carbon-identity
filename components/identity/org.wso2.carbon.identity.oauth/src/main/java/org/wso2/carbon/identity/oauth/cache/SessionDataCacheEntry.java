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

package org.wso2.carbon.identity.oauth.cache;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.oauth2.model.OAuth2Parameters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionDataCacheEntry extends CacheEntry {

    private static final long serialVersionUID = -4123547630178387354L;
    String loggedInUser;
    private OAuth2Parameters oAuth2Parameters;
    private ConcurrentMap<ClaimMapping, String> userAttributes;

    private String authenticatedIdPs;

    private String queryString = null;

    private ConcurrentMap<String, String[]> paramMap = new ConcurrentHashMap<String, String[]>();

    public OAuth2Parameters getoAuth2Parameters() {
        return oAuth2Parameters;
    }

    public void setoAuth2Parameters(OAuth2Parameters oAuth2Parameters) {
        this.oAuth2Parameters = oAuth2Parameters;
    }

    public String getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(String loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public Map<ClaimMapping, String> getUserAttributes() {
        return userAttributes;
    }

    public void setUserAttributes(ConcurrentMap<ClaimMapping, String> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public Map<String, String[]> getParamMap() {
        return paramMap;
    }

    public void setParamMap(ConcurrentMap<String, String[]> paramMap) {
        this.paramMap = paramMap;
    }

    public String getAuthenticatedIdPs() {
        return authenticatedIdPs;
    }

    public void setAuthenticatedIdPs(String authenticatedIdPs) {
        this.authenticatedIdPs = authenticatedIdPs;
    }
}
