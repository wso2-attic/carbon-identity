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

package org.wso2.carbon.identity.oauth2.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.cache.CacheEntry;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth.dao.OAuthConsumerDAO;
import org.wso2.carbon.identity.oauth.internal.OAuthComponentServiceHolder;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.ClientCredentialDO;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Utility methods for OAuth 2.0 implementation
 */
public class OAuth2Util {

    public static final String IMPLICIT = "implicit";
    private static Log log = LogFactory.getLog(OAuth2Util.class);
    private static boolean cacheEnabled = OAuthServerConfiguration.getInstance().isCacheEnabled();
    private static OAuthCache cache = OAuthCache.getInstance(OAuthServerConfiguration.getInstance().getOAuthCacheTimeout());
    private static long timestampSkew = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;
    private static ThreadLocal<Integer> clientTenatId = new ThreadLocal<>();

    private OAuth2Util(){

    }

    /**
     * @return
     */
    public static int getClientTenatId() {
        if (clientTenatId.get() == null) {
            return -1;
        }
        return clientTenatId.get().intValue();
    }

    /**
     * @param tenantId
     */
    public static void setClientTenatId(int tenantId) {
        Integer id = new Integer(tenantId);
        clientTenatId.set(id);
    }

    /**
     *
     */
    public static void clearClientTenantId() {
        clientTenatId.remove();
    }

    /**
     * Build a comma separated list of scopes passed as a String set by OLTU.
     *
     * @param scopes set of scopes
     * @return Comma separated list of scopes
     */
    public static String buildScopeString(String[] scopes) {
        if (scopes != null) {
            StringBuilder scopeString = new StringBuilder("");
            Arrays.sort(scopes);
            for (int i = 0; i < scopes.length; i++) {
                scopeString.append(scopes[i].trim());
                if (i != scopes.length - 1) {
                    scopeString.append(" ");
                }
            }
            return scopeString.toString();
        }
        return null;
    }

    /**
     * @param scopeStr
     * @return
     */
    public static String[] buildScopeArray(String scopeStr) {
        if (StringUtils.isNotBlank(scopeStr)) {
            scopeStr = scopeStr.trim();
            return scopeStr.split("\\s");
        }
        return new String[0];
    }

    /**
     * Authenticate the OAuth Consumer
     *
     * @param clientId             Consumer Key/Id
     * @param clientSecretProvided Consumer Secret issued during the time of registration
     * @return true, if the authentication is successful, false otherwise.
     * @throws IdentityOAuthAdminException Error when looking up the credentials from the database
     */
    public static boolean authenticateClient(String clientId, String clientSecretProvided)
            throws IdentityOAuthAdminException, IdentityOAuth2Exception, InvalidOAuthClientException {

        boolean cacheHit = false;
        String clientSecret = null;

        // Check the cache first.
        if (cacheEnabled) {
            CacheEntry cacheResult = cache.getValueFromCache(new OAuthCacheKey(clientId));
            if (cacheResult != null && cacheResult instanceof ClientCredentialDO) {
                // cache hit
                clientSecret = ((ClientCredentialDO) cacheResult).getClientSecret();
                cacheHit = true;
                if (log.isDebugEnabled()) {
                    log.debug("Client credentials were available in the cache for client id : " +
                            clientId);
                }
            }
        }
        // Cache miss
        if (clientSecret == null) {
            OAuthConsumerDAO oAuthConsumerDAO = new OAuthConsumerDAO();
            clientSecret = oAuthConsumerDAO.getOAuthConsumerSecret(clientId);
            if (log.isDebugEnabled()) {
                log.debug("Client credentials were fetched from the database.");
            }
        }

        if (clientSecret == null) {
            if (log.isDebugEnabled()) {
                log.debug("Provided Client ID : " + clientId + "is not valid.");
            }
            return false;
        }

        if (!clientSecret.equals(clientSecretProvided)) {
            if(StringUtils.isEmpty(clientSecretProvided) || StringUtils.isEmpty(clientSecretProvided.trim())) {
                OAuthAppDAO appDAO = new OAuthAppDAO();
                OAuthAppDO appDO = appDAO.getAppInformation(clientId);
                String grantTypesString = appDO.getGrantTypes();
                boolean isOnlyImplicit = true;
                if (StringUtils.isNotEmpty(grantTypesString) && StringUtils.isNotEmpty(grantTypesString.trim())) {
                    String[] grantTypes = grantTypesString.split(",");
                    for (String grantType : grantTypes) {
                        if (StringUtils.isNotBlank(grantType) && !IMPLICIT.equals(grantType.trim())) {
                            isOnlyImplicit = false;
                        }
                    }
                }
                if (isOnlyImplicit == true) {
                    if(log.isDebugEnabled()){
                        log.debug("Application " + appDO.getApplicationName() + " is registered only for Implicit " +
                                "grant type. Therefore providing client secret for token revocation is optional");
                    }
                } else {
                    return false;
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Provided the Client ID : " + clientId +
                            " and Client Secret do not match with the issued credentials.");
                    }
                return false;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Successfully authenticated the client with client id : " + clientId);
        }

        if (cacheEnabled && !cacheHit) {
            cache.addToCache(new OAuthCacheKey(clientId), new ClientCredentialDO(clientSecret));
            if (log.isDebugEnabled()) {
                log.debug("Client credentials were added to the cache for client id : " + clientId);
            }
        }

        return true;
    }

