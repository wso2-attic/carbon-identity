/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.um.ws.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.um.ws.api.stub.RemoteClaimManagerServiceStub;
import org.wso2.carbon.um.ws.api.stub.RemoteClaimManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.claim.ClaimManager;

import java.rmi.RemoteException;

public class WSClaimManager implements ClaimManager {

    private static final Log log = LogFactory.getLog(WSClaimManager.class);
    private RemoteClaimManagerServiceStub stub = null;

    private static final String SERVICE_NAME = "RemoteClaimManagerService";
    private static final String CONNECTION_ERROR_MESSAGE = "Error while establishing web service connection ";

    public WSClaimManager(String serverUrl, String cookie, ConfigurationContext configCtxt)
            throws UserStoreException {
        try {
            stub =
                    new RemoteClaimManagerServiceStub(configCtxt, serverUrl +
                            SERVICE_NAME);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (AxisFault e) {

            throw new UserStoreException("Axis error occurred while creating service client stub", e);
        }
    }

    @Override
    public void addNewClaimMapping(ClaimMapping mapping) throws UserStoreException {
        try {
            org.wso2.carbon.um.ws.api.stub.ClaimMapping claimMapping = WSRealmUtil
                    .convertToADBClaimMapping(mapping);
            stub.addNewClaimMapping(claimMapping);
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }

    }

    @Override

    public void deleteClaimMapping(ClaimMapping mapping) throws UserStoreException {
        try {
            org.wso2.carbon.um.ws.api.stub.ClaimMapping claimMapping = WSRealmUtil
                    .convertToADBClaimMapping(mapping);
            stub.deleteClaimMapping(claimMapping);
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }

    }

    @Override
    public String[] getAllClaimUris() throws UserStoreException {
        try {
            return stub.getAllClaimUris();
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    @Override
    public String getAttributeName(String claimURI) throws UserStoreException {
        try {
            return stub.getAttributeName(claimURI);
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Claim getClaim(String claimURI) throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaim(stub.getClaim(claimURI));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public ClaimMapping getClaimMapping(String claimURI) throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaimMapping(stub.getClaimMapping(claimURI));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void updateClaimMapping(ClaimMapping mapping) throws UserStoreException {
        try {
            stub.updateClaimMapping(WSRealmUtil.convertToADBClaimMapping(mapping));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }

    }

    private String[] handleException(String msg, Exception e) throws UserStoreException {
        log.error(e.getMessage(), e);
        throw new UserStoreException(msg, e);
    }

    @Override
    public String getAttributeName(String domainName, String claimURI)
            throws org.wso2.carbon.user.api.UserStoreException {
        try {
            return stub.getAttributeNameFromDomain(domainName, claimURI);
        } catch (RemoteException e) {
            this.handleException(e.getMessage(), e);
        } catch (RemoteClaimManagerServiceUserStoreExceptionException e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public org.wso2.carbon.user.api.ClaimMapping[] getAllSupportClaimMappingsByDefault()
            throws org.wso2.carbon.user.api.UserStoreException {
        try {
            return WSRealmUtil.convertToClaimMappings(stub.getAllSupportClaimMappingsByDefault());
        } catch (RemoteException e) {
            this.handleException(e.getMessage(), e);
        } catch (RemoteClaimManagerServiceUserStoreExceptionException e) {
            this.handleException(e.getMessage(), e);
        }
        return new ClaimMapping[0];
    }

    @Override
    public org.wso2.carbon.user.api.ClaimMapping[] getAllClaimMappings()
            throws org.wso2.carbon.user.api.UserStoreException {
        try {
            return WSRealmUtil.convertToClaimMappings(stub.getAllClaimMappings(null));
        } catch (RemoteException e) {
            this.handleException(e.getMessage(), e);
        } catch (RemoteClaimManagerServiceUserStoreExceptionException e) {
            this.handleException(e.getMessage(), e);
        }
        return new ClaimMapping[0];
    }

    @Override
    public org.wso2.carbon.user.api.ClaimMapping[] getAllClaimMappings(String dialectURI)
            throws org.wso2.carbon.user.api.UserStoreException {
        try {
            return WSRealmUtil.convertToClaimMappings(stub.getAllClaimMappings(null));
        } catch (RemoteException e) {
            this.handleException(e.getMessage(), e);
        } catch (RemoteClaimManagerServiceUserStoreExceptionException e) {
            this.handleException(e.getMessage(), e);
        }
        return new ClaimMapping[0];
    }

    @Override
    public org.wso2.carbon.user.api.ClaimMapping[] getAllRequiredClaimMappings()
            throws org.wso2.carbon.user.api.UserStoreException {
        try {
            return WSRealmUtil.convertToClaimMappings(stub.getAllRequiredClaimMappings());
        } catch (RemoteException e) {
            this.handleException(e.getMessage(), e);
        } catch (RemoteClaimManagerServiceUserStoreExceptionException e) {
            this.handleException(e.getMessage(), e);
        }
        return new ClaimMapping[0];
    }
}
