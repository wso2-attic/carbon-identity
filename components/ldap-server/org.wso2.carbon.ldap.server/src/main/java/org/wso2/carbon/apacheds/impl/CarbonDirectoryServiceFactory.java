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
package org.wso2.carbon.apacheds.impl;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.JdbmPartitionFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import java.io.File;
import java.util.List;

class CarbonDirectoryServiceFactory implements DirectoryServiceFactory {

    /**
     * A logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(CarbonDirectoryServiceFactory.class);
    /*Partition cache size is expressed as number of entries*/
    private static final int PARTITION_CACHE_SIZE = 500;
    private static final int INDEX_CACHE_SIZE = 100;
    /**
     * The default factory returns stock instances of a apacheds service with smart defaults
     */
    public static final DirectoryServiceFactory DEFAULT = new CarbonDirectoryServiceFactory();
    /**
     * The apacheds service.
     */
    private DirectoryService directoryService;
    /**
     * The partition factory.
     */
    private PartitionFactory partitionFactory;
    private String schemaZipStore;

    /* default access */

    @SuppressWarnings({"unchecked"})
    CarbonDirectoryServiceFactory() {
        try {
            // creating the instance here so that
            // we we can set some properties like access control, anon access
            // before starting up the service
            directoryService = new DefaultDirectoryService();

        } catch (Exception e) {
            String errorMessage = "Error in initializing the default directory service.";
            LOG.error(errorMessage);
            throw new RuntimeException(errorMessage, e);
        }

        try {
            String typeName = System.getProperty("apacheds.partition.factory");
            if (typeName != null) {
                Class<? extends PartitionFactory> type = (Class<? extends
                        PartitionFactory>) Class.forName(typeName);
                partitionFactory = type.newInstance();
            } else {
                partitionFactory = new JdbmPartitionFactory();
            }
        } catch (Exception e) {
            String errorMessage = "Error instantiating custom partition factory";
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(String name)
            throws Exception {

        this.schemaZipStore = System.getProperty("schema.zip.store.location");

        if (this.schemaZipStore == null) {
            throw new DirectoryServerException(
                    "Schema Jar repository is not set. Please set schema.jar.location property " +
                            "with proper schema storage");
        }

        if (directoryService != null && directoryService.isStarted()) {
            return;
        }

        build(name);
    }

    /**
     * Build the working apacheds
     *
     * @param name Name of the working directory.
     */
    private void buildWorkingDirectory(String name) {
        String workingDirectory = System.getProperty("workingDirectory");

        if (workingDirectory == null) {
            workingDirectory = System.getProperty("java.io.tmpdir") + File.separator +
                    "server-work-" + name;
        }

        directoryService.setWorkingDirectory(new File(workingDirectory));
    }

    /**
     * Inits the schema and schema partition.
     *
     * @throws Exception If unable to extract schema files.
     */
    private void initSchema()
            throws Exception {
        SchemaPartition schemaPartition = directoryService.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = directoryService.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory(workingDirectory + File.separator + "schema");

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File(workingDirectory, "schema");
        if (!schemaRepository.exists()) {
            SchemaLdifExtractor extractor =
                    new CarbonSchemaLdifExtractor(new File(workingDirectory),
                            new File(this.schemaZipStore));
            extractor.extractOrCopy();
        }

        schemaPartition.setWrappedPartition(ldifPartition);

        SchemaLoader loader = new LdifSchemaLoader(schemaRepository);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        directoryService.setSchemaManager(schemaManager);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager(schemaManager);

        List<Throwable> errors = schemaManager.getErrors();

        if (!errors.isEmpty()) {
            throw new DirectoryServerException(I18n.err(I18n.ERR_317, ExceptionUtils.printErrors(errors)));
        }
    }

    /**
     * Inits the system partition.
     *
     * @throws Exception the exception
     */
    private void initSystemPartition()
            throws Exception {
        // change the working apacheds to something that is unique
        // on the system and somewhere either under target apacheds
        // or somewhere in a temp area of the machine.

        // Inject the System Partition
        Partition systemPartition = partitionFactory.createPartition(
                "system", ServerDNConstants.SYSTEM_DN, PARTITION_CACHE_SIZE,
                new File(directoryService.getWorkingDirectory(), "system"));
        systemPartition.setSchemaManager(directoryService.getSchemaManager());

        partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT,
                INDEX_CACHE_SIZE);

        directoryService.setSystemPartition(systemPartition);
    }

    /**
     * Builds the apacheds server instance.
     *
     * @param name the instance name
     * @throws Exception In case if unable to extract schema or if an error occurred when building
     *                   the working directory.
     */
    private void build(String name)
            throws Exception {
        directoryService.setInstanceId(name);
        buildWorkingDirectory(name);

        // Init the service now
        initSchema();
        initSystemPartition();

        directoryService.startup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryService getDirectoryService()
            throws Exception {
        return directoryService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionFactory getPartitionFactory()
            throws Exception {
        return partitionFactory;
    }

}
