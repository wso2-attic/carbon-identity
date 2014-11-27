/*
 *  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.handler.claims.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.claims.ClaimHandler;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.mgt.ApplicationConstants;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class DefaultClaimHandler implements ClaimHandler {

    private static Log log = LogFactory.getLog(DefaultClaimHandler.class);
    private static volatile DefaultClaimHandler instance;

    public static DefaultClaimHandler getInstance() {
        if (instance == null) {
            synchronized (DefaultClaimHandler.class) {
                if (instance == null) {
                    instance = new DefaultClaimHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 
     */
    public Map<String, String> handleClaimMappings(StepConfig stepConfig,
            AuthenticationContext context, Map<String, String> remoteAttributes,
            boolean isFederatedClaims) throws FrameworkException {

        if (isFederatedClaims) {

            String requestingClientType = context.getRequestType();
            ExternalIdPConfig externalIdPConfig = context.getExternalIdP();

            ApplicationConfig appConfig = context.getSequenceConfig().getApplicationConfig();
            String remoteIdpClaimDialect = stepConfig.getAuthenticatedAutenticator()
                    .getApplicationAuthenticator().getClaimDialectURI();

            String spDialect = getDialectUri(requestingClientType,
                    appConfig.getRequestedClaimMappings() != null
                            && appConfig.getRequestedClaimMappings().size() > 0);

            return handleFederatedClaims(spDialect, appConfig.getClaimMappings(),
                    remoteIdpClaimDialect, externalIdPConfig.getClaimMappings(), remoteAttributes,
                    externalIdPConfig.useDefaultLocalIdpDialect(),
                    appConfig.getRequestedClaimMappings(), context.getTenantDomain(), context);
        } else {

            String authenticatedUser = null;
            
            if (stepConfig != null) { 
                //calling from StepBasedSequenceHandler
                authenticatedUser = stepConfig.getAuthenticatedUser();
            } else { 
                //calling from RequestPathBasedSequenceHandler
                authenticatedUser = context.getSequenceConfig().getAuthenticatedUser();
            }
            
            return handleLocalClaims(authenticatedUser, context);
        }
    }

    /**
     * 
     * @param allAttributes
     * @param requestedClaimMappings
     * @return
     */
    private Map<String, String> getFilteredAttributes(Map<String, String> allAttributes,
            Map<String, String> requestedClaimMappings, boolean isStandardDialect) {

        boolean hasClaimsRequested = false;

        if (requestedClaimMappings != null && requestedClaimMappings.size() > 0) {
            hasClaimsRequested = true;
        }

        Map<String, String> filteredAttributes = new HashMap<String, String>();

        for (Iterator<Entry<String, String>> iterator = allAttributes.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<String, String> entry = iterator.next();
            if (!isStandardDialect && entry.getKey() != null && hasClaimsRequested
                    && requestedClaimMappings.containsKey((entry.getKey()))) {
                filteredAttributes.put(entry.getKey(), entry.getValue());
            } else {
                filteredAttributes.put(entry.getKey(), entry.getValue());
            }
        }

        return filteredAttributes;

    }

    /**
     * 
     * @param spDialect
     * @param spClaimMapping
     * @param remoteIdpClaimDialect
     * @param remoteIdpClaimMapping
     * @param remoteAttributes
     * @param useDefaultLocalIdpDialect
     * @param spRequestedClaimMappings
     * @param tenantName
     * @param context
     * @return
     * @throws FrameworkException
     */
    protected Map<String, String> handleFederatedClaims(String spDialect,
            Map<String, String> spClaimMapping, String remoteIdpClaimDialect,
            ClaimMapping[] remoteIdpClaimMapping, Map<String, String> remoteAttributes,
            boolean useDefaultLocalIdpDialect, Map<String, String> spRequestedClaimMappings,
            String tenantName, AuthenticationContext context) throws FrameworkException {

        Map<String, String> mappedAppClaims = new HashMap<String, String>();

        try {

            if (remoteAttributes == null || remoteAttributes.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No attributes given. Returning");
                }
                return null;
            }

            if (remoteIdpClaimDialect != null && spDialect != null
                    && spDialect.equals(remoteIdpClaimDialect)) {
                log.debug("Federated IDP and SP are using common dialect. NO claim mapping required. Continuing!");

                // now we need to service provider requested attributes out of this.

                return getFilteredAttributes(remoteAttributes, spRequestedClaimMappings,
                        spDialect != null);
            }

            // claim mapping from external IDP to local IDP

            // local-claim-uri / idp-claim-uri
            Map<String, String> localToIdpClaimMap = null;

            if (remoteIdpClaimDialect == null && useDefaultLocalIdpDialect) {
                remoteIdpClaimDialect = ApplicationConstants.LOCAL_IDP_DEFAULT_CLAIM_DIALECT;
            }

            if (remoteIdpClaimDialect != null) {
                localToIdpClaimMap = getClaimMappings(remoteIdpClaimDialect,
                        remoteAttributes.keySet(), tenantName, true);
            } else if (remoteIdpClaimMapping != null && remoteIdpClaimMapping.length > 0) {
                localToIdpClaimMap = FrameworkUtils.getClaimMappings(remoteIdpClaimMapping, false);
            } else {
                // we do not know how to map remote idp claims to local claims.
                return remoteAttributes;

            }

            // claim mapping from local service provider to remote service provider.
            Map<String, String> spToLocalClaimMap = null;

            if (spDialect == null && (spClaimMapping == null || spClaimMapping.size() == 0)) {
                spDialect = ApplicationConstants.LOCAL_IDP_DEFAULT_CLAIM_DIALECT;
            }

            if (spDialect != null) {
                spToLocalClaimMap = getClaimMappings(spDialect, null, tenantName, false);
            } else if (spClaimMapping != null && spClaimMapping.size() > 0) {
                spToLocalClaimMap = spClaimMapping;
            } else {
                // we do not know how to map remote service provider claims to local claims.
                return remoteAttributes;

            }

            Map<String, String> unfilteredLocalClaimValues = new HashMap<String, String>();

            for (Iterator<Entry<String, String>> iterator = spToLocalClaimMap.entrySet().iterator(); iterator
                    .hasNext();) {
                Entry<String, String> entry = iterator.next();
                if (entry.getValue() != null) {
                    String idpClaimUri = localToIdpClaimMap.get(entry.getValue());
                    if (idpClaimUri != null) {
                        String claimValue = remoteAttributes.get(idpClaimUri);
                        if(claimValue != null){
                            mappedAppClaims.put(entry.getKey(), claimValue);
                            unfilteredLocalClaimValues.put(entry.getValue(), claimValue);
                        }
                    }
                }
            }

            // set as a property.
            context.setProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES,
                    unfilteredLocalClaimValues);

        } catch (Exception e) {
            throw new FrameworkException("Error while claim mapping", e);
        }

        return getFilteredAttributes(mappedAppClaims, spRequestedClaimMappings, spDialect != null);
    }

    /**
     * 
     * @param remoteIdpClaimDialect
     * @param keySet
     * @param tenantName
     * @param userLocalDialectAsKey
     * @return
     * @throws Exception
     */
    private Map<String, String> getClaimMappings(String remoteIdpClaimDialect, Set<String> keySet,
            String tenantName, boolean userLocalDialectAsKey) throws Exception {

        Map<String, String> claimMApping = ClaimManagerHandler.getInstance()
                .getMappingsMapFromOtherDialectToCarbon(remoteIdpClaimDialect, keySet, tenantName,
                        userLocalDialectAsKey);
        if (claimMApping == null) {
            claimMApping = new HashMap<String, String>();
        }

        return claimMApping;
    }

    /**
     * 
     * @param context
     * @return
     * @throws FrameworkException
     */
    protected Map<String, String> handleLocalClaims(String authenticatedUser,
            AuthenticationContext context) throws FrameworkException {

        try {

            String tenantDomain = MultitenantUtils.getTenantDomain(context.getSequenceConfig()
                    .getAuthenticatedUser());
            UserRealm realm = AnonymousSessionUtil.getRealmByTenantDomain(
                    FrameworkServiceComponent.getRegistryService(),
                    FrameworkServiceComponent.getRealmService(), tenantDomain);

            if (realm == null) {
                log.warn("No valid tenant domain provider. Empty claim returned back");
                return new HashMap<String, String>();
            }

            ClaimManager claimManager = realm.getClaimManager();

            String requestingClientType = context.getRequestType();
            ApplicationConfig appConfig = context.getSequenceConfig().getApplicationConfig();
            Map<String, String> spToLocalClaimMappings = appConfig.getClaimMappings();
            Map<String, String> requestedClaimMappings = appConfig.getRequestedClaimMappings();
            String spDialect = getDialectUri(requestingClientType, requestedClaimMappings != null
                    && requestedClaimMappings.size() > 0);

            UserStoreManager userstore = realm.getUserStoreManager();

            List<String> claimURIList = new ArrayList<String>();

            if (spDialect == null) {
                // if claim dialect is null then there must be a claim mapping.
                // there must be a requested claim list - otherwise ignore.
                if (requestedClaimMappings != null && requestedClaimMappings.size() > 0) {
                    for (Iterator<Entry<String, String>> iterator = requestedClaimMappings
                            .entrySet().iterator(); iterator.hasNext();) {
                        Entry<String, String> entry = iterator.next();
                        if (entry.getValue() != null) {
                            claimURIList.add(entry.getValue());
                        }

                    }
                }

            } else {
                if (ApplicationConstants.LOCAL_IDP_DEFAULT_CLAIM_DIALECT.equals(spDialect)
                        && requestedClaimMappings != null && requestedClaimMappings.size() > 0) {
                    for (Iterator<String> iterator = requestedClaimMappings.keySet().iterator(); iterator
                            .hasNext();) {
                        // in local idp dialect
                        claimURIList.add(iterator.next());
                    }
                } else if (FrameworkConstants.RequestType.CLAIM_TYPE_OPENID
                        .equals(requestingClientType)) {
                    org.wso2.carbon.user.api.ClaimMapping[] claimMappings = claimManager
                            .getAllClaimMappings(ApplicationConstants.LOCAL_IDP_DEFAULT_CLAIM_DIALECT);
                    for (org.wso2.carbon.user.api.ClaimMapping mapping : claimMappings) {
                        claimURIList.add(mapping.getClaim().getClaimUri());
                    }

                } else {
                    Map<String, String> requestedLocalClaimMap = null;

                    // need to get all the requested claims
                    requestedLocalClaimMap = ClaimManagerHandler.getInstance()
                            .getMappingsMapFromOtherDialectToCarbon(spDialect,
                                    requestedClaimMappings.keySet(), context.getTenantDomain(),
                                    true);
                    if (requestedLocalClaimMap != null && requestedLocalClaimMap.size() > 0) {
                        for (Iterator<String> iterator = requestedLocalClaimMap.keySet().iterator(); iterator
                                .hasNext();) {
                            // in local idp dialect
                            claimURIList.add(iterator.next());
                        }
                    }
                }

                spToLocalClaimMappings = ClaimManagerHandler.getInstance()
                        .getMappingsMapFromOtherDialectToCarbon(spDialect, null,
                                context.getTenantDomain(), false);

            }

            if (spToLocalClaimMappings == null || spToLocalClaimMappings.size() == 0
                    && spDialect == null) {
                return new HashMap<String, String>();
            }


            // user claim values against local claim uris.
            Map<String, String> userClaims = userstore.getUserClaimValues(
                    MultitenantUtils.getTenantAwareUsername(authenticatedUser),
                    claimURIList.toArray(new String[claimURIList.size()]), null);

            if(userClaims == null || userClaims.size() == 0){
                return new HashMap<String, String>();
            }


            HashMap<String, String> mappedAppClaims = new HashMap<String, String>();
            HashMap<String, String> filteredClaims = new HashMap<String, String>();

            for (Iterator<Entry<String, String>> iterator = spToLocalClaimMappings.entrySet()
                    .iterator(); iterator.hasNext();) {
                Entry<String, String> entry = iterator.next();
                String value = userClaims.get(entry.getValue());
                if (value != null) {
                    mappedAppClaims.put(entry.getKey(), value);
                    if (spDialect != null && requestedClaimMappings != null
                            && requestedClaimMappings.containsValue(entry.getValue())) {
                        filteredClaims.put(entry.getKey(), value);
                    }
                }
            }

            // set as a property.
            context.setProperty(FrameworkConstants.UNFILTERED_LOCAL_CLAIM_VALUES, mappedAppClaims);

            if (spDialect == null) {
                return getFilteredAttributes(mappedAppClaims, requestedClaimMappings, false);
            } else if(requestedClaimMappings.size() > 0) {
                return filteredClaims;
            } else {
            	return mappedAppClaims;
            }

        } catch (Exception e) {
            throw new FrameworkException(e.getMessage(), e);
        }
    }

    /**
     * Returns the claim dialect URI based on the client type
     * 
     * @param clientType
     * @return
     */
    protected String getDialectUri(String clientType, boolean claimMappingDefined) {

        if (FrameworkConstants.RequestType.CLAIM_TYPE_OIDC.equals(clientType)) {
            return "http://wso2.org/oidc/claim";
        } else if (FrameworkConstants.RequestType.CLAIM_TYPE_STS.equals(clientType)) {
            return "http://schemas.xmlsoap.org/ws/2005/05/identity";
        } else if (FrameworkConstants.RequestType.CLAIM_TYPE_OPENID.equals(clientType)) {
            return "http://axschema.org";
        } else if (FrameworkConstants.RequestType.CLAIM_TYPE_SCIM.equals(clientType)) {
            return "urn:scim:schemas:core:1.0";
        } else if (FrameworkConstants.RequestType.CLAIM_TYPE_WSO2.equals(clientType)) {
            return "http://wso2.org/claims";
        } else if (FrameworkConstants.RequestType.CLAIM_TYPE_SAML_SSO.equals(clientType)) {
            if (!claimMappingDefined) {
                return "http://wso2.org/claims";
            }
        }
        return null;
    }
}
