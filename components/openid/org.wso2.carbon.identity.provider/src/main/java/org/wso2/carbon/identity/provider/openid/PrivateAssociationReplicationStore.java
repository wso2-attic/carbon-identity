/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.provider.openid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.wso2.carbon.identity.provider.openid.replication.OpenIDAssociationReplicationManager;

import java.util.Date;
import java.util.Random;

public class PrivateAssociationReplicationStore extends InMemoryServerAssociationStore {

    private int storeId = 0;
    private String timestamp;
    private int counter;
    private String associationStoreType;

    private static Log log = LogFactory.getLog(PrivateAssociationReplicationStore.class);

    public PrivateAssociationReplicationStore(String associationsType) {
        storeId = new Random().nextInt(9999);
        timestamp = Long.toString(new Date().getTime());
        counter = 0;
        associationStoreType = associationsType;
    }

    public PrivateAssociationReplicationStore() {
        storeId = new Random().nextInt(9999);
        timestamp = Long.toString(new Date().getTime());
        counter = 0;
    }

    public Association generate(String type, int expiryIn) throws AssociationException {
        String handle = storeId + timestamp + "-" + getCounter();
        Association association = Association.generate(type, handle, expiryIn);

        // replicating association using cluster messages
        if(log.isDebugEnabled()) {
            log.debug("Storing association " + association.getHandle() + " in the map.");
        }
        OpenIDAssociationReplicationManager.getPersistenceManager().addAssociation(association);

        return association;
    }

    private synchronized int getCounter(){
        return counter++;
    }

    public Association load(String handle) {
        // get association using map
        Association association = OpenIDAssociationReplicationManager.getPersistenceManager().getAssociation(handle);

        // no association found for the given handle
        if (association == null) {
            log.warn("Association " + handle + " not found in the map.");
            return null;
        }

        // if the association is expired
        if (association.hasExpired()) {
            log.warn("Association is expired for handle " + handle);
            remove(handle); // remove from map
            return null;

        }

        return association;
    }

    /**
     * Removes the association from the map.
     */
    public void remove(String handle) {
        // replicating association using cluster messages
        if(log.isDebugEnabled()) {
            log.debug("Removing the association" + handle + " from the map");
        }
        OpenIDAssociationReplicationManager.getPersistenceManager().removeAssociation(handle);
    }
}
