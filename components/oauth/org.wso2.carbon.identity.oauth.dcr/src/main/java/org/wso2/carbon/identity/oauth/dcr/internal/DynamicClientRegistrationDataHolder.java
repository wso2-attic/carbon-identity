/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.dcr.internal;

import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;

/**
 * This is the DataHolder class of DynamicClientRegistration bundle. This holds a reference to the
 * ApplicationManagementService.
 */
public class DynamicClientRegistrationDataHolder {

    private ApplicationManagementService applicationManagementService;

    private static DynamicClientRegistrationDataHolder thisInstance = new DynamicClientRegistrationDataHolder();

    private DynamicClientRegistrationDataHolder() {
    }

    public static DynamicClientRegistrationDataHolder getInstance() {
        return thisInstance;
    }

    public ApplicationManagementService getApplicationManagementService() {
        if (applicationManagementService == null) {
            throw new IllegalStateException("ApplicationManagementService is not initialized properly");
        }
        return applicationManagementService;
    }

    public void setApplicationManagementService(ApplicationManagementService applicationManagementService) {
        this.applicationManagementService = applicationManagementService;
    }
}
