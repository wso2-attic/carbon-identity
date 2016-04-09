/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma.dto;

import org.wso2.carbon.identity.uma.UMAConstants;

public class UmaOAuthIntropectResponse extends UmaResponse {

    protected UmaOAuthIntropectResponse(int responseStatus) {
        super(responseStatus);
    }

    public static UmaOAuthIntrospectRespBuilder status(int code) {
        return new UmaOAuthIntrospectRespBuilder(code);
    }

    public static class UmaOAuthIntrospectRespBuilder extends UmaResponseBuilder{

        public UmaOAuthIntrospectRespBuilder(int responseStatus) {
            super(responseStatus);
        }

        public UmaOAuthIntrospectRespBuilder setActive(boolean activeStatus){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_ACTIVE, activeStatus);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setScope(String scope){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_SCOPE,scope);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setClientId(String clientId){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_CLIENT_ID,clientId);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setUserId(String userId){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_USER_ID,userId);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setTokenType(String tokenType){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_TOKEN_TYPE, tokenType);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setExpiraryTime(Long expiraryTime){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_EXP, expiraryTime);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setIssuedAt(Long issuedAt){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_IAT, issuedAt);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setNotBefore(Long notBefore){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_NBF, notBefore);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setSubject(String subject){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_SUB, subject);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setAudience(String audience){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_AUD, audience);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setIssuer(String issuer){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_ISS, issuer);
            return this;
        }

        public UmaOAuthIntrospectRespBuilder setTokenId(String tokenId){
            this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_JTI, tokenId);
            return this;
        }

        @Override
        public UmaResponse buildJSONResponse() {

            if (getParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_ACTIVE) == null){
                this.setParam(UMAConstants.OAuthIntrospectConstants.RESP_FIELD_ACTIVE, false);
            }

            return super.buildJSONResponse();
        }
    }
}
