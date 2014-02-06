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

package org.wso2.carbon.identity.authorization.ui.tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.dto.xsd.RolePermission;
import org.wso2.carbon.identity.authorization.ui.ErrorStatusBean;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationConstants.SessionConstants;

/**
 * 
 * @author venura
 * 
 */
public class PermissionsManagerTag extends BaseTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private byte action;

	public byte getAction() {
		return action;
	}

	public void setAction(byte action) {
		this.action = action;
	}

	@Override
	protected void process(IdentityAuthorizationClient client, HttpSession session,
	                       HttpServletRequest req) throws Exception {

		if (INSERT == action) {
			List<PermissionGroup> groups = mapInsertOperation(req);
			client.createPermissions(groups);

			ErrorStatusBean bean =
			                       new ErrorStatusBean("info", "Successfully saved the permissions");
			pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
			                         PageContext.SESSION_SCOPE);
		} else if (LOAD == action) {
			PermissionRequest request = mapPermissionRequest(req);
			PermissionModule permissions = client.getPermissionList(request);
			pageContext.setAttribute("permissionRequest", request, PageContext.SESSION_SCOPE);
			pageContext.setAttribute("permissionsResult", permissions, PageContext.SESSION_SCOPE);
		} else if (DELETE == action) {
			RolePermission[] array = mapRolePermissions(req);
			client.deleteRolePermissions(array);

			PermissionRequest request =
			                            (PermissionRequest) session.getAttribute("permissionRequest");
			PermissionModule permissions = client.getPermissionList(request);
			pageContext.setAttribute("permissionsResult", permissions, PageContext.SESSION_SCOPE);

			ErrorStatusBean bean = new ErrorStatusBean("info", "Successfully deleted permissions");
			pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
			                         PageContext.SESSION_SCOPE);

		}
	}

	private RolePermission[] mapRolePermissions(HttpServletRequest req) throws Exception {
		String deleted = req.getParameter("deleted");
		List<RolePermission> permissions = new ArrayList<RolePermission>();
		if (deleted != null && !deleted.isEmpty()) {
			String[] deletedIds = deleted.split(",");
			for (String id : deletedIds) {
				if (id == null || id.isEmpty()) {
					continue;
				}
				RolePermission permission = new RolePermission();
				permissions.add(permission);
				permission.setId(Integer.parseInt(id));
			}
		}
		return permissions.toArray(new RolePermission[permissions.size()]);
	}

	private PermissionRequest mapPermissionRequest(HttpServletRequest req) {
		PermissionRequest request = new PermissionRequest();
		request.setModule(req.getParameter("application"));
		String resource = req.getParameter("resource");
		if (resource != null && !resource.isEmpty()) {
			request.setResource(resource);
		}
		String role = req.getParameter("role");
		if (role != null && !role.isEmpty()) {
			request.setSubject(role);
			request.setUserPermissions(false);
		}
		return request;
	}

	private List<PermissionGroup> mapInsertOperation(HttpServletRequest req) {

		Map<String, PermissionGroup> permGroupMap = new HashMap<String, PermissionGroup>();
		Map<String, List<RolePermission>> rolePermissionMap =
		                                                      new HashMap<String, List<RolePermission>>();
		String module = req.getParameter("permModule_1");
		int index = 1;

		while (module != null) {

			module = module.split(",")[1];
			String resource = req.getParameter("permResource_" + index);
			String action = req.getParameter("permAction_" + index);

			PermissionGroup group = null;
			String key = module + resource + action;
			if (permGroupMap.containsKey(key)) {
				group = permGroupMap.get(key);
			} else {
				group = new PermissionGroup();
				group.setModuleName(module);
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

	@Override
	protected void setErrorCodes(Exception e) {
		ErrorStatusBean bean =
		                       new ErrorStatusBean("error", "Error while saving permissions, " +
		                                                    e.getMessage());
		pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
		                         PageContext.SESSION_SCOPE);
	}
}
