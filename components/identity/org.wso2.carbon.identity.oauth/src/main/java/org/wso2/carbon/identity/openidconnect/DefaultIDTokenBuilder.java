/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.PlainJWT;
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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the IDToken generator for the OpenID Connect Implementation. This
 * IDToken Generator utilizes the Amber IDTokenBuilder to build the IDToken.
 */
public class DefaultIDTokenBuilder implements org.wso2.carbon.identity.openidconnect.IDTokenBuilder {

    private static Log log = LogFactory.getLog(DefaultIDTokenBuilder.class);
    private static boolean DEBUG = log.isDebugEnabled();
    private static ConcurrentHashMap<Integer, Key> privateKeys = new ConcurrentHashMap<Integer, Key>();
    private static ConcurrentHashMap<Integer, Certificate> publicCerts = new ConcurrentHashMap<Integer, Certificate>();

    private static final String NONE = "NONE";
    private static final String RS256 = "RS256";
    private static final String RS384 = "RS384";
    private static final String RS512 = "RS512";

    private static final String INBOUND_AUTH2_TYPE = "oauth2";


    public String buildIDToken(OAuthTokenReqMessageContext request, OAuth2AccessTokenRespDTO tokenRespDTO)
            throws IdentityOAuth2Exception {

        OAuthServerConfiguration config = OAuthServerConfiguration.getInstance();
        String signatureAlgorithm = config.getSignatureAlgorithm();
        if (!signatureAlgorithm.equals(NONE)) {
            //if signature algorithm cannot map throws an Exception
            mapSignatureAlgorithm(signatureAlgorithm);
        }
        String issuer = config.getOpenIDConnectIDTokenIssuerIdentifier();
        long lifetime = Integer.parseInt(config.getOpenIDConnectIDTokenExpiration()) * 1000;
        long curTime = Calendar.getInstance().getTimeInMillis();
        // setting subject
        String subject = request.getAuthorizedUser();

        ApplicationManagementService applicationMgtService =
                OAuth2ServiceComponentHolder.getApplicationMgtService();
        ServiceProvider serviceProvider;
        String claim = null;
        try {
            String spName =
                    applicationMgtService.getServiceProviderNameByClientId(request.getOauth2AccessTokenReqDTO()
                                    .getClientId(),
                            INBOUND_AUTH2_TYPE);
            serviceProvider = applicationMgtService.getApplication(spName);
        } catch (IdentityApplicationManagementException ex) {
            String error = "Error occurred while getting service provider information.";
            log.error(error, ex);
            throw new IdentityOAuth2Exception(error, ex);
        }

        if (serviceProvider != null) {
            claim = serviceProvider.getLocalAndOutBoundAuthenticationConfig().getSubjectClaimUri();

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
                    log.error(error, e);
                    throw new IdentityOAuth2Exception("Error while generating the IDToken", e);
                } catch (UserStoreException e) {
                    String error = "Error occurred while generating the IDToken.";
                    log.error(error, e);
                    throw new IdentityOAuth2Exception(error, e);
                }
            }
        }

        String nonceValue = null;
        // AuthorizationCode only available for authorization code grant type
        if (request.getProperty("AuthorizationCode") != null) {
            nonceValue = getNonce(request);
        }
        // Get access token issued time
        long accessTokenIssuedTime = getAccessTokenIssuedTime(tokenRespDTO.getAccessToken(), request);
        String atHash = new String(Base64.encodeBase64(tokenRespDTO.getAccessToken().getBytes()));
        //if signature algorithm is NONE do not sign ID Token
        signatureAlgorithm = OAuthServerConfiguration.getInstance().getSignatureAlgorithm();

        if (DEBUG) {
            log.debug("Using issuer " + issuer);
            log.debug("Subject " + subject);
            log.debug("ID Token expiration seconds " + lifetime);
            log.debug("Current time " + curTime);
            log.debug("Nonce Value " + nonceValue);
            log.debug("Signature Algorithm " + signatureAlgorithm);
        }
        IDTokenBuilder builder = null;
        builder = new IDTokenBuilder().setIssuer(issuer).setSubject(subject)
                .setAudience(request.getOauth2AccessTokenReqDTO().getClientId())
                .setAuthorizedParty(request.getOauth2AccessTokenReqDTO().getClientId())
                .setExpiration(curTime + lifetime)
                .setAuthTime(accessTokenIssuedTime)
                .setAtHash(atHash)
                .setIssuedAt(curTime);
        if (nonceValue != null) {
            builder.setNonce(nonceValue);
        }
        CustomClaimsCallbackHandler claimsCallBackHandler = OAuthServerConfiguration.getInstance()
                .getOpenIDConnectCustomClaimsCallbackHandler();
        claimsCallBackHandler.handleCustomClaims(builder, request);
        try {
            String plainIDToken = builder.buildIDToken();
            if (signatureAlgorithm.equals(NONE)) {
                return PlainJWT.parse(plainIDToken).serialize();
            }
            return signJWT(plainIDToken, request);
        } catch (IDTokenException e) {
            throw new IdentityOAuth2Exception("Error while generating the IDToken", e);
        } catch (ParseException e) {
            throw new IdentityOAuth2Exception("Error while parsing the IDToken", e);
        }
    }

    /**
     * Sign with given RSA Algorithm
     *
     * @param payLoad
     * @param jwsAlgorithm
     * @param request
     * @return
     * @throws IdentityOAuth2Exception
     */
    protected String signJWTWithRSA(String payLoad, JWSAlgorithm jwsAlgorithm,
                                    OAuthTokenReqMessageContext request)
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
                        log.error("Error while obtaining private key for super tenant", e);
                    }
                }
                if (privateKey != null) {
                    privateKeys.put(tenantId, privateKey);
                }
            } else {
                privateKey = privateKeys.get(tenantId);
            }

            Certificate publicCert = null;

            if (!(publicCerts.containsKey(tenantId))) {
                // get tenant's key store manager
                KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);

                KeyStore keyStore = null;
                if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                    // derive key store name
                    String ksName = tenantDomain.trim().replace(".", "-");
                    String jksName = ksName + ".jks";
                    keyStore = tenantKSM.getKeyStore(jksName);
                    publicCert = keyStore.getCertificate(tenantDomain);
                } else {
                    publicCert = tenantKSM.getDefaultPrimaryCertificate();
                }
                if (publicCert != null) {
                    publicCerts.put(tenantId, publicCert);
                }
            } else {
                publicCert = publicCerts.get(tenantId);
            }
            JWSSigner signer = new RSASSASigner((RSAPrivateKey) privateKey);
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(jwsAlgorithm), PlainJWT.parse(payLoad).getJWTClaimsSet());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (KeyStoreException e) {
            String error = "Error in obtaining tenant's keystore";
            throw new IdentityOAuth2Exception(error);
        } catch (JOSEException e) {
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (Exception e) {
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    /**
     * Retrieve nonce value from user attribute cache, this implementation has to be change
     *
     * @param request
     * @return
     */
    private String getNonce(OAuthTokenReqMessageContext request) {
        //"AuthorizationCode" was not defined inside the constant file since
        // tempory implementation
        String authorizationCode = (String) request.getProperty("AuthorizationCode");
        AuthorizationGrantCacheKey authorizationGrantCacheKey = new
                AuthorizationGrantCacheKey(authorizationCode);

        AuthorizationGrantCacheEntry authorizationGrantCacheEntry =
                (AuthorizationGrantCacheEntry) AuthorizationGrantCache.getInstance()
                        .getValueFromCache
                                (authorizationGrantCacheKey);
        return authorizationGrantCacheEntry.getNonceValue();
    }

    /**
     * @param accessToken
     * @return
     * @throws IdentityOAuth2Exception
     */
    private long getAccessTokenIssuedTime(String accessToken, OAuthTokenReqMessageContext request) throws
            IdentityOAuth2Exception {
        AccessTokenDO accessTokenDO = null;
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

        OAuthCache oauthCache = OAuthCache.getInstance();
        CacheKey cacheKey =
                new OAuthCacheKey(request.getOauth2AccessTokenReqDTO().getClientId() +
                        ":" + request.getAuthorizedUser().toLowerCase() +
                        ":" + OAuth2Util.buildScopeString(request.getScope()));
        CacheEntry result = oauthCache.getValueFromCache(cacheKey);

        // cache hit, do the type check.
        if (result instanceof AccessTokenDO) {
            accessTokenDO = (AccessTokenDO) result;
        }

        // Cache miss, load the access token info from the database.
        if (null == accessTokenDO) {
            accessTokenDO = tokenMgtDAO.retrieveAccessToken(accessToken);
        }

        // if the access token or client id is not valid
        if (null == accessTokenDO) {
            log.error("Error occured while getting access token based information"); //$NON-NLS-1$
            throw new IdentityOAuth2Exception(
                    "Error occured while getting access token based information"); //$NON-NLS-1$
        }

        long timeIndMilliSeconds = accessTokenDO.getIssuedTime().getTime();

        return accessTokenDO.getIssuedTime().getTime();
    }

    /**
     * Generic Signing function
     *
     * @param payLoad
     * @param request
     * @return
     * @throws IdentityOAuth2Exception
     */
    protected String signJWT(String payLoad, OAuthTokenReqMessageContext request)
            throws IdentityOAuth2Exception {
        JWSAlgorithm jwsAlgorithm =
                mapSignatureAlgorithm(OAuthServerConfiguration.getInstance()
                        .getSignatureAlgorithm());
        if (JWSAlgorithm.RS256.equals(jwsAlgorithm) || JWSAlgorithm.RS384.equals(jwsAlgorithm) ||
                JWSAlgorithm.RS512.equals(jwsAlgorithm)) {
            return signJWTWithRSA(payLoad, jwsAlgorithm, request);
        } else if (JWSAlgorithm.HS256.equals(jwsAlgorithm) ||
                JWSAlgorithm.HS384.equals(jwsAlgorithm) ||
                JWSAlgorithm.HS512.equals(jwsAlgorithm)) {
            // return signWithHMAC(payLoad,jwsAlgorithm,request); implementation
            // need to be done
        } else if (JWSAlgorithm.ES256.equals(jwsAlgorithm) ||
                JWSAlgorithm.ES384.equals(jwsAlgorithm) ||
                JWSAlgorithm.ES512.equals(jwsAlgorithm)) {
            // return signWithEC(payLoad,jwsAlgorithm,request); implementation
            // need to be done
        }
        throw new IdentityOAuth2Exception("UnSupported Signature Algorithm");
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
    protected JWSAlgorithm mapSignatureAlgorithm(String signatureAlgorithm)
            throws IdentityOAuth2Exception {
        if ("SHA256withRSA".equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS256;
        } else if ("SHA384withRSA".equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS384;
        } else if ("SHA512withRSA".equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS512;
        } else if ("SHA256withHMAC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS256;
        } else if ("SHA384withHMAC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS384;
        } else if ("SHA512withHMAC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS512;
        } else if ("SHA256withEC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES256;
        } else if ("SHA384withEC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES384;
        } else if ("SHA512withEC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES512;
        }
        throw new IdentityOAuth2Exception("Unsupported Signature Algorithm in identity.xml");
    }

}

