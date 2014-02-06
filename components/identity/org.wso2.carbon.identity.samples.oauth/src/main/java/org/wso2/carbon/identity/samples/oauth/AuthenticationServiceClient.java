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
package org.wso2.carbon.identity.samples.oauth;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.core.services.authentication.AuthenticationAdminStub;
import org.wso2.carbon.identity.samples.oauth.admin.OAuthAdminServiceStub;


public class AuthenticationServiceClient {

    private String authCookie;
    private String backendServerURL;
    private ConfigurationContext configCtx;

    public AuthenticationServiceClient(String backendServerURL, ConfigurationContext configCtx) {
        this.backendServerURL = backendServerURL;
        this.configCtx = configCtx;

    }

    public boolean authenticate(String userName, String password, String remoteIp) throws Exception {
        String serviceURL = null;
        AuthenticationAdminStub authStub = null;
        boolean authenticate = false;

        serviceURL = backendServerURL + "AuthenticationAdmin";
        authStub = new AuthenticationAdminStub(configCtx, serviceURL);
        authStub._getServiceClient().getOptions().setManageSession(true);
        authenticate = authStub.login(userName, password, remoteIp);
        authCookie = (String) authStub._getServiceClient().getServiceContext().getProperty(
                HTTPConstants.COOKIE_STRING);
        return authenticate;
    }

    public String[] registerOAuthConsumer() throws Exception {
        String serviceURL = null;
        ServiceClient client = null;
        Options option = null;
        OAuthAdminServiceStub oauth = null;

        serviceURL = backendServerURL + "OAuthAdminService";
        oauth = new OAuthAdminServiceStub(configCtx, serviceURL);
        client = oauth._getServiceClient();
        option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, authCookie);
        return oauth.registerOAuthConsumer();
    }

    /*public boolean validateAuthenticationRequest(String shortLivedToken, OAuthConsumerDTO consumer)
            throws Exception {
        String serviceURL = null;
        ServiceClient client = null;
        Options option = null;
        OAuthServiceStub oauth = null;

        serviceURL = backendServerURL + "OAuthService";
        oauth = new OAuthServiceStub(configCtx, serviceURL);
        client = oauth._getServiceClient();
        option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, authCookie);
        return oauth.validateAuthenticationRequest(consumer, shortLivedToken);
    }*/

}