    /**
     * Authenticate the OAuth consumer and return the username of user which own the provided client id and client
     * secret.
     *
     * @param clientId             Consumer Key/Id
     * @param clientSecretProvided Consumer Secret issued during the time of registration
     * @return Username of the user which own client id and client secret if authentication is
     * successful. Empty string otherwise.
     * @throws IdentityOAuthAdminException Error when looking up the credentials from the database
     */
    public static String getAuthenticatedUsername(String clientId, String clientSecretProvided)
            throws IdentityOAuthAdminException, IdentityOAuth2Exception, InvalidOAuthClientException {

        boolean cacheHit = false;
        String username = null;
        boolean isUsernameCaseSensitive = IdentityUtil.isUserStoreInUsernameCaseSensitive(username);

        if (OAuth2Util.authenticateClient(clientId, clientSecretProvided)) {
            // check cache
            if (cacheEnabled) {
                CacheEntry cacheResult = cache.getValueFromCache(new OAuthCacheKey(clientId + ":" + username));
                if (cacheResult != null && cacheResult instanceof ClientCredentialDO) {
                    // Ugh. This is fugly. Have to have a generic way of caching a key:value pair
                    username = ((ClientCredentialDO) cacheResult).getClientSecret();
                    cacheHit = true;
                    if (log.isDebugEnabled()) {
                        log.debug("Username was available in the cache : " +
                                username);
                    }
                }
            }

            if (username == null) {
                // Cache miss
                OAuthConsumerDAO oAuthConsumerDAO = new OAuthConsumerDAO();
                username = oAuthConsumerDAO.getAuthenticatedUsername(clientId, clientSecretProvided);
                if (log.isDebugEnabled()) {
                    log.debug("Username fetch from the database");
                }
            }

            if (username != null && cacheEnabled && !cacheHit) {
                /**
                 * Using the same ClientCredentialDO to host username. Semantically wrong since ClientCredentialDo
                 * accept a client secret and we're storing a username in the secret variable. Do we have to make our
                 * own cache key and cache entry class every time we need to put something to it? Ideal solution is
                 * to have a generalized way of caching a key:value pair
                 */
                if (isUsernameCaseSensitive){
                    cache.addToCache(new OAuthCacheKey(clientId + ":" + username), new ClientCredentialDO(username));
                }else {
                    cache.addToCache(new OAuthCacheKey(clientId + ":" + username.toLowerCase()), new ClientCredentialDO(username));
                }
                if (log.isDebugEnabled()){
                    log.debug("Caching username : " + username);
                }

            }
        }
        return username;
    }

    /**
     * Build the cache key string when storing Authz Code info in cache
     *
     * @param clientId  Client Id representing the client
     * @param authzCode Authorization Code issued to the client
     * @return concatenated <code>String</code> of clientId:authzCode
     */
    public static String buildCacheKeyStringForAuthzCode(String clientId, String authzCode) {
        return clientId + ":" + authzCode;
    }

    public static AccessTokenDO validateAccessTokenDO(AccessTokenDO accessTokenDO) {

        long validityPeriodMillis = accessTokenDO.getValidityPeriodInMillis();
        long issuedTime = accessTokenDO.getIssuedTime().getTime();
        long currentTime = System.currentTimeMillis();

        //check the validity of cached OAuth2AccessToken Response
        long skew = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;
        if (issuedTime + validityPeriodMillis - (currentTime + skew) > 1000) {
            long refreshValidity = OAuthServerConfiguration.getInstance()
                    .getRefreshTokenValidityPeriodInSeconds() * 1000;
            if (issuedTime + refreshValidity - currentTime + skew > 1000) {
                //Set new validity period to response object
                accessTokenDO.setValidityPeriod((issuedTime + validityPeriodMillis - (currentTime + skew)) / 1000);
                accessTokenDO.setValidityPeriodInMillis(issuedTime + validityPeriodMillis - (currentTime + skew));
                //Set issued time period to response object
                accessTokenDO.setIssuedTime(new Timestamp(currentTime));
                return accessTokenDO;
            }
        }
        //returns null if cached OAuth2AccessToken response object is expired
        return null;
    }

