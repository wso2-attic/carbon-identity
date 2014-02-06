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
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.dto.xsd.Resource;
import org.wso2.carbon.identity.authorization.ui.ErrorStatusBean;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationClient;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationConstants;
import org.wso2.carbon.identity.authorization.ui.IdentityAuthorizationConstants.SessionConstants;
import org.wso2.carbon.registry.core.jdbc.dao.JDBCCommentsDAO;
import org.wso2.carbon.registry.core.jdbc.dataaccess.JDBCDAOManager;

/**
 * 
 * @author venura
 * 
 */
public class ModuleManagerTag extends BaseTag {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final byte UPDATE_ACTIONS = 20;

	private static final String TOKEN_DELETED = "deleted";

	public ModuleManagerTag() {
		super();
	}

	private byte action;
	private int moduleId;

	public byte getAction() {
		return action;
	}

	public void setAction(byte action) {
		this.action = action;
	}

	public int getModuleId() {
		return moduleId;
	}

	public void setModuleId(int moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	protected void process(IdentityAuthorizationClient client, HttpSession session,
	                       HttpServletRequest req) throws Exception {
		if (LOAD == action) {
			if (moduleId <= 0) {
				List<PermissionModule> modules = client.loadModules();
				pageContext.setAttribute(SessionConstants.MODULES, modules,
				                         PageContext.SESSION_SCOPE);
			} else {
				PermissionModule module = findModule(moduleId, session);
				pageContext.setAttribute("module", module);
			}
		} else if (UPDATE_ACTIONS == action) {
			PermissionModule updatedModule = updateActions(req, session);
			client.updateAuthorizedActions(updatedModule);

			ErrorStatusBean bean =
			                       new ErrorStatusBean("info",
			                                           "Successfully saved the authorized actions");
			pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
			                         PageContext.SESSION_SCOPE);

		} else if (INSERT == action) {
			PermissionModule module = createModule(req);
			client.createModule(module);

			ErrorStatusBean bean =
			                       new ErrorStatusBean("info", "Successfully registered the module");
			pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
			                         PageContext.SESSION_SCOPE);

		} else if (DELETE == action) {
			PermissionRequest request = createModuleDeleteRequest(req);
			client.removeModule(request);

			ErrorStatusBean bean = new ErrorStatusBean("info", "Successfully removed the module");
			pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
			                         PageContext.SESSION_SCOPE);

		}

	}

	private PermissionRequest createModuleDeleteRequest(HttpServletRequest req) {
		int moduleId = Integer.parseInt(req.getParameter("id"));
		PermissionRequest request = new PermissionRequest();
		request.setModuleId(moduleId);

		return request;
	}

	private PermissionModule createModule(HttpServletRequest req) {

		PermissionModule module = new PermissionModule();
		String moduleName = req.getParameter("moduleName");
		module.setModuleName(moduleName);

		List<String> newActions = new ArrayList<String>();
		int numberOfNewActions = Integer.parseInt(req.getParameter("numberOfActions"));
		for (int i = 0; i <= numberOfNewActions; i++) {
			String action = req.getParameter("newAction_" + i);
			newActions.add(action);
		}

		module.setActions(newActions.toArray(new String[newActions.size()]));

		int numberOfNewResources = Integer.parseInt(req.getParameter("numberOfResources"));
		List<Resource> resourceList = new ArrayList<Resource>();
		for (int i = 0; i <= numberOfNewResources; i++) {
			String resource = req.getParameter("newResource_" + i);
			Resource res = new Resource();
			res.setName(resource);
			resourceList.add(res);
		}

		module.setResources(resourceList.toArray(new Resource[resourceList.size()]));

		return module;
	}

	private PermissionModule findModule(int moduleId, HttpSession session) {
		PermissionModule module = null;
		List<PermissionModule> modules =
		                                 (List<PermissionModule>) session.getAttribute(SessionConstants.MODULES);
		if (modules != null) {
			for (PermissionModule mod : modules) {
				if (mod.getModuleId() == moduleId) {
					module = mod;
					break;
				}
			}
		} else {
			// Session expired
			pageContext.setAttribute("redirect", "index.jsp");
			return null;
		}

		return module;

	}

	private PermissionModule updateActions(HttpServletRequest req, HttpSession session)
	                                                                                   throws Exception {

		int moduleId = Integer.parseInt(req.getParameter("moduleId"));
		PermissionModule module = findModule(moduleId, session);

		if (module == null) {
			throw new Exception("Module with the id cannot be found within the session");
		}

		List<String> deletedActions = new ArrayList<String>();
		List<String> newActions = new ArrayList<String>();
		int index = 1;
		for (String authorizedAction : module.getActions()) {
			String deleted = req.getParameter("delete_" + index);
			if (authorizedAction.equals(deleted)) {
				// this action is removed
				deletedActions.add(authorizedAction);
			}
			++index;
		}

		int numberOfNewActions = Integer.parseInt(req.getParameter("numberOfActions"));
		for (int i = 1; i <= numberOfNewActions; i++) {
			String action = req.getParameter("newAction_" + i);
			String deleted = req.getParameter("deleteNewAction_" + i);
			if (!TOKEN_DELETED.equals(deleted)) {
				newActions.add(action);
			}
		}

		module.setDeletedActions(deletedActions.toArray(new String[deletedActions.size()]));
		module.setActions(newActions.toArray(new String[newActions.size()]));

		List<Resource> deletedResources = new ArrayList<Resource>();
		List<Resource> newResources = new ArrayList<Resource>();
		index = 1;
		for (Resource res : module.getResources()) {
			String deleted = req.getParameter("delete_" + res.getId());
			if (deleted.equals(String.valueOf(res.getId()))) {
				// this action is removed
				res.setState(IdentityAuthorizationConstants.DELETE);
				deletedResources.add(res);
			}
			++index;
		}

		int numberOfNewResources = Integer.parseInt(req.getParameter("numberOfResources"));
		for (int i = 1; i <= numberOfNewResources; i++) {
			String resource = req.getParameter("newResource_" + i);
			String deleted = req.getParameter("deleteNewResource_" + i);
			if (!TOKEN_DELETED.equals(deleted)) {
				Resource res = new Resource();
				res.setName(resource);
				newResources.add(res);
			}
		}

		List<Resource> resources = new ArrayList<Resource>();
		resources.addAll(deletedResources);
		resources.addAll(newResources);
		module.setResources(resources.toArray(new Resource[resources.size()]));
		return module;

	}

	@Override
	protected void setErrorCodes(Exception e) {
		ErrorStatusBean bean = new ErrorStatusBean("error", e.getMessage());
		pageContext.setAttribute(SessionConstants.ERROR_STATUS_BEAN, bean,
		                         PageContext.SESSION_SCOPE);

	}

}
