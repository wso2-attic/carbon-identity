/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.provider.openid.admin;

import org.wso2.carbon.core.util.AdminServicesUtil;
import org.wso2.carbon.identity.core.model.OpenIDAdminDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.provider.openid.admin.dto.OpenIDConfigurationDTO;

public class OpenIDConfigurationService {

    private static final String DEFAULT_OPENID_PATTERN = "https://identity.cloud.wso2.com/openid/{user}";
    private static final String OPENID_PATTERN_1 = "https://{subdomain}.yourdomain/openid/{userName}";
    private static final String OPENID_PATTERN_2 = "https://{user}.yourdomain/openid";

    public OpenIDConfigurationDTO getOpenIDConfiguration(String userName, String domainName)
            throws Exception {
        IdentityPersistenceManager persistenceManager = null;
        OpenIDAdminDO opdo = null;
        OpenIDConfigurationDTO configuration = null;

        persistenceManager = IdentityPersistenceManager.getPersistanceManager();
        opdo = persistenceManager.getOpenIDAdmin(IdentityTenantUtil.getRegistry());
        configuration = new OpenIDConfigurationDTO();
        configuration.setUserName(userName);
        configuration.setDomainName(domainName);
        if (opdo != null) {
            configuration.setSubDomain(opdo.getSubDomain());
            configuration.setTenantOpenIDPattern(opdo.getTenantOpenIDPattern());
        } else {
            configuration.setSubDomain("identity");
        }        
        configuration.setDefaultOpenIDPattern(DEFAULT_OPENID_PATTERN);
        configuration.setAvailableTenantOpenIDPattern(new String[] { OPENID_PATTERN_1,
                OPENID_PATTERN_2 });
        return configuration;
    }

    public void createOrUpdateOpenIDCOnfiguration(OpenIDConfigurationDTO configuration)
            throws Exception {
        IdentityPersistenceManager persistenceManager = null;
        OpenIDAdminDO opdo = null;

        persistenceManager = IdentityPersistenceManager.getPersistanceManager();
        opdo = new OpenIDAdminDO();
        opdo.setSubDomain(configuration.getSubDomain());
        opdo.setTenantOpenIDPattern(configuration.getTenantOpenIDPattern());
        persistenceManager.createOrUpdateOpenIDAdmin(IdentityTenantUtil.getRegistry(), opdo);
    }
}
