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
package org.wso2.carbon.identity.provider.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provider.stub.IdentityProviderAdminServiceStub;

public class IdentityProviderClient {

    private static final Log log = LogFactory.getLog(IdentityProviderClient.class);

    private IdentityProviderAdminServiceStub stub = null;

    public IdentityProviderClient(String cookie, String backendServerURL,
                                  ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "IdentityProviderAdminService";
        stub = new IdentityProviderAdminServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public String getOpenID(String username) throws AxisFault {
        try {
            return stub.getPrimaryOpenID(username);
        } catch (Exception e) {
            handleException("Error ouccured while retreiving OpeniD", e);
        }
        return null;
    }

    public String[] getAllOpenIDs(String userName) throws Exception {
        return stub.getAllOpenIDs(userName);
    }

    public void removeOpenIDSignUp(String openID) throws Exception {
        stub.removeOpenID(openID);
    }

    /**
     * Logs and wraps the given exception.
     *
     * @param msg Error message
     * @param e   Exception
     * @throws AxisFault
     */
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

}
