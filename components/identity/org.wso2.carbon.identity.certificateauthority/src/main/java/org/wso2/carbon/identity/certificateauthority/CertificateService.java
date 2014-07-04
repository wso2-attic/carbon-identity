package org.wso2.carbon.identity.certificateauthority;

import org.wso2.carbon.identity.certificateauthority.dao.CertificateDAO;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;
import org.wso2.carbon.identity.certificateauthority.utils.CertificateUtils;

public class CertificateService {
    private CertificateDAO certificateDAO = new CertificateDAO();

    public String getCertificate(String serial) throws CertAuthException {
        Certificate cert = certificateDAO.getPubCert(serial);
        if (cert != null) {
            return CertificateUtils.getEncodedCertificate(cert.getPublicCertificate());
        }
        return null;
    }
}
