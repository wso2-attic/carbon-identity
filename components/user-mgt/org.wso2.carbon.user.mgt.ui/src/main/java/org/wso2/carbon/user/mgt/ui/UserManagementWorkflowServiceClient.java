package org.wso2.carbon.user.mgt.ui;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.mgt.workflow.stub.UserManagementWorkflowServiceStub;
import org.wso2.carbon.user.mgt.workflow.stub.UserManagementWorkflowServiceWorkflowExceptionException;

import java.rmi.RemoteException;

public class UserManagementWorkflowServiceClient {

    private UserManagementWorkflowServiceStub stub;
    private static final Log log = LogFactory.getLog(UserManagementWorkflowServiceClient.class);

    /**
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws org.apache.axis2.AxisFault
     */
    public UserManagementWorkflowServiceClient(String cookie, String backendServerURL,
                                               ConfigurationContext configCtx) throws AxisFault {

        String serviceURL = backendServerURL + "UserManagementWorkflowService";
        stub = new UserManagementWorkflowServiceStub(configCtx, serviceURL);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * @param wfOperationType Operation Type of the Work-flow.
     * @param wfStatus        Current Status of the Work-flow.
     * @param entityType      Entity Type of the Work-flow.
     * @return
     * @throws java.rmi.RemoteException
     * @throws org.wso2.carbon.user.mgt.workflow.stub.UserManagementWorkflowServiceWorkflowExceptionException
     */

    public String[] listAllEntityNames(String wfOperationType, String wfStatus, String entityType)
            throws RemoteException, UserManagementWorkflowServiceWorkflowExceptionException {

        String[] entityNames = stub.listAllEntityNames(wfOperationType, wfStatus, entityType);
        if (entityNames == null) {
            entityNames = new String[0];
        }
        return entityNames;
    }
}
