package org.wso2.carbon.identity.certificateauthority.crl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.x509.X509V2CRLGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.wso2.carbon.identity.certificateauthority.dao.CertificateDAO;
import org.wso2.carbon.identity.certificateauthority.dao.CrlDataHolderDao;
import org.wso2.carbon.identity.certificateauthority.dao.RevocationDAO;
import org.wso2.carbon.identity.certificateauthority.data.CRLDataHolder;
import org.wso2.carbon.identity.certificateauthority.data.RevokedCertificate;
import org.wso2.carbon.identity.certificateauthority.utils.CAUtils;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CrlFactory {
    Log log = LogFactory.getLog(CrlFactory.class);

    private long CRL_UPDATE_TIME = 24 * 60 * 60 * 1000;


    /**
     * @param caCert              Certoficate authority's certificate
     * @param caKey               CA private key
     * @param revokedCertificates list of revoked certificates
     * @param crlNumber           unique number of the crl
     * @param baseCrlNumber       base crl number
     * @param isDeltaCrl          whether the crl is a delta crl or a full crl
     * @return returns the X509 Crl
     * @throws Exception
     */
    private X509CRL createCRL(X509Certificate caCert, PrivateKey caKey, RevokedCertificate[] revokedCertificates, int crlNumber, int baseCrlNumber, boolean isDeltaCrl) throws Exception {
        X509V2CRLGenerator crlGen = new X509V2CRLGenerator();
        Date now = new Date();
        CertificateDAO certificateDAO = new CertificateDAO();
        RevocationDAO revocationDAO = new RevocationDAO();
        crlGen.setIssuerDN(caCert.getSubjectX500Principal());
        crlGen.setThisUpdate(now);
        crlGen.setNextUpdate(new Date(now.getTime() + CRL_UPDATE_TIME));
        crlGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        for (RevokedCertificate cert : revokedCertificates) {
            BigInteger serialNo = new BigInteger(cert.getSerialNo());
            crlGen.addCRLEntry(serialNo, cert.getRevokedDate(), cert.getReason());
        }
        crlGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        crlGen.addExtension(X509Extensions.CRLNumber, false, new CRLNumber(BigInteger.valueOf(crlNumber)));
        if (isDeltaCrl) {
            crlGen.addExtension(X509Extensions.DeltaCRLIndicator, true, new CRLNumber(BigInteger.valueOf(baseCrlNumber)));
        }
        X509CRL crl = crlGen.generateX509CRL(caKey, "BC");
        return crl;
    }

    /**
     * @param tenantId tenant Id
     * @return full crl of the tenant
     * @throws Exception
     */

    public X509CRL createFullCrl(int tenantId) throws Exception {
        RevocationDAO revocationDAO = new RevocationDAO();
        CrlDataHolderDao crlDataHolderDao = new CrlDataHolderDao();
        RevokedCertificate[] revokedCertificates = revocationDAO.getRevokedCerts(tenantId);
        CRLDataHolder crlDataHolder = crlDataHolderDao.getLatestCRL(tenantId, false);
        PrivateKey privateKey = CAUtils.getConfiguredPrivateKey();
        X509Certificate certb = CAUtils.getConfiguredCaCert();
        int fullnumber = crlDataHolderDao.findHighestCrlNumber(tenantId, false);
        int deltanumber = crlDataHolderDao.findHighestCrlNumber(tenantId, true);
        // nextCrlNumber: The highest number of last CRL (full or delta) and increased by 1 (both full CRLs and deltaCRLs share the same series of CRL Number)
        int nextCrlNumber = ((fullnumber > deltanumber) ? fullnumber : deltanumber) + 1;
        X509CRL crl = createCRL(certb, privateKey, revokedCertificates, nextCrlNumber, -1, false);
        return crl;
    }

    /**
     * @param tenantId id of the tenant creating delta crl
     * @return a delta crl which
     * @throws Exception
     */
    public X509CRL creteDeltaCrl(int tenantId) throws Exception {
        RevocationDAO revocationDAO = new RevocationDAO();
        CrlDataHolderDao crlDataHolderDao = new CrlDataHolderDao();
        CRLDataHolder dataholder = crlDataHolderDao.getLatestCRL(tenantId, false);
        X509CRL latestCrl = null;
        if (dataholder != null) {
            latestCrl = crlDataHolderDao.getLatestCRL(tenantId, false).getCRL();
            RevokedCertificate[] revokedCertificates = revocationDAO.getRevokedCertsAfter(tenantId, latestCrl.getThisUpdate());
            CRLDataHolder crlDataHolder = crlDataHolderDao.getLatestCRL(tenantId, false);
            PrivateKey privateKey = CAUtils.getConfiguredPrivateKey();
            X509Certificate certb = CAUtils.getConfiguredCaCert();
            int fullnumber = crlDataHolderDao.findHighestCrlNumber(tenantId, false);
            int deltanumber = crlDataHolderDao.findHighestCrlNumber(tenantId, true);
            // nextCrlNumber: The highest number of last CRL (full or delta) and increased by 1 (both full CRLs and deltaCRLs share the same series of CRL Number)
            int nextCrlNumber = ((fullnumber > deltanumber) ? fullnumber : deltanumber) + 1;
            X509CRL crl = createCRL(certb, privateKey, revokedCertificates, nextCrlNumber, fullnumber, false);
            return crl;
        } else {
            log.info("No base crl found to create a delta crl");
        }

        return null;
    }

    /**
     * creates and store a crl in db for the given tenant
     *
     * @param tenantId tenant id
     * @throws Exception
     */
    public void createAndStoreCrl(int tenantId) throws Exception {
        X509CRL crl = createFullCrl(tenantId);
        CrlDataHolderDao crlDataHolderDao = new CrlDataHolderDao();
        RevocationDAO revocationDAO = new RevocationDAO();
        revocationDAO.removeActivedCerts();
        int fullnumber = crlDataHolderDao.findHighestCrlNumber(tenantId, false);
        int deltanumber = crlDataHolderDao.findHighestCrlNumber(tenantId, true);
        // nextCrlNumber: The highest number of last CRL (full or delta) and increased by 1 (both full CRLs and deltaCRLs share the same series of CRL Number)
        int nextCrlNumber = ((fullnumber > deltanumber) ? fullnumber : deltanumber) + 1;

        crlDataHolderDao.addCRL(crl, tenantId, crl.getThisUpdate(), crl.getNextUpdate(), nextCrlNumber, -1);

    }

    /**
     * create and store a delta crl in database
     *
     * @param tenantId id of the tenant
     * @throws Exception
     */
    public void createAndStoreDeltaCrl(int tenantId) throws Exception {
        X509CRL crl = creteDeltaCrl(tenantId);
        if (crl != null) {
            CrlDataHolderDao crlDataHolderDao = new CrlDataHolderDao();
            int fullnumber = crlDataHolderDao.findHighestCrlNumber(tenantId, false);
            int deltanumber = crlDataHolderDao.findHighestCrlNumber(tenantId, true);
            // nextCrlNumber: The highest number of last CRL (full or delta) and increased by 1 (both full CRLs and deltaCRLs share the same series of CRL Number)
            int nextCrlNumber = ((fullnumber > deltanumber) ? fullnumber : deltanumber) + 1;
            crlDataHolderDao.addCRL(crl, tenantId, crl.getThisUpdate(), crl.getNextUpdate(), nextCrlNumber, 1);
        } else {
            log.info("Error while creating delta crl for tenant " + tenantId);
        }
    }

}
