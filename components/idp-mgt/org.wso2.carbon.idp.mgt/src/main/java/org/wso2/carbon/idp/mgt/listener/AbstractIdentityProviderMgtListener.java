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

package org.wso2.carbon.idp.mgt.listener;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.core.model.IdentityEventListener;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

public abstract class AbstractIdentityProviderMgtListener implements IdentityProviderMgtListener {

    public boolean doPreAddResidentIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPostAddResidentIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPreUpdateResidentIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPostUpdateResidentIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPreAddIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPostAddIdP(IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPreDeleteIdP(String idPName, String tenantDomain) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPostDeleteIdP(String idPName, String tenantDomain) throws IdentityProviderManagementException {
        return true;
    }

    public boolean doPreUpdateIdP(String oldIdPName, IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean doPostUpdateIdP(String oldIdPName, IdentityProvider identityProvider, String tenantDomain) throws
            IdentityProviderManagementException {
        return true;
    }

    public boolean isEnable() {
        IdentityEventListener identityEventListener = IdentityUtil.readEventListenerProperty
                (IdentityProviderMgtListener.class.getName(), this.getClass().getName());
        if (identityEventListener == null) {
            return true;
        }
        if (StringUtils.isNotBlank(identityEventListener.getEnable())) {
            return Boolean.parseBoolean(identityEventListener.getEnable());
        } else {
            return true;
        }
    }

    public int getExecutionOrderId() {
        IdentityEventListener identityEventListener = IdentityUtil.readEventListenerProperty
                (IdentityProviderMgtListener.class.getName(), this.getClass().getName());
        int orderId;
        if (identityEventListener == null) {
            orderId = IdentityCoreConstants.EVENT_LISTENER_ORDER_ID;
        } else {
            orderId = identityEventListener.getOrder();
        }
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        return getDefaultOrderId();
    }
}
