package org.wso2.carbon.identity.certificateauthority;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.*;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.certificateauthority.dao.PublicCertificateDAO;
import org.wso2.carbon.identity.certificateauthority.dao.RevocationDAO;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;
import org.wso2.carbon.identity.certificateauthority.data.RevokedCertificate;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class OCSPService {

    private PublicCertificateDAO certificateDAO;
    private RevocationDAO revocationDAO;

    private Log log = LogFactory.getLog(OCSPService.class);

    public OCSPService() {
        this.certificateDAO = new PublicCertificateDAO();
        this.revocationDAO = new RevocationDAO();
    }

    public OCSPResp handleOCSPRequest(OCSPReq req, int tenantID) throws OCSPException {
        OCSPRespGenerator respGenerator = new OCSPRespGenerator();
        if (req == null || req.getRequestList().length <= 0) {
            return respGenerator.generate(OCSPRespGenerator.MALFORMED_REQUEST, null);
        }
        //sign with tenant's configured key
        Req[] requests = req.getRequestList();
        try {
            CertificateID certID;
            Certificate certificate;
            KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(tenantID);
            PublicKey publicKey = keyStoreManager.getDefaultPublicKey();
            PrivateKey privateKey = keyStoreManager.getDefaultPrivateKey();
            BasicOCSPRespGenerator basicRespGen = new BasicOCSPRespGenerator(publicKey);
            for (int i = 0; i < requests.length; i++) {
                certID = requests[i].getCertID();
                certificate = certificateDAO.getPubCert(certID.getSerialNumber().toString());
                if (certificate == null || tenantID != certificate.getTenantID()) {
                    basicRespGen.addResponse(certID, new UnknownStatus());
                } else {
                    org.wso2.carbon.identity.certificateauthority.data.CertificateStatus certificateStatus = org.wso2
                            .carbon.identity.certificateauthority.data.CertificateStatus.valueOf(certificate.getStatus());
                    switch (certificateStatus) {
                        case REVOKED:
                            RevokedCertificate revokedCertificate = revocationDAO.getRevokedCertificate(certificate.getSerialNo());
                            basicRespGen.addResponse(certID, new RevokedStatus(revokedCertificate.getRevokedDate(),
                                    revokedCertificate.getReason()));
                            break;
                        case ACTIVE:
                            basicRespGen.addResponse(certID, CertificateStatus.GOOD);
                            break;
                        default:
                            basicRespGen.addResponse(certID, new UnknownStatus());
                    }
                }
            }
            BasicOCSPResp basicOCSPResp = basicRespGen.generate("SHA256WithRSA", privateKey, null, new Date(), "BC");
            return respGenerator.generate(OCSPRespGenerator.SUCCESSFUL, basicOCSPResp);
        } catch (Exception e) {
            return respGenerator.generate(OCSPRespGenerator.INTERNAL_ERROR, null);
        }
    }
}
