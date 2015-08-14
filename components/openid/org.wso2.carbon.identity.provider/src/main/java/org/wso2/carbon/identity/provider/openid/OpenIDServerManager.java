/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.openid4java.message.AuthRequest;
import org.openid4java.message.Message;
import org.openid4java.server.ServerAssociationStore;
import org.openid4java.server.ServerManager;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * This class passes OpenID messages such as OpenID Association, OpenID Request
 * and Response messages to the ServerManager class of the openid4java library.
 * However the purpose of this class is to force the super class to use the
 * custom AssociationStore instead the default InMemoryAssociationStores.
 *
 * @author WSO2 Inc.
 */
public class OpenIDServerManager extends ServerManager {

    private static final Log log = LogFactory.getLog(OpenIDServerManager.class);

    /**
     * Here we set our AssociationStore implementation to the parent.
     *
     */
    public OpenIDServerManager() {
        /**
         * Keeps track of the associations established with consumer sites.
         */
        ServerAssociationStore sharedAssociations =
                new OpenIDServerAssociationStore(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_SHARED);
        super.setSharedAssociations(sharedAssociations);

        /**
         * Keeps track of private (internal) associations created for signing
         * authentication responses for stateless consumer sites.
         */
//        ServerAssociationStore privateAssociations =
//                new OpenIDServerAssociationStore(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE);
        ServerAssociationStore privateAssociations = null;
        if(log.isDebugEnabled()) {
            log.debug("Initialising privateAssociation Store");
        }

        synchronized (Runtime.getRuntime().getClass()){
            String privateAssociationStoreClassName = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_PRIVATE_ASSOCIATION_STORE_CLASS);
            if(privateAssociationStoreClassName != null && !privateAssociationStoreClassName.trim().isEmpty()) {
                privateAssociationStoreClassName = privateAssociationStoreClassName.trim();
                if(log.isDebugEnabled()) {
                    log.debug("Initialising privateAssociation Store : " + privateAssociationStoreClassName);
                }
                try {
                    privateAssociations = (ServerAssociationStore)Class.forName(privateAssociationStoreClassName)
                            .newInstance();
                    if(log.isDebugEnabled()) {
                        log.debug("Successfully initialized privateAssociation Store : "
                                +  privateAssociationStoreClassName);
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Private association store class : " + privateAssociationStoreClassName + " not found", e);
                } catch (InstantiationException e) {
                    log.error("Error while initializing association store class : " + privateAssociationStoreClassName, e);
                } catch (IllegalAccessException e) {
                    log.error("Error while initializing association store class : " + privateAssociationStoreClassName, e);
                } catch (Exception e) {
                    log.error("Error while initializing private association store", e);
                }
            }

            if(privateAssociations == null) {
                privateAssociations = new OpenIDServerAssociationStore(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE);
                if(log.isDebugEnabled()){
                    log.debug("Setting default OpenID Server Association Store: " +
                            OpenIDServerAssociationStore.class.getName());
                }
            }
        }

        super.setPrivateAssociations(privateAssociations);
    }

    @Override
    public Message authResponse(AuthRequest authReq,
                                String userSelId,
                                String userSelClaimed,
                                boolean authenticatedAndApproved,
                                String opEndpoint,
                                boolean signNow) {
        if(log.isDebugEnabled()) {
            log.info("Association handle in AuthRequest : " + authReq.getHandle());
        }
        return super.authResponse(authReq, userSelId, userSelClaimed, authenticatedAndApproved, opEndpoint, signNow);
    }
}

