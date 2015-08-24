/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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
package org.wso2.carbon.identity.application.common.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.Base64;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.claim.mgt.ClaimManagerHandler;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.CertData;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.ThreadLocalProvisioningServiceProvider;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

public class IdentityApplicationManagementUtil {

    private IdentityApplicationManagementUtil(){
    }

    private static final Log log = LogFactory.getLog(IdentityApplicationManagementUtil.class);
    private static ThreadLocal<ThreadLocalProvisioningServiceProvider> threadLocalProvisioningServiceProvider = new ThreadLocal<ThreadLocalProvisioningServiceProvider>();

    /**
     *
     */
    public static void resetThreadLocalProvisioningServiceProvider() {
        threadLocalProvisioningServiceProvider.remove();
    }

    /**
     * @param serviceProvider
     */
    public static ThreadLocalProvisioningServiceProvider getThreadLocalProvisioningServiceProvider() {
        return threadLocalProvisioningServiceProvider.get();
    }

    /**
     * @param serviceProviderName In-bound - or Just-in-Time provisioning service provider.
     */
    public static void setThreadLocalProvisioningServiceProvider(
            ThreadLocalProvisioningServiceProvider serviceProvider) {
        threadLocalProvisioningServiceProvider.set(serviceProvider);
    }

    /**
     * Validates an URI.
     *
     * @param uriString URI String
     * @return <code>true</code> if valid URI, <code>false</code> otherwise
     */
    public static boolean validateURI(String uriString) {

        if (uriString != null) {
            try {
                URL url = new URL(uriString);
            } catch (MalformedURLException e) {
                log.debug(e.getMessage(), e);
                return false;
            }
        } else {
            String errorMsg = "Invalid URL: \'NULL\'";
            log.debug(errorMsg);
            return false;
        }
        return true;
    }

