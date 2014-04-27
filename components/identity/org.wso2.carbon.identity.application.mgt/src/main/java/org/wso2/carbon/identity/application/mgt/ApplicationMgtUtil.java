/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.identity.application.mgt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.registry.api.Collection;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class ApplicationMgtUtil {

    public static final String APPLICATION_ROOT_PERMISSION = "/permission/application";
    public static final String PATH_CONSTANT = "/";

    private static Log log = LogFactory.getLog(ApplicationMgtUtil.class);

    public static org.wso2.carbon.user.api.Permission[] buildPermissions(String applicationName,
            String[] permissions) {

        org.wso2.carbon.user.api.Permission[] permissioSet = null;

        if (permissions != null) {
            permissioSet = new org.wso2.carbon.user.api.Permission[permissions.length];
            int i = 0;
            for (String permissionString : permissions) {
                permissioSet[i] = new org.wso2.carbon.user.api.Permission(applicationName + "\\"
                        + permissionString, "ui.execute");
            }
        }
        return permissioSet;
    }

    public static boolean isUserAuthorized(String applicationName, int applicationID)
            throws IdentityException {

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
     * 
     * @param applicationName
     * @return
     * @throws IdentityException
     */
    public static boolean isUserAuthorized(String applicationName) throws IdentityException {
        String qualifiedUsername = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String tenantUser = UserCoreUtil.removeDomainFromName(qualifiedUsername);
        String user = MultitenantUtils.getTenantAwareUsername(tenantUser);
        String applicationRoleName = UserCoreUtil.addInternalDomainName(applicationName);

        try {
            String[] userRoles = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                    .getUserStoreManager().getRoleListOfUser(user);
            for (String userRole : userRoles) {
                if (applicationRoleName.equals(userRole)) {
                    return true;
                }
            }
        } catch (UserStoreException e) {
            log.error(e.getMessage(), e);
            throw new IdentityException("Error while creating application", e);
        }

        return false;
    }

    /**
     * 
     * @param tenantApplicationNames
     * @return
     * @throws IdentityException
     */
    public static String[] getAuthorizedApps(String[] tenantApplicationNames)
            throws IdentityException {
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
     * @throws IdentityException
     */
    public static void createAppRole(String applicationName) throws IdentityException {
        String roleName = UserCoreUtil.addInternalDomainName(applicationName);
        String qualifiedUsername = CarbonContext.getThreadLocalCarbonContext().getUsername();
        String[] user = { UserCoreUtil.removeDomainFromName(qualifiedUsername) };

        try {
            // create a role for the application and assign the user to that role.
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .addRole(roleName, user, null);
        } catch (UserStoreException e) {
            throw new IdentityException("Error while creating application", e);
        }

    }

    /**
     * Delete the role of the app
     * 
     * @param applicationName
     * @throws IdentityException
     */
    public static void deleteAppRole(String applicationName) throws IdentityException {
        String roleName = UserCoreUtil.addInternalDomainName(applicationName);

        try {
            CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager()
                    .deleteRole(roleName);
        } catch (UserStoreException e) {
            throw new IdentityException("Error while creating application", e);
        }
    }

    /**
     * 
     * @param oldName
     * @param newName
     * @throws IdentityException
     */
    public static void renameRole(String oldName, String newName) throws UserStoreException {

        CarbonContext
                .getThreadLocalCarbonContext()
                .getUserRealm()
                .getUserStoreManager()
                .updateRoleName(UserCoreUtil.addInternalDomainName(oldName),
                        UserCoreUtil.addInternalDomainName(newName));

    }

    /**
     * 
     * @param applicationName
     * @param permissions
     * @throws IdentityException
     */
    public static void storePermission(String applicationName, String[] permissions)
            throws IdentityException {

        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        try {
            boolean exist = tenantGovReg.resourceExists(APPLICATION_ROOT_PERMISSION);
            if (!exist) {
                Collection appRootNode = tenantGovReg.newCollection();
                appRootNode.setProperty("name", "application");
                tenantGovReg.put(APPLICATION_ROOT_PERMISSION, appRootNode);
            }

            if (permissions == null || permissions.length < 1) {
                return;
            }

            // creating the application node in the tree
            String appNode = APPLICATION_ROOT_PERMISSION + PATH_CONSTANT + applicationName;
            Collection appNodeColl = tenantGovReg.newCollection();
            tenantGovReg.put(appNode, appNodeColl);

            // now start storing the permissions
            for (String permission : permissions) {
                String permissinPath = appNode + PATH_CONSTANT + permission;
                Resource permissionNode = tenantGovReg.newResource();
                permissionNode.setProperty("name", permission);
                tenantGovReg.put(permissinPath, permissionNode);
            }

        } catch (RegistryException e) {
            throw new IdentityException("Error while storing permissions", e);
        }
    }

    /**
     * Updates the permissions of the application
     * 
     * @param applicationName
     * @param permissions
     * @throws IdentityException
     */
    public static void updatePermissions(String applicationName, ApplicationPermission[] permissions)
            throws IdentityException {

        String applicationNode = APPLICATION_ROOT_PERMISSION + PATH_CONSTANT + applicationName;

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

            if (permissions == null) {
                return;
            }

            // no permission exist for the application, create new
            if (childern == null || appNodeCollec.getChildCount() < 1) {

                for (ApplicationPermission permission : permissions) {
                    String permissinPath = applicationNode + PATH_CONSTANT + permission.getValue();
                    Resource permissionNode = tenantGovReg.newResource();
                    permissionNode.setProperty("name", permission.getValue());
                    tenantGovReg.put(permissinPath, permissionNode);
                }

            } else { // there are permission
                // lets construct new permissions
                List<String> newPermissions = new ArrayList<String>();
                for (ApplicationPermission permission : permissions) {
                    newPermissions.add(applicationNode + PATH_CONSTANT + permission.getValue());
                }

                // remove deleted permissions
                for (String oldPermission : childern) {
                    if (!newPermissions.contains(oldPermission)) {
                        tenantGovReg.delete(oldPermission);
                    }
                }

                int permissionIndex = (applicationNode + PATH_CONSTANT).length();
                // add new permissions
                List<String> oldPermisions = Arrays.asList(childern);
                for (String newPermission : newPermissions) {
                    if (!oldPermisions.contains(newPermission)) {
                        Resource permissionNode = tenantGovReg.newResource();
                        permissionNode
                                .setProperty("name", newPermission.substring(permissionIndex));
                        tenantGovReg.put(newPermission, permissionNode);
                    }
                }
            }

        } catch (RegistryException e) {
            throw new IdentityException("Error while storing permissions", e);
        }

    }

    /**
     * Loads the permissions of the application
     * 
     * @param applicationName
     * @return
     * @throws IdentityException
     */
    public static List<ApplicationPermission> loadPermissions(String applicationName)
            throws IdentityException {
        String applicationNode = APPLICATION_ROOT_PERMISSION + PATH_CONSTANT + applicationName;
        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        try {
            boolean exist = tenantGovReg.resourceExists(applicationNode);

            if (!exist) {
                return null;
            }

            int permissionIndex = (applicationNode + PATH_CONSTANT).length();
            Collection appCollection = (Collection) tenantGovReg.get(applicationNode);
            String[] permissionPaths = appCollection.getChildren();

            List<ApplicationPermission> permissions = new ArrayList<ApplicationPermission>();

            for (String permissionPath : permissionPaths) {
                ApplicationPermission permission;
                permission = new ApplicationPermission();
                permission.setValue(permissionPath.substring(permissionIndex));
                permissions.add(permission);
            }

            return permissions;

        } catch (RegistryException e) {
            throw new IdentityException("Error while storing permissions", e);
        }
    }

    /**
     * Delete the resource
     * 
     * @param applicationName
     * @throws IdentityException
     */
    public static void deletePermissions(String applicationName) throws IdentityException {

        String applicationNode = APPLICATION_ROOT_PERMISSION + PATH_CONSTANT + applicationName;
        Registry tenantGovReg = CarbonContext.getThreadLocalCarbonContext().getRegistry(
                RegistryType.USER_GOVERNANCE);

        try {
            boolean exist = tenantGovReg.resourceExists(applicationNode);

            if (exist) {
                tenantGovReg.delete(applicationNode);
            }

        } catch (RegistryException e) {
            throw new IdentityException("Error while storing permissions", e);
        }
    }
}
