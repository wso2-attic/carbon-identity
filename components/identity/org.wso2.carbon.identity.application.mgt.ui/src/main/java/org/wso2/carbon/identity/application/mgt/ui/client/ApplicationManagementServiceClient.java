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

package org.wso2.carbon.identity.application.mgt.ui.client;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.xsd.ApplicationBasicInfo;
import org.wso2.carbon.identity.application.common.model.xsd.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.xsd.LocalAuthenticator;
import org.wso2.carbon.identity.application.common.model.xsd.RequestPathAuthenticator;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.ApplicationManagementServiceIdentityException;
import org.wso2.carbon.identity.application.mgt.stub.ApplicationManagementServiceStub;

public class ApplicationManagementServiceClient {

    ApplicationManagementServiceStub stub = null;

    Log log = LogFactory.getLog(ApplicationManagementServiceClient.class);
    boolean debugEnabled = log.isErrorEnabled();

    /**
     * 
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws AxisFault
     */
    public ApplicationManagementServiceClient(String cookie, String backendServerURL,
            ConfigurationContext configCtx) throws AxisFault {

        String serviceURL = backendServerURL + "ApplicationManagementService";
        stub = new ApplicationManagementServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        if (debugEnabled) {
            log.debug("Invoking service " + serviceURL);
        }

    }

    /**
     * 
     * @param serviceProvider
     * @throws Exception
     */
    public void createApplication(ServiceProvider serviceProvider) throws Exception {
        try {
            if (debugEnabled) {
                log.debug("Registering Service Provider " + serviceProvider.getApplicationName());
            }
            stub.createApplication(serviceProvider);
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        } catch (ApplicationManagementServiceIdentityException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }

    }

    /**
     * 
     * @param applicationName
     * @return
     * @throws Exception
     */
    public ServiceProvider getApplication(String applicationName) throws Exception {
        try {
            if (debugEnabled) {
                log.debug("Loading Service Provider " + applicationName);
            }
            return stub.getApplication(applicationName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }

    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public ApplicationBasicInfo[] getAllApplicationBasicInfo() throws Exception {
        try {
            return stub.getAllApplicationBasicInfo();
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        } catch (ApplicationManagementServiceIdentityException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
    }

    /**
     * 
     * @param serviceProvider
     * @throws Exception
     */
    public void updateApplicationData(ServiceProvider serviceProvider) throws Exception {
        try {
            stub.updateApplication(serviceProvider);
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        } catch (ApplicationManagementServiceIdentityException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }
    }

    /**
     * 
     * @param applicationID
     * @throws Exception
     */
    public void deleteApplication(String applicationID) throws Exception {
        try {
            stub.deleteApplication(applicationID);
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        } catch (ApplicationManagementServiceIdentityException e) {
            log.error(e.getMessage(), e);
            throw new Exception(e.getMessage());
        }

    }

    /**
     * 
     * @param identityProviderName
     * @throws Exception
     */
    public FederatedIdentityProvider getFederatedIdentityProvider(String identityProviderName)
            throws Exception {
        return stub.getFederatedIdentityProvider(identityProviderName);
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public RequestPathAuthenticator[] getAllRequestPathAuthenticators() throws Exception {
        return stub.getAllRequestPathAuthenticators();
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public LocalAuthenticator[] getAllLocalAuthenticators() throws Exception {
        return stub.getAllLocalAuthenticators();
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public FederatedIdentityProvider[] getAllFederatedIdentityProvider() throws Exception {
        FederatedIdentityProvider[] idps = null;

        try {
            idps = stub.getAllFederatedIdentityProviders();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return idps;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public String[] getAllClaimUris() throws Exception {
        return stub.getAllLocalClaimUris();
    }

}
