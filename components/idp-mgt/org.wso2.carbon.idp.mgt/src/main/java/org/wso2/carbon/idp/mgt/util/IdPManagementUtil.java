/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.idp.mgt.util;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.RegistryResources;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementUtil;
import org.wso2.carbon.idp.mgt.internal.IdPManagementServiceComponent;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreException;

public class IdPManagementUtil {

    private static final Log log = LogFactory.getLog(IdPManagementUtil.class);

    /**
     * Get the tenant id of the given tenant domain.
     * 
     * @param tenantDomain Tenant Domain
     * @return Tenant Id of domain user belongs to.
     * @throws UserStoreException Error when getting tenant id from tenant domain
     */
    public static int getTenantIdOfDomain(String tenantDomain) throws UserStoreException {

        if (tenantDomain != null) {
            TenantManager tenantManager = IdPManagementServiceComponent.getRealmService()
                    .getTenantManager();
            int tenantId = tenantManager.getTenantId(tenantDomain);
            return tenantId;
        } else {
            log.debug("Invalid tenant domain: \'NULL\'");
            throw new IllegalArgumentException("Invalid tenant domain: \'NULL\'");
        }
    }

    /**
     * Import a certificate to Identity Provider Management trust store
     * 
     * @param alias Alias name of the Identity Provider's certificate
     * @param certData Encoded Base64 encoded certificate
     * @param tenantId Tenant Id
     * @param tenantDomain Tenant Domain
     * @throws IdentityProviderMgtException Error when trying to import certificate
     */
    public static void importCertToStore(String alias, String certData, int tenantId,
            String tenantDomain) throws IdentityApplicationManagementException {

        if (alias != null && certData != null) {
            try {
                KeyStoreManager keyMan = KeyStoreManager.getInstance(tenantId);
                String trustStoreName = null;
                KeyStore trustStore = null;
                if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
                    trustStore = keyMan.getPrimaryKeyStore();
                    trustStoreName = IdentityApplicationManagementUtil
                            .extractKeyStoreFileName(ServerConfiguration
                                    .getInstance()
                                    .getFirstProperty(
                                            RegistryResources.SecurityManagement.SERVER_PRIMARY_KEYSTORE_FILE));
                } else {
                    trustStoreName = tenantDomain.trim().replace(".", "-")
                            + "-idp-mgt-truststore.jks";
                    trustStore = keyMan.getKeyStore(trustStoreName);
                }
                byte[] bytes = Base64.decode(certData);
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                X509Certificate cert;
                cert = (X509Certificate) factory
                        .generateCertificate(new ByteArrayInputStream(bytes));
                if (trustStore.getCertificate(alias) != null) {
                    String msg = "Certificate with alias " + alias + " already exists for tenant "
                            + tenantDomain;
                    log.error(msg);
                    throw new IdentityApplicationManagementException(msg);
                }
                trustStore.setCertificateEntry(alias, cert);
                keyMan.updateKeyStore(trustStoreName, trustStore);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String msg = "Error occurred while importing Identity Provider public certificate "
                        + "for tenant " + tenantDomain;
                throw new IdentityApplicationManagementException(msg);
            }
        } else {
            String errorMsg = "Invalid arguments. " + "Alias: " + alias + ", Cert Data: "
                    + certData;
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Update a certificate in Identity Provider Management trust store
     * 
     * @param oldAlias Existing alias name of the Identity Provider's certificate
     * @param newAlias New alias name of the Identity Provider's certificate
     * @param certData Encoded Base64 encoded certificate
     * @param tenantId Tenant Id
     * @param tenantDomain Tenant Domain
     * @throws IdentityProviderMgtException Error when trying to update certificate
     */
    public static void updateCertToStore(String oldAlias, String newAlias, String certData,
            int tenantId, String tenantDomain) throws IdentityApplicationManagementException {

        if (oldAlias != null && newAlias != null && certData != null) {
            try {
                KeyStoreManager keyMan = KeyStoreManager.getInstance(tenantId);
                String trustStoreName = null;
                KeyStore trustStore = null;
                if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
                    trustStore = keyMan.getPrimaryKeyStore();
                    trustStoreName = IdentityApplicationManagementUtil
                            .extractKeyStoreFileName(ServerConfiguration
                                    .getInstance()
                                    .getFirstProperty(
                                            RegistryResources.SecurityManagement.SERVER_PRIMARY_KEYSTORE_FILE));
                } else {
                    trustStoreName = tenantDomain.trim().replace(".", "-")
                            + "-idp-mgt-truststore.jks";
                    trustStore = keyMan.getKeyStore(trustStoreName);
                }
                byte[] bytes = Base64.decode(certData);
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                X509Certificate cert;
                cert = (X509Certificate) factory
                        .generateCertificate(new ByteArrayInputStream(bytes));
                if (trustStore.getCertificate(oldAlias) != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Deleting existing certificate with alias " + oldAlias
                                + " for tenant " + tenantDomain);
                    }
                    trustStore.deleteEntry(oldAlias);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No certificates found with alias" + oldAlias + " for tenant "
                                + tenantDomain);
                    }
                }
                trustStore.setCertificateEntry(newAlias, cert);
                keyMan.updateKeyStore(trustStoreName, trustStore);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String msg = "Error occurred while updating Identity Provider public certificate "
                        + "for tenant " + tenantDomain;
                throw new IdentityApplicationManagementException(msg);
            }
        } else {
            String errorMsg = "Invalid arguments. " + "Old Alias: " + oldAlias + ", New Alias: "
                    + newAlias + ", Cert Data: " + certData;
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Delete a certificate in Identity Provider Management trust store
     * 
     * @param alias Alias name of the Identity Provider's certificate
     * @param tenantId Tenant Id
     * @param tenantDomain Tenant Domain
     * @throws IdentityProviderMgtException Error when trying to delete certificate
     */
    public static void deleteCertFromStore(String alias, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (alias != null) {
            try {
                KeyStoreManager keyMan = KeyStoreManager.getInstance(tenantId);
                String trustStoreName = null;
                KeyStore trustStore = null;
                if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
                    trustStore = keyMan.getPrimaryKeyStore();
                    trustStoreName = IdentityApplicationManagementUtil
                            .extractKeyStoreFileName(ServerConfiguration
                                    .getInstance()
                                    .getFirstProperty(
                                            RegistryResources.SecurityManagement.SERVER_PRIMARY_KEYSTORE_FILE));
                } else {
                    trustStoreName = tenantDomain.trim().replace(".", "-")
                            + "-idp-mgt-truststore.jks";
                    trustStore = keyMan.getKeyStore(trustStoreName);
                }
                if (trustStore.getCertificate(alias) == null) {
                    log.debug("Certificate with alias " + alias
                            + " does not exist in tenant key store " + trustStore);
                } else {
                    trustStore.deleteEntry(alias);
                    keyMan.updateKeyStore(trustStoreName, trustStore);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String msg = "Error occurred while deleting Identity Provider public certificate by alias "
                        + "for tenant " + tenantDomain;
                throw new IdentityApplicationManagementException(msg);
            }
        } else {
            String errorMsg = "Invalid alias: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Retrieve Identity Provider certificate from alias name
     * 
     * @param alias Certificate alias name
     * @param tenantId Tenant Id
     * @param tenantDomain Tenant Domain
     * @return Base64 encoded certificate
     * @throws IdentityProviderMgtException Error when retrieving certificate by alias
     */
    public static String getEncodedIdPCertFromAlias(String alias, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (alias != null) {
            KeyStoreManager keyMan = KeyStoreManager.getInstance(tenantId);
            try {
                String trustStoreName = null;
                KeyStore trustStore = null;
                if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
                    trustStore = keyMan.getPrimaryKeyStore();
                } else {
                    trustStoreName = tenantDomain.trim().replace(".", "-")
                            + "-idp-mgt-truststore.jks";
                    trustStore = keyMan.getKeyStore(trustStoreName);
                }
                Certificate cert = trustStore.getCertificate(alias);
                if (cert != null) {
                    return Base64.encode(cert.getEncoded());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String msg = "Error occurred while retrieving Identity Provider public certificate by alias "
                        + "for tenant " + tenantDomain;
                throw new IdentityApplicationManagementException(msg);
            }
        } else {
            String errorMsg = "Invalid alias: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return null;

    }

    /**
     * Retrieve Identity Provider certificate from thumb print
     * 
     * @param thumb Certificate thumb print
     * @param tenantId Tenant Id
     * @param tenantDomain Tenant Domain
     * @return Base64 encoded certificate
     * @throws IdentityProviderMgtException Error when retrieving certificate by thumb print
     */
    public static String getEncodedIdPCertFromThumb(String thumb, int tenantId, String tenantDomain)
            throws IdentityApplicationManagementException {

        if (thumb != null) {
            try {
                KeyStoreManager keyMan = KeyStoreManager.getInstance(tenantId);
                String trustStoreName = null;
                KeyStore trustStore = null;
                if (MultitenantConstants.SUPER_TENANT_ID == tenantId) {
                    trustStore = keyMan.getPrimaryKeyStore();
                } else {
                    trustStoreName = tenantDomain.trim().replace(".", "-")
                            + "-idp-mgt-truststore.jks";
                    trustStore = keyMan.getKeyStore(trustStoreName);
                }
                Certificate cert = null;
                MessageDigest sha = null;
                sha = MessageDigest.getInstance("SHA-1");
                for (Enumeration e = trustStore.aliases(); e.hasMoreElements();) {
                    String alias = (String) e.nextElement();
                    Certificate[] certs = trustStore.getCertificateChain(alias);
                    if (certs == null || certs.length == 0) {
                        // no cert chain, so lets check if getCertificate gives us a result.
                        cert = trustStore.getCertificate(alias);
                        if (cert == null) {
                            return null;
                        }
                    } else {
                        cert = certs[0];
                    }
                    if (!(cert instanceof X509Certificate)) {
                        continue;
                    }
                    sha.reset();
                    sha.update(cert.getEncoded());
                    byte[] data = sha.digest();
                    if (thumb.equals(IdentityApplicationManagementUtil.hexify(data))) {
                        return Base64.encode(cert.getEncoded());
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                String errorMsg = "Error occurred while getting Identity Provider public certificate by thumb print "
                        + "for tenant " + tenantDomain;
                throw new IdentityApplicationManagementException(errorMsg);
            }
        } else {
            String errorMsg = "Invalid thumb print: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        return null;
    }
}
