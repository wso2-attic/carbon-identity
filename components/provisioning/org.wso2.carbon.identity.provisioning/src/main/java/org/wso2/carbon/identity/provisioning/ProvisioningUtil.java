/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provisioning;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProvisioningUtil {

    private ProvisioningUtil(){}

    /**
     * @return
     */
    public static List<String> getClaimValues(Map<ClaimMapping, List<String>> attributeMap,
                                              String claimUri, String userStoreDomainName) {

        List<String> claimValues = new ArrayList<String>();
        for (Map.Entry<ClaimMapping, List<String>> entry : attributeMap.entrySet()) {
            ClaimMapping mapping = entry.getKey();
            if (mapping.getLocalClaim() != null
                    && claimUri.equals(mapping.getLocalClaim().getClaimUri())) {
                claimValues = entry.getValue();
                break;
            }
        }

        if (userStoreDomainName != null) {

            List<String> modifiedClaimValues = new ArrayList<String>();

            for (Iterator<String> iterator = claimValues.iterator(); iterator.hasNext(); ) {
                String claimValue = iterator.next();
                if (claimValue != null && claimValue.indexOf("/") > 0) {
                    claimValue = claimValue.substring(claimValue.indexOf("/") + 1);
                }

                claimValue = userStoreDomainName + "/" + claimValue;
                modifiedClaimValues.add(claimValue);

            }

            claimValues = modifiedClaimValues;
        }

        return claimValues;
    }

    /**
     * @param claimUri
     * @param attributeList
     */
    public static void setClaimValue(String claimUri, Map<ClaimMapping, List<String>> attributeMap,
                                     List<String> attributeList) {

        ClaimMapping clmMapping = null;

        for (Map.Entry<ClaimMapping, List<String>> entry : attributeMap.entrySet()) {
            ClaimMapping mapping = entry.getKey();
            if (mapping.getLocalClaim() != null
                    && claimUri.equals(mapping.getLocalClaim().getClaimUri())) {
                clmMapping = mapping;
                break;
            }
        }

        if (clmMapping != null) {
            attributeMap.put(clmMapping, attributeList);
        }
    }
}
