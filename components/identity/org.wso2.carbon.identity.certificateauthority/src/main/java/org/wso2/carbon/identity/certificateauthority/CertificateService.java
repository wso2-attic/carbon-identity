package org.wso2.carbon.identity.certificateauthority;

import org.wso2.carbon.identity.certificateauthority.dao.PublicCertificateDAO;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;

public class CertificateService {
    private PublicCertificateDAO certificateDAO = new PublicCertificateDAO();

    public String getCertificate(String serial) throws CertAuthException {
        Certificate cert = certificateDAO.getPubCert(serial);
        if (cert != null) {
            return cert.getPublicCertificate();
        }
        return null;
    }
}
