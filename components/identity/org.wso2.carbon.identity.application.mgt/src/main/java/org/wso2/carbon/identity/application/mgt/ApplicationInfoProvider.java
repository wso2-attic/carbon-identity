package org.wso2.carbon.identity.application.mgt;

import java.util.List;
import java.util.Map;

import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.dao.ApplicationDAO;
import org.wso2.carbon.identity.base.IdentityException;

public class ApplicationInfoProvider {

    private static ApplicationInfoProvider appInfo = new ApplicationInfoProvider();

    /**
     * 
     */
    private ApplicationInfoProvider() {

    }

    /**
     * 
     * @return
     */
    public static ApplicationInfoProvider getInstance() {
        return appInfo;
    }

    /**
     * [sp-claim-uri,local-idp-claim-uri]
     * 
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public Map<String, String> getServiceProviderToLocalIdPClaimMapping(String serviceProviderName,
            String tenantDomain) throws IdentityException {
        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        return appDAO.getServiceProviderToLocalIdPClaimMapping(serviceProviderName, tenantDomain);
    }

    /**
     * [local-idp-claim-uri,sp-claim-uri]
     * 
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public Map<String, String> getLocalIdPToServiceProviderClaimMapping(String serviceProviderName,
            String tenantDomain) throws IdentityException {
        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        return appDAO.getLocalIdPToServiceProviderClaimMapping(serviceProviderName, tenantDomain);
    }

    /**
     * Returns back the requested set of claims by the provided service provider in local idp claim
     * dialect.
     * 
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public List<String> getAllRequestedClaimsByServiceProvider(String serviceProviderName,
            String tenantDomain) throws IdentityException {
        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        return appDAO.getAllRequestedClaimsByServiceProvider(serviceProviderName, tenantDomain);
    }

    /**
     * 
     * @param clientId
     * @param clientType
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public String getServiceProviderNameByClientId(String clientId, String clientType,
            String tenantDomain) throws IdentityException {
        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        return appDAO.getServiceProviderNameByClientId(clientId, clientType, tenantDomain);
    }

    /**
     * 
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public ServiceProvider getServiceProvider(String serviceProviderName, String tenantDomain)
            throws IdentityException {
        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        return appDAO.getApplication(serviceProviderName);
    }

    /**
     * 
     * @param serviceProviderName
     * @param tenantDomain
     * @return
     * @throws IdentityException
     */
    public ServiceProvider getServiceProviderByClienId(String clientId, String clientType,
            String tenantDomain) throws IdentityException {
        String serviceProviderName = getServiceProviderNameByClientId(clientId, clientType,
                tenantDomain);
        ApplicationDAO appDAO = ApplicationMgtSystemConfig.getInstance().getApplicationDAO();
        return appDAO.getApplication(serviceProviderName);
    }
}
