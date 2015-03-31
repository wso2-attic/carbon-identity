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

package org.wso2.carbon.identity.oauth2.authcontext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.oauth.cache.CacheKey;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth.internal.OAuthComponentServiceHolder;
import org.wso2.carbon.identity.oauth.util.ClaimCache;
import org.wso2.carbon.identity.oauth.util.ClaimCacheKey;
import org.wso2.carbon.identity.oauth.util.UserClaims;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.validators.OAuth2TokenValidationMessageContext;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents the JSON Web Token generator.
 * By default the following properties are encoded to each authenticated API request:
 * subscriber, applicationName, apiContext, version, tier, and endUserName
 * Additional properties can be encoded by engaging the ClaimsRetrieverImplClass callback-handler.
 * The JWT header and body are base64 encoded separately and concatenated with a dot.
 * Finally the token is signed using SHA256 with RSA algorithm.
 */
public class JWTTokenGenerator implements AuthorizationContextTokenGenerator {

    private static final Log log = LogFactory.getLog(JWTTokenGenerator.class);

    private static final String API_GATEWAY_ID = "http://wso2.org/gateway";

    private static final String SHA256_WITH_RSA = "SHA256withRSA";

    private static final String NONE = "NONE";

    private static final Base64 base64Url = new Base64(0, null, true);

    private static volatile long ttl = -1L;

    private ClaimsRetriever claimsRetriever;

    private String signatureAlgorithm = SHA256_WITH_RSA;

    private boolean includeClaims = true;

    private boolean enableSigning = true;

    private static ConcurrentHashMap<Integer, Key> privateKeys = new ConcurrentHashMap<Integer, Key>();
    private static ConcurrentHashMap<Integer, Certificate> publicCerts = new ConcurrentHashMap<Integer, Certificate>();

    private ClaimCache claimsLocalCache;

    public JWTTokenGenerator() {
        claimsLocalCache = ClaimCache.getInstance();
    }

    //constructor for testing purposes
    public JWTTokenGenerator(boolean includeClaims, boolean enableSigning) {
        this.includeClaims = includeClaims;
        this.enableSigning = enableSigning;
        signatureAlgorithm = NONE;
    }

    /**
     * Reads the ClaimsRetrieverImplClass from identity.xml ->
     * OAuth -> TokenGeneration -> ClaimsRetrieverImplClass.
     *
     * @throws IdentityOAuth2Exception
     */
    @Override
    public void init() throws IdentityOAuth2Exception {
        if (includeClaims && enableSigning) {
            String claimsRetrieverImplClass =
                    OAuthServerConfiguration.getInstance().getClaimsRetrieverImplClass();
            signatureAlgorithm =  OAuthServerConfiguration.getInstance().getSignatureAlgorithm();
            if(signatureAlgorithm == null || !(signatureAlgorithm.equals(NONE) || signatureAlgorithm.equals(SHA256_WITH_RSA))){
                signatureAlgorithm = SHA256_WITH_RSA;
            }
            if(claimsRetrieverImplClass != null){
                try{
                    claimsRetriever = (ClaimsRetriever)Class.forName(claimsRetrieverImplClass).newInstance();
                    claimsRetriever.init();
                } catch (ClassNotFoundException e){
                    log.error("Cannot find class: " + claimsRetrieverImplClass,e);
                } catch (InstantiationException e) {
                    log.error("Error instantiating " + claimsRetrieverImplClass);
                } catch (IllegalAccessException e) {
                    log.error("Illegal access to " + claimsRetrieverImplClass);
                } catch (IdentityOAuth2Exception e){
                    log.error("Error while initializing " + claimsRetrieverImplClass);
                }
            }
        }
    }

