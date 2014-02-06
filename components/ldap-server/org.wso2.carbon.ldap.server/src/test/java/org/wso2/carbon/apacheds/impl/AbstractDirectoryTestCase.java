package org.wso2.carbon.apacheds.impl;

import org.apache.commons.io.FileUtils;
import org.wso2.carbon.apacheds.AdminGroupInfo;
import org.wso2.carbon.apacheds.AdminInfo;
import org.wso2.carbon.apacheds.DirectoryServiceFactory;
import org.wso2.carbon.apacheds.LDAPConfiguration;
import org.wso2.carbon.apacheds.LDAPServer;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PartitionManager;
import org.wso2.carbon.apacheds.PasswordAlgorithm;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Date: Sep 20, 2010 Time: 12:43:34 PM
 */

public abstract class AbstractDirectoryTestCase extends TestCase {

    protected final static String KDC_ENABLED_TEST_CASE = "testKdcEnabledTest";

    protected LDAPServer embeddedLdap;

    protected DirectoryServerWorker worker;

    protected boolean kdcEnabled = false;

    protected Thread serverThread;

    public void setUp()
        throws Exception {

        String temDir = "tmp";
        File file = new File(".");
        String temDirectory = file.getAbsolutePath() + File.separator + temDir;
        File tempDirectory = new File(temDirectory);

        if (tempDirectory.exists()) {
            try {

                FileUtils.deleteDirectory(tempDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        assertFalse(tempDirectory.exists());

        tempDirectory.mkdir();

        kdcEnabled = false;

        System.setProperty("schema.zip.store.location", "./src/test/resources/is-default-schema.zip");

        System.out.println("Running Test case - " + this.getName());

        if (KDC_ENABLED_TEST_CASE.equals(this.getName())) {
            kdcEnabled = true;
        }

        embeddedLdap =
            DirectoryServiceFactory.createLDAPServer(DirectoryServiceFactory.LDAPServerType.APACHE_DIRECTORY_SERVICE);

        worker = new DirectoryServerWorker(embeddedLdap, tempDirectory.getAbsolutePath());
        serverThread = new Thread(worker);
        serverThread.start();

        while (!worker.isServerStarted()) {
            Thread.sleep(1000 * 10);
        }
        System.out.println("Server started ...");
    }

    public void tearDown()
        throws Exception {

        // If you want to connect using Apache Directory Studio, just uncomment following line
        //Thread.sleep(1000 * 60 * 10);
        System.out.println("Stopping the server ...");
        this.worker.stopServer();

        this.serverThread.join();
        this.serverThread = null;
        //Thread.sleep(1000 * 60);
    }

    protected void addDummyPartition()
        throws DirectoryServerException {
        PartitionManager partitionManager = this.embeddedLdap.getPartitionManager();

        AdminGroupInfo groupInfo = new AdminGroupInfo("cn", "member", "admin");
        AdminInfo adminInfo =
            new AdminInfo("uid", "amilaj", "Amila", "Jayasekara", "amilaj@wso2.com", "iceage", PasswordAlgorithm.SHA,
                          groupInfo);
        PartitionInfo partitionInfo = new PartitionInfo("example", "example.com", "dc=example,dc=com", adminInfo);
        partitionInfo.setKdcEnabled(kdcEnabled);

        partitionManager.addPartition(partitionInfo);
        assertTrue("Partition has not created", partitionManager.partitionInitialized(partitionInfo.getPartitionId()));
    }

    public class MonitorObject {

    }

    class DirectoryServerWorker implements Runnable {

        private boolean stop = false;

        private boolean started = false;

        private LDAPServer ldapServer = null;

        private String temDirectory = null;

        public DirectoryServerWorker(LDAPServer server, String directory) {
            this.ldapServer = server;
            this.temDirectory = directory;
        }

        MonitorObject myMonitorObject = new MonitorObject();

        public void stopServer()
            throws DirectoryServerException {
            synchronized (myMonitorObject) {
                stop = true;
                myMonitorObject.notifyAll();
            }
        }

        public boolean isServerStarted() {
            return started;
        }

        public void run() {

            try {
                LDAPConfiguration config = new LDAPConfiguration();                
                config.setWorkingDirectory(temDirectory);
                ldapServer.init(config);
                ldapServer.start();
                this.started = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (myMonitorObject) {
                while (!stop) {
                    try {
                        myMonitorObject.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            try {
                PartitionManager partitionManager = this.ldapServer.getPartitionManager();
                partitionManager.removeAllPartitions();
                this.ldapServer.stop();
            } catch (DirectoryServerException e) {
                e.printStackTrace();
            }

        }
    }

}
