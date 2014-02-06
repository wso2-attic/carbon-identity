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
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.callback.OAuthCallback;
import org.wso2.carbon.identity.oauth.callback.OAuthCallbackManager;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public abstract class AbstractAuthorizationGrantHandler implements AuthorizationGrantHandler {

    private static Log log = LogFactory.getLog(AbstractAuthorizationGrantHandler.class);

    protected TokenMgtDAO tokenMgtDAO;
    protected final OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
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

    public OAuth2AccessTokenRespDTO issue(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {

        OAuth2AccessTokenRespDTO tokenRespDTO;
        OAuth2AccessTokenReqDTO oAuth2AccessTokenReqDTO = tokReqMsgCtx.getOauth2AccessTokenReqDTO();

        String consumerKey = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId();
        String authorizedUser = tokReqMsgCtx.getAuthorizedUser();
        CacheKey cacheKey = new OAuthCacheKey(consumerKey + ":" + authorizedUser.toLowerCase());
        String userStoreDomain = null;

        //select the user store domain when multiple user stores are configured.
        if (OAuth2Util.checkAccessTokenPartitioningEnabled() &&
                OAuth2Util.checkUserNameAssertionEnabled()) {
            userStoreDomain = OAuth2Util.getUserStoreDomainFromUserId
                    (tokReqMsgCtx.getAuthorizedUser());
        }

        String tokenType;
        if(isOfTypeApplicationUser()){
            tokenType = OAuthConstants.USER_TYPE_FOR_USER_TOKEN;
        } else {
            tokenType = OAuthConstants.USER_TYPE_FOR_APPLICATION_TOKEN;
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
                            tokenRespDTO = new OAuth2AccessTokenRespDTO();
                            tokenRespDTO.setAccessToken(accessTokenDO.getAccessToken());
                            if (issueRefreshToken() &&
                                    OAuthServerConfiguration.getInstance().getSupportedGrantTypes().containsKey(
                                            GrantType.REFRESH_TOKEN.toString())) {
                                tokenRespDTO.setRefreshToken(accessTokenDO.getRefreshToken());
                            }
                            tokenRespDTO.setExpiresIn(accessTokenDO.getValidityPeriod());
                            if (log.isDebugEnabled()) {
                                log.debug("Access Token info retrieved from the cache and served to client with client id : " +
                                        oAuth2AccessTokenReqDTO.getClientId());
                            }
                            oauthCache.addToCache(cacheKey, accessTokenDO);
                            return tokenRespDTO;
                        } else {
                            oauthCache.clearCacheEntry(cacheKey);
                            //Token is expired. Mark it as expired on database
                            //TODO : Read token state from a constant
                            String tokenState = tokenMgtDAO.getAccessTokenState(consumerKey, authorizedUser);
                            if (tokenState.equals("REVOKED")) {
                                tokenMgtDAO.setAccessTokenState(consumerKey, authorizedUser,"REVOKED", UUID.randomUUID().toString(),userStoreDomain);
                            } else {//Token is expired. Mark it as expired on database
                                tokenMgtDAO.setAccessTokenState(consumerKey, authorizedUser, "EXPIRED",
                                        UUID.randomUUID().toString(), userStoreDomain);
                            }
                        }
                    }
                }

                //Check if previously issued token exists in database
                tokenRespDTO = tokenMgtDAO.getValidAccessTokenIfExist(oAuth2AccessTokenReqDTO.getClientId(),
                        tokReqMsgCtx.getAuthorizedUser(), userStoreDomain);
                if (tokenRespDTO != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Retrieving existing valid access token for client ID" + oAuth2AccessTokenReqDTO.getClientId());
                    }
                    if (cacheEnabled) {
                        AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, tokReqMsgCtx.getAuthorizedUser(),
                                tokReqMsgCtx.getScope(), new Timestamp(System.currentTimeMillis()), tokenRespDTO.getExpiresIn(), tokenType);
                        accessTokenDO.setRefreshToken(tokenRespDTO.getRefreshToken());
                        accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
                        accessTokenDO.setAccessToken(tokenRespDTO.getAccessToken());
                        if (log.isDebugEnabled()) {
                            log.debug("Access Token info was added to the cache for the client id : " +
                                    oAuth2AccessTokenReqDTO.getClientId());
                        }
                        oauthCache.addToCache(cacheKey, accessTokenDO);
                    }
                    if(!(issueRefreshToken() &&
                            OAuthServerConfiguration.getInstance().getSupportedGrantTypes().containsKey(
                                    GrantType.REFRESH_TOKEN.toString()))){
                        tokenRespDTO.setRefreshToken(null);
                    }
                    return tokenRespDTO;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Marking old token as expired for client Id : "
                                + consumerKey + " AuthorizedUser : " + authorizedUser);
                    }
                    //TODO : Read token state from a constant
                    //TODO : This should move to validation check of getValidAccessTokenIfExist() method
                    String tokenState = tokenMgtDAO.getAccessTokenState(consumerKey, authorizedUser);
                    if (tokenState != null){
                        if(tokenState.equals("REVOKED")) {
                            tokenMgtDAO.setAccessTokenState(consumerKey, authorizedUser, "REVOKED", UUID.randomUUID().toString(), userStoreDomain);
                        } else { // Token is expired. Mark it as expired on database
                            tokenMgtDAO.setAccessTokenState(consumerKey, authorizedUser, "EXPIRED",
                                    UUID.randomUUID().toString(), userStoreDomain);
                        }
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error while getting existing token for client ID" + oAuth2AccessTokenReqDTO.getClientId());
                }
            }

            //No valid access token is found in cache or database.
            //Need to issue a new access token.
            if (log.isDebugEnabled()) {
                log.debug("Issuing a new access token for "
                        + consumerKey + " AuthorizedUser : " + authorizedUser);
            }
            String accessToken;
            String refreshToken;

            try {
                accessToken = oauthIssuerImpl.accessToken();
                refreshToken = oauthIssuerImpl.refreshToken();
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

            // validityPeriod = validityPeriod * 1000;
            // Get the secured versions of the tokens to persist and to cache.

            AccessTokenDO accessTokenDO = new AccessTokenDO(consumerKey, tokReqMsgCtx.getAuthorizedUser(),
                    tokReqMsgCtx.getScope(), timestamp, validityPeriod, tokenType);
            accessTokenDO.setRefreshToken(refreshToken);
            accessTokenDO.setTokenState(OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
            accessTokenDO.setAccessToken(accessToken);
            accessTokenDO.setTenantID(tokReqMsgCtx.getTenantID());
            // store new token
            tokenMgtDAO.storeAccessToken(accessToken,
                    oAuth2AccessTokenReqDTO.getClientId(),
                    accessTokenDO, userStoreDomain);

            if (log.isDebugEnabled()) {
                log.debug("Persisted an access token with " +
                        "Client ID : " + oAuth2AccessTokenReqDTO.getClientId() +
                        "authorized user : " + tokReqMsgCtx.getAuthorizedUser() +
                        "timestamp : " + timestamp +
                        "validity period : " + validityPeriod +
                        "scope : " + OAuth2Util.buildScopeString(tokReqMsgCtx.getScope()) +
                        "Token State : " + OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE);
            }

            //update cache with newly added token
            if (cacheEnabled) {
                oauthCache.addToCache(cacheKey, accessTokenDO);
                if (log.isDebugEnabled()) {
                    log.debug("Access Token info was added to the cache for the client id : " +
                            oAuth2AccessTokenReqDTO.getClientId());
                }
            }
            tokenRespDTO = new OAuth2AccessTokenRespDTO();
            tokenRespDTO.setAccessToken(accessToken);
            if(issueRefreshToken() &&
                    OAuthServerConfiguration.getInstance().getSupportedGrantTypes().containsKey(
                            GrantType.REFRESH_TOKEN.toString())){
                tokenRespDTO.setRefreshToken(refreshToken);
            }
            tokenRespDTO.setExpiresIn(validityPeriod);
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
        if(tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().equals(
                org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString())){
            authzCallback.setCarbonGrantType(org.wso2.carbon.identity.oauth.common.GrantType.valueOf(
                    OAuthConstants.OAUTH_SAML2_BEARER_GRANT_ENUM.toString()));
        }else{
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
        if(tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().equals(
                org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString())){
            scopeValidationCallback.setCarbonGrantType(org.wso2.carbon.identity.oauth.common.GrantType.valueOf(
                    OAuthConstants.OAUTH_SAML2_BEARER_GRANT_ENUM.toString()));
        }else{
            scopeValidationCallback.setGrantType(GrantType.valueOf(
                    tokReqMsgCtx.getOauth2AccessTokenReqDTO().getGrantType().toUpperCase()));
        }

        callbackManager.handleCallback(scopeValidationCallback);
        tokReqMsgCtx.setValidityPeriod(scopeValidationCallback.getValidityPeriod());
        tokReqMsgCtx.setScope(scopeValidationCallback.getApprovedScope());
        return scopeValidationCallback.isValidScope();
    }
}
