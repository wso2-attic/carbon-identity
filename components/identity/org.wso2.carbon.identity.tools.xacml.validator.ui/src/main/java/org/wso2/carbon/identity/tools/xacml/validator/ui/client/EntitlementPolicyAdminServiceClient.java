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

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.stub.EntitlementPolicyAdminServiceEntitlementException;
import org.wso2.carbon.identity.entitlement.stub.EntitlementPolicyAdminServiceStub;

public class EntitlementPolicyAdminServiceClient {

    private EntitlementPolicyAdminServiceStub stub;

    private static final Log log = LogFactory.getLog(EntitlementPolicyAdminServiceClient.class);

    /**
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws AxisFault
     */
    public EntitlementPolicyAdminServiceClient(String cookie, String backendServerURL, ConfigurationContext configCtx)
            throws AxisFault {
        String serviceURL = backendServerURL + "EntitlementPolicyAdminService";
        stub = new EntitlementPolicyAdminServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public String importPolicyContentFromRegistry(String policyRegistryPath) throws AxisFault {
        String policy = null;
        try {
            policy = stub.importPolicyContentFromRegistry(policyRegistryPath);
        } catch (Exception e) {
            handleException(e);
        }
        return policy;
    }


    private void handleException(Exception e) throws AxisFault {

        String errorMessage = "Unknown";

        if (e instanceof EntitlementPolicyAdminServiceEntitlementException) {
            EntitlementPolicyAdminServiceEntitlementException entitlementException =
                    (EntitlementPolicyAdminServiceEntitlementException) e;
            if (entitlementException.getFaultMessage().getEntitlementException() != null) {
                errorMessage = entitlementException.getFaultMessage().getEntitlementException().getMessage();
            }
        } else {
            errorMessage = e.getMessage();
        }

        throw new AxisFault(errorMessage, e);
    }
}
