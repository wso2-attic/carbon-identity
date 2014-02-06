package org.wso2.carbon.apacheds.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apacheds.AdminGroupInfo;
import org.wso2.carbon.apacheds.AdminInfo;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PartitionManager;
import org.wso2.carbon.apacheds.PasswordAlgorithm;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

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
 * Date: Sep 8, 2010 Time: 10:39:07 AM
 */

public class ApacheLDAPServerTest extends AbstractDirectoryTestCase {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheLDAPServerTest.class);

    public void testAddUser()
        throws Exception {

        DirContext ctx = new InitialDirContext(getEnvironmentProperties("secret"));

        addMyUser(ctx, "uid=amilaj,ou=users,ou=system");

        ctx.close();
    }

    @SuppressWarnings({"unchecked"})
    private Hashtable getEnvironmentProperties(String password) {

        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        env.put(Context.PROVIDER_URL, "ldap://localhost:10389/");
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
        env.put(Context.SECURITY_CREDENTIALS, password);

        return env;        
    }

    private void addMyUser(DirContext ctx, String name)
        throws Exception {
        MyUser user = new MyUser("amilaj", "Jayasekara", "Amila");
        ctx.bind(name, user);

        // Lookup
        DirContext obj = (DirContext)ctx.lookup(name);
        assertNotNull(obj);
        LOG.info("User is bound to: " + obj.getNameInNamespace());

    }

    @SuppressWarnings({"unchecked"})
    public void testChangePassword()
        throws Exception {

        DirContext ctx = new InitialDirContext(getEnvironmentProperties("secret"));

        addMyUser(ctx, "uid=amilaj,ou=users,ou=system");

        ctx.close();

        // Now change the password
        this.embeddedLdap.changeConnectionUserPassword("scooby");

        ctx = new InitialDirContext(getEnvironmentProperties("scooby"));

        addMyUser(ctx, "uid=scooby,ou=users,ou=system");
    }

    public void testConnectionUid () throws Exception {
        String uid = this.embeddedLdap.getConnectionDomainName();
        assertEquals("uid=admin,ou=system", uid);        
    }

    public void testAddPartition()
        throws DirectoryServerException {

        addDummyPartition();
    }

    public void testAdd2Partitions()
        throws DirectoryServerException {

        PartitionManager partitionManager = this.embeddedLdap.getPartitionManager();

        AdminGroupInfo groupInfo = new AdminGroupInfo("cn", "member", "admin");
        AdminInfo adminInfo =
            new AdminInfo("uid", "duck", "Donald", "Duck", "dduck@wso2.com", "password", PasswordAlgorithm.MD5, groupInfo);
        PartitionInfo partitionInfo = new PartitionInfo("duck1", "wso2donald.com", "dc=wso2donald,dc=com", adminInfo);

        assertFalse("Partition already exists", partitionManager.partitionInitialized(partitionInfo.getPartitionId()));
        partitionManager.addPartition(partitionInfo);
        assertTrue("Partition has not created", partitionManager.partitionInitialized(partitionInfo.getPartitionId()));

        addDummyPartition();

    }

    public void testKdcEnabledTest()
        throws Exception {
        addDummyPartition();

        DirContext ctx = new InitialDirContext(getEnvironmentProperties("secret"));

        // Get password attribute and check whether it is plain text.
        String[] attrIDs = {"userPassword"};

        /*Commenting out the following part because we moved creating user admin entry and group
         *entry to user core*/

        /*Attributes answer = ctx.getAttributes("uid=amilaj,ou=Users,dc=example,dc=com", attrIDs);

        for (NamingEnumeration ae = answer.getAll(); ae.hasMore();) {
            Attribute attr = (Attribute)ae.next();

            NamingEnumeration e = attr.getAll();
            while (e.hasMore()) {
                String passwd = new String((byte[])e.next());
                assertEquals("iceage", passwd);
            }

        }*/

    }

    public void testRemovePartitions()
        throws Exception {
        addDummyPartition();
        PartitionManager partitionManager = this.embeddedLdap.getPartitionManager();
        partitionManager.removePartition("dc=example,dc=com");
        assertFalse("Partition still exists", partitionManager.partitionInitialized("dc=example,dc=com"));
    }

}


