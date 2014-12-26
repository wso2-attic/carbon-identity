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

package org.wso2.carbon.identity.authorization.core.permission;

import java.util.ArrayList;
import java.util.List;

import org.wso2.carbon.identity.authorization.core.dao.DAOFactory;
import org.wso2.carbon.identity.authorization.core.dao.ModuleDAO;
import org.wso2.carbon.identity.authorization.core.dao.ModuleResourceDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionAssignmentDAO;
import org.wso2.carbon.identity.authorization.core.dao.PermissionDAO;
import org.wso2.carbon.identity.authorization.core.dto.PermissionAssignment;
import org.wso2.carbon.identity.authorization.core.dto.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.Resource;
import org.wso2.carbon.identity.authorization.core.dto.RolePermission;
import org.wso2.carbon.identity.authorization.core.dto.UserPermission;

/**
 * 
 * @author venura
 * @dat May 22, 2013
 */
public final class PermissionMapper {

	private DAOFactory factory;

	private static PermissionMapper instance;

	private PermissionMapper() {
		super();
		factory = DAOFactory.createFactory();
	}

	public static PermissionMapper getInstance() {
		if (instance == null) {
			instance = new PermissionMapper();
		}
		return instance;
	}

	public PermissionDAO mapPermission(final PermissionGroup permission) {
		PermissionDAO perm = factory.createPermission();
		perm.map(permission);
		return perm;
	}

	public PermissionAssignmentDAO mapPermission(final PermissionAssignment permission) {
		PermissionAssignmentDAO perm = null;
		if (permission instanceof UserPermission) {
			perm = factory.createUserPermission();
		} else if (permission instanceof RolePermission) {
			perm = factory.createRolePermission();
		}
		perm.map(permission);
		return perm;
	}

	public ModuleDAO mapModule(PermissionModule module) {
		ModuleDAO dao = factory.createModule();
		dao.map(module);
		return dao;
	}

	public PermissionModule mapModule(ModuleDAO moduleDAO) {
		PermissionModule module = new PermissionModule();
		module.setModuleId(moduleDAO.getModuleId());
		module.setModuleName(moduleDAO.getModuleName());
		if (moduleDAO.getAllowedActions() != null && !moduleDAO.getAllowedActions().isEmpty()) {
			module.setActions(moduleDAO.getAllowedActions()
			                           .toArray(new String[moduleDAO.getAllowedActions().size()]));
		}

		if (moduleDAO.getResources() != null && !moduleDAO.getResources().isEmpty()) {
			List<Resource> resourceList = new ArrayList<Resource>();
			for (ModuleResourceDAO resDao : moduleDAO.getResources()) {
				Resource res = new Resource();
				res.setId(resDao.getId());
				res.setModuleId(resDao.getModuleId());
				res.setName(resDao.getResource());

				resourceList.add(res);
			}
			module.setResources(resourceList.toArray(new Resource[resourceList.size()]));
		}
		return module;
	}

}
