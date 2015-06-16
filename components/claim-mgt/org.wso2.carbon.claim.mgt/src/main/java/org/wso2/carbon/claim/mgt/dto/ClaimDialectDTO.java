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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.claim.mgt.dto;

public class ClaimDialectDTO {
    private ClaimMappingDTO[] claimMappings;

    private String dialectURI;
    private String userStore;

    public ClaimMappingDTO[] getClaimMappings() {
        if (claimMappings != null) {
            return claimMappings.clone();
        } else {
            return new ClaimMappingDTO[0];
        }
    }

    public void setClaimMappings(ClaimMappingDTO[] claimMappings) {
        if (claimMappings != null) {
            this.claimMappings = claimMappings.clone();
        }
    }

    public String getDialectURI() {
        return dialectURI;
    }

    public void setDialectURI(String dialectURI) {
        this.dialectURI = dialectURI;
    }

    public String getUserStore() {
        return userStore;
    }

    public void setUserStore(String userStore) {
        this.userStore = userStore;
    }
}
