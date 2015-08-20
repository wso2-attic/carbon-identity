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

package org.wso2.carbon.ldap.server.configuration;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.log4j.Logger;
import org.wso2.carbon.apacheds.AdminGroupInfo;
import org.wso2.carbon.apacheds.AdminInfo;
import org.wso2.carbon.apacheds.KdcConfiguration;
import org.wso2.carbon.apacheds.LDAPConfiguration;
import org.wso2.carbon.apacheds.PartitionInfo;
import org.wso2.carbon.apacheds.PasswordAlgorithm;
import org.wso2.carbon.ldap.server.exception.DirectoryServerException;
import org.wso2.carbon.ldap.server.util.EmbeddingLDAPException;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.config.RealmConfigXMLProcessor;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is responsible for building LDAP and KDC configurations. Given a file
 * name this will read configuration values and populate them into configuration classes.
 * Related configuration file is embedded-ldap.xml.
 */

public class LDAPConfigurationBuilder {

    private static String CARBON_KDC_PORT_CONFIG_SECTION = "Ports.EmbeddedLDAP.KDCServerPort";
    private static int DEFAULT_KDC_SERVER_PORT = 8000;
    private Logger logger = Logger.getLogger(LDAPConfigurationBuilder.class);
    private String userMgtXMLFilePath = null;
    private InputStream configurationFileStream;
    /*Password to connect with the embedded-ldap server*/
    private String connectionPassword;
    /*contains embedded-ldap server configurations*/
    private LDAPConfiguration ldapConfiguration;
    /*contains default partition's configurations*/
    private PartitionInfo partitionConfigurations;
    /*contains KDC server configurations*/
    private KdcConfiguration kdcConfigurations;
    private boolean kdcEnabled = false;


    /**
     * Constructor with the configuration file as input that reads the file into an InputStream.
     *
     * @param file that includes embedded-ldap server configurations.
     * @throws FileNotFoundException
     */
    public LDAPConfigurationBuilder(File file) throws FileNotFoundException {

        if (!file.exists()) {
            String msg = "File not found. - " + file.getAbsolutePath();
            logger.error(msg);
            throw new FileNotFoundException(msg);
        }

        try {
            configurationFileStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            String msg = "Could not open file - " + file.getAbsolutePath();
            logger.error(msg, e);
            throw new FileNotFoundException(msg);
        }

    }