    /**
     * Utility method to close a database connection
     *
     * @param dbConnection Database <code>Connection</code> object
     */
    public static void closeConnection(Connection dbConnection) {

        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                log.error("Database error. Could not close connection - " + e.getMessage(), e);
            }
        } else {
            String errorMsg = "Invalid Connection: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Utility method to rollback a database connection
     *
     * @param dbConnection Database <code>Connection</code> object
     */
    public static void rollBack(Connection dbConnection) {

        if (dbConnection != null) {
            try {
                dbConnection.rollback();
            } catch (SQLException e) {
                log.error("Database error. Could not rollback transaction  - " + e.getMessage(), e);
            }
        } else {
            String errorMsg = "Invalid Connection: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    public static ProvisioningConnectorConfig[] concatArrays(ProvisioningConnectorConfig[] o1,
                                                             ProvisioningConnectorConfig[] o2) {
        ProvisioningConnectorConfig[] ret = new ProvisioningConnectorConfig[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    public static Property[] concatArrays(Property[] o1, Property[] o2) {

        Set<Property> properties = new HashSet<Property>(Arrays.asList(o1));
        properties.addAll(Arrays.asList(o2));
        return properties.toArray(new Property[properties.size()]);
    }

    /**
     * @param o1
     * @param o2
     * @return
     */
    public static FederatedAuthenticatorConfig[] concatArrays(FederatedAuthenticatorConfig[] o1,
                                                              FederatedAuthenticatorConfig[] o2) {
        FederatedAuthenticatorConfig[] ret = new FederatedAuthenticatorConfig[o1.length + o2.length];

        System.arraycopy(o1, 0, ret, 0, o1.length);
        System.arraycopy(o2, 0, ret, o1.length, o2.length);

        return ret;
    }

    /**
     * Extract key store filename
     *
     * @param filePath File path of a key store
     * @return Key store file name
     */
    public static String extractKeyStoreFileName(String filePath) {

        if (filePath != null) {
            String name = null;
            int index = filePath.lastIndexOf('/');
            if (index != -1) {
                name = filePath.substring(index + 1);
            } else {
                index = filePath.lastIndexOf(File.separatorChar);
                if (index != -1) {
                    name = filePath.substring(filePath.lastIndexOf(File.separatorChar));
                } else {
                    name = filePath;
                }
            }
            return name;
        } else {
            String errorMsg = "Invalid file path: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Generate thumbprint of certificate
     *
     * @param encodedCert Base64 encoded certificate
     * @return Certificate thumbprint
     * @throws java.security.NoSuchAlgorithmException Unsupported hash algorithm
     */
    public static String generateThumbPrint(String encodedCert) throws NoSuchAlgorithmException {

        if (encodedCert != null) {
            MessageDigest digestValue = null;
            digestValue = MessageDigest.getInstance("SHA-1");
            byte[] der = Base64.decode(encodedCert);
            digestValue.update(der);
            byte[] digestInBytes = digestValue.digest();
            String publicCertThumbprint = hexify(digestInBytes);
            return publicCertThumbprint;
        } else {
            String errorMsg = "Invalid encoded certificate: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Generate thumbprint of certificate
     *
     * @param encodedCert Base64 encoded certificate
     * @return Decoded <code>Certificate</code>
     * @throws java.security.cert.CertificateException Error when decoding certificate
     */
    public static Certificate decodeCertificate(String encodedCert) throws CertificateException {

        if (encodedCert != null) {
            byte[] bytes = Base64.decode(encodedCert);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory
                    .generateCertificate(new ByteArrayInputStream(bytes));
            return cert;
        } else {
            String errorMsg = "Invalid encoded certificate: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * >>>>>>> .r201641 Helper method to hexify a byte array. TODO:need to verify the logic
     *
     * @param bytes
     * @return hexadecimal representation
     */
    public static String hexify(byte bytes[]) {

        if (bytes != null) {
            char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c',
                    'd', 'e', 'f'};
            StringBuilder buf = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; ++i) {
                buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
                buf.append(hexDigits[bytes[i] & 0x0f]);
            }
            return buf.toString();
        } else {
            String errorMsg = "Invalid byte array: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * @param encodedCert
     * @return
     * @throws CertificateException
     */
    public static CertData getCertData(String encodedCert) throws CertificateException {

        if (encodedCert != null) {
            byte[] bytes = Base64.decode(encodedCert);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) factory
                    .generateCertificate(new ByteArrayInputStream(bytes));
            Format formatter = new SimpleDateFormat("dd/MM/yyyy");
            return fillCertData(cert, formatter);
        } else {
            String errorMsg = "Invalid encoded certificate: \'NULL\'";
            log.debug(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * @param array
     * @return
     */
    public static boolean exclusiveOR(boolean[] array) {
        boolean foundTrue = false;
        for (boolean temp : array) {
            if (temp) {
                if (foundTrue) {
                    return false;
                } else {
                    foundTrue = true;
                }
            }
        }
        return foundTrue;
    }

    /**
     * @param cert
     * @param formatter
     * @return
     * @throws CertificateEncodingException
     */
    private static CertData fillCertData(X509Certificate cert, Format formatter)
            throws CertificateEncodingException {

        CertData certData = new CertData();
        certData.setSubjectDN(cert.getSubjectDN().getName());
        certData.setIssuerDN(cert.getIssuerDN().getName());
        certData.setSerialNumber(cert.getSerialNumber());
        certData.setVersion(cert.getVersion());
        certData.setNotAfter(formatter.format(cert.getNotAfter()));
        certData.setNotBefore(formatter.format(cert.getNotBefore()));
        certData.setPublicKey(Base64.encode(cert.getPublicKey().getEncoded()));
        return certData;
    }

    /**
     * @return
     * @throws IdentityException
     */
    public static Connection getDBConnection() throws IdentityException {
        return JDBCPersistenceManager.getInstance().getDBConnection();
    }

    /**
     * @param rs
     */
    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Database error. Could not close result set  - " + e.getMessage(), e);
            }
        }

    }

    public static void closeStatement(PreparedStatement preparedStatement) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                log.error("Database error. Could not close statement. Continuing with others - "
                        + e.getMessage(), e);
            }
        }
    }

    /**
     * @param outboundClaimDialect
     * @param inboundClaimValueMap
     * @param inboundClaimMapping
     * @param outboundClaimValueMapping
     * @param tenantDomainName
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static Map<ClaimMapping, List<String>> getMappedClaims(String outboundClaimDialect,
                                                                  Map<String, String> inboundClaimValueMap, ClaimMapping[] inboundClaimMappings,
                                                                  Map<ClaimMapping, List<String>> outboundClaimValueMappings, String tenantDomain)
            throws IdentityApplicationManagementException {

        try {

            // we do have in-bound claim mapping - but no out-bound claim mapping - no out-bound
            // default values.since we do not know the out-bound claim mapping - whatever in the
            // in-bound claims will be mapped into the out-bound claim dialect.

            if (MapUtils.isEmpty(inboundClaimValueMap)) {
                // we do not have out-bound claim mapping - and a default values to worry about.
                // just return what we got.
                return outboundClaimValueMappings;
            }

            Map<String, String> claimMap = null;

            // out-bound is not in wso2 carbon dialect. we need to find how it maps to wso2
            // carbon dialect.
            Map<String, String> outBoundToCarbonClaimMapppings = null;

            // we only know the dialect - it is a standard claim dialect.
            // this returns back a map - having carbon claim dialect as the key.
            // null argument is passed - because we do not know the required attributes for
            // out-bound provisioning. This will find carbon claim mappings for the entire out-bound
            // claim dialect.
            outBoundToCarbonClaimMapppings = ClaimManagerHandler.getInstance()
                    .getMappingsMapFromOtherDialectToCarbon(outboundClaimDialect, null,
                            tenantDomain, true);

            if (outBoundToCarbonClaimMapppings == null) {
                // we did not find any carbon claim mappings corresponding to the out-bound claim
                // dialect - we cannot map the in-bound claim dialect to out-bound claim dialect.
                // just return what we got.
                return outboundClaimValueMappings;
            }

            // {in-bound-claim-uri / out-bound-claim-uri
            claimMap = new HashMap<String, String>();

            for (ClaimMapping inboundClaimMapping : inboundClaimMappings) {
                // there can be a claim mapping without a mapped local claim.
                // if that is the case - we cannot map it to an out-bound claim.
                if (inboundClaimMapping.getLocalClaim() == null
                        || inboundClaimMapping.getLocalClaim().getClaimUri() == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Inbound claim - local claim is null");
                    }
                    continue;
                }

                // get the out-bound claim corresponding to the carbon dialect - which is the key.
                String outboundClaim = outBoundToCarbonClaimMapppings.get(inboundClaimMapping
                        .getLocalClaim().getClaimUri());

                if (outboundClaim != null) {
                    // in-bound claim uri / out-bound claim uri.
                    if (inboundClaimMapping.getRemoteClaim() != null
                            && inboundClaimMapping.getRemoteClaim().getClaimUri() != null) {
                        claimMap.put(inboundClaimMapping.getRemoteClaim().getClaimUri(),
                                outboundClaim);
                    }
                }
            }

            if (claimMap.isEmpty()) {
                // we do not have a claim map.
                // return what we got.
                return outboundClaimValueMappings;
            }

            for (Iterator<Entry<String, String>> iterator = claimMap.entrySet().iterator(); iterator
                    .hasNext(); ) {
                Entry<String, String> entry = iterator.next();

                String inboundClaimUri = entry.getKey();
                String outboundClaimUri = entry.getValue();
                String claimValue = null;

                if (outboundClaimUri != null) {
                    claimValue = inboundClaimValueMap.get(inboundClaimUri);
                }
                // null value goes there because we do not have an out-bound claim mapping - and
                // also default values.
                if (claimValue != null) {
                    outboundClaimValueMappings.put(
                            ClaimMapping.build(inboundClaimUri, outboundClaimUri, null, false),
                            Arrays.asList(new String[]{claimValue}));
                }
            }

        } catch (Exception e) {
            throw new IdentityApplicationManagementException("Error while loading claim mappings.",
                    e);
        }

        return outboundClaimValueMappings;
    }

    /**
     * @param outboundClaimDialect
     * @param inboundClaimValueMap
     * @param inboundClaimMappingDialect
     * @param outboundClaimValueMapping
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static Map<ClaimMapping, List<String>> getMappedClaims(String outboundClaimDialect,
                                                                  Map<String, String> inboundClaimValueMap, String inboundClaimMappingDialect,
                                                                  Map<ClaimMapping, List<String>> outboundClaimValueMappings, String tenantDomain)
            throws IdentityApplicationManagementException {

        // we have in-bound claim dialect and out-bound claim dialect. we do not have an in-bound
        // claim mapping or an out-bound claim mapping.

        try {

            if (MapUtils.isEmpty(inboundClaimValueMap)) {
                return outboundClaimValueMappings;
            }

            Map<String, String> claimMap = null;

            if (IdentityApplicationConstants.WSO2CARBON_CLAIM_DIALECT
                    .equals(inboundClaimMappingDialect)) {
                // in-bound dialect is in default carbon dialect.
                // otherDialectURI, carbonClaimURIs, tenantDomain, carbonDialectAsKey
                // this map will have out-bound dialect as the key.
                claimMap = ClaimManagerHandler.getInstance()
                        .getMappingsMapFromOtherDialectToCarbon(outboundClaimDialect, null,
                                tenantDomain, true);
            } else {
                // out-bound is not in wso2 carbon dialect. we need to find how it maps to wso2
                // carbon dialect.
                Map<String, String> inboundToCarbonClaimMaping = null;
                Map<String, String> outBoundToCarbonClaimMappping = null;

                // this will return back the mapped carbon dialect for the in-bound claims in the
                // in-bound provisioning request.
                // the key of this map will be in in-bound claim dialect.
                inboundToCarbonClaimMaping = ClaimManagerHandler.getInstance()
                        .getMappingsMapFromOtherDialectToCarbon(inboundClaimMappingDialect,
                                inboundClaimValueMap.keySet(), tenantDomain, false);

                // we only know the dialect - it is standard claim dialect.
                // this will return back all the wso2 carbon claims mapped to the out-bound dialect.
                // we send null here because we do not know the required claims for out-bound
                // provisioning.
                // the key of this map will be in carbon dialect.
                outBoundToCarbonClaimMappping = ClaimManagerHandler.getInstance()
                        .getMappingsMapFromOtherDialectToCarbon(outboundClaimDialect, null,
                                tenantDomain, true);

                // in-bound dialect / out-bound dialect.
                claimMap = new HashMap<String, String>();

                for (Iterator<Entry<String, String>> iterator = inboundToCarbonClaimMaping
                        .entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<String, String> entry = iterator.next();
                    String outboundClaim = outBoundToCarbonClaimMappping.get(entry.getValue());
                    if (outboundClaim != null) {
                        claimMap.put(entry.getKey(), outboundClaim);
                    }
                }
            }

            if (claimMap.isEmpty()) {
                return outboundClaimValueMappings;
            }

            // when we do not defined the claim mapping for out-bound provisioning we iterate
            // through the in-bound provisioning claim map.
            for (Iterator<Entry<String, String>> iterator = claimMap.entrySet().iterator(); iterator
                    .hasNext(); ) {
                Entry<String, String> entry = iterator.next();
                String outboundClaimUri = entry.getValue();
                String inboundClaimUri = entry.getKey();

                String claimValue = null;

                if (outboundClaimUri != null) {
                    claimValue = inboundClaimValueMap.get(inboundClaimUri);
                }

                if (claimValue != null) {
                    outboundClaimValueMappings.put(
                            ClaimMapping.build(inboundClaimUri, outboundClaimUri, null, false),
                            Arrays.asList(new String[]{claimValue}));
                }
            }

        } catch (Exception e) {
            throw new IdentityApplicationManagementException("Error while loading claim mappings.",
                    e);
        }

        return outboundClaimValueMappings;
    }

    /**
     * @param outboundClaimMapping
     * @param inboundClaimValueMap
     * @param inboundClaimMapping
     * @param outboundClaimValueMapping
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static Map<ClaimMapping, List<String>> getMappedClaims(
            ClaimMapping[] outboundClaimMappings, Map<String, String> inboundClaimValueMap,
            ClaimMapping[] inboundClaimMappings,
            Map<ClaimMapping, List<String>> outboundClaimValueMappings)
            throws IdentityApplicationManagementException {

        try {

            // we have in-bound claim mapping and out-bound claim mapping.

            if (outboundClaimValueMappings == null) {
                outboundClaimValueMappings = new HashMap<ClaimMapping, List<String>>();
            }

            if (MapUtils.isEmpty(inboundClaimValueMap)) {
                // we do not have any values in the incoming provisioning request.
                // we need to populate outboundClaimValueMappings map with the default values from
                // the out-bound claim mapping.
                if (outboundClaimMappings != null && outboundClaimMappings.length > 0) {
                    for (ClaimMapping mapping : outboundClaimMappings) {
                        if (mapping.getDefaultValue() != null) {
                            outboundClaimValueMappings.put(mapping,
                                    Arrays.asList(new String[]{mapping.getDefaultValue()}));
                        }
                    }
                }

                return outboundClaimValueMappings;
            }

            if (outboundClaimMappings == null || outboundClaimMappings.length == 0) {
                // we cannot find out-bound claim dialect - return what we have.
                return outboundClaimValueMappings;
            }

            Map<String, String> claimMap = null;

            // out-bound is not in wso2 carbon dialect. we need to find how it maps to wso2
            // carbon dialect.
            Map<String, String> inboundToCarbonClaimMaping = new HashMap<String, String>();
            Map<String, String> outBoundToCarbonClaimMappping = new HashMap<String, String>();

            Map<String, String> outboundClaimDefaultValues = new HashMap<String, String>();

            for (ClaimMapping inboundClaimMapping : inboundClaimMappings) {
                // populate map with in-bound claims.
                if (inboundClaimMapping.getLocalClaim() != null) {
                    inboundToCarbonClaimMaping.put(inboundClaimMapping.getLocalClaim()
                            .getClaimUri(), inboundClaimMapping.getRemoteClaim().getClaimUri());
                } else {
                    // ignore. if you do not have a local claim we cannot map it.
                }
            }

            for (ClaimMapping outboundClaimMapping : outboundClaimMappings) {
                // populate a map with the out-bound claims.
                // use remote claim uri as the key.
                if (outboundClaimMapping.getLocalClaim() != null) {
                    outBoundToCarbonClaimMappping.put(outboundClaimMapping.getRemoteClaim()
                            .getClaimUri(), outboundClaimMapping.getLocalClaim().getClaimUri());
                } else {
                    outBoundToCarbonClaimMappping.put(outboundClaimMapping.getRemoteClaim()
                            .getClaimUri(), null);
                }

                outboundClaimDefaultValues.put(outboundClaimMapping.getRemoteClaim().getClaimUri(),
                        outboundClaimMapping.getDefaultValue());
            }

            claimMap = new HashMap<String, String>();

            // we need to have everything in the out-bound claim dialect in the claimMap.
            for (Iterator<Entry<String, String>> iterator = outBoundToCarbonClaimMappping
                    .entrySet().iterator(); iterator.hasNext(); ) {
                Entry<String, String> entry = iterator.next();

                String localClaimUri = entry.getValue();
                String outboundClaimUri = entry.getKey();

                String inboundClaim = inboundToCarbonClaimMaping.get(localClaimUri);
                claimMap.put(outboundClaimUri, inboundClaim);
            }

            if (claimMap.isEmpty()) {
                return outboundClaimValueMappings;
            }

            for (Iterator<Entry<String, String>> iterator = claimMap.entrySet().iterator(); iterator
                    .hasNext(); ) {

                Entry<String, String> entry = iterator.next();
                String outboundClaimUri = entry.getKey();
                String inboundClaimUri = entry.getValue();

                if (inboundClaimUri != null && inboundClaimValueMap.get(inboundClaimUri) != null) {
                    outboundClaimValueMappings.put(ClaimMapping.build(inboundClaimUri,
                            outboundClaimUri, outboundClaimDefaultValues.get(outboundClaimUri),
                            false), Arrays.asList(new String[]{inboundClaimValueMap
                            .get(inboundClaimUri)}));
                } else {
                    outboundClaimValueMappings.put(ClaimMapping.build(inboundClaimUri,
                            outboundClaimUri, outboundClaimDefaultValues.get(outboundClaimUri),
                            false), Arrays.asList(new String[]{outboundClaimDefaultValues
                            .get(outboundClaimUri)}));
                }
            }

        } catch (Exception e) {
            throw new IdentityApplicationManagementException("Error while loading claim mappings.",
                    e);
        }

        return outboundClaimValueMappings;
    }

    /**
     * @param outboundClaimMapping
     * @param inboundClaimValueMap
     * @param inboundClaimMappingDialect
     * @param outboundClaimValueMapping
     * @return
     * @throws IdentityApplicationManagementException
     */
    public static Map<ClaimMapping, List<String>> getMappedClaims(
            ClaimMapping[] outboundClaimMappings, Map<String, String> inboundClaimValueMap,
            String inboundClaimMappingDialect,
            Map<ClaimMapping, List<String>> outboundClaimValueMappings, String tenantDomain)
            throws IdentityApplicationManagementException {

        // we know the out-bound claim mapping - and the in-bound claim dialect.

        try {

            if (MapUtils.isEmpty(inboundClaimValueMap)) {
                // we do not have any values in the incoming provisioning request.
                // we need to populate outboundClaimValueMappings map with the default values from
                // the out-bound claim mapping.
                if (outboundClaimMappings != null && outboundClaimMappings.length > 0) {
                    for (ClaimMapping mapping : outboundClaimMappings) {
                        if (mapping.getDefaultValue() != null) {
                            outboundClaimValueMappings.put(mapping,
                                    Arrays.asList(new String[]{mapping.getDefaultValue()}));
                        }
                    }
                }

                return outboundClaimValueMappings;
            }

            if (outboundClaimMappings == null || outboundClaimMappings.length == 0) {
                // we cannot find out-bound claim dialect - return what we have.
                return outboundClaimValueMappings;
            }

            Map<String, String> claimMap = null;

            // out-bound is not in wso2 carbon dialect. we need to find how it maps to wso2
            // carbon dialect.
            Map<String, String> carbonToInboundClaimMapping = null;

            // we only know the dialect - it is standard claim dialect.
            // returns the carbon claim mapping corresponding to claims in the the in-bound
            // provisioning request with carbon in-bound claim uris as the key.
            carbonToInboundClaimMapping = ClaimManagerHandler.getInstance()
                    .getMappingsMapFromOtherDialectToCarbon(inboundClaimMappingDialect,
                            inboundClaimValueMap.keySet(), tenantDomain, true);

            claimMap = new HashMap<String, String>();

            Map<String, String> outboundClaimDefaultValues = new HashMap<String, String>();

            for (ClaimMapping outboundClaimMapping : outboundClaimMappings) {

                String inboundClaim = null;

                if (outboundClaimMapping.getLocalClaim() != null) {
                    inboundClaim = carbonToInboundClaimMapping.get(outboundClaimMapping
                            .getLocalClaim().getClaimUri());
                }

                claimMap.put(outboundClaimMapping.getRemoteClaim().getClaimUri(), inboundClaim);

                outboundClaimDefaultValues.put(outboundClaimMapping.getRemoteClaim().getClaimUri(),
                        outboundClaimMapping.getDefaultValue());

            }

            if (claimMap.isEmpty()) {
                return outboundClaimValueMappings;
            }

            for (Iterator<Entry<String, String>> iterator = claimMap.entrySet().iterator(); iterator
                    .hasNext(); ) {
                Entry<String, String> entry = iterator.next();
                String outboundClaimUri = entry.getKey();
                String inboundClaimUri = entry.getValue();

                if (inboundClaimUri != null && inboundClaimValueMap.get(inboundClaimUri) != null) {
                    outboundClaimValueMappings.put(ClaimMapping.build(inboundClaimUri,
                            outboundClaimUri, outboundClaimDefaultValues.get(outboundClaimUri),
                            false), Arrays.asList(new String[]{inboundClaimValueMap
                            .get(inboundClaimUri)}));
                } else {
                    outboundClaimValueMappings.put(ClaimMapping.build(inboundClaimUri,
                            outboundClaimUri, outboundClaimDefaultValues.get(outboundClaimUri),
                            false), Arrays.asList(new String[]{outboundClaimDefaultValues
                            .get(outboundClaimUri)}));
                }
            }

        } catch (Exception e) {
            throw new IdentityApplicationManagementException("Error while loading claim mappings.",
                    e);
        }

        return outboundClaimValueMappings;
    }

    /**
     * @param configElem
     * @return
     */
    public static Properties readProperties(OMElement configElem) {

        Properties properties = new Properties();
        OMElement propsElem = configElem
                .getFirstChildWithName(getQNameWithIdentityApplicationNS(IdentityApplicationConstants.ConfigElements.PROPERTIES));
        if (propsElem != null) {
            Iterator propItr = propsElem
                    .getChildrenWithLocalName(IdentityApplicationConstants.ConfigElements.PROPERTY);
            for (; propItr.hasNext(); ) {
                OMElement propElem = (OMElement) propItr.next();
                String propName = propElem.getAttributeValue(
                        new QName(IdentityApplicationConstants.ConfigElements.ATTR_NAME)).trim();
                String propValue = propElem.getText().trim();
                properties.put(propName, propValue);
                if (log.isDebugEnabled()) {
                    log.debug("Property name : " + propName + ", Property Value : " + propValue);
                }
            }
        }
        return properties;
    }

    /**
     * @param localPart
     * @return
     */
    public static QName getQNameWithIdentityApplicationNS(String localPart) {
        return new QName(IdentityApplicationConstants.APPLICATION_AUTHENTICATION_DEFAULT_NAMESPACE, localPart);
    }

    /**
     * @param federatedAuthenticators
     * @param authenticatorName
     * @return
     */
    public static FederatedAuthenticatorConfig getFederatedAuthenticator(
            FederatedAuthenticatorConfig[] federatedAuthenticators, String authenticatorName) {

        for (FederatedAuthenticatorConfig authenticator : federatedAuthenticators) {
            if (authenticator.getName().equals(authenticatorName)) {
                return authenticator;
            }
        }
        return null;
    }

    /**
     * @param provisioningConnectors
     * @param connectorType
     * @return
     */
    public static ProvisioningConnectorConfig getProvisioningConnector(
            ProvisioningConnectorConfig[] provisioningConnectors, String connectorType) {

        for (ProvisioningConnectorConfig connector : provisioningConnectors) {
            if (connector.getName().equals(connectorType)) {
                return connector;
            }
        }
        return null;
    }

    public static Property getProperty(Property[] properties, String propertyName) {

        for (Property property : properties) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }
        return null;
    }

    /**
     * @param jsonObj
     * @return Base64 encoded JWT
     */
    public static String getSignedJWT(String jsonObj, ServiceProvider serviceProvider) {

        String oauthConsumerSecret = null;

        if (serviceProvider.getInboundAuthenticationConfig() != null
                && serviceProvider.getInboundAuthenticationConfig()
                .getInboundAuthenticationRequestConfigs() != null
                && serviceProvider.getInboundAuthenticationConfig()
                .getInboundAuthenticationRequestConfigs().length > 0) {

            InboundAuthenticationRequestConfig[] authReqConfigs = serviceProvider
                    .getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs();

            for (InboundAuthenticationRequestConfig authReqConfig : authReqConfigs) {
                if ("oauth2".equals(authReqConfig.getInboundAuthType())) {
                    if (authReqConfig.getProperties() != null) {
                        for (Property property : authReqConfig.getProperties()) {
                            if ("oauthConsumerSecret".equalsIgnoreCase(property.getName())) {
                                oauthConsumerSecret = property.getValue();
                                break;
                            }
                        }
                    }
                }
            }

        }

        String jwtBody = "{\"iss\":\"wso2\",\"exp\":" + new Date().getTime() + 3000 + ",\"iat\":"
                + new Date().getTime() + "," + jsonObj + "}";
        String jwtHeader = "{\"typ\":\"JWT\", \"alg\":\"HS256\"}";

        if (oauthConsumerSecret == null) {
            jwtHeader = "{\"typ\":\"JWT\", \"alg\":\"none\"}";
        }

        String base64EncodedHeader = Base64Utils.encode(jwtHeader.getBytes());
        String base64EncodedBody = Base64Utils.encode(jwtBody.getBytes());

        if (log.isDebugEnabled()) {
            log.debug("JWT Header :" + jwtHeader);
            log.debug("JWT Body :" + jwtBody);
        }

        String assertion = base64EncodedHeader + "." + base64EncodedBody;

        if (oauthConsumerSecret == null) {
            return assertion + ".";
        } else {
            String signedAssertion;
            try {
                signedAssertion = calculateHmacSha1(oauthConsumerSecret, assertion);
                return assertion + "." + signedAssertion;
            } catch (SignatureException e) {
                log.error("Error while siging the assertion", e);
                return assertion + ".";
            }
        }
    }

    /**
     * @param key
     * @param value
     * @return
     * @throws SignatureException
     */
    public static String calculateHmacSha1(String key, String value) throws SignatureException {
        String result;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(value.getBytes());
            result = Base64Utils.encode(rawHmac);
        } catch (Exception e) {
            if(log.isDebugEnabled()){
                log.debug("Failed to create the HMAC Signature",e);
            }
            throw new SignatureException("Failed to calculate HMAC : " + e.getMessage());
        }
        return result;
    }
}
