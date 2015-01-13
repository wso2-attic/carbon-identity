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
package org.wso2.carbon.identity.openidconnect;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.openidconnect.as.messages.IDTokenBuilder;
import org.apache.oltu.openidconnect.as.messages.IDTokenException;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.cache.*;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.internal.OAuth2ServiceComponentHolder;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.security.Key;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the IDToken generator for the OpenID Connect Implementation. This
 * IDToken Generator utilizes the Amber IDTokenBuilder to build the IDToken.
 */
public class DefaultIDTokenBuilder implements org.wso2.carbon.identity.openidconnect.IDTokenBuilder {

    private static final String NONE = "NONE";
    private static final String SHA256_WITH_RSA = "SHA256withRSA";
    private static final String SHA384_WITH_RSA = "SHA384withRSA";
    private static final String SHA512_WITH_RSA = "SHA512withRSA";
    private static final String SHA256_WITH_HMAC = "SHA256withHMAC";
    private static final String SHA384_WITH_HMAC = "SHA384withHMAC";
    private static final String SHA512_WITH_HMAC = "SHA512withHMAC";
    private static final String SHA256_WITH_EC = "SHA256withEC";
    private static final String SHA384_WITH_EC = "SHA384withEC";
    private static final String SHA512_WITH_EC = "SHA512withEC";
    private static final String AUTHORIZATION_CODE = "AuthorizationCode";
    private static final String INBOUND_AUTH2_TYPE = "oauth2";
    private static Log log = LogFactory.getLog(DefaultIDTokenBuilder.class);
    private static Map<Integer, Key> privateKeys = new ConcurrentHashMap<Integer, Key>();
    private OAuthServerConfiguration config = null;
    private Algorithm signatureAlgorithm = null;

    public DefaultIDTokenBuilder() throws IdentityOAuth2Exception {

        config = OAuthServerConfiguration.getInstance();
        //map signature algorithm from identity.xml to nimbus format, this is a one time configuration
        signatureAlgorithm = mapSignatureAlgorithm(config.getSignatureAlgorithm());
    }