    /**
     * Build separate sections of the configuration file and the configuration file as a whole, as
     * OMElements.
     *
     * @throws org.wso2.carbon.ldap.server.util.EmbeddingLDAPException Following is a sample configuration file:
     *                                                                 <EmbeddedLDAPConfig>
     *                                                                 <!-- LDAP server configurations -->
     *                                                                 <EmbeddedLDAP>
     *                                                                 <Property name="enable">true</Property>
     *                                                                 <Property name="instanceId">default</Property>
     *                                                                 <Property name="connectionPassword">admin</Property>
     *                                                                 <Property name="workingDirectory">.</Property>
     *                                                                 <Property name="allowAnonymousAccess">false</Property>
     *                                                                 <Property name="accessControlEnabled">true</Property>
     *                                                                 <Property name="denormalizeOpAttrsEnabled">false</Property>
     *                                                                 <Property name="maxPDUSize">2000000</Property>
     *                                                                 <Property name="saslHostName">localhost</Property>
     *                                                                 <Property name="saslPrincipalName">ldap/localhost@EXAMPLE.COM</Property>
     *                                                                 </EmbeddedLDAP>
     *                                                                 <p/>
     *                                                                 <!-- Default partition configurations -->
     *                                                                 <DefaultPartition>
     *                                                                 <Property name="id">root</Property>
     *                                                                 <Property name="realm">wso2.com</Property>
     *                                                                 <Property name="kdcPassword">secret</Property>
     *                                                                 <Property name="ldapServerPrinciplePassword">randall</Property>
     *                                                                 </DefaultPartition>
     *                                                                 <p/>
     *                                                                 <!-- Default partition admin configurations -->
     *                                                                 <PartitionAdmin>
     *                                                                 <Property name="uid">admin</Property>
     *                                                                 <Property name="commonName">admin</Property>
     *                                                                 <Property name="lastName">admin</Property>
     *                                                                 <Property name="email">admin</Property>
     *                                                                 <Property name="password">admin</Property>
     *                                                                 <Property name="passwordType">SHA</Property>
     *                                                                 </PartitionAdmin>
     *                                                                 <p/>
     *                                                                 <!-- Default partition admin's group configuration -->
     *                                                                 <PartitionAdminGroup>
     *                                                                 <Property name="adminRoleName">admin</Property>
     *                                                                 <Property name="groupNameAttribute">cn</Property>
     *                                                                 <Property name="memberNameAttribute">member</Property>
     *                                                                 </PartitionAdminGroup>
     *                                                                 <p/>
     *                                                                 <!-- KDC configurations -->
     *                                                                 <KDCServer>
     *                                                                 <Property name="name">defaultKDC</Property>
     *                                                                 <Property name="enabled">false</Property>
     *                                                                 <Property name="protocol">UDP</Property>
     *                                                                 <Property name="host">localhost</Property>
     *                                                                 <Property name="maximumTicketLifeTime">8640000</Property>
     *                                                                 <Property name="maximumRenewableLifeTime">604800000</Property>
     *                                                                 <Property name="preAuthenticationTimeStampEnabled">true</Property>
     *                                                                 </KDCServer>
     *                                                                 <p/>
     *                                                                 </EmbeddedLDAPConfig>
     */
    public void buildConfigurations() throws EmbeddingLDAPException {

        StAXOMBuilder builder;

        try {
            builder = new StAXOMBuilder(configurationFileStream);
        } catch (XMLStreamException e) {
            logger.error("Unable to build LDAP configurations.", e);
            throw new EmbeddingLDAPException("Unable to build LDAP configurations", e);
        }
        /*Read the whole config file as an OMElement*/
        OMElement documentElement = builder.getDocumentElement();

        /*Extract the part that contains embedded-ldap specific configurations*/
        OMElement embeddedLdap = documentElement.getFirstChildWithName(new QName("EmbeddedLDAP"));
        /*Set properties in ldapConfiguration object from those read from the config element.*/
        buildLDAPConfigurations(embeddedLdap);

        if (ldapConfiguration.isEnable()) {
            /*Set properties in partitionConfigurations object from those read from the config file.*/
            buildPartitionConfigurations(documentElement);

            /*Extract the part that contains kdc-server specific configurations*/
            OMElement kdcConfigElement = documentElement.getFirstChildWithName(new QName("KDCServer"));
            /*Set properties in kdcConfiguration object from those read from the config element.*/
            buildKDCConfigurations(kdcConfigElement);

            /*Says root partition that KDC is enabled. Root partition admin should have KDC object
          attributes in LDAP*/
            this.partitionConfigurations.setKdcEnabled(this.kdcEnabled);

            // Do some cross checking
            if (this.kdcEnabled) {

                this.kdcConfigurations.setSystemAdminPassword(this.getConnectionPassword());

                // Set admin partition for KDC
                this.kdcConfigurations.setPartitionInfo(this.getPartitionConfigurations());

            }
        }
    }

    public String getConnectionPassword() throws EmbeddingLDAPException {
        if (connectionPassword == null) {
            buildConfigurations();
        }
        return connectionPassword;
    }

    public LDAPConfiguration getLdapConfiguration() throws EmbeddingLDAPException {
        if (ldapConfiguration == null) {
            buildConfigurations();
        }
        return ldapConfiguration;
    }

    /**
     * Read and set the connection password from the property map.
     *
     * @param propertyMap : containing properties of EmbeddedLDAP Element.
     * @throws org.wso2.carbon.ldap.server.util.EmbeddingLDAPException
     */
    private void buildConnectionPassword(Map<String, String> propertyMap)
            throws EmbeddingLDAPException {

        connectionPassword = propertyMap.get("connectionPassword");
        if (connectionPassword == null) {
            throw new EmbeddingLDAPException("Connection password not specified in the " +
                    "configuration file.");
        }

    }

