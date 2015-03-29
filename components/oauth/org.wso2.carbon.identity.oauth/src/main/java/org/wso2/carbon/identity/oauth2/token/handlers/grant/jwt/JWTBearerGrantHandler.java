
/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License
 */

package org.wso2.carbon.identity.oauth2.token.handlers.grant.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.eac.ECDSAPublicKey;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.oauth.cache.JWTCache;
import org.wso2.carbon.identity.oauth.cache.JWTCacheEntry;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.model.RequestParameter;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *  Class to handle JSON Web Token(JWT) grant type
 */
public class JWTBearerGrantHandler extends AbstractAuthorizationGrantHandler {

    private static Log log = LogFactory.getLog(JWTBearerGrantHandler.class);
    private JWTCache jwtCache;

    // JWT Profile for Oauth2 constants
    private static final String OAUTH_JWT_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String OAUTH_JWT_BEARER_GRANT_ERROR = "invalid_grant";
    private static final String OAUTH_JWT_BEARER_ISSUER = "issuer";
    private static final String OAUTH_JWT_ASSERTION = "assertion";

    // allowed number of minutes a token can be old for
    private static final int REJECT_TOKEN_ISSUED_BEFORE = 30;

    // whether a list of used JWT IDs are cached/persisted
    private static final boolean CACHE_USED_JTI = true;
    private static String tenantDomain;

    // initialize the JWT cache
    public void init() throws IdentityOAuth2Exception {
        super.init();
        if (CACHE_USED_JTI) {
            this.jwtCache = JWTCache.getInstance();
        }
    }


