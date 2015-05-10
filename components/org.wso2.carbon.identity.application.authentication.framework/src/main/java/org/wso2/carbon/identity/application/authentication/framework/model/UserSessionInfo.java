/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents all sessions details for a unique user.
 */
public class UserSessionInfo implements Serializable {
    private static final long serialVersionUID = 1565107988768069823L;
    private String userName = null;
    private String userStoreDomain = null;
    private String tenantDomain = null;
    private boolean hasKillPermission = false;
    private ArrayList<SessionInfo> sessionsList = null;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserStoreDomain() {
        return userStoreDomain;
    }

    public void setUserStoreDomain(String userStoreDomain) {
        this.userStoreDomain = userStoreDomain;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public boolean isHasKillPermission() {
        return hasKillPermission;
    }

    public void setHasKillPermission(boolean hasKillPermission) {
        this.hasKillPermission = hasKillPermission;
    }

    public ArrayList<SessionInfo> getSessionsList() {
        return sessionsList;
    }

    public void setSessionsList(ArrayList<SessionInfo> sessionsList) {
        this.sessionsList = sessionsList;
    }
}