    /**
     * Method that generates the JWT.
     *
     * @return signed JWT token
     * @throws IdentityOAuth2Exception
     */
    @Override
    public void generateToken(OAuth2TokenValidationMessageContext messageContext) throws IdentityOAuth2Exception {

        String clientId = ((AccessTokenDO)messageContext.getProperty("AccessTokenDO")).getConsumerKey();

        OAuthAppDAO appDAO =  new OAuthAppDAO();
        OAuthAppDO appDO;
        try {
            appDO = appDAO.getAppInformation(clientId);
            // Adding the OAuthAppDO as a context property for further use
            messageContext.addProperty("OAuthAppDO", appDO);
        } catch (IdentityOAuth2Exception e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        } catch (InvalidOAuthClientException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
        String subscriber = appDO.getUserName();
        String applicationName = appDO.getApplicationName();
        String authzUser = messageContext.getResponseDTO().getAuthorizedUser();

        //generating expiring timestamp
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long expireIn = currentTime + 1000 * 60 * getTTL();

        String jwtBody;

        //Sample JWT body
        //{"iss":"wso2.org/gateway","exp":1349267862304,"http://wso2.org/claims/subscriber":"johann",
        // "http://wso2.org/claims/applicationname":"App1","http://wso2.org/claims/enduser":"asela"}

        StringBuilder jwtBuilder = new StringBuilder();
        jwtBuilder.append("{");
        jwtBuilder.append("\"iss\":\"");
        jwtBuilder.append(API_GATEWAY_ID);
        jwtBuilder.append("\",");

        jwtBuilder.append("\"exp\":");
        jwtBuilder.append(String.valueOf(expireIn));
        jwtBuilder.append(",");

        jwtBuilder.append("\"");
        jwtBuilder.append(API_GATEWAY_ID);
        jwtBuilder.append("/subscriber\":\"");
        jwtBuilder.append(subscriber);
        jwtBuilder.append("\",");

        jwtBuilder.append("\"");
        jwtBuilder.append(API_GATEWAY_ID);
        jwtBuilder.append("/applicationname\":\"");
        jwtBuilder.append(applicationName);
        jwtBuilder.append("\",");

        jwtBuilder.append("\"");
        jwtBuilder.append(API_GATEWAY_ID);
        jwtBuilder.append("/enduser\":\"");
        jwtBuilder.append(authzUser);
        jwtBuilder.append("\"");

        if(claimsRetriever != null){

            //check in local cache
            String[] requestedClaims = messageContext.getRequestDTO().getRequiredClaimURIs();
            if(requestedClaims == null)  {
                // if no claims were requested, return all
                requestedClaims = claimsRetriever.getDefaultClaims(authzUser);
            }
            CacheKey cacheKey = new ClaimCacheKey(authzUser, requestedClaims);
            Object result = claimsLocalCache.getValueFromCache(cacheKey);

            SortedMap<String,String> claimValues = null;
            if (result != null) {
                claimValues = ((UserClaims) result).getClaimValues();
            } else {
                claimValues = claimsRetriever.getClaims(authzUser, requestedClaims);
                UserClaims userClaims = new UserClaims(claimValues);
                claimsLocalCache.addToCache(cacheKey, userClaims);
            }


            Iterator<String> it = new TreeSet(claimValues.keySet()).iterator();
            while(it.hasNext()){
                String claimURI = it.next();
                jwtBuilder.append(", \"");
                jwtBuilder.append(claimURI);
                jwtBuilder.append("\":\"");
                jwtBuilder.append(claimValues.get(claimURI));
                jwtBuilder.append("\"");
            }
        }

        jwtBuilder.append("}");
        jwtBody = jwtBuilder.toString();

        String jwtHeader = null;

        //if signature algo==NONE, header with "alg":"none"
        if(signatureAlgorithm.equals(NONE)){
            jwtHeader = "{\"typ\":\"JWT\",\"alg\":\"none\"}";
        } else if (signatureAlgorithm.equals(SHA256_WITH_RSA)){
            jwtHeader = addCertToHeader(authzUser);
        }

        String base64EncodedHeader = new String(base64Url.encode(jwtHeader.getBytes()));
        String base64EncodedBody = new String(base64Url.encode(jwtBody.getBytes()));
        OAuth2TokenValidationResponseDTO.AuthorizationContextToken token;
        if(signatureAlgorithm.equals(SHA256_WITH_RSA)){
            String assertion = base64EncodedHeader + "." + base64EncodedBody;

            //get the assertion signed
            byte[] signedAssertion = signJWT(assertion, authzUser);

            if (log.isDebugEnabled()) {
                log.debug("Signed assertion value : " + new String(signedAssertion));
            }
            String base64EncodedAssertion = new String(base64Url.encode(signedAssertion));

            token = messageContext.getResponseDTO().new AuthorizationContextToken(
                    "JWT", base64EncodedHeader + "." + base64EncodedBody + "." + base64EncodedAssertion);
            messageContext.getResponseDTO().setAuthorizationContextToken(token);
        } else {
            token = messageContext.getResponseDTO().new AuthorizationContextToken(
                    "JWT", base64EncodedHeader + "." + base64EncodedBody + ".");
            messageContext.getResponseDTO().setAuthorizationContextToken(token);
        }
    }

    /**
     * Helper method to sign the JWT
     *
     * @param assertion
     * @param endUserName
     * @return signed assertion
     * @throws IdentityOAuth2Exception
     */
    private byte[] signJWT(String assertion, String endUserName)
            throws IdentityOAuth2Exception {

        try {
            //get tenant domain
            String tenantDomain = MultitenantUtils.getTenantDomain(endUserName);
            //get tenantId
            int tenantId = getTenantId(endUserName);

            Key privateKey = null;

            if (!(privateKeys.containsKey(tenantId))) {
                //get tenant's key store manager
                KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);

                if(!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
                    //derive key store name
                    String ksName = tenantDomain.trim().replace(".", "-");
                    String jksName = ksName + ".jks";
                    //obtain private key
                    //TODO: maintain a hash map with tenants' private keys after first initialization
                    privateKey = tenantKSM.getPrivateKey(jksName, tenantDomain);
                }else{
                    try{
                        privateKey = tenantKSM.getDefaultPrivateKey();
                    }catch (Exception e){
                        log.error("Error while obtaining private key for super tenant",e);
                    }
                }
                if (privateKey != null) {
                    privateKeys.put(tenantId, privateKey);
                }
            } else {
                privateKey = privateKeys.get(tenantId);
            }

            //initialize signature with private key and algorithm
            Signature signature = Signature.getInstance(signatureAlgorithm);
            signature.initSign((PrivateKey) privateKey);

            //update signature with data to be signed
            byte[] dataInBytes = assertion.getBytes();
            signature.update(dataInBytes);

            //sign the assertion and return the signature
            byte[] signedInfo = signature.sign();
            return signedInfo;

        } catch (NoSuchAlgorithmException e) {
            String error = "Signature algorithm not found.";
            //do not log
            throw new IdentityOAuth2Exception(error);
        } catch (InvalidKeyException e) {
            String error = "Invalid private key provided for the signature";
            //do not log
            throw new IdentityOAuth2Exception(error);
        } catch (SignatureException e) {
            String error = "Error in signature";
            //do not log
            throw new IdentityOAuth2Exception(error);
        } catch (IdentityOAuth2Exception e) {
            //do not log
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    /**
     * Helper method to add public certificate to JWT_HEADER to signature verification.
     *
     * @param endUserName
     * @throws IdentityOAuth2Exception
     */
    private String addCertToHeader(String endUserName) throws IdentityOAuth2Exception {

        try {
            //get tenant domain
            String tenantDomain = MultitenantUtils.getTenantDomain(endUserName);
            //get tenantId
            int tenantId = getTenantId(endUserName);
            Certificate publicCert = null;

            if (!(publicCerts.containsKey(tenantId))) {
                //get tenant's key store manager
                KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);

                KeyStore keyStore = null;
                if(!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
                    //derive key store name
                    String ksName = tenantDomain.trim().replace(".", "-");
                    String jksName = ksName + ".jks";
                    keyStore = tenantKSM.getKeyStore(jksName);
                    publicCert = keyStore.getCertificate(tenantDomain);
                }else{
                    publicCert = tenantKSM.getDefaultPrimaryCertificate();
                }
                if (publicCert != null) {
                    publicCerts.put(tenantId, publicCert);
                }
            } else {
                publicCert = publicCerts.get(tenantId);
            }

            //generate the SHA-1 thumbprint of the certificate
            //TODO: maintain a hashmap with tenants' pubkey thumbprints after first initialization
            MessageDigest digestValue = MessageDigest.getInstance("SHA-1");
            byte[] der = publicCert.getEncoded();
            digestValue.update(der);
            byte[] digestInBytes = digestValue.digest();

            String publicCertThumbprint = hexify(digestInBytes);
            String base64EncodedThumbPrint = new String(base64Url.encode(publicCertThumbprint.getBytes()));

            StringBuilder jwtHeader = new StringBuilder();

            //Sample header
            //{"typ":"JWT", "alg":"SHA256withRSA", "x5t":"NmJmOGUxMzZlYjM2ZDRhNTZlYTA1YzdhZTRiOWE0NWI2M2JmOTc1ZA=="}

            jwtHeader.append("{\"typ\":\"JWT\",");
            jwtHeader.append("\"alg\":\"");
            jwtHeader.append(signatureAlgorithm);
            jwtHeader.append("\",");

            jwtHeader.append("\"x5t\":\"");
            jwtHeader.append(base64EncodedThumbPrint);
            jwtHeader.append("\"");

            jwtHeader.append("}");
            return jwtHeader.toString();

        } catch (KeyStoreException e) {
            String error = "Error in obtaining tenant's keystore";
            throw new IdentityOAuth2Exception(error);
        } catch (CertificateEncodingException e) {
            String error = "Error in generating public cert thumbprint";
            throw new IdentityOAuth2Exception(error);
        } catch (NoSuchAlgorithmException e) {
            String error = "Error in generating public cert thumbprint";
            throw new IdentityOAuth2Exception(error);
        } catch (Exception e) {
            String error = "Error in obtaining tenant's keystore";
            throw new IdentityOAuth2Exception(error);
        }
    }

    private long getTTL() {
        if (ttl != -1) {
            return ttl;
        }

        synchronized (JWTTokenGenerator.class) {
            if (ttl != -1) {
                return ttl;
            }
            String ttlValue = OAuthServerConfiguration.getInstance().getAuthorizationContextTTL();
            if (ttlValue != null) {
                ttl = Long.parseLong(ttlValue);
            } else {
                ttl = 15L;
            }
            return ttl;
        }
    }

    /**
     * Helper method to get tenantId from userName
     *
     * @param userName
     * @return tenantId
     * @throws IdentityOAuth2Exception
     */
    static int getTenantId(String userName) throws IdentityOAuth2Exception {
        //get tenant domain from user name
        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        RealmService realmService = OAuthComponentServiceHolder.getRealmService();
        try {
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            return tenantId;
        } catch (UserStoreException e) {
            String error = "Error in obtaining tenantId from Domain";
            //do not log
            throw new IdentityOAuth2Exception(error);
        }
    }

    /**
     * Helper method to hexify a byte array.
     * TODO:need to verify the logic
     *
     * @param bytes
     * @return  hexadecimal representation
     */
    private String hexify(byte bytes[]) {

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }

        return buf.toString();
    }

}