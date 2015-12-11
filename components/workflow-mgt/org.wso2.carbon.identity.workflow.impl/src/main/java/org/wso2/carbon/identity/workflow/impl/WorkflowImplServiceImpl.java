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


import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bpel.stub.mgt.PackageManagementException;
import org.wso2.carbon.bpel.stub.mgt.ProcessManagementException;
import org.wso2.carbon.bpel.stub.mgt.types.DeployedPackagesPaginated;
import org.wso2.carbon.bpel.stub.mgt.types.PackageType;
import org.wso2.carbon.bpel.stub.mgt.types.Version_type0;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryCategory;
import org.wso2.carbon.humantask.stub.types.TSimpleQueryInput;
import org.wso2.carbon.humantask.stub.types.TStatus;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultRow;
import org.wso2.carbon.humantask.stub.types.TTaskSimpleQueryResultSet;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalAccessFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalArgumentFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalOperationFault;
import org.wso2.carbon.humantask.stub.ui.task.client.api.IllegalStateFault;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.impl.dao.BPSProfileDAO;
import org.wso2.carbon.identity.workflow.impl.internal.WorkflowImplServiceDataHolder;
import org.wso2.carbon.identity.workflow.impl.listener.WorkflowImplServiceListener;
import org.wso2.carbon.identity.workflow.impl.util.BPELPackageManagementServiceClient;
import org.wso2.carbon.identity.workflow.impl.util.HumanTaskClientAPIAdminClient;
import org.wso2.carbon.identity.workflow.impl.util.ProcessManagementServiceClient;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WFConstant;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

public class WorkflowImplServiceImpl implements WorkflowImplService {

    private static final Log log = LogFactory.getLog(WorkflowImplServiceImpl.class);

    BPSProfileDAO bpsProfileDAO = new BPSProfileDAO();


