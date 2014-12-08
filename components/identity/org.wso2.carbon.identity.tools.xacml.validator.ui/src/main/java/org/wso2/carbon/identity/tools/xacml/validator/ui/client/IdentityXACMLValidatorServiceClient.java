/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.tools.xacml.validator.ui.client;

import java.rmi.RemoteException;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.tools.xacml.validator.stub.*;
import org.wso2.carbon.identity.tools.xacml.validator.stub.types.ErrorItem;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.ui.CarbonUIMessage;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.identity.entitlement.dto.xsd.*;
import org.wso2.carbon.identity.entitlement.ui.EntitlementPolicyConstants;

public class IdentityXACMLValidatorServiceClient {

    private static Log log = LogFactory.getLog(IdentityXACMLValidatorServiceClient.class);

    private IdentityXACMLValidatorServiceStub stub;

    /**
     * 
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws AxisFault
     */
    public IdentityXACMLValidatorServiceClient(String cookie, String backendServerURL, ConfigurationContext configCtx)
                                                                                                                      throws AxisFault {
        try {
            String serviceURL = backendServerURL + "IdentityXACMLValidatorService";
            stub = new IdentityXACMLValidatorServiceStub(configCtx, serviceURL);
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (AxisFault ex) {
            log.error("Error generating stub for IdentityXACMLValidatorService", ex);
            throw new AxisFault("Error generating stub for IdentityXACMLValidatorService", ex);
        }
    }

    /**
     * 
     * @param policy
     * @return
     * @throws AxisFault
     */
    public ErrorItem[] validateXACMLPolicy(PolicyDTO policy) throws Exception {
        ErrorItem[] errorItems = null;
        try {
            errorItems = stub.validateXACMLPolicy(policy);
        } catch (Exception e) {
            throw e;
        }
        return errorItems;
    }

}
