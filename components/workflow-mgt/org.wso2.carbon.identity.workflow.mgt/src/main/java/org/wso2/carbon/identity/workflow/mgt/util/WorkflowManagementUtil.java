package org.wso2.carbon.identity.workflow.mgt.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;

import java.util.Arrays;

public class WorkflowManagementUtil {
    private static Log log = LogFactory.getLog(WorkflowManagementUtil.class);

    public static void createAppRole(String workflowName) throws WorkflowException {
        String roleName = getWorkflowRoleName(workflowName);
        String qualifiedUsername = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String[] user = {qualifiedUsername};

        try {
            if (log.isDebugEnabled()) {
                log.debug("Creating workflow role : " + roleName + " and assign the user : "
                          + Arrays.toString(user) + " to that role");
            }
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .addRole(roleName, user, null);
        } catch (UserStoreException e) {
            throw new WorkflowException("Error while creating role", e);
        }

    }

    public static void deleteWorkflowRole(String workflowName) throws WorkflowException {
        String roleName = getWorkflowRoleName(workflowName);

        try {
            if (log.isDebugEnabled()) {
                log.debug("Deleting workflow role : " + roleName);
            }
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .deleteRole(roleName);
        } catch (UserStoreException e) {
            throw new WorkflowException("Error while creating workflow", e);
        }
    }

    public static String getWorkflowRoleName(String workflowName) {
        return UserCoreConstants.INTERNAL_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + workflowName;
        //return WorkFlowConstants.WORKFLOW_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + workflowName;
    }
}
