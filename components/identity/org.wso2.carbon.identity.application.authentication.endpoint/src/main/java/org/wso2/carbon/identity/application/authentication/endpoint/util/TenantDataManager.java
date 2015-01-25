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

import org.apache.axiom.om.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.Collections;

public class TenantDataManager {

    private static final Log log = LogFactory.getLog(TenantDataManager.class);

    // Tenant list dropdown related properties
    private static final String USERNAME = "mutual.ssl.username";
    private static final String USERNAME_HEADER = "username.header";
    private static final String HOST = "identity.server.host";
    private static final String PORT = "identity.server.port";
    private static final String CLIENT_KEY_STORE = "client.keyStore";
    private static final String CLIENT_TRUST_STORE = "client.trustStore";
    private static final String CLIENT_KEY_STORE_PASSWORD = "client.keyStore.password";
    private static final String CLIENT_TRUST_STORE_PASSWORD = "client.trustStore.password";
    private static final String TENANT_CONFIG_PROPERTIES = "TenantConfig.properties";
    private static final String TENANT_LIST_ENABLED = "tenantListEnabled";

    // Service URL constants
    private static final String HTTPS_URL = "https://";
    private static final String TENANT_MGT_ADMIN_SERVICE_URL = "/services/TenantMgtAdminService/retrieveTenants";
    private static final String COLON = ":";

    // String constants for SOAP response processing
    private static final String RETURN = "return";
    private static final String RETRIEVE_TENANTS_RESPONSE = "retrieveTenantsResponse";
    private static final String TENANT_DOMAIN = "tenantDomain";
    private static final String ACTIVE = "active";
    private static final String TENANT_DATA_SEPARATOR = ",";
    private static final String RELATIVE_PATH_START_CHAR = ".";
    private static final String CHARACTER_ENCODING = "UTF-8";

    private static Properties prop;
    private static String carbonLogin = "";
    private static String serviceURL;
    private static String usernameHeaderName = "";
    private static List<String> tenantDomainList = new ArrayList<String>();
    private static boolean initialized = false;

