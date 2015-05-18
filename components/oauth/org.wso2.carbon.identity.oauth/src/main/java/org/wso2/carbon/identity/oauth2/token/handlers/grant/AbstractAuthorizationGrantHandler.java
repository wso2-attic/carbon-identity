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

import org.apache.amber.oauth2.as.issuer.MD5Generator;
import org.apache.amber.oauth2.as.issuer.OAuthIssuer;
import org.apache.amber.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.oauth.cache.AppInfoCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.callback.OAuthCallback;
import org.wso2.carbon.identity.oauth.callback.OAuthCallbackManager;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public abstract class AbstractAuthorizationGrantHandler implements AuthorizationGrantHandler {

    private static Log log = LogFactory.getLog(AbstractAuthorizationGrantHandler.class);
    protected final OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
    protected TokenMgtDAO tokenMgtDAO;
    protected OAuthCallbackManager callbackManager;
    protected boolean cacheEnabled;
    protected OAuthCache oauthCache;

    public void init() throws IdentityOAuth2Exception {
        tokenMgtDAO = new TokenMgtDAO();
        callbackManager = new OAuthCallbackManager();
        // Set the cache instance if caching is enabled.
        if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
            cacheEnabled = true;
            oauthCache = OAuthCache.getInstance();
        }
    }

    public boolean isConfidentialClient() throws IdentityOAuth2Exception {
        return true;
    }

    public boolean issueRefreshToken() throws IdentityOAuth2Exception {
        return true;
    }

    public boolean isOfTypeApplicationUser() throws IdentityOAuth2Exception {
        return true;
    }

    public OAuth2AccessTokenRespDTO issue(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {

        OAuth2AccessTokenRespDTO tokenRespDTO;
        OAuth2AccessTokenReqDTO oAuth2AccessTokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();
        String scope = OAuth2Util.buildScopeString(tokReqMsgCtx.getScope());

        String consumerKey = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId();
        String authorizedUser = tokReqMsgCtx.getAuthorizedUser();
        OAuthCacheKey cacheKey = new OAuthCacheKey(consumerKey + ":" + authorizedUser.toLowerCase() + ":" + scope);
        String userStoreDomain = null;

        //select the user store domain when multiple user stores are configured.
        if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
            userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId
                    (tokReqMsgCtx.getAuthorizedUser());
        }

        String tokenType;
        if (isOfTypeApplicationUser()) {
            tokenType = OAuthConstants.USER_TYPE_FOR_USER_TOKEN;
        } else {
            tokenType = OAuthConstants.USER_TYPE_FOR_APPLICATION_TOKEN;
        }

        String refreshToken = null;
        Timestamp refreshTokenIssuedTime = null;
        long refreshTokenValidityPeriodInMillis = 0;

        synchronized ((consumerKey + ":" + authorizedUser + ":" + scope).intern()) {
            // check if valid access token exists in cache
            if (cacheEnabled) {

                AccessTokenDO accessTokenDO = (AccessTokenDO) oauthCache.getValueFromCache(cacheKey);

                if (accessTokenDO != null) {

                    if (log.isDebugEnabled()) {
                        log.debug("Retrieved active access token : " + accessTokenDO.getAccessToken() +
                                " for client Id " + consumerKey + ", user " + authorizedUser +
                                " and scope " + scope + " from cache");
                    }

                    long expireTime = OAuth2Util.getTokenExpireTimeMillis(accessTokenDO);

                    if (expireTime > 0 || expireTime < 0) {
                        if (log.isDebugEnabled()) {
                            if(expireTime > 0) {
                                log.debug("Access Token " + accessTokenDO.getAccessToken() + " is still valid");
                            } else {
                                log.debug("Infinite lifetime Access Token " + accessTokenDO.getAccessToken() +
                                        " found in cache");
                            }
                        }
                        tokenRespDTO = new OAuth2AccessTokenRespDTO();
                        tokenRespDTO.setAccessToken(accessTokenDO.getAccessToken());
                        if (issueRefreshToken() &&
                                OAuthServerConfiguration.getInstance().getSupportedGrantTypes().containsKey(
                                        GrantType.REFRESH_TOKEN.toString())) {
                            tokenRespDTO.setRefreshToken(accessTokenDO.getRefreshToken());
                        }
                        if(expireTime > 0){
                            tokenRespDTO.setExpiresIn(expireTime/1000);
                            tokenRespDTO.setExpiresInMillis(expireTime);
                        } else {
                            tokenRespDTO.setExpiresIn(Long.MAX_VALUE/1000);
                            tokenRespDTO.setExpiresInMillis(Long.MAX_VALUE);
                        }
                        return tokenRespDTO;
                    } else {

                        long refreshTokenExpiryTime = OAuth2Util.getRefreshTokenExpireTimeMillis(accessTokenDO);

                        if (refreshTokenExpiryTime < 0 || refreshTokenExpiryTime > 0) {
                            log.debug("Access token has expired, But refresh token is still valid. User existing " +
                                      "refresh token.");
                            refreshToken = accessTokenDO.getRefreshToken();
                            refreshTokenIssuedTime = accessTokenDO.getRefreshTokenIssuedTime();
                            refreshTokenValidityPeriodInMillis = accessTokenDO.getRefreshTokenValidityPeriodInMillis();
                        }
                        //Token is expired. Clear it from cache and mark it as expired on database
                        oauthCache.clearCacheEntry(cacheKey);
                        tokenMgtDAO.setAccessTokenState(accessTokenDO.getAccessToken(),
                                OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED,
                                UUID.randomUUID().toString(), userStoreDomain);
                        if (log.isDebugEnabled()) {
                            log.debug("Access token " + accessTokenDO.getAccessToken() +
                                    " is expired. Therefore cleared it from cache and marked it" +
                                    " as expired in database");
                        }
                    }
                }
            }

            //Check if the last issued access token is still active and valid in database
            AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveLatestAccessToken(
                    oAuth2AccessTokenReqDTO.getClientId(), tokReqMsgCtx.getAuthorizedUser(),
                    userStoreDomain, scope, false);

            if (accessTokenDO != null) {

                if (log.isDebugEnabled()) {
                    log.debug("Retrieved latest access token : " + accessTokenDO.getAccessToken() +
                            " for client Id " + consumerKey + ", user " + authorizedUser +
                            " and scope " + scope + " from database");
                }

                long expireTime = OAuth2Util.getTokenExpireTimeMillis(accessTokenDO);

                long refreshTokenExpiryTime = OAuth2Util.getRefreshTokenExpireTimeMillis(accessTokenDO);

                if(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(
                                accessTokenDO.getTokenState()) && (expireTime > 0 || expireTime < 0)){
                    // token is active and valid
                    if (log.isDebugEnabled()) {
                        if(expireTime > 0){
                            log.debug("Access token " + accessTokenDO.getAccessToken() +
                                            " is valid for another " + expireTime + "ms");
                        } else {
                            log.debug("Infinite lifetime Access Token " + accessTokenDO.getAccessToken() +
                                            " found in cache");
                        }
                    }
                    tokenRespDTO = new OAuth2AccessTokenRespDTO();
                    tokenRespDTO.setAccessToken(accessTokenDO.getAccessToken());
                    if (issueRefreshToken() &&
                            OAuthServerConfiguration.getInstance().getSupportedGrantTypes().containsKey(
                                    GrantType.REFRESH_TOKEN.toString())) {
                        tokenRespDTO.setRefreshToken(accessTokenDO.getRefreshToken());
                    }
                    if(expireTime > 0) {
                        tokenRespDTO.setExpiresIn(expireTime / 1000);
                        tokenRespDTO.setExpiresInMillis(expireTime);
                    } else {
                        tokenRespDTO.setExpiresIn(Long.MAX_VALUE/1000);
                        tokenRespDTO.setExpiresInMillis(Long.MAX_VALUE);
                    }
                    if (cacheEnabled) {
                        oauthCache.addToCache(cacheKey, accessTokenDO);
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token info was added to the cache for the cache key : " +
                                    cacheKey.getCacheKeyString());
                        }
                    }

                    return tokenRespDTO;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Access token + " + accessTokenDO.getAccessToken() + " is not valid anymore");
                    }
                    String tokenState = accessTokenDO.getTokenState();
                    if (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(tokenState)) {

                        // Token is expired. If refresh token is still valid, use it.
                        if (refreshTokenExpiryTime > 0 || refreshTokenExpiryTime < 0) {
                            log.debug("Access token has expired, But refresh token is still valid. User existing " +
                                      "refresh token.");
                            refreshToken = accessTokenDO.getRefreshToken();
                            refreshTokenIssuedTime = accessTokenDO.getRefreshTokenIssuedTime();
                            refreshTokenValidityPeriodInMillis = accessTokenDO.getRefreshTokenValidityPeriodInMillis();
                        }
                        //  Mark token as expired on database
                        tokenMgtDAO.setAccessTokenState(accessTokenDO.getAccessToken(),
                                OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED,
                                UUID.randomUUID().toString(), userStoreDomain);
                        if (log.isDebugEnabled()) {
                            log.debug("Marked token " + accessTokenDO.getAccessToken() + " as expired");
                        }
                    } else {
                        //Token is revoked or inactive
                        if (log.isDebugEnabled()) {
                            log.debug("Token " + accessTokenDO.getAccessToken() + " is " + accessTokenDO.getTokenState());
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No access token found in database for client Id " + consumerKey +
                            ", user " + authorizedUser + " and scope " + scope +
                            ". Therefore issuing new token");
                }
            }

            // issue a new access token.
            if (log.isDebugEnabled()) {
                log.debug("Issuing a new access token for "
                        + consumerKey + " AuthorizedUser : " + authorizedUser);
            }

            String accessToken;

            try {
                accessToken = oauthIssuerImpl.accessToken();

                // regenerate only if refresh token is null
                if (refreshToken == null) {
                    refreshToken = oauthIssuerImpl.refreshToken();
                }

            } catch (OAuthSystemException e) {
                throw new IdentityOAuth2Exception(
                        "Error occurred while generating access token and refresh token", e);
            }

            if (OAuth2Util.checkUserNameAssertionEnabled()) {
                String userName = tokReqMsgCtx.getAuthorizedUser();
                //use ':' for token & userStoreDomain separation
                String accessTokenStrToEncode = accessToken + ":" + userName;
                accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

                String refreshTokenStrToEncode = refreshToken + ":" + userName;
                refreshToken = Base64Utils.encode(refreshTokenStrToEncode.getBytes());
            }

            Timestamp timestamp = new Timestamp(new Date().getTime());

            // if reusing existing refresh token, use its original issued time
            if (refreshTokenIssuedTime == null) {
                refreshTokenIssuedTime = timestamp;
            }

            // Default Validity Period (in seconds)
            long validityPeriodInMillis = OAuthServerConfiguration.getInstance().
                    getApplicationAccessTokenValidityPeriodInSeconds() * 1000;

            if(isOfTypeApplicationUser()){
                validityPeriodInMillis = OAuthServerConfiguration.getInstance().
                        getUserAccessTokenValidityPeriodInSeconds() * 1000;
            }

            // if a VALID validity period is set through the callback, then use it
            long callbackValidityPeriod = tokReqMsgCtx.getValidityPeriod();
            if ((callbackValidityPeriod != OAuthConstants.UNASSIGNED_VALIDITY_PERIOD)
                    && callbackValidityPeriod > 0) {
                validityPeriodInMillis = callbackValidityPeriod * 1000;
            }

            // If issuing new refresh token, use default refresh token validity Period
            // otherwise use existing refresh token's validity period
            if (refreshTokenValidityPeriodInMillis == 0) {
                refreshTokenValidityPeriodInMillis = OAuthServerConfiguration.getInstance()
                                                             .getRefreshTokenValidityPeriodInSeconds() * 1000;
            }

            accessTokenDO = new AccessTokenDO(consumerKey, tokReqMsgCtx.getAuthorizedUser(),
                                              tokReqMsgCtx.getScope(), timestamp, refreshTokenIssuedTime,
                                              validityPeriodInMillis, refreshTokenValidityPeriodInMillis, tokenType);

            accessTokenDO.setAccessToken(accessToken);
            accessTokenDO.setRefreshToken(refreshToken);
            accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
            accessTokenDO.setTenantID(tokReqMsgCtx.getTenantID());

            // Persist the access token in database
            try {
                tokenMgtDAO.storeAccessToken(accessToken, oAuth2AccessTokenReqDTO.getClientId(),
                                accessTokenDO, userStoreDomain);
            } catch (IdentityException e) {
                throw new IdentityOAuth2Exception(
                                "Error occurred while storing new access token : " + accessToken, e);
            }

            if (log.isDebugEnabled()) {
                log.debug("Persisted Access Token : " + accessToken + " for " +
                          "Client ID : " + oAuth2AccessTokenReqDTO.getClientId() +
                          ", Authorized User : " + tokReqMsgCtx.getAuthorizedUser() +
                          ", Timestamp : " + timestamp +
                          ", Validity period (s) : " + accessTokenDO.getValidityPeriod() +
                          ", Scope : " + OAuth2Util.buildScopeString(tokReqMsgCtx.getScope()) +
                          " and Token State : " + OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
            }

            //update cache with newly added token
            if (cacheEnabled) {
                oauthCache.addToCache(cacheKey, accessTokenDO);
                if (log.isDebugEnabled()) {
                    log.debug("Access token was added to OAuthCache for cache key : " +
                            cacheKey.getCacheKeyString());
                }
            }

            tokenRespDTO = new OAuth2AccessTokenRespDTO();
            tokenRespDTO.setAccessToken(accessToken);
            if (issueRefreshToken() &&
                    OAuthServerConfiguration.getInstance().getSupportedGrantTypes().containsKey(
                            GrantType.REFRESH_TOKEN.toString())) {
                tokenRespDTO.setRefreshToken(refreshToken);
            }
            if (validityPeriodInMillis > 0) {
                tokenRespDTO.setExpiresInMillis(accessTokenDO.getValidityPeriodInMillis());
                tokenRespDTO.setExpiresIn(accessTokenDO.getValidityPeriod());
            } else {
                tokenRespDTO.setExpiresInMillis(Long.MAX_VALUE);
                tokenRespDTO.setExpiresIn(Long.MAX_VALUE/1000);
            }
            tokenRespDTO.setAuthorizedScopes(scope);
            return tokenRespDTO;
        }
    }

    public boolean authorizeAccessDelegation(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {
        OAuthCallback authzCallback = new OAuthCallback(
                tokReqMsgCtx.getAuthorizedUser(),
                tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId(),
                OAuthCallback.OAuthCallbackType.ACCESS_DELEGATION_TOKEN);
        authzCallback.setRequestedScope(tokReqMsgCtx.getScope());
        if (tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().equals(
                org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString())) {
            authzCallback.setCarbonGrantType(org.wso2.carbon.identity.oauth.common.GrantType.valueOf(
                    OAuthConstants.OAUTH_SAML2_BEARER_GRANT_ENUM.toString()));
        } else if (tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().equals(
                org.wso2.carbon.identity.oauth.common.GrantType.IWA_NTLM.toString())) {
            authzCallback.setCarbonGrantType(org.wso2.carbon.identity.oauth.common.GrantType.valueOf(
                    OAuthConstants.OAUTH_IWA_NTLM_GRANT_ENUM.toString()));
        } else {
            authzCallback.setGrantType(GrantType.valueOf(
                    tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().toUpperCase()));
        }
        callbackManager.handleCallback(authzCallback);
        tokReqMsgCtx.setValidityPeriod(authzCallback.getValidityPeriod());
        return authzCallback.isAuthorized();
    }

    public boolean validateScope(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {
        OAuthCallback scopeValidationCallback = new OAuthCallback(
                tokReqMsgCtx.getAuthorizedUser(),
                tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId(),
                OAuthCallback.OAuthCallbackType.SCOPE_VALIDATION_TOKEN);
        scopeValidationCallback.setRequestedScope(tokReqMsgCtx.getScope());
        if (tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().equals(
                org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString())) {
            scopeValidationCallback.setCarbonGrantType(org.wso2.carbon.identity.oauth.common.GrantType.valueOf(
                    OAuthConstants.OAUTH_SAML2_BEARER_GRANT_ENUM.toString()));
        } else if (tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().equals(
                org.wso2.carbon.identity.oauth.common.GrantType.IWA_NTLM.toString())) {
            scopeValidationCallback.setCarbonGrantType(org.wso2.carbon.identity.oauth.common.GrantType.valueOf(
                    OAuthConstants.OAUTH_IWA_NTLM_GRANT_ENUM.toString()));
        } else {
            scopeValidationCallback.setGrantType(GrantType.valueOf(
                    tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().toUpperCase()));
        }

        callbackManager.handleCallback(scopeValidationCallback);
        tokReqMsgCtx.setValidityPeriod(scopeValidationCallback.getValidityPeriod());
        tokReqMsgCtx.setScope(scopeValidationCallback.getApprovedScope());
        return scopeValidationCallback.isValidScope();
    }

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenReqDTO tokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();
        String grantType = tokenReqDTO.getGrantType();

        // Load application data from the cache
        AppInfoCache appInfoCache = AppInfoCache.getInstance();
        OAuthAppDO oAuthAppDO = appInfoCache.getValueFromCache(tokenReqDTO.getClientId());
        if (oAuthAppDO == null) {
            try {
                oAuthAppDO = new OAuthAppDAO().getAppInformation(tokenReqDTO.getClientId());
                appInfoCache.addToCache(tokenReqDTO.getClientId(), oAuthAppDO);
            } catch (InvalidOAuthClientException e) {
                log.error("Error while reading application data for client id : " + tokenReqDTO.getClientId(), e);
                return false;
            }
        }
        // If the application has defined a limited set of grant types, then check the grant
        if (oAuthAppDO.getGrantTypes() != null && !oAuthAppDO.getGrantTypes().contains(grantType)) {
            if (log.isDebugEnabled()) {
                //Do not change this log format as these logs use by external applications
                log.debug("Unsupported Grant Type : " + grantType + " for client id : " + tokenReqDTO.getClientId());
            }
            return false;
        }
        return true;
    }
}
