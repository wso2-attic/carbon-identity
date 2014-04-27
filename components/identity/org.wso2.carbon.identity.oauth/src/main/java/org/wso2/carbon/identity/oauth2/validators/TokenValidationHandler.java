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

package org.wso2.carbon.identity.oauth2.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.authcontext.AuthorizationContextTokenGenerator;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;

import java.util.Hashtable;
import java.util.Map;

/**
 * Handles the token validation by invoking the proper validation handler by looking at the token
 * type.
 */
public class TokenValidationHandler {

    private Log log = LogFactory.getLog(TokenValidationHandler.class);

    private Map<String, OAuth2TokenValidator> tokenValidators =
            new Hashtable<String, OAuth2TokenValidator>();

    private static TokenValidationHandler instance = new TokenValidationHandler();

    private TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

    AuthorizationContextTokenGenerator tokenGenerator = null;

    private TokenValidationHandler() {
        if(instance == null){
            synchronized (this) {
                if(instance == null){
                    tokenValidators.put(DefaultOAuth2TokenValidator.TOKEN_TYPE, new DefaultOAuth2TokenValidator());
                    // setting up the JWT if required
                    if (OAuthServerConfiguration.getInstance().isAuthContextTokGenEnabled()) {
                        try {
                            Class clazz = this.getClass().getClassLoader().loadClass(OAuthServerConfiguration.getInstance().
                                    getTokenGeneratorImplClass());
                            tokenGenerator = (AuthorizationContextTokenGenerator)clazz.newInstance();
                            tokenGenerator.init();
                            if (log.isDebugEnabled()) {
                                log.debug("An instance of " + OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass() +
                                    " is created for OAuthServerConfiguration.");
                            }
                        } catch (ClassNotFoundException e){
                            String errorMsg = "Class not found: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg,e);
                        } catch (InstantiationException e) {
                            String errorMsg = "Error while instantiating: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg, e);
                        } catch (IllegalAccessException e) {
                            String errorMsg = "Illegal access to: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg, e);
                        } catch (IdentityOAuth2Exception e){
                            String errorMsg = "Error while initializing: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg, e);
                        }

                    }
                }
            }
        }
    }

    public static TokenValidationHandler getInstance() {
        return instance;
    }
    
    public void addTokenValidator(String type, OAuth2TokenValidator handler) {
        tokenValidators.put(type, handler);
    }

    public OAuth2TokenValidationResponseDTO validate(OAuth2TokenValidationRequestDTO requestDTO)
            throws IdentityOAuth2Exception {

        OAuth2TokenValidationResponseDTO responseDTO = new OAuth2TokenValidationResponseDTO();
        OAuth2TokenValidationMessageContext messageContext =
                new OAuth2TokenValidationMessageContext(requestDTO, responseDTO);

        OAuth2TokenValidationRequestDTO.OAuth2AccessToken accessToken = requestDTO.getAccessToken();

        // incomplete token validation request
        if (accessToken == null) {
            log.debug("Access Token is not present in the validation request");
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Access Token is not present in the validation request");
            return responseDTO;
        }

        String accessTokenIdentifier = accessToken.getIdentifier();
        // incomplete token validation request
        if (accessTokenIdentifier == null) {
            log.debug("Access token identifier is not present in the validation request");
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Access token identifier is not present in the validation request");
            return responseDTO;
        }

		OAuth2TokenValidator tokenValidator = tokenValidators.get(requestDTO.getAccessToken().getTokenType());

		// There is no token validator for the provided token type.
		if (tokenValidator == null) {
			log.debug("Unsupported access token type");
			responseDTO.setValid(false);
			responseDTO.setErrorMsg("Unsupported access token type");
			return responseDTO;
		}

        AccessTokenDO accessTokenDO = null;

        boolean cacheHit = false;
        // Check the cache, if caching is enabled.
        if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
            OAuthCache oauthCache = OAuthCache.getInstance();
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
            accessTokenDO = tokenMgtDAO.retrieveAccessToken(accessTokenIdentifier);
            
            // No data retrieved due to invalid input.
            if(accessTokenDO == null) {
            	responseDTO.setValid(false);
            	responseDTO.setErrorMsg("Invalid input. Access token validation failed");
            	return responseDTO;
            }
        }

        // Check whether the grant is expired
        long issuedTimeInMillis = accessTokenDO.getIssuedTime().getTime();
        long validityPeriodInMillis = accessTokenDO.getValidityPeriod();
        long timestampSkew = OAuthServerConfiguration.getInstance()
                .getTimeStampSkewInSeconds() * 1000;
        long currentTimeInMillis = System.currentTimeMillis();

        if ((currentTimeInMillis - timestampSkew) > (issuedTimeInMillis + validityPeriodInMillis)) {
            log.debug("Access token has expired");
            if (log.isDebugEnabled()) {
                log.debug("Access Token : " + accessTokenIdentifier + " has expired." +
                        " Issued Time(ms) : " + issuedTimeInMillis +
                        ", Validity Period : " + validityPeriodInMillis +
                        ", Timestamp Skew : " + timestampSkew +
                        ", Current Time : " + currentTimeInMillis);
            }
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Access token has expired");
            return responseDTO;
        }

        // Add the token back to the cache in the case of a cache miss
        if (OAuthServerConfiguration.getInstance().isCacheEnabled() && !cacheHit) {
            OAuthCache oauthCache = OAuthCache.getInstance();
            CacheKey cacheKey = new OAuthCacheKey(accessTokenIdentifier);
            oauthCache.addToCache(cacheKey, accessTokenDO);
            if(log.isDebugEnabled()){
                log.debug("Access Token Info object was added back to the cache.");
            }
        }

        // Set the token expiry time
        long expiryTime = (issuedTimeInMillis + validityPeriodInMillis) - (currentTimeInMillis + timestampSkew);
        responseDTO.setExpiryTime(expiryTime / 1000);

        // Adding the AccessTokenDO as a context property for further use
        messageContext.addProperty("AccessTokenDO", accessTokenDO);

        boolean isValidAccessDelegation = tokenValidator.validateAccessDelegation(messageContext);
        boolean isValidScope = tokenValidator.validateScope(messageContext);
        boolean isValidAccessToken = tokenValidator.validateAccessToken(messageContext);

        if(!isValidAccessDelegation){
            log.debug("Invalid access delegation");
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Invalid access delegation");
            return responseDTO;
        }

        if(!isValidScope){
            log.debug("Scope validation failed");
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("Scope validation failed");
            return responseDTO;
        }

        if(!isValidAccessToken){
            log.debug("OAuth2 access token validation failed");
            responseDTO.setValid(false);
            responseDTO.setErrorMsg("OAuth2 access token validation failed");
            return responseDTO;
        }

        if(responseDTO.getAuthorizedUser() == null ||
                responseDTO.getAuthorizedUser().equals("")){
            responseDTO.setAuthorizedUser(accessTokenDO.getAuthzUser());
        }
        if(responseDTO.getScope() == null ||
                responseDTO.getScope().equals("")){
            responseDTO.setScope(accessTokenDO.getScope());
        }
        responseDTO.setValid(true);

        if(tokenGenerator != null){
            tokenGenerator.generateToken(messageContext);
            if (log.isDebugEnabled()) {
                log.debug(tokenGenerator.getClass().getName() + "generated token set to response : " +
                        responseDTO.getAuthorizationContextToken().getTokenString());
            }
        }

        return responseDTO;
    }
}
