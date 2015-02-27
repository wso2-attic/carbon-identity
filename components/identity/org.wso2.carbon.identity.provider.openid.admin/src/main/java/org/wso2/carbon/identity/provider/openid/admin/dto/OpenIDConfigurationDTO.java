/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid.admin.dto;

public class OpenIDConfigurationDTO {

    private String subDomain;

    private String defaultOpenIDPattern;

    private String tenantOpenIDPattern;

    private String[] availableTenantOpenIDPattern;

    private String domainName;

    private String userName;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public String getDefaultOpenIDPattern() {
        return defaultOpenIDPattern;
    }

    public void setDefaultOpenIDPattern(String defaultOpenIDPattern) {
        this.defaultOpenIDPattern = defaultOpenIDPattern;
    }

    public String getTenantOpenIDPattern() {
        return tenantOpenIDPattern;
    }

    public void setTenantOpenIDPattern(String tenantOpenIDPattern) {
        this.tenantOpenIDPattern = tenantOpenIDPattern;
    }

    public String[] getAvailableTenantOpenIDPattern() {
        return availableTenantOpenIDPattern;
    }

    public void setAvailableTenantOpenIDPattern(
            String[] availableTenantOpenIDPattern) {
        this.availableTenantOpenIDPattern = availableTenantOpenIDPattern;
    }

}