    public static boolean checkAccessTokenPartitioningEnabled() {
        return OAuthServerConfiguration.getInstance().isAccessTokenPartitioningEnabled();
    }

    public static boolean checkUserNameAssertionEnabled() {
        return OAuthServerConfiguration.getInstance().isUserNameAssertionEnabled();
    }

    public static String getAccessTokenPartitioningDomains() {
        return OAuthServerConfiguration.getInstance().getAccessTokenPartitioningDomains();
    }

    public static Map<String, String> getAvailableUserStoreDomainMappings() throws
            IdentityOAuth2Exception {
        //TreeMap is used to ignore the case sensitivity of key. Because when user logged in, the case of the user name is ignored.
        Map<String, String> userStoreDomainMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        String domainsStr = getAccessTokenPartitioningDomains();
        if (domainsStr != null) {
            String[] userStoreDomainsArr = domainsStr.split(",");
            for (String userStoreDomains : userStoreDomainsArr) {
                String[] mapping = userStoreDomains.trim().split(":"); //A:foo.com , B:bar.com
                if (mapping.length < 2) {
                    throw new IdentityOAuth2Exception("Domain mapping has not defined correctly");
                }
                userStoreDomainMap.put(mapping[1].trim(), mapping[0].trim()); //key=domain & value=mapping
            }
        }
        return userStoreDomainMap;
    }

    public static String getUserStoreDomainFromUserId(String userId)
            throws IdentityOAuth2Exception {
        String userStore = null;
        if (userId != null) {
            String[] strArr = userId.split("/");
            if (strArr != null && strArr.length > 1) {
                userStore = strArr[0];
                Map<String, String> availableDomainMappings = getAvailableUserStoreDomainMappings();
                if (availableDomainMappings != null &&
                        availableDomainMappings.containsKey(userStore)) {
                    userStore = getAvailableUserStoreDomainMappings().get(userStore);
                }
            }
        }
        return userStore;
    }

