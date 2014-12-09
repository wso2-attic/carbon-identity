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

package org.wso2.carbon.identity.oauth2.token;

import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.openidconnect.as.util.OIDCAuthzServerUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.oauth.cache.*;
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

    private Map<String, AuthorizationGrantHandler> authzGrantHandlers =
            new Hashtable<String, AuthorizationGrantHandler>();
    private List<ClientAuthenticationHandler> clientAuthenticationHandlers =
            new ArrayList<ClientAuthenticationHandler>();

    private static AccessTokenIssuer instance;

    private static Log log = LogFactory.getLog(AccessTokenIssuer.class);
    private BaseCache<String, OAuthAppDO> appInfoCache;

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

    private AccessTokenIssuer() throws IdentityOAuth2Exception {

        authzGrantHandlers = OAuthServerConfiguration.getInstance().getSupportedGrantTypes();
        clientAuthenticationHandlers = OAuthServerConfiguration.getInstance().getSupportedClientAuthHandlers();
        appInfoCache = new BaseCache<String,OAuthAppDO>("AppInfoCache");
        if(appInfoCache != null) {
            if (log.isDebugEnabled()) {
                log.debug("Successfully created AppInfoCache under "+ OAuthConstants.OAUTH_CACHE_MANAGER);
            }
        }
        else {
            log.error("Error while creating AppInfoCache");
        }

    }

    public OAuth2AccessTokenRespDTO issue(OAuth2AccessTokenReqDTO tokenReqDTO)
            throws IdentityException, InvalidOAuthClientException {

        String grantType = tokenReqDTO.getGrantType();
        OAuth2AccessTokenRespDTO tokenRespDTO;

        if (!authzGrantHandlers.containsKey(grantType)) {
            //Do not change this log format as these logs use by external applications
            log.debug("Unsupported Grant Type : " + grantType +
                    " for client id : " + tokenReqDTO.getClientId());
            tokenRespDTO = handleError(OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE,
                    "Unsupported Grant Type!", tokenReqDTO);
            return tokenRespDTO;
        }

        // loading the stored application data
        OAuthAppDO oAuthAppDO = getAppInformation(tokenReqDTO);
        // If the application has defined a limited set of grant types, then check the grant
        if(oAuthAppDO.getGrantTypes() != null && !oAuthAppDO.getGrantTypes().contains(grantType)) {
            //Do not change this log format as these logs use by external applications
            log.debug("Unsupported Grant Type : " + grantType +
                    " for client id : " + tokenReqDTO.getClientId());
            tokenRespDTO = handleError(OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE,
                    "Unsupported Grant Type!", tokenReqDTO);
            return tokenRespDTO;
        }

        AuthorizationGrantHandler authzGrantHandler = authzGrantHandlers.get(grantType);

        OAuthTokenReqMessageContext tokReqMsgCtx = new OAuthTokenReqMessageContext(tokenReqDTO);

        // If multiple client authenticaton methods have been used the authorization server must reject the request
        int authenticatorHandlerIndex = -1;
        for(int i = 0; i < clientAuthenticationHandlers.size(); i++){
            if(clientAuthenticationHandlers.get(i).canAuthenticate(tokReqMsgCtx)){
                if(authenticatorHandlerIndex > -1) {
                    log.debug("Multiple Client Authentication Methods used for client id : " +
                            tokenReqDTO.getClientId());
                    tokenRespDTO = handleError(
                            OAuthConstants.OAuthError.TokenResponse.UNSUPPORTED_CLIENT_AUTHENTICATION_METHOD,
                            "Unsupported Client Authentication Method!", tokenReqDTO);
                    return tokenRespDTO;
                }
                authenticatorHandlerIndex = i;
            }
        }
        if(authenticatorHandlerIndex < 0 && authzGrantHandler.isConfidentialClient()){
            log.debug("Confidential client cannot be authenticated for client id : " +
                    tokenReqDTO.getClientId());
            tokenRespDTO = handleError(
                    OAuthConstants.OAuthError.TokenResponse.UNSUPPORTED_CLIENT_AUTHENTICATION_METHOD,
                    "Unsupported Client Authentication Method!", tokenReqDTO);
            return tokenRespDTO;
        }

        ClientAuthenticationHandler clientAuthHandler = null;
        if(authenticatorHandlerIndex > -1){
            clientAuthHandler = clientAuthenticationHandlers.get(authenticatorHandlerIndex);
        }
        boolean isAuthenticated;
        if(clientAuthHandler != null){
            isAuthenticated = clientAuthHandler.authenticateClient(tokReqMsgCtx);
        } else {
            isAuthenticated = true;
        }

        String applicationName = oAuthAppDO.getApplicationName();
        String userName = tokReqMsgCtx.getAuthorizedUser();
        if(!authzGrantHandler.isOfTypeApplicationUser()) {
            tokReqMsgCtx.setAuthorizedUser(oAuthAppDO.getUserName());
            tokReqMsgCtx.setTenantID(oAuthAppDO.getTenantId());
        }

        //boolean isAuthenticated = true;
        if (!isAuthenticated) {
            //Do not change this log format as these logs use by external applications
            log.debug("Client Authentication Failed for client id=" + tokenReqDTO.getClientId() + ", " +
                      "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.INVALID_CLIENT,
                                       "Client credentials are invalid.", tokenReqDTO);
            return tokenRespDTO;
        }

        boolean isValidGrant = authzGrantHandler.validateGrant(tokReqMsgCtx);
        //boolean isValidGrant = true;
        if (!isValidGrant) {
            //Do not change this log format as these logs use by external applications
            log.debug("Invalid Grant provided by the client, id=" + tokenReqDTO.getClientId() + ", " +
                      "" + "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.INVALID_GRANT,
                                       "Provided Authorization Grant is invalid.", tokenReqDTO);
            return tokenRespDTO;
        }

        boolean isAuthorized = authzGrantHandler.authorizeAccessDelegation(tokReqMsgCtx);
        //boolean isAuthorized = true;
        if (!isAuthorized) {
            //Do not change this log format as these logs use by external applications
            log.debug("Resource owner is not authorized to grant access, client-id="
                      + tokenReqDTO.getClientId() + " " + "user-name=" + userName + " to application=" +
                      applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT,
                                       "Unauthorized Client!", tokenReqDTO);
            return tokenRespDTO;
        }

        boolean isValidScope = authzGrantHandler.validateScope(tokReqMsgCtx);
        //boolean isValidScope = true;
        if (!isValidScope) {
            //Do not change this log format as these logs use by external applications
            log.debug("Invalid Scope provided. client-id=" + tokenReqDTO.getClientId() + " " +
                      "" + "user-name=" + userName + " to application=" + applicationName);
            tokenRespDTO = handleError(OAuthError.TokenResponse.INVALID_SCOPE, "Invalid Scope!", tokenReqDTO);
            return tokenRespDTO;
        }

        tokenRespDTO = authzGrantHandler.issue(tokReqMsgCtx);
        tokenRespDTO.setCallbackURI(oAuthAppDO.getCallbackUrl());

        String[] scopes = tokReqMsgCtx.getScope();
        if(scopes != null && scopes.length > 0){
            StringBuilder scopeString = new StringBuilder("");
            for(String scope : scopes){
                scopeString.append(scope);
                scopeString.append(" ");
            }
            tokenRespDTO.setAuthorizedScopes(scopeString.toString().trim());
        }

        if(tokReqMsgCtx.getProperty("RESPONSE_HEADERS") != null){
            tokenRespDTO.setResponseHeaders((ResponseHeader[]) tokReqMsgCtx.getProperty("RESPONSE_HEADERS"));
        }
        
        //Do not change this log format as these logs use by external applications
        if (log.isDebugEnabled()) {
            log.debug("Access Token issued to client. client-id=" + tokenReqDTO.getClientId() + " " +
                    "" + "user-name=" + userName + " to application=" + applicationName);
        }

        if(tokReqMsgCtx.getScope() != null && OIDCAuthzServerUtil.isOIDCAuthzRequest(tokReqMsgCtx.getScope())) {
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
        if(oldCacheKey.getUserAttributesId() != null){
            CacheEntry userAttributesCacheEntry = AuthorizationGrantCache.getInstance().getValueFromCache(oldCacheKey);
            AuthorizationGrantCacheKey newCacheKey = new AuthorizationGrantCacheKey(tokenRespDTO.getAccessToken());
            AuthorizationGrantCache.getInstance().addToCache(newCacheKey,userAttributesCacheEntry);
            AuthorizationGrantCache.getInstance().clearCacheEntry(oldCacheKey);
        }
    }

    private OAuthAppDO getAppInformation(OAuth2AccessTokenReqDTO tokenReqDTO) throws IdentityOAuth2Exception, InvalidOAuthClientException {
        OAuthAppDO oAuthAppDO = appInfoCache.getValueFromCache(tokenReqDTO.getClientId());
        if(oAuthAppDO != null){
            return oAuthAppDO;
        }else{
            oAuthAppDO = new OAuthAppDAO().getAppInformation(tokenReqDTO.getClientId());
            appInfoCache.addToCache(tokenReqDTO.getClientId(),oAuthAppDO);
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
}
