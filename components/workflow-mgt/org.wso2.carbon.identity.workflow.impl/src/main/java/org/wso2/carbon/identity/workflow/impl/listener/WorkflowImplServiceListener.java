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

import org.wso2.carbon.identity.workflow.impl.WorkflowImplException;
import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;

import java.util.List;

public interface WorkflowImplServiceListener {

    /**
     * Trigger before adding new BPS profile
     *
     * @param bpsProfileDTO Details of BPS profiles to add
     * @param tenantId      Tenant to add new profile
     * @throws WorkflowImplException
     */
    void doPreAddBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    /**
     * Trigger after adding new BPS profile
     *
     * @param bpsProfileDTO Details of BPS profiles to add
     * @param tenantId      Tenant to add new profile
     * @throws WorkflowImplException
     */
    void doPostAddBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    /**
     * Trigger before listing BPS profiles
     *
     * @param tenantId Tenant ID to list BPS profiles
     * @throws WorkflowImplException
     */
    void doPreListBPSProfiles(int tenantId) throws WorkflowImplException;

    /**
     * Trigger after listing BPS profiles
     *
     * @param tenantId Tenant ID to list BPS profiles
     * @param list     Result returned by  operation
     * @throws WorkflowImplException
     */
    void doPostListBPSProfiles(int tenantId, List<BPSProfile> list) throws WorkflowImplException;

    /**
     * Trigger before removing a BPS profile
     *
     * @param profileName Name of the BPS profile to remove
     * @throws WorkflowImplException
     */
    void doPreRemoveBPSProfile(String profileName) throws WorkflowImplException;

    /**
     * Trigger after removing a BPS profile
     *
     * @param profileName Name of the BPS profile to remove
     * @throws WorkflowImplException
     */
    void doPostRemoveBPSProfile(String profileName) throws WorkflowImplException;

    /**
     * Trigger before retrieving a BPS profile
     *
     * @param profileName Name of profile to retrieve
     * @param tenantId    Tenant ID
     * @throws WorkflowImplException
     */
    void doPreGetBPSProfile(String profileName, int tenantId) throws WorkflowImplException;

    /**
     * Trigger after retrieving a BPS profile
     *
     * @param profileName Name of profile to retrieve
     * @param tenantId    Tenant ID
     * @param result      Current result of the operation
     * @throws WorkflowImplException
     */
    void doPostGetBPSProfile(String profileName, int tenantId, BPSProfile result) throws WorkflowImplException;

    /**
     * Trigger before updating a BPS profile
     *
     * @param bpsProfileDTO New details of the profile to update
     * @param tenantId      Tenant ID
     * @throws WorkflowImplException
     */
    void doPreUpdateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    /**
     * Trigger after updating a BPS profile
     *
     * @param bpsProfileDTO
     * @param tenantId
     * @throws WorkflowImplException
     */
    void doPostUpdateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    /**
     * Trigger before deleting a human task
     *
     * @param workflowRequest Workflow which human task to be deleted
     * @throws WorkflowImplException
     */

    void doPreDeleteHumanTask(WorkflowRequest workflowRequest) throws WorkflowImplException;

    /**
     * Trigger after deleting a human task
     *
     * @param workflowRequest Workflow which human task to be deleted
     * @throws WorkflowImplException
     */
    void doPostDeleteHumanTask(WorkflowRequest workflowRequest) throws WorkflowImplException;

    /**
     * Trigger before deleting a BPS package
     *
     * @param workflow Workflow to delete packages
     * @throws WorkflowImplException
     */
    void doPreRemoveBPSPackage(Workflow workflow) throws WorkflowImplException;

    /**
     * Trigger after deleting a BPS package
     *
     * @param workflow Workflow to delete packages
     * @throws WorkflowImplException
     */
    void doPostRemoveBPSPackage(Workflow workflow) throws WorkflowImplException;

}
