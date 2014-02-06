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
package org.wso2.carbon.identity.scim.common.config;

import java.util.HashMap;
import java.util.Map;

public class SCIMProviderDTO {

    private String providerId;
    private String userName;
    private String password;
    private String userEPURL;
    private String groupEPURL;
    private String bulkEPURL;

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserEPURL() {
        return userEPURL;
    }

    public void setUserEPURL(String userEPURL) {
        this.userEPURL = userEPURL;
    }

    public String getGroupEPURL() {
        return groupEPURL;
    }

    public void setGroupEPURL(String groupEPURL) {
        this.groupEPURL = groupEPURL;
    }

    public String getBulkEPURL() {
        return bulkEPURL;
    }

    public void setBulkEPURL(String bulkEPURL) {
        this.bulkEPURL = bulkEPURL;
    }
}
