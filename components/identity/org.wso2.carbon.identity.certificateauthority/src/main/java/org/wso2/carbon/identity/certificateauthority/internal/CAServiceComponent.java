package org.wso2.carbon.identity.certificateauthority.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;
import org.wso2.carbon.identity.certificateauthority.scheduledTask.CrlUpdater;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.user.core.service.RealmService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @scr.component name="identity.certificateauthority" immediate="true"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 */
public class CAServiceComponent {


    private static RealmService realmService;
    private static Log log = LogFactory.getLog(CAServiceComponent.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static RealmService getRealmService() {
        return realmService;
    }

    protected void setRealmService(RealmService realmService) {
        CAServiceComponent.realmService = realmService;
    }

    public static int getUserDomainId(int tenantID, String tenantDomain) throws CertAuthException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet;
        String sql = null;

        try {
            log.debug("retriving csr information for tenant :" + tenantDomain);
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            sql = "SELECT * FROM UM_DOMAIN WHERE UM_DOMAIN_NAME = ? AND UM_TENANT_ID = ?";
            prepStmt = connection.prepareStatement(sql);

            prepStmt.setString(1, tenantDomain);
            prepStmt.setInt(2, tenantID);
            resultSet = prepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("UM_DOMAIN_ID");
            }

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new CertAuthException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + sql);
            log.error(e.getMessage(), e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return 0;

    }

    protected void unsetRealmService(RealmService realmService) {
        setRealmService(null);
    }

    protected void activate(ComponentContext ctxt) {
        log.info("starting scheduled task for creating CRLs for tenants");
        scheduler.scheduleAtFixedRate(new CrlUpdater(), 30, 60 * 60 * 24, TimeUnit.SECONDS);
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("CA component is deactivating ...");
        }
    }
}
