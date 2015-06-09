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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.authenticator.proxy.AuthenticationAdminClient;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.um.ws.api.internal.UserMgtWSAPIDSComponent;
import org.wso2.carbon.um.ws.api.stub.RemoteTenantManagerServiceStub;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.TimerTask;

/**
 * This is the Tenant manager used with WSRealm.
 */
public class WSRealmTenantManager implements TenantManager {

    private static final Log log = LogFactory.getLog(AnonymousSessionUtil.class);
    private static final String SERVICE_NAME = "RemoteTenantManagerService";
    private static final String AXIS_FAULT_MESSAGE = "Axis fault occured  ";
    private static final String SERVER_LOGIN_ERROR = "WSRealmTenantManager cannot login to server";

    private RemoteTenantManagerServiceStub stub;
    private String userName = null;
    private String password = null;
    private String url = null;

    public WSRealmTenantManager(String userName, String password, String url)
            throws java.lang.Exception {
        this.userName = userName;
        this.password = password;
        this.url = url;
        stub = this.getStub();
    }

    @Override
    public void activateTenant(int tenantId) throws UserStoreException {
        try {
            getStub().activateTenant(tenantId);
        } catch (Exception e) {
            handleException("", e);
        }
    }

    public int addTenant(Tenant tenant) throws UserStoreException {
        try {
            return getStub().addTenant(this.tenantToADBTenant(tenant));
        } catch (Exception e) {
            handleException("", e);
        }
        return -1;
    }

    @Override
    public void deactivateTenant(int tenantId) throws UserStoreException {
        try {
            getStub().deactivateTenant(tenantId);
        } catch (Exception e) {
            handleException("", e);
        }
    }

    @Override
    public void deleteTenant(int tenantId) throws UserStoreException {

        try {
            getStub().deleteTenant(tenantId);
        } catch (Exception e) {
            handleException("", e);
        }
    }

    @Override
    public void deleteTenant(int tenantId, boolean removeFromPersistentStorage) throws UserStoreException {
        //TODO Implement tenant deletion
        return;
    }


    @Override
    public Tenant[] getAllTenants() throws UserStoreException {
        try {
            org.wso2.carbon.um.ws.api.stub.Tenant[] tenats = stub.getAllTenants();
            if (tenats != null) {
                Tenant[] ts = new Tenant[tenats.length];
                for (int i = 0; i < tenats.length; i++) {
                    ts[i] = this.ADBTenantToTenant(tenats[i]);
                }
                return ts;
            }
        } catch (Exception e) {
            handleException("", e);
        }
        return new Tenant[0];
    }

    //TODO:implement methods
    @Override
    public org.wso2.carbon.user.api.Tenant[] getAllTenantsForTenantDomainStr(String s)
            throws org.wso2.carbon.user.api.UserStoreException {
        return new org.wso2.carbon.user.api.Tenant[0];
    }

    @Override
    public String getDomain(int tenantId) throws UserStoreException {
        try {
            return getStub().getDomain(tenantId);
        } catch (Exception e) {
            handleException("", e);
        }
        return null;
    }

    @Override
    public String getSuperTenantDomain() throws UserStoreException {
        try {
            return getStub().getSuperTenantDomain();
        } catch (Exception e) {
            handleException("", e);
        }
        return null;
    }

    @Override
    public int addTenant(org.wso2.carbon.user.api.Tenant tenant)
            throws org.wso2.carbon.user.api.UserStoreException {
        return addTenant((Tenant) tenant);
    }

    @Override
    public void updateTenant(org.wso2.carbon.user.api.Tenant tenant)
            throws org.wso2.carbon.user.api.UserStoreException {
        updateTenant((Tenant) tenant);
    }

    @Override
    public Tenant getTenant(int tenantId) throws UserStoreException {
        try {
            return this.ADBTenantToTenant(getStub().getTenant(tenantId));
        } catch (Exception e) {
            handleException("", e);
        }
        return null;
    }

    @Override
    public int getTenantId(String domain) throws UserStoreException {
        try {
            return getStub().getTenantId(domain);
        } catch (Exception e) {
            handleException("", e);
        }
        return 0;
    }

