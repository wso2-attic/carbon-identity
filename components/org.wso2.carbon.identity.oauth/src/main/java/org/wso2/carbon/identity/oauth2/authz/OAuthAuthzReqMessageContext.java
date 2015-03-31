/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth2.authz;

import org.apache.amber.oauth2.common.message.types.ResponseType;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;

public class OAuthAuthzReqMessageContext {

    private OAuth2AuthorizeReqDTO authorizationReqDTO;

    private ResponseType responseType;

    private String[] approvedScope;

    private long validityPeriod;

    public OAuthAuthzReqMessageContext(OAuth2AuthorizeReqDTO authorizationReqDTO) {
        this.authorizationReqDTO = authorizationReqDTO;
    }

    public OAuth2AuthorizeReqDTO getAuthorizationReqDTO() {
        return authorizationReqDTO;
    }

    public void setAuthorizationReqDTO(OAuth2AuthorizeReqDTO authorizationReqDTO) {
        this.authorizationReqDTO = authorizationReqDTO;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public String[] getApprovedScope() {
        return approvedScope;
    }

    public void setApprovedScope(String[] approvedScope) {
        this.approvedScope = approvedScope;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }
}
