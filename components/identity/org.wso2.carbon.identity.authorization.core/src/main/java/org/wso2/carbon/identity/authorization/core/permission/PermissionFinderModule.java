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

//import org.wso2.carbon.identity.authorization.core.dto.PermissionTreeNodeDTO;

import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * 
 */
public interface PermissionFinderModule {

	/**
	 * initializes the Attribute finder module
	 * 
	 * @param properties
	 *            properties, that need to initialize the module.
	 * @throws Exception
	 *             throws when initialization is failed
	 */
	public void init(Properties properties) throws Exception;

	/**
	 * gets name of this module
	 * 
	 * @return name as String
	 */
	public String getModuleName();

	/**
	 * finds attribute values for given category type
	 * 
	 * @param root
	 *            category of the attribute
	 * @param secondaryRoot
	 * @return Set of attribute values as String Set
	 * @throws Exception
	 *             throws if fails
	 */
	//public PermissionTreeNodeDTO getPermissionTree(String root, String secondaryRoot, String filter);

	public Set<String> getRootNodeNames(String filter);

	public Set<String> getSecondaryRootNodeNames(String primaryRoot, String filter);

	/**
	 * defines whether node (AttributeTreeNodeDTO) is defined by child node name
	 * or by full path name with parent node names
	 * 
	 * @return true or false
	 */
	public boolean isFullPathSupported();

	/**
	 * defines whether nodes (AttributeValueTreeNodeDTOs) are shown in UI by as
	 * a tree or flat
	 * 
	 * @return if as a tree -> true or else -> false
	 */
	public boolean isHierarchicalTree();

	public boolean isSecondaryRootSupported();

	public Set<String> getSupportedActions();

	public String getRootIdentifier();

	public List<String> getNameForChildRootNodeSet();

}
