/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.mgt.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.mgt.dto.*;
import org.wso2.carbon.identity.mgt.util.UserIdentityManagementUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.*;

/**
 * This is the admin service for the identity management. Some of these
 * operations are can only be carried out by admins. The other operations are
 * allowed to all logged in users.
 *
 * @author sga
 */
public class UserIdentityManagementAdminService {

    private static Log log = LogFactory.getLog(UserIdentityManagementAdminService.class);

    /**
     * Get all the configurations belong to a tenant.
     *
     * @return Configurations for the tenant ID
     */
    public TenantConfigDTO[] getAllConfigurations() {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        HashMap<String, String> configurationDetails = UserIdentityManagementUtil.getAllConfigurations(tenantId);
        TenantConfigDTO[] tenantConfigDTOs = new TenantConfigDTO[configurationDetails.size()];
        Iterator<Map.Entry<String, String>> iterator = configurationDetails.entrySet().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            Map.Entry<String, String> pair = iterator.next();
            TenantConfigDTO tenantConfigDTO = new TenantConfigDTO(pair.getKey(), pair.getValue());
            tenantConfigDTOs[count] = tenantConfigDTO;
            iterator.remove();
            ++count;
        }

        return tenantConfigDTOs;
    }

    /**
     * Set all the configurations of a tenant in database
     *
     * @param tenantConfigDTOs Configurations
     */
    public void setAllConfigurations(TenantConfigDTO[] tenantConfigDTOs) {

        HashMap<String, String> configurationDetails = new HashMap<String, String>();
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        for (int i = 0; i < tenantConfigDTOs.length; i++) {
            configurationDetails.put(tenantConfigDTOs[i].getProperty(), tenantConfigDTOs[i].getPropertyValue());
        }

        UserIdentityManagementUtil.setAllConfigurations(tenantId, configurationDetails);

    }

}
