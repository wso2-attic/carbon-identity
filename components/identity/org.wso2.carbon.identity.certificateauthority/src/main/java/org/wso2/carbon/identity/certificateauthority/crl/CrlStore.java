package org.wso2.carbon.identity.certificateauthority.crl;

import org.wso2.carbon.identity.certificateauthority.dao.CrlDataHolderDao;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;

import java.security.cert.CRLException;
import java.security.cert.CertificateException;

public class CrlStore {
    CrlDataHolderDao crlDataHolderDao = new CrlDataHolderDao();

    /**
     * to get the latest full crl or delta crl for a tenant
     *
     * @param tenantId tenant id
     * @param deltaCrl true if requesting a delta crl, false if requesting full crl
     * @return a X509Crl
     * @throws CertificateException
     * @throws CertAuthException
     */
    public byte[] getLatestCrl(int tenantId, boolean deltaCrl) throws CertificateException, CertAuthException, CRLException {
        return crlDataHolderDao.getLatestCRL(tenantId, deltaCrl).getCRL().getEncoded();
    }
}
