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
package org.wso2.carbon.identity.sso.saml.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusResponseType;
import org.opensaml.saml2.core.impl.AuthnRequestImpl;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.util.Base64;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.SAML2SSOFederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.builders.DefaultResponseBuilder;
import org.wso2.carbon.identity.sso.saml.builders.ErrorResponseBuilder;
import org.wso2.carbon.identity.sso.saml.builders.ResponseBuilder;
import org.wso2.carbon.identity.sso.saml.builders.X509CredentialImpl;
import org.wso2.carbon.identity.sso.saml.builders.encryption.SSOEncrypter;
import org.wso2.carbon.identity.sso.saml.builders.signature.SSOSigner;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.exception.IdentitySAML2SSOException;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.validators.SAML2HTTPRedirectSignatureValidator;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class SAMLSSOUtil {

    private static Log log = LogFactory.getLog(SAMLSSOUtil.class);
    private static RegistryService registryService;
    private static BundleContext bundleContext;
    private static RealmService realmService;
    private static ConfigurationContextService configCtxService;
    private static HttpService httpService;
    private static boolean isBootStrapped = false;
    private static Random random = new Random();
    private static final char[] charMapping = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p' };

    private static int singleLogoutRetryCount = 5;
    private static long singleLogoutRetryInterval = 60000;
    private static final Set<Character> UNRESERVED_CHARACTERS = new HashSet<Character>();
    private static String responseBuilderClassName = null;

    private static SSOEncrypter ssoEncrypter = null;
    private static SSOSigner ssoSigner = null;
    private static SAML2HTTPRedirectSignatureValidator samlHTTPRedirectSignatureValidator = null;
    private static ThreadLocal tenantDomainInThreadLocal = new ThreadLocal();
    private static ThreadLocal<Boolean> isSaaSApplication = null;
    

    static {
        for (char c = 'a'; c <= 'z'; c++)
            UNRESERVED_CHARACTERS.add(Character.valueOf(c));

        for (char c = 'A'; c <= 'A'; c++)
            UNRESERVED_CHARACTERS.add(Character.valueOf(c));

        for (char c = '0'; c <= '9'; c++)
            UNRESERVED_CHARACTERS.add(Character.valueOf(c));

        UNRESERVED_CHARACTERS.add(Character.valueOf('-'));
        UNRESERVED_CHARACTERS.add(Character.valueOf('.'));
        UNRESERVED_CHARACTERS.add(Character.valueOf('_'));
        UNRESERVED_CHARACTERS.add(Character.valueOf('~'));
    }
    
    
    public static boolean isSaaSApplication() {
        
        if (isSaaSApplication == null) {
            // this is the default behavior.
            return true;
        }
        
        if (isSaaSApplication.get() != null) {
            return isSaaSApplication.get().booleanValue();
        } else {
            return false;
        }
    }

    public static void setIsSaaSApplication(boolean isSaaSApp) {
        isSaaSApplication = new ThreadLocal<Boolean>();
        isSaaSApplication.set(Boolean.valueOf(isSaaSApp));
    }
    
    public static void removeSaaSApplicationThreaLocal() {
        if (isSaaSApplication != null) {
            isSaaSApplication.remove();
            isSaaSApplication = null;
        }
    }
    
    public static void setRegistryService(RegistryService registryService) {
        SAMLSSOUtil.registryService = registryService;
    }

    public static void setRealmService(RealmService realmService) {
        SAMLSSOUtil.realmService = realmService;
    }

    public static BundleContext getBundleContext() {
        return SAMLSSOUtil.bundleContext;
    }

    public static void setBundleContext(BundleContext bundleContext) {
        SAMLSSOUtil.bundleContext = bundleContext;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static ConfigurationContextService getConfigCtxService() {
        return configCtxService;
    }

    public static void setConfigCtxService(ConfigurationContextService configCtxService) {
        SAMLSSOUtil.configCtxService = configCtxService;
    }

    public static HttpService getHttpService() {
        return httpService;
    }

    public static void setHttpService(HttpService httpService) {
        SAMLSSOUtil.httpService = httpService;
    }

    /**
     * Constructing the AuthnRequest Object from a String
     *
     * @param authReqStr
     *            Decoded AuthReq String
     * @return AuthnRequest Object
     * @throws org.wso2.carbon.identity.base.IdentityException
     *
     */
    public static XMLObject unmarshall(String authReqStr) throws IdentityException {
        InputStream inputStream = null;
        try {
            doBootstrap();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            inputStream = new ByteArrayInputStream(authReqStr.trim().getBytes());
            Document document = docBuilder.parse(inputStream);
            Element element = document.getDocumentElement();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            return unmarshaller.unmarshall(element);
        } catch (Exception e) {
            log.error("Error in constructing AuthRequest from the encoded String", e);
            throw new IdentityException(
                    "Error in constructing AuthRequest from the encoded String ",
                    e);
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("Error while closing the stream");
                }
            }
        }
    }

    /**
     * Serialize the Auth. Request
     *
     * @param xmlObject
     * @return serialized auth. req
     */
    public static String marshall(XMLObject xmlObject) throws IdentityException {

        ByteArrayOutputStream byteArrayOutputStrm = null;
        try {
            doBootstrap();
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

            MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
            Element element = marshaller.marshall(xmlObject);

            byteArrayOutputStrm = new ByteArrayOutputStream();
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            return byteArrayOutputStrm.toString();
        } catch (Exception e) {
            log.error("Error Serializing the SAML Response");
            throw new IdentityException("Error Serializing the SAML Response", e);
        }finally {
            if(byteArrayOutputStrm != null){
                try {
                    byteArrayOutputStrm.close();
                } catch (IOException e) {
                    log.error("Error while closing the stream");
                }
            }
        }
    }

    /**
     * Encoding the response
     *
     * @param xmlString
     *            String to be encoded
     * @return encoded String
     */
    public static String encode(String xmlString) {
        // Encoding the message
        String encodedRequestMessage =
                Base64.encodeBytes(xmlString.getBytes(),
                        Base64.DONT_BREAK_LINES);
        return encodedRequestMessage.trim();
    }

    /**
     * Decoding and deflating the encoded AuthReq
     *
     * @param encodedStr
     *            encoded AuthReq
     * @return decoded AuthReq
     */
    public static String decode(String encodedStr) throws IdentityException {
        try {
            org.apache.commons.codec.binary.Base64 base64Decoder =
                    new org.apache.commons.codec.binary.Base64();
            byte[] xmlBytes = encodedStr.getBytes("UTF-8");
            byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

            try {
                Inflater inflater = new Inflater(true);
                inflater.setInput(base64DecodedByteArray);
                byte[] xmlMessageBytes = new byte[5000];
                int resultLength = inflater.inflate(xmlMessageBytes);

                if (inflater.getRemaining() > 0) {
                    throw new RuntimeException("didn't allocate enough space to hold "
                            + "decompressed data");
                }

                inflater.end();
                String decodedString = new String(xmlMessageBytes, 0, resultLength, "UTF-8");
                if(log.isDebugEnabled()) {
                    log.debug("Request message " + decodedString);
                }
                return decodedString;

            } catch (DataFormatException e) {
                ByteArrayInputStream bais = new ByteArrayInputStream(base64DecodedByteArray);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InflaterInputStream iis = new InflaterInputStream(bais);
                byte[] buf = new byte[1024];
                int count = iis.read(buf);
                while (count != -1) {
                    baos.write(buf, 0, count);
                    count = iis.read(buf);
                }
                iis.close();
                String decodedStr = new String(baos.toByteArray());
                if(log.isDebugEnabled()) {
                    log.debug("Request message " + decodedStr);
                }
                return decodedStr;
            }
        } catch (IOException e) {
            throw new IdentityException("Error when decoding the SAML Request.", e);
        }

    }


    public static String decodeForPost(String encodedStr)
            throws IdentityException {
        try {
            org.apache.commons.codec.binary.Base64 base64Decoder = new org.apache.commons.codec.binary.Base64();
            byte[] xmlBytes = encodedStr.getBytes("UTF-8");
            byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

            String decodedString = new String(base64DecodedByteArray, "UTF-8");
            if (log.isDebugEnabled()) {
                log.debug("Request message " + decodedString);
            }
            return decodedString;

        } catch (IOException e) {
            throw new IdentityException(
                    "Error when decoding the SAML Request.", e);
        }

    }

    /**
     * Get the Issuer
     *
     * @return Issuer
     */
    public static Issuer getIssuer() throws IdentityException {
        Issuer issuer = new IssuerBuilder().buildObject();
        String idPEntityId = null;
        IdentityProvider identityProvider = null;
        String tenantDomain = null;
        try {
            tenantDomain = SAMLSSOUtil.getTenantDomainFromThreadLocal();
            if(tenantDomain == null){
                tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }
            identityProvider = IdentityProviderManager.getInstance().getResidentIdP(tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            throw new IdentityException(
                    "Error occurred while retrieving Resident Identity Provider information for tenant " + tenantDomain);
        }
        FederatedAuthenticatorConfig[] authnConfigs = identityProvider.getFederatedAuthenticatorConfigs();
        for(FederatedAuthenticatorConfig config : authnConfigs){
            if(IdentityApplicationConstants.Authenticator.SAML2SSO.NAME.equals(config.getName())){
                SAML2SSOFederatedAuthenticatorConfig samlFedAuthnConfig = new SAML2SSOFederatedAuthenticatorConfig(config);
                idPEntityId = samlFedAuthnConfig.getIdpEntityId();
            }
        }
        if(idPEntityId == null){
            idPEntityId = IdentityUtil.getProperty(IdentityConstants.ServerConfig.ENTITY_ID);
        }
        issuer.setValue(idPEntityId);
        issuer.setFormat(SAMLSSOConstants.NAME_ID_POLICY_ENTITY);
        return issuer;
    }

    public static void doBootstrap() {
        if (!isBootStrapped) {
            try {
                DefaultBootstrap.bootstrap();
                isBootStrapped = true;
            } catch (ConfigurationException e) {
                log.error("Error in bootstrapping the OpenSAML2 library", e);
            }
        }
    }


    /**
	 * Sign the SAML Response message
	 * 
	 * @param response
	 * @param signatureAlgorithm
	 * @param cred
	 * @return
	 * @throws IdentityException
	 */
	public static Response setSignature(Response response, String signatureAlgorithm,
	                                    X509Credential cred) throws IdentityException {
        return (Response)signResponse(response, signatureAlgorithm,cred);
	}

    /**
     * Sign the SAML LogoutResponse message
     *
     * @param response
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws IdentityException
     */
    public static LogoutResponse setSignature(LogoutResponse response, String signatureAlgorithm,
                                        X509Credential cred) throws IdentityException {
        return (LogoutResponse)signResponse(response, signatureAlgorithm,cred);
    }

    /**
     * Generic method to sign both both SAML Response and SAML LogoutResponse
     *
     * @param response
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws IdentityException
     */
    private static StatusResponseType signResponse(StatusResponseType response, String signatureAlgorithm,
                                            X509Credential cred) throws IdentityException{

        doBootstrap();
        try {

            synchronized (Runtime.getRuntime().getClass()){
                ssoSigner = (SSOSigner)Class.forName(IdentityUtil.getProperty(
                        "SSOService.SAMLSSOSigner").trim()).newInstance();
                ssoSigner.init();
            }

            return ssoSigner.doSignResponse(response, signatureAlgorithm, cred);

        } catch (ClassNotFoundException e) {
            throw new IdentityException("Class not found: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (InstantiationException e) {
            throw new IdentityException("Error while instantiating class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (IllegalAccessException e) {
            throw new IdentityException("Illegal access to class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (Exception e) {
            throw new IdentityException("Error while signing the SAML Response message.", e);
        }

    }

    public static Assertion setSignature(Assertion assertion, String signatureAlgorithm,
                                         X509Credential cred) throws IdentityException {
        doBootstrap();
        try {

            synchronized (Runtime.getRuntime().getClass()){
                ssoSigner = (SSOSigner)Class.forName(IdentityUtil.getProperty(
                        "SSOService.SAMLSSOSigner").trim()).newInstance();
                ssoSigner.init();
            }

            return ssoSigner.doSetSignature(assertion, signatureAlgorithm,cred);
//            Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
//            signature.setSigningCredential(cred);
//            signature.setSignatureAlgorithm(signatureAlgorithm);
//            signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
//
//            try {
//                KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
//                X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
//                X509Certificate cert = (X509Certificate) buildXMLObject(X509Certificate.DEFAULT_ELEMENT_NAME);
//                String value = org.apache.xml.security.utils.Base64.encode(cred
//                        .getEntityCertificate().getEncoded());
//                cert.setValue(value);
//                data.getX509Certificates().add(cert);
//                keyInfo.getX509Datas().add(data);
//                signature.setKeyInfo(keyInfo);
//            } catch (CertificateEncodingException e) {
//                throw new IdentityException("errorGettingCert");
//            }
//
//            assertion.setSignature(signature);
//
//            List<Signature> signatureList = new ArrayList<Signature>();
//            signatureList.add(signature);
//
//            // Marshall and Sign
//            MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration
//                    .getMarshallerFactory();
//            Marshaller marshaller = marshallerFactory.getMarshaller(assertion);
//
//            marshaller.marshall(assertion);
//
//            org.apache.xml.security.Init.init();
//            Signer.signObjects(signatureList);
//            return assertion;
        } catch (ClassNotFoundException e) {
            throw new IdentityException("Class not found: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (InstantiationException e) {
            throw new IdentityException("Error while instantiating class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (IllegalAccessException e) {
            throw new IdentityException("Illegal access to class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (Exception e) {
            throw new IdentityException("Error while signing the SAML Response message.", e);
        }
    }

    public static EncryptedAssertion setEncryptedAssertion(Assertion assertion, String encryptionAlgorithm,
                                                           String alias, String domainName) throws IdentityException {
        doBootstrap();
        try {
            X509Credential cred = SAMLSSOUtil.getX509CredentialImplForTenant(domainName, alias);

                synchronized (Runtime.getRuntime().getClass()){
                            ssoEncrypter = (SSOEncrypter)Class.forName(IdentityUtil.getProperty(
                                    "SSOService.SAMLSSOEncrypter").trim()).newInstance();
                            ssoEncrypter.init();
                }
                return  ssoEncrypter.doEncryptedAssertion(assertion, cred, alias, encryptionAlgorithm);
//
//            Credential symmetricCredential = SecurityHelper.getSimpleCredential(
//                    SecurityHelper.generateSymmetricKey(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256));
//
//            EncryptionParameters encParams = new EncryptionParameters();
//            encParams.setAlgorithm(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256);
//            encParams.setEncryptionCredential(symmetricCredential);
//
//            KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
//            keyEncryptionParameters.setAlgorithm(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSA15);
//            keyEncryptionParameters.setEncryptionCredential(cred);
//
//            Encrypter encrypter = new Encrypter(encParams, keyEncryptionParameters);
//            encrypter.setKeyPlacement(Encrypter.KeyPlacement.INLINE);
//
//            EncryptedAssertion encrypted = encrypter.encrypt(assertion);
////            response.getEncryptedAssertions().add(encrypted);
//            return encrypted;
        } catch (ClassNotFoundException e) {
            throw new IdentityException("Class not found: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOEncrypter"), e);
        } catch (InstantiationException e) {
            throw new IdentityException("Error while instantiating class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOEncrypter"), e);
        } catch (IllegalAccessException e) {
            throw new IdentityException("Illegal access to class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOEncrypter"), e);
        } catch (Exception e) {
            throw new IdentityException("Error while signing the SAML Response message.", e);
        }
    }


    /**
     * Builds SAML Elements
     *
     * @param objectQName
     * @return
     * @throws IdentityException
     */
    private static XMLObject buildXMLObject(QName objectQName) throws IdentityException {
        XMLObjectBuilder builder =
                org.opensaml.xml.Configuration.getBuilderFactory()
                        .getBuilder(objectQName);
        if (builder == null) {
            throw new IdentityException("Unable to retrieve builder for object QName " +
                    objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(),
                objectQName.getPrefix());
    }

    public static String createID() {

        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);

        char[] chars = new char[40];

        for (int i = 0; i < bytes.length; i++) {
            int left = (bytes[i] >> 4) & 0x0f;
            int right = bytes[i] & 0x0f;
            chars[i * 2] = charMapping[left];
            chars[i * 2 + 1] = charMapping[right];
        }

        return String.valueOf(chars);
    }

    /**
     * Generate the key store name from the domain name
     *
     * @param tenantDomain
     *            tenant domain name
     * @return key store file name
     */
    public static String generateKSNameFromDomainName(String tenantDomain) {
        String ksName = tenantDomain.trim().replace(".", "-");
        return (ksName + ".jks");
    }

    /**
     * Get the X509CredentialImpl object for a particular tenant
     *
     * @param domainName
     *            domain name
     * @return X509CredentialImpl object containing the public certificate of
     *         that tenant
     * @throws org.wso2.carbon.identity.sso.saml.exception.IdentitySAML2SSOException
     *             Error when creating X509CredentialImpl object
     */
    public static X509CredentialImpl getX509CredentialImplForTenant(String domainName, String alias)
            throws IdentitySAML2SSOException {

        int tenantID = -1234;
        RealmService realmService = SAMLSSOUtil.getRealmService();

        // get the tenantID
        if (domainName != null) {
            try {
                tenantID = realmService.getTenantManager().getTenantId(domainName);
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                String errorMsg = "Error getting the TenantID for the domain name";
                log.error(errorMsg, e);
                throw new IdentitySAML2SSOException(errorMsg, e);
            }
        }

        KeyStoreManager keyStoreManager;
        // get an instance of the corresponding Key Store Manager instance
        keyStoreManager = KeyStoreManager.getInstance(tenantID);

        X509CredentialImpl credentialImpl = null;
        KeyStore keyStore;

        try {
            if (tenantID != -1234) {// for tenants, load private key from their generated key store
                keyStore = keyStoreManager.getKeyStore(generateKSNameFromDomainName(domainName));
            } else { // for super tenant, load the default pub. cert using the
                // config. in carbon.xml
                keyStore = keyStoreManager.getPrimaryKeyStore();
            }
            java.security.cert.X509Certificate cert =
                    (java.security.cert.X509Certificate) keyStore.getCertificate(alias);
            credentialImpl = new X509CredentialImpl(cert);

        } catch (Exception e) {
            String errorMsg =
                    "Error instantiating an X509CredentialImpl object for the public cert.";
            log.error(errorMsg, e);
            throw new IdentitySAML2SSOException(errorMsg, e);
        }
        return credentialImpl;
    }

    /**
     * Validates the request message's signature. Validates the signature of
     * both HTTP POST Binding and HTTP Redirect Binding.
     *
     * @param authnReqDTO
     * @return
     */
    public static boolean validateAuthnRequestSignature(SAMLSSOAuthnReqDTO authnReqDTO) {
        log.debug("Validating SAML Request signature");

        //String domainName = MultitenantUtils.getTenantDomain(authnReqDTO.getUsername());
        String domainName = authnReqDTO.getTenantDomain();
        if (authnReqDTO.isStratosDeployment()) {
            domainName = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        String alias = authnReqDTO.getCertAlias();
        RequestAbstractType request = null;
        try {
            String decodedReq = null;

            if (authnReqDTO.getQueryString() != null) {
                decodedReq = SAMLSSOUtil.decode(authnReqDTO.getRequestMessageString());
            } else {
                decodedReq = SAMLSSOUtil.decodeForPost(authnReqDTO.getRequestMessageString());
            }

            request = (RequestAbstractType) SAMLSSOUtil.unmarshall(decodedReq);
        } catch (IdentityException e) {
            log.warn("Signature Validation failed for the SAMLRequest : Failed to unmarshall the SAML Assertion");
            log.debug(e);
        }

        try{
        if (authnReqDTO.getQueryString() != null) {
            // DEFLATE signature in Redirect Binding
            return validateDeflateSignature(authnReqDTO.getQueryString(), authnReqDTO.getIssuer(), alias,
                    domainName);
        } else {
            // XML signature in SAML Request message for POST Binding
            return validateXMLSignature(request, alias, domainName);
        }
        } catch (IdentityException e) {
            log.warn("Signature Validation failed for the SAMLRequest : Failed to validate the SAML Assertion");
            log.debug(e);
            return false;
        }
    }

    /**
     * Validates the signature of the LogoutRequest message.
     * TODO : for stratos deployment, super tenant key should be used
     *
     * @param logoutRequest
     * @param alias
     * @param subject
     * @param httpRequest
     * @param isHTTPRedirectBinding
     * @return
     */
    public static boolean validateLogoutRequestSignature(LogoutRequest logoutRequest, String alias,
                                                         String subject, String queryString) {
        String domainName = MultitenantUtils.getTenantDomain(subject);
		/*
		 * if (isStratosDeployment) {
		 * domainName = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
		 * }
		 */
        try{
        if (queryString != null) {
            return validateDeflateSignature(queryString, logoutRequest.getIssuer().getValue(), alias,
                    domainName);
        } else {
            return validateXMLSignature(logoutRequest, alias, domainName);
        }
    } catch (IdentityException e) {
        log.warn("Failed to validate login request signature ");
        log.debug(e);
            return false;
    }
    }

    /**
     * Signature validation for HTTP Redirect Binding
     *
     * @param authnReqDTO
     * @param samlRequest
     * @param alias
     * @param domainName
     * @return
     */
    public static boolean validateDeflateSignature(String queryString, String issuer,
                                                    String alias, String domainName) throws IdentityException{
        try {

                synchronized (Runtime.getRuntime().getClass()){
                    samlHTTPRedirectSignatureValidator = (SAML2HTTPRedirectSignatureValidator)Class.forName(IdentityUtil.getProperty(
                            "SSOService.SAML2HTTPRedirectSignatureValidator").trim()).newInstance();
                    samlHTTPRedirectSignatureValidator.init();
                }

                return samlHTTPRedirectSignatureValidator.validateSignature(queryString, issuer,
                        alias, domainName);

        } catch (SecurityException e) {
            log.error("Error validating deflate signature", e);
            return false;
        } catch (IdentitySAML2SSOException e) {
            log.warn("Signature validation failed for the SAML Message : Failed to construct the X509CredentialImpl for the alias " +
                    alias);
            return false;
        } catch (ClassNotFoundException e) {
            throw new IdentityException("Class not found: "
                    + IdentityUtil.getProperty("SSOService.SAML2HTTPRedirectSignatureValidator"), e);
        } catch (InstantiationException e) {
            throw new IdentityException("Error while instantiating class: "
                    + IdentityUtil.getProperty("SSOService.SAML2HTTPRedirectSignatureValidator"), e);
        } catch (IllegalAccessException e) {
            throw new IdentityException("Illegal access to class: "
                    + IdentityUtil.getProperty("SSOService.SAML2HTTPRedirectSignatureValidator"), e);
        }
    }

    /**
     * Validate the signature of an assertion
     *
     * @param request
     *            SAML Assertion, this could be either a SAML Request or a
     *            LogoutRequest
     * @param alias
     *            Certificate alias against which the signature is validated.
     * @param domainName
     *            domain name of the subject
     * @return true, if the signature is valid.
     */
    public static boolean validateXMLSignature(RequestAbstractType request, String alias,
                                                String domainName) throws IdentityException{

        boolean isSignatureValid = false;

        if (request.getSignature() != null) {
            try {
                X509Credential cred = SAMLSSOUtil.getX509CredentialImplForTenant(domainName, alias);

                synchronized (Runtime.getRuntime().getClass()){
                    ssoSigner = (SSOSigner)Class.forName(IdentityUtil.getProperty(
                            "SSOService.SAMLSSOSigner").trim()).newInstance();
                    ssoSigner.init();
                }

                return ssoSigner.doValidateXMLSignature(request, cred, alias);
            } catch (IdentitySAML2SSOException ignore) {
                log.warn("Signature validation failed for the SAML Message : Failed to construct the X509CredentialImpl for the alias " +
                        alias);
                log.debug(ignore);
            } catch (IdentityException ignore) {
                log.warn("Signature Validation Failed for the SAML Assertion : Signature is invalid.");
                log.debug(ignore);
        } catch (ClassNotFoundException e) {
            throw new IdentityException("Class not found: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (InstantiationException e) {
            throw new IdentityException("Error while instantiating class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        } catch (IllegalAccessException e) {
            throw new IdentityException("Illegal access to class: "
                    + IdentityUtil.getProperty("SSOService.SAMLSSOSigner"), e);
        }catch (Exception e){

            }
        }
        return isSignatureValid;
    }

    /**
     * Return a Array of Claims containing requested attributes and values
     *
     * @param authnReqDTO
     * @return Map with attributes and values
     * @throws IdentityException
     */
    public static Map<String, String> getAttributes(SAMLSSOAuthnReqDTO authnReqDTO) throws IdentityException {

        int index = 0;

        // trying to get the Service Provider Configurations
        SSOServiceProviderConfigManager spConfigManager =
                SSOServiceProviderConfigManager.getInstance();
        SAMLSSOServiceProviderDO spDO = spConfigManager.getServiceProvider(authnReqDTO.getIssuer());

        if (spDO == null) {
            IdentityPersistenceManager persistenceManager =
                    IdentityPersistenceManager.getPersistanceManager();

        	Registry registry = (Registry)PrivilegedCarbonContext.getThreadLocalCarbonContext().getRegistry(RegistryType.SYSTEM_CONFIGURATION);
        	spDO = persistenceManager.getServiceProvider(registry,authnReqDTO.getIssuer());
        }

        if(!authnReqDTO.isIdPInitSSO()){

            AuthnRequestImpl request = null;

            try {
                request = (AuthnRequestImpl) SAMLSSOUtil.unmarshall(SAMLSSOUtil.decode(authnReqDTO
                        .getRequestMessageString()));
            } catch (IdentityException e) {
                request = (AuthnRequestImpl) SAMLSSOUtil.unmarshall(SAMLSSOUtil
                        .decodeForPost(authnReqDTO.getRequestMessageString()));
            }

            if (request.getAttributeConsumingServiceIndex() == null) {
                if (authnReqDTO.getAttributeConsumingServiceIndex() != 0) {
                    index = authnReqDTO.getAttributeConsumingServiceIndex();
                    spDO.setAttributeConsumingServiceIndex(String.valueOf(index));
                } else {
                    return null; // not requesting for attributes
                }
            } else {
                index = request.getAttributeConsumingServiceIndex();
            }
        } else {
            index = authnReqDTO.getAttributeConsumingServiceIndex();
            if(index != 0){
                spDO.setAttributeConsumingServiceIndex(String.valueOf(index));
            }

        }

		
		/*
		 * IMPORTANT : checking if the consumer index in the request matches the
		 * given id to the SP
		 */
        if (spDO.getAttributeConsumingServiceIndex() == null ||
                "".equals(spDO.getAttributeConsumingServiceIndex()) ||
                index != Integer.parseInt(spDO.getAttributeConsumingServiceIndex())) {
            log.debug("Invalid AttributeConsumingServiceIndex in AuthnRequest");
            return null;
        }

        Map<String, String> claimsMap = new HashMap<String, String>();
        if (authnReqDTO.getUserAttributes() != null) {
            for (Map.Entry<ClaimMapping, String> entry : authnReqDTO.getUserAttributes().entrySet()) {
                claimsMap.put(entry.getKey().getRemoteClaim().getClaimUri(), entry.getValue());
            }
        }
        return claimsMap;
    }



    /**
     * build the error response
     * @param id
     * @param statusCodes
     * @param statusMsg
     * @return decoded response
     * @throws IdentityException
     */
    public static String buildErrorResponse(String id, List<String> statusCodes, String statusMsg) throws IdentityException {
        ErrorResponseBuilder respBuilder = new ErrorResponseBuilder();
        Response response = respBuilder.buildResponse(id, statusCodes, statusMsg);
        return SAMLSSOUtil.encode(SAMLSSOUtil.marshall(response));
    }

    public static int getSAMLResponseValidityPeriod(){
        if(IdentityUtil.getProperty(IdentityConstants.ServerConfig.SAML_RESPONSE_VALIDITY_PERIOD) != null &&
                !IdentityUtil.getProperty(IdentityConstants.ServerConfig.SAML_RESPONSE_VALIDITY_PERIOD).trim().equals("")){
            return Integer.parseInt(IdentityUtil.getProperty(
                    IdentityConstants.ServerConfig.SAML_RESPONSE_VALIDITY_PERIOD).trim());
        } else {
            return 5;
        }
    }

    public static int getSingleLogoutRetryCount() {
        return singleLogoutRetryCount;
    }

    public static void setSingleLogoutRetryCount(int singleLogoutRetryCount) {
        SAMLSSOUtil.singleLogoutRetryCount = singleLogoutRetryCount;
    }

    public static long getSingleLogoutRetryInterval() {
        return singleLogoutRetryInterval;
    }

    public static void setSingleLogoutRetryInterval(long singleLogoutRetryInterval) {
        SAMLSSOUtil.singleLogoutRetryInterval = singleLogoutRetryInterval;
    }
    
    public static ResponseBuilder getResponseBuilder() {
    	if(responseBuilderClassName == null || "".equals(responseBuilderClassName)) {
    		return new DefaultResponseBuilder();
    	} else {
    		try {
				// Bundle class loader will cache the loaded class and returned
				// the already loaded instance, hence calling this method
				// multiple times doesn't cost.
				Class clazz = Thread.currentThread().getContextClassLoader()
						.loadClass(responseBuilderClassName);
				return (ResponseBuilder) clazz.newInstance();

			} catch (ClassNotFoundException e) {
				log.error("Error while instantiating the SAMLResponseBuilder ", e);
			} catch (InstantiationException e) {
				log.error("Error while instantiating the SAMLResponseBuilder ", e);
			} catch (IllegalAccessException e) {
				log.error("Error while instantiating the SAMLResponseBuilder ", e);
			}
    	}
		return null;
    }
    
    public static void setResponseBuilder(String responseBuilder) {
    	responseBuilderClassName = responseBuilder;
    }

    /**
     * This check if the status code is 2XX, check value between 200 and 300
     *
     * @param status
     * @return
     */
    public static boolean isHttpSuccessStatusCode(int status) {
        return status >= 200 && status < 300;
    }

    public static String getUserNameFromOpenID(String openid) throws IdentityException {
        String caller = null;
        String path = null;
        URI uri = null;
        String contextPath = "/openid/";

        try {
            uri = new URI(openid);
            path = uri.getPath();
        } catch (URISyntaxException e) {
            throw new IdentityException("Invalid OpenID", e);
        }
        caller = path.substring(path.indexOf(contextPath) + contextPath.length(), path.length());
        return caller;
    }

    /**
     * Find the OpenID corresponding to the given user name.
     *
     * @param userName
     *            User name
     * @return OpenID corresponding the given user name.
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public static String getOpenID(String userName) throws IdentityException {
        return generateOpenID(userName);
    }

    /**
     * Generate OpenID for a given user.
     *
     * @param user
     *            User
     * @return Generated OpenID
     * @throws org.wso2.carbon.identity.base.IdentityException
     */
    public static String generateOpenID(String user) throws IdentityException {
        String openIDUserUrl = null;
        String openID = null;
        URI uri = null;
        URL url = null;
        openIDUserUrl = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_USER_PATTERN);
        user = normalizeUrlEncoding(user);
        openID = openIDUserUrl + user;
        try {
            uri = new URI(openID);
        } catch (URISyntaxException e) {
            throw new IdentityException("Invalid OpenID URL :" + openID, e);
        }
        try {
            url = uri.normalize().toURL();
            if (url.getQuery() != null || url.getRef() != null) {
                throw new IdentityException("Invalid user name for OpenID :" + openID);
            }
        } catch (MalformedURLException e) {
            throw new IdentityException("Malformed OpenID URL :" + openID, e);
        }
        openID = url.toString();
        return openID;
    }

    private static String normalizeUrlEncoding(String text) {

        if (text == null)
            return null;

        int len = text.length();
        StringBuffer normalized = new StringBuffer(len);

        for (int i = 0; i < len; i++) {
            char current = text.charAt(i);
            if (current == '%' && i < len - 2) {
                String percentCode = text.substring(i, i + 3).toUpperCase();
                try {
                    String str = URLDecoder.decode(percentCode, "ISO-8859-1");
                    char chr = str.charAt(0);
                    if (UNRESERVED_CHARACTERS.contains(Character.valueOf(chr)))
                        normalized.append(chr);
                    else
                        normalized.append(percentCode);
                } catch (UnsupportedEncodingException e) {
                    normalized.append(percentCode);
                }
                i += 2;
            } else {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }
    
    public static void removeSession(String sessionId, String issuer) {
		SSOSessionPersistenceManager ssoSessionPersistenceManager = SSOSessionPersistenceManager
				.getPersistenceManager();
		
		String sessionIndex = ssoSessionPersistenceManager.getSessionIndexFromTokenId(sessionId);
		
		SSOSessionPersistenceManager.removeSessionInfoDataFromCache(sessionIndex);
		SSOSessionPersistenceManager.removeSessionIndexFromCache(sessionId);
	}

    public static void setTenantDomainInThreadLocal(String tenantDomain){
        SAMLSSOUtil.tenantDomainInThreadLocal.set(tenantDomain);
    }

    public static String getTenantDomainFromThreadLocal(){
        return (String)SAMLSSOUtil.tenantDomainInThreadLocal.get();
    }

}
