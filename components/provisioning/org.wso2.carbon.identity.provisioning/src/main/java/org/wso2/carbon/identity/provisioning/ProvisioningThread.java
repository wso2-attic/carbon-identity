package org.wso2.carbon.identity.provisioning;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.provisioning.dao.CacheBackedProvisioningMgtDAO;
import org.wso2.carbon.idp.mgt.util.IdPManagementUtil;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.UUID;
import java.util.concurrent.Callable;

public class ProvisioningThread implements Callable<Boolean> {

    private static final Log log = LogFactory.getLog(ProvisioningThread.class);
    private ProvisioningEntity provisioningEntity;
    private String tenantDomainName;
    private AbstractOutboundProvisioningConnector connector;
    private String connectorType;
    private String idPName;
    private CacheBackedProvisioningMgtDAO dao;

    public ProvisioningThread(ProvisioningEntity provisioningEntity, String tenantDomainName,
                              AbstractOutboundProvisioningConnector connector, String connectorType, String idPName,
                              CacheBackedProvisioningMgtDAO dao) {
        super();
        this.provisioningEntity = provisioningEntity;
        this.tenantDomainName = tenantDomainName;
        this.connector = connector;
        this.connectorType = connectorType;
        this.idPName = idPName;
        this.dao = dao;
    }

    public Boolean call() throws IdentityProvisioningException {

        boolean success = false;
        String tenantDomainName = this.tenantDomainName;

        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomainName);

            ProvisionedIdentifier provisionedIdentifier = null;
            // real provisioning happens now.
            provisionedIdentifier = connector.provision(provisioningEntity);

            if (provisioningEntity.getOperation() == ProvisioningOperation.DELETE) {
                deleteProvisionedEntityIdentifier(idPName, connectorType, provisioningEntity,
                        tenantDomainName);
            } else if (provisioningEntity.getOperation() == ProvisioningOperation.POST) {

                if (provisionedIdentifier == null || provisionedIdentifier.getIdentifier() == null) {
                    provisionedIdentifier = new ProvisionedIdentifier();
                    provisionedIdentifier.setIdentifier(UUID.randomUUID().toString());
                }

                provisioningEntity.setIdentifier(provisionedIdentifier);

                // store provisioned identifier for future reference.
                storeProvisionedEntityIdentifier(idPName, connectorType, provisioningEntity,
                        tenantDomainName);
            }

            success = true;
        } catch (IdentityApplicationManagementException e) {
            String errMsg = " Provisioning for Entity " + provisioningEntity.getEntityName() +
                    " For operation = " + provisioningEntity.getOperation();
            throw new IdentityProvisioningException(errMsg, e);
        } catch (Exception e) {
            throw new IdentityProvisioningException(e.getMessage(), e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();

            if (tenantDomainName != null) {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                        tenantDomainName);
            }
        }

        return success;
    }

    /**
     * @param idpName
     * @param connectorType
     * @param provisioningEntity
     * @param tenantDomain
     * @throws IdentityApplicationManagementException
     */
    private void storeProvisionedEntityIdentifier(String idpName, String connectorType,
                                                  ProvisioningEntity provisioningEntity, String tenantDomain)
            throws IdentityApplicationManagementException {
        int tenantId;
        try {
            tenantId = IdPManagementUtil.getTenantIdOfDomain(tenantDomain);
            dao.addProvisioningEntity(idpName, connectorType, provisioningEntity, tenantId, tenantDomain);
        } catch (UserStoreException e) {
            throw new IdentityApplicationManagementException(
                    "Error while storing provisioning identifier.", e);
        }
    }

    /**
     * @param idpName
     * @param connectorType
     * @param provisioningEntity
     * @param tenantDomain
     * @return
     * @throws IdentityApplicationManagementException
     */
    private void deleteProvisionedEntityIdentifier(String idpName, String connectorType,
                                                   ProvisioningEntity provisioningEntity, String tenantDomain)
            throws IdentityApplicationManagementException {
        int tenantId;
        try {
            tenantId = IdPManagementUtil.getTenantIdOfDomain(tenantDomain);
            dao.deleteProvisioningEntity(idpName, connectorType, provisioningEntity, tenantId, tenantDomain);
        } catch (UserStoreException e) {
            throw new IdentityApplicationManagementException(
                    "Error while deleting provisioning identifier.", e);
        }
    }

}
