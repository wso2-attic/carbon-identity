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

import org.apache.rahas.RahasData;
import org.joda.time.DateTime;
import org.opensaml.xml.security.x509.X509Credential;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.provider.GenericIdentityProviderData;
import org.wso2.carbon.identity.provider.IdentityProviderException;

/**
 * The builder pattern. Builder interface. Concrete implementations build
 * SAMLAssertions of different types.
 */
public interface SAMLTokenBuilder {

    void createStatement(GenericIdentityProviderData ipData, RahasData rahasData) throws IdentityProviderException;

    void createSAMLAssertion(DateTime notAfter, DateTime notBefore, String assertionId)
            throws IdentityProviderException;

    void setSignature(String signatureAlgorithm, X509Credential cred) throws IdentityProviderException;

    void marshellAndSign() throws IdentityProviderException;

    Element getSAMLasDOM() throws IdentityProviderException;
}
