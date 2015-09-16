/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.mgt.listener;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtListener;

public class AbstractAppAndIdpOperationEventListener implements ApplicationMgtListener, IdentityProviderMgtListener {

    public boolean doPreCreateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName) throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPostCreateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName) throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName) throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPostUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName) throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPreDeleteApplication(String applicationName, String tenantDomain, String userName) throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPostDeleteApplication(String applicationName, String tenantDomain, String userName) throws IdentityApplicationManagementException {
        return true;
    }

    public boolean doPreUpdateResidentIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPostUpdateResidentIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPreAddIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPostAddIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPreDeleteIdP(String idPName) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPostDeleteIdP(String idPName) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPreUpdateIdP(String oldIdPName, IdentityProvider identityProvider) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPostUpdateIdP(String oldIdPName, IdentityProvider identityProvider) throws IdentityProviderManagementException {
        return true;
    }

}
