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

import net.minidev.json.JSONArray;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.openidconnect.as.messages.IDTokenBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.wso2.carbon.claim.mgt.ClaimManagementException;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCache;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheEntry;
import org.wso2.carbon.identity.oauth.cache.AuthorizationGrantCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.internal.OAuth2ServiceComponentHolder;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Returns the claims of the SAML assertion
 */
public class SAMLAssertionClaimsCallback implements CustomClaimsCallbackHandler {

    private final static Log log = LogFactory.getLog(SAMLAssertionClaimsCallback.class);
    private final static String INBOUND_AUTH2_TYPE = "oauth2";
    private final static String SP_DIALECT = "http://wso2.org/oidc/claim";

    private String userAttributeSeparator = IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR_DEFAULT;

    @Override
    public void handleCustomClaims(IDTokenBuilder builder, OAuthTokenReqMessageContext requestMsgCtx) {
        // reading the token set in the same grant
        Assertion assertion = (Assertion) requestMsgCtx.getProperty(OAuthConstants.OAUTH_SAML2_ASSERTION);

        if (assertion != null) {
            List<AttributeStatement> list = assertion.getAttributeStatements();
            if (CollectionUtils.isNotEmpty(list)) {
                Iterator<Attribute> attributeIterator = list.get(0).getAttributes()
                        .iterator();
                while (attributeIterator.hasNext()) {
                    Attribute attribute = attributeIterator.next();
                    String value = attribute.getAttributeValues().get(0).getDOM().getTextContent();
                    builder.setClaim(attribute.getName(), value);
                    if (log.isDebugEnabled()) {
                        log.debug("Attribute: " + attribute.getName() + ", Value: " + value);
                    }
                }
            } else {
                log.debug("No AttributeStatement found! ");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Adding claims for user " + requestMsgCtx.getAuthorizedUser() + " to id token.");
            }
            try {
                JSONArray values;
                Map<String, Object> claims = getResponse(requestMsgCtx);
                Object claimSeparator = claims.get(IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR);
                if (claimSeparator != null) {
                    String claimSeparatorString = (String) claimSeparator;
                    if(StringUtils.isNotBlank(claimSeparatorString)) {
                        userAttributeSeparator = (String) claimSeparator;
                    }
                    claims.remove(IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR);
                }

                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    String value = entry.getValue().toString();
                    values = new JSONArray();
                    if (userAttributeSeparator != null && value.contains(userAttributeSeparator)) {
                        StringTokenizer st = new StringTokenizer(value, userAttributeSeparator);
                        while (st.hasMoreElements()) {
                            String attributeValue = st.nextElement().toString();
                            if (StringUtils.isNotBlank(attributeValue)) {
                                values.add(attributeValue);
                            }
                        }
                    } else {
                        values.add(value);
                    }
                    builder.setClaim(entry.getKey(), values.toJSONString());
                }
            } catch (OAuthSystemException e) {
                log.error("Error occurred while adding claims of " + requestMsgCtx.getAuthorizedUser() +
                                " to id token.", e);
            }
        }
    }

    /**
     * Get response map
     *
     * @param requestMsgCtx Token request message context
     * @return Mapped claimed
     * @throws OAuthSystemException
     */
    private Map<String, Object> getResponse(OAuthTokenReqMessageContext requestMsgCtx)
            throws OAuthSystemException {

        Map<ClaimMapping, String> userAttributes =
                getUserAttributesFromCache(requestMsgCtx.getProperty(OAuthConstants.ACCESS_TOKEN).toString());
        Map<String, Object> claims = Collections.emptyMap();

        // If subject claim uri is null, we get the actual user name of the logged in user.
        if (MapUtils.isEmpty(userAttributes) && (getSubjectClaimUri(requestMsgCtx) == null)) {
            if (log.isDebugEnabled()) {
                log.debug("User attributes not found in cache. Trying to retrieve attribute for user " + requestMsgCtx
                        .getAuthorizedUser());
            }
            try {
                claims = getClaimsFromUserStore(requestMsgCtx);
            } catch (UserStoreException | IdentityException | ClaimManagementException |
                    IdentityApplicationManagementException e) {
                log.error("Error occurred while getting claims for user " + requestMsgCtx.getAuthorizedUser(), e);
            }
        } else {
            claims = getClaimsMap(userAttributes);
        }
        return claims;
    }

    /**
     * Get claims map
     *
     * @param userAttributes User Attributes
     * @return User attribute map
     */
    private Map<String, Object> getClaimsMap(Map<ClaimMapping, String> userAttributes) {

        Map<String, Object> claims = new HashMap();
        if (MapUtils.isNotEmpty(userAttributes)) {
            for (Map.Entry<ClaimMapping, String> entry : userAttributes.entrySet()) {
                claims.put(entry.getKey().getRemoteClaim().getClaimUri(), entry.getValue());
            }
        }
        return claims;
    }

    /**
     * Get claims from user store
     *
     * @param requestMsgCtx Token request message context
     * @return Users claim map
     * @throws Exception
     */
    private static Map<String, Object> getClaimsFromUserStore(OAuthTokenReqMessageContext requestMsgCtx)
            throws IdentityApplicationManagementException, IdentityException, UserStoreException,
            ClaimManagementException {

        String username = requestMsgCtx.getAuthorizedUser().toString();
        String tenantDomain = requestMsgCtx.getAuthorizedUser().getTenantDomain();

        UserRealm realm;
        List<String> claimURIList = new ArrayList<String>();
        Map<String, Object> mappedAppClaims = new HashMap<String, Object>();

        ApplicationManagementService applicationMgtService = OAuth2ServiceComponentHolder.getApplicationMgtService();
        String spName = applicationMgtService
                .getServiceProviderNameByClientId(requestMsgCtx.getOauth2AccessTokenReqDTO().getClientId(),
                                                  INBOUND_AUTH2_TYPE, tenantDomain);
        ServiceProvider serviceProvider = applicationMgtService.getApplicationExcludingFileBasedSPs(spName,
                                                                                                    tenantDomain);
        if (serviceProvider == null) {
            return mappedAppClaims;
        }

        realm = IdentityTenantUtil.getRealm(tenantDomain, username);
        if (realm == null) {
            log.warn("No valid tenant domain provider. Empty claim returned back for tenant " + tenantDomain
                     + " and user " + username);
            return new HashMap<>();
        }

        Map<String, String> spToLocalClaimMappings;
        UserStoreManager userStoreManager = realm.getUserStoreManager();
        ClaimMapping[] requestedLocalClaimMap = serviceProvider.getClaimConfig().getClaimMappings();

        if (requestedLocalClaimMap != null && requestedLocalClaimMap.length > 0) {

            for (ClaimMapping mapping : requestedLocalClaimMap) {
                if (mapping.isRequested()) {
                    claimURIList.add(mapping.getLocalClaim().getClaimUri());
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Requested number of local claims: " + claimURIList.size());
            }

            spToLocalClaimMappings = ClaimManagerHandler.getInstance().getMappingsMapFromOtherDialectToCarbon(
                    SP_DIALECT, null, tenantDomain, false);

            Map<String, String> userClaims = userStoreManager.getUserClaimValues(
                    MultitenantUtils.getTenantAwareUsername(username),
                    claimURIList.toArray(new String[claimURIList.size()]), null);

            if (log.isDebugEnabled()) {
                log.debug("Number of user claims retrieved from user store: " + userClaims.size());
            }

            if (MapUtils.isEmpty(userClaims)) {
                return new HashMap<>();
            }

            for (Iterator<Map.Entry<String, String>> iterator = spToLocalClaimMappings.entrySet().iterator(); iterator
                    .hasNext(); ) {
                Map.Entry<String, String> entry = iterator.next();
                String value = userClaims.get(entry.getValue());
                if (value != null) {
                    mappedAppClaims.put(entry.getKey(), value);
                    if (log.isDebugEnabled()) {
                        log.debug("Mapped claim: key -  " + entry.getKey() + " value -" + value);
                    }
                }
            }

            String domain = UserCoreUtil.extractDomainFromName(username);
            RealmConfiguration realmConfiguration = userStoreManager.getSecondaryUserStoreManager(domain)
                    .getRealmConfiguration();

            String claimSeparator = realmConfiguration.getUserStoreProperty(
                    IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR);
            if (StringUtils.isNotBlank(claimSeparator)) {
                mappedAppClaims.put(IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR, claimSeparator);
            }
        }
        return mappedAppClaims;
    }

    /**
     * Get user attribute from cache
     *
     * @param accessToken Access token
     * @return User attributes
     */
    private Map<ClaimMapping, String> getUserAttributesFromCache(String accessToken) {

        AuthorizationGrantCacheKey cacheKey = new AuthorizationGrantCacheKey(accessToken);
        AuthorizationGrantCacheEntry cacheEntry = (AuthorizationGrantCacheEntry) AuthorizationGrantCache.
                getInstance(OAuthServerConfiguration.getInstance().getAuthorizationGrantCacheTimeout())
                                                                                        .getValueFromCache(cacheKey);
        if (cacheEntry == null) {
            return new HashMap<ClaimMapping, String>();
        }
        return cacheEntry.getUserAttributes();
    }

    private String getSubjectClaimUri(OAuthTokenReqMessageContext request) {
        ApplicationManagementService applicationMgtService = OAuth2ServiceComponentHolder
                .getApplicationMgtService();
        ServiceProvider serviceProvider = null;
        try {
            String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String spName = applicationMgtService.getServiceProviderNameByClientId(request.getOauth2AccessTokenReqDTO()
                                                                                           .getClientId(),
                                                                                   INBOUND_AUTH2_TYPE, tenantDomain);
            serviceProvider = applicationMgtService.getApplicationExcludingFileBasedSPs(spName, tenantDomain);
            if (serviceProvider != null) {
                return serviceProvider.getLocalAndOutBoundAuthenticationConfig().getSubjectClaimUri();
            }
        } catch (IdentityApplicationManagementException ex) {
            log.error("Error while getting service provider information.", ex);
        }
        return null;
    }

}
