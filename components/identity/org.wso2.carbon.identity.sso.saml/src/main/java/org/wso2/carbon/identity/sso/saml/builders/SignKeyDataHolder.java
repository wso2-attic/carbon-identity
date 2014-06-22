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
package org.wso2.carbon.identity.sso.saml.builders;

import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.CredentialContextSet;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.x509.X509Credential;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.security.keystore.KeyStoreAdmin;
import org.wso2.carbon.utils.AuthenticationObserver;
import org.wso2.carbon.utils.TenantUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.crypto.SecretKey;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

public class SignKeyDataHolder implements X509Credential {

    private String signatureAlgorithm = null;

    private static SignKeyDataHolder instance = null;

    private X509Certificate[] issuerCerts = null;

    private PrivateKey issuerPK = null;

    private void initializeRegistry(int tenantId) {
        BundleContext bundleContext = SAMLSSOUtil.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                    new ServiceTracker(bundleContext,
                            AuthenticationObserver.class.getName(), null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).startedAuthentication(tenantId);
                }
            }
            tracker.close();
        }
    }


    public SignKeyDataHolder(String username) throws IdentityException {
        String keyAlias = null;
        KeyStoreAdmin keyAdmin ;
        KeyStoreManager keyMan ;
        Certificate[] certificates ;
        int tenantID;
        String tenantDomain;
        String userTenantDomain;
        String spTenantDomain;

        try {

            userTenantDomain =  MultitenantUtils.getTenantDomain(username);
            spTenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            
            if (!SAMLSSOUtil.isSaaSApplication() && !spTenantDomain.equalsIgnoreCase(userTenantDomain)) {
                throw new IdentityException("Service Provider tenant domian must be equal to user tenant domain"
                        + " for non-SaaS applications");
            }
            
            String signWithValue = IdentityUtil.getProperty(
                    SAMLSSOConstants.FileBasedSPConfig.USE_AUTHENTICATED_USER_DOMAIN_CRYPTO);
            if (signWithValue != null  && "true".equalsIgnoreCase(signWithValue.trim())) {
                tenantDomain = userTenantDomain;
                tenantID = SAMLSSOUtil.getRealmService().getTenantManager().
                        getTenantId(tenantDomain);
            } else {
                tenantDomain = spTenantDomain;
                tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            }

            initializeRegistry(tenantID);
            
            if (tenantID != MultitenantConstants.SUPER_TENANT_ID) {
                String keyStoreName = SAMLSSOUtil.generateKSNameFromDomainName(tenantDomain);
                keyAlias = tenantDomain;
                keyMan = KeyStoreManager.getInstance(tenantID);
                KeyStore keyStore = keyMan.getKeyStore(keyStoreName);
                issuerPK = (PrivateKey) keyMan.getPrivateKey(keyStoreName, tenantDomain);
                certificates = keyStore.getCertificateChain(keyAlias);
                issuerCerts = new X509Certificate[certificates.length];

                int i = 0;
                for (Certificate certificate : certificates) {
                    issuerCerts[i++] = (X509Certificate) certificate;
                }

                signatureAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_RSA;

                String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
                if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
                    signatureAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_DSA;
                }

            } else {
                keyAlias = ServerConfiguration.getInstance().getFirstProperty(
                        "Security.KeyStore.KeyAlias");

                keyAdmin = new KeyStoreAdmin(tenantID,
                        SAMLSSOUtil.getRegistryService().getGovernanceSystemRegistry());
                keyMan = KeyStoreManager.getInstance(tenantID);

                issuerPK = (PrivateKey) keyAdmin.getPrivateKey(keyAlias, true);

                certificates = keyMan.getPrimaryKeyStore().getCertificateChain(keyAlias);

                issuerCerts = new X509Certificate[certificates.length];

                int i = 0;
                for (Certificate certificate : certificates) {
                    issuerCerts[i++] = (X509Certificate) certificate;
                }

                signatureAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_RSA;

                String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
                if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
                    signatureAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_DSA;
                }
            }

        } catch (Exception e) {
            throw new IdentityException(e.getMessage(), e);
        }

    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public Collection<X509CRL> getCRLs() {
        return null;
    }

    public X509Certificate getEntityCertificate() {
        return issuerCerts[0];
    }

    public Collection<X509Certificate> getEntityCertificateChain() {
        return Arrays.asList(issuerCerts);
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

    public PrivateKey getPrivateKey() {
        return issuerPK;
    }

    public PublicKey getPublicKey() {
        return issuerCerts[0].getPublicKey();
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

