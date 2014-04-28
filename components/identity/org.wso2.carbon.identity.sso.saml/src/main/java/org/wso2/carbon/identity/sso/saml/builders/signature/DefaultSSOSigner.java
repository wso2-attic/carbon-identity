/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.builders.signature;

import org.apache.xml.security.c14n.Canonicalizer;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.StatusResponseType;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.*;
import org.opensaml.xml.validation.ValidationException;
import org.wso2.carbon.identity.base.IdentityException;

import javax.xml.namespace.QName;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;

public class DefaultSSOSigner implements SSOSigner{
    @Override
    public void init() throws IdentityException {
    }

    @Override
    public boolean doValidateXMLSignature(RequestAbstractType request, X509Credential cred, String alias) throws IdentityException{
        boolean isSignatureValid = false;

        if (request.getSignature() != null) {
            try {
                SignatureValidator validator =
                        new SignatureValidator(cred);
                validator.validate(request.getSignature());
                isSignatureValid = true;

            } catch (ValidationException e) {
                throw new IdentityException("Signature Validation Failed for the SAML Assertion : Signature is invalid.");
            }
        }
        return isSignatureValid;
    }

    @Override
    public Assertion doSetSignature(Assertion assertion, String signatureAlgorithm, X509Credential cred) throws IdentityException {
       try{
        Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(cred);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        try {
            KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
            X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
            X509Certificate cert = (X509Certificate) buildXMLObject(X509Certificate.DEFAULT_ELEMENT_NAME);
            String value = org.apache.xml.security.utils.Base64.encode(cred
                    .getEntityCertificate().getEncoded());
            cert.setValue(value);
            data.getX509Certificates().add(cert);
            keyInfo.getX509Datas().add(data);
            signature.setKeyInfo(keyInfo);
        } catch (CertificateEncodingException e) {
            throw new IdentityException("errorGettingCert");
        }

        assertion.setSignature(signature);

        List<Signature> signatureList = new ArrayList<Signature>();
        signatureList.add(signature);

        // Marshall and Sign
        MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration
                .getMarshallerFactory();
        Marshaller marshaller = marshallerFactory.getMarshaller(assertion);

        marshaller.marshall(assertion);

        org.apache.xml.security.Init.init();
        Signer.signObjects(signatureList);
        return assertion;
    } catch (Exception e) {
        throw new IdentityException("Error while signing the SAML Response message.", e);
    }
    }

    @Override
    public StatusResponseType doSignResponse(StatusResponseType response, String signatureAlgorithm, X509Credential cred) throws IdentityException {
        try {
            Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
            signature.setSigningCredential(cred);
            signature.setSignatureAlgorithm(signatureAlgorithm);
            signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            try {
                KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
                X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
                X509Certificate cert =
                        (X509Certificate) buildXMLObject(X509Certificate.DEFAULT_ELEMENT_NAME);
                String value =
                        org.apache.xml.security.utils.Base64.encode(cred.getEntityCertificate()
                                .getEncoded());
                cert.setValue(value);
                data.getX509Certificates().add(cert);
                keyInfo.getX509Datas().add(data);
                signature.setKeyInfo(keyInfo);
            } catch (CertificateEncodingException e) {
                throw new IdentityException("errorGettingCert");
            }

            response.setSignature(signature);

            List<Signature> signatureList = new ArrayList<Signature>();
            signatureList.add(signature);

            // Marshall and Sign
            MarshallerFactory marshallerFactory =
                    org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(response);

            marshaller.marshall(response);

            org.apache.xml.security.Init.init();
            Signer.signObjects(signatureList);
            return response;

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
}
