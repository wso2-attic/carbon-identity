package org.wso2.carbon.identity.certificateauthority;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.certificateauthority.dao.CertificateDAO;
import org.wso2.carbon.identity.certificateauthority.dao.CsrDAO;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.data.Certificate;
import org.wso2.carbon.identity.certificateauthority.data.CsrFile;
import org.wso2.carbon.identity.certificateauthority.data.CsrMetaInfo;
import org.wso2.carbon.identity.certificateauthority.internal.CAServiceComponent;
import org.wso2.carbon.user.core.util.UserCoreUtil;

public class CAClientService {
    Log log = LogFactory.getLog(CAClientService.class);

    private CsrDAO csrDAO = new CsrDAO();
    private CertificateDAO certificateDAO = new CertificateDAO();

    public String addCsr(String csrContent) throws CertAuthException {
        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String userStoreDomain = UserCoreUtil.extractDomainFromName(username);
        return csrDAO.addCsr(csrContent, username, tenantID, userStoreDomain);
    }

    public CsrFile getCsr(String serial) throws CertAuthException {
        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String userStoreDomain = UserCoreUtil.extractDomainFromName(username);
        int userStoreId = CAServiceComponent.getUserDomainId(tenantID, userStoreDomain);
        return csrDAO.getCSR(serial, userStoreId, username, tenantID);
    }

    public CsrMetaInfo[] getCsrList() throws CertAuthException {
        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String userStoreDomain = UserCoreUtil.extractDomainFromName(username);
        int userStoreId = CAServiceComponent.getUserDomainId(tenantID, userStoreDomain);
        return csrDAO.getCsrList(tenantID, username, userStoreId);
    }

    public Certificate getCertificate(String serialNo) throws CertAuthException {
        String username = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String userStoreDomain = UserCoreUtil.extractDomainFromName(username);
        int userStoreId = CAServiceComponent.getUserDomainId(tenantID, userStoreDomain);
        return certificateDAO.getPubCert(serialNo, tenantID, username, userStoreId);
    }
}
