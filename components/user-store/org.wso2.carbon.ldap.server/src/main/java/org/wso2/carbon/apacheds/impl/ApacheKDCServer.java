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

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apacheds.KDCServer;
import org.wso2.carbon.apacheds.KdcConfiguration;
import org.wso2.carbon.apacheds.LDAPServer;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.IOException;
import java.util.Hashtable;

/**
 * An implementation of the KDC server. This uses KDC server which comes with ApacheDS.
 */
public class ApacheKDCServer implements KDCServer {

    private static final Logger logger = LoggerFactory.getLogger(ApacheKDCServer.class);

    private static final int START_PORT = 6088;
    /**
     * the context root for the schema
     */
    protected LdapContext schemaRoot;
    private KdcServer kdcServer;

    public ApacheKDCServer() {
        this.kdcServer = new KdcServer();
    }

    @Override
    public void init(final KdcConfiguration configuration, LDAPServer ldapServer)
            throws DirectoryServerException {

        if (configuration == null) {
            throw new DirectoryServerException("Could not initialize KDC server. " +
                    "KDC configurations are null");
        }

        if (ldapServer == null) {
            throw new DirectoryServerException("Could not initialize KDC server. " +
                    "Directory service is null.");
        }

        if (!(ldapServer instanceof ApacheLDAPServer)) {
            throw new DirectoryServerException("Apache KDC server is only compatible with " +
                    "ApacheLDAPServer");
        }

        ApacheLDAPServer apacheLDAP = (ApacheLDAPServer) ldapServer;

        this.kdcServer.setServiceName(configuration.getKdcName());
        this.kdcServer.setKdcPrincipal(configuration.getKdcPrinciple());
        this.kdcServer.setPrimaryRealm(configuration.getPrimaryRealm());
        this.kdcServer.setMaximumTicketLifetime(configuration.getMaxTicketLifeTime());
        this.kdcServer.setMaximumRenewableLifetime(configuration.getMaxRenewableLifeTime());
        this.kdcServer.setSearchBaseDn(configuration.getSearchBaseDomainName());
        this.kdcServer.setPaEncTimestampRequired(
                configuration.isPreAuthenticateTimeStampRequired());

        configureTransportHandlers(configuration);

        DirectoryService directoryService = apacheLDAP.getService();

        if (directoryService == null) {
            throw new DirectoryServerException("LDAP service is null. " +
                    "Could not configure Kerberos.");
        }

        this.kdcServer.setDirectoryService(directoryService);

        setSchemaContext(configuration, directoryService, ldapServer.getConnectionDomainName());

        enableKerberoseSchema();

    }

    private void enableKerberoseSchema() throws DirectoryServerException {
        // check if krb5kdc is disabled
        Attributes krb5kdcAttrs;
        try {
            krb5kdcAttrs = schemaRoot.getAttributes("cn=Krb5kdc");

            boolean isKrb5KdcDisabled = false;
            if (krb5kdcAttrs.get("m-disabled") != null) {
                isKrb5KdcDisabled = "TRUE".equalsIgnoreCase((String) krb5kdcAttrs.get("m-disabled").get());
            }

            // if krb5kdc is disabled then enable it
            if (isKrb5KdcDisabled) {
                Attribute disabled = new BasicAttribute("m-disabled");
                ModificationItem[] mods =
                        new ModificationItem[]{new ModificationItem(
                                DirContext.REMOVE_ATTRIBUTE, disabled)};
                schemaRoot.modifyAttributes("cn=Krb5kdc", mods);
            }
        } catch (NamingException e) {
            String msg = "An error occurred while enabling Kerberos schema.";
            logger.error(msg, e);
            throw new DirectoryServerException(msg, e);
        }
    }

