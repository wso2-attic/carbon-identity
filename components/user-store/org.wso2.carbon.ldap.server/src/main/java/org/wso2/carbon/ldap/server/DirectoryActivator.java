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

package org.wso2.carbon.ldap.server;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.apacheds.DirectoryServiceFactory;
import org.wso2.carbon.apacheds.KDCServer;
import org.wso2.carbon.apacheds.KdcConfiguration;
import org.wso2.carbon.apacheds.LDAPConfiguration;
import org.wso2.carbon.apacheds.LDAPServer;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PartitionManager;
import org.wso2.carbon.ldap.server.configuration.LDAPConfigurationBuilder;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;
import org.wso2.carbon.ldap.server.tenant.LDAPTenantManagerService;
import org.wso2.carbon.ldap.server.util.EmbeddingLDAPException;
import org.wso2.carbon.user.core.tenant.LDAPTenantManager;

import java.io.File;
import java.io.FileNotFoundException;

public class DirectoryActivator implements BundleActivator {

    private final Logger log = Logger.getLogger(DirectoryActivator.class);

    private LDAPServer ldapServer;
    private KDCServer kdcServer;

    /**
     * This is called at the bundle start, EmbeddedLDAP-server is started and an implementation
     * of LDAPTenantManager is registered in OSGI.
     *
     * @param bundleContext The input bundle context.
     */
    @Override
    public void start(BundleContext bundleContext) {

        try {

            /*Read the embedded-ldap configuration file.*/
            LDAPConfigurationBuilder configurationBuilder = new LDAPConfigurationBuilder(
                    getLdapConfigurationFile());
            /*Make relevant objects that encapsulate different parts of config file.*/
            configurationBuilder.buildConfigurations();

            boolean embeddedLDAPEnabled = configurationBuilder.isEmbeddedLDAPEnabled();
            //start LDAPServer only if embedded-ldap is enabled.
            if (embeddedLDAPEnabled) {

                LDAPConfiguration ldapConfiguration = configurationBuilder.getLdapConfiguration();
                /*set the embedded-apacheds's schema location which is: carbon-home/repository/data/
                is-default-schema.zip
                */
                setSchemaLocation();

                /* Set working directory where schema directory and ldap partitions are created*/
                setWorkingDirectory(ldapConfiguration);

                startLdapServer(ldapConfiguration);

                /* replace default password with that is provided in the configuration file.*/
                this.ldapServer.changeConnectionUserPassword(
                        configurationBuilder.getConnectionPassword());

                // Add admin (default)partition if it is not already created.
                PartitionManager partitionManager = this.ldapServer.getPartitionManager();
                PartitionInfo defaultPartitionInfo = configurationBuilder.getPartitionConfigurations();
                boolean defaultPartitionAlreadyExisted =
                        partitionManager.partitionDirectoryExists(defaultPartitionInfo.getPartitionId());

                if (!defaultPartitionAlreadyExisted) {
                    partitionManager.addPartition(defaultPartitionInfo);
                    if (kdcServer == null) {
                        kdcServer = DirectoryServiceFactory.createKDCServer(DirectoryServiceFactory.LDAPServerType.
                                APACHE_DIRECTORY_SERVICE);
                    }
                    kdcServer.kerberizePartition(configurationBuilder.
                            getPartitionConfigurations(), this.ldapServer);
                } else {
                    partitionManager.initializeExistingPartition(defaultPartitionInfo);
                }

                // Start KDC if enabled
                if (configurationBuilder.isKdcEnabled()) {

                    startKDC(configurationBuilder.getKdcConfigurations());

                }

                //create and register LDAPTenantManager implementation in OSGI.
                LDAPTenantManager ldapTenantManager = new LDAPTenantManagerService(this.ldapServer.
                        getPartitionManager());
                bundleContext.registerService(LDAPTenantManager.class.getName(), ldapTenantManager,
                        null);
                if (log.isDebugEnabled()) {
                    log.debug("apacheds-server component started.");
                }

            } else if (!embeddedLDAPEnabled) {
                //if needed, create a dummy tenant manager service and register it.
                log.info("Embedded LDAP is disabled.");
            }

        } catch (FileNotFoundException | EmbeddingLDAPException | DirectoryServerException e) {
            log.error("Could not start the embedded-ldap. ", e);
        }

    }

    private void setSchemaLocation() throws EmbeddingLDAPException {
        String schemaLocation = "repository" + File.separator + "data" + File.separator +
                "is-default-schema.zip";
        File dataDir = new File(getCarbonHome(), schemaLocation);

        // Set schema location
        System.setProperty("schema.zip.store.location", dataDir.getAbsolutePath());
    }

    private String getCarbonHome() throws EmbeddingLDAPException {
        String carbonHome = System.getProperty("carbon.home");
        if (carbonHome == null) {
            String msg = "carbon.home property not set. Cannot find carbon home directory.";
            log.error(msg);
            throw new EmbeddingLDAPException(msg);
        }

        return carbonHome;
    }

    private File getLdapConfigurationFile() throws EmbeddingLDAPException {

        String configurationFilePath =  "repository" + File.separator + "conf" + File.separator + "identity"
                + File.separator + "embedded-ldap.xml";
        return new File(getCarbonHome(), configurationFilePath);
    }

    private void setWorkingDirectory(LDAPConfiguration ldapConfiguration)
            throws EmbeddingLDAPException {

        if (".".equals(ldapConfiguration.getWorkingDirectory())) {
            File dataDir = new File(getCarbonHome(), "repository/data");
            if (!dataDir.exists() && !dataDir.mkdir()) {
                String msg = "Unable to create data directory at " + dataDir.getAbsolutePath();
                log.error(msg);
                throw new EmbeddingLDAPException(msg);
            }

            File bundleDataDir = new File(dataDir, "org.wso2.carbon.directory");
            if (!bundleDataDir.exists() && !bundleDataDir.mkdirs()) {
                String msg = "Unable to create schema data directory at " + bundleDataDir.
                        getAbsolutePath();
                log.error(msg);
                throw new EmbeddingLDAPException(msg);


            }

            ldapConfiguration.setWorkingDirectory(bundleDataDir.getAbsolutePath());
        }
    }

    private void startLdapServer(LDAPConfiguration ldapConfiguration)
            throws DirectoryServerException {

        this.ldapServer = DirectoryServiceFactory.createLDAPServer(DirectoryServiceFactory.
                LDAPServerType.APACHE_DIRECTORY_SERVICE);

        if (log.isDebugEnabled()) {
            log.debug("Initializing Directory Server with working directory " + ldapConfiguration.
                    getWorkingDirectory() + " and port " + ldapConfiguration.getLdapPort());
        }

        this.ldapServer.init(ldapConfiguration);

        this.ldapServer.start();
    }

    private void startKDC(KdcConfiguration kdcConfiguration)
            throws DirectoryServerException {

        if (kdcServer == null) {
            kdcServer = DirectoryServiceFactory
                    .createKDCServer(DirectoryServiceFactory.LDAPServerType.APACHE_DIRECTORY_SERVICE);
        }
        kdcServer.init(kdcConfiguration, this.ldapServer);

        kdcServer.start();

    }

    @Override
    public void stop(BundleContext bundleContext)
            throws Exception {

        if (this.kdcServer != null) {
            this.kdcServer.stop();
        }

        if (this.ldapServer != null) {

            this.ldapServer.stop();
        }
    }

}