    /**
     * Read properties from EmbeddedLDAP element in configuration and set them in the
     * ldapConfiguration object.
     *
     * @param embeddedLDAP: part of the XML config file named: EmbeddedLDAP
     * @throws org.wso2.carbon.ldap.server.util.EmbeddingLDAPException
     */
    private void buildLDAPConfigurations(OMElement embeddedLDAP) throws EmbeddingLDAPException {
        /*Read the properties of EmbeddedLDAP XML element.*/
        Map<String, String> propertyMap = getChildPropertyElements(embeddedLDAP);

        ldapConfiguration = new LDAPConfiguration();
        /*set connectionPassword*/
        buildConnectionPassword(propertyMap);
        String booleanString;

        if ((booleanString = propertyMap.get("accessControlEnabled")) != null) {
            ldapConfiguration.setAccessControlOn(Boolean.parseBoolean(booleanString));
        }

        if ((booleanString = propertyMap.get("allowAnonymousAccess")) != null) {
            ldapConfiguration.setAllowAnonymousAccess(Boolean.parseBoolean(booleanString));
        }

        if ((booleanString = propertyMap.get("changedLogEnabled")) != null) {
            ldapConfiguration.setChangeLogEnabled(Boolean.parseBoolean(booleanString));
        }

        if ((booleanString = propertyMap.get("denormalizeOpAttrsEnabled")) != null) {
            ldapConfiguration.setDeNormalizedAttributesEnabled(Boolean.parseBoolean(booleanString));
        }
        //check and set whether embedded ldap is enabled.
        String enableInfo = propertyMap.get("enable");

        if (("true").equals(enableInfo)) {
            ldapConfiguration.setEnable(true);
        } else {
            ldapConfiguration.setEnable(false);
        }
        ldapConfiguration.setInstanceId(propertyMap.get("instanceId"));

        //Read LDAP server port from carbon.xml and set it
        ldapConfiguration.setLdapPort(getPort(propertyMap.get("port")));

        ldapConfiguration.setWorkingDirectory(propertyMap.get("workingDirectory"));
        ldapConfiguration.setAdminEntryObjectClass(propertyMap.get("AdminEntryObjectClass"));
        ldapConfiguration.setMaxPDUSize(getIntegerValue(propertyMap.get("maxPDUSize")));
        ldapConfiguration.setSaslHostName(propertyMap.get("saslHostName"));
        ldapConfiguration.setSaslPrincipalName(propertyMap.get("saslPrincipalName"));
    }

    /**
     * Set LDAP server port. Port is read from embedded-ldap.conf, and if it refers to port in
     * carbon.xml (Ports/EmbeddedLDAP configuration section), then it is read from carbon.xml
     *
     * @param portParamValue
     * @return The port either read from embedded-ldap.xml or carbon.xml
     */
    private int getPort(String portParamValue) {
        int port = -1;
        if (portParamValue != null) {
            if (portParamValue.startsWith("${")) { // should port be taken from carbon.xml?
                port = CarbonUtils.getPortFromServerConfig(portParamValue);
            } else { //else read from embedded-ldap.xml
                port = Integer.parseInt(portParamValue);
            }
        }
        return port;
    }

    private int getIntegerValue(String value) {
        if (value != null) {
            return Integer.parseInt(value);
        }

        return -1;

    }

    /**
     * Reads the properties mentioned under the XML Element in the config file, which is passed
     * as an OMElement to the method.
     *
     * @param omElement : main XML element whose properties should be read
     * @return : the map containing property names and the values.
     */
    private Map<String, String> getChildPropertyElements(OMElement omElement) {
        Map<String, String> map = new HashMap<String, String>();
        Iterator<?> ite = omElement.getChildrenWithName(new QName(
                UserCoreConstants.RealmConfig.LOCAL_NAME_PROPERTY));
        while (ite.hasNext()) {
            OMElement propElem = (OMElement) ite.next();
            String propName = propElem.getAttributeValue(new QName(
                    UserCoreConstants.RealmConfig.ATTR_NAME_PROP_NAME));
            String propValue = propElem.getText();
            map.put(propName, propValue);
        }
        return map;
    }