    /**
     * We're validating the JWT token that we receive from the request. Through the assertion parameter is the POST
     * request. A request format that we handle here looks like,
     * <p/>
     * POST /token.oauth2 HTTP/1.1
     * Host: as.example.com
     * Content-Type: application/x-www-form-urlencoded
     * <p/>
     * grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer
     * &assertion=eyJhbGciOiJFUzI1NiJ9.
     * eyJpc3Mi[...omitted for brevity...].
     *
     * @param tokReqMsgCtx Token message request context
     * @return true if validation is successful, false otherwise
     * @throws org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception
     */
    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {

        super.validateGrant(tokReqMsgCtx);

        SignedJWT signedJWT;
        IdentityProvider identityProvider = null;
        String tokenEndPointAlias = null;

        // get the tenant domain from the token request message context
        tenantDomain = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getTenantDomain();

        if (tenantDomain == null || "".equals(tenantDomain)) {
            tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        }


        // Try to get the assertion from the Oauth2AccessTokenReqDTO assertion attribute
        String assertion = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getAssertion();


        // Since the assertion was not found in the assertion variable of the Oauth2AccessTokenReqDTO
        // we try to find it in the request parameters
        if (assertion == null) {
            RequestParameter[] params = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getRequestParameters();

            for (RequestParameter param : params) {
                if (param.getKey().equals(OAUTH_JWT_ASSERTION)) {
                    // if there is a param called assertion
                    assertion = param.getValue()[0];
                    if (log.isDebugEnabled()){
                        log.debug("JWT assertion found in request parameters: " + assertion);
                    }
                    break;
                }
            }

            // check whether a non empty assertion string was acquired
            if (assertion == null || StringUtils.isEmpty(assertion)) {
                log.error("No Valid Assertion was found for " + OAUTH_JWT_BEARER_GRANT_TYPE);
                return false;
            }
        }

        //logging the JSON Web Token(JWT) assertion
        if (log.isDebugEnabled()) {
            log.debug("Received JSON Web Token assertion: " + assertion);
        }

        try {
            signedJWT = SignedJWT.parse(assertion);

            // logging the parsed JWT
            if (log.isDebugEnabled()) {
                logJWT(signedJWT);
            }

        } catch (ParseException e) {
            // there's some error in the JSON Web Token
            log.error("Error parsing the JWT received " + e.getMessage(), e);
            return false;
        }

        // get the claims from the parsed JWT
        ReadOnlyJWTClaimsSet claimsSet;
        try {
            claimsSet = signedJWT.getJWTClaimsSet();
            if(log.isDebugEnabled()){
                log.debug("Claim Set of the JWT: " + claimsSet.toJSONObject().toString());
            }
        } catch (ParseException e) {
            log.error("Error when trying to retrieve claimsSet from the JWT", e);
            return false;
        }


        /**
         *  The JWT MUST contain an iss (issuer) claim that contains a unique identifier for the entity
         *  that issued the JWT. In the absence of an application profile specifying otherwise, compliant
         *  applications MUST compare Issuer values using the Simple String Comparison method
         */
        String jwtIssuer = claimsSet.getIssuer();

        if (jwtIssuer == null || StringUtils.isEmpty(jwtIssuer)) {
            log.debug("Issuer(iss) is empty in the JSON Web Token");
            return false;
        } else {

            log.debug("Issuer Found in JWT: " + jwtIssuer);

            // find and match a registered IDP to confirm that JWT is issued by a known IDP
            try {

                // TODO add an additional param called "issuer" in Oauth
                /**
                 // find a registered IDP with the 'Issuer' property matching that of the issuer value of JWT
                 identityProvider = IdentityProviderManager.getInstance().getIdPByAuthenticatorPropertyValue(
                 ISSUER,jwtIssuer, tenantDomain, false);
                 **/

                // TODO remove this after adding a property called "issuer" in Oauth Authenticator UI
                identityProvider = createDummyIDP();

                if (identityProvider != null) {
                    // check whether the IDP found is a resident IDP
                    if (IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME.equals(
                            identityProvider.getIdentityProviderName())) {
                        identityProvider = IdentityProviderManager.getInstance().getResidentIdP(tenantDomain);

                        FederatedAuthenticatorConfig[] fedAuthnConfigs =
                                identityProvider.getFederatedAuthenticatorConfigs();

                        // Get OpenIDConnect authenticator == OAuth
                        // authenticator
                        FederatedAuthenticatorConfig oauthAuthenticatorConfig =
                                IdentityApplicationManagementUtil.getFederatedAuthenticator(fedAuthnConfigs,
                                        IdentityApplicationConstants.Authenticator.OIDC.NAME);

                        // Get OAuth token endpoint
                        Property oauthTokenURL = null;

                        if (oauthAuthenticatorConfig != null){
                            oauthTokenURL = IdentityApplicationManagementUtil.getProperty(
                                    oauthAuthenticatorConfig.getProperties(),
                                    IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL);
                        }

                        if (oauthTokenURL != null) {
                            tokenEndPointAlias = oauthTokenURL.getValue();
                            if (log.isDebugEnabled()){
                                log.debug("Token End Point Alias of Resident IDP :" + tokenEndPointAlias);
                            }
                        }
                    }
                    // our identity provider is a federated IDP
                    else {
                        tokenEndPointAlias = identityProvider.getAlias();
                        if (log.isDebugEnabled()){
                            log.debug("Token End Point Alias of the Federated IDP: " + tokenEndPointAlias);
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No Registered IDP found for the JWT with issuer name : " + jwtIssuer);
                    }
                    return false;
                }

            } catch (IdentityApplicationManagementException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Error while getting the Federated Identity Provider ", e);
                }
            }
        }

        log.debug("Issuer(iss) of the JWT validated successfully");


        /**
         *  The JWT MUST be digitally signed or have a Message Authentication Code applied by the
         *  issuer. The authorization server MUST reject JWTs with an invalid signature or Message
         *  Authentication Code
         *
         *  Therefore we need to verify the signature of the issuer
         */

        boolean signatureValid = false;
        try {
            signatureValid = validateSignature(signedJWT, identityProvider);
        } catch (JOSEException e) {
            if (log.isDebugEnabled()){
                log.error("Error when verifying signature", e);
            }
        }

        if (signatureValid) {
            log.debug("Signature/MAC validated successfully");
        } else {
            log.debug("Signature or Message Authentication invalid");
            log.debug("JWT Rejected and validation terminated");
            return false;
        }

        /**
         * The JWT MUST contain a sub (subject) claim identifying the principal that is the subject of the JWT
         * For the authorization grant, the subject typically identifies an authorized accessor for which
         * the access token is being requested (i.e., the resource owner or an authorized delegate),
         * but in some cases, may be a pseudonymous identifier or other value denoting an anonymous user
         *
         */
        String subject = claimsSet.getSubject();

        if (subject == null || StringUtils.isEmpty(subject)) {
            log.debug("subject(sub)is empty in JSON Web Token(JWT) ");
            return false;
        } else {
            tokReqMsgCtx.setAuthorizedUser(subject);
            if (log.isDebugEnabled()){
                log.debug("Subject(sub) found in JWT: " + subject);
                log.debug(subject + " set as the Authorized User");
            }
        }

        /**
         * The JWT MUST contain an aud (audience) claim containing a value that identifies the
         * authorization server as an intended audience. The token endpoint URL of the authorization
         * server MAY be used as a value for an aud element to identify the authorization server as an
         * intended audience of the JWT. The Authorization Server MUST reject any JWT that does not
         * contain its own identity as the intended audience
         */
        List<String> audience = claimsSet.getAudience();
        if (tokenEndPointAlias == null ||StringUtils.isEmpty(tokenEndPointAlias)) {
            log.debug("Token End Point of the IDP is empty");
            return false;
        }
        if (log.isDebugEnabled()){
            log.debug("Audience(aud) found in JWT: " + audience.toString());
        }

        boolean audienceFound = false;
        // iterate through the list audience in JWT if one of the audience entries matches the TokenEndPoint URL
        // of the IDP then we can proceed further with the check
        for (String aud : audience) {
            // if the audience entry matches with the token end point URL we are good to go
            if (StringUtils.equals(tokenEndPointAlias, aud)) {
                if (log.isDebugEnabled()) {
                    log.debug(tokenEndPointAlias + " of IDP was found in the list of audiences");
                }
                audienceFound = true;
                break;
            }
        }

        if (!audienceFound){
            // we are here because none of the audience(aud) matched the token End Point
            if (log.isDebugEnabled()) {
                log.debug("None of the audience values matched the tokenEndpoint Alias " + tokenEndPointAlias);
            }
            return false;
        }

        log.debug("Audience(aud) of the JWT validated successfully");


        /**
         * The JWT MUST contain an exp (expiration) claim that limits the time window during which
         * the JWT can be used. The authorization server MUST reject any JWT with an expiration time
         * that has passed, subject to allowable clock skew between systems. Note that the
         * authorization server may reject JWTs with an exp claim value that is unreasonably far in the
         * future.
         */
        Date expirationTime = claimsSet.getExpirationTime();

        if (expirationTime == null) {
            log.debug("No Expiration Time(exp) found in the JWT");
            return false;
        }

        long currentTimeInMillis = System.currentTimeMillis();
        long expirationTimeInMillis = expirationTime.getTime();
        long timeStampSkewMillis = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;


        if (log.isDebugEnabled()) {
            log.debug("Current time: " + new Date(currentTimeInMillis));
            log.debug("Expiration Time(exp) found in JWT: " + expirationTime);

            log.debug("Current time in ms: " + currentTimeInMillis);
            log.debug("Expiration Time(exp) found in JWT in ms: " + expirationTimeInMillis);
            log.debug("Timestamp skew of the OauthServer : " + timeStampSkewMillis + "ms");
        }

        // check whether the JWT has expired
        if ((currentTimeInMillis + timeStampSkewMillis) > expirationTimeInMillis) {
            if (log.isDebugEnabled()) {
                log.debug("JSON Web Token : " + signedJWT.getPayload().toJSONObject().toString() + " is expired." +
                        ", Expiration Time(ms) : " + expirationTimeInMillis +
                        ", TimeStamp Skew : " + timeStampSkewMillis +
                        ", Current Time : " + currentTimeInMillis);
            }
            log.debug("JWT Rejected and validation terminated");
            return false;
        }
        log.debug("Expiration Time(exp) of JWT was validated successfully");


        /**
         * The JWT MAY contain an nbf (not before) claim that identifies the time before which the
         * token MUST NOT be accepted for processing.
         *
         */
        Date notBeforeTime = claimsSet.getNotBeforeTime();

        // if not before time not found log and continue validation
        if (notBeforeTime == null) {
            log.debug("Not Before Time(nbf) not found in JWT. Continuing Validation");
        } else {
            long notBeforeTimeMillis = notBeforeTime.getTime();

            if (log.isDebugEnabled()) {
                log.debug("Not Before Time(nbf) found in JWT: " + notBeforeTime);
                log.debug("Not Before Time(nbf) found in JWT ms: " + notBeforeTimeMillis);
            }
            // validate whether current time is not before the nbf value in JWT
            if (currentTimeInMillis + timeStampSkewMillis < notBeforeTimeMillis) {
                if (log.isDebugEnabled()) {
                    log.debug("JSON Web Token : " + signedJWT.getPayload().toJSONObject().toString() +
                            " is used before Not Before Time." +
                            ", Not Before Time(ms) : " + notBeforeTimeMillis +
                            ", TimeStamp Skew : " + timeStampSkewMillis +
                            ", Current Time : " + currentTimeInMillis);
                }
                log.debug("JWT Rejected and validation terminated");
                return false;
            } else {
                log.debug("Not Before Time(nbf) of JWT was validated successfully");
            }
        }


        /**
         * The JWT MAY contain an iat (issued at) claim that identifies the time at which the JWT was
         * issued. Note that the authorization server may reject JWTs with an iat claim value that is
         * unreasonably far in the past
         */
        Date issuedAtTime = claimsSet.getIssueTime();

        // if issued at time is not found log and continue validation
        if (issuedAtTime == null) {
            log.debug("Issued At Time(iat) not found in JWT. Continuing Validation");
        } else {
            long issuedAtTimeMillis = issuedAtTime.getTime();
            // allowed time limit a token issued before can be used
            long rejectBeforeMillis = REJECT_TOKEN_ISSUED_BEFORE * 60 * 1000;


            if (log.isDebugEnabled()) {
                log.debug("Issued At Time(iat)found in JWT: " + issuedAtTime);
                log.debug("Issued At Time(iat) found in JWT: " + issuedAtTimeMillis + " ms");
                log.debug("Allowed Time Before for a JWT ms: " + rejectBeforeMillis + " ms");
            }

            // validate whether the issued time of the JWT is within allowed threshold
            if (currentTimeInMillis + timeStampSkewMillis - issuedAtTimeMillis >
                    rejectBeforeMillis) {
                if (log.isDebugEnabled()) {
                    log.debug("JSON Web Token : " + signedJWT.getPayload().toJSONObject().toString() +
                            " is issued before the allowed time." +
                            ", Issued At Time(ms) : " + issuedAtTimeMillis +
                            ", Reject before limit(ms) : " + rejectBeforeMillis +
                            ", TimeStamp Skew : " + timeStampSkewMillis +
                            ", Current Time : " + currentTimeInMillis);
                }
                // reject the token for further processing
                log.debug("JWT Rejected and validation terminated");
                return false;
            } else {
                log.debug("Issued At Time(iat) of JWT was validated successfully");
            }

        }

        /**
         * The JWT MAY contain a jti (JWT ID) claim that provides a unique identifier for the token. The
         * authorization server MAY ensure that JWTs are not replayed by maintaining the set of used
         * jti values for the length of time for which the JWT would be considered valid based on the
         * applicable exp instant
         */
        String jti = claimsSet.getJWTID();


        if (CACHE_USED_JTI && (jti != null)) {
            // check whether the JTI is already in the cache
            JWTCacheEntry entry = (JWTCacheEntry) jwtCache.getValueFromCache(jti);

            if (entry != null) {
                try {
                    // parse the cached assertion and get the Signed JWT
                    SignedJWT cachedJWT = entry.getJwt();
                    // validate that the same JWT ID has been sent again before the expiration time of the previous
                    // cached JWT
                    long cachedJWTExpiryTimeMillis = cachedJWT.getJWTClaimsSet().getExpirationTime().getTime();
                    if (currentTimeInMillis + timeStampSkewMillis > cachedJWTExpiryTimeMillis) {

                        if (log.isDebugEnabled()) {
                            log.debug("JWT Token \n" + signedJWT.getHeader().toJSONObject().toString() + "\n"
                                    + signedJWT.getPayload().toJSONObject().toString() + "\n"
                                    + "has been reused after the allowed expiry time : "
                                    + cachedJWT.getJWTClaimsSet().getExpirationTime());
                        }

                        // update the cache with the new JWT for the same JTI
                        this.jwtCache.addToCache(jti, new JWTCacheEntry(signedJWT));
                        log.debug("jti of the JWT has been validated successfully and cache updated");

                    } else {
                        log.error("JWT Token \n" + signedJWT.getHeader().toJSONObject().toString() + "\n"
                                + signedJWT.getPayload().toJSONObject().toString() + "\n" +
                                "Has been replayed before the allowed expiry time : "
                                + cachedJWT.getJWTClaimsSet().getExpirationTime());
                        return false;
                    }

                } catch (ParseException e) {
                    log.error("Unable to parse the cached jwt assertion : " + entry.getEncodedJWt(), e);
                    return false;
                }
            } else {
                if (log.isDebugEnabled()){
                    log.debug("JWT id: " + jti + " not found in the cache");
                    log.debug("jti of the JWT has been validated successfully");
                }
            }
        } else {
            if (!CACHE_USED_JTI) {
                log.debug("List of used JSON Web Token IDs are not maintained. Continuing Validation");
            }
            if (jti == null) {
                log.debug("JSON Web Token ID(jti) not found in JWT. Continuing Validation");
            }
        }


        /**
         *  The JWT MAY contain other claims
         */
        // check whether custom claims are available
        Map<String, Object> customClaims = claimsSet.getCustomClaims();
        if (customClaims == null) {
            log.debug("No custom claims found. Continue validating other claims.");
        } else {
            boolean customClaimsValidated = validateCustomClaims(claimsSet.getCustomClaims());
            if (!customClaimsValidated) {
                log.debug("Custom Claims in the JWT were not validated");
                return false;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("JWT Token \n" + signedJWT.getHeader().toJSONObject().toString() + "\n"
                    + signedJWT.getPayload().toJSONObject().toString() + "\n" + "was validated successfully");
        }

        // add the JWT to the cache to prevent replay before allowed expiry time
        jwtCache.addToCache(jti, new JWTCacheEntry(signedJWT));

        if (log.isDebugEnabled()) {
            log.debug("JWT Token: \n" + signedJWT.serialize() + "\n" +
                    signedJWT.getPayload().toJSONObject().toString() + "\n" + "was added to the cache successfully");
        }
        return true;
    }

    /**
     * This method is for testing to create a dummy IDP matching the issuer
     * value of the JWT token
     */
    private IdentityProvider createDummyIDP() {
        IdentityProvider idp = new IdentityProvider();
        idp.setIdentityProviderName("LOCAL");
        return idp;
    }

    /**
     * @param signedJWT the signedJWT to be logged
     */
    private void logJWT(SignedJWT signedJWT) {

    /*
     * Signed JWT has three parts
      * Part1 --> JWT header contains type and algorithm used to sign etc.
     *  Part2 --> JWT Payload containing the claims
     *  Part3 --> Signature to be verified
    */
        log.debug("JWT Header: " + signedJWT.getHeader().toJSONObject().toString());
        log.debug("JWT Payload: " + signedJWT.getPayload().toJSONObject().toString());
        log.debug("Signature: " + signedJWT.getSignature().toString());

    }


    /**
     *  Method to validate the signature of the JWT
     * @param signedJWT signed JWT whose signature is to be verified
     * @param idp Identity provider who issued the signed JWT
     * @return whether signature is valid, true if valid else false
     * @throws CertificateException
     * @throws JOSEException
     * @throws IdentityOAuth2Exception
     */
    private boolean validateSignature(SignedJWT signedJWT, IdentityProvider idp)
            throws JOSEException, IdentityOAuth2Exception {

        JWSVerifier verifier = null;
        X509Certificate x509Certificate;

        try {
            x509Certificate = (X509Certificate) IdentityApplicationManagementUtil
                    .decodeCertificate(idp.getCertificate());
        } catch (CertificateException e) {
            log.error(e.getMessage(), e);
            throw new IdentityOAuth2Exception("Error occurred while decoding public certificate of Identity Provider "
                    + idp.getIdentityProviderName() + " for tenant domain " + tenantDomain);
        }

        String alg = signedJWT.getHeader().getAlgorithm().getName();
        log.debug("Signature Algorithm found in the JWT Header: " + alg);

        // check whether signature is RSA
        if (alg.indexOf("RS") == 0) {
            RSAPublicKey publicKey = (RSAPublicKey) x509Certificate.getPublicKey();
            verifier = new RSASSAVerifier(publicKey);
        } else if (alg.indexOf("ES") == 0) {
            ECDSAPublicKey publicKey = (ECDSAPublicKey) x509Certificate.getPublicKey();
            // TODO support ECDSA signature verification
            //   verifier = new ECDSAVerifier(publicKey.getFirstCoefA(),publicKey.getSecondCoefB());
        } else {
            log.debug("Signature Algorithm not supported yet : " + alg);
        }

        // we were unable to find a supported signature type
        if (verifier == null) {
            log.error("Could create a signature verifier for algorithm type: "+alg);
            return false;
        }

        // return the result of signature verification
        return signedJWT.verify(verifier);
    }

    /**
     * Method to validate the claims other than
     * iss - Issuer
     * sub - Subject
     * aud - Audience
     * exp - Expiration Time
     * nbf - Not Before
     * iat - Issued At
     * jti - JWT ID
     * typ - Type
     * <p/>
     * in order to write your own way of validation and use the JWT grant handler,
     * you can extend this class and override this method
     *
     * @param customClaims a map of custom claims
     * @return whether the token is valid based on other claim values
     */
    protected boolean validateCustomClaims(Map<String, Object> customClaims) {
        return true;
    }

}

