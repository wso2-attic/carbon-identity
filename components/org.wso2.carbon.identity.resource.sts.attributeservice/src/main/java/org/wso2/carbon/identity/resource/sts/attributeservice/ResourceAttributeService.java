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
package org.wso2.carbon.identity.resource.sts.attributeservice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.impl.util.SAMLAttributeCallback;
import org.opensaml.Configuration;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLException;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.schema.impl.XSStringBuilder;
import org.wso2.carbon.identity.provider.IdentityAttributeService;

public class ResourceAttributeService implements IdentityAttributeService {

	private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
	private static final String WSSE_LN = "Security";
	private static final QName SEC_HEADER = new QName(WSSE_NS, WSSE_LN);

	private static final String ASSERTION_TOKEN_IN = "Assertion";
	private static final String ATTRIBUTE_STATMENT_IN = "AttributeStatement";
	private static final String ATTRIBUTE_IN = "Attribute";
	private static final String SUBJECT_IN = "Subject";
	private static final String NAMEID_IN = "NameID";

	private static final String ATTRIBUTE_NS = "AttributeNamespace";
	private static final String ATTRIBUTE_VALUE = "AttributeValue";

	public final static String SAML10_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
	public final static String SAML11_NS = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
	public final static String SAML20_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
	public final static String SAML20_NS_V2 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";

	private static Log log = LogFactory.getLog(ResourceAttributeService.class);

	/**
	 * {@inheritDoc}
	 */
	public void handle(SAMLAttributeCallback attrCallback) throws SAMLException {

		try {

			Map<String, String> orgAttributes = null;
			orgAttributes = getSAMLAssertions(MessageContext.getCurrentMessageContext()
					.getEnvelope());

			if (orgAttributes != null && orgAttributes.size() > 0) {
				String version = attrCallback.getData().getTokenType();
				for (Map.Entry<String, String> entry : orgAttributes.entrySet()) {
					if (!NAMEID_IN.equals(entry.getKey())) {
						if (SAML20_NS.equals(version) || SAML20_NS_V2.equals(version)) {
							attrCallback.addAttributes(getSAML2Attribute(entry.getKey(),
									entry.getValue(), entry.getKey()));
						} else {
							SAMLAttribute attribute = new SAMLAttribute(entry.getKey(),
									entry.getKey(), null, -1, Arrays.asList(new String[] { entry
											.getValue() }));
							attrCallback.addAttributes(attribute);
						}
					}
				}
				String subject = orgAttributes.get(NAMEID_IN);
				attrCallback.getData().setOverridenSubjectValue(subject);
			}

			
		} catch (Exception e) {
			log.error("Error occuerd while populating claim data", e);
		}

	}

	private Attribute getSAML2Attribute(String name, String value, String namespace) {
		XMLObjectBuilderFactory builderFactory = null;
		SAMLObjectBuilder<Attribute> attrBuilder = null;
		Attribute attribute = null;
		XSStringBuilder attributeValueBuilder = null;
		XSString stringValue = null;

		builderFactory = Configuration.getBuilderFactory();
		attrBuilder = (SAMLObjectBuilder<Attribute>) builderFactory
				.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
		attribute = attrBuilder.buildObject();
		attribute.setName(name);
		attribute.setNameFormat(namespace);

		attributeValueBuilder = (XSStringBuilder) builderFactory.getBuilder(XSString.TYPE_NAME);
		stringValue = attributeValueBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME,
				XSString.TYPE_NAME);
		stringValue.setValue(value);
		attribute.getAttributeValues().add(stringValue);
		return attribute;
	}

	private Map<String, String> getSAMLAssertions(SOAPEnvelope envelope) {
		SOAPHeaderBlock secHeader = null;
		OMElement assertionToken = null;

		secHeader = getSecHeader(envelope);
		if (secHeader == null) {
			return null;
		}

		String samlNameSpace = null;
		assertionToken = secHeader.getFirstChildWithName(new QName(SAML10_NS, ASSERTION_TOKEN_IN));
		if (assertionToken != null) {
			samlNameSpace = SAML10_NS;
		} else {
			assertionToken = secHeader.getFirstChildWithName(new QName(SAML11_NS,
					ASSERTION_TOKEN_IN));
			if (assertionToken != null) {
				samlNameSpace = SAML11_NS;
			} else {
				assertionToken = secHeader.getFirstChildWithName(new QName(SAML20_NS,
						ASSERTION_TOKEN_IN));
				if (assertionToken != null) {
					samlNameSpace = SAML20_NS;
				}
			}
		}

		if (samlNameSpace == null) {
			log.info("Unsupported SAML token type");
			return null;
		}

		if (assertionToken != null) {

			Map<String, String> attributeMap;
			attributeMap = new HashMap<String, String>();

			OMElement subjectElem = assertionToken.getFirstChildWithName(new QName(samlNameSpace,
					SUBJECT_IN));

			if (subjectElem != null) {
				OMElement nameId = subjectElem.getFirstChildWithName(new QName(samlNameSpace,
						NAMEID_IN));
				if (nameId != null) {
					attributeMap.put(NAMEID_IN, nameId.getText());
				}
			}

			OMElement attributeStatementElem = assertionToken.getFirstChildWithName(new QName(
					samlNameSpace, ATTRIBUTE_STATMENT_IN));
			if (attributeStatementElem != null) {
				Iterator<OMElement> attributes = null;
				attributes = attributeStatementElem.getChildrenWithName(new QName(samlNameSpace,
						ATTRIBUTE_IN));
				if (attributes != null) {
					while (attributes.hasNext()) {
						OMElement attr = attributes.next();
						OMElement attrValElement = null;
						String attributeName = null;
						String attributeValue = null;
						attributeName = attr.getAttributeValue(new QName(ATTRIBUTE_NS));
						attrValElement = attr.getFirstChildWithName(new QName(samlNameSpace,
								ATTRIBUTE_VALUE));
						attributeValue = attrValElement.getText();
						attributeMap.put(attributeName, attributeValue);
					}
					return attributeMap;
				}
			}
		}
		return null;
	}

	private SOAPHeaderBlock getSecHeader(SOAPEnvelope envelope) {
		SOAPHeader header = envelope.getHeader();
		if (header != null) {
			return (SOAPHeaderBlock) header.getFirstChildWithName(SEC_HEADER);
		}
		return null;
	}
}
