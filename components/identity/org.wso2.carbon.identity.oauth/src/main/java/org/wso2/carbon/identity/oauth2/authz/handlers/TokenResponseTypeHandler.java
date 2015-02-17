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
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.OAuthAuthzReqMessageContext;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;
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
        String scope = OAuth2Util.buildScopeString(oauthAuthzMsgCtx.getApprovedScope());
        respDTO.setCallbackURI(authorizationReqDTO.getCallbackUrl());
        String consumerKey = authorizationReqDTO.getConsumerKey();
        String authorizedUser = authorizationReqDTO.getUsername();
        OAuthCacheKey cacheKey = new OAuthCacheKey(consumerKey + ":" + authorizedUser + ":" + scope);
        String userStoreDomain = null;

        //select the user store domain when multiple user stores are configured.
        if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
            userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId(authorizedUser);
        }

        synchronized ((consumerKey + ":" + authorizedUser + ":" + scope).intern()) {

            // check if valid access token exists in cache
            if (cacheEnabled) {
                AccessTokenDO accessTokenDO = (AccessTokenDO) oauthCache.getValueFromCache(cacheKey);
                if (accessTokenDO != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Retrieved active Access Token : " + accessTokenDO.getAccessToken() +
                                " for Client Id : " + consumerKey + ", User ID :" + authorizedUser +
                                " and Scope : " + scope + " from cache");
                    }
                    long expireTime = OAuth2Util.getTokenExpireTimeMillis(accessTokenDO);
                    if (expireTime > 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token " + accessTokenDO.getAccessToken() + " is still valid");
                        }
                        respDTO.setAccessToken(accessTokenDO.getAccessToken());
                        respDTO.setValidityPeriod(accessTokenDO.getValidityPeriod());
                        respDTO.setScope(oauthAuthzMsgCtx.getApprovedScope());
                        return respDTO;
                    } else {
                        //Token is expired. Clear it from cache and mark it as expired on database
                        oauthCache.clearCacheEntry(cacheKey);
                        tokenMgtDAO.setAccessTokenState(accessTokenDO.getAccessToken(),
                                OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED,
                                UUID.randomUUID().toString(), userStoreDomain);
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token " + accessTokenDO.getAccessToken() +
                                    " is expired. Therefore cleared it from cache and marked it" +
                                    " as expired in database");
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No active access token found in cache for Client ID : " + consumerKey +
                                ", User ID : " + authorizedUser + " and Scope : " + scope);
                    }
                }
            }

            // check if the last issued access token is still active and valid in the database
            AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveLatestAccessToken(
                    consumerKey, authorizedUser, userStoreDomain, scope, false);

            if (accessTokenDO != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Retrieved latest Access Token : " + accessTokenDO.getAccessToken() +
                            " for Client ID : " + consumerKey + ", User ID :" + authorizedUser +
                            " and Scope : " + scope + " from database");
                }
                if (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(accessTokenDO.getTokenState()) &&
                        OAuth2Util.getTokenExpireTimeMillis(accessTokenDO) > 0) {
                    // token is active and valid
                    if (log.isDebugEnabled()) {
                        log.debug("Access token : " + accessTokenDO.getAccessToken() + " is still valid");
                    }
                    if (cacheEnabled) {
                        oauthCache.addToCache(cacheKey, accessTokenDO);
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token : " + accessTokenDO.getAccessToken() +
                                    " was added to cache for cache key : " + cacheKey.getCacheKeyString());
                        }
                    }
                    respDTO.setAccessToken(accessTokenDO.getAccessToken());
                    respDTO.setValidityPeriod(OAuth2Util.getTokenExpireTimeMillis(accessTokenDO) / 1000);
                    respDTO.setScope(oauthAuthzMsgCtx.getApprovedScope());
                    return respDTO;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Access token + " + accessTokenDO.getAccessToken() + " is not valid anymore");
                    }
                    String tokenState = accessTokenDO.getTokenState();
                    if (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(tokenState)) {
                        // Token is expired. Mark it as expired on database
                        tokenMgtDAO.setAccessTokenState(accessTokenDO.getAccessToken(),
                                OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED,
                                UUID.randomUUID().toString(), userStoreDomain);
                        if (log.isDebugEnabled()) {
                            log.debug("Marked Access Token " + accessTokenDO.getAccessToken() + " as expired");
                        }
                    } else {
                        //Token is revoked or inactive
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token " + accessTokenDO.getAccessToken() + " is " + accessTokenDO.getTokenState());
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No access token found in database for Client ID : " + consumerKey +
                            ", User ID : " + authorizedUser + " and Scope : " + scope +
                            ". Therefore issuing new access token");
                }
            }

            // issue a new access token
            String accessToken;
            String refreshToken;
            try {
                accessToken = oauthIssuerImpl.accessToken();
                refreshToken = oauthIssuerImpl.refreshToken();
            } catch (OAuthSystemException e) {
                throw new IdentityOAuth2Exception(
                        "Error occurred while generating access token and refresh token", e);
            }
            accessTokenDO = tokenMgtDAO.retrieveLatestAccessToken(
                    consumerKey, authorizedUser, userStoreDomain, scope, true);
            if (accessTokenDO != null) {
                RefreshTokenValidationDataDO refreshTokenValidationDataDO =
                        tokenMgtDAO.validateRefreshToken(consumerKey, accessTokenDO.getRefreshToken());
                String state = refreshTokenValidationDataDO.getRefreshTokenState();
                long createdTime = refreshTokenValidationDataDO.getIssuedAt();
                long refreshValidity = OAuthServerConfiguration.getInstance().
                        getRefreshTokenValidityPeriodInSeconds() * 1000;
                long currentTime = System.currentTimeMillis();
                long skew = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;
                if (OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED.equals(state) &&
                        createdTime + refreshValidity - (currentTime + skew) > 1000) {
                    refreshToken = accessTokenDO.getRefreshToken();
                }
            }

            if (OAuth2Util.checkUserNameAssertionEnabled()) {
                String userName = oauthAuthzMsgCtx.getAuthorizationReqDTO().getUsername();
                //use ':' for token & userStoreDomain separation
                String accessTokenStrToEncode = accessToken + ":" + userName;
                accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

                String refreshTokenStrToEncode = refreshToken + ":" + userName;
                refreshToken = Base64Utils.encode(refreshTokenStrToEncode.getBytes());
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

            accessTokenDO = new AccessTokenDO(consumerKey, authorizationReqDTO.getUsername(),
                    oauthAuthzMsgCtx.getApprovedScope(), timestamp, validityPeriod,
                    OAuthConstants.USER_TYPE_FOR_USER_TOKEN);
            accessTokenDO.setAccessToken(accessToken);
            accessTokenDO.setRefreshToken(refreshToken);
            accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);

            // Persist the access token in database
            tokenMgtDAO.storeAccessToken(accessToken, authorizationReqDTO.getConsumerKey(),
                    accessTokenDO, userStoreDomain);
            if (log.isDebugEnabled()) {
                log.debug("Persisted Access Token : " + accessToken + " for " +
                        "Client ID : " + authorizationReqDTO.getConsumerKey() +
                        ", Authorized User : " + authorizationReqDTO.getUsername() +
                        ", Timestamp : " + timestamp +
                        ", Validity period : " + validityPeriod +
                        ", Scope : " + OAuth2Util.buildScopeString(oauthAuthzMsgCtx.getApprovedScope()) +
                        ", Callback URL : " + authorizationReqDTO.getCallbackUrl() +
                        ", Token State : " + OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE +
                        " and User Type : " + OAuthConstants.USER_TYPE_FOR_USER_TOKEN);
            }

            // Add the access token to the cache.
            if (cacheEnabled) {
                oauthCache.addToCache(cacheKey, accessTokenDO);
                if (log.isDebugEnabled()) {
                    log.debug("Access Token : " + accessToken + " was added to OAuthCache for " +
                            "cache key : " + cacheKey.getCacheKeyString());
                }
            }

            respDTO.setAccessToken(accessToken);
            respDTO.setValidityPeriod(validityPeriod);
            respDTO.setScope(accessTokenDO.getScope());
            return respDTO;
        }
    }
}
