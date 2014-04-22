/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.idp.mgt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.FacebookFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.OpenIDConnectFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.OpenIDFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.PassiveSTSFederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.ResidentIdentityProvider;
import org.wso2.carbon.identity.application.common.model.SAMLFederatedAuthenticator;
import org.wso2.carbon.user.api.ClaimMapping;

public class IdentityProviderManagementService {

    private static Log log = LogFactory.getLog(IdentityProviderManager.class);
    private static String LOCAL_DEFAULT_CLAIM_DIALECT = "http://wso2.org/claims";

    /**
     * Retrieves resident Identity provider for the logged-in tenant
     * 
     * @return <code>ResidentIdentityProvider</code>
     * @throws IdentityApplicationManagementException Error when getting Resident Identity Provider
     */
    public ResidentIdentityProvider getResidentIdP() throws IdentityApplicationManagementException {

        try {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            ResidentIdentityProvider residentIdP = IdentityProviderManager.getInstance()
                    .getResidentIdP(tenantDomain);
            return residentIdP;
        } catch (Exception e) {
            String message = "Error occured while loading resident identity provider.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }
    }

    /**
     * Updated resident Identity provider for the logged-in tenant
     * 
     * @param identityProvider <code>ResidentIdentityProvider</code>
     * @throws IdentityApplicationManagementException Error when getting Resident Identity Provider
     */
    public void updateResidentIdP(ResidentIdentityProvider identityProvider)
            throws IdentityApplicationManagementException {

        try {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            IdentityProviderManager.getInstance().updateResidentIdP(identityProvider, tenantDomain);
        } catch (Exception e) {
            String message = "Error occured while updating resident identity provider.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }
    }

    /**
     * Retrieves registered Identity providers for the logged-in tenant
     * 
     * @return Array of <code>FederatedIdentityProvider</code>. IdP names, primary IdP and home
     *         realm identifiers of each IdP
     * @throws IdentityApplicationManagementException Error when getting list of Identity Providers
     */
    public FederatedIdentityProvider[] getAllIdPs() throws IdentityApplicationManagementException {
        try {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            List<FederatedIdentityProvider> identityProviders = IdentityProviderManager
                    .getInstance().getIdPs(tenantDomain);
            return identityProviders
                    .toArray(new FederatedIdentityProvider[identityProviders.size()]);
        } catch (Exception e) {
            String message = "Error occured while loading all federated identity providers.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }
    }

    /**
     * Retrieves Identity provider information for the logged-in tenant by Identity Provider name
     * 
     * @param idPName Unique name of the Identity provider of whose information is requested
     * @return <code>IdentityProviderDTO</code> Identity Provider information
     * @throws IdentityProviderMgtException
     */
    public FederatedIdentityProvider getIdPByName(String idPName)
            throws IdentityApplicationManagementException {
        try {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            return IdentityProviderManager.getInstance().getIdPByName(idPName, tenantDomain);
        } catch (Exception e) {
            String message = "Error occured while loading federated identity provider by name.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }
    }

    /**
     * Adds an Identity Provider to the logged-in tenant
     * 
     * @param identityProviderDTO Identity Provider information
     * @throws IdentityProviderMgtException Error when adding Identity Provider
     */
    public void addIdP(FederatedIdentityProvider identityProvider)
            throws IdentityApplicationManagementException {

        try {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            IdentityProviderManager.getInstance().addIdP(identityProvider, tenantDomain);
        } catch (Exception e) {
            String message = "Error occured while adding federated identity provider.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }
    }

    /**
     * Deletes an Identity Provider from the logged-in tenant
     * 
     * @param idPName Name of the IdP to be deleted
     * @throws IdentityProviderMgtException Error when deleting Identity Provider
     */
    public void deleteIdP(String idPName) throws IdentityApplicationManagementException {

        try {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            IdentityProviderManager.getInstance().deleteIdP(idPName, tenantDomain);
        } catch (Exception e) {
            String message = "Error occured while adding deleting identity provider.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }

    }

    /**
     * 
     * @return
     * @throws IdentityProviderMgtException
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
            return claimUris.toArray(new String[claimUris.size()]);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new IdentityApplicationManagementException("Error while reading system claims");
        }
    }

    /**
     * Updates a given Identity Provider's information in the logged-in tenant
     * 
     * @param oldIdPName existing Identity Provider name
     * @param newIdentityProviderDTO new Identity Provider information
     * @throws IdentityProviderMgtException Error when updating Identity Provider
     */
    public void updateIdP(String oldIdPName, FederatedIdentityProvider identityProvider)
            throws IdentityApplicationManagementException {

        try {

            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            IdentityProviderManager.getInstance().updateIdP(oldIdPName, identityProvider,
                    tenantDomain);
        } catch (Exception e) {
            String message = "Error occured while updating federated identity provider.";
            log.error(message, e);
            throw new IdentityApplicationManagementException(message, e);
        }
    }

    /**
     * Bogus operation - doing nothing.
     * @param openid
     * @param fb
     * @param saml
     * @param oidc
     * @param passive
     */
    public void bogusOperation(OpenIDFederatedAuthenticator openid,
            FacebookFederatedAuthenticator fb, SAMLFederatedAuthenticator saml,
            OpenIDConnectFederatedAuthenticator oidc, PassiveSTSFederatedAuthenticator passive) {

    }
}
