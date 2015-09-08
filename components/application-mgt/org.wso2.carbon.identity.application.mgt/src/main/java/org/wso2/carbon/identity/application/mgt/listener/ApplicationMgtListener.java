/*
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.application.mgt.listener;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;

public interface ApplicationMgtListener {

    public boolean doPreCreateApplication(ServiceProvider serviceProvider) throws IdentityApplicationManagementException;

    public boolean doPostCreateApplication(ServiceProvider serviceProvider) throws IdentityApplicationManagementException;

    public boolean doPreUpdateApplication(ServiceProvider serviceProvider) throws IdentityApplicationManagementException;

    public boolean doPostUpdateApplication(ServiceProvider serviceProvider) throws IdentityApplicationManagementException;

    public boolean doPreDeleteApplication(String applicationName) throws IdentityApplicationManagementException;

    public boolean doPostDeleteApplication(String applicationName) throws IdentityApplicationManagementException;


}
