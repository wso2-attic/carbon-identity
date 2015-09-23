/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.i18n.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.i18n.mgt.config.ConfigBuilder;
import org.wso2.carbon.i18n.mgt.config.ConfigType;
import org.wso2.carbon.i18n.mgt.config.StorageType;
import org.wso2.carbon.i18n.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

public class TenantManagementListener implements TenantMgtListener {

    private static final int EXEC_ORDER = 21;
    private static Log log = LogFactory.getLog(TenantManagementListener.class);

    /**
     * Add the default Email Templates to the registry when a new tenant is registered.
     *
     * @param tenantInfo Information about the newly created tenant.
     */
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
        //Load email template configuration on tenant creation.
        int tenantId = tenantInfo.getTenantId();

        ConfigBuilder configBuilder = ConfigBuilder.getInstance();
        try {
            configBuilder.loadDefaultConfiguration(ConfigType.EMAIL, StorageType.REGISTRY, tenantId);
        } catch (I18nMgtEmailConfigException e) {
            String message = "Error occurred while loading default email templates for the tenant " +
                    " " + tenantInfo.getTenantDomain();
            throw new StratosException(message, e);
        }
    }

    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    @Override
    public void onPreDelete(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    @Override
    public void onTenantDelete(int i) {
        // It is not required to implement this method for IdP mgt.
    }

    public void onTenantRename(int tenantId, String oldDomainName,
                               String newDomainName) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, String newPlan) throws StratosException {
        // It is not required to implement this method for I18n mgt.
    }

}
