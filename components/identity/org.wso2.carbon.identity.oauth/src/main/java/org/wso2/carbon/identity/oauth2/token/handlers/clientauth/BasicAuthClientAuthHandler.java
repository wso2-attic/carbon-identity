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

package org.wso2.carbon.identity.oauth2.token.handlers.clientauth;

import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

public class BasicAuthClientAuthHandler implements ClientAuthenticationHandler {

    @Override
    public void init() throws IdentityOAuth2Exception {

    }

    @Override
    public boolean canAuthenticate(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        if (tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId() != null &&
                tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientSecret() != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean authenticateClient(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        OAuth2AccessTokenReqDTO oAuth2AccessTokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();
        try {
            return OAuth2Util.authenticateClient(
                    oAuth2AccessTokenReqDTO.getClientId(),
                    oAuth2AccessTokenReqDTO.getClientSecret());
        } catch (IdentityOAuthAdminException e) {
            throw new IdentityOAuth2Exception(e.getMessage(), e);
        }
    }
}
