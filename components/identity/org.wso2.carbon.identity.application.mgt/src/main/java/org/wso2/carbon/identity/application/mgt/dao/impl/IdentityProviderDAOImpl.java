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
package org.wso2.carbon.identity.application.mgt.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticator;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnector;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticator;
import org.wso2.carbon.identity.application.mgt.dao.IdentityProviderDAO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;

public class IdentityProviderDAOImpl implements IdentityProviderDAO {

    public static final String BASIC = "basic";
    public static final String OAUTH_BEARER = "oauth-bearer";
    public static final String BASIC_AUTH = "basic-auth";
    public static final String SAML_SSO = "samlsso";
    public static final String OPENID_CONNECT = "openidconnect";
    public static final String OPENID = "openid";
    public static final String PASSIVE_STS = "passive-sts";
    public static final String FACEBOOK_AUTH = "facebook";

    /**
     * 
     * @param identityProvider
     * @return
     */
    private String[] getFederatedAuthenticatorNames(FederatedIdentityProvider identityProvider) {
        List<String> authns = new ArrayList<String>();

        authns.add(FACEBOOK_AUTH);
        authns.add(OPENID_CONNECT);
        authns.add(OPENID);
        authns.add(SAML_SSO);

        return authns.toArray(new String[authns.size()]);
    }

    /**
     * 
     * @return
     */
    private String[] getLocalAuthenticatorNames() {
        List<String> authns = new ArrayList<String>();
        authns.add(BASIC);
        return authns.toArray(new String[authns.size()]);
    }

    /**
     * 
     * @return
     */
    private String[] getRequestPathAuthenticatorNames() {
        List<String> authns = new ArrayList<String>();
        authns.add(BASIC_AUTH);
        authns.add(OAUTH_BEARER);
        return authns.toArray(new String[authns.size()]);
    }

    /**
     * 
     * @param idp
     * @return
     * @throws IdentityException
     */
    public String getDefaultAuthenticator(String idpName) throws IdentityException {
        IdentityProviderManager idpManager = IdentityProviderManager.getInstance();
        try {
            FederatedIdentityProvider idp = idpManager.getIdPByName(idpName, CarbonContext
                    .getThreadLocalCarbonContext().getTenantDomain());
            return idp.getDefaultAuthenticator().getName();
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityException(e.getMessage(), e);
        }
    }

    @Override
    /**
     *
     */
    public FederatedIdentityProvider getFederatedIdentityProvider(String idpName)
            throws IdentityException {
        IdentityProviderManager idpManager = IdentityProviderManager.getInstance();
        try {
            FederatedIdentityProvider idp = idpManager
                    .getIdPByName(idpName, CarbonContext.getThreadLocalCarbonContext()
                            .getTenantDomain());

            FederatedIdentityProvider identityProvider = new FederatedIdentityProvider();
            identityProvider.setIdentityProviderName(idp.getIdentityProviderName());

            FederatedAuthenticator defaultAuthenticator = new FederatedAuthenticator();
            defaultAuthenticator.setName(getDefaultAuthenticator(idp.getIdentityProviderName()));

            List<FederatedAuthenticator> federatedAuthenticators = new ArrayList<FederatedAuthenticator>();
            String[] authenticators = getFederatedAuthenticatorNames(idp);
            for (int i = 0; i < authenticators.length; i++) {
                FederatedAuthenticator fedAuthenticator = new FederatedAuthenticator();
                fedAuthenticator.setName(authenticators[i]);
                federatedAuthenticators.add(fedAuthenticator);
            }
            identityProvider.setFederatedAuthenticators(federatedAuthenticators
                    .toArray(new FederatedAuthenticator[federatedAuthenticators.size()]));

            // TODO:
            ProvisioningConnector google = new ProvisioningConnector();
            google.setName("googleapps");
            ProvisioningConnector spml = new ProvisioningConnector();
            spml.setName("spml");
            ProvisioningConnector salesforce = new ProvisioningConnector();
            salesforce.setName("salesforce");
            ProvisioningConnector scim = new ProvisioningConnector();
            scim.setName("scim");
            identityProvider.setProvisoningConnectors(new ProvisioningConnector[] { google, spml,
                    salesforce, scim });

            return identityProvider;

        } catch (IdentityApplicationManagementException e) {
            throw new IdentityException(e.getMessage(), e);
        }
    }

    @Override
    /**
     *
     */
    public List<FederatedIdentityProvider> getAllFederatedIdentityProviders()
            throws IdentityException {

        IdentityProviderManager idpManager = IdentityProviderManager.getInstance();

        List<FederatedIdentityProvider> idps;
        try {
            idps = idpManager
                    .getIdPs(CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityException(e.getMessage(), e);
        }

        List<FederatedIdentityProvider> federatedIdentityProviders = new ArrayList<FederatedIdentityProvider>();

        if (idps.size() > 0) {
            for (FederatedIdentityProvider idp : idps) {
                federatedIdentityProviders.add(getFederatedIdentityProvider(idp.getIdentityProviderName()));
            }
        }

        return federatedIdentityProviders;
    }

    @Override
    /**
     * 
     */
    public List<LocalAuthenticator> getAllLocalAuthenticators() throws IdentityException {

        List<LocalAuthenticator> localAuthenticatorList = new ArrayList<LocalAuthenticator>();
        String[] localAuthenticators = getLocalAuthenticatorNames();
        for (int i = 0; i < localAuthenticators.length; i++) {
            LocalAuthenticator localAuthenticator = new LocalAuthenticator();
            localAuthenticator.setName(localAuthenticators[i]);
            localAuthenticatorList.add(localAuthenticator);
        }
        return localAuthenticatorList;
    }

    @Override
    /**
     * 
     */
    public List<RequestPathAuthenticator> getAllRequestPathAuthenticators()
            throws IdentityException {
        List<RequestPathAuthenticator> reqPathAuthenticatorList = new ArrayList<RequestPathAuthenticator>();
        String[] reqPathAuthenticators = getRequestPathAuthenticatorNames();
        for (int i = 0; i < reqPathAuthenticators.length; i++) {
            RequestPathAuthenticator reqPathAuthenticator = new RequestPathAuthenticator();
            reqPathAuthenticator.setName(reqPathAuthenticators[i]);
            reqPathAuthenticatorList.add(reqPathAuthenticator);
        }
        return reqPathAuthenticatorList;
    }

}
