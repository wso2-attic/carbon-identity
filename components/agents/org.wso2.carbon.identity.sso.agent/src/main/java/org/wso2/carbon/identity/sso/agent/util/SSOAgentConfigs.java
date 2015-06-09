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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.wso2.carbon.identity.sso.agent.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentConfig;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;

import javax.servlet.FilterConfig;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Properties;
import java.util.logging.Logger;

public class SSOAgentConfigs {

    private static final Logger LOGGER = Logger.getLogger("InfoLogging");
    private static final Log log = LogFactory.getLog(SSOAgentConfig.class);


    private static Boolean samlSSOLoginEnabled;
    private static Boolean openidLoginEnabled;
    private static Boolean saml2GrantEnabled;
    private static String sessionBeanName;
    private static String loginUrl;
    private static String samlSSOUrl;
    private static String openIdUrl;
    private static String saml2GrantUrl;

    private static String issuerId;
    private static String consumerUrl;
    private static String idPUrl;

    private static String attributeConsumingServiceIndex;
    private static Boolean isSLOEnabled;
    private static String logoutUrl;
    private static Boolean isResponseSigned;
    private static Boolean isAssertionSigned;
    private static Boolean isAssertionEncrypted;
    private static Boolean isRequestSigned;
    private static Boolean isForceAuthn;
    private static String ssoAgentCredentialImplClass;
    private static InputStream keyStoreStream;
    private static String keyStorePassword;
    private static KeyStore keyStore;
    private static String idPCertAlias;
    private static String privateKeyAlias;
    private static String privateKeyPassword;
    private static String tokenEndpoint;
    private static String clientId;
    private static String clientSecret;

    private static String openIdProviderUrl;
    private static String returnTo;
    private static String claimedIdParameterName;
    private static String attributesRequestorImplClass;

    private static String requestQueryParameters;

    private static String addExtension;

    private SSOAgentConfigs() {
    }

