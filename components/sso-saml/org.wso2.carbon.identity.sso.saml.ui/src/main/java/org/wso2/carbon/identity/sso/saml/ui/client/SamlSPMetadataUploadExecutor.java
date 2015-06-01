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

package org.wso2.carbon.identity.sso.saml.ui.client;

import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO;
import org.wso2.carbon.ui.CarbonUIMessage;
import org.wso2.carbon.ui.transports.fileupload.AbstractFileUploadExecutor;
import org.wso2.carbon.utils.FileItemData;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SamlSPMetadataUploadExecutor extends AbstractFileUploadExecutor {

    private static final String[] ALLOWED_FILE_EXTENSIONS = new String[]{".xml"};

    private String errorRedirectionPage;

    @Override
    public boolean execute(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws CarbonException, IOException {

        String webContext = (String) httpServletRequest.getAttribute(CarbonConstants.WEB_CONTEXT);
        String serverURL = (String) httpServletRequest.getAttribute(CarbonConstants.SERVER_URL);
        String cookie = (String) httpServletRequest.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String spName = (String) httpServletRequest.getSession().getAttribute("application-sp-name");
        httpServletRequest.getSession().removeAttribute("application-sp-name");
        log.info(spName);
        errorRedirectionPage = getContextRoot(httpServletRequest) + "/" + webContext
                + "/sso-saml/add_service_provider.jsp?spName="+spName;

        Map<String, ArrayList<FileItemData>> fileItemsMap = getFileItemsMap();
        if (fileItemsMap == null || fileItemsMap.isEmpty()) {
            String msg = "File uploading failed. No files are specified";
            log.error(msg);
            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, httpServletRequest,
                    httpServletResponse, errorRedirectionPage);
            return false;
        }

        SAMLSSOConfigServiceClient client =
                new SAMLSSOConfigServiceClient(cookie, serverURL, configurationContext);
        List<FileItemData> fileItems = fileItemsMap.get("metadataFromFileSystem");
        String msg;
        try {
            SAMLSSOServiceProviderDTO serviceProviderDTO = null;
            for (FileItemData fileItem : fileItems) {
                String filename = getFileName(fileItem.getFileItem().getName());
                checkServiceFileExtensionValidity(filename, ALLOWED_FILE_EXTENSIONS);

                if (!filename.endsWith(".xml")) {
                    throw new CarbonException("File with extension " +
                            getFileName(fileItem.getFileItem().getName()) + " is not supported!");
                } else {
                    BufferedReader br = new BufferedReader(new InputStreamReader(fileItem.getDataHandler().getInputStream()));
                    String temp;
                    String policyContent = "";
                    while ((temp = br.readLine()) != null) {
                        policyContent += temp;
                    }
                    if (!"".equals(policyContent)) {
                        serviceProviderDTO = client.uploadServiceProvider(policyContent);
                    }
                }
            }

            if (serviceProviderDTO != null){
                httpServletResponse.setContentType("text/html; charset=utf-8");
                msg = "Metadata have been uploaded successfully.";
                String attributeConsumingServiceIndex = "";
                if (serviceProviderDTO.getAttributeConsumingServiceIndex() != null){
                    attributeConsumingServiceIndex = serviceProviderDTO.getAttributeConsumingServiceIndex();
                }

                CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.INFO, httpServletRequest,
                        httpServletResponse, getContextRoot(httpServletRequest)
                                + "/" + webContext + "/application/configure-service-provider.jsp?action=update&display=samlIssuer&spName="+spName+"&samlIssuer="+serviceProviderDTO.getIssuer()+"&attrConServIndex="+attributeConsumingServiceIndex);
                return true;
            }else{
                msg = "Metadata uploading failed. ";
                log.error(msg);
                CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, httpServletRequest,
                        httpServletResponse, errorRedirectionPage);
            }

        } catch (Exception e) {
            msg = "Metadata uploading failed. " + e.getMessage();
            log.error(msg);
            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, httpServletRequest,
                    httpServletResponse, errorRedirectionPage);
        }
        return false;
    }

    @Override
    protected String getErrorRedirectionPage() {
        return errorRedirectionPage;
    }
}
