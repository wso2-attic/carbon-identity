/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.dto;

import org.wso2.carbon.identity.application.common.model.ClaimMapping;

import java.util.Map;

public class OpenIDAuthRequestDTO {

    private OpenIDParameterDTO[] params;

    private boolean multiFactorAuthRequested;

    private boolean phishiingResistanceAuthRequest;

    private String opLocalId;

    private String openID;

    private String profileName;

    private String userSelectedClaimedId;

    private boolean authenticated;

    private Map<ClaimMapping, String> responseClaims = null;

    public String getOpenID() {
        return openID;
    }

    public void setOpenID(String openID) {
        this.openID = openID;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public OpenIDParameterDTO[] getParams() {
        return params;
    }

    public void setParams(OpenIDParameterDTO[] params) {
        this.params = params;
    }

    public boolean isMultiFactorAuthRequested() {
        return multiFactorAuthRequested;
    }

    public void setMultiFactorAuthRequested(boolean multiFactorAuthRequested) {
        this.multiFactorAuthRequested = multiFactorAuthRequested;
    }

    public boolean isPhishiingResistanceAuthRequest() {
        return phishiingResistanceAuthRequest;
    }

    public void setPhishiingResistanceAuthRequest(boolean phishiingResistanceAuthRequest) {
        this.phishiingResistanceAuthRequest = phishiingResistanceAuthRequest;
    }

    public String getOpLocalId() {
        return opLocalId;
    }

    public void setOpLocalId(String opLocalId) {
        this.opLocalId = opLocalId;
    }

    public String getUserSelectedClaimedId() {
        return userSelectedClaimedId;
    }

    public void setUserSelectedClaimedId(String userSelectedClaimedId) {
        this.userSelectedClaimedId = userSelectedClaimedId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Map<ClaimMapping, String> getResponseClaims() {
        return responseClaims;
    }

    public void setResponseClaims(Map<ClaimMapping, String> responseClaims) {
        this.responseClaims = responseClaims;
    }

}
