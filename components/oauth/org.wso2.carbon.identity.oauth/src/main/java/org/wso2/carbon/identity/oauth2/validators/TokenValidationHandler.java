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

package org.wso2.carbon.identity.oauth2.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authcontext.AuthorizationContextTokenGenerator;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.Map;
import java.util.TreeMap;

/**
 * Handles the token validation by invoking the proper validation handler by looking at the token
 * type.
 */
public class TokenValidationHandler {

    private static TokenValidationHandler instance = null;
    AuthorizationContextTokenGenerator tokenGenerator = null;
    private Log log = LogFactory.getLog(TokenValidationHandler.class);
    private Map<String, OAuth2TokenValidator> tokenValidators =  new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

    private TokenValidationHandler() {
        tokenValidators.put(DefaultOAuth2TokenValidator.TOKEN_TYPE, new DefaultOAuth2TokenValidator());
        for (Map.Entry<String, String> entry : OAuthServerConfiguration.getInstance().getTokenValidatorClassNames().entrySet()) {
            String className = null;
            try {
                String type = entry.getKey();
                className = entry.getValue();
                Class clazz = Thread.currentThread().getContextClassLoader().loadClass(entry.getValue());
                OAuth2TokenValidator tokenValidator = (OAuth2TokenValidator) clazz.newInstance();
                tokenValidators.put(type, tokenValidator);
            } catch (ClassNotFoundException e) {
                log.error("Class not in build path " + className, e);
            } catch (InstantiationException e) {
                log.error("Class initialization error " + className, e);
            } catch (IllegalAccessException e) {
                log.error("Class access error " + className, e);
            }
        }

        // setting up the JWT if required
        if (OAuthServerConfiguration.getInstance().isAuthContextTokGenEnabled()) {
            try {
                Class clazz = this.getClass().getClassLoader().loadClass(OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass());
                tokenGenerator = (AuthorizationContextTokenGenerator) clazz.newInstance();
                tokenGenerator.init();
                if (log.isDebugEnabled()) {
                    log.debug("An instance of " + OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass() +
                            " is created for OAuthServerConfiguration.");
                }
            } catch (ClassNotFoundException e) {
                String errorMsg = "Class not found: " +
                        OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                log.error(errorMsg, e);
            } catch (InstantiationException e) {
                String errorMsg = "Error while instantiating: " +
                        OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                log.error(errorMsg, e);
            } catch (IllegalAccessException e) {
                String errorMsg = "Illegal access to: " +
                        OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                log.error(errorMsg, e);
            } catch (IdentityOAuth2Exception e) {
                String errorMsg = "Error while initializing: " +
                        OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                log.error(errorMsg, e);
            }
        }
    }

    public static TokenValidationHandler getInstance() {
        if (instance == null) {
            synchronized (TokenValidationHandler.class) {
                if (instance == null) {
                    instance = new TokenValidationHandler();
                }
            }
        }
        return instance;
    }

    public void addTokenValidator(String type, OAuth2TokenValidator handler) {
        tokenValidators.put(type, handler);
    }

    /**
     * @param requestDTO
     * @return
     * @throws IdentityOAuth2Exception
     */
    public OAuth2TokenValidationResponseDTO validate(OAuth2TokenValidationRequestDTO requestDTO)
            throws IdentityOAuth2Exception {

        OAuth2ClientApplicationDTO appToken = findOAuthConsumerIfTokenIsValid(requestDTO);
        return appToken.getAccessTokenValidationResponse();
    }

