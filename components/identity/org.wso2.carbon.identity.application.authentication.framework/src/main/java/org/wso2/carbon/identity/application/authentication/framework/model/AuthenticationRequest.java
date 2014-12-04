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

package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This cache keeps all parameters and headers which are directed towards authentication
 * framework. Whenever a request to authentication framework comes, The relevant component which
 * sends the request saves all required information to this cache, which are retrieved later from
 * authentication framework
 */
public class AuthenticationRequest implements Serializable {
    private static final long serialVersionUID = -5407487459807348541L;

    private String type;
    private String commonAuthCallerPath;
    private boolean forceAuth;
    private boolean passiveAuth;
    private String tenantDomain;
    private boolean isPost;
    private String relyingParty;
    //used to store query params which should be sent to Authentication Framework
    private Map<String, String[]> requestQueryParams = new HashMap<String, String[]>();
    //used to store request headers which should be sent to Authentication Framework.
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    public Map<String, String[]> getRequestQueryParams() {
        return requestQueryParams;
    }

    /**
     * Set request query params which are comming from the calling servelets
     * @param requestQueryParams Map of query params
     */
    public void setRequestQueryParams(Map<String, String[]> requestQueryParams) {
        this.requestQueryParams.putAll(requestQueryParams);
    }

    public void addHeader(String key, String values) {
        requestHeaders.put(key, values);
    }

    public String getRelyingParty() {
        return relyingParty;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRelyingParty(String relyingParty) {
        this.relyingParty = relyingParty;
    }

    public boolean isPost() {
        return isPost;
    }

    public void setPost(boolean post) {
        isPost = post;
    }

    public String[] addRequestQueryParam(String key, String value[]) {
        return requestQueryParams.put(key, value);
    }

    public String[] getRequestQueryParam(String key) {
        return requestQueryParams.get(key);
    }

    public void appendRequestQueryParams(Map<String, String[]> map) {
        requestQueryParams.putAll(map);
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public boolean getForceAuth() {
        return forceAuth;
    }

    public void setForceAuth(boolean forceAuth) {
        this.forceAuth = forceAuth;
    }

    public String getCommonAuthCallerPath() {
        return commonAuthCallerPath;
    }

    public void setCommonAuthCallerPath(String commonAuthCallerPath) {
        this.commonAuthCallerPath = commonAuthCallerPath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getPassiveAuth() {
        return passiveAuth;
    }

    public void setPassiveAuth(boolean passiveAuth) {
        this.passiveAuth = passiveAuth;
    }

}
