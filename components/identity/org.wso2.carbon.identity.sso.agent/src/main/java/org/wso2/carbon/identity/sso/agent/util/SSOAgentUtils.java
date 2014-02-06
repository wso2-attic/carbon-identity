/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.sso.agent.util;

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
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;

import javax.xml.namespace.QName;
import java.net.URLEncoder;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class SSOAgentUtils {

    private static Logger LOGGER = Logger.getLogger("InfoLogging");

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
     * @throws org.wso2.carbon.identity.sso.agent.exception.SSOAgentException
     */
    public static AuthnRequest setSignature(AuthnRequest authnRequest, String signatureAlgorithm,
                                        X509Credential cred) throws SSOAgentException {
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
                throw new SSOAgentException("Error getting certificate", e);
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
            throw new SSOAgentException("Error while signing the SAML Request message", e);
        }
    }

    /**
     * Sign the SAML AuthnRequest message
     *
     * @param logoutRequest
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws SSOAgentException
     */
    public static LogoutRequest setSignature(LogoutRequest logoutRequest, String signatureAlgorithm,
                                            X509Credential cred) throws SSOAgentException {
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
                throw new SSOAgentException("Error getting certificate",e);
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
            throw new SSOAgentException("Error while signing the Logout Request message", e);
        }
    }
    
    public static void addDeflateSignatureToHTTPQueryString(StringBuilder httpQueryString,
                                                            X509Credential cred) throws SSOAgentException {
        try {
            httpQueryString.append("&SigAlg="
                    + URLEncoder.encode(XMLSignature.ALGO_ID_SIGNATURE_RSA, "UTF-8").trim());

            java.security.Signature signature = java.security.Signature.getInstance("SHA1withRSA");
            signature.initSign(cred.getPrivateKey());
            signature.update(httpQueryString.toString().getBytes());
            byte[] signatureByteArray = signature.sign();

            String signatureBase64encodedString = Base64.encodeBytes(signatureByteArray,
                    Base64.DONT_BREAK_LINES);
            httpQueryString.append("&Signature="
                    + URLEncoder.encode(signatureBase64encodedString, "UTF-8").trim());
        } catch (Exception e) {
            throw new SSOAgentException("Error applying SAML2 Redirect Binding signature", e);
        }
    }

    /**
     * Builds SAML Elements
     *
     * @param objectQName
     * @return
     * @throws SSOAgentException
     */
    private static XMLObject buildXMLObject(QName objectQName) throws SSOAgentException {
        XMLObjectBuilder builder =
                org.opensaml.xml.Configuration.getBuilderFactory()
                        .getBuilder(objectQName);
        if (builder == null) {
            throw new SSOAgentException("Unable to retrieve builder for object QName " +
                    objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(),
                objectQName.getPrefix());
    }

}
