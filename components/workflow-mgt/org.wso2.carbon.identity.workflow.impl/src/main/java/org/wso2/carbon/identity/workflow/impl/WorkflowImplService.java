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

    void addBPSProfile(BPSProfile bpsProfileDTO, int tenantId)
            throws WorkflowImplException;

    List<BPSProfile> listBPSProfiles(int tenantId) throws WorkflowImplException;

    void removeBPSProfile(String profileName) throws WorkflowImplException;

    BPSProfile getBPSProfile(String profileName, int tenantId) throws WorkflowImplException;

    void updateBPSProfile(BPSProfile bpsProfileDTO, int tenantId) throws WorkflowImplException;

    void deleteHumanTask(WorkflowRequest workflowRequest) throws WorkflowImplException;

    void removeBPSPackage(Workflow workflowRequest) throws WorkflowImplException;

}
