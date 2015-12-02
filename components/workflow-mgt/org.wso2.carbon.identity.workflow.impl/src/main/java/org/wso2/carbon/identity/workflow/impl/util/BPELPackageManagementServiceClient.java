/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.impl.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bpel.stub.mgt.BPELPackageManagementServiceStub;
import org.wso2.carbon.bpel.stub.mgt.PackageManagementException;
import org.wso2.carbon.bpel.stub.mgt.types.DeployedPackagesPaginated;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;

import javax.xml.stream.XMLStreamException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class BPELPackageManagementServiceClient {

    private static final Log log = LogFactory.getLog(BPELPackageManagementServiceClient.class);

    private BPELPackageManagementServiceStub stub;

    public BPELPackageManagementServiceClient(String bpsURL, String username, char[] password) throws AxisFault {
        stub = new BPELPackageManagementServiceStub(bpsURL + WFImplConstant.BPS_PACKAGE_SERVICES_URL);
        ServiceClient serviceClient = stub._getServiceClient();
        Options options = serviceClient.getOptions();
        HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
        auth.setUsername(username);
        auth.setPassword(new String(password));
        auth.setPreemptiveAuthentication(true);
        List<String> authSchemes = new ArrayList<>();
        authSchemes.add(HttpTransportProperties.Authenticator.BASIC);
        auth.setAuthSchemes(authSchemes);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
        serviceClient.setOptions(options);
    }

    public BPELPackageManagementServiceClient(String bpsURL, String username) throws AxisFault {

        stub = new BPELPackageManagementServiceStub(bpsURL + WFImplConstant.BPS_PACKAGE_SERVICES_URL);
        ServiceClient serviceClient = stub._getServiceClient();
        OMElement mutualSSLHeader;
        try {
            String headerString = WFImplConstant.MUTUAL_SSL_HEADER.replaceAll("\\$username", username);
            mutualSSLHeader = AXIOMUtil.stringToOM(headerString);
            serviceClient.addHeader(mutualSSLHeader);
        } catch (XMLStreamException e) {
            throw new AxisFault("Error while creating mutualSSLHeader XML Element.", e);
        }
        Options options = serviceClient.getOptions();
        serviceClient.setOptions(options);
    }

    /**
     * This method list deployed BPS packages
     *
     * @param page
     * @param packageSearchString
     * @return
     * @throws PackageManagementException
     * @throws RemoteException
     */
    public DeployedPackagesPaginated listDeployedPackagesPaginated(int page, String packageSearchString)
            throws PackageManagementException, RemoteException {
        try {
            return stub.listDeployedPackagesPaginated(page, packageSearchString);
        } catch (RemoteException e) {
            log.error("listDeployedPackagesPaginated operation invocation failed.", e);
            throw e;
        } catch (PackageManagementException e) {
            log.error("listDeployedPackagesPaginated operation failed.", e);
            throw e;
        }
    }

}
