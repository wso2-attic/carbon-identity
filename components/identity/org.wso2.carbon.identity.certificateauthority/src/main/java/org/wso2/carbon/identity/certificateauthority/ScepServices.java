package org.wso2.carbon.identity.certificateauthority;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.wso2.carbon.identity.certificateauthority.dao.CertificateDAO;
import org.wso2.carbon.identity.certificateauthority.dao.CsrDAO;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;
import org.wso2.carbon.identity.certificateauthority.data.CsrFile;
import org.wso2.carbon.identity.certificateauthority.data.CsrStatus;
import org.wso2.carbon.identity.certificateauthority.utils.CAUtils;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class ScepServices {

    private CsrDAO csrDAO = new CsrDAO();
    private CertificateDAO certDao = new CertificateDAO();

    public void addCsr(PKCS10CertificationRequest certReq, String transId, int tenantId)
            throws CertAuthException {
        csrDAO.addCsrFromScep(certReq, transId, tenantId);
    }

    public X509Certificate getCertificate(int tenantId, String transactionId)
            throws CertAuthException {
        CsrFile csrFile = csrDAO.getCsrWithTransactionId(transactionId);
        if (CsrStatus.SIGNED.toString().equals(csrFile.getStatus())) {
            Certificate cert = certDao.getPubCert(csrFile.getSerialNo());
            if (cert != null) {
                return cert.getPublicCertificate();
            }
        }
        return null;
    }

    public X509Certificate getCertificate(String serialNo) throws CertAuthException {
        Certificate cert = certDao.getPubCert(serialNo);
        if (cert != null) {
            return cert.getPublicCertificate();
        }
        return null;
    }

    public X509Certificate getCaCert(int tenantId) {
        return CAUtils.getConfiguredCaCert(tenantId);
    }

    public PrivateKey getCaKey(int tenantId) {
        return CAUtils.getConfiguredPrivateKey();
    }


}
