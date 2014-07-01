package org.wso2.carbon.identity.certificateauthority.scheduledTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.certificateauthority.crl.CrlFactory;
import org.wso2.carbon.identity.certificateauthority.internal.CAServiceComponent;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserRealmService;

public class CrlUpdater implements Runnable {
    private Log log = LogFactory.getLog(CrlUpdater.class);

    public void buildFullCrl() throws Exception {
        CrlFactory crlFactory = new CrlFactory();
        UserRealmService service = CAServiceComponent.getRealmService();
        setTenant(MultitenantConstants.SUPER_TENANT_ID, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        crlFactory.createAndStoreCrl(MultitenantConstants.SUPER_TENANT_ID);

        for (Tenant tenant : service.getTenantManager().getAllTenants()) {
            setTenant(tenant.getId(), tenant.getDomain());
            crlFactory.createAndStoreCrl(tenant.getId());
        }
        // setTenant(-1,null);

    }

    @Override
    public void run() {
        try {
            log.info("building full crls for tenants... and testing for scripts");
            buildFullCrl();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void setTenant(int tenantId, String tenantDomain) {

        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
    }
}


