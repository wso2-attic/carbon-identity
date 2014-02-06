/*
* Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sts.passive.internal;

import org.wso2.carbon.identity.sts.passive.ClaimDTO;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTrustedServiceStore {
    private static Map<String, ClaimDTO> claimDTOMap = new ConcurrentHashMap<String, ClaimDTO>();

    public ClaimDTO[] getAllTrustedServices() throws Exception {
        ArrayList<ClaimDTO> claimDTOArrayList = new ArrayList<ClaimDTO>(claimDTOMap.values());
        return claimDTOArrayList.toArray(new ClaimDTO[claimDTOArrayList.size()]);
    }

    public ClaimDTO getTrustedServiceClaims(String realmName) throws Exception {
        return claimDTOMap.get(realmName);
    }

    public void addTrustedService(String realmName, String claimDialect, String claims)
            throws Exception {
        ClaimDTO claimDTO = new ClaimDTO();
        claimDTO.setRealm(realmName);
        claimDTO.setClaimDialect(claimDialect);
        claimDTO.setDefaultClaims(claims.split(","));
        claimDTOMap.put(realmName, claimDTO);
    }

    public void removeTrustedService(String realmName) throws Exception {
        claimDTOMap.remove(realmName);
    }

    public void addTrustedServices(ClaimDTO[] claimDTOs) throws Exception {
        for (ClaimDTO claimDTO : claimDTOs) {
            claimDTOMap.put(claimDTO.getRealm(), claimDTO);
        }
    }
}