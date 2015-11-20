package org.wso2.carbon.user.mgt.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.Arrays;
import java.util.Map;

public class UserMgtAuditLogger extends AbstractIdentityUserOperationEventListener {

    private static final Log log = LogFactory.getLog(UserMgtAuditLogger.class);

    private static final Log audit = CarbonConstants.AUDIT_LOG;
    public static final String SUCCESS = "Success";
    public static final String FAILED = "Failed";

    private static String AUDIT_MESSAGE = "Initiator : %s | Action : %s | Target : %s | Data : { %s } | Result : %s ";

    public boolean doPostAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims,
                                 String profile, UserStoreManager userStoreManager) throws UserStoreException {

        StringBuilder builder = new StringBuilder();
        if (roleList != null) {
            for (int i = 0; i < roleList.length; i++) {
                builder.append(roleList[i] + ",");
            }
        }
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Add User", userName, "Roles :"
                + builder.toString(), SUCCESS));
        return true;
    }

    public boolean doPostDeleteUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {
        String currentUser = "wso2.system";
        if (getUser() != null) {
            currentUser = getUser();
        }
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Delete User",
                userName, "", SUCCESS));
        return true;
    }

    public boolean doPostUpdateCredential(String userName, Object credential, UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Change Password by User",
                userName, "", SUCCESS));
        return true;
    }

    public boolean doPreUpdateCredentialByAdmin(String userName, Object newCredential, UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Change Password by Administrator",
                userName, "", SUCCESS));
        return true;
    }

    public boolean doPostDeleteRole(String roleName, UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Delete Role", roleName, "",
                SUCCESS));
        return true;
    }

    public boolean doPostAddRole(String roleName, String[] userList, Permission[] permissions, UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Add Role", roleName, "Users : "
                + userList.toString() + " Permissions : " + Arrays.toString(permissions), SUCCESS));
        return true;
    }

    public boolean doPostUpdateRoleName(String roleName, String newRoleName, UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Update Role Name", roleName,
                "Old : " + roleName + " New : " + newRoleName, SUCCESS));
        return true;
    }

    public boolean doPostUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers,
                                            UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Update Users of Role", roleName,
                "Users : " + newUsers.toString(), SUCCESS));
        return true;
    }

    public boolean doPostUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles, UserStoreManager userStoreManager) throws UserStoreException {
        audit.info(String.format(AUDIT_MESSAGE, getUser(), "Update Roles of User", userName,
                "Roles : " + newRoles.toString(), SUCCESS));
        return true;
    }

    private String getUser() {
        String user = CarbonContext.getThreadLocalCarbonContext().getUsername() + "@" +
                CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (user == null){
            user = "wso2.system";
        }
        return user;
    }
}
