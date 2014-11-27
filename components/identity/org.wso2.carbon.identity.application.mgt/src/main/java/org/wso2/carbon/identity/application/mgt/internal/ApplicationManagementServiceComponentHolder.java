/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.mgt.internal;

import org.wso2.carbon.registry.api.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

public class ApplicationManagementServiceComponentHolder {
    private static RegistryService registryService;
    private static RealmService realmService;
    private static ConfigurationContextService configContextService;

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static void setRegistryService(RegistryService registryService) {
        ApplicationManagementServiceComponentHolder.registryService = registryService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static void setRealmService(RealmService realmService) {
        ApplicationManagementServiceComponentHolder.realmService = realmService;
    }

	public static ConfigurationContextService getConfigContextService() {
		return configContextService;
	}

	public static void setConfigContextService(
			ConfigurationContextService configContextService) {
		ApplicationManagementServiceComponentHolder.configContextService = configContextService;
	}
    
    
}
