/*
 * Copyright (c) 2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.passive.sts.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authenticator.passive.sts.exception.PassiveSTSException;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class STSAgentKeyStoreCredential implements STSAgentCredential {

    private static Log log = LogFactory.getLog(STSAgentKeyStoreCredential.class);

    private static PublicKey publicKey = null;
    private static PrivateKey privateKey = null;
    private static X509Certificate entityCertificate = null;

    private static void readX509Credentials(ExternalIdPConfig externalIdPConfig) throws PassiveSTSException {
        externalIdPConfig.getIdPName();
        IdentityProvider identityProvider = externalIdPConfig.getIdentityProvider();
        X509Certificate x509Certificate = null;
        try {
            x509Certificate = (X509Certificate) IdentityApplicationManagementUtil
                    .decodeCertificate(identityProvider.getCertificate());
            entityCertificate = x509Certificate;
            publicKey = x509Certificate.getPublicKey();
        } catch (CertificateException e) {
            log.error(e.getMessage(), e);
            throw new PassiveSTSException("Error occurred while decoding public certificate of Identity Provider "
                    + identityProvider.getIdentityProviderName());
        }

    }

    @Override
    public void init(ExternalIdPConfig externalIdPConfig) throws PassiveSTSException {
        readX509Credentials(externalIdPConfig);
    }

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public X509Certificate getEntityCertificate() {
        return entityCertificate;
    }
}
