/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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
package org.wso2.carbon.identity.sso.saml.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.sso.saml.stub.IdentitySAMLSSOConfigServiceStub;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderInfoDTO;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class SAMLSSOConfigServiceClient {

    private static Log log = LogFactory.getLog(SAMLSSOConfigServiceClient.class);

    private IdentitySAMLSSOConfigServiceStub stub;

    public SAMLSSOConfigServiceClient(String cookie, String backendServerURL, ConfigurationContext configCtx)
            throws AxisFault {
        try {
            String serviceURL = backendServerURL + "IdentitySAMLSSOConfigService";
            stub = new IdentitySAMLSSOConfigServiceStub(configCtx, serviceURL);
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (AxisFault ex) {
            log.error("Error generating stub for IdentitySAMLSSOConfigService", ex);
            throw new AxisFault("Error generating stub for IdentitySAMLSSOConfigService", ex);
        }
    }

    // TODO : This method must return the added service provider data instead
    public boolean addServiceProvider(SAMLSSOServiceProviderDTO serviceProviderDTO) throws AxisFault {
        boolean status = false;
        try {
            status = stub.addRPServiceProvider(serviceProviderDTO);
        } catch (Exception e) {
            log.error("Error adding a new Service Provider", e);
            throw new AxisFault(e.getMessage(), e);
        }
        return status;
    }

    // TODO : remove bellow method once above is fixed
    // this kills performance
    public SAMLSSOServiceProviderDTO getServiceProvider(String issuer) throws AxisFault {
        try {
            SAMLSSOServiceProviderInfoDTO dto = stub.getServiceProviders();
            SAMLSSOServiceProviderDTO[] sps = dto.getServiceProviders();
            if (sps != null) {
                for (SAMLSSOServiceProviderDTO sp : sps) {
                    if (sp.getIssuer().equals(issuer)) {
                        return sp;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving service provider information", e);
            throw new AxisFault(e.getMessage(), e);
        }
        return null;

    }

    public SAMLSSOServiceProviderInfoDTO getRegisteredServiceProviders() throws AxisFault {
        try {
            SAMLSSOServiceProviderInfoDTO spInfo = stub.getServiceProviders();
            return spInfo;
        } catch (Exception e) {
            log.error("Error retrieving service provider information", e);
            throw new AxisFault(e.getMessage(), e);
        }
    }

    public List<String> getCertAlias() throws AxisFault {
        List<String> certAliasList = new ArrayList<>();
        String[] certAliases;
        try {
            certAliases = stub.getCertAliasOfPrimaryKeyStore();
            for (String alias : certAliases) {
                certAliasList.add(alias);
            }
        } catch (Exception e) {
            log.error("Error retrieving Cert Aliases", e);
            throw new AxisFault(e.getMessage(), e);
        }
        return certAliasList;
    }

    public boolean removeServiceProvier(String issuerName) throws AxisFault {
        try {
            return stub.removeServiceProvider(issuerName);
        } catch (Exception e) {
            log.error("Error when removing the service provider", e);
            throw new AxisFault(e.getMessage(), e);
        }
    }

    public String[] getClaimURIs() throws AxisFault {
        String[] claimUris = null;
        try {
            claimUris = stub.getClaimURIs();
        } catch (Exception e) {
            log.error("Error when reading claims", e);
            throw new AxisFault(e.getMessage(), e);
        }
        return claimUris;
    }

    public String[] getSigningAlgorithmUris() throws IdentityRuntimeException {
        String[] signingAlgorithmUris;
        try {
            signingAlgorithmUris = stub.getSigningAlgorithmUris();
        } catch (RemoteException e) {
            throw IdentityRuntimeException.error(e.getMessage(), e);
        }
        return signingAlgorithmUris;
    }

    public String getSigningAlgorithmUriByConfig() throws IdentityRuntimeException {
        String signingAlgo;
        try {
            signingAlgo = stub.getSigningAlgorithmUriByConfig();
        } catch (RemoteException e) {
            throw IdentityRuntimeException.error(e.getMessage(), e);
        }
        return signingAlgo;
    }

    public String[] getDigestAlgorithmURIs() throws IdentityRuntimeException {
        String[] digestAlgorithms;
        try {
            digestAlgorithms = stub.getDigestAlgorithmURIs();
        } catch (RemoteException e) {
            throw IdentityRuntimeException.error(e.getMessage(), e);
        }
        return digestAlgorithms;
    }

    public String getDigestAlgorithmURIByConfig() throws IdentityRuntimeException {
        String digestAlgo;
        try {
            digestAlgo = stub.getDigestAlgorithmURIByConfig();
        } catch (RemoteException e) {
            throw IdentityRuntimeException.error(e.getMessage(), e);
        }
        return digestAlgo;
    }
}
