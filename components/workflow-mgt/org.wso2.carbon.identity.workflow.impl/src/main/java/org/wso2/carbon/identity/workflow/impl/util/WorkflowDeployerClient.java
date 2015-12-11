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
import org.wso2.carbon.bpel.stub.upload.BPELUploaderStub;
import org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem;
import org.wso2.carbon.humantask.stub.upload.HumanTaskUploaderStub;
import org.wso2.carbon.identity.workflow.impl.WFImplConstant;

import javax.xml.stream.XMLStreamException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class WorkflowDeployerClient {
    private static final String BPEL_UPLOADER_SERVICE = "/BPELUploader";
    private final static String HT_UPLOADER_SERVICE = "/HumanTaskUploader";

    private BPELUploaderStub bpelUploaderStub;
    private HumanTaskUploaderStub humanTaskUploaderStub;

    public WorkflowDeployerClient(String bpsURL, String username, char[] password) throws AxisFault {
        bpelUploaderStub = new BPELUploaderStub(bpsURL + BPEL_UPLOADER_SERVICE);
        ServiceClient serviceClient = bpelUploaderStub._getServiceClient();
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

        humanTaskUploaderStub = new HumanTaskUploaderStub(bpsURL + HT_UPLOADER_SERVICE);
        ServiceClient htServiceClient = humanTaskUploaderStub._getServiceClient();
        Options htOptions = htServiceClient.getOptions();
        htOptions.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
        htServiceClient.setOptions(htOptions);
    }

    public WorkflowDeployerClient(String bpsURL, String username) throws AxisFault {

        bpelUploaderStub = new BPELUploaderStub(bpsURL + BPEL_UPLOADER_SERVICE);
        ServiceClient serviceClient = bpelUploaderStub._getServiceClient();
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

        humanTaskUploaderStub = new HumanTaskUploaderStub(bpsURL + HT_UPLOADER_SERVICE);
        ServiceClient htServiceClient = humanTaskUploaderStub._getServiceClient();
        Options htOptions = htServiceClient.getOptions();
        htServiceClient.setOptions(htOptions);
        htServiceClient.addHeader(mutualSSLHeader);
    }

    /**
     * Upload BPEL artifacts
     *
     * @param fileItems Artifacts todeploy
     * @throws RemoteException
     */
    public void uploadBPEL(UploadedFileItem[] fileItems) throws RemoteException {
        bpelUploaderStub.uploadService(fileItems);
    }

    /**
     * Upload human task artifacts
     *
     * @param fileItems Artifacts to deploy
     * @throws RemoteException
     */
    public void uploadHumanTask(org.wso2.carbon.humantask.stub.upload.types.UploadedFileItem[] fileItems)
            throws RemoteException {
        humanTaskUploaderStub.uploadHumanTask(fileItems);
    }

}
