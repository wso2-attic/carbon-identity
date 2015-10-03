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

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.openid4java.association.Association;

import java.io.Serializable;

public class AssociationClusterMessage extends ClusteringMessage implements Serializable {

    private static final long serialVersionUID = 3421028095886637679L;

    private static Log log = org.apache.commons.logging.LogFactory.getLog(AssociationClusterMessage.class);

    private Association association;
    private boolean isRemove;


    public AssociationClusterMessage(Association association, boolean isRemove){
        this.setAssociation(association);
        this.setRemove(isRemove);
    }

    public Association getAssociation() {
        return association;
    }

    public void setAssociation(Association association) {
        this.association = association;
    }

    public boolean isRemove() {
        return isRemove;
    }

    public void setRemove(boolean isRemove) {
        this.isRemove = isRemove;
    }


    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    @Override
    public void execute(ConfigurationContext configCtx) throws ClusteringFault {
        OpenIDAssociationReplicationManager associationReplicationManager = OpenIDAssociationReplicationManager.getPersistenceManager();
        Association association = getAssociation();
        if(isRemove()){
            if (log.isDebugEnabled()) {
                log.debug("Removing association : " + association.getHandle() + " during the replication replication");
            }

            if(associationReplicationManager.isExistingSession(association.getHandle())) {
                if (log.isDebugEnabled()) {
                    log.debug("Association : " + association.getHandle() + " does exist");
                }
                associationReplicationManager.getAssociationMap().remove(association.getHandle());
                if (log.isDebugEnabled()) {
                    log.debug("Removed association : " + association.getHandle() + " during the replication replication");
                }
            }
            else {
                if (log.isDebugEnabled()) {
                    log.debug("Association : " + association.getHandle() + " does not exist. Cannot remove association");
                }
            }
        }
        else{
            if (log.isDebugEnabled()) {
                log.debug("Adding new association : " + association.getHandle() + " during the replication replication");
            }
            associationReplicationManager.getAssociationMap().put(association.getHandle(), association);
            if (log.isDebugEnabled()) {
                log.debug("Added new association : " + association.getHandle() + " during the replication replication");
            }
        }
    }
}