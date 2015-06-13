/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.entitlement.dto;

/**
 * Encapsulates the data of entitlement data finder modules
 */
public class EntitlementFinderDataHolder {

    private String name;

    private String[] applicationIds = new String[0];

    private String[] supportedCategory = new String[0];

    private int hierarchicalLevels;

    private boolean fullPathSupported;

    private boolean hierarchicalTree;

    private boolean searchSupported;

    private boolean allApplicationRelated;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getApplicationIds() {
        if (applicationIds != null) {
            return applicationIds.clone();
        } else {
            return new String[0];
        }
    }

    public void setApplicationIds(String[] applicationIds) {
        if (applicationIds != null) {
            this.applicationIds = applicationIds.clone();
        }
    }

    public boolean isFullPathSupported() {
        return fullPathSupported;
    }

    public void setFullPathSupported(boolean fullPathSupported) {
        this.fullPathSupported = fullPathSupported;
    }

    public int getHierarchicalLevels() {
        return hierarchicalLevels;
    }

    public void setHierarchicalLevels(int hierarchicalLevels) {
        this.hierarchicalLevels = hierarchicalLevels;
    }

    public boolean isHierarchicalTree() {
        return hierarchicalTree;
    }

    public void setHierarchicalTree(boolean hierarchicalTree) {
        this.hierarchicalTree = hierarchicalTree;
    }

    public boolean isAllApplicationRelated() {
        return allApplicationRelated;
    }

    public void setAllApplicationRelated(boolean allApplicationRelated) {
        this.allApplicationRelated = allApplicationRelated;
    }

    public String[] getSupportedCategory() {
        if (supportedCategory != null) {
            return supportedCategory.clone();
        } else {
            return new String[0];
        }
    }

    public void setSupportedCategory(String[] supportedCategory) {
        if (supportedCategory != null) {
            this.supportedCategory = supportedCategory.clone();
        }
    }

    public boolean isSearchSupported() {
        return searchSupported;
    }

    public void setSearchSupported(boolean searchSupported) {
        this.searchSupported = searchSupported;
    }
}
