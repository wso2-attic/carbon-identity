/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sts.passive.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sts.passive.stub.IdentityPassiveSTSServiceStub;
import org.wso2.carbon.identity.sts.passive.stub.types.ClaimDTO;
import org.wso2.carbon.identity.sts.passive.stub.types.RequestToken;
import org.wso2.carbon.identity.sts.passive.stub.types.ResponseToken;

public class IdentityPassiveSTSClient {

    private static final Log log = LogFactory.getLog(IdentityPassiveSTSClient.class);

    private IdentityPassiveSTSServiceStub stub = null;

    public IdentityPassiveSTSClient(String backendServerURL, ConfigurationContext configCtx)
            throws AxisFault {
        String serviceURL = backendServerURL + "IdentityPassiveSTSService";
        stub = new IdentityPassiveSTSServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
    }

    public ResponseToken getResponse(RequestToken request) throws AxisFault {
        try {
            return stub.getResponse(request);
        } catch (Exception e) {
            handleException("Error ouccured getting the response from the backend service", e);
        }
        return null;
    }

    /**
     * Logs and wraps the given exception.
     *
     * @param msg Error message
     * @param e   Exception
     * @throws org.apache.axis2.AxisFault
     */
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    public void addTrustedService(String realmName, String claimDialect, String claims)
            throws AxisFault {
        try {
            stub.addTrustedService(realmName, claimDialect, claims);
        } catch (Exception e) {
            handleException("Error occurred while adding the trusted service: " + realmName, e);
        }
    }

    public void removeTrustedService(String realmName) throws AxisFault {
        try {
            stub.removeTrustedService(realmName);
        } catch (Exception e) {
            handleException("Error occurred while removing the trusted service: " + realmName, e);
        }
    }

    public ClaimDTO[] getAllTrustedServices() throws AxisFault {
        try {
            return stub.getAllTrustedServices();
        } catch (Exception e) {
            handleException("Error occurred while getting all trusted services.", e);
        }
        return new ClaimDTO[0];
    }
}
