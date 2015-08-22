/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.core.util;

import org.apache.axiom.om.impl.dom.factory.OMDOMFactory;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.util.SecurityManager;
import org.apache.xml.security.utils.Base64;
import org.opensaml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.base.CarbonEntityResolver;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.base.IdentityRuntimeException;
import org.wso2.carbon.identity.core.internal.IdentityCoreServiceComponent;
import org.wso2.carbon.identity.core.model.IdentityErrorMsgContext;
import org.wso2.carbon.identity.core.model.IdentityEventListener;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfigKey;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.xml.sax.SAXException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IdentityUtil {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private final static char[] ppidDisplayCharMap = new char[]{'Q', 'L', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'R', 'S', 'T', 'U',
            'V', 'W', 'X', 'Y', 'Z'};
    private static Log log = LogFactory.getLog(IdentityUtil.class);
    private static Map<String, Object> configuration = new HashMap<String, Object>();
    private static Map<IdentityEventListenerConfigKey, IdentityEventListener> eventListenerConfiguration = new HashMap<>();
    private static Document importerDoc = null;
    private static ThreadLocal<IdentityErrorMsgContext> IdentityError = new ThreadLocal<IdentityErrorMsgContext>();
    private static final String SECURITY_MANAGER_PROPERTY = Constants.XERCES_PROPERTY_PREFIX +
            Constants.SECURITY_MANAGER_PROPERTY;
    private static final int ENTITY_EXPANSION_LIMIT = 0;

    /**
     * @return
     */
    public static IdentityErrorMsgContext getIdentityErrorMsg() {
        if (IdentityError.get() == null) {
            return null;
        }
        return IdentityError.get();
    }

    /**
     * @param error
     */
    public static void setIdentityErrorMsg(IdentityErrorMsgContext error) {
        IdentityError.set(error);
    }

    /**
     *
     */
    public static void clearIdentityErrorMsg() {
        IdentityError.remove();
    }

    /**
     * Read configuration elements from the identity.xml
     *
     * @param key Element Name as specified from the parent elements in the XML structure.
     *            To read the element value of b in {@code<a><b>text</b></a>}, the property
     *            name should be passed as "a.b"
     * @return Element text value, "text" for the above element.
     */
    public static String getProperty(String key) {
        Object value = configuration.get(key);
        if (value instanceof ArrayList) {
            return (String) ((ArrayList) value).get(0);
        }
        return (String) value;
    }

    public static IdentityEventListener readEventListenerProperty(String type, String name) {
        IdentityEventListenerConfigKey identityEventListenerConfigKey = new IdentityEventListenerConfigKey(type, name);
        IdentityEventListener identityEventListener = eventListenerConfiguration.get(identityEventListenerConfigKey);
        return identityEventListener;
    }

    public static void populateProperties() throws ServerConfigurationException {
        configuration = IdentityConfigParser.getInstance().getConfiguration();
        eventListenerConfiguration = IdentityConfigParser.getInstance().getEventListenerConfiguration();
    }

    public static String getPPIDDisplayValue(String value) throws Exception {
        log.info("Generating display value of PPID : " + value);
        byte[] rawPpid = Base64.decode(value);
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.update(rawPpid);
        byte[] hashId = sha1.digest();
        char[] returnChars = new char[10];
        for (int i = 0; i < 10; i++) {
            int rawValue = (hashId[i] + 128) % 32;
            returnChars[i] = ppidDisplayCharMap[rawValue];
        }
        StringBuilder sb = new StringBuilder();
        sb.append(returnChars, 0, 3);
        sb.append("-");
        sb.append(returnChars, 3, 4);
        sb.append("-");
        sb.append(returnChars, 6, 3);
        return sb.toString();

    }

    /**
     * Serialize the given node to a String.
     *
     * @param node Node to be serialized.
     * @return The serialized node as a java.lang.String instance.
     */
    public static String nodeToString(Node node) {

        if (importerDoc == null) {
            OMDOMFactory fac = new OMDOMFactory();
            importerDoc = (Document) fac.createOMDocument();
        }
        // Import the node as an AXIOM-DOOM node and use toSting()
        Node axiomNode = importerDoc.importNode(node, true);
        return axiomNode.toString();
    }

    public static String getHMAC(String secretKey, String baseString) throws SignatureException {
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA1_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(key);
            byte[] rawHmac = mac.doFinal(baseString.getBytes());
            return Base64.encode(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
    }

    /**
     * Generates a secure random hexadecimal string using SHA1 PRNG and digest
     *
     * @return Random hexadecimal encoded String
     * @throws Exception
     */
    public static String generateUUID() throws Exception {

        try {
            // SHA1 Pseudo Random Number Generator
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");

            // random number
            String randomNum = Integer.toString(prng.nextInt());
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha.digest(randomNum.getBytes());

            // Hexadecimal encoding
            return new String(Hex.encodeHex(digest));

        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Failed to generate UUID ", e);
        }
    }

    /**
     * Get the tenant id of the given user.
     *
     * @param username Username
     * @return Tenant Id of domain user belongs to.
     * @throws IdentityException Error when getting an instance of Tenant Manger
     */
    public static int getTenantIdOFUser(String username) throws IdentityException {
        int tenantId = 0;
        String domainName = MultitenantUtils.getTenantDomain(username);
        if (domainName != null) {
            try {
                TenantManager tenantManager = IdentityTenantUtil.getRealmService().getTenantManager();
                tenantId = tenantManager.getTenantId(domainName);
            } catch (UserStoreException e) {
                String errorMsg = "Error when getting the tenant id from the tenant domain : " +
                        domainName;
                log.error(errorMsg, e);
                throw new IdentityException(errorMsg, e);
            }
        }
        return tenantId;
    }

    /**
     * Generates a random number using two UUIDs and HMAC-SHA1
     *
     * @return Random Number generated.
     * @throws IdentityException Exception due to Invalid Algorithm or Invalid Key
     */
    public static String getRandomNumber() throws IdentityException {
        try {
            String secretKey = UUIDGenerator.generateUUID();
            String baseString = UUIDGenerator.generateUUID();

            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            byte[] rawHmac = mac.doFinal(baseString.getBytes());
            String random = Base64.encode(rawHmac);
            // Registry doesn't have support for these character.
            random = random.replace("/", "_");
            random = random.replace("=", "a");
            random = random.replace("+", "f");
            return random;
        } catch (Exception e) {
            log.error("Error when generating a random number.", e);
            throw new IdentityException("Error when generating a random number.", e);
        }
    }

    public static int getRandomInteger() throws IdentityException {

        try {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            int number = prng.nextInt();
            while (number < 0) {
                number = prng.nextInt();
            }
            return number;
        } catch (NoSuchAlgorithmException e) {
            log.error("Error when generating a random number.", e);
            throw new IdentityException("Error when generating a random number.", e);
        }

    }

    public static String getIdentityConfigDirPath() {
        return CarbonUtils.getCarbonConfigDirPath() + File.separator + "identity";
    }

    public static String getServerURL(String endpoint) throws IdentityRuntimeException {
        String hostName = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.HOST_NAME);

        try {
            if (hostName == null) {
                hostName = NetworkUtils.getLocalHostname();
            }
        } catch (SocketException e) {
            throw new IdentityRuntimeException("Error while trying to read hostname.", e);
        }

        String mgtTransport = CarbonUtils.getManagementTransport();
        AxisConfiguration axisConfiguration = IdentityCoreServiceComponent.getConfigurationContextService().
                getServerConfigContext().getAxisConfiguration();
        int mgtTransportPort = CarbonUtils.getTransportProxyPort(axisConfiguration, mgtTransport);
        if (mgtTransportPort <= 0) {
            mgtTransportPort = CarbonUtils.getTransportPort(axisConfiguration, mgtTransport);
        }
        String serverUrl = mgtTransport + "://" + hostName.toLowerCase();
        // If it's well known HTTPS port, skip adding port
        if (mgtTransportPort != IdentityCoreConstants.DEFAULT_HTTPS_PORT) {
            serverUrl += ":" + mgtTransportPort;
        }
        // If ProxyContextPath is defined then append it
        String proxyContextPath = ServerConfiguration.getInstance().getFirstProperty(IdentityCoreConstants.PROXY_CONTEXT_PATH);
        if (proxyContextPath != null && !proxyContextPath.trim().isEmpty()) {
            if (proxyContextPath.charAt(0) == '/') {
                serverUrl += proxyContextPath;
            } else {
                serverUrl += "/" + proxyContextPath;
            }
        }

        if(StringUtils.isNotBlank(endpoint)) {
            if(!endpoint.startsWith("/")) {
                serverUrl += "/";
            }
            serverUrl += endpoint;
        }

        return serverUrl;
    }

    /**
     * Constructing the SAML or XACML Objects from a String
     *
     * @param xmlString Decoded SAML or XACML String
     * @return SAML or XACML Object
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public static XMLObject unmarshall(String xmlString) throws IdentityException {

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);

            documentBuilderFactory.setExpandEntityReferences(false);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            org.apache.xerces.util.SecurityManager securityManager = new SecurityManager();
            securityManager.setEntityExpansionLimit(ENTITY_EXPANSION_LIMIT);
            documentBuilderFactory.setAttribute(SECURITY_MANAGER_PROPERTY, securityManager);

            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new CarbonEntityResolver());
            Document document = docBuilder.parse(new ByteArrayInputStream(xmlString.trim().getBytes(Charsets.UTF_8)));
            Element element = document.getDocumentElement();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            return unmarshaller.unmarshall(element);
        } catch (ParserConfigurationException|UnmarshallingException|SAXException|IOException e) {
            String message = "Error in constructing XML Object from the encoded String";
            throw new IdentityException(message, e);
        }
    }
}