    public static void initConfig(FilterConfig fConfigs) throws SSOAgentException {

        Properties properties = new Properties();
        try {
            if (fConfigs.getInitParameter("SSOAgentPropertiesFilePath") != null &&
                    !"".equals(fConfigs.getInitParameter("SSOAgentPropertiesFilePath"))) {
                properties.load(new FileInputStream(fConfigs.getInitParameter("SSOAgentPropertiesFilePath")));
                initConfig(properties);
            } else {
                LOGGER.warning("\'SSOAgentPropertiesFilePath\' not configured");
            }
        } catch (FileNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("File not found  ", e);
            }
            throw new SSOAgentException("Agent properties file not found");

        } catch (IOException e) {

            throw new SSOAgentException("Error occurred while reading Agent properties file", e);
        }

    }

    public static void initConfig(String propertiesFilePath) throws SSOAgentException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertiesFilePath));
            initConfig(properties);
        } catch (FileNotFoundException e) {

            throw new SSOAgentException("Agent properties file not found at " + propertiesFilePath, e);
        } catch (IOException e) {

            throw new SSOAgentException("Error reading Agent properties file at " + propertiesFilePath, e);
        }
    }

    public static void initConfig(Properties properties) throws SSOAgentException {

        if (properties.getProperty("EnableSAMLSSOLogin") != null) {
            samlSSOLoginEnabled = Boolean.parseBoolean(properties.getProperty("EnableSAMLSSOLogin"));
        } else {
            LOGGER.info("\'EnableSAMLSSOLogin\' not configured. Defaulting to \'true\'");
            samlSSOLoginEnabled = true;
        }

        if (properties.getProperty("EnableOpenIDLogin") != null) {
            openidLoginEnabled = Boolean.parseBoolean(properties.getProperty("EnableOpenIDLogin"));
        } else {
            LOGGER.info("\'EnableOpenIDLogin\' not configured. Defaulting to \'true\'");
            openidLoginEnabled = true;
        }

        if (properties.getProperty("EnableSAML2Grant") != null) {
            saml2GrantEnabled = Boolean.parseBoolean(properties.getProperty("EnableSAML2Grant"));
        } else {
            LOGGER.info("\'EnableSAML2Grant\' not configured. Defaulting to \'true\'");
            saml2GrantEnabled = true;
        }

        if (properties.getProperty("SSOAgentSessionBeanName") != null) {
            sessionBeanName = properties.getProperty("SSOAgentSessionBeanName");
        } else {
            LOGGER.info("\'SSOAgentSessionBeanName\' not configured. Defaulting to \'SSOAgentSessionBean\'");
            sessionBeanName = "SSOAgentSessionBean";
        }

        loginUrl = properties.getProperty("LoginUrl");
        samlSSOUrl = properties.getProperty("SAMLSSOUrl");
        saml2GrantUrl = properties.getProperty("SAML2GrantUrl");
        openIdUrl = properties.getProperty("OpenIDUrl");

        issuerId = properties.getProperty("SAML.IssuerID");
        consumerUrl = properties.getProperty("SAML.ConsumerUrl");
        idPUrl = properties.getProperty("SAML.IdPUrl");
        attributeConsumingServiceIndex = properties.getProperty("SAML.AttributeConsumingServiceIndex");

        if (properties.getProperty("SAML.EnableSLO") != null) {
            isSLOEnabled = Boolean.parseBoolean(properties.getProperty("SAML.EnableSLO"));
        } else {
            LOGGER.info("\'SAML.EnableSLO\' not configured. Defaulting to \'false\'");
            isSLOEnabled = false;
        }

        logoutUrl = properties.getProperty("SAML.LogoutUrl");

        if (properties.getProperty("SAML.EnableResponseSigning") != null) {
            isResponseSigned = Boolean.parseBoolean(properties.getProperty("SAML.EnableResponseSigning"));
        } else {
            LOGGER.info("\'SAML.EnableResponseSigning\' not configured. Defaulting to \'false\'");
            isResponseSigned = false;
        }

        if (properties.getProperty("SAML.EnableAssertionSigning") != null) {
            isAssertionSigned = Boolean.parseBoolean(properties.getProperty("SAML.EnableAssertionSigning"));
        } else {
            LOGGER.info("\'SAML.EnableAssertionSigning\' not configured. Defaulting to \'true\'");
            isAssertionSigned = true;
        }

        if (properties.getProperty("SAML.EnableAssertionEncryption") != null) {
            isAssertionEncrypted = Boolean.parseBoolean(properties.getProperty("SAML.EnableAssertionEncryption"));
        } else {
            LOGGER.info("\'SAML.EnableAssertionEncryption\' not configured. Defaulting to \'false\'");
            isAssertionEncrypted = false;
        }

        if (properties.getProperty("SAML.EnableRequestSigning") != null) {
            isRequestSigned = Boolean.parseBoolean(properties.getProperty("SAML.EnableRequestSigning"));
        } else {
            LOGGER.info("\'SAML.EnableRequestSigning\' not configured. Defaulting to \'false\'");
            isRequestSigned = false;
        }

        if (properties.getProperty("SAML.EnableForceAuthentication") != null) {
            isForceAuthn = Boolean.parseBoolean(properties.getProperty("SAML.EnableForceAuthentication"));
        } else {
            LOGGER.info("\'SAML.EnableForceAuthentication\' not configured. Defaulting to \'false\'");
            isForceAuthn = false;
        }

        ssoAgentCredentialImplClass = properties.getProperty("SAML.SSOAgentCredentialImplClass");
        if (properties.getProperty("KeyStore") != null) {
            try {
                keyStoreStream = new FileInputStream(properties.getProperty("KeyStore"));
            } catch (FileNotFoundException e) {

                throw new SSOAgentException("Cannot find file " + properties.getProperty("KeyStore"), e);
            }
        }
        keyStorePassword = properties.getProperty("KeyStorePassword");
        idPCertAlias = properties.getProperty("SAML.IdPCertAlias");
        privateKeyAlias = properties.getProperty("SAML.PrivateKeyAlias");
        privateKeyPassword = properties.getProperty("SAML.PrivateKeyPassword");

        tokenEndpoint = properties.getProperty("SAML.OAuth2TokenEndpoint");
        clientId = properties.getProperty("SAML.OAuth2ClientID");
        clientSecret = properties.getProperty("SAML.OAuth2ClientSecret");

        openIdProviderUrl = properties.getProperty("OpenID.OpenIdProviderUrl");
        returnTo = properties.getProperty("OpenID.ReturnToUrl");
        claimedIdParameterName = properties.getProperty("OpenID.ClaimedIDParameterName");
        attributesRequestorImplClass = properties.getProperty("OpenID.AttributesRequestorImplClass");

        requestQueryParameters = properties.getProperty("SAML.Request.Query.Param");

        addExtension = properties.getProperty("SAML.Request.Add.Extension");

    }

    public static void initCheck() throws SSOAgentException {

        if ((SSOAgentConfigs.isSAMLSSOLoginEnabled() || SSOAgentConfigs.isOpenIDLoginEnabled()) &&
                SSOAgentConfigs.getLoginUrl() == null) {

            throw new SSOAgentException("\'LoginUrl\' not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSAML2GrantEnabled() &&
                SSOAgentConfigs.getSAML2GrantUrl() == null) {
            throw new SSOAgentException("\'SAML2GrantUrl\' not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.getSAMLSSOUrl() == null) {
            throw new SSOAgentException("\'SAMLSSOUrl\' not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.getIssuerId() == null) {
            throw new SSOAgentException("\'SAML.IssuerId\' not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.getConsumerUrl() == null) {
            throw new SSOAgentException("\'SAML.ConsumerUrl\' not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.getIdPUrl() == null) {
            throw new SSOAgentException("\'SAML.IdPUrl\' not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.getAttributeConsumingServiceIndex() == null) {
            LOGGER.info("\'SAML.AttributeConsumingServiceIndex\' not configured. " +
                    "No attributes of the Subject will be requested");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSLOEnabled() &&
                SSOAgentConfigs.getLogoutUrl() == null) {
            throw new SSOAgentException("Single Logout enabled, but SAML.LogoutUrl not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() &&
                (SSOAgentConfigs.isResponseSigned() || SSOAgentConfigs.isAssertionSigned() || SSOAgentConfigs.isAssertionEncripted() || SSOAgentConfigs.isRequestSigned()) &&
                SSOAgentConfigs.getSSOAgentCredentialImplClass() == null) {
            LOGGER.info("SAML.SSOAgentCredentialImplClass not configured." +
                    " Defaulting to \'org.wso2.carbon.identity.sso.agent.saml.SSOAgentKeyStoreCredential\'");
            SSOAgentConfigs.setSSOAgentCredentialImplClass("org.wso2.carbon.identity.sso.agent.saml.SSOAgentKeyStoreCredential");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() &&
                (SSOAgentConfigs.isResponseSigned() || SSOAgentConfigs.isAssertionSigned() || SSOAgentConfigs.isAssertionEncripted() || SSOAgentConfigs.isRequestSigned()) &&
                SSOAgentConfigs.getSSOAgentCredentialImplClass() != null && SSOAgentConfigs.getKeyStoreStream() == null) {
            throw new SSOAgentException("KeyStore not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() &&
                (SSOAgentConfigs.isResponseSigned() || SSOAgentConfigs.isAssertionSigned() || SSOAgentConfigs.isAssertionEncripted() || SSOAgentConfigs.isRequestSigned()) &&
                SSOAgentConfigs.getSSOAgentCredentialImplClass() != null && SSOAgentConfigs.getKeyStoreStream() != null &&
                SSOAgentConfigs.getKeyStorePassword() == null) {
            LOGGER.info("KeyStorePassword not configured." +
                    " Defaulting to \'wso2carbon\'");
            SSOAgentConfigs.setKeyStorePassword("wso2carbon");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() &&
                (SSOAgentConfigs.isResponseSigned() || SSOAgentConfigs.isAssertionSigned()) &&
                SSOAgentConfigs.getSSOAgentCredentialImplClass() != null && SSOAgentConfigs.getIdPCertAlias() == null) {
            LOGGER.info("\'SAML.IdPCertAlias\' not configured. Defaulting to \'wso2carbon\'");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && (SSOAgentConfigs.isRequestSigned() || SSOAgentConfigs.isAssertionEncripted()) &&
                SSOAgentConfigs.getSSOAgentCredentialImplClass() != null && SSOAgentConfigs.getPrivateKeyAlias() == null) {
            LOGGER.info("SAML.PrivateKeyAlias not configured. Defaulting to \'wso2carbon\'");
            SSOAgentConfigs.setPrivateKeyAlias("wso2carbon");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && (SSOAgentConfigs.isRequestSigned() || SSOAgentConfigs.isAssertionEncripted()) &&
                SSOAgentConfigs.getSSOAgentCredentialImplClass() != null && SSOAgentConfigs.getPrivateKeyPassword() == null) {
            LOGGER.info("SAML.PrivateKeyPassword not configured. Defaulting to \'wso2carbon\'");
            SSOAgentConfigs.setPrivateKeyPassword("wso2carbon");
        }

        if (!SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSAML2GrantEnabled()) {
            LOGGER.info("SAMLSSOLogin disabled. Therefore disabling SAML2Grant as well");
            SSOAgentConfigs.setSAML2GrantEnabled(false);
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSAML2GrantEnabled() &&
                SSOAgentConfigs.getTokenEndpoint() == null) {
            LOGGER.info("SAML.OAuth2TokenEndpoint not configured. Defaulting to \'https://localhost:9443/oauth2/token\'");
            SSOAgentConfigs.setTokenEndpoint("https://localhost:9443/oauth2/token");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSAML2GrantEnabled() &&
                SSOAgentConfigs.getTokenEndpoint() != null && SSOAgentConfigs.getOAuth2ClientId() == null) {
            LOGGER.info("SAML.OAuth2ClientID not configured");
            throw new SSOAgentException("SAML.OAuth2ClientId not configured");
        }

        if (SSOAgentConfigs.isSAMLSSOLoginEnabled() && SSOAgentConfigs.isSAML2GrantEnabled() &&
                SSOAgentConfigs.getTokenEndpoint() != null && SSOAgentConfigs.getOAuth2ClientSecret() == null) {
            throw new SSOAgentException("SAML.OAuth2ClientSecret not configured");
        }

        if (SSOAgentConfigs.isOpenIDLoginEnabled() && SSOAgentConfigs.getOpenIdUrl() == null) {
            throw new SSOAgentException("\'OpenIDUrl\' not configured");
        }

        if (SSOAgentConfigs.isOpenIDLoginEnabled() && SSOAgentConfigs.getOpenIdProviderUrl() == null) {
            throw new SSOAgentException("\'OpenID.OpenIdProviderUrl\' not configured");
        }

        if (SSOAgentConfigs.isOpenIDLoginEnabled() && SSOAgentConfigs.getReturnTo() == null) {
            throw new SSOAgentException("OpenID.ReturnToUrl not configured");
        }

        if (SSOAgentConfigs.isOpenIDLoginEnabled() && SSOAgentConfigs.getClaimedIdParameterName() == null) {
            LOGGER.info("OpenID.ClaimIDParameterName not configured. Defaulting to \'claimed_id\'");
            SSOAgentConfigs.setClaimedIdParameterName("claimed_id");
        }

        if (SSOAgentConfigs.isOpenIDLoginEnabled() && SSOAgentConfigs.getAttributesRequestorImplClass() == null) {
            LOGGER.info("OpenID.AttributesRequestorImplClass not configured. No attributes of the subject will be fetched");
        }

    }

    public static boolean isSAMLSSOLoginEnabled() {
        return samlSSOLoginEnabled;
    }

    public static void setSAMLSSOLoginEnabled(Boolean samlSSOLoginEnabled) {
        SSOAgentConfigs.samlSSOLoginEnabled = samlSSOLoginEnabled;
    }

    public static boolean isOpenIDLoginEnabled() {
        return openidLoginEnabled;
    }

    public static boolean isSAML2GrantEnabled() {
        return saml2GrantEnabled;
    }

    public static void setSAML2GrantEnabled(Boolean saml2GrantEnabled) {
        SSOAgentConfigs.saml2GrantEnabled = saml2GrantEnabled;
    }

    public static String getSessionBeanName() {
        return sessionBeanName;
    }

    public static void setSessionBeanName(String sessionBeanName) {
        SSOAgentConfigs.sessionBeanName = sessionBeanName;
    }

    public static String getLoginUrl() {
        return loginUrl;
    }

    public static void setLoginUrl(String loginUrl) {
        SSOAgentConfigs.loginUrl = loginUrl;
    }

    public static String getSAMLSSOUrl() {
        return samlSSOUrl;
    }

    public static void setSAMLSSOUrl(String samlSSOUrl) {
        SSOAgentConfigs.samlSSOUrl = samlSSOUrl;
    }

    public static String getOpenIdUrl() {
        return openIdUrl;
    }

    public static void setOpenIdUrl(String openIdUrl) {
        SSOAgentConfigs.openIdUrl = openIdUrl;
    }

    public static String getSAML2GrantUrl() {
        return saml2GrantUrl;
    }

    public static void setSAML2GrantUrl(String saml2GrantUrl) {
        SSOAgentConfigs.saml2GrantUrl = saml2GrantUrl;
    }

    public static String getIssuerId() {
        return issuerId;
    }

    public static void setIssuerId(String issuerId) {
        SSOAgentConfigs.issuerId = issuerId;
    }

    public static String getConsumerUrl() {
        return consumerUrl;
    }

    public static void setConsumerUrl(String consumerUrl) {
        SSOAgentConfigs.consumerUrl = consumerUrl;
    }

    public static String getIdPUrl() {
        return idPUrl;
    }

    public static void setIdPUrl(String idPUrl) {
        SSOAgentConfigs.idPUrl = idPUrl;
    }

    public static String getAttributeConsumingServiceIndex() {
        return attributeConsumingServiceIndex;
    }

    public static void setAttributeConsumingServiceIndex(String attributeConsumingServiceIndex) {
        SSOAgentConfigs.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
    }

    public static boolean isSLOEnabled() {
        return isSLOEnabled;
    }

    public static void setSLOEnabled(Boolean SLOEnabled) {
        isSLOEnabled = SLOEnabled;
    }

    public static String getLogoutUrl() {
        return logoutUrl;
    }

    public static void setLogoutUrl(String logoutUrl) {
        SSOAgentConfigs.logoutUrl = logoutUrl;
    }

    public static boolean isResponseSigned() {
        return isResponseSigned;
    }

    public static void setResponseSigned(Boolean responseSigned) {
        isResponseSigned = responseSigned;
    }

    public static boolean isAssertionSigned() {
        return isAssertionSigned;
    }

    public static void setAssertionSigned(Boolean assertionSigned) {
        isAssertionSigned = assertionSigned;
    }

    public static boolean isAssertionEncripted() {
        return isAssertionEncrypted;
    }

    public static boolean isRequestSigned() {
        return isRequestSigned;
    }

    public static void setRequestSigned(Boolean requestSigned) {
        isRequestSigned = requestSigned;
    }

    public static boolean isForceAuthn() {
        return isForceAuthn;
    }

    public static void setForceAuthn(Boolean forceAuthn) {
        isForceAuthn = forceAuthn;
    }

    public static String getSSOAgentCredentialImplClass() {
        return ssoAgentCredentialImplClass;
    }

    public static void setSSOAgentCredentialImplClass(String ssoAgentCredentialImplClass) {
        SSOAgentConfigs.ssoAgentCredentialImplClass = ssoAgentCredentialImplClass;
    }

    private static InputStream getKeyStoreStream() {
        return keyStoreStream;
    }

    public static void setKeyStoreStream(InputStream keyStoreStream) {
        if (SSOAgentConfigs.keyStoreStream == null) {
            SSOAgentConfigs.keyStoreStream = keyStoreStream;
        }
    }

    private static String getKeyStorePassword() {
        return keyStorePassword;
    }

    public static void setKeyStorePassword(String keyStorePassword) {
        SSOAgentConfigs.keyStorePassword = keyStorePassword;
    }

    public static KeyStore getKeyStore() throws SSOAgentException {
        if (keyStore == null) {
            setKeyStore(readKeyStore(getKeyStoreStream(), getKeyStorePassword()));
        }
        return keyStore;
    }

    public static void setKeyStore(KeyStore keyStore) {
        SSOAgentConfigs.keyStore = keyStore;
    }

    public static String getIdPCertAlias() {
        return idPCertAlias;
    }

    public static void setIdPCertAlias(String idPCertAlias) {
        SSOAgentConfigs.idPCertAlias = idPCertAlias;
    }

    public static String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public static void setPrivateKeyAlias(String privateKeyAlias) {
        SSOAgentConfigs.privateKeyAlias = privateKeyAlias;
    }

    public static String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public static void setPrivateKeyPassword(String privateKeyPassword) {
        SSOAgentConfigs.privateKeyPassword = privateKeyPassword;
    }

    public static String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public static void setTokenEndpoint(String tokenEndpoint) {
        SSOAgentConfigs.tokenEndpoint = tokenEndpoint;
    }

    public static String getOAuth2ClientId() {
        return clientId;
    }

    public static void setOAuth2ClientId(String clientSecret) {
        SSOAgentConfigs.clientSecret = clientSecret;
    }

    public static String getOAuth2ClientSecret() {
        return clientSecret;
    }

    public static void setOAuth2ClientSecret(String clientId) {
        SSOAgentConfigs.clientId = clientId;
    }

    public static String getOpenIdProviderUrl() {
        return openIdProviderUrl;
    }

    public static void setOpenIdProviderUrl(String openIdProviderUrl) {
        SSOAgentConfigs.openIdProviderUrl = openIdProviderUrl;
    }

    public static String getReturnTo() {
        return returnTo;
    }

    public static void setReturnTo(String returnTo) {
        SSOAgentConfigs.returnTo = returnTo;
    }

    public static String getClaimedIdParameterName() {
        return claimedIdParameterName;
    }

    public static void setClaimedIdParameterName(String claimedIdParameterName) {
        SSOAgentConfigs.claimedIdParameterName = claimedIdParameterName;
    }

    public static String getAttributesRequestorImplClass() {
        return attributesRequestorImplClass;
    }

    public static void setAttributesRequestorImplClass(String attributesRequestorImplClass) {
        SSOAgentConfigs.attributesRequestorImplClass = attributesRequestorImplClass;
    }

    public static void setOpenidLoginEnabled(Boolean openidLoginEnabled) {
        SSOAgentConfigs.openidLoginEnabled = openidLoginEnabled;
    }

    public static void setAssertionEncrypted(Boolean assertionEncrypted) {
        isAssertionEncrypted = assertionEncrypted;
    }

    public static void setKeyStoreStream(String keyStore) throws SSOAgentException {
        try {
            SSOAgentConfigs.keyStoreStream = new FileInputStream(keyStore);
        } catch (FileNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("File not found : ", e);
            }

            throw new SSOAgentException("Cannot find file " + keyStore);
        }
    }

    public static String getRequestQueryParameters() {
        return requestQueryParameters;
    }

    public static String getAddExtension() {
        return addExtension;
    }

    public static void setAddExtension(String addExtension) {
        SSOAgentConfigs.addExtension = addExtension;
    }

    /**
     * get the key store instance
     *
     * @param is            KeyStore InputStream
     * @param storePassword password of key store
     * @return KeyStore instant
     * @throws SSOAgentException if fails to load key store
     */
    private static KeyStore readKeyStore(InputStream is, String storePassword) throws SSOAgentException {

        if (storePassword == null) {
            throw new SSOAgentException("KeyStore password can not be null");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, storePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {

            throw new SSOAgentException("Error while loading key store file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {

                    throw new SSOAgentException("Error while closing input stream of key store", ignored);
                }
            }
        }
    }
}
