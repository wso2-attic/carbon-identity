/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sso.saml.builders.encryption;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.xml.security.x509.X509Credential;
import org.wso2.carbon.identity.base.IdentityException;

/**
 * Interface to Encrypt SAML assertion
 */

public interface SSOEncrypter {

    public void init() throws IdentityException;

    /**
     * Encrypt the SAML assertion
     *
     * @param assertion           SAML assertion to be encrypted
     * @param cred                Encrypting credential
     * @param alias               Certificate alias against which use to Encrypt the assertion.
     * @param encryptionAlgorithm Encryption algorithm
     * @return SAML EncryptedAssertion
     * @throws IdentityException
     */
    public EncryptedAssertion doEncryptedAssertion(Assertion assertion, X509Credential cred, String alias,
                                                   String encryptionAlgorithm) throws IdentityException;
}
