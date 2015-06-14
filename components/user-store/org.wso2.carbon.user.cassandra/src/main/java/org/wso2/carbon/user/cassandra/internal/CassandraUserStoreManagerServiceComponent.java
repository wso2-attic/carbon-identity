/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
    private static final Log log = LogFactory.getLog(CassandraUserStoreManagerServiceComponent.class);

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
        CassandraUserStoreManagerServiceDataHolder.getInstance().setRealmService(rlmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        CassandraUserStoreManagerServiceDataHolder.getInstance().setRealmService(null);
    }
}

