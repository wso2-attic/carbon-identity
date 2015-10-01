/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.oauth.cache.AppInfoCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheKey;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.ResponseHeader;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.token.handlers.clientauth.ClientAuthenticationHandler;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AuthorizationGrantHandler;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.identity.openidconnect.IDTokenBuilder;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class AccessTokenIssuer {

    private static AccessTokenIssuer instance;
    private static Log log = LogFactory.getLog(AccessTokenIssuer.class);
    private Map<String, AuthorizationGrantHandler> authzGrantHandlers =
            new Hashtable<String, AuthorizationGrantHandler>();
    private List<ClientAuthenticationHandler> clientAuthenticationHandlers =
            new ArrayList<ClientAuthenticationHandler>();
    private AppInfoCache appInfoCache;

    private AccessTokenIssuer() throws IdentityOAuth2Exception {

        authzGrantHandlers = OAuthServerConfiguration.getInstance().getSupportedGrantTypes();
        clientAuthenticationHandlers = OAuthServerConfiguration.getInstance().getSupportedClientAuthHandlers();
        appInfoCache = AppInfoCache.getInstance(OAuthServerConfiguration.getInstance().getAppInfoCacheTimeout());
        if (appInfoCache != null) {
            if (log.isDebugEnabled()) {
                log.debug("Successfully created AppInfoCache under " + OAuthConstants.OAUTH_CACHE_MANAGER);
            }
        } else {
            log.error("Error while creating AppInfoCache");
        }

    }

    public static AccessTokenIssuer getInstance() throws IdentityOAuth2Exception {

        CarbonUtils.checkSecurity();

        if (instance == null) {
            synchronized (AccessTokenIssuer.class) {
                if (instance == null) {
                    instance = new AccessTokenIssuer();
                }
            }
        }
        return instance;
    }

    public OAuth2AccessTokenRespDTO issue(OAuth2AccessTokenReqDTO tokenReqDTO)
            throws IdentityException, InvalidOAuthClientException {

        String grantType = tokenReqDTO.getGrantType();
        OAuth2AccessTokenRespDTO tokenRespDTO;

        AuthorizationGrantHandler authzGrantHandler = authzGrantHandlers.get(grantType);

        OAuthTokenReqMessageContext tokReqMsgCtx = new OAuthTokenReqMessageContext(tokenReqDTO);

        // If multiple client authenticaton methods have been used the authorization server must reject the request
        int authenticatorHandlerIndex = -1;
        for (int i = 0; i < clientAuthenticationHandlers.size(); i++) {
            if (clientAuthenticationHandlers.get(i).canAuthenticate(tokReqMsgCtx)) {
                if (authenticatorHandlerIndex > -1) {
                    log.debug("Multiple Client Authentication Methods used for client id : " +
                            tokenReqDTO.getClientId());
                    tokenRespDTO = handleError(
                            OAuthConstants.OAuthError.TokenResponse.UNSUPPORTED_CLIENT_AUTHENTICATION_METHOD,
                            "Unsupported Client Authentication Method!", tokenReqDTO);
                    setResponseHeaders(tokReqMsgCtx, tokenRespDTO);
                    return tokenRespDTO;
                }
                authenticatorHandlerIndex = i;
            }
        }
        if (authenticatorHandlerIndex < 0 && authzGrantHandler.isConfidentialClient()) {
            log.debug("Confidential client cannot be authenticated for client id : " +
                    tokenReqDTO.getClientId());
            tokenRespDTO = handleError(
                    OAuthConstants.OAuthError.TokenResponse.UNSUPPORTED_CLIENT_AUTHENTICATION_METHOD,
                    "Unsupported Client Authentication Method!", tokenReqDTO);
            setResponseHeaders(tokReqMsgCtx, tokenRespDTO);
            return tokenRespDTO;
        }

        ClientAuthenticationHandler clientAuthHandler = null;
        if (authenticatorHandlerIndex > -1) {
            clientAuthHandler = clientAuthenticationHandlers.get(authenticatorHandlerIndex);
        }
        boolean isAuthenticated;
        if (clientAuthHandler != null) {
            isAuthenticated = clientAuthHandler.authenticateClient(tokReqMsgCtx);
        } else {
            isAuthenticated = true;
        }

        // loading the stored application data
        OAuthAppDO oAuthAppDO = getAppInformation(tokenReqDTO);
        String applicationName = oAuthAppDO.getApplicationName();
        if (!authzGrantHandler.isOfTypeApplicationUser()) {
            tokReqMsgCtx.setAuthorizedUser(OAuth2Util.getUserFromUserName(oAuthAppDO.getUserName()));
            tokReqMsgCtx.setTenantID(oAuthAppDO.getTenantId());
        }

        boolean isValidGrant = authzGrantHandler.validateGrant(tokReqMsgCtx);
        boolean isAuthorized = authzGrantHandler.authorizeAccessDelegation(tokReqMsgCtx);
        boolean isValidScope = authzGrantHandler.validateScope(tokReqMsgCtx);

        String userName = tokReqMsgCtx.getAuthorizedUser().toString();

        //boolean isAuthenticated = true;
        if (!isAuthenticated) {
            //Do not change this log format as these logs use by external applications
            log.debug("Client Authentication Failed for client id=" + tokenReqDTO.getClientId() + ", " +
                    "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.INVALID_CLIENT,
                    "Client credentials are invalid.", tokenReqDTO);
            setResponseHeaders(tokReqMsgCtx, tokenRespDTO);
            return tokenRespDTO;
        }

        //boolean isValidGrant = true;
        if (!isValidGrant) {
            //Do not change this log format as these logs use by external applications
            log.debug("Invalid Grant provided by the client, id=" + tokenReqDTO.getClientId() + ", " +
                    "" + "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.INVALID_GRANT,
                    "Provided Authorization Grant is invalid.", tokenReqDTO);
            setResponseHeaders(tokReqMsgCtx, tokenRespDTO);
            return tokenRespDTO;
        }

        //boolean isAuthorized = true;
        if (!isAuthorized) {
            //Do not change this log format as these logs use by external applications
            log.debug("Resource owner is not authorized to grant access, client-id="
                    + tokenReqDTO.getClientId() + " " + "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT,
                    "Unauthorized Client!", tokenReqDTO);
            setResponseHeaders(tokReqMsgCtx, tokenRespDTO);
            return tokenRespDTO;
        }

        //boolean isValidScope = true;
        if (!isValidScope) {
            //Do not change this log format as these logs use by external applications
            log.debug("Invalid Scope provided. client-id=" + tokenReqDTO.getClientId() + " " +
                    "" + "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.INVALID_SCOPE, "Invalid Scope!", tokenReqDTO);
            setResponseHeaders(tokReqMsgCtx, tokenRespDTO);
            return tokenRespDTO;
        }

        tokenRespDTO = authzGrantHandler.issue(tokReqMsgCtx);
        tokenRespDTO.setCallbackURI(oAuthAppDO.getCallbackUrl());

        String[] scopes = tokReqMsgCtx.getScope();
        if (scopes != null && scopes.length > 0) {
            StringBuilder scopeString = new StringBuilder("");
            for (String scope : scopes) {
                scopeString.append(scope);
                scopeString.append(" ");
            }
            tokenRespDTO.setAuthorizedScopes(scopeString.toString().trim());
        }

        setResponseHeaders(tokReqMsgCtx, tokenRespDTO);

        //Do not change this log format as these logs use by external applications
        if (log.isDebugEnabled()) {
            log.debug("Access Token issued to client. client-id=" + tokenReqDTO.getClientId() + " " +
                    "" + "user-name=" + userName + " to application=" + applicationName);
        }

        if (tokReqMsgCtx.getScope() != null && OAuth2Util.isOIDCAuthzRequest(tokReqMsgCtx.getScope())) {
            IDTokenBuilder builder = OAuthServerConfiguration.getInstance().getOpenIDConnectIDTokenBuilder();
            tokenRespDTO.setIDToken(builder.buildIDToken(tokReqMsgCtx, tokenRespDTO));
        }

        if (tokenReqDTO.getGrantType().equals(GrantType.AUTHORIZATION_CODE.toString())) {
            addUserAttributesToCache(tokenReqDTO, tokenRespDTO);
        }

        return tokenRespDTO;
    }

    private void addUserAttributesToCache(OAuth2AccessTokenReqDTO tokenReqDTO, OAuth2AccessTokenRespDTO tokenRespDTO) {
        AuthorizationGrantCacheKey oldCacheKey = new AuthorizationGrantCacheKey(tokenReqDTO.getAuthorizationCode());
        //checking getUserAttributesId vale of cacheKey before retrieve entry from cache as it causes to NPE
        if (oldCacheKey.getUserAttributesId() != null) {
            CacheEntry authorizationGrantCacheEntry = AuthorizationGrantCache.getInstance(OAuthServerConfiguration.
                    getInstance().getAuthorizationGrantCacheTimeout())
                    .getValueFromCache(oldCacheKey);
            AuthorizationGrantCacheKey newCacheKey = new AuthorizationGrantCacheKey(tokenRespDTO.getAccessToken());
            int authorizationGrantCacheTimeout = OAuthServerConfiguration.getInstance().getAuthorizationGrantCacheTimeout();
            AuthorizationGrantCache.getInstance(authorizationGrantCacheTimeout).addToCache(newCacheKey, authorizationGrantCacheEntry);
            AuthorizationGrantCache.getInstance(authorizationGrantCacheTimeout).clearCacheEntry(oldCacheKey);
        }
    }

    private OAuthAppDO getAppInformation(OAuth2AccessTokenReqDTO tokenReqDTO) throws IdentityOAuth2Exception, InvalidOAuthClientException {
        OAuthAppDO oAuthAppDO = appInfoCache.getValueFromCache(tokenReqDTO.getClientId());
        if (oAuthAppDO != null) {
            return oAuthAppDO;
        } else {
            oAuthAppDO = new OAuthAppDAO().getAppInformation(tokenReqDTO.getClientId());
            appInfoCache.addToCache(tokenReqDTO.getClientId(), oAuthAppDO);
            return oAuthAppDO;
        }
    }

    private OAuth2AccessTokenRespDTO handleError(String errorCode,
                                                 String errorMsg,
                                                 OAuth2AccessTokenReqDTO tokenReqDTO) {
        if (log.isDebugEnabled()) {
            log.debug("OAuth-Error-Code=" + errorCode + " client-id=" + tokenReqDTO.getClientId()
                    + " grant-type=" + tokenReqDTO.getGrantType()
                    + " scope=" + OAuth2Util.buildScopeString(tokenReqDTO.getScope()));
        }
        OAuth2AccessTokenRespDTO tokenRespDTO;
        tokenRespDTO = new OAuth2AccessTokenRespDTO();
        tokenRespDTO.setError(true);
        tokenRespDTO.setErrorCode(errorCode);
        tokenRespDTO.setErrorMsg(errorMsg);
        return tokenRespDTO;
    }

    private void setResponseHeaders(OAuthTokenReqMessageContext tokReqMsgCtx,
                                    OAuth2AccessTokenRespDTO tokenRespDTO) {
        if (tokReqMsgCtx.getProperty("RESPONSE_HEADERS") != null) {
            tokenRespDTO.setResponseHeaders((ResponseHeader[]) tokReqMsgCtx.getProperty("RESPONSE_HEADERS"));
        }
    }
}