    /**
     * Initialize Tenant data manager
     */
    public static synchronized void init() {

        if (!initialized) {
            InputStream inputStream =
                    TenantDataManager.class.getClassLoader().getResourceAsStream(TENANT_CONFIG_PROPERTIES);
            prop = new Properties();

            if (inputStream != null) {
                try {
                    prop.load(inputStream);
                    usernameHeaderName = getPropertyValue(USERNAME_HEADER);
                    carbonLogin = getPropertyValue(USERNAME);

                    // Base64 encoded username
                    carbonLogin = new String(Base64.encode(carbonLogin.getBytes(CHARACTER_ENCODING)));

                    String clientKeyStorePath = buildFilePath(getPropertyValue(CLIENT_KEY_STORE));
                    String clientTrustStorePath = buildFilePath(getPropertyValue(CLIENT_TRUST_STORE));

                    TenantMgtAdminServiceClient
                            .loadKeyStore(clientKeyStorePath, getPropertyValue(CLIENT_KEY_STORE_PASSWORD));
                    TenantMgtAdminServiceClient
                            .loadTrustStore(clientTrustStorePath, getPropertyValue(CLIENT_TRUST_STORE_PASSWORD));
                    TenantMgtAdminServiceClient.initMutualSSLConnection();

                    // Build the service URL of tenant management admin service
                    StringBuilder builder = new StringBuilder();
                    serviceURL = builder.append(HTTPS_URL).append(getPropertyValue(HOST)).append(COLON)
                            .append(getPropertyValue(PORT)).append(TENANT_MGT_ADMIN_SERVICE_URL).toString();

                    initialized = true;

                } catch (Exception e) {
                    // Catching the general exception as for any exception server should not proceed further.
                    log.error("Error when initializing TenantDataManager for listing tenant domains dropdown", e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            log.error("Error when closing stream for " + TENANT_CONFIG_PROPERTIES, e);
                        }
                    }
                }
            } else {
                log.error("Configuration file " + TENANT_CONFIG_PROPERTIES + " not found");
            }
        }
    }

    /**
     * Build the absolute path of a give file path
     * @param path File path
     * @return Absolute file path
     * @throws IOException
     */
    private static String buildFilePath(String path) throws IOException {

        if (StringUtils.isNotEmpty(path) && path.startsWith(RELATIVE_PATH_START_CHAR)) {
            // Relative file path is given
            File currentDirectory = new File(new File(RELATIVE_PATH_START_CHAR).getAbsolutePath());
            path = currentDirectory.getCanonicalPath() + File.separator + path;
        }

        if (log.isDebugEnabled()) {
            log.debug("File path for KeyStore/TrustStore : " + path);
        }
        return path;
    }

    /**
     * Get property value by key
     * @param key Property key
     * @return Property value
     */
    private static String getPropertyValue(String key) {
        return prop.getProperty(key);
    }

    /**
     * Call service and return response
     * @param url Service URL
     * @return Response from service
     */
    private static String getServiceResponse(String url) {
        String serviceResponse = null;
        Map<String, String> headerParams = new HashMap<String, String>();
        // Set the username in HTTP header for mutual ssl authentication
        headerParams.put(usernameHeaderName, carbonLogin);
        serviceResponse = TenantMgtAdminServiceClient.sendPostRequest(url, null, headerParams);
        return serviceResponse;
    }

    /**
     * Get active tenants list
     * @return List of tenant domains
     */
    public static List<String> getAllActiveTenantDomains() {

        if (initialized && tenantDomainList.isEmpty()) {
            refreshActiveTenantDomainsList();
        }
        return tenantDomainList;
    }

    /**
     * Set the updated tenant domains list
     * @param dataList List of active tenant domains
     */
    public static void setTenantDataList(String dataList) {

        if (!initialized) {
            if (log.isDebugEnabled()) {
                log.debug("Tenant domains list not set as TenantDataManager is not initialized.");
            }
            return;
        }

        if (StringUtils.isNotEmpty(dataList)) {
            synchronized (tenantDomainList) {
                String[] domains = dataList.split(TENANT_DATA_SEPARATOR);
                // Remove all existing tenant domains from the list
                tenantDomainList.clear();

                for (String domain : domains) {
                    tenantDomainList.add(domain);
                }
                // Sort the tenant domains list according to alphabetical order
                Collections.sort(tenantDomainList);
            }
        } else {
            // Reset active tenant domains list
            tenantDomainList.clear();
        }
    }

    /**
     * Retrieve latest active tenant domains list
     */
    private static void refreshActiveTenantDomainsList() {

        try {
            String xmlString = getServiceResponse(serviceURL);

            if (StringUtils.isNotEmpty(xmlString)) {

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();

                InputSource inputSource = new InputSource(new StringReader(xmlString));
                String xPathExpression = "/*[local-name() = '" + RETRIEVE_TENANTS_RESPONSE + "']/*[local-name() = '" +
                        RETURN + "']";
                NodeList nodeList = null;
                nodeList = (NodeList) xpath.evaluate(xPathExpression, inputSource, XPathConstants.NODESET);

                // Reset existing tenant domains list
                tenantDomainList.clear();

                // For each loop is not supported for NodeList
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        NodeList tenantData = element.getChildNodes();
                        boolean activeChecked = false;
                        boolean domainChecked = false;
                        boolean isActive = false;
                        String tenantDomain = null;

                        // For each loop is not supported for NodeList
                        for (int j = 0; j < tenantData.getLength(); j++) {
                            Node dataItem = tenantData.item(j);
                            String localName = dataItem.getLocalName();

                            if (ACTIVE.equals(localName)) {
                                // Current element has domain status active or inactive
                                activeChecked = true;
                                if (Boolean.parseBoolean(dataItem.getTextContent())) {
                                    isActive = true;
                                }
                            }

                            if (TENANT_DOMAIN.equals(localName)) {
                                // Current element has domain name of the tenant
                                domainChecked = true;
                                tenantDomain = dataItem.getTextContent();
                            }

                            if (activeChecked && domainChecked) {
                                if (isActive) {
                                    tenantDomainList.add(tenantDomain);

                                    if (log.isDebugEnabled()) {
                                        log.debug(tenantDomain + " is active and added to the dropdown list");
                                    }
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug(tenantDomain + " is inactive and not added to the dropdown list");
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                // Sort the list of tenant domains alphabetically
                Collections.sort(tenantDomainList);
            }
        } catch (Exception e) {
            // Catching the general exception as if no tenants are available it should stop processing
            log.error("Retrieving list of active tenant domains failed. Ignore this if there are no tenants : ", e);
        }
    }

    /**
     * Get status of the tenant list dropdown enabled or disabled
     * @return Tenant list enabled or disabled status
     */
    public static boolean isTenantListEnabled() {

        return (initialized && Boolean.parseBoolean(getPropertyValue(TENANT_LIST_ENABLED)));
    }
}
