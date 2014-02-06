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
package org.wso2.carbon.identity.samples.saml.mediator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SAMLAttrMediator extends AbstractMediator {

    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSSE_LN = "Security";
    private static final String ASSERTION_TOKEN_IN = "Assertion";
    private static final String ATTRIBUTE_STATMENT_IN = "AttributeStatement";
    private static final String ATTRIBUTE_IN = "Attribute";

    private static final String ATTRIBUTE_NS = "AttributeNamespace";
    private static final String ATTRIBUTE_VALUE = "AttributeValue";

    private static final QName SEC_HEADER = new QName(WSSE_NS, WSSE_LN);
    public final static String SAML10_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
    public final static String SAML11_NS = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    public final static String SAML20_NS = "urn:oasis:names:tc:SAML:2.0:assertion";

    private static final Log log = LogFactory.getLog(SAMLAttrMediator.class);

    private String injectorClassName;

    public String getInjectorClassName() {
        return injectorClassName;
    }

    public void setInjectorClassName(String injectorClassName) {
        this.injectorClassName = injectorClassName;
    }

    /**
     * {@inheritDoc}
     */
    public boolean mediate(MessageContext synCtx) {
        log.info("Mediation SAMLAttrMediator started");

        Map<String, String> attributes = null;
        attributes = getSAMLAssertions(synCtx.getEnvelope());

        if (attributes != null) {
            SAMLAttrCallbackHandler callback = getCallbackHandler();
            if (callback != null) {
                callback.injectCustomAttributes(attributes, synCtx.getEnvelope());
                log.info("Added custom SAML attributes");
            } else {
                log.info("No callback handler being set");
            }
        } else {
            log.info("Custom SAML attributes NOT added");
        }

        return true;
    }

    private SAMLAttrCallbackHandler getCallbackHandler() {
        if (injectorClassName == null || injectorClassName.trim().length() == 0) {
            return null;
        }

        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(
                    injectorClassName);
            return (SAMLAttrCallbackHandler) clazz.newInstance();
        } catch (Exception e) {
            log.error("Error occured while loading " + injectorClassName, e);
        }
        return null;
    }

    /**
     * 
     * @param envelope
     * @return
     */
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
            OMElement attributeStatementElem = assertionToken.getFirstChildWithName(new QName(
                    samlNameSpace, ATTRIBUTE_STATMENT_IN));
            if (attributeStatementElem != null) {
                Iterator<OMElement> attributes = null;
                attributes = attributeStatementElem.getChildrenWithName(new QName(samlNameSpace,
                        ATTRIBUTE_IN));
                if (attributes != null) {
                    Map<String, String> attributeMap;
                    attributeMap = new HashMap<String, String>();
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

    /**
     * 
     * @param envelope
     * @return
     */
    private SOAPHeaderBlock getSecHeader(SOAPEnvelope envelope) {
        SOAPHeader header = envelope.getHeader();
        if (header != null) {
            return (SOAPHeaderBlock) header.getFirstChildWithName(SEC_HEADER);
        }
        return null;
    }
}
