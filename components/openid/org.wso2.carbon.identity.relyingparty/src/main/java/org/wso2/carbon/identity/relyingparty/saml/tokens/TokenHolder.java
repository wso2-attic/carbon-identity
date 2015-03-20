/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.saml.tokens;

import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.Signature;
import org.w3c.dom.Element;

import java.util.Map;

public interface TokenHolder {

    /**
     * Creates the SAML object from the element This method must be called first
     *
     * @param elem
     * @throws UnmarshallingException If the token creation fails
     */
    public void createToken(Element elem) throws UnmarshallingException;

    /**
     * @return the SAML signature.
     */
    public Signature getSAMLSignature();

    /**
     * Populates the attributes.
     *
     * @param attributeTable
     */
    public void populateAttributeTable(Map<String, String> attributeTable);

    /**
     * Issuer of the SAML token
     *
     * @return
     */
    public String getIssuerName();

}
