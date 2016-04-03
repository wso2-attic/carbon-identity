/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.event;

import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuthRevocationRequestDTO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuthRevocationResponseDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;

public interface OauthEventListener {

    /**
     * Called prior to issuing tokens.
     * Note : This won't be called for implicit grant. Use the overloaded method for implicit grant
     * @param tokenReqDTO
     * @param tokReqMsgCtx
     * @throws IdentityOAuth2Exception
     */
    void onPreTokenIssue(OAuth2AccessTokenReqDTO tokenReqDTO, OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception;

    /**
     * Called after issuing tokens
     * Note : This won't be called for implicit grant. Use the overloaded method for implicit grant
     * @param tokenReqDTO
     * @param tokenRespDTO
     * @param tokReqMsgCtx
     * @throws IdentityOAuth2Exception
     */
    void onPostTokenIssue(OAuth2AccessTokenReqDTO tokenReqDTO, OAuth2AccessTokenRespDTO tokenRespDTO,
                          OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception;

    /**
     * Called prior to issuing tokens in implicit grant
     * @param oauthAuthzMsgCtx
     * @throws IdentityOAuth2Exception
     */
    void onPreTokenIssue(OAuthAuthzReqMessageContext oauthAuthzMsgCtx)
            throws IdentityOAuth2Exception;

    /**
     * Called after generating tokens in implicit grant
     * @param oauthAuthzMsgCtx
     * @param respDTO
     * @throws IdentityOAuth2Exception
     */
    void onPostTokenIssue(OAuthAuthzReqMessageContext oauthAuthzMsgCtx,OAuth2AuthorizeRespDTO respDTO) throws
            IdentityOAuth2Exception;


    /**
     * Called prior to renewing tokens (Refresh grant)
     * @param tokenReqDTO
     * @param tokReqMsgCtx
     * @throws IdentityOAuth2Exception
     */
    void onPreTokenRenewal(OAuth2AccessTokenReqDTO tokenReqDTO, OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception;

    /**
     * Called after renewing a token
     * @param tokenReqDTO
     * @param tokenRespDTO
     * @param tokReqMsgCtx
     * @throws IdentityOAuth2Exception
     */
    void onPostTokenRenewal(OAuth2AccessTokenReqDTO tokenReqDTO, OAuth2AccessTokenRespDTO tokenRespDTO,
                            OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception;

    /**
     * Called prior to revoking a token
     * @param revokeRequestDTO
     * @throws IdentityOAuth2Exception
     */
    void onPreTokenRevocation(OAuthRevocationRequestDTO revokeRequestDTO) throws IdentityOAuth2Exception;

    /**
     * Called after revoking a token
     * @param revokeRequestDTO
     * @param revokeResponseDTO
     * @throws IdentityOAuth2Exception
     */
    void onPostTokenRevocation(OAuthRevocationRequestDTO revokeRequestDTO,
                               OAuthRevocationResponseDTO revokeResponseDTO) throws IdentityOAuth2Exception;

    /**
     * Called prior to validate an issued token
     * @param validationReqDTO
     * @throws IdentityOAuth2Exception
     */
    void onPreTokenValidation(OAuth2TokenValidationRequestDTO validationReqDTO) throws IdentityOAuth2Exception;

    /**
     * Called after validating an issued token
     * @param validationReqDTO
     * @param validationResponseDTO
     * @throws IdentityOAuth2Exception
     */
    void onPostTokenValidation(OAuth2TokenValidationRequestDTO validationReqDTO,
                               OAuth2TokenValidationResponseDTO validationResponseDTO) throws IdentityOAuth2Exception;

}
