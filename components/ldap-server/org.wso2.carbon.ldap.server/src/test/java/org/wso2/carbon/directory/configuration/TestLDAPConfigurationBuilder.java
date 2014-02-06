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
package org.wso2.carbon.directory.configuration;

import org.wso2.carbon.apacheds.AdminGroupInfo;
import org.wso2.carbon.apacheds.AdminInfo;
import org.wso2.carbon.apacheds.KdcConfiguration;
import org.wso2.carbon.apacheds.LDAPConfiguration;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PasswordAlgorithm;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import junit.framework.TestCase;
import org.wso2.carbon.ldap.server.configuration.LDAPConfigurationBuilder;
import org.wso2.carbon.ldap.server.util.EmbeddingLDAPException;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileNotFoundException;

public class TestLDAPConfigurationBuilder extends TestCase {

    private LDAPConfigurationBuilder configurationBuilder;

    private static final String configurationFile = "src/test/resources/ldap-config.xml";

    public void setUp ()
            throws DirectoryServerException, FileNotFoundException {
        File f = new File(".");
        System.out.println(f.getAbsolutePath());
        File file = new File(configurationFile);
        assertTrue(file.exists());

        this.configurationBuilder = new LDAPConfigurationBuilder(file);
        this.configurationBuilder.setUserMgtXMLFilePath("src" + File.separator + "test" +
                                                        File.separator + "resources" + File.separator);

    }

    public void testBuildLDAPConfigurations()
            throws XMLStreamException, DirectoryServerException, EmbeddingLDAPException {

        assertEquals(this.configurationBuilder.getConnectionPassword(), "tweety");

        LDAPConfiguration configurations = this.configurationBuilder.getLdapConfiguration();

        assertEquals(configurations.getInstanceId(), "id1");
        assertEquals(configurations.getLdapPort(), 10389); //Should get the default value
        assertEquals(configurations.getSaslHostName(), "ldap.wso2.com");
        assertEquals(configurations.getSaslPrincipalName(), "ldap/localhost@WSO2.COM");
        assertEquals(configurations.getMaxPDUSize(), 5000000);
        assertEquals(configurations.getWorkingDirectory(), "/home/tweety");
        assertEquals(configurations.isAccessControlOn(), false);
        assertEquals(configurations.isAllowAnonymousAccess(), true);
        assertEquals(configurations.isDeNormalizedAttributesEnabled(), true);


    }

    public void testBuildPartitionConfigurations ()
            throws DirectoryServerException, EmbeddingLDAPException {
        PartitionInfo info = this.configurationBuilder.getPartitionConfigurations();

        assertEquals(info.getPartitionId(), "tenant0");
        assertEquals(info.getRealm(), "WSO2.ORG");
        assertEquals(info.isKdcEnabled(), true);
        assertEquals(info.getPartitionKdcPassword(), "sunday");
        assertEquals(info.getLdapServerPrinciplePassword(), "wendesday");
        //assertEquals(info.getAdminDomainName(), "uid=myadmin,ou=Users,dc=wso2,dc=org");
        assertEquals(info.getPreferredDomainComponent(), "wso2");
        assertEquals(info.getRootDN(), "dc=wso2,dc=org");

        /*Commenting out the following part because we moved creating user admin entry and group
         *entry to user core and not reading following config in ldap component anymore.*/

        /*AdminInfo admin = info.getPartitionAdministrator();
        assertNotNull(admin);

        //assertEquals(admin.getAdminUID(), "myadmin");
        assertEquals(admin.getAdminCommonName(), "me");
        assertEquals(admin.getAdminLastName(), "admin");
        assertEquals(admin.getAdminEmail(), "admin@example.com");
        assertEquals(admin.getAdminPassword(), "admin");
        assertEquals(admin.getPasswordAlgorithm(), PasswordAlgorithm.SHA);

        AdminGroupInfo groupInfo = admin.getGroupInformation();

        //assertEquals(groupInfo.getAdminRoleName(), "super");
        assertEquals(groupInfo.getGroupNameAttribute(), "cn");
        assertEquals(groupInfo.getMemberNameAttribute(), "member");*/


    }

    public void testBuildKDCConfigurations ()
            throws DirectoryServerException, EmbeddingLDAPException {

        KdcConfiguration kdcConfigs = this.configurationBuilder.getKdcConfigurations();

        assertTrue(this.configurationBuilder.isKdcEnabled());

        assertEquals(kdcConfigs.getKdcCommunicationProtocol(), KdcConfiguration.ProtocolType.TCP_PROTOCOL);
        assertEquals(kdcConfigs.getKdcHostAddress(), "ldap.wso2.com");
        assertEquals(kdcConfigs.getKdcCommunicationPort(), 8000); // should get the default value
        assertEquals(kdcConfigs.getMaxTicketLifeTime(), 8640000);
        assertEquals(kdcConfigs.getMaxRenewableLifeTime(), 604800000);
        assertFalse(kdcConfigs.isPreAuthenticateTimeStampRequired());

       
    }

}
