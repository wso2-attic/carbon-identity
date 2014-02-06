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

package org.wso2.carbon.identity.authorization.ui;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionGroup;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionModule;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionRequest;
import org.wso2.carbon.identity.authorization.core.dto.xsd.RolePermission;
import org.wso2.carbon.identity.authorization.stub.AuthorizationAdminServiceIdentityAuthorizationException;
import org.wso2.carbon.identity.authorization.stub.AuthorizationAdminServiceStub;

/**
 * 
 */
public class IdentityAuthorizationClient {

	private AuthorizationAdminServiceStub stub;

	private static final Log log = LogFactory.getLog(IdentityAuthorizationClient.class);

	/**
	 * Instantiates IdentityAuthorizationClient
	 * 
	 * @param cookie
	 *            For session management
	 * @param backendServerURL
	 *            URL of the back end server
	 * @param configCtx
	 *            ConfigurationContext
	 * @throws org.apache.axis2.AxisFault
	 */
	public IdentityAuthorizationClient(String cookie, String backendServerURL,
	                                   ConfigurationContext configCtx) throws AxisFault {
		String serviceURL = backendServerURL + "AuthorizationAdminService";
		stub = new AuthorizationAdminServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setTimeOutInMilliSeconds(15 * 60 * 1000);
		option.setProperty(HTTPConstants.SO_TIMEOUT, 15 * 60 * 1000);
		option.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 15 * 60 * 1000);
		option.setManageSession(true);
		option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
	}

	public List<PermissionModule> loadModules() {
		try {
			PermissionModule[] modules = stub.loadModules();
			List<PermissionModule> moduleList = new ArrayList<PermissionModule>();
			if (modules != null) {
				for (PermissionModule module : modules) {
					moduleList.add(module);

					if (module.getActions() != null && module.getActions().length > 0) {
						List<String> validActions = new ArrayList<String>();
						for (String action : module.getActions()) {
							if (action != null && !action.equals("null")) {
								validActions.add(action);
							}
						}
						module.setActions(validActions.toArray(new String[validActions.size()]));

					}
				}
				return Arrays.asList(modules);
			}

		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (AuthorizationAdminServiceIdentityAuthorizationException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void createPermissions(List<PermissionGroup> permissions) throws RemoteException,
	                                                              AuthorizationAdminServiceIdentityAuthorizationException {
		PermissionGroup[] groupArray = permissions.toArray(new PermissionGroup[permissions.size()]);
		stub.createPermissions(groupArray);

	}

	public PermissionModule getPermissionList(PermissionRequest req) throws RemoteException,
	                                                              AuthorizationAdminServiceIdentityAuthorizationException {
		return stub.getPermissionList(req);
	}

	public void deleteRolePermissions(RolePermission[] rolePermissions) throws RemoteException,
	                                                                   AuthorizationAdminServiceIdentityAuthorizationException {
		stub.deleteRolePermissions(rolePermissions);
	}

	public void updateAuthorizedActions(PermissionModule module) throws RemoteException,
	                                                        AuthorizationAdminServiceIdentityAuthorizationException {
		stub.updateAuthorizedActions(module);
	}

	public void createModule(PermissionModule module) throws RemoteException,
	                                                 AuthorizationAdminServiceIdentityAuthorizationException {
		stub.createModule(module);
	}

	public String getVersion() throws RemoteException {
		String version = stub.getVersion();
		return version;
	}

	public void removeModule(PermissionRequest request) throws RemoteException, AuthorizationAdminServiceIdentityAuthorizationException {
		stub.removeModule(request);
	}

}
