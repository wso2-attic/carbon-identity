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

import org.wso2.carbon.apacheds.impl.ConfigurationConstants;

import java.util.Arrays;

/**
 * This class encapsulates information needed to create an apacheds partition.
 * <defaultPartition>
 *  <Property name="id">root</Property>
 *  <Property name="realm">wso2.com</Property>
 *  <Property name="kdcEnabled">false</Property>
 *  <Property name="kdcPassword">secret</Property>
 *  <Property name="ldapServerPrinciplePassword">randall</Property>
 * </defaultPartition>
 */
@SuppressWarnings({"UnusedDeclaration"})
public class PartitionInfo extends DomainNameEntry {

    /**
     * An id given to the partition.
     */
    private String partitionId;

    /**
     * Each partition is given a realm. This specifies the realm name.
     */
    private String realm;

    /**
     * The root domain name.
     */
    private String rootDN;

    /**
     * The administrator information of the partition.
     */
    private AdminInfo partitionAdministrator;

    /**
     * If this partition is operating with a KDC, then this partition needs a password.
     */
    private String partitionKdcPassword = "secret";

    /**
     * LDAP server principle password.
     */
    private String ldapServerPrinciplePassword = "randall";

    /**
     * Says whether KDC is enabled for this partition. true if enabled, else false.
     */
    private boolean kdcEnabled = false;

    public PartitionInfo() {                                                                                      
        this.objectClassList.addAll(Arrays.asList("top", "organization", "dcObject",
                                                  "extensibleObject"));
    }

    public PartitionInfo(String partitionId, String realm, String rootDN,
                         AdminInfo partitionAdministrator) {
        this.partitionId = partitionId;
        this.realm = realm;
        this.rootDN = rootDN;
        this.partitionAdministrator = partitionAdministrator;

        this.objectClassList.addAll(Arrays.asList("top", "organization", "dcObject",
                                                  "extensibleObject"));
    }

    public boolean isKdcEnabled() {
        return kdcEnabled;
    }

    public void setKdcEnabled(boolean kdcEnabled) {
        this.kdcEnabled = kdcEnabled;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        if (partitionId == null) return;

        this.partitionId = partitionId;
    }

    public String getRealm() {
        return realm.toUpperCase();
    }

    public void setRealm(String realm) {
        if (realm == null) return;

        this.realm = realm;
    }

    public String getRootDN() {
        return rootDN;
    }

    public void setRootDN(String rootDN) {
        if (rootDN == null) return;

        this.rootDN = rootDN;
    }

    public AdminInfo getPartitionAdministrator() {
        return partitionAdministrator;
    }

    public void setPartitionAdministrator(AdminInfo partitionAdministrator) {
        if (partitionAdministrator == null) return;
        
        this.partitionAdministrator = partitionAdministrator;
    }

    /**
     * This method will construct the admin domain name for given partition.
     * @return Will return domain name in the form, uid=aj,dc=example,dc=com
     */
    public String getAdminDomainName () {
        StringBuilder builder = new StringBuilder("uid=");
        builder.append(this.getPartitionAdministrator().getAdminUserName());
        builder.append(",");
        builder.append(ConfigurationConstants.USER_SUB_CONTEXT);
        builder.append(",");
        builder.append(this.getRootDN());

        return builder.toString(); 
    }

    /**
     * Usually domain DN of a partition will have a domain component.
     * This method will gets the first domain component.
     * E.g :- dc=example,dc=com would return "example" as the domain component.
     * @return First domain component in the domain name. If dc not found in rootDN will return null.
     */
    public String getPreferredDomainComponent() {
        String[] parts = this.rootDN.split(",");

        for (String part : parts) {
            String[] elements = part.split("=");
            if (elements[0].equals("dc")) {
                return elements[1];
            }
        }

        return null;

    }

    public String getPartitionKdcPassword() {
        return partitionKdcPassword;
    }

    public void setPartitionKdcPassword(String partitionKdcPassword) {
        if (partitionKdcPassword == null) return;

        this.partitionKdcPassword = partitionKdcPassword;
    }

    public String getLdapServerPrinciplePassword() {
        return ldapServerPrinciplePassword;
    }

    public void setLdapServerPrinciplePassword(String ldapServerPrinciplePassword) {
        if (ldapServerPrinciplePassword == null) return;
        
        this.ldapServerPrinciplePassword = ldapServerPrinciplePassword;
    }


}
