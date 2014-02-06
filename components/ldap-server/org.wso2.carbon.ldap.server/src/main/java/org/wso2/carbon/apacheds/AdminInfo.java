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
package org.wso2.carbon.apacheds;

import java.util.Arrays;

/**
 * Wraps Admin information.
 * <partitionAdmin>
 *  <Property name="uid">admin</Property>
 *  <Property name="commonName">admin</Property>
 *  <Property name="surName">admin</Property>
 *  <Property name="email">admin</Property>
 *  <Property name="password">admin</Property>
 *  <Property name="passwordType">SHA</Property>
 * </partitionAdmin>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class AdminInfo extends DomainNameEntry {
    
    private String usernameAttribute;
    
    private String adminUserName;

    private String adminCommonName;

    private String adminLastName;

    private String adminEmail;

    private String adminPassword;

    private AdminGroupInfo groupInformation;

    // Default we will use plain text passwords

    private PasswordAlgorithm passwordAlgorithm = PasswordAlgorithm.PLAIN_TEXT;

    public AdminInfo(String attribute, String adminUserName, String adminCommonName,
                     String adminLastName,
                     String adminEmail, String password,
                     PasswordAlgorithm algorithm, AdminGroupInfo groupInformation) {
        this.adminUserName = adminUserName;
        this.adminCommonName = adminCommonName;
        this.adminLastName = adminLastName;
        this.adminEmail = adminEmail;
        this.adminPassword = password;
        this.passwordAlgorithm = algorithm;
        this.groupInformation = groupInformation;
        this.usernameAttribute = attribute;

        this.objectClassList.addAll(Arrays.asList("top", "person", "organizationalPerson", "inetOrgPerson"));
    }

    public AdminInfo() {
        /*this.objectClassList.addAll(Arrays.asList("top", "person", "organizationalPerson",
                                                  "inetOrgPerson"))*/
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        if (adminPassword == null) return;

        this.adminPassword = adminPassword;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUID) {
        if (adminUID == null) return;

        this.adminUserName = adminUID;
    }

    public String getAdminCommonName() {
        return adminCommonName;
    }

    public void setAdminCommonName(String adminCommonName) {
        if (adminCommonName == null) return;

        this.adminCommonName = adminCommonName;
    }

    public String getAdminLastName() {
        return adminLastName;
    }

    public void setAdminLastName(String adminLastName) {
        if (adminLastName == null) return;

        this.adminLastName = adminLastName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        if (adminEmail == null) return;

        this.adminEmail = adminEmail;
    }

    public AdminGroupInfo getGroupInformation() {
        return groupInformation;
    }

    public void setGroupInformation(AdminGroupInfo groupInformation) {
        if (groupInformation == null) return;

        this.groupInformation = groupInformation;
    }

    public PasswordAlgorithm getPasswordAlgorithm() {
        return passwordAlgorithm;
    }

    public void setPasswordAlgorithm(PasswordAlgorithm passwordAlgorithm) {
        if (passwordAlgorithm == null) return;

        this.passwordAlgorithm = passwordAlgorithm;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }
    
    
}
