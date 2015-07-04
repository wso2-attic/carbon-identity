/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.user.account.association.internal;


import org.wso2.carbon.identity.user.account.association.dao.UserAccountAssociationDAO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;
import org.wso2.carbon.identity.user.account.association.util.UserAccountAssociationConstants;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

public class TenantMgtListenerImpl implements TenantMgtListener {


    private static final int EXEC_ORDER = 20;

    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {
        return;
    }

    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {
        return;
    }

    @Override
    public void onTenantDelete(int i) {
        return;
    }

    @Override
    public void onTenantRename(int i, String s, String s2) throws StratosException {
        return;
    }

    @Override
    public void onTenantInitialActivation(int i) throws StratosException {
        return;
    }

    @Override
    public void onTenantActivation(int i) throws StratosException {
        return;
    }

    @Override
    public void onTenantDeactivation(int i) throws StratosException {
        return;
    }

    @Override
    public void onSubscriptionPlanChange(int i, String s, String s2) throws StratosException {
        return;
    }

    @Override
    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    @Override
    public void onPreDelete(int i) throws StratosException {
        try {
            UserAccountAssociationDAO.getInstance().deleteUserAssociationsFromTenantId(i);
        } catch (UserAccountAssociationException e) {
            throw new StratosException(String.format(UserAccountAssociationConstants.ErrorMessages
                    .CONN_DELETE_FROM_TENANT_ID_ERROR.getDescription(), i), e);
        }
    }
}
