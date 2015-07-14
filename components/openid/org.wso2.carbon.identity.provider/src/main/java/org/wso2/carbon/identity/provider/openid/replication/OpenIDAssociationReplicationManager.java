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
package org.wso2.carbon.identity.provider.openid.replication;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.Association;
import org.wso2.carbon.identity.base.IdentityConstants.ServerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.internal.IdentityProviderServiceComponent;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to replicate OpenID associations across the cluster
 */
public class OpenIDAssociationReplicationManager {

    private static Log log = LogFactory.getLog(OpenIDAssociationReplicationManager.class);
    private static OpenIDAssociationReplicationManager associationReplicationManager = new OpenIDAssociationReplicationManager();
    private Map<String, Association> associationMap = new ConcurrentHashMap<String, Association>();

    public OpenIDAssociationReplicationManager() {
        String isCleanupTaskEnabled = "true";
        if (IdentityUtil.getProperty(ServerConfig.ENABLE_OPENID_ASSOCIATION_CLEANUP_TASK) != null) {
            isCleanupTaskEnabled = IdentityUtil.getProperty(ServerConfig.ENABLE_OPENID_ASSOCIATION_CLEANUP_TASK);
        }
        if(Boolean.parseBoolean(isCleanupTaskEnabled)){
            // default period is set to 15 minutes
            long cleanupPeriod = 15;

            String associationCleanupPeriod = IdentityUtil.getProperty(ServerConfig.OPENID_ASSOCIATION_CLEANUP_PERIOD);
            if (associationCleanupPeriod != null || !associationCleanupPeriod.trim().isEmpty()) {
                try {
                    cleanupPeriod = Long.parseLong(associationCleanupPeriod);
                } catch (NumberFormatException e) {
                    log.warn("Error while setting association cleanup period as " + associationCleanupPeriod
                            + ". Setting association cleanup period to default (15 mins)", e);
                }
            }

            AssociationCleanupService sessionCleanUpService = new AssociationCleanupService(cleanupPeriod,
                    cleanupPeriod);
            sessionCleanUpService.activateCleanUp();
        }  else {
            log.info("Association CleanUp Task is not enabled.");
        }
    }

    public static OpenIDAssociationReplicationManager getPersistenceManager() {
        return associationReplicationManager;
    }

    /**
     * Get the association map
     *
     * @return
     */
    public Map<String, Association> getAssociationMap() {
        return associationMap;
    }

    /**
     * Check whether this is an existing association
     *
     * @return
     */
    public boolean isExistingSession(String id) {
        if (associationMap.containsKey(id)) {
            return true;
        }
        return false;
    }

    public void addAssociation(Association association) {
        associationMap.put(association.getHandle(),association);
        AssociationClusterMessage associationInfoData = new AssociationClusterMessage(association, false);
        replicateAssociationInfo(associationInfoData);
    }

    public Association getAssociation(String handle) {
        return associationMap.get(handle);
    }

    public void removeAssociation(String handle) {
        if(associationMap.containsKey(handle)) {
            Association association = associationMap.get(handle);
            AssociationClusterMessage associationInfoData = new AssociationClusterMessage(association, true);
            replicateAssociationInfo(associationInfoData);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Association does not exists. Cannot remove association : " + handle);
            }
        }
    }

    private void replicateAssociationInfo(AssociationClusterMessage associationInfoData) {
        if (log.isDebugEnabled()) {
            log.debug("Starting to replicate association : " + associationInfoData.getAssociation().getHandle());
        }

        ClusteringAgent agent = IdentityProviderServiceComponent.getConfigContext().getAxisConfiguration().getClusteringAgent();
        if (log.isDebugEnabled()) {
            log.debug("Clustering Agent: " + agent);
        }

        if (agent != null) {
            try {
                agent.sendMessage(associationInfoData, true);
            } catch (ClusteringFault e) {
                log.error("Unable to send cluster message :" + e.getMessage(), e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Completed replicating association : " + associationInfoData.getAssociation().getHandle());
        }
    }

    public void removeExpiredAssociations() {
        Date currentTime = new Date();
        for (Map.Entry<String, Association> entry : associationMap.entrySet()) {
            Association association = entry.getValue();
            if(currentTime.after(association.getExpiry())) {
                if (log.isDebugEnabled()) {
                    log.debug("Current time : " + currentTime.getTime() + ", expiry time : "
                            + association.getExpiry().getTime() + ". Hence removing expired association : "
                            + association.getHandle());
                }
                removeAssociation(association.getHandle());
            }
        }

    }
}
