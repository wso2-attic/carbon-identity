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

package org.wso2.carbon.identity.workflow.mgt.template.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem;
import org.wso2.carbon.identity.workflow.mgt.util.WorkFlowConstants;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDeployerClient;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.RuntimeWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.template.TemplateInitializer;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;

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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BPELApprovalDeployer implements TemplateInitializer {

    private static Log log = LogFactory.getLog(BPELApprovalDeployer.class);

    private String processName;
    private String bpsHost;
    private String bpsUser;
    private String password;
    private String htName;
    private String htSubject;
    private String htBody;
    private String callBackUser;
    private String callBackUserPassword;

    private String role;

    @Override
    public boolean initNeededAtStartUp() {

        return false;
    }

    @Override
    public void initialize(Map<String, Object> initParams) throws WorkflowException {

        if (!validateParams(initParams)) {
            throw new RuntimeWorkflowException("Workflow initialization failed, required parameter is missing");
        }
        processName = (String) initParams.get(WorkFlowConstants.TemplateConstants.WORKFLOW_NAME);
        bpsHost = (String) initParams.get(WorkFlowConstants.TemplateConstants.HOST);
        bpsUser = (String) initParams.get(WorkFlowConstants.TemplateConstants.AUTH_USER);
        password = (String) initParams.get(WorkFlowConstants.TemplateConstants.AUTH_USER_PASSWORD);
        callBackUser = (String) initParams.get(WorkFlowConstants.TemplateConstants.CALLBACK_USER);
        callBackUserPassword = (String) initParams.get(WorkFlowConstants.TemplateConstants.CALLBACK_USER_PASSWORD);
        role = WorkflowManagementUtil.getWorkflowRoleName((String) initParams.get(WorkFlowConstants.TemplateConstants.WORKFLOW_NAME));


        htName = processName + Constants.HT_SUFFIX;
        generateAndDeployArtifacts();
    }

    private boolean validateParams(Map<String, Object> initParams) {
        //todo: implement
        return true;
    }


    public void generateAndDeployArtifacts() throws WorkflowException {

        try {
            generateProcessArtifact();
            generateHTArtifact();
        } catch (IOException e) {
            throw new InternalWorkflowException("Error when generating process artifacts");
        }

        try {
            deployArtifacts();
        } catch (RemoteException e) {
            throw new RuntimeWorkflowException("Error occurred when deploying the BPEL");
        }
    }

    private void deployArtifacts() throws RemoteException {

        String bpelArchiveName = processName + Constants.ZIP_EXT;
        String archiveHome = System.getProperty(Constants.TEMP_DIR_PROPERTY) + File.separator;
        DataSource bpelDataSource = new FileDataSource(archiveHome + bpelArchiveName);
        WorkflowDeployerClient workflowDeployerClient = new WorkflowDeployerClient(bpsHost,
                bpsUser, password.toCharArray());
        workflowDeployerClient.uploadBPEL(getBPELUploadedFileItem(new DataHandler(bpelDataSource),
                bpelArchiveName, Constants.ZIP_TYPE));
        String htArchiveName = htName + Constants.ZIP_EXT;
        DataSource htDataSource = new FileDataSource(archiveHome + htArchiveName);
        workflowDeployerClient.uploadHumanTask(getHTUploadedFileItem(new DataHandler(htDataSource), htArchiveName,
                Constants.ZIP_TYPE));
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
        placeHolderValues.put(Constants.BPEL_PROCESS_NAME, processName);
        placeHolderValues.put(Constants.HT_SERVICE_NAME, htName);
        placeHolderValues.put(Constants.BPS_HOST_NAME, bpsHost);
        placeHolderValues.put(Constants.CARBON_HOST_NAME, Constants.CARBON_HOST_URL);
        placeHolderValues.put(Constants.CARBON_CALLBACK_AUTH_USER, callBackUser);
        placeHolderValues.put(Constants.CARBON_CALLBACK_AUTH_PASSWORD, callBackUserPassword);

        placeHolderValues.put(Constants.HT_OWNER_ROLE, role);
        placeHolderValues.put(Constants.HT_ADMIN_ROLE, role);
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
                Constants.TEMPLATE_RESOURCE_LOCATION + File.separator + Constants.BPEL_RESOURCE_LOCATION +
                        File.separator + Constants.APPROVAL_SERVICE_RESOURCE_LOCATION + File.separator;
        String outputPath = System.getProperty(Constants.TEMP_DIR_PROPERTY) + File.separator;
        //process.wsdl
        String outputFile = outputPath + processName + Constants.WSDL_EXT;
        removePlaceHolders(resourceHomePath + Constants.PROCESS_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //process.bpel
        outputFile = outputPath + processName + Constants.BPEL_EXT;
        removePlaceHolders(resourceHomePath + Constants.PROCESS_BPEL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //callback.wsdl
        outputFile = outputPath + Constants.CALLBACK_WSDL_FILE;
        removePlaceHolders(resourceHomePath + Constants.CALLBACK_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task.wsdl
        outputFile = outputPath + htName + Constants.SERVICE_TXT + Constants.WSDL_EXT;
        removePlaceHolders(resourceHomePath + Constants.TASK_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        taskWsdl = outputFile;
        //callback.epr
        outputFile = outputPath + Constants.CALLBACK_EPR_FILE;
        removePlaceHolders(resourceHomePath + Constants.CALLBACK_EPR_FILE, outputFile);
        filesToAdd.add(outputFile);
        //deploy.xml
        outputFile = outputPath + Constants.DEPLOY_XML_FILE;
        removePlaceHolders(resourceHomePath + Constants.DEPLOY_XML_FILE, outputFile);
        filesToAdd.add(outputFile);

        //taskService.epr
        outputFile = outputPath + Constants.TASK_SERVICE_EPR_FILE;
        removePlaceHolders(resourceHomePath + Constants.TASK_SERVICE_EPR_FILE, outputFile);
        filesToAdd.add(outputFile);

        //ws-humantask.xsd
        outputFile = outputPath + Constants.WS_HUMAN_TASK_XSD_FILE;
        removePlaceHolders(resourceHomePath + Constants.WS_HUMAN_TASK_XSD_FILE, outputFile);
        filesToAdd.add(outputFile);
        ws_humantask = outputFile ;

        //ws-humantask-types.xsd
        outputFile = outputPath + Constants.WS_HUMAN_TASK_TYPE_XSD_FILE;
        removePlaceHolders(resourceHomePath + Constants.WS_HUMAN_TASK_TYPE_XSD_FILE, outputFile);
        filesToAdd.add(outputFile);

        FileOutputStream zipFOS =
                new FileOutputStream(outputPath + processName + Constants.ZIP_EXT);
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
                Constants.TEMPLATE_RESOURCE_LOCATION + File.separator + Constants.HT_RESOURCE_LOCATION +
                        File.separator + Constants.APPROVAL_HT_RESOURCE_LOCATION + File.separator;
        String outputPath = System.getProperty(Constants.TEMP_DIR_PROPERTY) + File.separator;
        //task.ht
        String outputFile = outputPath + htName + Constants.HT_EXT;
        removePlaceHolders(resourceHomePath + Constants.TASK_HT_FILE, outputFile);
        filesToAdd.add(outputFile);
        //htconfig.xml
        outputFile = outputPath + Constants.HTCONFIG_XML_FILE;
        removePlaceHolders(resourceHomePath + Constants.HTCONFIG_XML_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-input.jsp
        outputFile =
                outputPath + Constants.APPROVAL_JSP_LOCATION + File.separator + htName + Constants.INPUT_JSP_SUFFIX;
        File outputFileParent = new File(outputFile).getParentFile();
        if (!outputFileParent.exists()) {
            outputFileParent.mkdirs();
        }
        removePlaceHolders(resourceHomePath + Constants.APPROVAL_JSP_LOCATION  + File.separator + Constants.TASK_INPUT_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-output.jsp
        outputFile =
                outputPath + Constants.APPROVAL_JSP_LOCATION + File.separator + htName + Constants.OUTPUT_JSP_SUFFIX;
        removePlaceHolders(resourceHomePath + Constants.APPROVAL_JSP_LOCATION  + File.separator + Constants.TASK_OUTPUT_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-response.jsp
        outputFile =
                outputPath + Constants.APPROVAL_JSP_LOCATION + File.separator + htName + Constants.RESPONSE_JSP_SUFFIX;
        removePlaceHolders(resourceHomePath + Constants.APPROVAL_JSP_LOCATION  + File.separator + Constants.TASK_RESPONSE_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);

        //created from process
        filesToAdd.add(outputPath + htName + Constants.SERVICE_TXT + Constants.WSDL_EXT);
        filesToAdd.add(outputPath + Constants.WS_HUMAN_TASK_XSD_FILE );

        FileOutputStream zipFOS =
                new FileOutputStream(outputPath + htName + Constants.ZIP_EXT);
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

        File zipRootPath = new File(System.getProperty(Constants.TEMP_DIR_PROPERTY));
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
            if(fis!=null){
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error("Error when closing the file input stream for "+fileName);
                }
            }
        }
    }

    private static class Constants {

        private static final String BPEL_PROCESS_NAME = "${bpelProcessName}";
        private static final String HT_SERVICE_NAME = "${htServiceName}";
        private static final String BPS_HOST_NAME = "${bpsHostName}";
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

        private static final String CARBON_HOST_URL;

        static {
            String PORT_OFFSET = "Ports.Offset";
            String HOST_NAME = "HostName";
            int DEFAULT_HTTPS_PORT = 9443;
            CARBON_HOST_URL = "https://" + ServerConfiguration.getInstance().getFirstProperty(HOST_NAME) + ":" +
                    //adds the offset defined in the server configs to the default 9763 port
                    (Integer.parseInt(ServerConfiguration.getInstance().getFirstProperty(PORT_OFFSET)) +
                            DEFAULT_HTTPS_PORT);
        }
    }
}
