/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.impl.bean;

/**
 * BPSProfileDTO represent a BPS server and user credentilas.
 */
public class BPSProfile {
    private String profileName;
    private String managerHostURL;
    private String workerHostURL;
    private String username;
    private char[] password;

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public String getManagerHostURL() {
        return managerHostURL;
    }

    public void setManagerHostURL(String managerHostURL) {
        this.managerHostURL = managerHostURL;
    }

    public String getWorkerHostURL() {
        return workerHostURL;
    }

    public void setWorkerHostURL(String workerHostURL) {
        this.workerHostURL = workerHostURL;
    }
}