    @Override
    public String buildIDToken(OAuthTokenReqMessageContext request, OAuth2AccessTokenRespDTO tokenRespDTO)
            throws IdentityOAuth2Exception {

        String issuer = config.getOpenIDConnectIDTokenIssuerIdentifier();
        long lifetime = Integer.parseInt(config.getOpenIDConnectIDTokenExpiration()) * 1000;
        long curTime = Calendar.getInstance().getTimeInMillis();
        // setting subject
        String subject = request.getAuthorizedUser();
        ApplicationManagementService applicationMgtService = OAuth2ServiceComponentHolder.getApplicationMgtService();
        ServiceProvider serviceProvider;
        try {
            String spName = applicationMgtService.getServiceProviderNameByClientId(request.getOauth2AccessTokenReqDTO().
                    getClientId(), INBOUND_AUTH2_TYPE);
            serviceProvider = applicationMgtService.getApplication(spName);
        } catch (IdentityApplicationManagementException ex) {
            String error = "Error occurred while getting service provider information.";
            throw new IdentityOAuth2Exception(error, ex);
        }

        if (serviceProvider != null) {
            String claim = serviceProvider.getLocalAndOutBoundAuthenticationConfig().getSubjectClaimUri();

            if (claim != null) {
                String username = request.getAuthorizedUser();
                String tenantUser = MultitenantUtils.getTenantAwareUsername(username);
                String domainName = MultitenantUtils.getTenantDomain(request.getAuthorizedUser());
                try {
                    subject =
                            IdentityTenantUtil.getRealm(domainName, username)
                                              .getUserStoreManager()
                                              .getUserClaimValue(tenantUser, claim, null);
                    if (subject == null) {
                        subject = request.getAuthorizedUser();
                    }
                } catch (IdentityException e) {
                    String error = "Error occurred while generating the IDToken.";
                    throw new IdentityOAuth2Exception("Error while generating the IDToken", e);
                } catch (UserStoreException e) {
                    String error = "Error occurred while generating the IDToken.";
                    throw new IdentityOAuth2Exception(error, e);
                }
            }
        }
        String nonceValue = null;
        // AuthorizationCode only available for authorization code grant type
        if (request.getProperty(AUTHORIZATION_CODE) != null) {
            AuthorizationGrantCacheEntry authorizationGrantCacheEntry = getAuthorizationGrantCacheEntry(request);
            if (authorizationGrantCacheEntry != null) {
                nonceValue = authorizationGrantCacheEntry.getNonceValue();
            }
        }
        // Get access token issued time
        long accessTokenIssuedTime = getAccessTokenIssuedTime(tokenRespDTO.getAccessToken(), request);
        String atHash = new String(Base64.encodeBase64(tokenRespDTO.getAccessToken().getBytes()));

        if (log.isDebugEnabled()) {
            StringBuilder stringBuilder = (new StringBuilder()).
            append("Using issuer " + issuer).
            append("\n").

            append("Subject " + subject).
            append("\n").

            append("ID Token life time " + lifetime).
            append("\n").

            append("Current time " + curTime).
            append("\n").

            append("Nonce Value " + nonceValue).
            append("\n").

            append("Signature Algorithm " + signatureAlgorithm).
            append("\n");
            log.debug(stringBuilder.toString());
        }

        IDTokenBuilder builder =
                new IDTokenBuilder().setIssuer(issuer).setSubject(subject)
                                    .setAudience(request.getOauth2AccessTokenReqDTO().getClientId())
                                    .setAuthorizedParty(request.getOauth2AccessTokenReqDTO().getClientId())
                                    .setExpiration(curTime + lifetime).setAuthTime(accessTokenIssuedTime)
                                    .setAtHash(atHash).setIssuedAt(curTime);
        if (nonceValue != null) {
            builder.setNonce(nonceValue);
        }
        CustomClaimsCallbackHandler claimsCallBackHandler =
                OAuthServerConfiguration.getInstance().
                        getOpenIDConnectCustomClaimsCallbackHandler();
        claimsCallBackHandler.handleCustomClaims(builder, request);
        try {
            String plainIDToken = builder.buildIDToken();
            if (JWSAlgorithm.NONE.getName().equals(signatureAlgorithm.getName())) {
                return new PlainJWT((com.nimbusds.jwt.JWTClaimsSet) PlainJWT.parse(plainIDToken).getJWTClaimsSet())
                        .serialize();
            }
            return signJWT(plainIDToken, request);
        } catch (IDTokenException e) {
            throw new IdentityOAuth2Exception("Error while generating the IDToken", e);
        } catch (ParseException e) {
            throw new IdentityOAuth2Exception("Error while parsing the IDToken", e);
        }
    }

