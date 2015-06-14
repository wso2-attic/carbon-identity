/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.scim.ui.utils;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.scim.common.stub.config.SCIMProviderDTO;
import org.wso2.carbon.identity.scim.ui.SCIMConstants;

public class SCIMUIUtils {

    private SCIMUIUtils(){}

    public static String getGlobalConsumerId() {
        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    public static String getUserConsumerId(String loggedInUser) {
        String currentTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String consumerId = loggedInUser + "@" + currentTenantDomain;
        return consumerId;
    }

    public static SCIMProviderDTO[] doPaging(int pageNumber, SCIMProviderDTO[] scimProviderDTOSet) {

        int itemsPerPageInt = SCIMConstants.DEFAULT_ITEMS_PER_PAGE;
        SCIMProviderDTO[] returnedSCIMProviderDTOSet;

        int startIndex = pageNumber * itemsPerPageInt;
        int endIndex = (pageNumber + 1) * itemsPerPageInt;
        if (itemsPerPageInt < scimProviderDTOSet.length) {
            returnedSCIMProviderDTOSet = new SCIMProviderDTO[itemsPerPageInt];
        } else {
            returnedSCIMProviderDTOSet = new SCIMProviderDTO[scimProviderDTOSet.length];
        }
        for (int i = startIndex, j = 0; i < endIndex && i < scimProviderDTOSet.length; i++, j++) {
            returnedSCIMProviderDTOSet[j] = scimProviderDTOSet[i];
        }

        return returnedSCIMProviderDTOSet;
    }
}
