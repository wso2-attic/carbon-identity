/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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
package org.wso2.carbon.apacheds;

import java.util.Arrays;

/**
 * This class represents the group information for an admin.
 * <partitionAdminGroup>
 * <Property name="adminRoleName">admin</Property>
 * <Property name="groupNameAttribute">admin</Property>
 * <Property name="memberNameAttribute">admin</Property>
 * </partitionAdminGroup>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class AdminGroupInfo extends DomainNameEntry {

    private String adminRoleName;

    private String groupNameAttribute;

    private String memberNameAttribute;

    public AdminGroupInfo(String grpNameAttribute, String groupMemberNameAttribute,
                          String adminRole) {
        this.groupNameAttribute = grpNameAttribute;
        this.memberNameAttribute = groupMemberNameAttribute;
        this.adminRoleName = adminRole;

        this.objectClassList.addAll(Arrays.asList("top", "groupOfNames"));
    }

    public AdminGroupInfo() {
        this.objectClassList.addAll(Arrays.asList("top", "groupOfNames"));
    }

    public String getAdminRoleName() {
        return adminRoleName;
    }

    public void setAdminRoleName(String adminRoleName) {
        if (adminRoleName == null) {
            return;
        }

        this.adminRoleName = adminRoleName;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        if (groupNameAttribute == null) {
            return;
        }

        this.groupNameAttribute = groupNameAttribute;
    }

    public String getMemberNameAttribute() {
        return memberNameAttribute;
    }

    public void setMemberNameAttribute(String memberNameAttribute) {
        if (memberNameAttribute == null) {
            return;
        }

        this.memberNameAttribute = memberNameAttribute;
    }
}
