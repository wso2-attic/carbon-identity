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

import org.wso2.carbon.identity.workflow.impl.bean.BPSProfile;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest;

import java.util.List;
import java.util.Map;

public interface WorkflowImplService {

    /**
     * Add new BPS profile
     *
     * @param bpsProfileDTO Details of new profile to add
     * @param tenantId      Tenant domain
     * @throws WorkflowImplException
     */
    void addBPSProfile(BPSProfile bpsProfileDTO, int tenantId)
            throws WorkflowImplException;

    /**
     * List BPS profile of tenant domain
     *
     * @param tenantId ID of tenant domain
     * @return
     * @throws WorkflowImplException
     */
    List<BPSProfile> listBPSProfiles(int tenantId) throws WorkflowImplException;


    /**
     * Remove a BPS profile
     *
     * @param profileName Name of profile
     * @throws WorkflowImplException
     */
    void removeBPSProfile(String profileName) throws WorkflowImplException;

    /**
     * Reading BPS profile for given profile name and for current tenant
     *
     * @param profileName Name of the profile
     * @param tenantId    ID of tenant domain
     * @return
     * @throws WorkflowImplException
     */
    BPSProfile getBPSProfile(String profileName, int tenantId) throws WorkflowImplException;

    /**
     * update BPS profile for given data
     *
     * @param bpsProfileDTO BPS profile object with new details
     * @param tenantId
     * @throws WorkflowImplException
     */
    void updateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    /**
     * Delete a human task in BPS
     *
     * @param workflowRequest  Corresponding workflow request of the human task to be deleted
     * @throws WorkflowImplException
     */
    void deleteHumanTask(WorkflowRequest workflowRequest) throws WorkflowImplException;

    /**
     * Removes BPS artifacts of a particular workflow.
     *
     * @param workflow Workflow request to be deleted.
     * @throws WorkflowImplException
     */
    void removeBPSPackage(Workflow workflow) throws WorkflowImplException;

}
