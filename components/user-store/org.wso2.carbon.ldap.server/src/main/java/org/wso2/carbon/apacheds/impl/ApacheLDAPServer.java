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


import org.apache.axiom.om.util.Base64;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.entry.DefaultServerAttribute;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerModification;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apacheds.LDAPConfiguration;
import org.wso2.carbon.apacheds.LDAPServer;
import org.wso2.carbon.apacheds.PartitionManager;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;

import javax.naming.NamingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of LDAP server. This wraps apacheds implementation and provides an
 * abstract interface to operate on directory server.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ApacheLDAPServer implements LDAPServer {

    private static final Logger logger = LoggerFactory.getLogger(ApacheLDAPServer.class);
    private DirectoryService service;
    private LdapServer ldapServer;
    private PartitionManager partitionManager;
    private LDAPConfiguration ldapConfigurations;

    @Override
    public void init(LDAPConfiguration configurations)
            throws DirectoryServerException {

        if (configurations == null) {
            logger.error("LDAP server initialization failed. " +
                    "LDAP server configuration is invalid.");
            throw new DirectoryServerException("Cannot initialize LDAP server. " +
                    "Configuration is null");
        }


        this.ldapConfigurations = configurations;

        try {

            initializeDefaultDirectoryService();
            initializeLDAPServer();

            partitionManager = new ApacheDirectoryPartitionManager(
                    this.service, this.ldapConfigurations.getWorkingDirectory());

        } catch (Exception e) {
            logger.error("LDAP server initialization failed.", e);
            throw new DirectoryServerException("Error initializing ApacheLDAPServer. ", e);
        }

    }

    public DirectoryService getService() {
        return service;
    }

    public void setService(DirectoryService service) {
        this.service = service;
    }

    @Override
    public void start()
            throws DirectoryServerException {

        try {
            this.service.startup();
            this.ldapServer.start();
            logger.info("LDAP server started.");
        } catch (Exception e) {

            logger.error("Error starting LDAP server.", e);
            throw new DirectoryServerException("Can not start the server ", e);
        }
    }

    @Override
    public void stop()
            throws DirectoryServerException {

        try {

            this.ldapServer.stop();
            this.service.shutdown();
            logger.info("LDAP server stopped.");
        } catch (Exception e) {

            logger.error("Error stopping LDAP server.", e);
            throw new DirectoryServerException("Can not start the server ", e);
        }
    }

    @Override
    public PartitionManager getPartitionManager()
            throws DirectoryServerException {

        return this.partitionManager;
    }

    protected void initializeDefaultDirectoryService()
            throws DirectoryServerException {
        try {

            DirectoryServiceFactory factory = CarbonDirectoryServiceFactory.DEFAULT;
            this.service = factory.getDirectoryService();

            configureDirectoryService();

            factory.init(this.ldapConfigurations.getInstanceId());

        } catch (Exception e) {
            throw new DirectoryServerException("Can not start the Default apacheds service ", e);
        }
    }

    private AttributeType getAttributeType(String attributeName)
            throws DirectoryServerException {
        if (this.service != null) {
            SchemaManager schemaManager = this.service.getSchemaManager();
            if (schemaManager != null) {
                AttributeTypeRegistry registry = schemaManager.getAttributeTypeRegistry();
                if (registry != null) {
                    try {
                        String oid = registry.getOidByName(attributeName);
                        return registry.lookup(oid);
                    } catch (LdapException e) {
                        String msg = "An error occurred while querying attribute " + attributeName +
                                " from registry.";
                        logger.error(msg, e);
                        throw new DirectoryServerException(msg, e);
                    }
                } else {
                    String msg = "Could not get attribute registry.";
                    logger.error(msg);
                    throw new DirectoryServerException(msg);

                }

            } else {
                String msg = "Cannot access schema manager. Directory server may not have started.";
                logger.error(msg);
                throw new DirectoryServerException(msg);

            }
        } else {
            String msg = "The directory service is null. LDAP server might not have started.";
            logger.error(msg);
            throw new DirectoryServerException(msg);

        }
    }

    @Override
    public String getConnectionDomainName()
            throws DirectoryServerException {

        LdapPrincipal adminPrinciple = getAdminPrinciple();
        return adminPrinciple.getClonedName().getName();
    }

    private LdapPrincipal getAdminPrinciple()
            throws DirectoryServerException {

        if (this.service != null) {
            CoreSession adminSession;
            try {
                adminSession = this.service.getAdminSession();
            } catch (Exception e) {
                String msg = "An error occurred while retraining admin session.";
                logger.error(msg, e);
                throw new DirectoryServerException(msg, e);
            }
            if (adminSession != null) {
                LdapPrincipal adminPrincipal = adminSession.getAuthenticatedPrincipal();
                if (adminPrincipal != null) {
                    return adminPrincipal;
                } else {
                    String msg = "Could not retrieve admin principle. Failed changing connection " +
                            "user password.";
                    logger.error(msg);
                    throw new DirectoryServerException(msg);
                }
            } else {
                String msg = "Directory admin session is null. The LDAP server may not have " +
                        "started yet.";
                logger.error(msg);
                throw new DirectoryServerException(msg);
            }
        } else {
            String msg = "Directory service is null. The LDAP server may not have started yet.";
            logger.error(msg);
            throw new DirectoryServerException(msg);
        }

    }

    @Override
    public void changeConnectionUserPassword(String password)
            throws DirectoryServerException {

        if (this.service != null) {
            CoreSession adminSession;
            try {
                adminSession = this.service.getAdminSession();
            } catch (Exception e) {
                String msg = "An error occurred while retraining admin session.";
                logger.error(msg, e);
                throw new DirectoryServerException(msg, e);
            }
            if (adminSession != null) {
                LdapPrincipal adminPrincipal = adminSession.getAuthenticatedPrincipal();
                if (adminPrincipal != null) {

                    String passwordToStore = "{" + ConfigurationConstants.ADMIN_PASSWORD_ALGORITHM +
                            "}";

                    MessageDigest messageDigest;
                    try {
                        messageDigest = MessageDigest.getInstance(
                                ConfigurationConstants.ADMIN_PASSWORD_ALGORITHM);
                    } catch (NoSuchAlgorithmException e) {
                        throw new DirectoryServerException(
                                "Could not find digest algorithm - " +
                                        ConfigurationConstants.ADMIN_PASSWORD_ALGORITHM, e);
                    }
                    messageDigest.update(password.getBytes());
                    byte[] bytes = messageDigest.digest();
                    String hash = Base64.encode(bytes);
                    passwordToStore = passwordToStore + hash;

                    adminPrincipal.setUserPassword(passwordToStore.getBytes());

                    EntryAttribute passwordAttribute = new DefaultServerAttribute(
                            getAttributeType("userPassword"));
                    passwordAttribute.add(passwordToStore.getBytes());

                    ServerModification serverModification =
                            new ServerModification(ModificationOperation.REPLACE_ATTRIBUTE,
                                    passwordAttribute);

                    List<Modification> modifiedList = new ArrayList<Modification>();
                    modifiedList.add(serverModification);

                    try {
                        adminSession.modify(adminPrincipal.getClonedName(), modifiedList);
                    } catch (Exception e) {
                        String msg = "Failed changing connection user password.";
                        logger.error(msg, e);
                        throw new DirectoryServerException(msg, e);
                    }

                } else {
                    String msg = "Could not retrieve admin principle. Failed changing connection " +
                            "user password.";
                    logger.error(msg);
                    throw new DirectoryServerException(msg);
                }
            } else {
                String msg = "Directory admin session is null. The LDAP server may not have " +
                        "started yet.";
                logger.error(msg);
                throw new DirectoryServerException(msg);
            }
        } else {
            String msg = "Directory service is null. The LDAP server may not have started yet.";
            logger.error(msg);
            throw new DirectoryServerException(msg);
        }

    }

    private void configureDirectoryService()
            throws NamingException, DirectoryServerException {

        if (null == this.ldapConfigurations) {
            throw new DirectoryServerException("Directory service is not initialized.");
        }

        System.setProperty("workingDirectory", this.ldapConfigurations.getWorkingDirectory());

        this.service.setShutdownHookEnabled(false);

        this.service.setInstanceId(this.ldapConfigurations.getInstanceId());
        this.service.setAllowAnonymousAccess(this.ldapConfigurations.isAllowAnonymousAccess());
        this.service.setAccessControlEnabled(this.ldapConfigurations.isAccessControlOn());
        this.service.setDenormalizeOpAttrsEnabled(
                this.ldapConfigurations.isDeNormalizedAttributesEnabled());
        this.service.setMaxPDUSize(this.ldapConfigurations.getMaxPDUSize());

        this.service.getChangeLog().setEnabled(this.ldapConfigurations.isChangeLogEnabled());

        // Add interceptors
        List<Interceptor> list = this.service.getInterceptors();
        list.add(new KeyDerivationInterceptor());
        this.service.setInterceptors(list);

    }

    protected void initializeLDAPServer()
            throws DirectoryServerException {

        if (null == this.service || null == this.ldapConfigurations) {
            throw new DirectoryServerException(
                    "The default apacheds service is not initialized. " +
                            "Make sure apacheds service is initialized first.");
        }

        this.ldapServer = new LdapServer();

        this.ldapServer.setTransports(new TcpTransport(this.ldapConfigurations.getLdapPort()));

        // set server initial properties
        this.ldapServer.setAllowAnonymousAccess(false);
        this.ldapServer.setMaxTimeLimit(this.ldapConfigurations.getMaxTimeLimit());
        this.ldapServer.setMaxSizeLimit(this.ldapConfigurations.getMaxSizeLimit());
        this.ldapServer.setSaslHost(this.ldapConfigurations.getSaslHostName());
        this.ldapServer.setSaslPrincipal(this.ldapConfigurations.getSaslPrincipalName());

        // add the apacheds service
        this.ldapServer.setDirectoryService(this.service);

        setupSaslMechanisms();

        try {
            this.ldapServer.addExtendedOperationHandler(new StartTlsHandler());
            this.ldapServer.addExtendedOperationHandler(
                    new StoredProcedureExtendedOperationHandler());
        } catch (Exception e) {
            throw new DirectoryServerException("can not add the extension handlers ", e);
        }
    }

    private void setupSaslMechanisms() {
        Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String, MechanismHandler>();

        mechanismHandlerMap.put(SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler());

        CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
        mechanismHandlerMap.put(SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler);

        DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
        mechanismHandlerMap.put(SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler);

        GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
        mechanismHandlerMap.put(SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler);

        NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();

        mechanismHandlerMap.put(SupportedSaslMechanisms.NTLM, ntlmMechanismHandler);
        mechanismHandlerMap.put(SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler);

        this.ldapServer.setSaslMechanismHandlers(mechanismHandlerMap);
    }
}
