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

/**
 * Encapsulates LDAP specific configurations.
 * <EmbeddedLDAP>
 * <Property name="enable">true</Property>
 * <Property name="instanceId">default</Property>
 * <Property name="port">10389</Property>
 * <Property name="connectionPassword">admin</Property>
 * <Property name="workingDirectory">.</Property>
 * <Property name="allowAnonymousAccess">false</Property>
 * <Property name="accessControlEnabled">true</Property>
 * <Property name="denormalizeOpAttrsEnabled">false</Property>
 * <Property name="maxPDUSize">2000000</Property>
 * <Property name="saslHostName">localhost</Property>
 * <Property name="saslPrincipalName">ldap/localhost@EXAMPLE.COM</Property>
 * </EmbeddedLDAP>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class LDAPConfiguration {

    private final static String DEFAULT_INSTANCE_ID = "default";

    /**
     * ====================================
     * LDAP server specific configurations
     * ====================================
     */
    private boolean enable = false;

    private int ldapPort = 10389;

    private String workingDirectory = ".";

    private boolean allowAnonymousAccess = false;

    private boolean accessControlOn = true;

    private boolean deNormalizedAttributesEnabled = false;

    private int maxPDUSize = 2000000;

    private boolean changeLogEnabled = false;

    /**
     * ==================================
     * Instance specific configurations
     * ==================================
     */
    private String instanceId = DEFAULT_INSTANCE_ID;
    /*Object class used to create admin entry of a partition. It is hard coded to "inetOrgPerson"
    by default. And it is configurable.*/

    private String adminEntryObjectClass = "inetOrgPerson";

    private int maxTimeLimit = 15000;

    private int maxSizeLimit = 1000;

    private String saslHostName = "localhost";

    private String saslPrincipalName = "ldap/localhost@EXAMPLE.COM";

    public LDAPConfiguration() {
    }

    public boolean isAllowAnonymousAccess() {
        return allowAnonymousAccess;
    }

    public void setAllowAnonymousAccess(boolean allowAnonymousAccess) {
        this.allowAnonymousAccess = allowAnonymousAccess;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isAccessControlOn() {
        return accessControlOn;
    }

    public void setAccessControlOn(boolean accessControlOn) {
        this.accessControlOn = accessControlOn;
    }

    public String getAdminEntryObjectClass() {
        return adminEntryObjectClass;
    }

    public void setAdminEntryObjectClass(String adminEntryObjectClass) {
        this.adminEntryObjectClass = adminEntryObjectClass;
    }

    public boolean isDeNormalizedAttributesEnabled() {
        return deNormalizedAttributesEnabled;
    }

    public void setDeNormalizedAttributesEnabled(boolean deNormalizedAttributesEnabled) {
        this.deNormalizedAttributesEnabled = deNormalizedAttributesEnabled;
    }

    public int getMaxPDUSize() {
        return maxPDUSize;
    }

    public void setMaxPDUSize(int maxPDUSize) {
        if (maxPDUSize == -1) return;

        this.maxPDUSize = maxPDUSize;
    }

    public boolean isChangeLogEnabled() {
        return changeLogEnabled;
    }

    public void setChangeLogEnabled(boolean changeLogEnabled) {
        this.changeLogEnabled = changeLogEnabled;
    }

    public int getMaxTimeLimit() {
        return maxTimeLimit;
    }

    public void setMaxTimeLimit(int maxTimeLimit) {
        if (maxTimeLimit == -1) return;

        this.maxTimeLimit = maxTimeLimit;
    }

    public int getMaxSizeLimit() {
        return maxSizeLimit;
    }

    public void setMaxSizeLimit(int maxSizeLimit) {
        if (maxSizeLimit == -1) return;

        this.maxSizeLimit = maxSizeLimit;
    }

    public String getSaslHostName() {
        return saslHostName;
    }

    public void setSaslHostName(String saslHostName) {
        if (saslHostName == null) return;

        this.saslHostName = saslHostName;
    }

    public String getSaslPrincipalName() {
        return saslPrincipalName;
    }

    public void setSaslPrincipalName(String saslPrincipalName) {
        if (saslPrincipalName == null) return;

        this.saslPrincipalName = saslPrincipalName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        if (instanceId == null) return;

        this.instanceId = instanceId;
    }

    public int getLdapPort() {
        return ldapPort;
    }

    public void setLdapPort(int ldapPort) {
        if (ldapPort == -1) return;

        this.ldapPort = ldapPort;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        if (workingDirectory == null) return;

        this.workingDirectory = workingDirectory;
    }
}
