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

/**
 * 
 */
public class PermissionModuleDTO {

    private String moduleName;

    private boolean fullyPathSupported;

    private boolean hierarchicalTree;

    private boolean secondaryRootSupported;

    private String[] supportedActions = new String[0];

    private String rootIdentifier;

    private String[] rootNodeNames = new String[0];

    private String[] nameForChildRootNodeSet = new String[0];

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String[] getRootNodeNames() {
        return Arrays.copyOf(rootNodeNames, rootNodeNames.length);
    }

    public void setRootNodeNames(String[] rootNodeNames) {
        this.rootNodeNames = Arrays.copyOf(rootNodeNames, rootNodeNames.length);
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

    public boolean isSecondaryRootSupported() {
        return secondaryRootSupported;
    }

    public void setSecondaryRootSupported(boolean secondaryRootSupported) {
        this.secondaryRootSupported = secondaryRootSupported;
    }

    public boolean isHierarchicalTree() {
        return hierarchicalTree;
    }

    public void setHierarchicalTree(boolean hierarchicalTree) {
        this.hierarchicalTree = hierarchicalTree;
    }

    public boolean isFullyPathSupported() {
        return fullyPathSupported;
    }

    public void setFullyPathSupported(boolean fullyPathSupported) {
        this.fullyPathSupported = fullyPathSupported;
    }

    public String[] getNameForChildRootNodeSet() {
        return Arrays.copyOf(nameForChildRootNodeSet, nameForChildRootNodeSet.length);
    }

    public void setNameForChildRootNodeSet(String[] nameForChildRootNodeSet) {
        this.nameForChildRootNodeSet = Arrays.copyOf(nameForChildRootNodeSet, nameForChildRootNodeSet.length);
    }
}
