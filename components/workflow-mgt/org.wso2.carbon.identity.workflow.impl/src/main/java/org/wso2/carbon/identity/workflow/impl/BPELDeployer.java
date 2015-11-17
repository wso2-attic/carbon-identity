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

package org.wso2.carbon.identity.workflow.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.impl.util.WorkflowDeployerClient;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowRuntimeException;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.mgt.workflow.TemplateInitializer;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BPELDeployer implements TemplateInitializer {

    private static Log log = LogFactory.getLog(BPELDeployer.class);

    private BPSProfile bpsProfile = null;

    private String processName;
    private String htName;

    private String role;
    private String tenantContext = "" ;

    @Override
    public boolean initNeededAtStartUp() {

        return false;
    }

    @Override
    public void initialize(List<Parameter> parameterList) throws WorkflowImplException {

        if (!validateParams(parameterList)) {
            throw new WorkflowRuntimeException("Workflow initialization failed, required parameter is missing");
        }

        Parameter wfNameParameter = WorkflowManagementUtil
                .getParameter(parameterList, WFConstant.ParameterName.WORKFLOW_NAME, WFConstant.ParameterHolder
                        .WORKFLOW_IMPL);

        if (wfNameParameter != null) {
            processName = StringUtils.deleteWhitespace(wfNameParameter.getParamValue());
            role = WorkflowManagementUtil
                    .createWorkflowRoleName(StringUtils.deleteWhitespace(wfNameParameter.getParamValue()));
        }

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if(!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)){
            tenantContext = "t/" + tenantDomain + "/";
        }

        Parameter bpsProfileParameter = WorkflowManagementUtil
                .getParameter(parameterList, WFImplConstant.ParameterName.BPS_PROFILE, WFConstant.ParameterHolder
                        .WORKFLOW_IMPL);
        if (bpsProfileParameter != null) {
            String bpsProfileName = bpsProfileParameter.getParamValue();
            bpsProfile = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService().getBPSProfile
                    (bpsProfileName, tenantId);
        }
        htName = processName + BPELDeployer.Constants.HT_SUFFIX;

        generateAndDeployArtifacts();
    }

    private boolean validateParams(List<Parameter> parameterList) {
        //todo: implement
        return true;
    }


    public void generateAndDeployArtifacts() throws WorkflowImplException {

        try {
            generateProcessArtifact();
            generateHTArtifact();
        } catch (IOException e) {
            throw new WorkflowImplException("Error when generating process artifacts");
        }

        try {
            deployArtifacts();
        } catch (RemoteException e) {
            throw new WorkflowRuntimeException("Error occurred when deploying the BPEL");
        }
    }

    private void deployArtifacts() throws RemoteException {

        String bpelArchiveName = processName + BPELDeployer.Constants.ZIP_EXT;
        String archiveHome = System.getProperty(BPELDeployer.Constants.TEMP_DIR_PROPERTY) + File.separator;
        DataSource bpelDataSource = new FileDataSource(archiveHome + bpelArchiveName);

        WorkflowDeployerClient workflowDeployerClient;
        if (bpsProfile.getProfileName().equals(WFImplConstant.DEFAULT_BPS_PROFILE_NAME)) {
            workflowDeployerClient = new WorkflowDeployerClient(bpsProfile.getManagerHostURL(),
                    bpsProfile.getUsername());
        } else {
            workflowDeployerClient = new WorkflowDeployerClient(bpsProfile.getManagerHostURL(),
                    bpsProfile.getUsername(), bpsProfile.getPassword().toCharArray());
        }
        workflowDeployerClient.uploadBPEL(getBPELUploadedFileItem(new DataHandler(bpelDataSource),
                                                                  bpelArchiveName, BPELDeployer.Constants.ZIP_TYPE));
        String htArchiveName = htName + BPELDeployer.Constants.ZIP_EXT;
        DataSource htDataSource = new FileDataSource(archiveHome + htArchiveName);
        workflowDeployerClient.uploadHumanTask(getHTUploadedFileItem(new DataHandler(htDataSource), htArchiveName,
                                                                     BPELDeployer.Constants.ZIP_TYPE));
    }

    private UploadedFileItem[] getBPELUploadedFileItem(DataHandler dataHandler, String fileName,
                                                       String fileType) {

        UploadedFileItem[] uploadedFileItems = new UploadedFileItem[1];
        UploadedFileItem uploadedFileItem = new UploadedFileItem();
        uploadedFileItem.setDataHandler(dataHandler);
        uploadedFileItem.setFileName(fileName);
        uploadedFileItem.setFileType(fileType);
        uploadedFileItems[0] = uploadedFileItem;
        return uploadedFileItems;
    }

    private org.wso2.carbon.humantask.stub.upload.types.UploadedFileItem[] getHTUploadedFileItem(
            DataHandler dataHandler,
            String fileName,
            String fileType) {

        org.wso2.carbon.humantask.stub.upload.types.UploadedFileItem[] uploadedFileItems = new org.wso2.carbon
                .humantask.stub.upload.types.UploadedFileItem[1];
        org.wso2.carbon.humantask.stub.upload.types.UploadedFileItem uploadedFileItem =
                new org.wso2.carbon.humantask.stub.upload.types.UploadedFileItem();
        uploadedFileItem.setDataHandler(dataHandler);
        uploadedFileItem.setFileName(fileName);
        uploadedFileItem.setFileType(fileType);
        uploadedFileItems[0] = uploadedFileItem;
        return uploadedFileItems;
    }

    private Map<String, String> getPlaceHolderValues() {

        Map<String, String> placeHolderValues = new HashMap<>();
        placeHolderValues.put(BPELDeployer.Constants.BPEL_PROCESS_NAME, processName);
        placeHolderValues.put(BPELDeployer.Constants.HT_SERVICE_NAME, htName);
        String url = bpsProfile.getWorkerHostURL() != null ? bpsProfile.getWorkerHostURL() : "" ;
        if (url.endsWith("/")) {
            url = url.substring(0,url.lastIndexOf("/"));
        }
        placeHolderValues.put(BPELDeployer.Constants.BPS_HOST_NAME, url);
        placeHolderValues.put(Constants.URL_TENANT_CONTEXT, tenantContext);
        placeHolderValues.put(BPELDeployer.Constants.CARBON_HOST_NAME, IdentityUtil.getServerURL("", true));
        placeHolderValues.put(BPELDeployer.Constants.CARBON_CALLBACK_AUTH_USER, (bpsProfile.getCallbackUser() != null ?
                bpsProfile.getCallbackUser() : ""));
        placeHolderValues
                .put(BPELDeployer.Constants.CARBON_CALLBACK_AUTH_PASSWORD, (bpsProfile.getCallbackPassword() != null ?
                        bpsProfile.getCallbackPassword() : ""));
        placeHolderValues.put(BPELDeployer.Constants.HT_OWNER_ROLE, role);
        placeHolderValues.put(BPELDeployer.Constants.HT_ADMIN_ROLE, role);
        return placeHolderValues;
    }

    private void removePlaceHolders(String relativeFilePath, String destination) throws IOException {

        InputStream inputStream = getClass().getResourceAsStream("/" + relativeFilePath);
        String content = IOUtils.toString(inputStream);
        for (Map.Entry<String, String> placeHolderEntry : getPlaceHolderValues().entrySet()) {
            content = content.replaceAll(Pattern.quote(placeHolderEntry.getKey()), Matcher.quoteReplacement
                    (placeHolderEntry.getValue()));
        }
        File destinationParent = new File(destination).getParentFile();
        if (!destinationParent.exists()) {
            destinationParent.mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(new File(destination), false);
        IOUtils.write(content, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
        inputStream.close();
    }

    private void generateProcessArtifact() throws IOException {

        Set<String> filesToAdd = new HashSet<>();
        String taskWsdl, ws_humantask = null; //to keep without deleting for human task
        String resourceHomePath =
                BPELDeployer.Constants.TEMPLATE_RESOURCE_LOCATION + File.separator +
                BPELDeployer.Constants.BPEL_RESOURCE_LOCATION +
                File.separator + BPELDeployer.Constants.APPROVAL_SERVICE_RESOURCE_LOCATION + File.separator;
        String outputPath = System.getProperty(BPELDeployer.Constants.TEMP_DIR_PROPERTY) + File.separator;
        //process.wsdl
        String outputFile = outputPath + processName + BPELDeployer.Constants.WSDL_EXT;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.PROCESS_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //process.bpel
        outputFile = outputPath + processName + BPELDeployer.Constants.BPEL_EXT;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.PROCESS_BPEL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //callback.wsdl
        outputFile = outputPath + BPELDeployer.Constants.CALLBACK_WSDL_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.CALLBACK_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task.wsdl
        outputFile = outputPath + htName + BPELDeployer.Constants.SERVICE_TXT + BPELDeployer.Constants.WSDL_EXT;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.TASK_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        taskWsdl = outputFile;
        //callback.epr
        outputFile = outputPath + BPELDeployer.Constants.CALLBACK_EPR_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.CALLBACK_EPR_FILE, outputFile);
        filesToAdd.add(outputFile);
        //deploy.xml
        outputFile = outputPath + BPELDeployer.Constants.DEPLOY_XML_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.DEPLOY_XML_FILE, outputFile);
        filesToAdd.add(outputFile);

        //taskService.epr
        outputFile = outputPath + BPELDeployer.Constants.TASK_SERVICE_EPR_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.TASK_SERVICE_EPR_FILE, outputFile);
        filesToAdd.add(outputFile);

        //ws-humantask.xsd
        outputFile = outputPath + BPELDeployer.Constants.WS_HUMAN_TASK_XSD_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.WS_HUMAN_TASK_XSD_FILE, outputFile);
        filesToAdd.add(outputFile);
        ws_humantask = outputFile;

        //ws-humantask-types.xsd
        outputFile = outputPath + BPELDeployer.Constants.WS_HUMAN_TASK_TYPE_XSD_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.WS_HUMAN_TASK_TYPE_XSD_FILE, outputFile);
        filesToAdd.add(outputFile);

        FileOutputStream zipFOS =
                new FileOutputStream(outputPath + processName + BPELDeployer.Constants.ZIP_EXT);
        ZipOutputStream zipOutputStream = new ZipOutputStream(zipFOS);
        for (String fileName : filesToAdd) {
            addToZipFile(fileName, zipOutputStream);
        }
        zipOutputStream.close();
        zipFOS.close();
        for (String fileName : filesToAdd) {
            File file = new File(fileName);
            if (file.exists() && file.isFile() && !fileName.equals(taskWsdl) && !fileName.equals(ws_humantask)) {
                boolean deleteSuccess = file.delete();
                if (!deleteSuccess) {
                    log.warn("Temporary file " + fileName + " deletion failed.");
                }
            }
        }
    }

    private void generateHTArtifact() throws IOException {

        Set<String> filesToAdd = new HashSet<>();
        String resourceHomePath =
                BPELDeployer.Constants.TEMPLATE_RESOURCE_LOCATION + File.separator +
                BPELDeployer.Constants.HT_RESOURCE_LOCATION +
                File.separator + BPELDeployer.Constants.APPROVAL_HT_RESOURCE_LOCATION + File.separator;
        String outputPath = System.getProperty(BPELDeployer.Constants.TEMP_DIR_PROPERTY) + File.separator;
        //task.ht
        String outputFile = outputPath + htName + BPELDeployer.Constants.HT_EXT;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.TASK_HT_FILE, outputFile);
        filesToAdd.add(outputFile);
        //htconfig.xml
        outputFile = outputPath + BPELDeployer.Constants.HTCONFIG_XML_FILE;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.HTCONFIG_XML_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-input.jsp
        outputFile =
                outputPath + BPELDeployer.Constants.APPROVAL_JSP_LOCATION + File.separator + htName + BPELDeployer
                        .Constants.INPUT_JSP_SUFFIX;
        File outputFileParent = new File(outputFile).getParentFile();
        if (!outputFileParent.exists()) {
            outputFileParent.mkdirs();
        }
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.APPROVAL_JSP_LOCATION + File.separator +
                           BPELDeployer.Constants.TASK_INPUT_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-output.jsp
        outputFile =
                outputPath + BPELDeployer.Constants.APPROVAL_JSP_LOCATION + File.separator + htName +
                BPELDeployer.Constants.OUTPUT_JSP_SUFFIX;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.APPROVAL_JSP_LOCATION + File.separator +
                           BPELDeployer.Constants.TASK_OUTPUT_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-response.jsp
        outputFile =
                outputPath + BPELDeployer.Constants.APPROVAL_JSP_LOCATION + File.separator + htName +
                BPELDeployer.Constants.RESPONSE_JSP_SUFFIX;
        removePlaceHolders(resourceHomePath + BPELDeployer.Constants.APPROVAL_JSP_LOCATION + File.separator +
                           BPELDeployer.Constants.TASK_RESPONSE_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);

        //created from process
        filesToAdd.add(outputPath + htName + BPELDeployer.Constants.SERVICE_TXT + BPELDeployer.Constants.WSDL_EXT);
        filesToAdd.add(outputPath + BPELDeployer.Constants.WS_HUMAN_TASK_XSD_FILE);

        FileOutputStream zipFOS =
                new FileOutputStream(outputPath + htName + BPELDeployer.Constants.ZIP_EXT);
        ZipOutputStream zipOutputStream = new ZipOutputStream(zipFOS);
        for (String fileName : filesToAdd) {
            addToZipFile(fileName, zipOutputStream);
        }
        zipOutputStream.close();
        zipFOS.close();
        for (String fileName : filesToAdd) {
            File file = new File(fileName);
            if (file.exists() && file.isFile()) {
                boolean deleteSuccess = file.delete();
                if (!deleteSuccess) {
                    log.warn("Temporary file " + fileName + " deletion failed.");
                }
            }
        }
    }

    private void addToZipFile(String fileName, ZipOutputStream zos) throws IOException {

        File zipRootPath = new File(System.getProperty(BPELDeployer.Constants.TEMP_DIR_PROPERTY));
        File file = new File(fileName);
        String relativePath = zipRootPath.toURI().relativize(file.toURI()).getPath();
        ZipEntry zipEntry = new ZipEntry(relativePath);
        zos.putNextEntry(zipEntry);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        } finally {
            zos.closeEntry();
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error("Error when closing the file input stream for " + fileName);
                }
            }
        }
    }

    private static class Constants {

        private static final String BPEL_PROCESS_NAME = "${bpelProcessName}";
        private static final String HT_SERVICE_NAME = "${htServiceName}";
        private static final String BPS_HOST_NAME = "${bpsURL}";
        private static final String URL_TENANT_CONTEXT = "${tenantContext}";
        private static final String CARBON_HOST_NAME = "${carbonHostName}";
        private static final String CARBON_CALLBACK_AUTH_USER = "${carbonUserName}";
        private static final String CARBON_CALLBACK_AUTH_PASSWORD = "${carbonUserPassword}";
        private static final String HT_SUBJECT = "${htSubject}";
        private static final String HT_DESCRIPTION = "${htDescription}";
        private static final String HT_OWNER_ROLE = "${htOwnerRole}";
        private static final String HT_ADMIN_ROLE = "${htAdminRole}";

        private static final String PROCESS_BPEL_FILE = "ApprovalProcess.bpel";
        private static final String PROCESS_WSDL_FILE = "ApprovalProcessArtifacts.wsdl";
        private static final String TASK_WSDL_FILE = "ApprovalTaskService.wsdl";
        private static final String CALLBACK_WSDL_FILE = "CallbackService.wsdl";
        private static final String CALLBACK_EPR_FILE = "callbackService.epr";
        private static final String DEPLOY_XML_FILE = "deploy.xml";
        private static final String TASK_SERVICE_EPR_FILE = "taskService.epr";
        private static final String WS_HUMAN_TASK_XSD_FILE = "ws-humantask.xsd";
        private static final String WS_HUMAN_TASK_TYPE_XSD_FILE = "ws-humantask-types.xsd";

        private static final String HTCONFIG_XML_FILE = "htconfig.xml";
        private static final String TASK_HT_FILE = "ApprovalTask.ht";
        private static final String TASK_INPUT_JSP_FILE = "ApprovalTask-input.jsp";
        private static final String TASK_OUTPUT_JSP_FILE = "ApprovalTask-output.jsp";
        private static final String TASK_RESPONSE_JSP_FILE = "ApprovalTask-response.jsp";

        private static final String TEMPLATE_RESOURCE_LOCATION = "templates";
        private static final String BPEL_RESOURCE_LOCATION = "bpel";
        private static final String HT_RESOURCE_LOCATION = "humantask";
        private static final String APPROVAL_SERVICE_RESOURCE_LOCATION = "SimpleApprovalService";
        private static final String APPROVAL_HT_RESOURCE_LOCATION = "SimpleApprovalTask";
        private static final String APPROVAL_JSP_LOCATION = "web";
        private static final String SERVICE_TXT = "Service";

        private static final String WSDL_EXT = ".wsdl";
        private static final String BPEL_EXT = ".bpel";
        private static final String ZIP_EXT = ".zip";
        private static final String XSD_EXT = ".xsd";
        private static final String HT_EXT = ".ht";
        private static final String HT_SUFFIX = "Task";
        private static final String INPUT_JSP_SUFFIX = "-input.jsp";
        private static final String OUTPUT_JSP_SUFFIX = "-output.jsp";
        private static final String RESPONSE_JSP_SUFFIX = "-response.jsp";

        private static final String TEMP_DIR_PROPERTY = "java.io.tmpdir";
        private static final String ZIP_TYPE = "zip";
    }
}
