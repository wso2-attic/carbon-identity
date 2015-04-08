/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.endpoint.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.wso2.securevault.SecurityConstants;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.Collections;
import java.util.Enumeration;

public class TenantDataManager {

    private static final Log log = LogFactory.getLog(TenantDataManager.class);
    private static final String USERNAME = "mutual.ssl.username";
    private static final String USERNAME_HEADER = "username.header";
    private static final String HOST = "identity.server.host";
    private static final String PORT = "identity.server.port";
    private static final String CLIENT_KEY_STORE = "client.keyStore";
    private static final String CLIENT_TRUST_STORE = "client.trustStore";
    private static final String CLIENT_KEY_STORE_PASSWORD = "Carbon.Security.KeyStore.Password";
    private static final String CLIENT_TRUST_STORE_PASSWORD = "Carbon.Security.TrustStore.Password";
    private static final String HOSTNAME_VERIFICATION_ENABLED = "hostname.verification.enabled";
    private static final String RETURN = "return";
    private static final String RETRIEVE_TENANTS_RESPONSE = "retrieveTenantsResponse";
    private static final String TENANT_DOMAIN = "tenantDomain";
    private static final String ACTIVE = "active";
    private static final String TENANT_LIST_ENABLED = "tenantListEnabled";
    private static final String CONFIG_RELATIVE_PATH = "./repository/conf/TenantConfig.properties";
    private static Properties prop;
    private static String carbonLogin = "";
    private static String usernameHeaderName = "";
    private static List<String> tenantDomainList;
    private static boolean isInitialized = false;
    private static boolean configFileExist = true;

