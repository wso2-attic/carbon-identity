/*
*Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.oauth.config.OAuthClientAuthHandlerMetaData;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;

import java.util.Set;


public abstract class AbstractClientAuthHandler implements ClientAuthenticationHandler {

    public boolean canAuthenticate(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenReqDTO oAuth2AccessTokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();

        if (!StringUtils.isEmpty(oAuth2AccessTokenReqDTO.getClientId()) &&
                !StringUtils.isEmpty(oAuth2AccessTokenReqDTO.getClientSecret())) {
            return true;

        } else if (org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString().equals(
                oAuth2AccessTokenReqDTO.getGrantType())) {

            String authHandlerClass = this.getClass().getName();

            //Setting default value for strict client authentication as true
            boolean isStrictClientAuthEnabled = true;

            Set<OAuthClientAuthHandlerMetaData> clientAuthHandlerMetaData =
                    OAuthServerConfiguration.getInstance().getClientAuthHandlerMetaData();
            for (OAuthClientAuthHandlerMetaData metaData : clientAuthHandlerMetaData) {
                if (authHandlerClass.equals(metaData.getClassName())) {
                    isStrictClientAuthEnabled = metaData.isClientAuthenticationEnabled();
                    break;
                }
            }
            return !isStrictClientAuthEnabled;
        }
        return false;
    }

    public boolean authenticateClient(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenReqDTO oAuth2AccessTokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();

        if (StringUtils.isEmpty(oAuth2AccessTokenReqDTO.getClientSecret())) {
            String authHandlerClass = this.getClass().getName();
            boolean isStrictValidationEnabled = true;

            //Checking if credential validation is needed according to the config
            Set<OAuthClientAuthHandlerMetaData> clientAuthHandlerMetaData =
                    OAuthServerConfiguration.getInstance().getClientAuthHandlerMetaData();
            for (OAuthClientAuthHandlerMetaData metaData : clientAuthHandlerMetaData) {
                if (authHandlerClass.equals(metaData.getClassName())) {
                    isStrictValidationEnabled = metaData.isClientAuthenticationEnabled();
                    break;
                }
            }

            //Skipping credentials validation for saml2 bearer if not configured as needed
            if (org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString()
                    .equals(oAuth2AccessTokenReqDTO.getGrantType()) && !isStrictValidationEnabled) {
                return true;
            }
        }
        return false;
    }
}
