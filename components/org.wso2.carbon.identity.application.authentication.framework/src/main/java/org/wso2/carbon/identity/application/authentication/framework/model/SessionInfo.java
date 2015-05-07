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
package org.wso2.carbon.identity.application.authentication.framework.model;

import java.io.Serializable;

/**
 * Represents Each Session Information.
 */
public class SessionInfo implements Serializable {
    private static final long serialVersionUID = 1565107988768069823L;
    private String applicationId = null;
    private String loggedInTimeStamp= null;
    private long loggedInDuration = 0;
    private String applicationTenantDomain = null;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getLoggedInTimeStamp() {
        return loggedInTimeStamp;
    }

    public void setLoggedInTimeStamp(String loggedInTimeStamp) {
        this.loggedInTimeStamp = loggedInTimeStamp;
    }

    public long getLoggedInDuration() {
        return loggedInDuration;
    }

    public void setLoggedInDuration(long loggedInDuration) {
        this.loggedInDuration = loggedInDuration;
    }

    public String getApplicationTenantDomain() {
        return applicationTenantDomain;
    }

    public void setApplicationTenantDomain(String applicationTenantDomain) {
        this.applicationTenantDomain = applicationTenantDomain;
    }
}