    /** sign JWT token from RSA algorithm
     * @param payLoad contains JWT body
     * @param request
     * @return signed JWT token
     * @throws IdentityOAuth2Exception
     */
    protected String signJWTWithRSA(String payLoad, OAuthTokenReqMessageContext request)
            throws IdentityOAuth2Exception {
        try {
            String tenantDomain = request.getOauth2AccessTokenReqDTO().getTenantDomain();
            int tenantId = request.getTenantID();
            if (tenantDomain == null) {
                tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }
            if (tenantId == 0) {
                tenantId = MultitenantConstants.SUPER_TENANT_ID;
            }
            Key privateKey = null;

            if (!(privateKeys.containsKey(tenantId))) {
                // get tenant's key store manager
                KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);

                if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                    // derive key store name
                    String ksName = tenantDomain.trim().replace(".", "-");
                    String jksName = ksName + ".jks";
                    // obtain private key
                    privateKey = tenantKSM.getPrivateKey(jksName, tenantDomain);

                } else {
                    try {
                        privateKey = tenantKSM.getDefaultPrivateKey();
                    } catch (Exception e) {
                        throw new IdentityOAuth2Exception("Error while obtaining private key for super tenant", e);
                    }
                }
                //privateKey will not be null always
                privateKeys.put(tenantId, privateKey);
            } else {
                //privateKey will not be null because containsKey() true says given key is exist and ConcurrentHashMap
                // does not allow to store null values
                privateKey = privateKeys.get(tenantId);
            }
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) privateKey);
            SignedJWT signedJWT = new SignedJWT(new JWSHeader((JWSAlgorithm) signatureAlgorithm),
                                                PlainJWT.parse(payLoad).getJWTClaimsSet());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IdentityOAuth2Exception("Error occurred while signing JWT", e);
        } catch (ParseException e) {
            throw new IdentityOAuth2Exception("Error occurred while retrieving claim set for JWT", e);
        }
    }

    /**
     * @param request
     * @return AuthorizationGrantCacheEntry contains user attributes and nonce value
     */
    private AuthorizationGrantCacheEntry getAuthorizationGrantCacheEntry(
            OAuthTokenReqMessageContext request) {

        String authorizationCode = (String) request.getProperty(AUTHORIZATION_CODE);
        AuthorizationGrantCacheKey authorizationGrantCacheKey = new AuthorizationGrantCacheKey(authorizationCode);

        AuthorizationGrantCacheEntry authorizationGrantCacheEntry =
                (AuthorizationGrantCacheEntry) AuthorizationGrantCache.getInstance().
                        getValueFromCache(authorizationGrantCacheKey);
        return authorizationGrantCacheEntry;
    }

    private long getAccessTokenIssuedTime(String accessToken, OAuthTokenReqMessageContext request)
            throws IdentityOAuth2Exception {

        AccessTokenDO accessTokenDO = null;
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

        OAuthCache oauthCache = OAuthCache.getInstance();
        CacheKey cacheKey = new OAuthCacheKey(
                request.getOauth2AccessTokenReqDTO().getClientId() + ":" + request.getAuthorizedUser().toLowerCase() +
                ":" + OAuth2Util.buildScopeString(request.getScope()));
        CacheEntry result = oauthCache.getValueFromCache(cacheKey);

        // cache hit, do the type check.
        if (result instanceof AccessTokenDO) {
            accessTokenDO = (AccessTokenDO) result;
        }

        // Cache miss, load the access token info from the database.
        if (accessTokenDO == null) {
            accessTokenDO = tokenMgtDAO.retrieveAccessToken(accessToken);
        }

        // if the access token or client id is not valid
        if (accessTokenDO == null) {
            throw new IdentityOAuth2Exception("Access token based information is not available in cache or database");
        }

        return accessTokenDO.getIssuedTime().getTime();
    }

    /**
     * Generic Signing function
     *
     * @param payLoad contains JWT body
     * @param request
     * @return
     * @throws IdentityOAuth2Exception
     */
    protected String signJWT(String payLoad, OAuthTokenReqMessageContext request)
            throws IdentityOAuth2Exception {

        if (JWSAlgorithm.RS256.equals(signatureAlgorithm) || JWSAlgorithm.RS384.equals(signatureAlgorithm) ||
            JWSAlgorithm.RS512.equals(signatureAlgorithm)) {
            return signJWTWithRSA(payLoad, request);
        } else if (JWSAlgorithm.HS256.equals(signatureAlgorithm) || JWSAlgorithm.HS384.equals(signatureAlgorithm) ||
                   JWSAlgorithm.HS512.equals(signatureAlgorithm)) {
            // return signWithHMAC(payLoad,jwsAlgorithm,request); implementation need to be done
            return null;
        } else {
            // return signWithEC(payLoad,jwsAlgorithm,request); implementation need to be done
            return null;
        }
    }

    /**
     * This method map signature algorithm define in identity.xml to nimbus
     * signature algorithm
     * format, Strings are defined inline hence there are not being used any
     * where
     *
     * @param signatureAlgorithm
     * @return
     * @throws IdentityOAuth2Exception
     */
    protected JWSAlgorithm mapSignatureAlgorithm(String signatureAlgorithm) throws IdentityOAuth2Exception {

        if (NONE.equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS256;
        } else if (SHA256_WITH_RSA.equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS256;
        } else if (SHA384_WITH_RSA.equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS384;
        } else if (SHA512_WITH_RSA.equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS512;
        } else if (SHA256_WITH_HMAC.equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS256;
        } else if (SHA384_WITH_HMAC.equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS384;
        } else if (SHA512_WITH_HMAC.equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS512;
        } else if (SHA256_WITH_EC.equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES256;
        } else if (SHA384_WITH_EC.equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES384;
        } else if (SHA512_WITH_EC.equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES512;
        }
        throw new IdentityOAuth2Exception("Unsupported Signature Algorithm in identity.xml");
    }

}

