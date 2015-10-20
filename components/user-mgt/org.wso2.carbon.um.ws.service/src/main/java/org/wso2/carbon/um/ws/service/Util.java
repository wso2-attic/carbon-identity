/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.wso2.carbon.um.ws.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.user.core.UserStoreException;

/**
 * Utility class for remote user management.
 */
public class Util {


    private static final Log log = LogFactory.getLog(Util.class.getClass());
    private static final String PROTECTED_STRING = "protected";

    private Util() {
    }

    static void checkAccess(String resourceId) throws UserStoreException {

        if (isSuperTenantResource(resourceId) && !isSuperTenant()) {

            StringBuilder stringBuilder
                    = new StringBuilder("Unauthorized attempt to modify super tenant resource by tenant domain - ");
            stringBuilder.append(CarbonContext.getThreadLocalCarbonContext().getTenantDomain()).append(" tenant id - ")
                    .append(CarbonContext.getThreadLocalCarbonContext().getTenantId()).append(" user - ")
                    .append(CarbonContext.getThreadLocalCarbonContext().getUsername());
            log.warn(stringBuilder.toString());

            throw new UserStoreException("Access Denied");

        }
    }

    private static boolean isSuperTenantResource(String resource) {
        return resource.contains(PROTECTED_STRING);
    }

    static boolean isSuperTenant() {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return tenantId == MultitenantConstants.SUPER_TENANT_ID;
    }
}
