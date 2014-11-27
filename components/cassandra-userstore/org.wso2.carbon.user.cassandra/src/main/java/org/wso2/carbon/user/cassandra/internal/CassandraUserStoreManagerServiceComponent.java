package org.wso2.carbon.user.cassandra.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.cassandra.CassandraUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

	/**
	 * @scr.component name="cassandra.user.store.manager.dscomponent" immediate=true
	 * @scr.reference name="user.realmservice.default"
	 * interface="org.wso2.carbon.user.core.service.RealmService"
	 * cardinality="1..1" policy="dynamic" bind="setRealmService"
	 * unbind="unsetRealmService"
	 */
	public class CassandraUserStoreManagerServiceComponent {
	    private static Log log = LogFactory.getLog(CassandraUserStoreManagerServiceComponent.class);
	    private static RealmService realmService;

	    protected void activate(ComponentContext ctxt) {

	        CassandraUserStoreManager cassandraUserStoreManager = new CassandraUserStoreManager();
	        ctxt.getBundleContext().registerService(UserStoreManager.class.getName(), cassandraUserStoreManager, null);
	        log.info("cassandraUserStoreManager bundle activated successfully..");
	    }

	    protected void deactivate(ComponentContext ctxt) {
	        if (log.isDebugEnabled()) {
	            log.debug("Cassandra User Store Manager is deactivated ");
	        }
	    }

	    protected void setRealmService(RealmService rlmService) {
	         realmService = rlmService;
	    }

	    protected void unsetRealmService(RealmService realmService) {
	        realmService = null;
	    }
	}

