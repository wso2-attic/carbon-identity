/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.relyingparty.ui.client;

import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.identity.relyingparty.stub.auth.LoggedUserInfoAdminStub;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class AuthenticationServiceClient {

    private AuthenticationAdminStub stub;
    /**
     * Instantiates RelyingPartyServiceClient
     * 
     * @param cookie
     *            For session management
     * @param backendServerURL
     *            URL of the back end server where UserRegistrationAdminService is running.
     * @param configCtx
     *            ConfigurationContext
     * @throws org.apache.axis2.AxisFault
     *             if error occurs when instantiating the stub
     */
    public AuthenticationServiceClient(String cookie, String backendServerURL,
            ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "AuthenticationAdmin";
        stub = new AuthenticationAdminStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public static void setUserInformation(HttpSession session, String cookie, HttpServletRequest request, String userName)
            throws RemoteException {
        try {
            String backendServerURL = (String) session.getAttribute(CarbonConstants.SERVER_URL);

            ServletContext servletContext = session.getServletContext();
            ConfigurationContext configContext = (ConfigurationContext) servletContext
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

            LoggedUserInfoAdminStub stub = new LoggedUserInfoAdminStub(configContext,
                    backendServerURL + "LoggedUserInfoAdmin");
            ServiceClient client = stub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
            org.wso2.carbon.identity.relyingparty.stub.auth.loggeduserinfo.LoggedUserInfo userInfo
                    = stub.getUserInfo();

            String[] permissionArray = userInfo.getUIPermissionOfUser();
            ArrayList<String> list = new ArrayList<String>();
            for (String permission : permissionArray) {
                list.add(permission);
            }

            session.setAttribute(ServerConstants.USER_PERMISSIONS, list);
            if (userInfo.getPasswordExpiration() != null) {
                session.setAttribute(ServerConstants.PASSWORD_EXPIRATION, userInfo
                        .getPasswordExpiration());
            }
            session.setAttribute(ServerConstants.ADMIN_SERVICE_COOKIE, cookie);
            String tenantAwareUserName = MultitenantUtils.getTenantAwareUsername(userName);
            if (userName.equals(tenantAwareUserName)) {
            	 session.setAttribute(MultitenantConstants.IS_MASTER_TENANT,"true");
            }
            String domain = MultitenantUtils.getTenantDomain(userName);
            session.setAttribute("logged-user", tenantAwareUserName);
            session.setAttribute(MultitenantConstants.TENANT_DOMAIN, domain);
            session.getServletContext().setAttribute("logged-user", tenantAwareUserName);
            
        } catch (AxisFault e) {
            throw e;
        } catch (RemoteException e) {
            throw e;
        } catch (Exception e) {
            throw new AxisFault("Exception occured", e);
        }
    }

}