    /**
     * @param requestDTO
     * @return
     * @throws IdentityOAuth2Exception
     */
    public OAuth2ClientApplicationDTO findOAuthConsumerIfTokenIsValid(OAuth2TokenValidationRequestDTO requestDTO)
            throws IdentityOAuth2Exception {

        OAuth2ClientApplicationDTO clientApp = new OAuth2ClientApplicationDTO();
        OAuth2TokenValidationResponseDTO responseDTO = new OAuth2TokenValidationResponseDTO();
        OAuth2TokenValidationMessageContext messageContext =
                new OAuth2TokenValidationMessageContext(requestDTO, responseDTO);

        OAuth2TokenValidationRequestDTO.OAuth2AccessToken accessToken = requestDTO.getAccessToken();

        // incomplete token validation request
        if (accessToken == null) {
            if (log.isDebugEnabled()) {
                log.debug("Access Token is not present in the validation request");
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Access Token is not present in the validation request");
            clientApp.setAccessTokenValidationResponse(responseDTO);
            return clientApp;
        }

        String accessTokenIdentifier = accessToken.getIdentifier();
        // incomplete token validation request
        if (accessTokenIdentifier == null) {
            if (log.isDebugEnabled()) {
                log.debug("Access token identifier is not present in the validation request");
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Access token identifier is not present in the validation request");
            clientApp.setAccessTokenValidationResponse(responseDTO);
            return clientApp;
        }

        OAuth2TokenValidator tokenValidator = tokenValidators.get(requestDTO.getAccessToken().getTokenType());

        // There is no token validator for the provided token type.
        if (tokenValidator == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unsupported access token type : " + requestDTO.getAccessToken().getTokenType());
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Unsupported access token type");
            clientApp.setAccessTokenValidationResponse(responseDTO);
            return clientApp;
        }

        AccessTokenDO accessTokenDO = null;

        boolean cacheHit = false;
        // Check the cache, if caching is enabled.
        if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
            OAuthCache oauthCache = OAuthCache.getInstance(OAuthServerConfiguration.getInstance().getOAuthCacheTimeout());
            CacheKey cacheKey = new OAuthCacheKey(requestDTO.getAccessToken().getIdentifier());
            CacheEntry result = oauthCache.getValueFromCache(cacheKey);
            // cache hit, do the type check.
            if (result instanceof AccessTokenDO) {
                accessTokenDO = (AccessTokenDO) result;
                cacheHit = true;
            }
        }
        // Cache miss, load the access token info from the database.
        if (accessTokenDO == null) {
            accessTokenDO = tokenMgtDAO.retrieveAccessToken(accessTokenIdentifier, false);

            // No data retrieved due to invalid input.
            if (accessTokenDO == null) {
                if (log.isDebugEnabled()){
                    log.debug("Invalid access token. Access token validation failed");
                }
                responseDTO.setValid(false);
                responseDTO.setErrorMsg("Invalid input. Access token validation failed");
                clientApp.setAccessTokenValidationResponse(responseDTO);
                return clientApp;
            }
        }

        // Check whether the grant is expired
        if(accessTokenDO.getValidityPeriod() < 0) {
            if (log.isDebugEnabled()) {
                log.debug("Access Token has infinite lifetime");
            }
        } else {
            if (OAuth2Util.getAccessTokenExpireMillis(accessTokenDO) == 0) {
                if (log.isDebugEnabled()) {
                        log.debug("Access Token has expired");
                    }
                responseDTO.setValid(false);
                responseDTO.setErrorMsg("Access token has expired");
                clientApp.setAccessTokenValidationResponse(responseDTO);
                return clientApp;
            }
        }

        // Add the token back to the cache in the case of a cache miss
        if (OAuthServerConfiguration.getInstance().isCacheEnabled() && !cacheHit) {
            OAuthCache oauthCache = OAuthCache.getInstance(OAuthServerConfiguration.getInstance().getOAuthCacheTimeout());
            CacheKey cacheKey = new OAuthCacheKey(accessTokenIdentifier);
            oauthCache.addToCache(cacheKey, accessTokenDO);
            if (log.isDebugEnabled()) {
                log.debug("Access Token Info object was added back to the cache.");
            }
        }

        // Set the token expiry time
        long expiryTime = OAuth2Util.getAccessTokenExpireMillis(accessTokenDO);
        if(OAuthConstants.UserType.APPLICATION_USER.equals(accessTokenDO.getTokenType()) &&
                OAuthServerConfiguration.getInstance().getUserAccessTokenValidityPeriodInSeconds() < 0){
            responseDTO.setExpiryTime(Long.MAX_VALUE);
        } else if (OAuthConstants.UserType.APPLICATION.equals(accessTokenDO.getTokenType()) &&
                    OAuthServerConfiguration.getInstance().getApplicationAccessTokenValidityPeriodInSeconds() < 0) {
            responseDTO.setExpiryTime(Long.MAX_VALUE);
        } else if(expiryTime > 0){
            responseDTO.setExpiryTime(expiryTime / 1000);
        } else if (expiryTime < 0) {
            responseDTO.setExpiryTime(Long.MAX_VALUE);
        }

        // Adding the AccessTokenDO as a context property for further use
        messageContext.addProperty("AccessTokenDO", accessTokenDO);

        boolean isValidAccessDelegation = tokenValidator.validateAccessDelegation(messageContext);
        boolean isValidScope = tokenValidator.validateScope(messageContext);
        boolean isValidAccessToken = tokenValidator.validateAccessToken(messageContext);

        if (!isValidAccessDelegation) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid access delegation");
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Invalid access delegation");
            clientApp.setAccessTokenValidationResponse(responseDTO);
            return clientApp;
        }

        if (!isValidScope) {
            if (log.isDebugEnabled()) {
                log.debug("Scope validation failed");
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Scope validation failed");
            clientApp.setAccessTokenValidationResponse(responseDTO);
            return clientApp;
        }

        if (!isValidAccessToken) {
            if (log.isDebugEnabled()) {
                log.debug("OAuth2 access token validation failed");
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("OAuth2 access token validation failed");
            clientApp.setAccessTokenValidationResponse(responseDTO);
            return clientApp;
        }

        User user = accessTokenDO.getAuthzUser();
        String authzUser = UserCoreUtil.addDomainToName(user.getUserName(), user.getUserStoreDomain());
        authzUser = UserCoreUtil.addTenantDomainToEntry(authzUser, user.getTenantDomain());
        responseDTO.setAuthorizedUser(authzUser);
        responseDTO.setScope(accessTokenDO.getScope());
        responseDTO.setValid(true);

        if (tokenGenerator != null) {
            tokenGenerator.generateToken(messageContext);
        }

        clientApp.setAccessTokenValidationResponse(responseDTO);
        clientApp.setConsumerKey(accessTokenDO.getConsumerKey());
        return clientApp;
    }
}
