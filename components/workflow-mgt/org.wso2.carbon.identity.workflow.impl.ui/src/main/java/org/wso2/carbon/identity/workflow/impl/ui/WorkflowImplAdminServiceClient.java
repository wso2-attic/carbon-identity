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

package org.wso2.carbon.identity.workflow.impl.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.impl.stub.WorkflowImplAdminServiceStub;
import org.wso2.carbon.identity.workflow.impl.stub.WorkflowImplAdminServiceWorkflowImplException;
import org.wso2.carbon.identity.workflow.impl.stub.bean.BPSProfile;
import java.rmi.RemoteException;

public class WorkflowImplAdminServiceClient {

    private WorkflowImplAdminServiceStub stub;
    private static final Log log = LogFactory.getLog(WorkflowImplAdminServiceClient.class);

    /**
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws AxisFault
     */
    public WorkflowImplAdminServiceClient(String cookie, String backendServerURL,
                                          ConfigurationContext configCtx) throws AxisFault {

        String serviceURL = backendServerURL + "WorkflowImplAdminService";
        stub = new WorkflowImplAdminServiceStub(configCtx, serviceURL);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }


    /**
     * Add new BPS profile
     *
     * @param bpsProfile
     * @throws RemoteException
     * @throws WorkflowImplAdminServiceWorkflowImplException
     */
    public void addBPSProfile(BPSProfile bpsProfile)
            throws RemoteException, WorkflowImplAdminServiceWorkflowImplException {

        stub.addBPSProfile(bpsProfile);
    }


    /**
     * Retrieve BPS Profiles
     *
     * @return
     * @throws RemoteException
     * @throws WorkflowImplAdminServiceWorkflowImplException
     */
    public BPSProfile[] listBPSProfiles() throws RemoteException, WorkflowImplAdminServiceWorkflowImplException {

        BPSProfile[] bpsProfiles = stub.listBPSProfiles();
        if (bpsProfiles == null) {
            bpsProfiles = new BPSProfile[0];
        }
        return bpsProfiles;
    }

    /**
     * Get BPS Profile detail for given profile name
     *
     * @param profileName
     * @return
     * @throws RemoteException
     * @throws WorkflowImplAdminServiceWorkflowImplException
     */
    public BPSProfile getBPSProfiles(String profileName)
            throws RemoteException, WorkflowImplAdminServiceWorkflowImplException {

        BPSProfile bpsProfile = stub.getBPSProfile(profileName);
        return bpsProfile;
    }

    /**
     * Update BPS Profile
     *
     * @param bpsProfileDTO
     * @throws RemoteException
     * @throws WorkflowImplAdminServiceWorkflowImplException
     */
    public void updateBPSProfile(BPSProfile bpsProfileDTO)
            throws RemoteException, WorkflowImplAdminServiceWorkflowImplException {

        stub.updateBPSProfile(bpsProfileDTO);
    }

    public void deleteBPSProfile(String profileName) throws RemoteException, WorkflowImplAdminServiceWorkflowImplException {

        stub.removeBPSProfile(profileName);
    }

}
