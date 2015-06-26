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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.apache.amber.oauth2.common.message.types.GrantType;
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
	private static ConcurrentHashMap<Integer, Key> privateKeys =
	                                                             new ConcurrentHashMap<Integer, Key>();
	private static ConcurrentHashMap<Integer, Certificate> publicCerts =
	                                                                     new ConcurrentHashMap<Integer, Certificate>();

	private static final String NONE = "NONE";
	private static final String INBOUND_AUTH2_TYPE = "oauth2";

	/**
	 * Build ID Token
	 *
	 * @param request
	 * @param tokenRespDTO
	 * @return generated id token
	 * @throws IdentityOAuth2Exception
	 */
	public String buildIDToken(OAuthTokenReqMessageContext request,
	                           OAuth2AccessTokenRespDTO tokenRespDTO)
	                                                                 throws IdentityOAuth2Exception {

		OAuthServerConfiguration config = OAuthServerConfiguration.getInstance();
		String signatureAlgorithm = config.getSignatureAlgorithm();
		if (!signatureAlgorithm.equals(NONE)) {
			// if signature algorithm cannot map throws an Exception
			mapSignatureAlgorithm(signatureAlgorithm);
		}
		String issuer = config.getOpenIDConnectIDTokenIssuerIdentifier();

        long lifetime;
        long curTime = Calendar.getInstance().getTimeInMillis();
        long accessTokenIssuedTime = getAccessTokenIssuedTime(tokenRespDTO.getAccessToken(), request);
        if(OAuthServerConfiguration.getInstance().isOIDCIDTokenExpInSecs()){
            lifetime = Integer.parseInt(config.getOpenIDConnectIDTokenExpiration());
            curTime = curTime/1000;
            accessTokenIssuedTime = accessTokenIssuedTime/1000;
        } else {
            lifetime = Integer.parseInt(config.getOpenIDConnectIDTokenExpiration()) * 1000;
        }

        String subject = request.getAuthorizedUser();

        if (!GrantType.AUTHORIZATION_CODE.toString().equals(request.getOauth2AccessTokenReqDTO().getGrantType()) &&
            !org.wso2.carbon.identity.oauth.common.GrantType.SAML20_BEARER.toString().equals(request
                                                                        .getOauth2AccessTokenReqDTO().getGrantType())) {

            ApplicationManagementService applicationMgtService = OAuth2ServiceComponentHolder
                    .getApplicationMgtService();
            ServiceProvider serviceProvider = null;
            String claim = null;
            try {
                String spName =
                        applicationMgtService.getServiceProviderNameByClientId(request.getOauth2AccessTokenReqDTO()
                                                                                       .getClientId(),
                                                                               INBOUND_AUTH2_TYPE);
                serviceProvider = applicationMgtService.getApplication(spName);
            } catch (IdentityApplicationManagementException ex) {
                log.error("Error while getting service provider information.", ex);
                throw new IdentityOAuth2Exception("Error while getting service provider information.",
                                                  ex);
            }

            if (serviceProvider != null) {
                claim = serviceProvider.getLocalAndOutBoundAuthenticationConfig().getSubjectClaimUri();
            }

            if (claim != null) {
                String username = request.getAuthorizedUser();
                String tenantUser = MultitenantUtils.getTenantAwareUsername(username);
                String domainName = MultitenantUtils.getTenantDomain(request.getAuthorizedUser());
                try {
                    subject =
                            IdentityTenantUtil.getRealm(domainName, username).getUserStoreManager()
                                    .getUserClaimValue(tenantUser, claim, null);
                    if (subject == null) {
                        subject = request.getAuthorizedUser();
                    }
                } catch (Exception e) {
                    log.error("Error while generating the IDToken.", e);
                    throw new IdentityOAuth2Exception("Error while generating the IDToken", e);
                }
            }
        }

		String nonceValue = null;
		// AuthorizationCode only available for authorization code grant type
		if (request.getProperty("AuthorizationCode") != null) {
			nonceValue = getNonce(request);
		}


		String atHash = new String(Base64.encodeBase64(tokenRespDTO.getAccessToken().getBytes()));

		if (log.isDebugEnabled()) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Using issuer " + issuer);
			stringBuilder.append("\n");

			stringBuilder.append("Subject " + subject);
			stringBuilder.append("\n");

			stringBuilder.append("ID Token life time " + lifetime);
			stringBuilder.append("\n");

			stringBuilder.append("Current time " + curTime);
			stringBuilder.append("\n");

			stringBuilder.append("Nonce Value " + nonceValue);
			stringBuilder.append("\n");

			stringBuilder.append("Signature Algorithm " + signatureAlgorithm);
			stringBuilder.append("\n");
			log.debug(stringBuilder.toString());
		}
		IDTokenBuilder builder = null;
		builder =
		          new IDTokenBuilder().setIssuer(issuer)
		                              .setSubject(subject)
		                              .setAudience(request.getOauth2AccessTokenReqDTO()
		                                                  .getClientId())
		                              .setAuthorizedParty(request.getOauth2AccessTokenReqDTO()
		                                                         .getClientId())
		                              .setExpiration(curTime + lifetime)
		                              .setAuthTime(accessTokenIssuedTime).setAtHash(atHash)
		                              .setIssuedAt(curTime);
		if (nonceValue != null) {
			builder.setNonce(nonceValue);
		}

        request.addProperty("accessToken", tokenRespDTO.getAccessToken());
		CustomClaimsCallbackHandler claimsCallBackHandler =
		                                                    OAuthServerConfiguration.getInstance()
		                                                                            .getOpenIDConnectCustomClaimsCallbackHandler();
		claimsCallBackHandler.handleCustomClaims(builder, request);

		try {
			String plainIDToken = builder.buildIDToken();
			if (signatureAlgorithm.equals(NONE)) {
				return new PlainJWT((com.nimbusds.jwt.JWTClaimsSet)
					PlainJWT.parse(plainIDToken).getJWTClaimsSet()).serialize();
			}
			return signJWT(plainIDToken, request);
		} catch (IDTokenException e) {
			log.error("Error while generating the IDToken", e);
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
				tenantId = OAuth2Util.getTenantId(tenantDomain);
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
			SignedJWT signedJWT =
			                      new SignedJWT(new JWSHeader(jwsAlgorithm),
			                                    PlainJWT.parse(payLoad).getJWTClaimsSet());
			signedJWT.sign(signer);
			return signedJWT.serialize();
		} catch (KeyStoreException e) {
			log.error("Error in obtaining tenant's keystore", e);
			throw new IdentityOAuth2Exception("Error in obtaining tenant's keystore", e);
		} catch (JOSEException e) {
			log.error("Error in obtaining tenant's keystore", e);
			throw new IdentityOAuth2Exception("Error in obtaining tenant's keystore", e);
		} catch (Exception e) {
			log.error("Error in obtaining tenant's keystore", e);
			throw new IdentityOAuth2Exception("Error in obtaining tenant's keystore", e);
		}
	}

	/**
	 * Retrieve nonce value from user attribute cache, this implementation has
	 * to be change
	 *
	 * @param request
	 * @return
	 */
	private String getNonce(OAuthTokenReqMessageContext request) {
		// "AuthorizationCode" was not defined inside the constant file since
		// temporary implementation
		String authorizationCode = (String) request.getProperty("AuthorizationCode");
		AuthorizationGrantCacheKey authorizationGrantCacheKey =
		                                                        new AuthorizationGrantCacheKey(
		                                                                                       authorizationCode);

		AuthorizationGrantCacheEntry authorizationGrantCacheEntry =
		                                                            (AuthorizationGrantCacheEntry) AuthorizationGrantCache.getInstance()
		                                                                                                                  .getValueFromCache(authorizationGrantCacheKey);

		return authorizationGrantCacheEntry.getNonceValue();
	}

	/**
	 * Get access token issued time from cache, if cache miss get access token
	 * issued time from DB
	 *
	 * @param accessToken
	 * @return
	 * @throws IdentityOAuth2Exception
	 */
	private long getAccessTokenIssuedTime(String accessToken, OAuthTokenReqMessageContext request)
	                                                                                              throws IdentityOAuth2Exception {
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
		if (accessTokenDO == null) {
			accessTokenDO = tokenMgtDAO.retrieveAccessToken(accessToken, false);
		}

		// if the access token or client id is not valid
		if (accessTokenDO == null) {
			log.error("Error occurred while getting access token based information");
			throw new IdentityOAuth2Exception(
			                                  "Error occurred while getting access token based information");
		}
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
		log.error("UnSupported Signature Algorithm");
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
		log.error("Unsupported Signature Algorithm in identity.xml");
		throw new IdentityOAuth2Exception("Unsupported Signature Algorithm in identity.xml");
	}
}
