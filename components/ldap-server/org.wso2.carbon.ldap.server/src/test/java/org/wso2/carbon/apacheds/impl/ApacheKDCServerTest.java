package org.wso2.carbon.apacheds.impl;

import org.wso2.carbon.apacheds.AdminGroupInfo;
import org.wso2.carbon.apacheds.AdminInfo;
import org.wso2.carbon.apacheds.DirectoryServiceFactory;
import org.wso2.carbon.apacheds.KDCServer;
import org.wso2.carbon.apacheds.KdcConfiguration;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PartitionManager;
import org.wso2.carbon.apacheds.PasswordAlgorithm;


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
 * Date: Sep 16, 2010 Time: 12:43:53 PM
 */

public class ApacheKDCServerTest extends AbstractDirectoryTestCase {

    private KDCServer server;

    public void tearDown()
        throws Exception {
        //server.stop();
        super.tearDown();
    }

    public void testKDC()
        throws Exception {

        // Create partition
        PartitionManager partitionManager = this.embeddedLdap.getPartitionManager();

        AdminGroupInfo groupInfo = new AdminGroupInfo("cn", "member", "admin");
        AdminInfo adminInfo =
            new AdminInfo("uid", "amilaj", "Amila", "Jayasekara", "amilaj@wso2.com", "iceage", PasswordAlgorithm.SHA,
                          groupInfo);
        PartitionInfo partitionInfo = new PartitionInfo("example", "example.com", "dc=example,dc=com", adminInfo);
        partitionInfo.setKdcEnabled(true);

        partitionManager.addPartition(partitionInfo);
        assertTrue("Partition has not created", partitionManager.partitionInitialized(partitionInfo.getPartitionId()));

        KdcConfiguration config = new KdcConfiguration(partitionInfo);
        //config.setPrimaryRealm("EXAMPLE.COM");

        server =
            DirectoryServiceFactory.createKDCServer(DirectoryServiceFactory.LDAPServerType.APACHE_DIRECTORY_SERVICE);
        server.init(config, this.embeddedLdap);

        /*Commenting this out because we moved adding admin user, group entries to embedded-ldap,
        * into user-core. So there is no principal to add kerberos properties*/
        //server.kerberizePartition(partitionInfo, this.embeddedLdap);

        server.start();


    }

}