    public static String getUserStoreDomainFromAccessToken(String apiKey)
            throws IdentityOAuth2Exception {
        String userStoreDomain = null;
        String userId;
        String decodedKey = new String(Base64.decodeBase64(apiKey.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
        String[] tmpArr = decodedKey.split(":");
        if (tmpArr != null) {
            userId = tmpArr[1];
            if (userId != null) {
                userStoreDomain = getUserStoreDomainFromUserId(userId);
            }
        }
        return userStoreDomain;
    }

    public static String getAccessTokenStoreTableFromUserId(String userId)
            throws IdentityOAuth2Exception {
        String accessTokenStoreTable = OAuthConstants.ACCESS_TOKEN_STORE_TABLE;
        String userStore;
        if (userId != null) {
            String[] strArr = userId.split("/");
            if (strArr != null && strArr.length > 1) {
                userStore = strArr[0];
                Map<String, String> availableDomainMappings = getAvailableUserStoreDomainMappings();
                if (availableDomainMappings != null &&
                        availableDomainMappings.containsKey(userStore)) {
                    accessTokenStoreTable = accessTokenStoreTable + "_" +
                            availableDomainMappings.get(userStore);
                }
            }
        }
        return accessTokenStoreTable;
    }

    public static String getAccessTokenStoreTableFromAccessToken(String apiKey)
            throws IdentityOAuth2Exception {
        String userId = getUserIdFromAccessToken(apiKey); //i.e: 'foo.com/admin' or 'admin'
        return getAccessTokenStoreTableFromUserId(userId);
    }

    public static String getUserIdFromAccessToken(String apiKey) {
        String userId = null;
        String decodedKey = new String(Base64.decodeBase64(apiKey.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
        String[] tmpArr = decodedKey.split(":");
        if (tmpArr != null) {
            userId = tmpArr[1];
        }
        return userId;
    }

    public static String getSafeText(String text) {
        if (text == null) {
            return text;
        }
        text = text.trim();
        if (text.indexOf('<') > -1) {
            text = text.replace("<", "&lt;");
        }
        if (text.indexOf('>') > -1) {
            text = text.replace(">", "&gt;");
        }
        return text;
    }

    public static long getTokenExpireTimeMillis(AccessTokenDO accessTokenDO) {

        if (accessTokenDO == null) {
            throw new IllegalArgumentException("accessTokenDO is " + "\'NULL\'");
        }

        long currentTime;
        long validityPeriodMillis = accessTokenDO.getValidityPeriodInMillis();

        if(validityPeriodMillis < 0){
            log.debug("Access Token : " + accessTokenDO.getAccessToken() + " has infinite lifetime");
            return -1;
        }

        long refreshTokenValidityPeriodMillis = accessTokenDO.getRefreshTokenValidityPeriodInMillis();
        long issuedTime = accessTokenDO.getIssuedTime().getTime();
        currentTime = System.currentTimeMillis();
        long refreshTokenIssuedTime = accessTokenDO.getRefreshTokenIssuedTime().getTime();
        long accessTokenValidity = issuedTime + validityPeriodMillis - (currentTime + timestampSkew);
        long refreshTokenValidity = (refreshTokenIssuedTime + refreshTokenValidityPeriodMillis)
                                    - (currentTime + timestampSkew);
        if(accessTokenValidity > 1000 && refreshTokenValidity > 1000){
            return accessTokenValidity;
        }
        return 0;
    }

    public static long getRefreshTokenExpireTimeMillis(AccessTokenDO accessTokenDO) {

        if (accessTokenDO == null) {
            throw new IllegalArgumentException("accessTokenDO is " + "\'NULL\'");
        }

        long currentTime;
        long refreshTokenValidityPeriodMillis = accessTokenDO.getRefreshTokenValidityPeriodInMillis();

        if (refreshTokenValidityPeriodMillis < 0) {
            log.debug("Refresh Token : " + accessTokenDO.getRefreshToken() + " has infinite lifetime");
            return -1;
        }

        currentTime = System.currentTimeMillis();
        long refreshTokenIssuedTime = accessTokenDO.getRefreshTokenIssuedTime().getTime();
        long refreshTokenValidity = (refreshTokenIssuedTime + refreshTokenValidityPeriodMillis)
                                    - (currentTime + timestampSkew);
        if(refreshTokenValidity > 1000){
            return refreshTokenValidity;
        }
        return 0;
    }

    public static long getAccessTokenExpireMillis(AccessTokenDO accessTokenDO) {

        if(accessTokenDO == null){
            throw new IllegalArgumentException("accessTokenDO is " + "\'NULL\'");
        }
        long currentTime;
        long validityPeriodMillis = accessTokenDO.getValidityPeriodInMillis();

        if (validityPeriodMillis < 0) {
            log.debug("Access Token : " + accessTokenDO.getAccessToken() + " has infinite lifetime");
            return -1;
        }

        long issuedTime = accessTokenDO.getIssuedTime().getTime();
        currentTime = System.currentTimeMillis();
        long validityMillis = issuedTime + validityPeriodMillis - (currentTime + timestampSkew);
        if (validityMillis > 1000) {
            return validityMillis;
        } else {
            return 0;
        }
    }

    public static int getTenantId(String tenantDomain) throws IdentityOAuth2Exception {
        RealmService realmService = OAuthComponentServiceHolder.getRealmService();
        try {
            return realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String error = "Error in obtaining tenant ID from tenant domain : " + tenantDomain;
            throw new IdentityOAuth2Exception(error, e);
        }
    }

    public static String getTenantDomain(int tenantId) throws IdentityOAuth2Exception {
        RealmService realmService = OAuthComponentServiceHolder.getRealmService();
        try {
            return realmService.getTenantManager().getDomain(tenantId);
        } catch (UserStoreException e) {
            String error = "Error in obtaining tenant domain from tenant ID : " + tenantId;
            throw new IdentityOAuth2Exception(error, e);
        }
    }

    public static int getTenantIdFromUserName(String username) throws IdentityOAuth2Exception {

        String domainName = MultitenantUtils.getTenantDomain(username);
        return getTenantId(domainName);
    }

    public static String hashScopes(String[] scope){
        if (scope.length > 0){
            return DigestUtils.md5Hex(OAuth2Util.buildScopeString(scope));
        } else {
            return null;
        }

    }

    public static String hashScopes(String scope){
        if (StringUtils.isNotBlank(scope)) {
            //first converted to an array to sort the scopes
            return DigestUtils.md5Hex(OAuth2Util.buildScopeString(buildScopeArray(scope)));
        } else {
           return null;
        }
    }

    public static User getUserFromUserName(String username) throws IllegalArgumentException{
        if (StringUtils.isNotBlank(username)) {
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(username);
            String tenantAwareUsernameWithNoUserDomain = UserCoreUtil.removeDomainFromName(tenantAwareUsername);
            String userStoreDomain = UserCoreUtil.extractDomainFromName(username).toUpperCase();
            User user = new User();
            user.setUserName(tenantAwareUsernameWithNoUserDomain);
            user.setTenantDomain(tenantDomain);
            user.setUserStoreDomain(userStoreDomain);

            return user;
        }
        throw  new IllegalArgumentException("Cannot create user from empty user name");
    }

    public static String getIDTokenIssuer() {
        String issuer = OAuthServerConfiguration.getInstance().getOpenIDConnectIDTokenIssuerIdentifier();
        if (StringUtils.isBlank(issuer)) {
            issuer = OAuthURL.getOAuth2TokenEPUrl();
        }
        return issuer;
    }

    public static class OAuthURL {

        public static String getOAuth1RequestTokenUrl() {
            String oauth1RequestTokenUrl = OAuthServerConfiguration.getInstance().getOAuth1RequestTokenUrl();
            if(StringUtils.isBlank(oauth1RequestTokenUrl)){
                oauth1RequestTokenUrl = IdentityUtil.getServerURL("oauth/request-token", false);
            }
            return oauth1RequestTokenUrl;
        }

        public static String getOAuth1AuthorizeUrl() {
            String oauth1AuthorizeUrl = OAuthServerConfiguration.getInstance().getOAuth1AuthorizeUrl();
            if(StringUtils.isBlank(oauth1AuthorizeUrl)){
                oauth1AuthorizeUrl = IdentityUtil.getServerURL("oauth/authorize-url", false);
            }
            return oauth1AuthorizeUrl;
        }

        public static String getOAuth1AccessTokenUrl() {
            String oauth1AccessTokenUrl = OAuthServerConfiguration.getInstance().getOAuth1AccessTokenUrl();
            if(StringUtils.isBlank(oauth1AccessTokenUrl)){
                oauth1AccessTokenUrl = IdentityUtil.getServerURL("oauth/access-token", false);
            }
            return oauth1AccessTokenUrl;
        }

        public static String getOAuth2AuthzEPUrl() {
            String oauth2AuthzEPUrl = OAuthServerConfiguration.getInstance().getOAuth2AuthzEPUrl();
            if(StringUtils.isBlank(oauth2AuthzEPUrl)){
                oauth2AuthzEPUrl = IdentityUtil.getServerURL("oauth2/authorize", false);
            }
            return oauth2AuthzEPUrl;
        }

        public static String getOAuth2TokenEPUrl() {
            String oauth2TokenEPUrl = OAuthServerConfiguration.getInstance().getOAuth2TokenEPUrl();
            if(StringUtils.isBlank(oauth2TokenEPUrl)){
                oauth2TokenEPUrl = IdentityUtil.getServerURL("oauth2/token", false);
            }
            return oauth2TokenEPUrl;
        }

        public static String getOAuth2UserInfoEPUrl() {
            String oauth2UserInfoEPUrl = OAuthServerConfiguration.getInstance().getOauth2UserInfoEPUrl();
            if(StringUtils.isBlank(oauth2UserInfoEPUrl)){
                oauth2UserInfoEPUrl = IdentityUtil.getServerURL("oauth2/userinfo", false);
            }
            return oauth2UserInfoEPUrl;
        }

        public static String getOIDCConsentPageUrl() {
            String OIDCConsentPageUrl = OAuthServerConfiguration.getInstance().getOIDCConsentPageUrl();
            if(StringUtils.isBlank(OIDCConsentPageUrl)){
                OIDCConsentPageUrl = IdentityUtil.getServerURL("/authenticationendpoint/oauth2_consent.do", false);
            }
            return OIDCConsentPageUrl;
        }

        public static String getOAuth2ConsentPageUrl() {
            String oAuth2ConsentPageUrl = OAuthServerConfiguration.getInstance().getOauth2ConsentPageUrl();
            if(StringUtils.isBlank(oAuth2ConsentPageUrl)){
                oAuth2ConsentPageUrl = IdentityUtil.getServerURL("/authenticationendpoint/oauth2_authz.do", false);
            }
            return oAuth2ConsentPageUrl;
        }
    }

    public static boolean isOIDCAuthzRequest(Set<String> scope) {
        return scope.contains(OAuthConstants.Scope.OPENID);
    }

    public static boolean isOIDCAuthzRequest(String[] scope) {
        for(String openidscope : scope) {
            if (openidscope.equals(OAuthConstants.Scope.OPENID)) {
                return true;
            }
        }
        return false;
    }
}
