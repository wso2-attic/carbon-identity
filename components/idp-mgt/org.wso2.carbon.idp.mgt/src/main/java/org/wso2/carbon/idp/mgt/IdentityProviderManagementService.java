/*
 * Copyright (c) 2014 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * under the License.
 */

package org.wso2.carbon.idp.mgt;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.idp.mgt.internal.IdpMgtListenerServiceComponent;
import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtLister;
import org.wso2.carbon.idp.mgt.util.IdPManagementConstants;
import org.wso2.carbon.user.api.ClaimMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IdentityProviderManagementService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(IdentityProviderManager.class);
    private static String LOCAL_DEFAULT_CLAIM_DIALECT = "http://wso2.org/claims";

    /**
     *
     * Retrieves resident Identity provider for the logged-in tenant
     *
     * @return <code>IdentityProvider</code>
     * @throws IdentityApplicationManagementException Error when getting Resident Identity Provider
     */
    public IdentityProvider getResidentIdP() throws IdentityApplicationManagementException {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        IdentityProvider residentIdP = IdentityProviderManager.getInstance()
                .getResidentIdP(tenantDomain);
        return residentIdP;
    }

    /**
     * Updated resident Identity provider for the logged-in tenant
     *
     * @param identityProvider <code>IdentityProvider</code>
     * @throws IdentityApplicationManagementException Error when getting Resident Identity Provider
     */
    public void updateResidentIdP(IdentityProvider identityProvider)
            throws IdentityApplicationManagementException {
        if (identityProvider == null) {
            throw new IllegalArgumentException("Identity provider is null");
        }
        // invoking the listeners
        List<IdentityProviderMgtLister> listeners = IdpMgtListenerServiceComponent.getListners();
        for (IdentityProviderMgtLister listener : listeners) {
            listener.updateResidentIdP(identityProvider);
        }

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        IdentityProviderManager.getInstance().updateResidentIdP(identityProvider, tenantDomain);
    }

    /**
     * Retrieves registered Identity providers for the logged-in tenant
     *
     * @return Array of <code>IdentityProvider</code>. IdP names, primary IdP and home
     * realm identifiers of each IdP
     * @throws IdentityApplicationManagementException Error when getting list of Identity Providers
     */
    public IdentityProvider[] getAllIdPs() throws IdentityApplicationManagementException {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        List<IdentityProvider> identityProviders = IdentityProviderManager.getInstance().getIdPs(tenantDomain);
        for (int i = 0; i < identityProviders.size(); i++) {
            String providerName = identityProviders.get(i).getIdentityProviderName();
            if (providerName != null && providerName.startsWith(IdPManagementConstants.SHARED_IDP_PREFIX)) {
                identityProviders.remove(i);
                i--;
            }
        }
        return identityProviders.toArray(new IdentityProvider[identityProviders.size()]);
    }


    /**
     * Retrieves Enabled registered Identity providers for the logged-in tenant
     *
     * @return Array of <code>IdentityProvider</code>. IdP names, primary IdP and home
     * realm identifiers of each IdP
     * @throws IdentityApplicationManagementException Error when getting list of Identity Providers
     */
    public IdentityProvider[] getEnabledAllIdPs() throws IdentityApplicationManagementException {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        List<IdentityProvider> identityProviders = IdentityProviderManager.getInstance().getEnabledIdPs(tenantDomain);
        return identityProviders.toArray(new IdentityProvider[identityProviders.size()]);
    }


    /**
     * Retrieves Identity provider information for the logged-in tenant by Identity Provider name
     *
     * @param idPName Unique name of the Identity provider of whose information is requested
     * @return <code>IdentityProvider</code> Identity Provider information
     * @throws IdentityApplicationManagementException
     */
    public IdentityProvider getIdPByName(String idPName)
            throws IdentityApplicationManagementException {
        if (StringUtils.isEmpty(idPName)) {
            throw new IllegalArgumentException("Provided IdP name is empty");
        }
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        return IdentityProviderManager.getInstance().getIdPByName(idPName, tenantDomain, true);
    }

    /**
     * Adds an Identity Provider to the logged-in tenant
     *
     * @param identityProvider <code>IdentityProvider</code> new Identity Provider information
     * @throws IdentityApplicationManagementException Error when adding Identity Provider
     */
    public void addIdP(IdentityProvider identityProvider) throws IdentityApplicationManagementException {
        if (identityProvider == null) {
            throw new IllegalArgumentException("Identity provider cannot be null when adding an IdP");
        }
        if (identityProvider.getIdentityProviderName() != null && identityProvider.getIdentityProviderName().startsWith
                (IdPManagementConstants.SHARED_IDP_PREFIX)) {
            throw new IdentityApplicationManagementException("Identity provider name cannot have " +
                    IdPManagementConstants.SHARED_IDP_PREFIX + " as prefix.");
        }

        // invoking the listeners
        List<IdentityProviderMgtLister> listeners = IdpMgtListenerServiceComponent.getListners();
        for (IdentityProviderMgtLister listener : listeners) {
            listener.addIdP(identityProvider);
        }

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        IdentityProviderManager.getInstance().addIdP(identityProvider, tenantDomain);
    }

    /**
     * Deletes an Identity Provider from the logged-in tenant
     *
     * @param idPName Name of the IdP to be deleted
     * @throws IdentityApplicationManagementException Error when deleting Identity Provider
     */
    public void deleteIdP(String idPName) throws IdentityApplicationManagementException {
        if (StringUtils.isEmpty(idPName)) {
            throw new IllegalArgumentException("Provided IdP name is empty");
        }
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        IdentityProviderManager.getInstance().deleteIdP(idPName, tenantDomain);

        // invoking the listeners
        List<IdentityProviderMgtLister> listeners = IdpMgtListenerServiceComponent.getListners();
        for (IdentityProviderMgtLister listener : listeners) {
            listener.deleteIdP(idPName);
        }
    }

    /**
     * @return
     * @throws IdentityApplicationManagementException
     */
    public String[] getAllLocalClaimUris() throws IdentityApplicationManagementException {
        try {
            String claimDialect = LOCAL_DEFAULT_CLAIM_DIALECT;
            ClaimMapping[] claimMappings = CarbonContext.getThreadLocalCarbonContext()
                    .getUserRealm().getClaimManager().getAllClaimMappings(claimDialect);
            List<String> claimUris = new ArrayList<String>();
            for (ClaimMapping claimMap : claimMappings) {
                claimUris.add(claimMap.getClaim().getClaimUri());
            }
            String[] allLocalClaimUris = claimUris.toArray(new String[claimUris.size()]);
            if (ArrayUtils.isNotEmpty(allLocalClaimUris)) {
                Arrays.sort(allLocalClaimUris);
            }
            return allLocalClaimUris;
        } catch (Exception e) {
            String message = "Error while reading system claims";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message);
        }
    }

    /**
     * Updates a given Identity Provider's information in the logged-in tenant
     *
     * @param oldIdPName       existing Identity Provider name
     * @param identityProvider <code>IdentityProvider</code> new Identity Provider information
     * @throws IdentityApplicationManagementException Error when updating Identity Provider
     */
    public void updateIdP(String oldIdPName, IdentityProvider identityProvider) throws
            IdentityApplicationManagementException {
        if (identityProvider == null) {
            throw new IllegalArgumentException("Provided IdP is null");
        }
        if (StringUtils.isEmpty(oldIdPName)) {
            throw new IllegalArgumentException("The IdP name which need to be updated is empty");
        }
        //Updating a non-shared IdP's name to have shared prefix is not allowed
        if (oldIdPName != null && !oldIdPName.startsWith(IdPManagementConstants.SHARED_IDP_PREFIX) &&
                identityProvider != null && identityProvider.getIdentityProviderName() != null && identityProvider
                .getIdentityProviderName().startsWith(IdPManagementConstants.SHARED_IDP_PREFIX)) {
            throw new IdentityApplicationManagementException("Cannot update Idp name to have '" +
                    IdPManagementConstants.SHARED_IDP_PREFIX + "' as a prefix (previous name:" + oldIdPName + ", " +
                    "New name: " + identityProvider.getIdentityProviderName() + ")");
        }
        // invoking the listeners
        List<IdentityProviderMgtLister> listeners = IdpMgtListenerServiceComponent.getListners();
        for (IdentityProviderMgtLister listener : listeners) {
            listener.updateIdP(oldIdPName, identityProvider);
        }
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        IdentityProviderManager.getInstance().updateIdP(oldIdPName, identityProvider, tenantDomain);
    }

    /**
     * Get the authenticators registered in the system.
     *
     * @return <code>FederatedAuthenticatorConfig</code> array.
     * @throws IdentityApplicationManagementException Error when getting authenticators registered in the system
     */
    public FederatedAuthenticatorConfig[] getAllFederatedAuthenticators() throws IdentityApplicationManagementException {
        return IdentityProviderManager.getInstance().getAllFederatedAuthenticators();
    }

    public ProvisioningConnectorConfig[] getAllProvisioningConnectors() throws IdentityApplicationManagementException {
        return IdentityProviderManager.getInstance().getAllProvisioningConnectors();
    }
}
