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

package org.wso2.carbon.identity.oauth2.token.handlers.grant;

import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.ResponseHeader;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Grant Type handler for Grant Type refresh_token which is used to get a new access token.
 */
public class RefreshGrantHandler extends AbstractAuthorizationGrantHandler {

    private static final String PREV_ACCESS_TOKEN = "previousAccessToken";
    private static Log log = LogFactory.getLog(RefreshGrantHandler.class);

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        super.validateGrant(tokReqMsgCtx);

        OAuth2AccessTokenReqDTO tokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();

        String refreshToken = tokenReqDTO.getRefreshToken();

        RefreshTokenValidationDataDO validationDataDO = tokenMgtDAO.validateRefreshToken(
                tokenReqDTO.getClientId(), refreshToken);

        if (validationDataDO.getAccessToken() == null) {
            log.debug("Invalid Refresh Token provided for Client with " +
                    "Client Id : " + tokenReqDTO.getClientId());
            return false;
        }

        if (validationDataDO.getRefreshTokenState() != null &&
                !OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(
                        validationDataDO.getRefreshTokenState()) &&
                !OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED.equals(
                        validationDataDO.getRefreshTokenState())) {
            log.debug("Refresh Token is not in 'ACTIVE' or 'EXPIRED' state for Client with " +
                    "Client Id : " + tokenReqDTO.getClientId() + " " +
                    "Refresh Token: " + tokenReqDTO.getRefreshToken());
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Refresh token validation successful for " +
                    "Client id : " + tokenReqDTO.getClientId() +
                    ", Authorized User : " + validationDataDO.getAuthorizedUser() +
                    ", Token Scope : " + OAuth2Util.buildScopeString(validationDataDO.getScope()));
        }

        tokReqMsgCtx.setAuthorizedUser(validationDataDO.getAuthorizedUser());
        tokReqMsgCtx.setScope(validationDataDO.getScope());
        // Store the old access token as a OAuthTokenReqMessageContext property, this is already
        // a preprocessed token.
        tokReqMsgCtx.addProperty(PREV_ACCESS_TOKEN, validationDataDO);
        return true;
    }

    @Override
    public OAuth2AccessTokenRespDTO issue(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenRespDTO tokenRespDTO = new OAuth2AccessTokenRespDTO();
        OAuth2AccessTokenReqDTO oauth2AccessTokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();
        String scope = OAuth2Util.buildScopeString(tokReqMsgCtx.getScope());

        String accessToken;
        String refreshToken;
        String userStoreDomain = null;

        try {
            accessToken = oauthIssuerImpl.accessToken();
            refreshToken = oauthIssuerImpl.refreshToken();
            boolean renew = OAuthServerConfiguration.getInstance().isRefreshTokenRenewalEnabled();
            if (!renew) {
                RefreshTokenValidationDataDO refreshTokenValidationDataDO =
                        tokenMgtDAO.validateRefreshToken(oauth2AccessTokenReqDTO.getClientId(),
                                oauth2AccessTokenReqDTO.getRefreshToken());
                if (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(
                        refreshTokenValidationDataDO.getRefreshTokenState())) {
                    long issuedAt = refreshTokenValidationDataDO.getIssuedAt();
                    long refreshValidity =
                            OAuthServerConfiguration.getInstance().getRefreshTokenValidityPeriodInSeconds() * 1000;
                    long skew = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;
                    if (issuedAt + refreshValidity - (System.currentTimeMillis() + skew) > 1000) {
                        refreshToken = oauth2AccessTokenReqDTO.getRefreshToken();
                    }
                }
            }
        } catch (OAuthSystemException e) {
            throw new IdentityOAuth2Exception("Error when generating the tokens.", e);
        }

        if (OAuth2Util.checkUserNameAssertionEnabled()) {
            String userName = tokReqMsgCtx.getAuthorizedUser();
            //use ':' for token & userStoreDomain separation
            String accessTokenStrToEncode = accessToken + ":" + userName;
            accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

            String refreshTokenStrToEncode = refreshToken + ":" + userName;
            refreshToken = Base64Utils.encode(refreshTokenStrToEncode.getBytes());

            //logic to store access token into different tables when multiple user stores are configured.
            if (OAuth2Util.checkAccessTokenPartitioningEnabled()) {
                userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId(userName);
            }
        }

        Timestamp timestamp = new Timestamp(new Date().getTime());

        // Default Validity Period (in seconds)
        long validityPeriod = OAuthServerConfiguration.getInstance()
                .getUserAccessTokenValidityPeriodInSeconds();

        // if a VALID validity period is set through the callback, then use it
        long callbackValidityPeriod = tokReqMsgCtx.getValidityPeriod();
        if ((callbackValidityPeriod != OAuthConstants.UNASSIGNED_VALIDITY_PERIOD)
                && callbackValidityPeriod > 0) {
            validityPeriod = callbackValidityPeriod;
        }

        String tokenType;
        if(isOfTypeApplicationUser()) {
            tokenType = OAuthConstants.USER_TYPE_FOR_USER_TOKEN;
        } else {
            tokenType = OAuthConstants.USER_TYPE_FOR_APPLICATION_TOKEN;
        }

        String clientId = oauth2AccessTokenReqDTO.getClientId();
        AccessTokenDO accessTokenDO = new AccessTokenDO(clientId, tokReqMsgCtx.getAuthorizedUser(),
                tokReqMsgCtx.getScope(), timestamp, validityPeriod, tokenType);
        accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
        accessTokenDO.setRefreshToken(refreshToken);
        accessTokenDO.setAccessToken(accessToken);

        RefreshTokenValidationDataDO oldAccessToken =
                (RefreshTokenValidationDataDO)tokReqMsgCtx.getProperty(PREV_ACCESS_TOKEN);

        String authorizedUser = tokReqMsgCtx.getAuthorizedUser();
	    // set the previous access token state to "INACTIVE" and store new access token in single db connection
	    tokenMgtDAO.invalidateAndCreateNewToken(oldAccessToken.getAccessToken(), "INACTIVE", clientId,
	                                            UUID.randomUUID().toString(), accessTokenDO,
	                                            userStoreDomain);

        //remove the previous access token from cache and add the new access token info to the cache,
        // if it's enabled.
        if (cacheEnabled) {
            // Remove the old access token from the OAuthCache
            CacheKey oauthCacheKey = new OAuthCacheKey(clientId + ":" + authorizedUser + ":" + scope);
            oauthCache.clearCacheEntry(oauthCacheKey);

            // Remove the old access token from the AccessTokenCache
            CacheKey accessTokenCacheKey = new OAuthCacheKey(oldAccessToken.getAccessToken());
            oauthCache.clearCacheEntry(accessTokenCacheKey);

            // Add new access token to the OAuthCache
            oauthCache.addToCache(oauthCacheKey, accessTokenDO);

            // Add new access token to the AccessTokenCache
            accessTokenCacheKey = new OAuthCacheKey(accessToken);
            oauthCache.addToCache(accessTokenCacheKey, accessTokenDO);

            if (log.isDebugEnabled()) {
                log.debug("Access Token info for the refresh token was added to the cache for " +
                        "the client id : " + clientId + ". Old access token entry was " +
                        "also removed from the cache.");
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Persisted an access token for the refresh token, " +
                    "Client ID : " + clientId +
                    "authorized user : " + tokReqMsgCtx.getAuthorizedUser() +
                    "timestamp : " + timestamp +
                    "validity period : " + validityPeriod +
                    "scope : " + OAuth2Util.buildScopeString(tokReqMsgCtx.getScope()) +
                    "Token State : " + OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE +
                    "User Type : " + tokenType);
        }

        tokenRespDTO.setAccessToken(accessToken);
        tokenRespDTO.setRefreshToken(refreshToken);
        if(validityPeriod > 0){
            tokenRespDTO.setExpiresIn(accessTokenDO.getValidityPeriod());
            tokenRespDTO.setExpiresInMillis(accessTokenDO.getValidityPeriodInMillis());
        } else {
            tokenRespDTO.setExpiresIn(Long.MAX_VALUE/1000);
            tokenRespDTO.setExpiresInMillis(Long.MAX_VALUE);
        }
        tokenRespDTO.setAuthorizedScopes(scope);

        ArrayList<ResponseHeader> respHeaders = new ArrayList<ResponseHeader>();
        ResponseHeader header = new ResponseHeader();
        header.setKey("DeactivatedAccessToken");
        header.setValue(oldAccessToken.getAccessToken());
        respHeaders.add(header);

        tokReqMsgCtx.addProperty("RESPONSE_HEADERS", respHeaders.toArray(
                new ResponseHeader[respHeaders.size()]));

        return tokenRespDTO;
    }
}
