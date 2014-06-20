/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authenticator.samlsso.manager;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.crypto.SecretKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialContextSet;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.x509.X509Credential;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.internal.SAMLSSOAuthenticatorServiceComponent;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.user.api.UserStoreException;

/**
 * X509Credential implementation for signing and verification.
 */
public class X509CredentialImpl implements X509Credential {

    private static Log log = LogFactory.getLog(X509CredentialImpl.class);

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;
    private X509Certificate entityCertificate = null;

    public X509CredentialImpl(String tenantDomain, String idpCert) throws SAMLSSOException {

        int tenantId = -1;
        
        try {
            tenantId = SAMLSSOAuthenticatorServiceComponent.getRealmService().getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            throw new SAMLSSOException(
                    "Exception occurred while retrieving Tenant ID from tenant domain " + tenantDomain, e);
        }
        
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantId);
        X509Certificate cert = null;
        PrivateKey privateKey = null;
        
        try {
            if(!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
                // derive key store name
                String ksName = tenantDomain.trim().replace(".", "-");
                // derive JKS name
                String jksName = ksName + ".jks";
                privateKey = (PrivateKey) keyStoreManager.getPrivateKey(jksName, tenantDomain);
            } else {
                privateKey = keyStoreManager.getDefaultPrivateKey();
            }
        } catch (Exception e) {
            throw new SAMLSSOException("Error retrieving private key for tenant : " + tenantDomain, e);
        }
        
        if(privateKey == null){
            throw new SAMLSSOException("Cannot find the primary private key for tenant " + tenantDomain);
        }
        
        this.privateKey = privateKey;
        
        if (idpCert != null && !idpCert.isEmpty()) {
            try {
                cert = (X509Certificate) IdentityApplicationManagementUtil.decodeCertificate(idpCert);
            } catch (CertificateException e) {
                throw new SAMLSSOException("Cannot find the certificate for alias: " + idpCert, e);
            }
            
            if(cert == null){
                throw new SAMLSSOException("Cannot find the certificate for alias: " + idpCert);
            }
            
            entityCertificate = cert;
            publicKey = cert.getPublicKey();
        }
    }

    /**
     * Retrieves the publicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public X509Certificate getEntityCertificate() {
        return entityCertificate;
    }

    // ********** Not implemented **************************************************************

    public Collection<X509CRL> getCRLs() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<X509Certificate> getEntityCertificateChain() {
        // TODO Auto-generated method stub
        return null;
    }

    public CredentialContextSet getCredentalContextSet() {
        // TODO Auto-generated method stub
        return null;
    }

    public Class<? extends Credential> getCredentialType() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getEntityId() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<String> getKeyNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public SecretKey getSecretKey() {
        // TODO Auto-generated method stub
        return null;
    }

    public UsageType getUsageType() {
        // TODO Auto-generated method stub
		return null;
	}
}
