/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 * /
 */

package org.wso2.carbon.idp.mgt.listener;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

/**
 * Created by maduranga on 10/9/15.
 */
public class ResidentIdentityProviderMgtListener extends AbstractIdentityProviderMgtListener {

    @Override
    public int getDefaultOrderId() {
        return 30;
    }

    @Override
    public boolean doPreDeleteIdP(String idPName, int tenantId) throws IdentityProviderManagementException {

        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);
        if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(idPName)) {
            throw new IdentityProviderManagementException("Can't delete Resident Identity Provider for tenant " +
                    tenantDomain);
        }
        return true;
    }
}
