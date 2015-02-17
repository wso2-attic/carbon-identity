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
package org.wso2.carbon.identity.relyingparty.dto;

import java.util.Arrays;

public class OpenIDAuthInfoDTO {

    private String[] requiredClaims;

    private String[] optionalClaims;

    private String[] requestTypes;

    private String realm;

    private boolean requestClaimsFromIdP = false;

    public String[] getRequiredClaims() {
        return Arrays.copyOf(requiredClaims, requiredClaims.length);
    }

    public void setRequiredClaims(String[] requiredClaims) {
        this.requiredClaims = Arrays.copyOf(requiredClaims, requiredClaims.length);
    }

    public String[] getOptionalClaims() {
        return Arrays.copyOf(optionalClaims, optionalClaims.length);
    }

    public void setOptionalClaims(String[] optionalClaims) {
        this.optionalClaims = Arrays.copyOf(optionalClaims, optionalClaims.length);
    }

    public String[] getRequestTypes() {
        return Arrays.copyOf(requestTypes, requestTypes.length);
    }

    public void setRequestTypes(String[] requestTypes) {
        this.requestTypes = Arrays.copyOf(requestTypes, requestTypes.length);
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean getRequestClaimsFromIdP() {
        return requestClaimsFromIdP;
    }

    public void setRequestClaimsFromIdP(boolean requestClaimsFromIdP) {
        this.requestClaimsFromIdP = requestClaimsFromIdP;
    }


}
