/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.relyingparty.saml.tokens;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.signature.Signature;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SAML2TokenHolder implements TokenHolder {

    private static final Log log = LogFactory.getLog(SAML2TokenHolder.class);
    private Assertion assertion = null;

    public SAML2TokenHolder(Element element) throws UnmarshallingException {
        createToken(element);
    }

    /**
     * Creates the SAML object from the element This method must be called first
     *
     * @param elem
     * @throws UnmarshallingException If the token creation fails
     */
    @Override
    public void createToken(Element elem) throws UnmarshallingException {
        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(elem);
        assertion = (Assertion) unmarshaller.unmarshall(elem);
    }

    /**
     * @return the SAML signature.
     */
    @Override
    public Signature getSAMLSignature() {
        return assertion.getSignature();
    }

    /**
     * Issuer of the SAML token
     *
     * @return
     */
    @Override
    public String getIssuerName() {
        return assertion.getIssuer().getValue();
    }

    /**
     * Populates the attributes.
     *
     * @param attributeTable
     */
    @Override
    public void populateAttributeTable(Map<String, String> attributeTable) {
        Iterator<AttributeStatement> statements = assertion.getAttributeStatements().iterator();

        while (statements.hasNext()) {
            AttributeStatement statement = statements.next();
            Iterator<Attribute> attrs = statement.getAttributes().iterator();

            while (attrs.hasNext()) {
                Attribute attr = attrs.next();
                String attrNamesapce = attr.getNameFormat();
                String attrName = attr.getName();
                String name = attrNamesapce + "/" + attrName;

                List attributeValues = attr.getAttributeValues();
                Iterator values = attributeValues.iterator();
                StringBuilder buff = new StringBuilder();

                while (values.hasNext()) {
                    Object value = values.next();
                    if (value instanceof XSString) {
                        buff.append(((XSString) value).getValue());
                    } else if (value instanceof XSAny) {
                        buff.append(((XSAny) value).getTextContent());
                    }
                    buff.append(",");
                }

                if (buff.length() > 1) {
                    buff.deleteCharAt(buff.length() - 1);
                }

                attributeTable.put(name, buff.toString());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Attribute table populated for SAML 2 Token");
        }
    }

}