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

import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

/**
 * Represents a LDAP server.
 */
public interface LDAPServer {

    /**
     * Initializes directory service. Will create a LDAP server instance using apacheeds
     * APIs.
     *
     * @param configurations This wraps LDAP specific configurations. This includes information
     *                       such as ldap running port, kdc running port, whether to enable kdc server etc ...
     * @throws DirectoryServerException If initialization failed.
     */
    void init(LDAPConfiguration configurations) throws DirectoryServerException;

    /**
     * Will start LDAP server. Must be initialized first.
     *
     * @throws DirectoryServerException If unable to start LDAP server.
     */
    void start() throws DirectoryServerException;

    /**
     * Stopes the LDAP server.
     *
     * @throws DirectoryServerException If unable to stop LDAP server.
     */
    void stop() throws DirectoryServerException;

    /**
     * Gets the partition manager from LDAP server. This is used to create partitions for tenants.
     *
     * @return Returns a {@link PartitionManager}
     * @throws DirectoryServerException If unable to extract PartitionManager from LDAP service
     *                                  context.
     */
    PartitionManager getPartitionManager() throws DirectoryServerException;

    /**
     * Changes the connection user password. As for now we don't allow users to change admin domain
     * name.
     * But users can change admin's password using this method.
     *
     * @param password New password.
     * @throws DirectoryServerException If an error occurred while changing the password.
     */
    void changeConnectionUserPassword(String password)
            throws DirectoryServerException;

    /**
     * Gets system admins connection domain name.
     * Ex :- uid=admin,ou=system
     *
     * @return Admin's domain name as a domain name entry.
     * @throws DirectoryServerException If an error occurred when quarrying the admin user domain
     *                                  name.
     */
    String getConnectionDomainName() throws DirectoryServerException;

}
