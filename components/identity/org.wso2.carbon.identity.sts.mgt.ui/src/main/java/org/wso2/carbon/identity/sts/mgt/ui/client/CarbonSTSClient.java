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
package org.wso2.carbon.identity.sts.mgt.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.sts.mgt.stub.generic.STSAdminServiceStub;
import org.wso2.carbon.identity.sts.mgt.stub.service.util.xsd.TrustedServiceData;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class CarbonSTSClient {

    private STSAdminServiceStub stub;
    private String serviceEndPoint;
    private ConfigurationContext configContext = null;
    private static final Log log = LogFactory.getLog(CarbonSTSClient.class);

    /**
     * Initializes STSUtil
     * 
     * @param cookie Cookie string
     * @throws Exception
     */
    public CarbonSTSClient(ServletConfig config, HttpSession session, String cookie)
            throws Exception {
        ServiceClient client = null;
        Options option = null;
        String serverUrl = null;

        // Obtaining the client-side ConfigurationContext instance.
        configContext = (ConfigurationContext) config.getServletContext().getAttribute(
                CarbonConstants.CONFIGURATION_CONTEXT);

        // Server URL which is defined in the server.xml
        serverUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);

        this.serviceEndPoint = serverUrl + "STSAdminService";
        try {
            this.stub = new STSAdminServiceStub(configContext, serviceEndPoint);
        } catch (AxisFault e) {
            log.error("Error while creating STSAdminServiceStub", e);
            throw new Exception(e);
        }
        client = stub._getServiceClient();
        option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * Returns the key aliases from the primary key store.
     * 
     * @return A set of key aliases from the primary key store.
     * @throws Exception
     */
    public String[] getAliasFromPrimaryKeystore() throws Exception {
        try {
            return stub.getCertAliasOfPrimaryKeyStore();
        } catch (Exception e) {
            log.error("Error while retrieving primary key stores", e);
            throw e;
        }
    }

    /**
     * Returns services already added as trusted services.
     * 
     * @return A set of services already added as trusted services.
     * @throws Exception
     */
    public TrustedServiceData[] getTrustedServices() throws Exception {
        try {
            return stub.getTrustedServices();
        } catch (Exception e) {
            log.error("Error while retrieving trusted services", e);
            throw e;
        }
    }

    /**
     * Adds an end point URL of a service as a trusted
     * 
     * @param endpoint The end point URL of the service
     * @param keyAlias Key alias used from the primary key store.
     * @throws Exception
     */
    public void addTrustedService(String endpoint, String keyAlias) throws Exception {
        try {
            if (endpoint != null && endpoint.trim().length() > 0 && keyAlias != null
                    && keyAlias.trim().length() > 0) {
                stub.addTrustedService(endpoint.trim(), keyAlias);
            }
        } catch (Exception e) {
            log.error("Error while adding trusted services", e);
            throw e;
        }
    }

    /**
     * 
     * @param endpoint
     * @throws Exception
     */
    public void removeTrustedService(String endpoint) throws Exception {
        try {
            if (endpoint != null && endpoint.trim().length() > 0) {
                stub.removeTrustedService(endpoint);
            }
        } catch (Exception e) {
            log.error("Error while removing trusted services", e);
            throw e;
        }
    }

    private boolean isValidCookieSet() {
        if (stub._getServiceClient().getOptions().getProperty(
                org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING) != null) {
            return true;
        }
        return false;
    }
}