    private static synchronized void init() {

        FileInputStream fileInputStream = null;

        try {
            if (!isInitialized) {
                prop = new Properties();
                String configFilePath = buildFilePath(CONFIG_RELATIVE_PATH);
                File configFile = new File(configFilePath);

                if(configFile.exists()){
                    configFileExist = true;
                    fileInputStream = new FileInputStream(configFile);

                    prop.load(fileInputStream);
                    // Resolve encrypted properties with secure vault
                    resolveSecrets(prop);

                    usernameHeaderName = getPropertyValue(USERNAME_HEADER);

                    carbonLogin = getPropertyValue(USERNAME);

                    byte[] base64EncodedByteArray =
                            new Base64().encode(carbonLogin.getBytes("UTF-8"));

                    carbonLogin = new String(base64EncodedByteArray); //Base64 encode username

                    String clientKeyStorePath = buildFilePath(getPropertyValue(CLIENT_KEY_STORE));
                    String clientTrustStorePath = buildFilePath(getPropertyValue(CLIENT_TRUST_STORE));

                    MutualSSLClient.loadKeyStore(clientKeyStorePath, getPropertyValue(CLIENT_KEY_STORE_PASSWORD));
                    MutualSSLClient.loadTrustStore(clientTrustStorePath, getPropertyValue(CLIENT_TRUST_STORE_PASSWORD));
                    MutualSSLClient.initMutualSSLConnection(
                            Boolean.parseBoolean(getPropertyValue(HOSTNAME_VERIFICATION_ENABLED)));

                    tenantDomainList = new ArrayList<String>();

                    isInitialized = true;
                } else {
                    configFileExist = false;

                    if (log.isDebugEnabled()) {
                        log.debug("Configuration file " + configFilePath + " not found");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Initialization failed : ", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error("Failed to close the FileInputStream, file : " + CONFIG_RELATIVE_PATH, e);
                }
            }
        }
    }

    private static String buildFilePath(String path) throws IOException {

        if(path != null && path.startsWith(".")){ //relative path is given
            File currentDirectory = new File(new File(".").getAbsolutePath());
            path = currentDirectory.getCanonicalPath() + File.separator + path;
        }

        if(log.isDebugEnabled()){
            log.debug("File path for KeyStore/TrustStore : " + path);
        }
        return path;
    }

    private static String getPropertyValue(String key) {

        return prop.getProperty(key);
    }

    private static String getServiceResponse(String url) {

        try {
            Map<String, String> headerParams = new HashMap<String, String>();
            headerParams.put(usernameHeaderName, carbonLogin);
            return MutualSSLClient.sendGetRequest(url, null, headerParams);
        } catch (Exception e) {
            log.error("Processing request for " + url + " Failed : " , e);
            return null;
        }
    }

    public static List<String> getAllActiveTenantDomains() {

        if (!isInitialized) {
            init();
        }
        if (tenantDomainList == null || tenantDomainList.size() == 0) {
            refreshActiveTenantDomainsList();
        }
        return tenantDomainList;
    }

    public static void setTenantDataList(String dataList) {

        if (!isInitialized) {
            init();
        }

        if (dataList != null && dataList.trim().length() > 0) {

            synchronized (tenantDomainList) {
                String[] domains = dataList.split(",");
                tenantDomainList = new ArrayList<String>();
                for (String domain : domains) {
                    tenantDomainList.add(domain);
                }

                Collections.sort(tenantDomainList);
            }
        } else {
            tenantDomainList = null;
        }
    }

    private static void refreshActiveTenantDomainsList() {

        try {
            String url = "https://" + getPropertyValue(HOST) + ":" + getPropertyValue(PORT) +
                    "/services/TenantMgtAdminService/retrieveTenants";

            String xmlString = getServiceResponse(url);

            if (xmlString != null && !"".equals(xmlString)) {

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();

                InputSource inputSource = new InputSource(new StringReader(xmlString));
                String xPathExpression =
                        "/*[local-name() = '" + RETRIEVE_TENANTS_RESPONSE +
                                "']/*[local-name() = '" +
                                RETURN + "']";
                NodeList nodeList =
                        (NodeList) xpath
                                .evaluate(xPathExpression, inputSource, XPathConstants.NODESET);
                tenantDomainList = new ArrayList<String>();

                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        NodeList tenantData = element.getChildNodes();
                        boolean activeChecked = false;
                        boolean domainChecked = false;
                        boolean isActive = false;
                        String tenantDomain = null;

                        for (int j = 0; j < tenantData.getLength(); j++) {

                            Node dataItem = tenantData.item(j);
                            String localName = dataItem.getLocalName();

                            if (ACTIVE.equals(localName)) {
                                activeChecked = true;
                                if ("true".equals(dataItem.getTextContent())) {
                                    isActive = true;
                                }
                            }

                            if (TENANT_DOMAIN.equals(localName)) {
                                domainChecked = true;
                                tenantDomain = dataItem.getTextContent();
                            }

                            if (activeChecked && domainChecked) {
                                if (isActive) {
                                    tenantDomainList.add(tenantDomain);
                                }
                                break;
                            }
                        }
                    }
                }

                Collections.sort(tenantDomainList);
            }

        } catch (Exception e) {
            if(log.isDebugEnabled()) {
                log.debug("Retrieving Active Tenant Domains Failed. Ignore this if there are no tenants : ", e);
            }
        }
    }

    public static boolean isTenantListEnabled() {
        if (configFileExist && !isInitialized) {
            init();
        }
        if (configFileExist) {
            return Boolean.parseBoolean(getPropertyValue(TENANT_LIST_ENABLED));
        }

        return false;
    }

    /**
     * There can be sensitive information like passwords in configuration file. If they are encrypted using secure
     * vault, this method will resolve them and replace with original values.
     */
    private static void resolveSecrets(Properties properties) {

        SecretResolver secretResolver = SecretResolverFactory.create(properties);
        Enumeration propertyNames = properties.propertyNames();
        if (secretResolver != null && secretResolver.isInitialized()) {
            // Iterate through whole config file and find encrypted properties and resolve them
            while (propertyNames.hasMoreElements()) {
                String key = (String) propertyNames.nextElement();
                if (secretResolver.isTokenProtected(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resolving and replacing secret for " + key);
                    }
                    // Resolving the secret password.
                    String value = secretResolver.resolve(key);
                    // Replaces the original encrypted property with resolved property
                    properties.put(key, value);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No encryption done for value with key :" + key);
                    }
                }
            }
        } else {
            log.warn("Secret Resolver is not present. Will not resolve encryptions in " + CONFIG_RELATIVE_PATH + " file");
        }
    }
}
