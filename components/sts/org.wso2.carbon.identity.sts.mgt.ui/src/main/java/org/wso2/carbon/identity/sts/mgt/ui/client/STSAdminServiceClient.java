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
package org.wso2.carbon.identity.sts.mgt.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sts.mgt.stub.IdentitySTSAdminServiceStub;
import org.wso2.carbon.identity.sts.mgt.stub.dto.CardIssuerDTO;
import org.wso2.carbon.identity.sts.mgt.stub.dto.CardIssuerTokenDTO;

public class STSAdminServiceClient {

    private static final Log log = LogFactory.getLog(STSAdminServiceClient.class);
    private IdentitySTSAdminServiceStub stub;

    /**
     * Instantiates STSAdminServiceClient
     *
     * @param cookie           For session management
     * @param backendServerURL URL of the back end server where IdentitySTSAdminService is running.
     * @param configCtx        ConfigurationContext
     * @throws AxisFault
     */
    public STSAdminServiceClient(String cookie, String backendServerURL,
                                 ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "IdentitySTSAdminService";
        stub = new IdentitySTSAdminServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public CardIssuerDTO readCardIssuerConfiguration() throws AxisFault {
        try {
            return stub.readCardIssuerConfiguration();
        } catch (Exception e) {
            handleException(
                    "Error ouccured while reading card issuer configurations from the backend service",
                    e);
        }
        return null;
    }

    public void updateCardIssueConfiguration(CardIssuerDTO issuer) throws AxisFault {
        try {
            stub.updateCardIssueConfiguration(issuer);
        } catch (Exception e) {
            handleException(
                    "Error ouccured while updating card issuer configurations at the backend service",
                    e);
        }
    }

    public String getSupportingTokens(CardIssuerTokenDTO[] tokens) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].getSupported()) {
                if (i < tokens.length - 1) {
                    buffer.append(tokens[i].getTokenType() + ",");
                } else {
                    buffer.append(tokens[i].getTokenType());
                }
            }
        }
        return buffer.toString();
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