    /**
     * Read properties related to default partition and set them in partitionConfigurations object.
     *
     * @param documentElement: whole config file read as an OMElement.
     *                         Following parts are read from the config file:
     *                         <!-- Default partition configurations -->
     *                         <DefaultPartition>
     *                         <Property name="id">root</Property>
     *                         <Property name="realm">wso2.com</Property>
     *                         <Property name="kdcPassword">secret</Property>
     *                         <Property name="ldapServerPrinciplePassword">randall</Property>
     *                         </DefaultPartition>
     *                         <p/>
     *                         <!-- Default partition admin configurations -->
     *                         <PartitionAdmin>
     *                         <Property name="uid">admin</Property>
     *                         <Property name="commonName">admin</Property>
     *                         <Property name="lastName">admin</Property>
     *                         <Property name="email">admin</Property>
     *                         <Property name="password">admin</Property>
     *                         <Property name="passwordType">SHA</Property>
     *                         </PartitionAdmin>
     *                         <p/>
     *                         <!-- Default partition admin's group configuration -->
     *                         <PartitionAdminGroup>
     *                         <Property name="adminRoleName">admin</Property>
     *                         <Property name="groupNameAttribute">cn</Property>
     *                         <Property name="memberNameAttribute">member</Property>
     *                         </PartitionAdminGroup>
     */
    private void buildPartitionConfigurations(OMElement documentElement) {

        this.partitionConfigurations = new PartitionInfo();

        OMElement defaultPartition = documentElement.getFirstChildWithName(new QName("DefaultPartition"));
        Map<String, String> propertyMap = getChildPropertyElements(defaultPartition);

        this.partitionConfigurations.setPartitionId(propertyMap.get("id"));
        this.partitionConfigurations.setRealm(propertyMap.get("realm"));

        this.partitionConfigurations.setPartitionKdcPassword(propertyMap.get("kdcPassword"));
        this.partitionConfigurations.setLdapServerPrinciplePassword(propertyMap.get("ldapServerPrinciplePassword"));
        this.partitionConfigurations.setRootDN(getDomainNameForRealm(propertyMap.get("realm")));

        // Admin user config
        OMElement partitionAdmin = documentElement.getFirstChildWithName(new QName("PartitionAdmin"));
        propertyMap = getChildPropertyElements(partitionAdmin);
        AdminInfo defaultPartitionAdmin = buildPartitionAdminConfigurations(propertyMap);

        // Admin role config
        OMElement partitionAdminRole = documentElement.getFirstChildWithName(new QName("PartitionAdminGroup"));
        propertyMap = getChildPropertyElements(partitionAdminRole);
        AdminGroupInfo adminGroupInfo = buildPartitionAdminGroupConfigurations(propertyMap);

        defaultPartitionAdmin.setGroupInformation(adminGroupInfo);
        this.partitionConfigurations.setPartitionAdministrator(defaultPartitionAdmin);

    }

    private AdminInfo buildPartitionAdminConfigurations(Map<String, String> propertyMap) {
        AdminInfo adminInfo = new AdminInfo();

        adminInfo.setAdminUserName(propertyMap.get("uid"));
        adminInfo.setAdminCommonName(propertyMap.get("firstName"));
        adminInfo.setAdminLastName(propertyMap.get("lastName"));
        adminInfo.setAdminEmail(propertyMap.get("email"));
        adminInfo.setAdminPassword(propertyMap.get("password"));
        adminInfo.setPasswordAlgorithm(PasswordAlgorithm.valueOf(propertyMap.get("passwordType")));
        adminInfo.addObjectClass(ldapConfiguration.getAdminEntryObjectClass());
        adminInfo.setUsernameAttribute("uid");

        return adminInfo;
    }

    private AdminGroupInfo buildPartitionAdminGroupConfigurations(Map<String, String> propertyMap) {
        AdminGroupInfo adminGroupInfo = new AdminGroupInfo();
        adminGroupInfo.setAdminRoleName(propertyMap.get("adminRoleName"));
        adminGroupInfo.setGroupNameAttribute(propertyMap.get("groupNameAttribute"));
        adminGroupInfo.setMemberNameAttribute(propertyMap.get("memberNameAttribute"));
        return adminGroupInfo;
    }

    private String getDomainNameForRealm(String realm) {

        if (realm == null) {
            return null;
        }

        String[] components = realm.split("\\.");

        if (components.length == 0) {
            return "dc=" + realm;
        }

        StringBuilder domainName = new StringBuilder();

        for (int i = 0; i < components.length; ++i) {
            domainName.append("dc=");
            domainName.append(components[i]);

            if (i != (components.length - 1)) {
                domainName.append(",");
            }
        }

        return domainName.toString();
    }

    public PartitionInfo getPartitionConfigurations() throws EmbeddingLDAPException {
        if (partitionConfigurations == null) {
            buildConfigurations();
        }
        return partitionConfigurations;
    }

    public KdcConfiguration getKdcConfigurations() throws EmbeddingLDAPException {
        if (kdcConfigurations == null) {
            buildConfigurations();
        }
        return kdcConfigurations;
    }

    public boolean isKdcEnabled() {
        return kdcEnabled;
    }

