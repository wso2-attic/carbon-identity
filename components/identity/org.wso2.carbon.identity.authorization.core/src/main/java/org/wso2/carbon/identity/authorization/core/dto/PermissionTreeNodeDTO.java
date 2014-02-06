/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.authorization.core.dto;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class PermissionTreeNodeDTO {

    /**
     * Node name
     */
    private String name;

    /**
     * Category id of this node
     */
    private String rootId;

    /**
     * whether node is defined by child node name or by full path name with parent node names
     */
    private boolean fullPathSupported;

    /**
     * whether node must be shown as hierarchical tree or flat tree in UI
     */
    private boolean hierarchicalTree;

    /**
     * module name which node has been created.
     */
    private String moduleName;


    private String rootIdentifier;


    private String[] supportedActions = new String[0];

    /**
     * children of the Node
     */
    private PermissionTreeNodeDTO[] childNodes = new PermissionTreeNodeDTO[]{};

    public PermissionTreeNodeDTO(String name) {
        this.name = name;
    }

    public PermissionTreeNodeDTO[] getChildNodes() {
        return  Arrays.copyOf(childNodes, childNodes.length);
    }

    public void setChildNodes(PermissionTreeNodeDTO[] childNodes) {
        this.childNodes = Arrays.copyOf(childNodes, childNodes.length);
    }

    public void addChildNode(PermissionTreeNodeDTO node){
        Set<PermissionTreeNodeDTO> valueNodes = new HashSet<PermissionTreeNodeDTO>(Arrays.asList(this.childNodes));
        valueNodes.add(node);
        this.childNodes = valueNodes.toArray(new PermissionTreeNodeDTO[valueNodes.size()]);
    }

    public String getName() {
        return name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public boolean isHierarchicalTree() {
        return hierarchicalTree;
    }

    public void setHierarchicalTree(boolean hierarchicalTree) {
        this.hierarchicalTree = hierarchicalTree;
    }

    public boolean isFullPathSupported() {
        return fullPathSupported;
    }

    public void setFullPathSupported(boolean fullPathSupported) {
        this.fullPathSupported = fullPathSupported;
    }

    public String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    public String getRootIdentifier() {
        return rootIdentifier;
    }

    public void setRootIdentifier(String rootIdentifier) {
        this.rootIdentifier = rootIdentifier;
    }

    public String[] getSupportedActions() {
        return Arrays.copyOf(supportedActions, supportedActions.length);
    }

    public void setSupportedActions(String[] supportedActions) {
        this.supportedActions = Arrays.copyOf(supportedActions, supportedActions.length);
    }
}
