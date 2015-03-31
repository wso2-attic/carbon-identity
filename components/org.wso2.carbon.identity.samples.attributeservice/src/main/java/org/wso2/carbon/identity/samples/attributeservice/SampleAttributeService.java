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
package org.wso2.carbon.identity.samples.attributeservice;

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasData;
import org.apache.rahas.impl.util.SAMLAttributeCallback;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLException;
import org.wso2.carbon.identity.provider.IdentityAttributeService;

public class SampleAttributeService implements IdentityAttributeService {

    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String WSSE_LN = "Security";
    private static final String USERNAME_TOKEN_LN = "UsernameToken";
    private static final String PASSWORD_LN = "Password";
    private static final QName SEC_HEADER = new QName(WSSE_NS, WSSE_LN);
    private static final QName USERNAME_TOKEN = new QName(WSSE_NS, USERNAME_TOKEN_LN);
    private static final QName PASSWORD = new QName(WSSE_NS, PASSWORD_LN);

    private static Log log = LogFactory.getLog(SampleAttributeService.class);

    public void handle(SAMLAttributeCallback attrCallback) throws SAMLException {
        log.info("SampleAttributeService being called");
        RahasData data = null;
        String userIdentifier = null;
        String password = null;

        try {
            data = attrCallback.getData();
            userIdentifier = data.getPrincipal().getName();
            password = getUserCredentials(MessageContext.getCurrentMessageContext().getEnvelope());
            log.info("User Identifier : " + userIdentifier);
            log.info("User Password : " + password);

            // Retrieving all attribute values set before.
            if (attrCallback.getAttributes() != null || attrCallback.getAttributes().length > 0) {
                SAMLAttribute[] attributes = attrCallback.getAttributes();
                for (int i = 0; i < attributes.length; i++) {
                    log.info(attributes[i].toString());
                    // Overriding and filtering.
                    if ("First Name".equals(attributes[i].getName())) {
                        attributes[i].setValues(Arrays.asList(new String[]{"New Name"}));
                    }
                }
            }

            // Adding a new attribute
            SAMLAttribute attribute = null;
            attribute = new SAMLAttribute("Custom Attribute",
                    "http://wso2.org/claims/customAttribute", null, -1, Arrays
                            .asList(new String[]{"Sample Value"}));
            attrCallback.addAttributes(attribute);

        } catch (Exception e) {
            log.error("Error occuerd while populating claim data", e);
        }

    }

    private String getUserCredentials(SOAPEnvelope envelope) {
        SOAPHeaderBlock secHeader = null;
        OMElement usernameToken = null;

        secHeader = getSecHeader(envelope);
        if (secHeader == null) {
            return null;
        }

        usernameToken = secHeader.getFirstChildWithName(USERNAME_TOKEN);
        if (usernameToken != null) {
            OMElement userElem = usernameToken.getFirstChildWithName(PASSWORD);
            if (userElem != null) {
                return userElem.getText().trim();
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
