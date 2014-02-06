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

package org.wso2.carbon.identity.authorization.ui.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.xsd.RolePermission;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient;

public class ModuleManager extends BaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(ModuleManager.class);

	private static final String ADD = "add";
	private static final String EDIT = "edit";
	private static final String DELETE = "delete";

	@Override
	protected void doProcess(HttpServletRequest req, HttpServletResponse resp,
	                         IdentityAuthorizationClient client) {
		String operation = req.getParameter("op");
		if (ADD.equals(operation)) {
			List<PermissionGroup> groups = mapParameters(req);
			// client.savePermissions(groups);

		}
	}

	private List<PermissionGroup> mapParameters(HttpServletRequest req) {

		String module = req.getParameter("permModule_1");
		int index = 1;

		Map<String, PermissionGroup> permGroupMap = new HashMap<String, PermissionGroup>();
		Map<String, List<RolePermission>> rolePermissionMap =
		                                                      new HashMap<String, List<RolePermission>>();

		while (module != null) {

			String resource = req.getParameter("permResource_" + index);
			String action = req.getParameter("permAction_" + index);

			PermissionGroup group = null;
			String key = module + resource + action;
			if (permGroupMap.containsKey(key)) {
				group = permGroupMap.get(key);
			} else {
				group = new PermissionGroup();
				group.setAction(action);
				group.setResource(resource);
				permGroupMap.put(key, group);
			}

			List<RolePermission> permissionList = null;
			if (rolePermissionMap.containsKey(key)) {
				permissionList = rolePermissionMap.get(key);
			} else {
				permissionList = new ArrayList<RolePermission>();
				rolePermissionMap.put(key, permissionList);
			}

			String[] roleNames = req.getParameter("permRole_" + index).split(",");
			for (String role : roleNames) {
				RolePermission rolePerm = new RolePermission();
				rolePerm.setRoleName(role);
				rolePerm.setAuthorized(true);

				permissionList.add(rolePerm);
			}

			++index;
			module = req.getParameter("permModule_" + index);

		}

		List<PermissionGroup> groupList = new ArrayList<PermissionGroup>();

		Set<Entry<String, PermissionGroup>> entrySet = permGroupMap.entrySet();
		for (Entry<String, PermissionGroup> entry : entrySet) {
			List<RolePermission> roles = rolePermissionMap.get(entry.getKey());
			if (roles != null) {
				entry.getValue()
				     .setRolePermissions(roles.toArray(new RolePermission[roles.size()]));
				groupList.add(entry.getValue());
			}
		}

		return groupList;

	}

}