    @Override
    public void kerberizePartition(final PartitionInfo partitionInfo, final LDAPServer ldapServer)
            throws DirectoryServerException {

        DirContext ctx = null;

        try {

            if (!(ldapServer instanceof ApacheLDAPServer)) {
                throw new DirectoryServerException("Apache KDC server is only compatible with " +
                        "ApacheLDAPServer");
            }

            ApacheLDAPServer apacheLDAP = (ApacheLDAPServer) ldapServer;

            // Get a context, create the ou=users subcontext, then create the 3 principals.
            Hashtable<String, Object> env = new Hashtable<String, Object>();
            env.put(DirectoryService.JNDI_KEY, apacheLDAP.getService());
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    ConfigurationConstants.LDAP_INITIAL_CONTEXT_FACTORY);
            env.put(Context.PROVIDER_URL, ConfigurationConstants.USER_SUB_CONTEXT + "," +
                    partitionInfo.getRootDN());
            env.put(Context.SECURITY_PRINCIPAL, partitionInfo.getAdminDomainName());
            env.put(Context.SECURITY_CREDENTIALS,
                    partitionInfo.getPartitionAdministrator().getAdminPassword());
            env.put(Context.SECURITY_AUTHENTICATION, ConfigurationConstants.SIMPLE_AUTHENTICATION);

            ctx = new InitialDirContext(env);


            // Set KDC principle for this partition
            Attributes attrs = getPrincipalAttributes(ConfigurationConstants.SERVER_PRINCIPLE,
                    ConfigurationConstants.KDC_SERVER_COMMON_NAME,
                    ConfigurationConstants.KDC_SERVER_UID,
                    partitionInfo.getPartitionKdcPassword(),
                    getKDCPrincipleName(partitionInfo));

            ctx.createSubcontext("uid=" + ConfigurationConstants.KDC_SERVER_UID, attrs);

            // Set LDAP principle for this partition
            attrs = getPrincipalAttributes(ConfigurationConstants.SERVER_PRINCIPLE,
                    ConfigurationConstants.LDAP_SERVER_COMMON_NAME,
                    ConfigurationConstants.LDAP_SERVER_UID,
                    partitionInfo.getLdapServerPrinciplePassword(),
                    getLDAPPrincipleName(partitionInfo));

            ctx.createSubcontext("uid=" + ConfigurationConstants.LDAP_SERVER_UID, attrs);

        } catch (NamingException e) {
            String msg = "Unable to add server principles for KDC and LDAP. " +
                    "Incorrect domain names.";
            logger.error(msg, e);
            throw new DirectoryServerException(msg, e);
        } finally {

            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    logger.error("Error closing LDAP context.", e);
                }
            }
        }

    }

    private String getKDCPrincipleName(final PartitionInfo partitionInfo) {

        return ConfigurationConstants.KDC_SERVER_UID + "/" + partitionInfo.getRealm() + "@" +
                partitionInfo.getRealm();
    }

    private String getLDAPPrincipleName(PartitionInfo partitionInfo) {
        // TODO find a way to get host name
        return ConfigurationConstants.LDAP_SERVER_UID + "/" + "localhost" + "@" +
                partitionInfo.getRealm();
    }

    /**
     * Convenience method for creating principals.
     *
     * @param cn           the commonName of the person
     * @param principal    the kerberos principal name for the person
     * @param sn           the surName of the person
     * @param uid          the unique identifier for the person
     * @param userPassword the credentials of the person
     * @return the attributes of the person principal
     */
    protected Attributes getPrincipalAttributes(String sn, String cn, String uid,
                                                String userPassword, String principal) {
        Attributes attributes = new BasicAttributes(true);
        Attribute basicAttribute = new BasicAttribute("objectClass");
        basicAttribute.add("top");
        basicAttribute.add("person"); // sn $ cn
        basicAttribute.add("inetOrgPerson"); // uid
        basicAttribute.add("krb5principal");
        basicAttribute.add("krb5kdcentry");
        attributes.put(basicAttribute);
        attributes.put("cn", cn);
        attributes.put("sn", sn);
        attributes.put("uid", uid);
        attributes.put(SchemaConstants.USER_PASSWORD_AT, userPassword);
        attributes.put(KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principal);
        attributes.put(KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, "0");

        return attributes;
    }

    private void setSchemaContext(KdcConfiguration configuration, DirectoryService service,
                                  String connectionUser)
            throws DirectoryServerException {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(DirectoryService.JNDI_KEY, service);
        env.put(Context.SECURITY_PRINCIPAL, connectionUser);
        env.put(Context.SECURITY_CREDENTIALS, configuration.getSystemAdminPassword());
        env.put(Context.SECURITY_AUTHENTICATION, ConfigurationConstants.SIMPLE_AUTHENTICATION);
        env.put(Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName());

        env.put(Context.PROVIDER_URL, SchemaConstants.OU_SCHEMA);

        try {
            schemaRoot = new InitialLdapContext(env, null);
        } catch (NamingException e) {
            throw new DirectoryServerException(
                    "Unable to create Schema context with user " + connectionUser, e);
        }

    }

    @Override
    public void start()
            throws DirectoryServerException {
        try {
            this.kdcServer.start();
            logger.info("KDC server started ...");
        } catch (IOException e) {

            String msg = "Could not start KDC server due to an IOException";
            logger.error(msg, e);
            throw new DirectoryServerException(msg, e);

        } catch (LdapInvalidDnException e) {
            String msg = "Could not start KDC server due to an error in a domain name.";
            logger.error(msg, e);
            throw new DirectoryServerException(msg, e);
        }
    }

    @Override
    public boolean isKDCServerStarted() {
        return this.kdcServer.isStarted();
    }

    @Override
    public void stop()
            throws DirectoryServerException {

        this.kdcServer.stop();
        logger.info("KDC server stopped ...");

    }

    private void configureTransportHandlers(KdcConfiguration configuration) {

        int port = getServerPort(configuration);
        if (configuration.getKdcCommunicationProtocol() ==
                KdcConfiguration.ProtocolType.UDP_PROTOCOL) {

            logger.info("Starting KDC on UDP mode at port - " + port + " at host - " +
                    configuration.getKdcHostAddress());

            UdpTransport defaultTransport = new UdpTransport(port);
            this.kdcServer.addTransports(defaultTransport);

        } else {

            logger.info("Starting KDC on a TCP port " + port + " at host " +
                    configuration.getKdcHostAddress());
            Transport tcp =
                    new TcpTransport(configuration.getKdcHostAddress(), port,
                            configuration.getNumberOfThreads(),
                            configuration.getBackLogCount());
            this.kdcServer.addTransports(tcp);

        }
    }

    private int getServerPort(KdcConfiguration configuration) {
        int port = configuration.getKdcCommunicationPort();

        if (port == -1) {
            port = AvailablePortFinder.getNextAvailable(START_PORT);
        }

        return port;
    }

}
