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

package org.wso2.carbon.identity.workflow.impl.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.workflow.impl.WorkflowImplException;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;

public class WorkflowImplAuditLogger extends AbstractWorkflowImplServiceListener {

    private static final Log AUDIT_LOG = CarbonConstants.AUDIT_LOG;
    private static final String AUDIT_MESSAGE = "Initiator : %s | Action : %s | Data : { %s } | Result " +
            ":  %s ";
    private static final String AUDIT_SUCCESS = "Success";

    /**
     * Trigger after adding new BPS profile
     *
     * @param bpsProfileDTO
     * @param tenantId
     * @throws WorkflowImplException
     */
    @Override
    public void doPostAddBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException {
        String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (StringUtils.isBlank(loggedInUser)) {
            loggedInUser = CarbonConstants.REGISTRY_SYSTEM_USERNAME;
        }
        String auditData = "\"" + "Profile Name" + "\" : \"" + bpsProfileDTO.getProfileName()
                + "\",\"" + "Manager Host URL" + "\" : \"" + bpsProfileDTO.getManagerHostURL()
                + "\",\"" + "Worker Host URL" + "\" : \"" + bpsProfileDTO.getWorkerHostURL()
                + "\",\"" + "User" + "\" : \"" + bpsProfileDTO.getUsername()
                + "\"";
        AUDIT_LOG.info(String.format(AUDIT_MESSAGE, loggedInUser, "Add BPS Profile", auditData, AUDIT_SUCCESS));
    }

    /**
     * Trigger after removing a BPS profile
     *
     * @param profileName
     * @throws WorkflowImplException
     */
    @Override
    public void doPostRemoveBPSProfile(String profileName) throws WorkflowImplException {
        String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (StringUtils.isBlank(loggedInUser)) {
            loggedInUser = CarbonConstants.REGISTRY_SYSTEM_USERNAME;
        }
        String auditData = "\"" + "Profile Name" + "\" : \"" + profileName + "\"";
        AUDIT_LOG.info(String.format(AUDIT_MESSAGE, loggedInUser, "Delete BPS Profile", auditData, AUDIT_SUCCESS));
    }

    /**
     * Trigger after updating a BPS profile
     *
     * @param bpsProfileDTO
     * @param tenantId
     * @throws WorkflowImplException
     */
    @Override
    public void doPostUpdateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException {
        String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (StringUtils.isBlank(loggedInUser)) {
            loggedInUser = CarbonConstants.REGISTRY_SYSTEM_USERNAME;
        }
        String auditData = "\"" + "Profile Name" + "\" : \"" + bpsProfileDTO.getProfileName()
                + "\",\"" + "Manager Host URL" + "\" : \"" + bpsProfileDTO.getManagerHostURL()
                + "\",\"" + "Worker Host URL" + "\" : \"" + bpsProfileDTO.getWorkerHostURL()
                + "\",\"" + "User" + "\" : \"" + bpsProfileDTO.getUsername()
                + "\"";
        AUDIT_LOG.info(String.format(AUDIT_MESSAGE, loggedInUser, "Update BPS Profile", auditData, AUDIT_SUCCESS));
    }

    /**
     * Trigger after deleting a BPS package
     *
     * @param workflow
     * @throws WorkflowImplException
     */
    @Override
    public void doPostRemoveBPSPackage(Workflow workflow) throws WorkflowImplException {
        String loggedInUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        if (StringUtils.isBlank(loggedInUser)) {
            loggedInUser = CarbonConstants.REGISTRY_SYSTEM_USERNAME;
        }
        String auditData = "\"" + "Workflow Name" + "\" : \"" + workflow.getWorkflowName()
                + "\",\"" + "Template ID" + "\" : \"" + workflow.getTemplateId()
                + "\",\"" + "Workflow Description" + "\" : \"" + workflow.getWorkflowDescription()
                + "\",\"" + "Workflow ID" + "\" : \"" + workflow.getWorkflowId()
                + "\",\"" + "Workflow Impl ID" + "\" : \"" + workflow.getWorkflowImplId()
                + "\"";
        AUDIT_LOG.info(String.format(AUDIT_MESSAGE, loggedInUser, "Remove BPS Package",
                "Workflow Impl Admin Service", auditData, AUDIT_SUCCESS));
    }
}
