/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.authorization.core.dao;

import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.Resource;
import org.wso2.carbon.user.core.UserStoreException;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class represents an authorization module. Permissions can be vary for
 * several modules.
 *
 * @author venura
 * @date May 13, 2013
 */
public abstract class ModuleDAO extends GenericDAO {

    private int moduleId;
    private String moduleName;
    private List<String> allowedActions;
    private List<String> deletedActions;

    private List<ModuleResourceDAO> resources;

    protected abstract void loadDependancies(ModuleDAO module, Connection connection)
            throws UserStoreException;

    public int getModuleId() {
        return moduleId;
    }

    public void setModuleId(int moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public List<String> getAllowedActions() {
        if (allowedActions == null) {
            allowedActions = new ArrayList<String>();
        }
        return allowedActions;
    }

    public void setAllowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions;
    }

    public List<String> getDeletedActions() {
        return deletedActions;
    }

    public void setDeletedActions(List<String> deletedActions) {
        this.deletedActions = deletedActions;
    }

    @Override
    public int getIdentifier() {
        return moduleId;
    }

    public List<ModuleResourceDAO> getResources() {
        return resources;
    }

    public void setResources(List<ModuleResourceDAO> resources) {
        this.resources = resources;
    }

    public ModuleDAO map(PermissionModule module) {
        moduleId = module.getModuleId();
        moduleName = module.getModuleName();
        if (module.getActions() != null && module.getActions().length > 0) {
            allowedActions = Arrays.asList(module.getActions());
        }

        if (module.getDeletedActions() != null && module.getDeletedActions().length > 0) {
            deletedActions = Arrays.asList(module.getDeletedActions());
        }

        if (module.getResources() != null && module.getResources().length > 0) {
            resources = new ArrayList<ModuleResourceDAO>();
            for (Resource resource : module.getResources()) {
                ModuleResourceDAO dao = createResource();
                dao.map(resource);
                resources.add(dao);
            }
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(super.toString());
        builder.append("{").append(getClass()).append(" Module Id: ").append(moduleId)
                .append(" Module Name: ").append(moduleName);
        if (allowedActions != null) {
            builder.append(" Allowed actions: ");
            for (String s : allowedActions) {
                builder.append(s).append(", ");
            }
        }

        if (deletedActions != null) {
            builder.append(" Deleted actions: ");
            for (String s : deletedActions) {
                builder.append(s).append(", ");
            }
        }
        builder.append("}");

        return builder.toString();
    }

    protected abstract ModuleResourceDAO createResource();
}
