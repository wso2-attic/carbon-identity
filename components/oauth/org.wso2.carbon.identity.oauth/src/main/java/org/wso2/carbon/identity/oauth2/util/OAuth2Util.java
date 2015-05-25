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

package org.wso2.carbon.identity.oauth2.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
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
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility methods for OAuth 2.0 implementation
 */
public class OAuth2Util {

    private static Log log = LogFactory.getLog(OAuth2Util.class);
    private static boolean cacheEnabled = OAuthServerConfiguration.getInstance().isCacheEnabled();
    private static OAuthCache cache = OAuthCache.getInstance();
    private static long timestampSkew = OAuthServerConfiguration.getInstance().getTimeStampSkewInSeconds() * 1000;
    private static ThreadLocal<Integer> clientTenatId = new ThreadLocal<Integer>();


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
     * Build a comma separated list of scopes passed as a String set by Amber.
     *
     * @param scopes set of scopes
     * @return Comma separated list of scopes
     */
    public static String buildScopeString(String[] scopes) {
        StringBuilder scopeString = new StringBuilder("");
        if (scopes != null) {
            Arrays.sort(scopes);
            for (int i = 0; i < scopes.length; i++) {
                scopeString.append(scopes[i].trim());
                if (i != scopes.length - 1) {
                    scopeString.append(" ");
                }
            }
        }
        return scopeString.toString();
    }

    /**
     * @param scopeStr
     * @return
     */
    public static String[] buildScopeArray(String scopeStr) {
        if (scopeStr != null) {
            scopeStr = scopeStr.trim();
            return scopeStr.split("\\s");
        }
        return null;
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
                        if (StringUtils.isNotEmpty(grantType) && StringUtils.isNotEmpty(grantType.trim())) {
                            if (!"implicit".equals(grantType.trim())) {
                                isOnlyImplicit = false;
                            }
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
                log.debug("Username fetch from the database");
            }

            if (username != null && cacheEnabled && !cacheHit) {
                /**
                 * Using the same ClientCredentialDO to host username. Semantically wrong since ClientCredentialDo
                 * accept a client secret and we're storing a username in the secret variable. Do we have to make our
                 * own cache key and cache entry class every time we need to put something to it? Ideal solution is
                 * to have a generalized way of caching a key:value pair
                 */
                cache.addToCache(new OAuthCacheKey(clientId + ":" + username), new ClientCredentialDO(username));
                log.debug("Caching username : " + username);
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

        //long validityPeriod = accessTokenDO.getValidityPeriod() * 1000;
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
        String decodedKey = new String(Base64.decodeBase64(apiKey.getBytes()));
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
        String decodedKey = new String(Base64.decodeBase64(apiKey.getBytes()));
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
        long issuedTime = accessTokenDO.getIssuedTime().getTime();
        currentTime = System.currentTimeMillis();
        long accessTokenValidity = issuedTime + validityPeriodMillis - (currentTime + timestampSkew);
        long refreshTokenValidity = (issuedTime + OAuthServerConfiguration.getInstance().
                getRefreshTokenValidityPeriodInSeconds() * 1000) - (currentTime + timestampSkew);
        if(accessTokenValidity > 1000 && refreshTokenValidity > 1000){
            return accessTokenValidity;
        }
        return 0;
    }

    public static long getAccessTokenExpireMillis(AccessTokenDO accessTokenDO) {

        if(accessTokenDO == null){
            throw new IllegalArgumentException("accessTokenDO is " + "\'NULL\'");
        }
        long currentTime;
        long validityPeriodMillis = accessTokenDO.getValidityPeriodInMillis();
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
            throw new IdentityOAuth2Exception(error);
        }
    }

    public static String getTenantDomain(int tenantId) throws IdentityOAuth2Exception {
        RealmService realmService = OAuthComponentServiceHolder.getRealmService();
        try {
            return realmService.getTenantManager().getDomain(tenantId);
        } catch (UserStoreException e) {
            String error = "Error in obtaining tenant domain from tenant ID : " + tenantId;
            throw new IdentityOAuth2Exception(error);
        }
    }

    public static int getTenantIdFromUserName(String username) throws IdentityOAuth2Exception {

        String domainName = MultitenantUtils.getTenantDomain(username);
        return getTenantId(domainName);
    }

}