    @Override
    public boolean isTenantActive(int tenantId) throws UserStoreException {
        try {
            return getStub().isTenantActive(tenantId);
        } catch (Exception e) {
            handleException("", e);
        }
        return false;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        return;
    }

    public void updateTenant(Tenant tenant) throws UserStoreException {
        try {
            getStub().updateTenant(this.tenantToADBTenant(tenant));
        } catch (Exception e) {
            handleException("", e);
        }
    }

    private String[] handleException(String msg, Exception e) throws UserStoreException {
        log.error(msg, e);
        throw new UserStoreException(msg, e);
    }

    private Tenant ADBTenantToTenant(org.wso2.carbon.um.ws.api.stub.Tenant stubTenant) {
        Tenant tenant = new Tenant();
        tenant.setActive(stubTenant.getActive());
        tenant.setAdminName(stubTenant.getAdminName());
        tenant.setAdminPassword(stubTenant.getAdminPassword());
        tenant.setDomain(stubTenant.getDomain());
        tenant.setEmail(stubTenant.getEmail());
        tenant.setId(stubTenant.getId());
        return tenant;
    }

    private org.wso2.carbon.um.ws.api.stub.Tenant tenantToADBTenant(Tenant tenant) {
        org.wso2.carbon.um.ws.api.stub.Tenant stubTenant = new org.wso2.carbon.um.ws.api.stub.Tenant();
        stubTenant.setActive(tenant.isActive());
        stubTenant.setAdminName(tenant.getAdminName());
        stubTenant.setAdminPassword(tenant.getAdminPassword());
        stubTenant.setDomain(tenant.getDomain());
        stubTenant.setEmail(tenant.getEmail());
        stubTenant.setId(tenant.getId());
        return stubTenant;
    }

    private RemoteTenantManagerServiceStub getStub() throws UserStoreException {
        if (stub == null) {
            try {
                stub = new RemoteTenantManagerServiceStub(UserMgtWSAPIDSComponent
                        .getCcServiceInstance().getClientConfigContext(), url
                        + SERVICE_NAME);
                ServiceClient client = stub._getServiceClient();
                Options option = client.getOptions();
                option.setManageSession(true);
                LoginSender sender = new LoginSender();
                String sessionCookie = sender.login();
                if (sessionCookie == null) {
                    throw new UserStoreException(SERVER_LOGIN_ERROR);
                }
                option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                        sessionCookie);
            } catch (AxisFault axisFault) {


                throw new UserStoreException("Axis error occurred while creating service client stub",axisFault);
            }


        }
        return stub;
    }

    /**
     * This method was added to TenantManager interface to support tenant management with LDAP.
     * Hence no implementation currently in this context.
     */
    @Override
    public void initializeExistingPartitions() {
        return;
    }

    private class LoginSender extends TimerTask {


        private static final String ERROR_MESSAGE = "Logging in to Tenant Manager";
        private static final String DEFAULT_LOCALHOST = "127.0.0.1";

        @Override
        public void run() {
            try {
                synchronized (stub) {
                    String sessionCookie = login();
                    ServiceClient client = stub._getServiceClient();
                    Options option = client.getOptions();
                    option.setManageSession(true);
                    option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                            sessionCookie);
                }
            } catch (UserStoreException e) {
                log.error(ERROR_MESSAGE, e);
            }
        }

        public String login() throws UserStoreException {
            try {
                synchronized (stub) {
                    AuthenticationAdminClient client = new AuthenticationAdminClient(
                            UserMgtWSAPIDSComponent.getCcServiceInstance().getClientConfigContext(),
                            url, null, null, false);
                    boolean isLogin = client.login(userName, password, DEFAULT_LOCALHOST);
                    if (isLogin) {
                        return client.getAdminCookie();
                    } else {
                        throw new UserStoreException(ERROR_MESSAGE);
                    }
                }
            } catch (Exception e) {
                throw new UserStoreException(ERROR_MESSAGE, e);
            }
        }

    }
}