    /**
     * Read properties from KDCConfiguration element in configuration and set them in the
     * kdcConfigurations object.
     *
     * @param kdcConfigElement
     * @throws org.wso2.carbon.ldap.server.util.EmbeddingLDAPException <!-- KDC configurations -->
     *                                                                 <KDCServer>
     *                                                                 <Property name="name">defaultKDC</Property>
     *                                                                 <Property name="enabled">false</Property>
     *                                                                 <Property name="protocol">UDP</Property>
     *                                                                 <Property name="host">localhost</Property>
     *                                                                 <Property name="maximumTicketLifeTime">8640000</Property>
     *                                                                 <Property name="maximumRenewableLifeTime">604800000</Property>
     *                                                                 <Property name="preAuthenticationTimeStampEnabled">true</Property>
     *                                                                 </KDCServer>
     */
    private void buildKDCConfigurations(OMElement kdcConfigElement)
            throws EmbeddingLDAPException {
        Map<String, String> propertyMap = getChildPropertyElements(kdcConfigElement);
        String booleanString;
        if ((booleanString = propertyMap.get("enabled")) != null) {
            this.kdcEnabled = Boolean.parseBoolean(booleanString);
            if (!this.kdcEnabled) {
                logger.info("KDC server is disabled.");
                return;
            }
        } else {
            logger.info("KDC server is disabled.");
            return;
        }
        this.kdcConfigurations = new KdcConfiguration();
        this.kdcConfigurations.setKdcName(propertyMap.get("name"));
        try {
            this.kdcConfigurations.setKdcCommunicationProtocol(propertyMap.get("protocol"));
        } catch (DirectoryServerException e) {
            String errorMessage = "Can not read/set protocol parameter in KDCConfig.";
            logger.error(errorMessage, e);
            throw new EmbeddingLDAPException(errorMessage, e);
        }
        this.kdcConfigurations.setKdcHostAddress(propertyMap.get("host"));

        //Read KDC port from carbon.xml and set it
        int port = getPort(propertyMap.get("port"));
        if (port == -1) {
            logger.warn("KDC port defined in carbon.xml's " + CARBON_KDC_PORT_CONFIG_SECTION +
                    " config section or embedded-ldap.xml is invalid. " +
                    "Setting KDC server port to default - " + DEFAULT_KDC_SERVER_PORT);
            port = DEFAULT_KDC_SERVER_PORT;
        }

        this.kdcConfigurations.setKdcCommunicationPort(port);

        this.kdcConfigurations.setMaxTicketLifeTime(getIntegerValue(propertyMap.get(
                "maximumTicketLifeTime")));
        this.kdcConfigurations.setMaxRenewableLifeTime(getIntegerValue(propertyMap.get(
                "maximumRenewableLifeTime")));

        if ((booleanString = propertyMap.get("preAuthenticationTimeStampEnabled")) != null) {
            boolean preAuthenticationTSEnabled = Boolean.parseBoolean(booleanString);
            this.kdcConfigurations.setPreAuthenticateTimeStampRequired(preAuthenticationTSEnabled);
        }
    }

    public boolean isEmbeddedLDAPEnabled() {
        return ldapConfiguration.isEnable();
    }

    protected RealmConfiguration getUserManagementXMLElement() {
        String REALM_CONFIG_FILE = "user-mgt.xml";
        StAXOMBuilder builder = null;
        InputStream inStream = null;
        OMElement realmElement = null;
        RealmConfiguration config = null;
        if (userMgtXMLFilePath == null) {
            String carbonHome = CarbonUtils.getCarbonHome();
            if (carbonHome != null) {
                userMgtXMLFilePath = CarbonUtils.getCarbonConfigDirPath();
            }
        }
        try {
            File userMgtXMLFile = new File(userMgtXMLFilePath,
                    REALM_CONFIG_FILE);
            if (userMgtXMLFile.exists()) {
                inStream = new FileInputStream(userMgtXMLFile);
            }

            builder = new StAXOMBuilder(inStream);
            OMElement documentElement = builder.getDocumentElement();

            realmElement = documentElement.getFirstChildWithName(new QName(
                    UserCoreConstants.RealmConfig.LOCAL_NAME_REALM));
            RealmConfigXMLProcessor rmProcessor = new RealmConfigXMLProcessor();
            config = rmProcessor.buildRealmConfiguration(realmElement);
        } catch (XMLStreamException | FileNotFoundException e) {
            String errorMsg = "User-mgt.xml is not found. " +
                    "Hence admin properties will be read from embedded-ldap.xml";
            if (logger.isDebugEnabled()) {
                logger.debug(errorMsg, e);
            }
        } catch (UserStoreException e) {
            logger.error("Error occured while reading user-mgt.xml", e);
        }

        return config;
    }

    public void setUserMgtXMLFilePath(String filePath) {
        this.userMgtXMLFilePath = filePath;
    }

}



