/*
 * Copyright (c) 2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.carbon.authenticator.proxy.AuthenticationAdminClient;
import org.wso2.carbon.um.ws.api.internal.UserMgtWSAPIDSComponent;
import org.wso2.carbon.um.ws.api.internal.UserMgtWSAPIDataHolder;
import org.wso2.carbon.um.ws.api.stub.RealmConfigurationDTO;
import org.wso2.carbon.um.ws.api.stub.RemoteUserRealmServiceStub;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.AuthorizationManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.claim.ClaimMapping;
import org.wso2.carbon.user.core.profile.ProfileConfiguration;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;

import java.util.Map;

public class WSRealm implements UserRealm {

    private static final Log log = LogFactory.getLog(WSRealm.class);
    private RealmConfiguration realmConfig = null;
    private WSUserStoreManager userStoreMan = null;
    private WSAuthorizationManager authzMan = null;
    private WSClaimManager claimManager = null;
    private WSProfileConfigurationManager profileManager = null;
    private int tenantId = -1;
    private RemoteUserRealmServiceStub stub = null;

    private static final String REALM_SERVICE_NAME = "RemoteUserRealmService";
    private static final String CONNECTION_ERROR_MESSAGE = "Error while establishing web service connection ";
    private static final String REALM_CREATION_ERROR_MESSAGE = "Cannot create session for WSRealm";
    private static final String CONFIG_RETREIVAL_ERROR_MESSAGE = "Error getting realm config";
    private static final String DEFAULT_LOCAL_IP = "127.0.0.1";
    private static final String LOGIN_ERROR_MESSAGE = "Error logging in ";
    private static final String UNSUPPORTED_MESSAGE = "Not implemented";


    /**
     * Initialize WSRealm by Carbon
     *
     * @see org.wso2.carbon.user.core.UserRealm#init(org.wso2.carbon.user.api.RealmConfiguration, java.util.Map, int)
     */
    @Override
    public void init(RealmConfiguration configBean, Map<String, Object> properties, int tenantId)
            throws UserStoreException {
        ConfigurationContext configCtxt =
                UserMgtWSAPIDSComponent.
                        getCcServiceInstance().
                        getClientConfigContext();
        init(configBean, configCtxt);
    }

    /**
     * Initialize WSRealm by Carbon
     *
     * @see org.wso2.carbon.user.core.UserRealm#init(org.wso2.carbon.user.api.RealmConfiguration, java.util.Map, int)
     */
    @Override
    public void init(RealmConfiguration configBean, Map<String, ClaimMapping> claimMapping,
                     Map<String, ProfileConfiguration> profileConfigs, int tenantId)
            throws UserStoreException {
        ConfigurationContext configCtxt =
                UserMgtWSAPIDSComponent.
                        getCcServiceInstance().
                        getClientConfigContext();
        init(configBean, configCtxt);
    }


    /**
     * Initialize WSRealm by Non-carbon environment
     */
    public void init(RealmConfiguration configBean, ConfigurationContext configCtxt)
            throws UserStoreException {
        realmConfig = configBean;

        if (UserMgtWSAPIDataHolder.getInstance().getSessionCookie() == null) {
            synchronized (WSRealm.class) {
                if (UserMgtWSAPIDataHolder.getInstance().getSessionCookie() == null) {
                    login();
                }
            }
        }

        if (UserMgtWSAPIDataHolder.getInstance().getSessionCookie() == null) {
            throw new UserStoreException(REALM_CREATION_ERROR_MESSAGE);
        }

        init((String) realmConfig.getRealmProperty(WSRemoteUserMgtConstants.SERVER_URL),
                UserMgtWSAPIDataHolder.getInstance().getSessionCookie(), configCtxt);
    }

    /**
     * Initialize WSRealm by Non-carbon environment
     */
    void init(String url, String cookie, ConfigurationContext configCtxt)
            throws UserStoreException {
        try {

            stub = new RemoteUserRealmServiceStub(configCtxt, url + REALM_SERVICE_NAME);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                    this
                            .getSessionCookie());
        } catch (AxisFault e) {

            throw new UserStoreException("Axis error occurred while creating service client stub");
        }

        userStoreMan = new WSUserStoreManager(url, cookie, configCtxt);
        authzMan = new WSAuthorizationManager(url, cookie, configCtxt);
        claimManager = new WSClaimManager(url, cookie, configCtxt);
        profileManager = new WSProfileConfigurationManager(url, cookie, configCtxt);
    }

    @Override
    public UserStoreManager getUserStoreManager() throws UserStoreException {
        return userStoreMan;
    }

    @Override
    public AuthorizationManager getAuthorizationManager() throws UserStoreException {
        return authzMan;
    }

    @Override
    public ClaimManager getClaimManager() throws UserStoreException {
        return claimManager;
    }

    @Override
    public ProfileConfigurationManager getProfileConfigurationManager() throws UserStoreException {
        return profileManager;
    }

    @Override
    public RealmConfiguration getRealmConfiguration() throws UserStoreException {
        try {
            RealmConfigurationDTO realmConfigurationDTO = stub.getRealmConfiguration();
            return WSRealmUtil.convertToRealmConfiguration(realmConfigurationDTO);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new UserStoreException(CONFIG_RETREIVAL_ERROR_MESSAGE, e);
        }
    }

    @Override
    public void cleanUp() throws UserStoreException {
        throw new UserStoreException(new UnsupportedOperationException(UNSUPPORTED_MESSAGE));
    }

    public void login() throws UserStoreException {
        String userName = realmConfig.getRealmProperty(WSRemoteUserMgtConstants.USER_NAME);
        String password = realmConfig.getRealmProperty(WSRemoteUserMgtConstants.PASSWORD);
        try {
            ConfigurationContext configContext = UserMgtWSAPIDSComponent
                    .getCcServiceInstance().getClientConfigContext();
            AuthenticationAdminClient client = new AuthenticationAdminClient(configContext,
                    realmConfig.getRealmProperty(WSRemoteUserMgtConstants.SERVER_URL),
                    UserMgtWSAPIDataHolder.getInstance().getSessionCookie(), null, false);
            boolean isLogin = client.login(userName, password, DEFAULT_LOCAL_IP);
            if (isLogin) {
                UserMgtWSAPIDataHolder.getInstance().setSessionCookie(client.getAdminCookie());
            }
        } catch (Exception e) {
            throw new UserStoreException(LOGIN_ERROR_MESSAGE + e.getMessage(), e);
        }
    }

    public void lastAccess() {
        return;
    }

    public String getSessionCookie() {
        return UserMgtWSAPIDataHolder.getInstance().getSessionCookie();
    }

    protected int getTenantId() {
        return tenantId;
    }
}
