/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.saml.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class reads the Service Providers info from sso-idp-config.xml and add them to the
 * in-memory service provider map exposed by org.wso2.carbon.identity.sso.saml.
 * SSOServiceProviderConfigManager class.
 */
public class FileBasedConfigManager {

    private static Log log = LogFactory.getLog(FileBasedConfigManager.class);

    private static volatile FileBasedConfigManager instance = null;

    private static List<String> issuerList = new ArrayList<>();

    private FileBasedConfigManager() {

    }

    public static FileBasedConfigManager getInstance() {
        if (instance == null) {
            synchronized (FileBasedConfigManager.class) {
                if (instance == null) {
                    instance = new FileBasedConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     *Get the list of issuers which has been added through the file
     * @return a String list of file based issuers
     */
    public static List<String> getIssuerList() {
        return issuerList;
    }

    /**
     * Read the service providers from file, create SAMLSSOServiceProviderDO beans and add them
     * to the service providers map.
     */
    public void addServiceProviders() {
        SAMLSSOServiceProviderDO[] serviceProviders = readServiceProvidersFromFile();
        if (serviceProviders != null) {
            SSOServiceProviderConfigManager configManager = SSOServiceProviderConfigManager.getInstance();
            for (SAMLSSOServiceProviderDO spDO : serviceProviders) {
                configManager.addServiceProvider(spDO.getIssuer(), spDO);
                log.info("A SSO Service Provider is registered for : " + spDO.getIssuer());
            }
        }
    }

    /**
     * Read the SP info from the sso-idp-config.xml and create an array of SAMLSSOServiceProviderDO
     * beans
     *
     * @return An array of SAMLSSOServiceProviderDO beans
     */
    private SAMLSSOServiceProviderDO[] readServiceProvidersFromFile() {
        Document document = null;
        try {
            String configFilePath = IdentityUtil.getIdentityConfigDirPath() + File.separator + "sso-idp-config.xml";

            if (!isFileExisting(configFilePath)) {
                log.warn("sso-idp-config.xml does not exist in the "+IdentityUtil.getIdentityConfigDirPath() +
                        " directory. The system may depend on the service providers added through the UI.");
                return new SAMLSSOServiceProviderDO[0];
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(configFilePath);
        } catch (Exception e) {
            log.error("Error reading Service Providers from sso-idp-config.xml", e);
            return new SAMLSSOServiceProviderDO[0];
        }

        Element element = document.getDocumentElement();
        NodeList nodeSet = element.getElementsByTagName(SAMLSSOConstants.FileBasedSPConfig.SERVICE_PROVIDER);
        SAMLSSOServiceProviderDO[] serviceProviders = new SAMLSSOServiceProviderDO[nodeSet.getLength()];
        boolean singleLogout = true;
        boolean signAssertion = true;
        boolean validateSignature = false;
        boolean encryptAssertion = false;
        String certAlias = null;

        for (int i = 0; i < nodeSet.getLength(); i++) {
            Element elem = (Element) nodeSet.item(i);
            SAMLSSOServiceProviderDO spDO = new SAMLSSOServiceProviderDO();
            spDO.setIssuer(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.ISSUER));
            issuerList.add(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.ISSUER));
            List<String> assertionConsumerUrls = new ArrayList<>();
            for(String assertionConsumerUrl : getTextValueList(elem, SAMLSSOConstants
                    .FileBasedSPConfig.ASSERTION_CONSUMER_URL)) {
                assertionConsumerUrls.add(IdentityUtil.fillURLPlaceholders(assertionConsumerUrl));
            }
            spDO.setAssertionConsumerUrls(assertionConsumerUrls);

            spDO.setDefaultAssertionConsumerUrl(IdentityUtil
                    .fillURLPlaceholders(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.DEFAULT_ACS_URL)));
            spDO.setLoginPageURL(IdentityUtil
                    .fillURLPlaceholders(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.CUSTOM_LOGIN_PAGE)));

            if ((getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SINGLE_LOGOUT)) != null) {
                singleLogout = Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SINGLE_LOGOUT));
                spDO.setSloResponseURL(IdentityUtil
                        .fillURLPlaceholders(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SLO_RESPONSE_URL)));
                spDO.setSloRequestURL(IdentityUtil
                        .fillURLPlaceholders(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SLO_REQUEST_URL)));
            }

            if ((getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SIGN_ASSERTION)) != null) {
                signAssertion = Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SIGN_ASSERTION));
            }
            if ((getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SIG_VALIDATION)) != null) {
                validateSignature = Boolean.valueOf(getTextValue(elem, SAMLSSOConstants
                        .FileBasedSPConfig.SIG_VALIDATION));
            }
            if ((getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.ENCRYPT_ASSERTION)) != null) {
                encryptAssertion = Boolean.valueOf(getTextValue(elem, SAMLSSOConstants
                        .FileBasedSPConfig.ENCRYPT_ASSERTION));
            }
            if (getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SSO_DEFAULT_SIGNING_ALGORITHM) != null) {
                spDO.setSigningAlgorithmUri(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig
                        .SSO_DEFAULT_SIGNING_ALGORITHM));
            } else {
                spDO.setSigningAlgorithmUri(IdentityApplicationManagementUtil.getSigningAlgoURIByConfig());
            }
            if (getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.SSO_DEFAULT_DIGEST_ALGORITHM) != null) {
                spDO.setDigestAlgorithmUri(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig
                        .SSO_DEFAULT_DIGEST_ALGORITHM));
            } else {
                spDO.setDigestAlgorithmUri(IdentityApplicationManagementUtil.getDigestAlgoURIByConfig());
            }
            if (validateSignature || encryptAssertion) {
                certAlias = getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.CERT_ALIAS);
                if (certAlias == null) {
                    log.warn("Certificate alias for Signature verification or Assertion encryption not specified. " +
                            "Defaulting to \'wso2carbon\'");
                    certAlias = "wso2carbon";
                }
            }
            if (Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.ATTRIBUTE_PROFILE))) {
                spDO.setEnableAttributesByDefault(Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.INCLUDE_ATTRIBUTE)));
                spDO.setAttributeConsumingServiceIndex(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.CONSUMING_SERVICE_INDEX));
            }
            if (Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.AUDIENCE_RESTRICTION)) &&
                    elem.getElementsByTagName(SAMLSSOConstants.FileBasedSPConfig.AUDIENCE_LIST) != null) {
                spDO.setRequestedAudiences(getTextValueList(elem, SAMLSSOConstants.FileBasedSPConfig.AUDIENCE));
            }
            if (Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.RECIPIENT_VALIDATION)) &&
                    elem.getElementsByTagName(SAMLSSOConstants.FileBasedSPConfig.RECIPIENT_LIST) != null) {
                spDO.setRequestedRecipients(getTextValueList(elem, SAMLSSOConstants.FileBasedSPConfig.RECIPIENT));
            }

            if (Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.ENABLE_IDP_INIT_SLO))) {
                spDO.setIdPInitSLOEnabled(true);
                if (elem.getElementsByTagName(SAMLSSOConstants.FileBasedSPConfig.RETURN_TO_URL_LIST) != null) {
                    List<String> sloReturnToUrls = new ArrayList<>();
                    for(String sloReturnUrl : getTextValueList(elem, SAMLSSOConstants
                            .FileBasedSPConfig.RETURN_TO_URL)) {
                        sloReturnToUrls.add(IdentityUtil.fillURLPlaceholders(sloReturnUrl));
                    }
                    spDO.setIdpInitSLOReturnToURLs(sloReturnToUrls);
                }
            }

            spDO.setDoSingleLogout(singleLogout);
            spDO.setDoSignAssertions(signAssertion);
            spDO.setDoValidateSignatureInRequests(validateSignature);
            spDO.setDoEnableEncryptedAssertion(encryptAssertion);
            spDO.setDoSignResponse(Boolean.valueOf(getTextValue(elem, SAMLSSOConstants
                    .FileBasedSPConfig.SIGN_RESPONSE)));
            spDO.setCertAlias(certAlias);
            spDO.setIdPInitSSOEnabled(Boolean.valueOf(getTextValue(elem, SAMLSSOConstants.FileBasedSPConfig.IDP_INIT)));
            serviceProviders[i] = spDO;
        }
        return serviceProviders;
    }

    /**
     * Read the element value for the given element
     *
     * @param element Parent element
     * @param tagName name of the child element
     * @return value of the element
     */
    private String getTextValue(Element element, String tagName) {
        String textVal = null;
        NodeList nl = element.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            if (el != null) {
                String text = el.getTextContent();
                if (text != null && text.length() > 0) {
                    textVal = text;
                }
            }
        }
        return textVal;
    }

    private List<String> getTextValueList(Element element, String tagName) {
        List<String> textValList = new ArrayList<>();
        NodeList nl = element.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                if (el != null) {
                    String text = el.getTextContent();
                    if (text != null && text.length() > 0) {
                        textValList.add(text);
                    }
                }
            }
        }
        return textValList;
    }

    /**
     * Check whether a given file exists in the system
     *
     * @param path file path
     * @return true, if file exists. False otherwise
     */
    private boolean isFileExisting(String path) {
        File file = new File(path);
        if (file.exists()) {
            return true;
        }
        return false;
    }


}
