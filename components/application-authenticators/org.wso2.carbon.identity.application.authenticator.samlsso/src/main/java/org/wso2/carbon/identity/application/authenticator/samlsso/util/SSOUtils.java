/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.samlsso.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Data;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.X509CredentialImpl;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class SSOUtils {
    private SSOUtils() {

    }

    private static Log log = LogFactory.getLog(SSOUtils.class);

    /**
     * Generates a unique Id for Authentication Requests
     *
     * @return generated unique ID
     */

    public static String createID() {

        byte[] bytes = new byte[20]; // 160 bit

        new Random().nextBytes(bytes);

        char[] charMapping = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p'};

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
     * Sign the SAML AuthnRequest message
     *
     * @param authnRequest
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException
     */
    public static AuthnRequest setSignature(AuthnRequest authnRequest, String signatureAlgorithm,
                                            X509Credential cred) throws SAMLSSOException {
        try {
            Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSigningCredential(cred);
            signature.setSignatureAlgorithm(signatureAlgorithm);
            signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            try {
                KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
                X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
                org.opensaml.xml.signature.X509Certificate cert =
                        (org.opensaml.xml.signature.X509Certificate) buildXMLObject(org.opensaml.xml.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
                String value =
                        org.apache.xml.security.utils.Base64.encode(cred.getEntityCertificate().getEncoded());
                cert.setValue(value);
                data.getX509Certificates().add(cert);
                keyInfo.getX509Datas().add(data);
                signature.setKeyInfo(keyInfo);
            } catch (CertificateEncodingException e) {
                throw new SAMLSSOException("Error getting certificate", e);
            }

            authnRequest.setSignature(signature);

            List<Signature> signatureList = new ArrayList<Signature>();
            signatureList.add(signature);

            // Marshall and Sign
            MarshallerFactory marshallerFactory =
                    org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(authnRequest);

            marshaller.marshall(authnRequest);

            org.apache.xml.security.Init.init();
            Signer.signObjects(signatureList);
            return authnRequest;

        } catch (Exception e) {
            throw new SAMLSSOException("Error while signing the SAML Request message", e);
        }
    }

    /**
     * Sign the SAML AuthnRequest message
     *
     * @param logoutRequest
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws SAMLSSOException
     */
    public static LogoutRequest setSignature(LogoutRequest logoutRequest, String signatureAlgorithm,
                                             X509Credential cred) throws SAMLSSOException {
        try {
            Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSigningCredential(cred);
            signature.setSignatureAlgorithm(signatureAlgorithm);
            signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            try {
                KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
                X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
                org.opensaml.xml.signature.X509Certificate cert =
                        (org.opensaml.xml.signature.X509Certificate) buildXMLObject(org.opensaml.xml.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
                String value =
                        org.apache.xml.security.utils.Base64.encode(cred.getEntityCertificate().getEncoded());
                cert.setValue(value);
                data.getX509Certificates().add(cert);
                keyInfo.getX509Datas().add(data);
                signature.setKeyInfo(keyInfo);
            } catch (CertificateEncodingException e) {
                throw new SAMLSSOException("Error getting certificate", e);
            }

            logoutRequest.setSignature(signature);

            List<Signature> signatureList = new ArrayList<Signature>();
            signatureList.add(signature);

            // Marshall and Sign
            MarshallerFactory marshallerFactory =
                    org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(logoutRequest);

            marshaller.marshall(logoutRequest);

            org.apache.xml.security.Init.init();
            Signer.signObjects(signatureList);
            return logoutRequest;

        } catch (Exception e) {
            throw new SAMLSSOException("Error while signing the Logout Request message", e);
        }
    }

    /**
     * Appends the RSA-SHA1 signature to the query string
     *
     * @param tenantDomain    tenant domain
     * @param httpQueryString http query string to append the signature
     * @throws SAMLSSOException
     */
    public static void addSignatureToHTTPQueryString(String tenantDomain, StringBuilder
            httpQueryString) throws SAMLSSOException {
        try {
            httpQueryString.append("&SigAlg=");
            httpQueryString
                    .append(URLEncoder.encode(XMLSignature.ALGO_ID_SIGNATURE_RSA, "UTF-8").trim());

            X509Credential credential = new X509CredentialImpl(tenantDomain, null);
            java.security.Signature signature = java.security.Signature.getInstance("SHA1withRSA");
            signature.initSign(credential.getPrivateKey());
            signature.update(httpQueryString.toString().getBytes());

            byte[] signatureByteArray = signature.sign();
            String signatureBase64encodedString = Base64.encodeBytes(signatureByteArray,
                    Base64.DONT_BREAK_LINES);
            httpQueryString.append("&Signature=");
            httpQueryString.append(URLEncoder.encode(signatureBase64encodedString, "UTF-8").trim());
        } catch (Exception e) {
            throw new SAMLSSOException("Error while applying SAML2 Redirect Binding signature", e);
        }
    }

    /**
     * Builds SAML Elements
     *
     * @param objectQName
     * @return
     * @throws SAMLSSOException
     */
    private static XMLObject buildXMLObject(QName objectQName) throws SAMLSSOException {
        XMLObjectBuilder builder =
                org.opensaml.xml.Configuration.getBuilderFactory()
                        .getBuilder(objectQName);
        if (builder == null) {
            throw new SAMLSSOException("Unable to retrieve builder for object QName " +
                    objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(),
                objectQName.getPrefix());
    }

    /**
     * Decoding and deflating the encoded AuthReq
     *
     * @param encodedStr encoded AuthReq
     * @return decoded AuthReq
     */
    public static String decode(String encodedStr) throws SAMLSSOException {
        try {
            org.apache.commons.codec.binary.Base64 base64Decoder =
                    new org.apache.commons.codec.binary.Base64();
            byte[] xmlBytes = encodedStr.getBytes("UTF-8");
            byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

            try {
                //TODO if the request came in POST, inflating is wrong
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
                if (log.isDebugEnabled()) {
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
                String decodedStr = new String(baos.toByteArray(), Charset.forName("UTF-8"));
                if (log.isDebugEnabled()) {
                    log.debug("Request message " + decodedStr);
                }
                return decodedStr;
            }
        } catch (IOException e) {
            throw new SAMLSSOException("Error when decoding the SAML Request.", e);
        }

    }

    public static String decodeForPost(String encodedStr)
            throws SAMLSSOException {
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
            throw new SAMLSSOException(
                    "Error when decoding the SAML Request.", e);
        }
    }

    /**
     * Serializing a SAML2 object into a String
     *
     * @param xmlObject object that needs to serialized.
     * @return serialized object
     * @throws SAMLSSOException
     */
    public static String marshall(XMLObject xmlObject) throws SAMLSSOException {
        try {

            System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

            MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration
                    .getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
            Element element = marshaller.marshall(xmlObject);

            ByteArrayOutputStream byteArrayOutputStrm = new ByteArrayOutputStream();
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            return byteArrayOutputStrm.toString();
        } catch (Exception e) {
            log.error("Error Serializing the SAML Response");
            throw new SAMLSSOException("Error Serializing the SAML Response", e);
        }
    }

    /**
     * Encoding the response
     *
     * @param xmlString String to be encoded
     * @return encoded String
     */
    public static String encode(String xmlString) {
        String encodedRequestMessage = Base64.encodeBytes(xmlString.getBytes(), Base64.DONT_BREAK_LINES);
        return encodedRequestMessage.trim();
    }

    public static boolean isAuthnRequestSigned(Map<String, String> properties) {
        if (properties != null) {
            String prop = properties.get(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_AUTHN_REQ_SIGNED);
            if (prop != null) {
                return Boolean.parseBoolean(prop);
            }
        }
        return false;
    }

    public static boolean isLogoutEnabled(Map<String, String> properties) {
        if (properties != null) {
            String prop = properties.get(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_LOGOUT_ENABLED);
            if (prop != null) {
                return Boolean.parseBoolean(prop);
            }
        }
        return false;
    }

    public static boolean isLogoutRequestSigned(Map<String, String> properties) {
        if (properties != null) {
            String prop = properties.get(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_LOGOUT_REQ_SIGNED);
            if (prop != null) {
                return Boolean.parseBoolean(prop);
            }
        }
        return false;
    }

    public static boolean isAuthnResponseSigned(Map<String, String> properties) {
        if (properties != null) {
            String prop = properties.get(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_AUTHN_RESP_SIGNED);
            if (prop != null) {
                return Boolean.parseBoolean(prop);
            }
        }
        return false;
    }

    public static boolean isAssertionSigningEnabled(Map<String, String> properties) {
        if (properties != null) {
            String prop = properties.get(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_ENABLE_ASSERTION_SIGNING);
            if (prop != null) {
                return Boolean.parseBoolean(prop);
            }
        }
        return false;
    }

    public static boolean isAssertionEncryptionEnabled(Map<String, String> properties) {
        if (properties != null) {
            String prop = properties.get(IdentityApplicationConstants.Authenticator.SAML2SSO.IS_ENABLE_ASSERTION_ENCRYPTION);
            if (prop != null) {
                return Boolean.parseBoolean(prop);
            }
        }
        return false;
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String[] paramSplitArr = param.split("=");
            String name = paramSplitArr[0];
            String value = "";
            if (paramSplitArr.length > 1) {
                value = paramSplitArr[1];
            }
            map.put(name, value);
        }
        return map;
    }
}
