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

package org.wso2.carbon.identity.oauth2.authz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.cache.AppInfoCache;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authz.handlers.ResponseTypeHandler;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.HashMap;
import java.util.Map;

public class AuthorizationHandlerManager {

    public static final String CODE = "code";
    public static final String TOKEN = "token";
    public static final String IMPLICIT = "implicit";
    private static Log log = LogFactory.getLog(AuthorizationHandlerManager.class);

    private static AuthorizationHandlerManager instance;

    private Map<String, ResponseTypeHandler> responseHandlers = new HashMap<>();

    private AppInfoCache appInfoCache;

    private AuthorizationHandlerManager() throws IdentityOAuth2Exception {
        responseHandlers = OAuthServerConfiguration.getInstance().getSupportedResponseTypes();
        appInfoCache = AppInfoCache.getInstance(OAuthServerConfiguration.getInstance().getAppInfoCacheTimeout());
        if (appInfoCache != null) {
            if (log.isDebugEnabled()) {
                log.debug("Successfully created AppInfoCache under " + OAuthConstants.OAUTH_CACHE_MANAGER);
            }
        } else {
            log.error("Error while creating AppInfoCache");
        }
    }

    public static AuthorizationHandlerManager getInstance() throws IdentityOAuth2Exception {

        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (AuthorizationHandlerManager.class) {
                if (instance == null) {
                    instance = new AuthorizationHandlerManager();
                }
            }
        }
        return instance;
    }

    public OAuth2AuthorizeRespDTO handleAuthorization(OAuth2AuthorizeReqDTO authzReqDTO)
            throws IdentityOAuth2Exception, IdentityOAuthAdminException, InvalidOAuthClientException {

        String responseType = authzReqDTO.getResponseType();
        OAuth2AuthorizeRespDTO authorizeRespDTO = new OAuth2AuthorizeRespDTO();

        if (!responseHandlers.containsKey(responseType)) {
            log.warn("Unsupported Response Type : " + responseType +
                    " provided  for user : " + authzReqDTO.getUsername());
            handleErrorRequest(authorizeRespDTO, OAuth2ErrorCodes.UNSUPPORTED_RESP_TYPE,
                    "Unsupported Response Type!");
            authorizeRespDTO.setCallbackURI(authzReqDTO.getCallbackUrl());
            return authorizeRespDTO;
        }

        // loading the stored application data
        OAuthAppDO oAuthAppDO = getAppInformation(authzReqDTO);
        // If the application has defined a limited set of grant types, then check the grant
        if (oAuthAppDO.getGrantTypes() != null) {
            if (CODE.equals(responseType)) {
                //Do not change this log format as these logs use by external applications
                if (!oAuthAppDO.getGrantTypes().contains("authorization_code")) {
                    log.debug("Unsupported Response Type : " + responseType +
                            " for client id : " + authzReqDTO.getConsumerKey());
                    handleErrorRequest(authorizeRespDTO, OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE,
                            "Unsupported Response Type!");
                    return authorizeRespDTO;
                }
            } else if (TOKEN.equals(responseType) && !oAuthAppDO.getGrantTypes().contains(IMPLICIT)) {
                //Do not change this log format as these logs use by external applications
                log.debug("Unsupported Response Type : " + responseType + " for client id : " + authzReqDTO
                        .getConsumerKey());
                handleErrorRequest(authorizeRespDTO, OAuthError.TokenResponse.UNSUPPORTED_GRANT_TYPE,
                        "Unsupported Response Type!");
                return authorizeRespDTO;
            }
        }

        ResponseTypeHandler authzHandler = responseHandlers.get(responseType);
        OAuthAuthzReqMessageContext authzReqMsgCtx = new OAuthAuthzReqMessageContext(authzReqDTO);

        boolean accessDelegationAuthzStatus = authzHandler.validateAccessDelegation(authzReqMsgCtx);
        if (!accessDelegationAuthzStatus) {
            log.warn("User : " + authzReqDTO.getUsername() +
                    " doesn't have necessary rights to grant access to the resource(s) " +
                    OAuth2Util.buildScopeString(authzReqDTO.getScopes()));
            handleErrorRequest(authorizeRespDTO, OAuth2ErrorCodes.UNAUTHORIZED_CLIENT,
                    "Authorization Failure!");
            authorizeRespDTO.setCallbackURI(authzReqDTO.getCallbackUrl());
            return authorizeRespDTO;
        }

        boolean scopeValidationStatus = authzHandler.validateScope(authzReqMsgCtx);
        if (!scopeValidationStatus) {
            log.warn("Scope validation failed for user : "
                    + authzReqDTO.getUsername() + ", for the scope : "
                    + OAuth2Util.buildScopeString(authzReqDTO.getScopes()));
            handleErrorRequest(authorizeRespDTO,
                    OAuth2ErrorCodes.INVALID_SCOPE, "Invalid Scope!");
            authorizeRespDTO.setCallbackURI(authzReqDTO.getCallbackUrl());
            return authorizeRespDTO;
        } else {
            // We are here because the call-back handler has approved the scope.
            // If call-back handler set the approved scope - then we respect that. If not we take
            // the approved scope as the provided scope.
            if (authzReqMsgCtx.getApprovedScope() == null
                    || authzReqMsgCtx.getApprovedScope().length == 0) {
                authzReqMsgCtx
                        .setApprovedScope(authzReqMsgCtx.getAuthorizationReqDTO().getScopes());
            }
        }

        authorizeRespDTO = authzHandler.issue(authzReqMsgCtx);
        return authorizeRespDTO;
    }

    private void handleErrorRequest(OAuth2AuthorizeRespDTO respDTO, String errorCode,
                                    String errorMsg) {
        respDTO.setErrorCode(errorCode);
        respDTO.setErrorMsg(errorMsg);
    }

    private OAuthAppDO getAppInformation(OAuth2AuthorizeReqDTO authzReqDTO) throws IdentityOAuth2Exception, InvalidOAuthClientException {
        OAuthAppDO oAuthAppDO;
        Object obj = appInfoCache.getValueFromCache(authzReqDTO.getConsumerKey());
        if (obj != null) {
            oAuthAppDO = (OAuthAppDO) obj;
            return oAuthAppDO;
        } else {
            oAuthAppDO = new OAuthAppDAO().getAppInformation(authzReqDTO.getConsumerKey());
            appInfoCache.addToCache(authzReqDTO.getConsumerKey(), oAuthAppDO);
            return oAuthAppDO;
        }
    }
}
