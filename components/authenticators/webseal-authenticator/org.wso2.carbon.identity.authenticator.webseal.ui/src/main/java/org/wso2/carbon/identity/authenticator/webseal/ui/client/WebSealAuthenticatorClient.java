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

package org.wso2.carbon.identity.authenticator.webseal.ui.client;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.identity.authenticator.webseal.stub.client.WebSealAuthenticatorStub;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpSession;

/**
 * Service client
 */
public class WebSealAuthenticatorClient {

    private WebSealAuthenticatorStub stub;
    private static final Log log = LogFactory.getLog(WebSealAuthenticatorClient.class);
    private HttpSession session;

    public WebSealAuthenticatorClient(ConfigurationContext ctx, String serverURL, String cookie,
            HttpSession session) throws Exception {
        this.session = session;
        String serviceEPR = serverURL + "WebSealAuthenticator";
        stub = new WebSealAuthenticatorStub(ctx, serviceEPR);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        if (cookie != null) {
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        }
    }

    public boolean login(String websealUser, String password, String userName) throws AuthenticationException {
        try {
            boolean loginStatus =  stub.login(websealUser, password, userName, "WEBSEAL_CLIENT");
            setAdminCookie(loginStatus, userName);
            return loginStatus;
        } catch (Exception e) {
            log.error("Error when sign-in for the user : " + websealUser, e);
            throw new AuthenticationException("Error when sign-in for the user : " + websealUser, e);
        }
    }

    public void logout(HttpSession session) throws AuthenticationException {
        try {
            session.removeAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN);
        } catch (java.lang.Exception e) {
            String msg = "Error occurred while logging out";
            log.error(msg, e);
            throw new AuthenticationException(msg, e);
        }
    }

    private void setAdminCookie(boolean result, String userName) {
        if (result) {
            String cookie = (String) stub._getServiceClient().getServiceContext().getProperty(
                    HTTPConstants.COOKIE_STRING);
            session.setAttribute(ServerConstants.ADMIN_SERVICE_AUTH_TOKEN, cookie);
        }
    }
}
