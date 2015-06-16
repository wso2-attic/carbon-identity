package org.wso2.carbon.identity.saml.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.saml.metadata.dao.SAMLSSOServiceProviderDAO;
import org.wso2.carbon.identity.saml.metadata.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.registry.core.Registry;

public class SAMLSSOMetadataConfigService extends AbstractAdmin {

    private static Log log = LogFactory.getLog(SAMLSSOMetadataConfigService.class);

    private static SAMLSSOMetadataConfigService samlssoMetadataConfigService = new SAMLSSOMetadataConfigService();

    private SAMLSSOMetadataConfigService(){

    }

    public static SAMLSSOMetadataConfigService getInstance(){
        return samlssoMetadataConfigService;
    }

    /**
     * Add a relying party service provider for SAML SSO
     *
     * @param serviceProviderDO
     * @return
     * @throws IdentityException
     */
    public boolean addServiceProvider(Registry registry, SAMLSSOServiceProviderDO serviceProviderDO)
            throws IdentityException {
        SAMLSSOServiceProviderDAO serviceProviderDAO = new SAMLSSOServiceProviderDAO(registry);
        return serviceProviderDAO.addServiceProvider(serviceProviderDO);
    }

    /**
     * update an existing SAML SSO service provider
     *
     * @param registry
     * @param serviceProviderDO
     * @return
     * @throws IdentityException
     */
    public boolean updateServiceProvider(Registry registry, SAMLSSOServiceProviderDO serviceProviderDO)
            throws IdentityException {
        SAMLSSOServiceProviderDAO serviceProviderDAO = new SAMLSSOServiceProviderDAO(registry);
        return serviceProviderDAO.updateServiceProvider(serviceProviderDO);
    }

    /**
     * upload service provider metadata directly for SAML SSO
     *
     * @param registry
     * @param metadata
     * @return
     * @throws IdentityException
     */
    public SAMLSSOServiceProviderDO uploadServiceProvider(Registry registry, String metadata) throws IdentityException {
        SAMLSSOServiceProviderDAO serviceProviderDAO = new SAMLSSOServiceProviderDAO(registry);
        return serviceProviderDAO.uploadServiceProvider(metadata);
    }

    public SAMLSSOServiceProviderDO[] getServiceProviders(Registry registry)
            throws IdentityException {
        SAMLSSOServiceProviderDAO serviceProviderDOA = new SAMLSSOServiceProviderDAO(registry);
        return serviceProviderDOA.getServiceProviders();
    }

    public boolean removeServiceProvider(Registry registry, String issuer) throws IdentityException {
        SAMLSSOServiceProviderDAO serviceProviderDAO = new SAMLSSOServiceProviderDAO(registry);
        return serviceProviderDAO.removeServiceProvider(issuer);
    }

    public SAMLSSOServiceProviderDO getServiceProvider(Registry registry, String issuer)
            throws IdentityException {
        SAMLSSOServiceProviderDAO serviceProviderDAO = new SAMLSSOServiceProviderDAO(registry);
        return serviceProviderDAO.getServiceProvider(issuer);
    }
}
