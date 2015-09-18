/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.idp.mgt.listener;

import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

public interface IdentityProviderMgtListener {

    /**
     * Get the execution order identifier for this listener.
     *
     * @return The execution order identifier integer value.
     */
    int getExecutionOrderId();

    public boolean isEnable();

    public boolean doPreUpdateResidentIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException;

    public boolean doPostUpdateResidentIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException;

    public boolean doPreAddIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException;

    public boolean doPostAddIdP(IdentityProvider identityProvider) throws IdentityProviderManagementException;

    public boolean doPreDeleteIdP(String idPName) throws IdentityProviderManagementException;

    public boolean doPostDeleteIdP(String idPName) throws IdentityProviderManagementException;

    public boolean doPreUpdateIdP(String oldIdPName, IdentityProvider identityProvider) throws IdentityProviderManagementException;

    public boolean doPostUpdateIdP(String oldIdPName, IdentityProvider identityProvider) throws IdentityProviderManagementException;

}
