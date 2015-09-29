/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.mgt;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.registry.api.Collection;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.user.mgt.UserMgtConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ApplicationMgtUtil {

    public static final String APPLICATION_ROOT_PERMISSION = "applications";
    public static final String PATH_CONSTANT = RegistryConstants.PATH_SEPARATOR;
    private static final List<String> paths = new ArrayList<String>();
    private static String applicationNode;

    private static Log log = LogFactory.getLog(ApplicationMgtUtil.class);

    private ApplicationMgtUtil() {
    }

    public static org.wso2.carbon.user.api.Permission[] buildPermissions(String applicationName,
                                                                         String[] permissions) {

        org.wso2.carbon.user.api.Permission[] permissionSet = null;

        if (permissions != null) {
            permissionSet = new org.wso2.carbon.user.api.Permission[permissions.length];
            int i = 0;
            for (String permissionString : permissions) {
                permissionSet[i] = new org.wso2.carbon.user.api.Permission(applicationName + "\\"
                        + permissionString, "ui.execute");
            }
        }
        return permissionSet;
    }

    public static boolean isUserAuthorized(String applicationName, int applicationID)
            throws IdentityApplicationManagementException {

        if (!isUserAuthorized(applicationName)) {
            // maybe the role name of the app has updated. In this case, lets
            // load back the old app name
            ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
            String storedApplicationName = appDAO.getApplicationName(applicationID);
            return isUserAuthorized(storedApplicationName);
        }

        return true;
    }

    /**
     * @param applicationName
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static boolean isUserAuthorized(String applicationName) throws IdentityApplicationManagementException {
        String user = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String applicationRoleName = getAppRoleName(applicationName);

        try {
            if (log.isDebugEnabled()) {
                log.debug("Checking whether user has role : " + applicationRoleName + " by retrieving role list of " +
                          "user : " + user);
            }
            String[] userRoles = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                    .getUserStoreManager().getRoleListOfUser(user);
            for (String userRole : userRoles) {
                if (applicationRoleName.equals(userRole)) {
                    return true;
                }
            }
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new IdentityApplicationManagementException("Error while creating application", e);
        }

        return false;
    }

    /**
     * @param tenantApplicationNames
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static String[] getAuthorizedApps(String[] tenantApplicationNames)
            throws IdentityApplicationManagementException {
        List<String> authorizedApps = new ArrayList<String>();
        for (String applicationName : tenantApplicationNames) {
            if (isUserAuthorized(applicationName)) {
                authorizedApps.add(applicationName);
            }
        }
        return authorizedApps.toArray(new String[authorizedApps.size()]);
    }

    /**
     * Create a role for the application and assign the user to that role.
     *
     * @param applicationName
     * @throws IdentityApplicationManagementException
     */
    public static void createAppRole(String applicationName) throws IdentityApplicationManagementException {
        String roleName = getAppRoleName(applicationName);
        String qualifiedUsername = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String[] user = {qualifiedUsername};

        try {
            // create a role for the application and assign the user to that role.
            if (log.isDebugEnabled()) {
                log.debug("Creating application role : " + roleName + " and assign the user : "
                          + Arrays.toString(user) + " to that role");
            }
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .addRole(roleName, user, null);
        } catch (UserStoreException e) {
            throw new IdentityApplicationManagementException("Error while creating application", e);
        }

    }

    private static String getAppRoleName(String applicationName) {
//        return ApplicationConstants.APPLICATION_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + applicationName;
        return UserCoreConstants.INTERNAL_DOMAIN + UserCoreConstants.DOMAIN_SEPARATOR + applicationName;
    }

    /**
     * Delete the role of the app
     *
     * @param applicationName
     * @throws IdentityApplicationManagementException
     */
    public static void deleteAppRole(String applicationName) throws IdentityApplicationManagementException {
        String roleName = getAppRoleName(applicationName);

        try {
            if (log.isDebugEnabled()) {
                log.debug("Deleting application role : " + roleName);
            }
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .deleteRole(roleName);
        } catch (UserStoreException e) {
            throw new IdentityApplicationManagementException("Error while creating application", e);
        }
    }

    /**
     * @param oldName
     * @param newName
     * @throws IdentityApplicationManagementException
     */
    public static void renameRole(String oldName, String newName) throws UserStoreException {

        if (log.isDebugEnabled()) {
            log.debug("Renaming application role : " + UserCoreUtil.addInternalDomainName(oldName)
                    + " to new role : " + UserCoreUtil.addInternalDomainName(newName));
        }
        CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                .updateRoleName(UserCoreUtil.addInternalDomainName(oldName), UserCoreUtil.addInternalDomainName(newName));

    }

    /**
     * Rename the registry path node name for a deleted Service provider role.
     *
     * @param oldName
     * @param newName
     * @throws IdentityApplicationManagementException
     */
    public static void renameAppPermissionPathNode(String oldName, String newName)
            throws IdentityApplicationManagementException {

        List<ApplicationPermission> loadPermissions = loadPermissions(oldName);
        String newApplicationNode = ApplicationMgtUtil.getApplicationPermissionPath() + PATH_CONSTANT + oldName;
        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);
        //creating new application node
        try {
            for (ApplicationPermission applicationPermission : loadPermissions) {
                tenantGovReg.delete(newApplicationNode + PATH_CONSTANT + applicationPermission.getValue());
            }
            tenantGovReg.delete(newApplicationNode);
            Collection permissionNode = tenantGovReg.newCollection();
            permissionNode.setProperty("name", newName);
            newApplicationNode = ApplicationMgtUtil.getApplicationPermissionPath() + PATH_CONSTANT + newName;
            ApplicationMgtUtil.applicationNode = newApplicationNode;
            tenantGovReg.put(newApplicationNode, permissionNode);
            addPermission(loadPermissions.toArray(new ApplicationPermission[loadPermissions.size()]), tenantGovReg);
        } catch (RegistryException e) {
            throw new IdentityApplicationManagementException("Error while renaming permission node "
                    + oldName + "to " + newName, e);
        }
    }

    /**
     * @param applicationName
     * @param permissionsConfig
     * @throws IdentityApplicationManagementException
     */
    public static void storePermission(String applicationName, PermissionsAndRoleConfig permissionsConfig)
            throws IdentityApplicationManagementException {

        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        String permissionResourcePath = getApplicationPermissionPath();
        try {
            if (!tenantGovReg.resourceExists(permissionResourcePath)) {
                String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
                boolean loggedInUserChanged = false;
                UserRealm realm =
                        (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();
                if (!realm.getAuthorizationManager()
                        .isUserAuthorized(loggedInUser, permissionResourcePath,
                                UserMgtConstants.EXECUTE_ACTION)) {
                    //Logged in user is not authorized to create the permission.
                    // Temporarily change the user to the admin for creating the permission
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(
                            realm.getRealmConfiguration().getAdminUserName());
                    tenantGovReg = CarbonContext.getThreadLocalCarbonContext()
                            .getRegistry(RegistryType.USER_GOVERNANCE);
                    loggedInUserChanged = true;
                }
                Collection appRootNode = tenantGovReg.newCollection();
                appRootNode.setProperty("name", "Applications");
                tenantGovReg.put(permissionResourcePath, appRootNode);
                if (loggedInUserChanged) {
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(loggedInUser);
                }
            }

            if (permissionsConfig != null) {
                ApplicationPermission[] permissions = permissionsConfig.getPermissions();
                if (permissions == null || permissions.length < 1) {
                    return;
                }

                // creating the application node in the tree
                String appNode = permissionResourcePath + PATH_CONSTANT + applicationName;
                Collection appNodeColl = tenantGovReg.newCollection();
                tenantGovReg.put(appNode, appNodeColl);

                // now start storing the permissions
                for (ApplicationPermission permission : permissions) {
                    String permissinPath = appNode + PATH_CONSTANT + permission;
                    Resource permissionNode = tenantGovReg.newResource();
                    permissionNode.setProperty("name", permission.getValue());
                    tenantGovReg.put(permissinPath, permissionNode);
                }
            }

        } catch (Exception e) {
            throw new IdentityApplicationManagementException("Error while storing permissions", e);
        }
    }

    /**
     * Updates the permissions of the application
     *
     * @param applicationName
     * @param permissions
     * @throws IdentityApplicationManagementException
     */
    public static void updatePermissions(String applicationName, ApplicationPermission[] permissions)
            throws IdentityApplicationManagementException {

        applicationNode = getApplicationPermissionPath() + PATH_CONSTANT + applicationName;

        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        try {

            boolean exist = tenantGovReg.resourceExists(applicationNode);
            if (!exist) {
                Collection appRootNode = tenantGovReg.newCollection();
                appRootNode.setProperty("name", applicationName);
                tenantGovReg.put(applicationNode, appRootNode);
            }

            Collection appNodeCollec = (Collection) tenantGovReg.get(applicationNode);
            String[] childern = appNodeCollec.getChildren();

            // new permissions are null. deleting all permissions case
            if ((childern != null && childern.length > 0)
                    && (permissions == null || permissions.length == 0)) { // there are permissions
                tenantGovReg.delete(applicationNode);
            }

            if (ArrayUtils.isEmpty(permissions)) {
                return;
            }

            // no permission exist for the application, create new
            if (childern == null || appNodeCollec.getChildCount() < 1) {

                addPermission(permissions, tenantGovReg);

            } else { // there are permission
                List<ApplicationPermission> loadPermissions = loadPermissions(applicationName);
                for (ApplicationPermission applicationPermission : loadPermissions) {
                    tenantGovReg.delete(applicationNode + PATH_CONSTANT + applicationPermission.getValue());
                }
                addPermission(permissions, tenantGovReg);
            }

        } catch (RegistryException e) {
            throw new IdentityApplicationManagementException("Error while storing permissions", e);
        }

    }

    private static void addPermission(ApplicationPermission[] permissions, Registry tenantGovReg) throws
            RegistryException {
        for (ApplicationPermission permission : permissions) {
            String permissionValue = permission.getValue();

            if ("/".equals(permissionValue.substring(0, 1))) {         //if permissions are starts with slash remove that
                permissionValue = permissionValue.substring(1);
            }
            String[] splitedPermission = permissionValue.split("/");
            String permissinPath = applicationNode + PATH_CONSTANT;

            for (int i = 0; i < splitedPermission.length; i++) {
                permissinPath = permissinPath + splitedPermission[i] + PATH_CONSTANT;
                Collection permissionNode = tenantGovReg.newCollection();
                permissionNode.setProperty("name", splitedPermission[i]);
                tenantGovReg.put(permissinPath, permissionNode);
            }

        }
    }

    /**
     * Loads the permissions of the application
     *
     * @param applicationName
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static List<ApplicationPermission> loadPermissions(String applicationName)
            throws IdentityApplicationManagementException {
        applicationNode = getApplicationPermissionPath() + PATH_CONSTANT + applicationName;
        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        try {
            boolean exist = tenantGovReg.resourceExists(applicationNode);

            if (!exist) {
                return Collections.emptyList();
            }

            boolean loggedInUserChanged = false;
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();

            UserRealm realm = (UserRealm) CarbonContext.getThreadLocalCarbonContext().getUserRealm();
            if (loggedInUser == null || !realm.getAuthorizationManager().isUserAuthorized(
                    loggedInUser, applicationNode, UserMgtConstants.EXECUTE_ACTION)) {
                //Logged in user is not authorized to read the permission.
                // Temporarily change the user to the admin for reading the permission
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(
                        realm.getRealmConfiguration().getAdminUserName());
                tenantGovReg = CarbonContext.getThreadLocalCarbonContext()
                        .getRegistry(RegistryType.USER_GOVERNANCE);
                loggedInUserChanged = true;
            }


            paths.clear();             //clear current paths
            List<ApplicationPermission> permissions = new ArrayList<ApplicationPermission>();


            permissionPath(tenantGovReg, applicationNode);      //get permission paths recursively

            for (String permissionPath : paths) {
                ApplicationPermission permission;
                permission = new ApplicationPermission();
                permission.setValue(permissionPath);
                permissions.add(permission);
            }

            if (loggedInUserChanged) {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(loggedInUser);
            }

            return permissions;

        } catch (RegistryException | org.wso2.carbon.user.core.UserStoreException e) {
            throw new IdentityApplicationManagementException("Error while reading permissions", e);
        }
    }

    private static void permissionPath(Registry tenantGovReg, String permissionPath) throws RegistryException {

        Collection appCollection = (Collection) tenantGovReg.get(permissionPath);
        String[] childern = appCollection.getChildren();

        if (childern == null || childern.length == 0) {
            paths.add(permissionPath.replace(applicationNode, "").substring(2));
        }

        while (childern != null && childern.length != 0) {
            for (int i = 0; i < childern.length; i++) {
                permissionPath(tenantGovReg, childern[i]);
            }
            break;

        }
    }

    /**
     * Delete the resource
     *
     * @param applicationName
     * @throws IdentityApplicationManagementException
     */
    public static void deletePermissions(String applicationName) throws IdentityApplicationManagementException {

        String applicationNode = getApplicationPermissionPath() + PATH_CONSTANT + applicationName;
        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        try {
            boolean exist = tenantGovReg.resourceExists(applicationNode);

            if (exist) {
                tenantGovReg.delete(applicationNode);
            }

        } catch (RegistryException e) {
            throw new IdentityApplicationManagementException("Error while storing permissions", e);
        }
    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    public static Property[] concatArrays(Property[] o1, Property[] o2) {
        Property[] ret = new Property[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }


    public static String getApplicationPermissionPath() {

        return CarbonConstants.UI_PERMISSION_NAME + RegistryConstants.PATH_SEPARATOR + APPLICATION_ROOT_PERMISSION;

    }
}
