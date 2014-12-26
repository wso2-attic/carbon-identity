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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
//import org.wso2.carbon.identity.authorization.core.dto.PermissionTreeNodeDTO;
import org.wso2.carbon.identity.authorization.core.internal.AuthorizationServiceComponent;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class CarbonPermissionFinderModule implements PermissionFinderModule {

	private Registry registry;

	private static Log log = LogFactory.getLog(CarbonPermissionFinderModule.class);

	@Override
	public void init(Properties properties) throws Exception {

	}

	@Override
	public String getModuleName() {
		return "CarbonPermissionFinderModule";
	}

//	@Override
//	public PermissionTreeNodeDTO getPermissionTree(String root, String secondaryRoot, String filter) {
//
//		PermissionTreeNodeDTO nodeDTO = new PermissionTreeNodeDTO("/");
//
//		try {
//			registry =
//			           AuthorizationServiceComponent.getRegistryService()
//			                                        .getSystemRegistry(CarbonContext.getCurrentContext()
//			                                                                        .getTenantId());
//			nodeDTO = new PermissionTreeNodeDTO("/");
//			getChildResources(nodeDTO, "_system");
//		} catch (RegistryException e) {
//			// ignore
//		}
//
//		return nodeDTO;
//	}

	@Override
	public Set<String> getRootNodeNames(String filter) {

		Set<String> rootNodes = new HashSet<String>();

		rootNodes.add("_system");
		return rootNodes;
	}

	@Override
	public Set<String> getSecondaryRootNodeNames(String primaryRoot, String filter) {
		return null;
	}

	@Override
	public boolean isFullPathSupported() {
		return true;
	}

	@Override
	public boolean isHierarchicalTree() {
		return true;
	}

	@Override
	public boolean isSecondaryRootSupported() {
		return false;
	}

	public Set<String> getSupportedActions() {
		Set<String> actions = new HashSet<String>();
		actions.add("read");
		actions.add("write");
		actions.add("delete");
		return actions;
	}

	public String getRootIdentifier() {
		return "WSO2Carbon";
	}

	/**
	 * This helps to find resources un a recursive manner
	 * 
	 * @param node
	 *            attribute value node
	 * @param parentResource
	 *            parent resource Name
	 * @return child resource set
	 * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
	 *             throws
	 */
//	private PermissionTreeNodeDTO getChildResources(PermissionTreeNodeDTO node,
//	                                                String parentResource) throws RegistryException {
//
//		if (registry.resourceExists(parentResource)) {
//			String[] resourcePath = parentResource.split("/");
//			PermissionTreeNodeDTO childNode =
//			                                  new PermissionTreeNodeDTO(
//			                                                            resourcePath[resourcePath.length - 1]);
//			node.addChildNode(childNode);
//			Resource root = registry.get(parentResource);
//			if (root instanceof Collection) {
//				Collection collection = (Collection) root;
//				String[] resources = collection.getChildren();
//				for (String resource : resources) {
//					getChildResources(childNode, resource);
//				}
//			}
//		}
//		return node;
//	}

	@Override
	public List<String> getNameForChildRootNodeSet() {
		return null;
	}
}
