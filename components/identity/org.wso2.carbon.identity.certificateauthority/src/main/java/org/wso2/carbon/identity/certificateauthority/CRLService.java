package org.wso2.carbon.identity.certificateauthority;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.certificateauthority.crl.CrlFactory;
import org.wso2.carbon.identity.certificateauthority.crl.CrlStore;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.scheduledTask.CrlUpdater;

import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;

public class CRLService {
    public void addCRL() throws Exception {
        CrlFactory factory = new CrlFactory();
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        factory.createAndStoreCrl(tenantID);
    }

    public void addDeltaCrl() throws Exception {
        CrlFactory factory = new CrlFactory();
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        factory.createAndStoreDeltaCrl(tenantID);
    }

    public byte[] getLatestCrl(int tenantID) throws CertificateException, CertAuthException, CRLException {
        CrlStore store = new CrlStore();
        byte[] crlBytes = store.getLatestCrl(tenantID, false);
        return crlBytes;
    }

    public byte[] getLatestDeltaCrl(int tenantId) throws CertificateException, CertAuthException, CRLException {
        CrlStore store = new CrlStore();
        byte[] crlBytes = store.getLatestCrl(tenantId, true);
        return crlBytes;

    }

    public void updateCrl() throws Exception {
        CrlUpdater updater = new CrlUpdater();
        updater.buildFullCrl();
    }

    public X509CRL getLatestX509Crl(int tenantId) throws CertificateException, CertAuthException {
        CrlStore store = new CrlStore();
        return store.getLatestX509Crl(tenantId, false);
    }
}
