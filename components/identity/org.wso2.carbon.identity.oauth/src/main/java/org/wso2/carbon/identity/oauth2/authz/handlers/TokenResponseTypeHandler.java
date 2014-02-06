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

package org.wso2.carbon.identity.oauth2.authz.handlers;

import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class TokenResponseTypeHandler extends AbstractResponseTypeHandler {

    private static Log log = LogFactory.getLog(TokenResponseTypeHandler.class);

    @Override
    public OAuth2AuthorizeRespDTO issue(OAuthAuthzReqMessageContext oauthAuthzMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AuthorizeRespDTO respDTO = new OAuth2AuthorizeRespDTO();
        OAuth2AuthorizeReqDTO authorizationReqDTO = oauthAuthzMsgCtx.getAuthorizationReqDTO();
        respDTO.setCallbackURI(authorizationReqDTO.getCallbackUrl());
        String consumerKey = authorizationReqDTO.getConsumerKey();
        String authorizedUser = authorizationReqDTO.getUsername();
        CacheKey cacheKey = new OAuthCacheKey(consumerKey + ":" + authorizedUser);
        String userStoreDomain = null;

        //select the user store domain when multiple user stores are configured.
        if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
            userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId(authorizedUser);
        }

        synchronized ((consumerKey + ":" + authorizedUser).intern()) {
            try {
                //TODO Need to refactor this logic
                //First serve from the cache
                if (cacheEnabled) {
                    AccessTokenDO newAccessTokenDO = (AccessTokenDO) oauthCache.getValueFromCache(cacheKey);
                    if (newAccessTokenDO != null) {
                        AccessTokenDO accessTokenDO = OAuth2Util.validateAccessTokenDO(newAccessTokenDO);
                        if (accessTokenDO != null) {
                            respDTO.setAccessToken(accessTokenDO.getAccessToken());
                            respDTO.setValidityPeriod(accessTokenDO.getValidityPeriod());
                            if (log.isDebugEnabled()) {
                                log.debug("Access Token info retrieved from the cache and served to client with client id : " +
                                        consumerKey);
                            }
                            oauthCache.addToCache(cacheKey, accessTokenDO);
                            return respDTO;
                        } else {
                            oauthCache.clearCacheEntry(cacheKey);
                            //Token is expired. Mark it as expired on database
                            //TODO : Read token state from a constant
                            tokenMgtDAO.setAccessTokenState(consumerKey, authorizedUser, "EXPIRED",
                                    UUID.randomUUID().toString(),
                                    userStoreDomain);
                        }
                    }
                }

                //Check if previously issued token exists in database
                OAuth2AccessTokenRespDTO tokenRespDTO = tokenMgtDAO.getValidAccessTokenIfExist(consumerKey,authorizedUser,userStoreDomain);
                if (tokenRespDTO != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Retrieving existing valid access token for client ID" + consumerKey);
                    }
                    if (cacheEnabled) {
                        AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, authorizedUser,
                                oauthAuthzMsgCtx.getApprovedScope(), new Timestamp(System.currentTimeMillis()), tokenRespDTO.getExpiresIn(), OAuthConstants.USER_TYPE_FOR_USER_TOKEN);
                        accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
                        accessTokenDO.setAccessToken(tokenRespDTO.getAccessToken());
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token info was added to the cache for the client id : " +
                                    consumerKey);
                        }
                        oauthCache.addToCache(cacheKey, accessTokenDO);
                    }
                    respDTO.setAccessToken(tokenRespDTO.getAccessToken());
                    respDTO.setValidityPeriod(tokenRespDTO.getExpiresIn());
                    return respDTO;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Marking old token as expired for client Id : "
                                + consumerKey + " AuthorizedUser : " + authorizedUser);
                    }
                    //Token is expired. Mark it as expired on database
                    //TODO : Read token state from a constant
                    //TODO : This should move to validation check of getValidAccessTokenIfExist() method
                    tokenMgtDAO.setAccessTokenState(consumerKey, authorizedUser, "EXPIRED",
                            UUID.randomUUID().toString(),
                            userStoreDomain);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error while getting existing token for client ID" + consumerKey);
                }
            }

            //No valid access token is found in cache or database.
            //Need to issue a new access token.
            if (log.isDebugEnabled()) {
                log.debug("Issuing a new access token for "
                        + consumerKey + " AuthorizedUser : " + authorizedUser);
            }

            String accessToken;
            try {
                accessToken = oauthIssuerImpl.accessToken();
            } catch (OAuthSystemException e) {
                throw new IdentityOAuth2Exception(e.getMessage(), e);
            }

            if(OAuth2Util.checkUserNameAssertionEnabled()) {
                String userName = oauthAuthzMsgCtx.getAuthorizationReqDTO().getUsername();
                //use ':' for token & userStoreDomain separation
                String accessTokenStrToEncode = accessToken + ":" + userName;
                accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());
            }

            Timestamp timestamp = new Timestamp(new Date().getTime());
            // Default Validity Period
            long validityPeriod = OAuthServerConfiguration.getInstance().
                    getUserAccessTokenValidityPeriodInSeconds();
            // if a VALID validity period is set through the callback, then use it
            long callbackValidityPeriod = oauthAuthzMsgCtx.getValidityPeriod();
            if ((callbackValidityPeriod != OAuthConstants.UNASSIGNED_VALIDITY_PERIOD)
                    && callbackValidityPeriod > 0) {
                validityPeriod = callbackValidityPeriod;
            }
            // convert back to milliseconds
            //validityPeriod = validityPeriod * 1000;

            AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, authorizationReqDTO.getUsername(),
                    oauthAuthzMsgCtx.getApprovedScope(), timestamp, validityPeriod, OAuthConstants.USER_TYPE_FOR_USER_TOKEN);
            accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
            accessTokenDO.setAccessToken(accessToken);
            // Persist the access token in database
            tokenMgtDAO.storeAccessToken(accessToken, authorizationReqDTO.getConsumerKey(),
                    accessTokenDO, userStoreDomain);

            // Add the access token to the cache.
            if(cacheEnabled){
                oauthCache.addToCache(cacheKey, accessTokenDO);
                if(log.isDebugEnabled()){
                    log.debug("AccessTokenDO was added to the cache for client id : " +
                            authorizationReqDTO.getConsumerKey());
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Persisted an access token with " +
                        "Client ID : " + authorizationReqDTO.getConsumerKey() +
                        "authorized user : " + authorizationReqDTO.getUsername() +
                        "timestamp : " + timestamp +
                        "validity period : " + validityPeriod +
                        "scope : " + OAuth2Util.buildScopeString(oauthAuthzMsgCtx.getApprovedScope()) +
                        "callback url : " + authorizationReqDTO.getCallbackUrl() +
                        "Token State : " + OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE +
                        "User Type : " + OAuthConstants.USER_TYPE_FOR_USER_TOKEN);
            }

            respDTO.setAccessToken(accessToken);
            respDTO.setValidityPeriod(validityPeriod);
            return respDTO;
        }
    }
}
