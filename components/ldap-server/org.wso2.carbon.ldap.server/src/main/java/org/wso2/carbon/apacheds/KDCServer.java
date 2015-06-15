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
 * This interface represents a KDC (Key Distribution Center) server. Currently we have an apacheDS
 * based KDC server implementation.
 */
@SuppressWarnings({"UnusedDeclaration"})
public interface KDCServer {

    /**
     * Initializes the KDC server.
     *
     * @param configuration Configuration of the KDC server. This includes,
     *                      host name to use, port numbers to use etc ...     *
     * @param ldapServer    KDC server also needs a user store to authenticate users.
     *                      This variable represents the LDAP server which stores users.
     * @throws DirectoryServerException If an error occurred during initialization.
     */
    void init(final KdcConfiguration configuration, LDAPServer ldapServer)
            throws DirectoryServerException;

    /**
     * Starts the KDC server.
     *
     * @throws DirectoryServerException If an error occurred during startup.
     */
    void start() throws DirectoryServerException;

    /**
     * Once we add a new partition to LDAP server we need to make it a kerberos enabled
     * realm. For this we need to add special server principles to newly added partition.
     * Following method will add those new server principles to the given partition.
     *
     * @param configuration Partition configurations.
     * @param ldapServer    The LDAP server instance which we add the partition.
     * @throws DirectoryServerException If an error occurred during operation.
     */
    public void kerberizePartition(final PartitionInfo configuration, final LDAPServer ldapServer)
            throws DirectoryServerException;

    /**
     * Stops the KDC server.
     *
     * @throws DirectoryServerException If an error occurred during server termination.
     */
    void stop() throws DirectoryServerException;

    /**
     * Says whether KDC server is started or not.
     *
     * @return <code>true</code> if started else <code>false</code>.
     */
    public boolean isKDCServerStarted();


}