    @Override
    public void addBPSProfile(BPSProfile bpsProfileDTO, int tenantId)
            throws WorkflowImplException {

        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreAddBPSProfile(bpsProfileDTO, tenantId);
            }

        }
        bpsProfileDAO.addProfile(bpsProfileDTO, tenantId);
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostAddBPSProfile(bpsProfileDTO, tenantId);
            }
        }

    }

    @Override
    public List<BPSProfile> listBPSProfiles(int tenantId) throws WorkflowImplException {

        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreListBPSProfiles(tenantId);
            }
        }
        List<BPSProfile> bpsProfiles = bpsProfileDAO.listBPSProfiles(tenantId);
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostListBPSProfiles(tenantId, bpsProfiles);
            }
        }

        return bpsProfiles;
    }

    @Override
    public void removeBPSProfile(String profileName) throws WorkflowImplException {

        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreRemoveBPSProfile(profileName);
            }
        }
        bpsProfileDAO.removeBPSProfile(profileName);
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostRemoveBPSProfile(profileName);
            }
        }

    }


    @Override
    public BPSProfile getBPSProfile(String profileName, int tenantId) throws WorkflowImplException {

        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreGetBPSProfile(profileName, tenantId);
            }
        }
        BPSProfile bpsProfile = bpsProfileDAO.getBPSProfile(profileName, tenantId, true);
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostGetBPSProfile(profileName, tenantId, bpsProfile);
            }
        }
        return bpsProfile;
    }

    @Override
    public void updateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException {

        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreUpdateBPSProfile(bpsProfileDTO, tenantId);
            }
        }
        BPSProfile currentBpsProfile = bpsProfileDAO.getBPSProfile(bpsProfileDTO.getProfileName(), tenantId, true);
        if (ArrayUtils.isEmpty(bpsProfileDTO.getPassword())) {
            bpsProfileDTO.setPassword(currentBpsProfile.getPassword());
        }
        bpsProfileDAO.updateProfile(bpsProfileDTO, tenantId);
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostUpdateBPSProfile(bpsProfileDTO, tenantId);
            }
        }


    }

    @Override
    public void deleteHumanTask(WorkflowRequest workflowRequest) throws WorkflowImplException {
        BPSProfileDAO bpsProfileDAO = new BPSProfileDAO();
        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();

        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreDeleteHumanTask(workflowRequest);
            }
        }
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        List<BPSProfile> bpsProfiles = bpsProfileDAO.listBPSProfiles(tenantId);
        HumanTaskClientAPIAdminClient client = null;
        TSimpleQueryInput input = new TSimpleQueryInput();
        TStatus reservedState = new TStatus();
        reservedState.setTStatus(WFImplConstant.HT_STATE_RESERVED);
        input.addStatus(reservedState);
        TStatus readyState = new TStatus();
        readyState.setTStatus(WFImplConstant.HT_STATE_READY);
        input.addStatus(readyState);
        input.setPageSize(100000);
        input.setPageNumber(0);
        input.setSimpleQueryCategory(TSimpleQueryCategory.ALL_TASKS);
        for (int i = 0; i < bpsProfiles.size(); i++) {
            try {
                String host = bpsProfiles.get(i).getWorkerHostURL();

                if (bpsProfiles.get(i).getProfileName().equals(WFImplConstant.DEFAULT_BPS_PROFILE_NAME)) {

                    client = new HumanTaskClientAPIAdminClient(host, bpsProfiles.get(i).getUsername());
                } else {
                    client = new HumanTaskClientAPIAdminClient(host, bpsProfiles.get(i).getUsername(),
                            bpsProfiles.get(i).getPassword());
                }
                TTaskSimpleQueryResultSet results = client.simpleQuery(input);
                TTaskSimpleQueryResultRow[] arr = results.getRow();
                if (ArrayUtils.isNotEmpty(arr)) {
                    for (int j = 0; j < arr.length; j++) {
                        try {
                            Object task = client.getInput(arr[j].getId());
                            InputStream stream = new ByteArrayInputStream(task.toString().getBytes(StandardCharsets
                                    .UTF_8));


                            OMElement taskXML = new StAXOMBuilder(stream).getDocumentElement();
                            Iterator<OMElementImpl> iterator = taskXML.getChildElements();
                            while (iterator.hasNext()) {
                                OMElementImpl child = iterator.next();
                                checkMatchingTaskAndDelete(workflowRequest.getRequestId(), client, arr, j, child);
                            }
                        } catch (IllegalStateFault | XMLStreamException | IllegalArgumentFault | RemoteException |
                                IllegalOperationFault | IllegalAccessFault e) {
                            //If exception throws when retrieving and deleting a specific task, it will continue with
                            // other tasks without terminating.
                            log.info("Failed to check human task.");
                        }

                    }
                }

            } catch (IllegalArgumentFault | RemoteException | IllegalStateFault e) {
                //If exception throws at one iteration of loop, which is testing 1 BPS profile, it will continue with
                // other profiles without terminating.
                log.info("Failed to delete human task associated for this request in BPS profile : " + bpsProfiles.get
                        (i).getProfileName());
            }
        }

        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostDeleteHumanTask(workflowRequest);
            }
        }
    }

    /**
     * This method is used to remove the BPS Artifacts upon a deletion of
     * a Workflow.
     *
     * @param workflow - Workflow request to be deleted.
     * @throws WorkflowImplException
     */

    @Override
    public void removeBPSPackage(Workflow workflow) throws WorkflowImplException {

        List<WorkflowImplServiceListener> workflowListenerList =
                WorkflowImplServiceDataHolder.getInstance().getWorkflowListenerList();
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPreRemoveBPSPackage(workflow);
            }
        }
        WorkflowImplService workflowImplService = WorkflowImplServiceDataHolder.getInstance().getWorkflowImplService();
        WorkflowManagementService workflowManagementService = WorkflowImplServiceDataHolder.getInstance().
                getWorkflowManagementService();

        if (workflowImplService == null || workflowManagementService == null) {
            throw new WorkflowImplException("Error while deleting the BPS artifacts of: " + workflow.getWorkflowName());
        }

        try {
            List<Parameter> workflowParameters = workflowManagementService.
                    getWorkflowParameters(workflow.getWorkflowId());
            Parameter bpsParameter = WorkflowManagementUtil.getParameter(workflowParameters,
                    WFImplConstant.ParameterName.BPS_PROFILE, WFConstant.ParameterHolder.WORKFLOW_IMPL);
            if (bpsParameter == null) {
                throw new WorkflowImplException("Error while deleting the BPS artifacts of: " +
                        workflow.getWorkflowName());
            }
            String bpsProfileName = bpsParameter.getParamValue();
            if (StringUtils.isEmpty(bpsProfileName)) {
                throw new WorkflowImplException("Error while deleting the BPS artifacts of: " +
                        workflow.getWorkflowName());
            }

            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            BPSProfile bpsProfile = workflowImplService.getBPSProfile(bpsProfileName, tenantId);

            if (log.isDebugEnabled()) {
                log.debug("Removing BPS Artifacts of " + bpsProfileName + " " + "for Tenant ID : " + tenantId);
            }

            BPELPackageManagementServiceClient bpsPackageClient;
            ProcessManagementServiceClient bpsProcessClient;

            //Authorizing BPS Package Management & BPS Process Management Stubs.
            String host = bpsProfile.getManagerHostURL();
            if (bpsProfileName.equals(WFImplConstant.DEFAULT_BPS_PROFILE_NAME)) {
                //If emebeded_bps, use mutual ssl authentication
                bpsPackageClient = new BPELPackageManagementServiceClient(host, bpsProfile
                        .getUsername());
                bpsProcessClient = new ProcessManagementServiceClient(host, bpsProfile
                        .getUsername());
            } else {
                //For external BPS profiles, use password authentication
                bpsPackageClient = new BPELPackageManagementServiceClient(host, bpsProfile
                        .getUsername(), bpsProfile.getPassword());
                bpsProcessClient = new ProcessManagementServiceClient(host, bpsProfile
                        .getUsername(), bpsProfile.getPassword());
            }

            DeployedPackagesPaginated deployedPackagesPaginated =
                    bpsPackageClient.listDeployedPackagesPaginated(0, workflow.getWorkflowName());
            PackageType[] packageTypes = deployedPackagesPaginated.get_package();
            if (packageTypes == null || packageTypes.length == 0) {
                throw new WorkflowImplException("Error while deleting the BPS artifacts of: " +
                        workflow.getWorkflowName());
            }
            int numberOfPackages = packageTypes.length;
            for (int i = 0; i < numberOfPackages; i++) {
                PackageType packageType = deployedPackagesPaginated.get_package()[i];
                int numberOfVersions = packageType.getVersions().getVersion().length;
                //Iterating through BPS Packages deployed for the Workflow and retires each associated active processes.
                for (int j = 0; j < numberOfVersions; j++) {
                    Version_type0 versionType = packageType.getVersions().getVersion()[j];
                    if (versionType.getIsLatest()) {
                        int numberOfProcesses = versionType.getProcesses().getProcess().length;
                        if (numberOfProcesses == 0) {
                            throw new WorkflowImplException("Error while deleting the BPS artifacts of: " +
                                    workflow.getWorkflowName());
                        }
                        for (int k = 0; k < numberOfProcesses; k++) {
                            QName pid = null;
                            try {
                                String processStatus = versionType.getProcesses().getProcess()[k].getStatus()
                                        .getValue();
                                if (StringUtils.equals(processStatus, WFImplConstant.BPS_STATUS_ACTIVE)) {
                                    String processID = versionType.getProcesses().getProcess()[k].getPid();
                                    pid = QName.valueOf(processID);
                                    bpsProcessClient.retireProcess(pid);
                                }
                            } catch (RemoteException | ProcessManagementException e) {
                                //If exception throws at retiring one process, it will continue with
                                // other processes without terminating.
                                log.info("Failed to retire BPS process : " + pid);
                            }
                        }
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("BPS Artifacts Successfully removed for Workflow : " + workflow.getWorkflowName());
            }

        } catch (WorkflowException | PackageManagementException | RemoteException e) {
            throw new WorkflowImplException("Error while deleting the BPS Artifacts of the Workflow "
                    + workflow.getWorkflowName(), e);
        }
        for (WorkflowImplServiceListener workflowListener : workflowListenerList) {
            if (workflowListener.isEnable()) {
                workflowListener.doPostRemoveBPSPackage(workflow);
            }
        }


    }

    /*
     *
     *
     * @param requestId Id of the deleting request
     * @param stub stub to call HumanTaskClientAPIAdmin
     * @param resultsList task list in the current human task engine
     * @param resultIndex index of the currently considering rask
     * @param taskElement currently considering task
     * @throws RemoteException
     */

    private void checkMatchingTaskAndDelete(String requestId, HumanTaskClientAPIAdminClient client,
                                            TTaskSimpleQueryResultRow[] resultsList, int resultIndex, OMElementImpl
                                                    taskElement) throws RemoteException, IllegalStateFault,
            IllegalOperationFault, IllegalArgumentFault, IllegalAccessFault {

        if (taskElement.getLocalName().equals(WFImplConstant.HT_PARAMETER_LIST_ELEMENT)) {
            Iterator<OMElementImpl> parameters = taskElement.getChildElements();
            while (parameters.hasNext()) {
                OMElementImpl parameter = parameters.next();
                Iterator<OMAttribute> attributes = parameter.getAllAttributes();
                while (attributes.hasNext()) {
                    OMAttribute currentAttribute = attributes.next();
                    if (currentAttribute.getLocalName().equals(WFImplConstant.HT_ITEM_NAME_ATTRIBUTE) &&
                            currentAttribute
                                    .getAttributeValue().equals(WFImplConstant.HT_REQUEST_ID_ATTRIBUTE_VALUE)) {
                        Iterator<OMElementImpl> itemValues = parameter.getChildElements();
                        if (itemValues.hasNext()) {
                            String taskRequestId = itemValues.next().getText();
                            if (taskRequestId.contains(",")) {
                                taskRequestId = taskRequestId.replaceAll(",", "");
                            }
                            if (taskRequestId.equals(requestId)) {
                                client.skip(resultsList[resultIndex].getId());
                            }
                        }

                    }
                }
            }
        }

    }

}
