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
 * Handles apacheds service operations. E.g :- Adding new partitions, removing/deleting existing
 * partitions etc ... In a multi-tenant environment we would have a new partition for each tenant.
 * So we need to manage partitions according to the way tenants are created and deleted.
 */
@SuppressWarnings({"UnusedDeclaration"})
public interface PartitionManager {

    /**
     * Adds a new partition to current apacheds store.
     *
     * @param partitionInformation This contains necessary information to create a partition.
     *                             Mainly it contains following information,
     *                             1. partitionId - The partition id. Usually this is tenant id.
     *                             2. realm domain - name of the partition. Usually domain name and partition DN are same.
     *                             Ex :- o=example.com
     *                             3. partitionDN - Suffix used when creating the partition. Usually this is the domain name.
     *                             DN: dc=example,dc=com
     *                             4. adminInfo - User information for the domain.
     * @throws DirectoryServerException If an error occurs while adding the partition.
     */
    void addPartition(PartitionInfo partitionInformation) throws DirectoryServerException;

    /**
     * This checks whether a partition directory exists with the given partition ID.
     *
     * @param partitionID
     * @return
     * @throws DirectoryServerException
     */
    boolean partitionDirectoryExists(String partitionID) throws DirectoryServerException;

    /**
     * Checks whether a given partition is initialized in the directory service. This differs from
     * partitionExists because, although a partition directory is existed, we need to
     * specifically initialize a partition in the current instance of the directory service.
     *
     * @param partitionId - Partition identifier to check availability.
     * @return true if partition exists else false.
     */
    boolean partitionInitialized(String partitionId);

    /**
     * Removes given partition from apacheds server.
     *
     * @param partitionSuffix Partition suffix to be removed.
     * @throws DirectoryServerException If error occurs during partition deletion.
     */
    void removePartition(String partitionSuffix) throws DirectoryServerException;

    /**
     * Synchronizes the modified partitions.
     *
     * @throws DirectoryServerException If an error occurred during operation.
     */
    void synchronizePartitions() throws DirectoryServerException;

    /**
     * Removes all partitions except system partition.
     *
     * @throws DirectoryServerException If an error occurred during partition removal.
     */
    void removeAllPartitions() throws DirectoryServerException;

    /**
     * count the number of existing partitions
     *
     * @return number of partitions
     */
    int getNumberOfPartitions();

    /**
     * This method initializes a partition from an existing partition directory.
     *
     * @param parttionInfo
     * @throws Exception
     */
    public void initializeExistingPartition(PartitionInfo parttionInfo)
            throws DirectoryServerException;

}

