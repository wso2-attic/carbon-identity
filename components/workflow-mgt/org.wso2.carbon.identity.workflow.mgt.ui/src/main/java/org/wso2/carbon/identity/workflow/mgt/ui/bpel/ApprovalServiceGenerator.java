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

package org.wso2.carbon.identity.workflow.mgt.ui.bpel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem;
import org.wso2.carbon.identity.workflow.mgt.ui.WorkflowDeployerClient;
import org.wso2.carbon.identity.workflow.mgt.ui.bpel.bean.ApprovalServiceParams;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class ApprovalServiceGenerator {

    private static final String TEMP_DIR_PROPERTY = "java.io.tmpdir";
    private static final String ZIP_TYPE = "zip";
    private static Log log = LogFactory.getLog(ApprovalServiceGenerator.class);

    private ApprovalServiceParams serviceParams;

    public ApprovalServiceGenerator(ApprovalServiceParams serviceParams) {
        this.serviceParams = serviceParams;
    }

    public void generateAndDeployArtifacts() {
        try {
            generateProcessArtifact();
            generateHTArtifact();
            deployArtifacts();
            //todo:deploy
        } catch (IOException e) {
            log.error("Error when generating artifacts.", e);
            //todo: throw
        }

    }

    private void deployArtifacts() throws RemoteException {
        String bpelArchiveName = serviceParams.getBpelProcessName() + Constants.ZIP_EXT;
        String archiveHome = System.getProperty(TEMP_DIR_PROPERTY) + File.separator;
        DataSource bpelDataSource = new FileDataSource(archiveHome + bpelArchiveName);
        WorkflowDeployerClient workflowDeployerClient = new WorkflowDeployerClient(serviceParams.getBpsHostName(),
                serviceParams.getBpsUsername(), serviceParams.getBpsUserPassword().toCharArray());
        workflowDeployerClient.uploadBPEL(getBPELUploadedFileItem(new DataHandler(bpelDataSource),
                bpelArchiveName, ZIP_TYPE));
        String htArchiveName = serviceParams.getHtServiceName() + Constants.ZIP_EXT;
        DataSource htDataSource = new FileDataSource(archiveHome + htArchiveName);
        workflowDeployerClient.uploadHumanTask(getHTUploadedFileItem(new DataHandler(htDataSource), htArchiveName,
                ZIP_TYPE));
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
        placeHolderValues.put(Constants.BPEL_PROCESS_NAME, serviceParams.getBpelProcessName());
        placeHolderValues.put(Constants.HT_SERVICE_NAME, serviceParams.getHtServiceName());
        placeHolderValues.put(Constants.BPS_HOST_NAME, serviceParams.getBpsHostName());
        placeHolderValues.put(Constants.CARBON_HOST_NAME, serviceParams.getCarbonHostName());
        placeHolderValues.put(Constants.CARBON_CALLBACK_AUTH_USER, serviceParams.getCarbonAuthUser());
        placeHolderValues.put(Constants.CARBON_CALLBACK_AUTH_PASSWORD, serviceParams.getCarbonUserPassword());
        placeHolderValues.put(Constants.HT_SUBJECT, serviceParams.getHumanTaskSubject());
        placeHolderValues.put(Constants.HT_DESCRIPTION, serviceParams.getHumanTaskDescription());
        placeHolderValues.put(Constants.HT_OWNER_ROLE, serviceParams.getHtPotentialOwnerRole());
        placeHolderValues.put(Constants.HT_ADMIN_ROLE, serviceParams.getHtAdminRole());
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
        String taskWsdl = null; //to keep without deleting for human task
        String resourceHomePath =
                Constants.TEMPLATE_RESOURCE_LOCATION + File.separator + Constants.BPEL_RESOURCE_LOCATION +
                        File.separator + Constants.APPROVAL_SERVICE_RESOURCE_LOCATION + File.separator;
        String outputPath = System.getProperty(TEMP_DIR_PROPERTY) + File.separator;
        //process.wsdl
        String outputFile = outputPath + serviceParams.getBpelProcessName() + Constants.PROCESS_WSDL_FILE;
        removePlaceHolders(resourceHomePath + Constants.PROCESS_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //process.bpel
        outputFile = outputPath + serviceParams.getBpelProcessName() + Constants.BPEL_EXT;
        removePlaceHolders(resourceHomePath + Constants.PROCESS_BPEL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //callback.wsdl
        outputFile = outputPath + Constants.CALLBACK_WSDL_FILE;
        removePlaceHolders(resourceHomePath + Constants.CALLBACK_WSDL_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task.wsdl
        outputFile = outputPath + serviceParams.getHtServiceName() + Constants.WSDL_EXT;
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

        FileOutputStream zipFOS =
                new FileOutputStream(outputPath + serviceParams.getBpelProcessName() + Constants.ZIP_EXT);
        ZipOutputStream zipOutputStream = new ZipOutputStream(zipFOS);
        for (String fileName : filesToAdd) {
            addToZipFile(fileName, zipOutputStream);
        }
        zipOutputStream.close();
        zipFOS.close();
        for (String fileName : filesToAdd) {
            File file = new File(fileName);
            if (file.exists() && file.isFile() && !fileName.equals(taskWsdl)) {
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
        String outputPath = System.getProperty(TEMP_DIR_PROPERTY) + File.separator;
        //task.ht
        String outputFile = outputPath + serviceParams.getHtServiceName() + Constants.HT_EXT;
        removePlaceHolders(resourceHomePath + Constants.TASK_HT_FILE, outputFile);
        filesToAdd.add(outputFile);
        //htconfig.xml
        outputFile = outputPath + Constants.HTCONFIG_XML_FILE;
        removePlaceHolders(resourceHomePath + Constants.HTCONFIG_XML_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-input.jsp
        outputFile = outputPath + Constants.APPROVAL_JSP_LOCATION + File.separator + serviceParams
                .getHtServiceName() + Constants.INPUT_JSP_SUFFIX;
        File outputFileParent = new File(outputFile).getParentFile();
        if (!outputFileParent.exists()) {
            outputFileParent.mkdirs();
        }
        removePlaceHolders(resourceHomePath + Constants.TASK_INPUT_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-output.jsp
        outputFile = outputPath + Constants.APPROVAL_JSP_LOCATION + File.separator + serviceParams
                .getHtServiceName() + Constants.OUTPUT_JSP_SUFFIX;
        removePlaceHolders(resourceHomePath + Constants.TASK_OUTPUT_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);
        //task-response.jsp
        outputFile = outputPath + Constants.APPROVAL_JSP_LOCATION + File.separator + serviceParams
                .getHtServiceName() + Constants.RESPONSE_JSP_SUFFIX;
        removePlaceHolders(resourceHomePath + Constants.TASK_RESPONSE_JSP_FILE, outputFile);
        filesToAdd.add(outputFile);

        //created from process
        filesToAdd.add(outputPath + serviceParams.getHtServiceName() + Constants.WSDL_EXT);

        FileOutputStream zipFOS =
                new FileOutputStream(outputPath + serviceParams.getHtServiceName() + Constants.ZIP_EXT);
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

    private void addToZipFile(String fileName, ZipOutputStream zos) throws FileNotFoundException, IOException {
        File zipRootPath = new File(System.getProperty(TEMP_DIR_PROPERTY));
        File file = new File(fileName);
        String relativePath = zipRootPath.toURI().relativize(file.toURI()).getPath();
        FileInputStream fis = new FileInputStream(fileName);
        ZipEntry zipEntry = new ZipEntry(relativePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
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

        private static final String PROCESS_BPEL_FILE = "Process.bpel";
        private static final String PROCESS_WSDL_FILE = "Artifacts.wsdl";
        private static final String TASK_WSDL_FILE = "TaskService.wsdl";
        private static final String CALLBACK_WSDL_FILE = "CallbackService.wsdl";
        private static final String CALLBACK_EPR_FILE = "callbackService.epr";
        private static final String DEPLOY_XML_FILE = "deploy.xml";

        private static final String HTCONFIG_XML_FILE = "htconfig.xml";
        private static final String TASK_HT_FILE = "Task.ht";
        private static final String TASK_INPUT_JSP_FILE = "Task-input.jsp";
        private static final String TASK_OUTPUT_JSP_FILE = "Task-output.jsp";
        private static final String TASK_RESPONSE_JSP_FILE = "Task-response.jsp";

        private static final String TEMPLATE_RESOURCE_LOCATION = "templates";
        private static final String BPEL_RESOURCE_LOCATION = "bpel";
        private static final String HT_RESOURCE_LOCATION = "humantask";
        private static final String APPROVAL_SERVICE_RESOURCE_LOCATION = "SimpleApprovalService";
        private static final String APPROVAL_HT_RESOURCE_LOCATION = "SimpleApprovalTask";
        private static final String APPROVAL_JSP_LOCATION = "web";

        private static final String WSDL_EXT = ".wsdl";
        private static final String BPEL_EXT = ".bpel";
        private static final String ZIP_EXT = ".zip";
        private static final String HT_EXT = ".ht";
        private static final String INPUT_JSP_SUFFIX = "-input.jsp";
        private static final String OUTPUT_JSP_SUFFIX = "-output.jsp";
        private static final String RESPONSE_JSP_SUFFIX = "-response.jsp";

    }

}
