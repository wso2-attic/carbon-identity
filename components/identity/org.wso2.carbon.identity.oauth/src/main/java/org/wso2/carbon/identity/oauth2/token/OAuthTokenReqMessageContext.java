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

package org.wso2.carbon.identity.oauth2.token;

import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;

import java.util.Properties;

public class OAuthTokenReqMessageContext {

    private OAuth2AccessTokenReqDTO oauth2AccessTokenReqDTO;

    private String authorizedUser;

    private String[] scope;

    private int tenantID;

    private long validityPeriod;
    
    private Properties properties = new Properties();

    public OAuthTokenReqMessageContext(OAuth2AccessTokenReqDTO oauth2AccessTokenReqDTO) {
        this.oauth2AccessTokenReqDTO = oauth2AccessTokenReqDTO;
    }

    public OAuth2AccessTokenReqDTO getOauth2AccessTokenReqDTO() {
        return oauth2AccessTokenReqDTO;
    }

    public String getAuthorizedUser() {
        return authorizedUser;
    }

    public void setAuthorizedUser(String authorizedUser) {
        this.authorizedUser = authorizedUser;
    }

    public String[] getScope() {
        return scope;
    }
    
    public int getTenantID() {
	return tenantID;
    }

    public void setTenantID(int tenantID) {
	this.tenantID = tenantID;
    }

    public void setScope(String[] scope) {
        this.scope = scope;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }
    
    public void addProperty(Object propName, Object propValue){
        properties.put(propName, propValue);
    }
    
    public Object getProperty(Object propName){
        return properties.get(propName);
    }
}
