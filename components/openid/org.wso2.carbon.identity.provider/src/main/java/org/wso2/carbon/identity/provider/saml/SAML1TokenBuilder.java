/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasData;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.utils.Base64;
import org.joda.time.DateTime;
import org.opensaml.saml1.core.Assertion;
import org.opensaml.saml1.core.Attribute;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.AttributeValue;
import org.opensaml.saml1.core.Audience;
import org.opensaml.saml1.core.AudienceRestrictionCondition;
import org.opensaml.saml1.core.Conditions;
import org.opensaml.saml1.core.ConfirmationMethod;
import org.opensaml.saml1.core.Subject;
import org.opensaml.saml1.core.SubjectConfirmation;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Certificate;
import org.opensaml.xml.signature.X509Data;
import org.w3c.dom.Element;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.provider.GenericIdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.RequestedClaimData;

import javax.xml.namespace.QName;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SAML1TokenBuilder implements SAMLTokenBuilder {

    public static final String CONF_KEY = "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";
    private static final Log log = LogFactory.getLog(SAML1TokenBuilder.class);
    protected Assertion assertion = null;
    protected AttributeStatement attributeStmt = null;
    protected List<Signature> signatureList = new ArrayList<Signature>();
    protected Element signedAssertion = null;
    protected String appilesTo = null;

    protected static XMLObject buildXMLObject(QName objectQName) throws IdentityProviderException {
        XMLObjectBuilder builder = Configuration.getBuilderFactory().getBuilder(objectQName);
        if (builder == null) {
            throw new IdentityProviderException("Unable to retrieve builder for object QName " + objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(), objectQName.getPrefix());
    }

    @Override
    public void createStatement(GenericIdentityProviderData ipData, RahasData rahasData)
            throws IdentityProviderException {
        if (log.isDebugEnabled()) {
            log.debug("Begin SAML statement creation.");
        }
        attributeStmt = (AttributeStatement) buildXMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME);

        Subject subject = (Subject) buildXMLObject(Subject.DEFAULT_ELEMENT_NAME);
        SubjectConfirmation subjectConf =
                (SubjectConfirmation) buildXMLObject(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        ConfirmationMethod confMethod = (ConfirmationMethod) buildXMLObject(ConfirmationMethod.DEFAULT_ELEMENT_NAME);
        confMethod.setConfirmationMethod(CONF_KEY);
        subjectConf.getConfirmationMethods().add(confMethod);
        subject.setSubjectConfirmation(subjectConf);

        attributeStmt.setSubject(subject);

        Map<String, RequestedClaimData> mapClaims = ipData.getRequestedClaims();

        if (rahasData.getAppliesToAddress() != null) {
            appilesTo = rahasData.getAppliesToAddress();
        }

        Iterator<RequestedClaimData> ite = mapClaims.values().iterator();

        while (ite.hasNext()) {
            RequestedClaimData claim = ite.next();
            String uri = claim.getUri();

            int index = uri.lastIndexOf("/");
            String attrName = uri.substring(index + 1, uri.length());
            String attrNamespace = uri.substring(0, index);

            Attribute attribute = (Attribute) buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
            attribute.setAttributeName(attrName);
            attribute.setAttributeNamespace(attrNamespace);

            XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
            XSStringBuilder attributeValueBuilder = (XSStringBuilder) builderFactory.getBuilder(XSString.TYPE_NAME);

            XSString stringValue =
                    attributeValueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
            stringValue.setValue(claim.getValue());
            attribute.getAttributeValues().add(stringValue);

            attributeStmt.getAttributes().add(attribute);
        }
    }

    @Override
    public void createSAMLAssertion(DateTime notAfter, DateTime notBefore, String assertionId)
            throws IdentityProviderException {
        assertion = (Assertion) buildXMLObject(Assertion.DEFAULT_ELEMENT_NAME);
        Conditions conditions = (Conditions) buildXMLObject(Conditions.DEFAULT_ELEMENT_NAME);
        conditions.setNotBefore(notBefore);
        conditions.setNotOnOrAfter(notAfter);

        ServerConfiguration config = ServerConfiguration.getInstance();
        String host = "http://" + config.getFirstProperty("HostName");
        assertion.setIssuer(host);
        assertion.setIssueInstant(new DateTime());

        if (appilesTo != null) {
            Audience audience = (Audience) buildXMLObject(Audience.DEFAULT_ELEMENT_NAME);
            audience.setUri(appilesTo);
            AudienceRestrictionCondition audienceRestrictions =
                    (AudienceRestrictionCondition) buildXMLObject(AudienceRestrictionCondition.DEFAULT_ELEMENT_NAME);
            audienceRestrictions.getAudiences().add(audience);

            conditions.getAudienceRestrictionConditions().add(audienceRestrictions);
        }

        assertion.setConditions(conditions);

        assertion.getAttributeStatements().add(this.attributeStmt);
        assertion.setID(assertionId);

    }

    @Override
    public void setSignature(String signatureAlgorithm, X509Credential cred) throws IdentityProviderException {
        Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(cred);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        try {
            KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
            X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
            X509Certificate cert = (X509Certificate) buildXMLObject(X509Certificate.DEFAULT_ELEMENT_NAME);
            String value = Base64.encode(cred.getEntityCertificate().getEncoded());
            cert.setValue(value);
            data.getX509Certificates().add(cert);
            keyInfo.getX509Datas().add(data);
            signature.setKeyInfo(keyInfo);
        } catch (CertificateEncodingException e) {
            log.error("Error while getting the encoded certificate", e);
            throw new IdentityProviderException("Error while getting the encoded certificate");
        }

        assertion.setSignature(signature);
        signatureList.add(signature);
    }

    @Override
    public void marshellAndSign() throws IdentityProviderException {
        try {
            MarshallerFactory marshallerFactory = Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(assertion);
            signedAssertion = marshaller.marshall(assertion);

            Signer.signObjects(signatureList);

        } catch (MarshallingException e) {
            log.debug(e);
            throw new IdentityProviderException("errorMarshellingOrSigning", e);
        } catch (Exception e) {
            log.debug(e);
            throw new IdentityProviderException("errorMarshellingOrSigning", e);
        }
    }

    @Override
    public Element getSAMLasDOM() throws IdentityProviderException {
        return signedAssertion;
    }

}
