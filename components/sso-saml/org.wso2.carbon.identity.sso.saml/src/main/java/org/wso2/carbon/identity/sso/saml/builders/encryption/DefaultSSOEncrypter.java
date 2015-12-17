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
import org.opensaml.saml2.encryption.Encrypter;
import org.opensaml.xml.encryption.EncryptionConstants;
import org.opensaml.xml.encryption.EncryptionParameters;
import org.opensaml.xml.encryption.KeyEncryptionParameters;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.X509Credential;
import org.wso2.carbon.identity.base.IdentityException;

public class DefaultSSOEncrypter implements SSOEncrypter {
    @Override
    public void init() throws IdentityException {
        //Overridden method, no need to implement the body
    }

    @Override
    public EncryptedAssertion doEncryptedAssertion(Assertion assertion, X509Credential cred, String alias, String encryptionAlgorithm) throws IdentityException {
        try {

            Credential symmetricCredential = SecurityHelper.getSimpleCredential(
                    SecurityHelper.generateSymmetricKey(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256));

            EncryptionParameters encParams = new EncryptionParameters();
            encParams.setAlgorithm(EncryptionConstants.ALGO_ID_BLOCKCIPHER_AES256);
            encParams.setEncryptionCredential(symmetricCredential);

            KeyEncryptionParameters keyEncryptionParameters = new KeyEncryptionParameters();
            keyEncryptionParameters.setAlgorithm(EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSA15);
            keyEncryptionParameters.setEncryptionCredential(cred);

            Encrypter encrypter = new Encrypter(encParams, keyEncryptionParameters);
            encrypter.setKeyPlacement(Encrypter.KeyPlacement.INLINE);

            EncryptedAssertion encrypted = encrypter.encrypt(assertion);
            return encrypted;
        } catch (Exception e) {
            throw IdentityException.error("Error while Encrypting Assertion", e);
        }
    }
}
