/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.provider.openid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.Message;
import org.openid4java.server.ServerAssociationStore;
import org.openid4java.server.ServerManager;

/**
 * This class passes OpenID messages such as OpenID Association, OpenID Request
 * and Response messages to the ServerManager class of the openid4java library.
 * However the purpose of this class is to force the super class to use the
 * custom AssociationStore instead the default InMemoryAssociationStores.
 * 
 * @author WSO2 Inc.
 * 
 */
public class OpenIDServerManager extends ServerManager {
	
	protected Log log = LogFactory.getLog(OpenIDServerManager.class);

	/**
	 * Here we set our AssociationStore implementation to the parent.
	 * 
	 * @param dbConnection
	 *            for the identity database
	 */
	public OpenIDServerManager() {
		/**
	     * Keeps track of the associations established with consumer sites.
	     */
	    ServerAssociationStore sharedAssociations = new OpenIDServerAssociationStore(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_SHARED);
	    super.setSharedAssociations(sharedAssociations);

	    /**
	     * Keeps track of private (internal) associations created for signing
	     * authentication responses for stateless consumer sites.
	     */
	    ServerAssociationStore privateAssociations = new OpenIDServerAssociationStore(OpenIDServerConstants.ASSOCIATION_STORE_TYPE_PRIVATE);
	    super.setPrivateAssociations(privateAssociations);
	}
	
	public Message authResponse(AuthRequest authReq,
            String userSelId,
            String userSelClaimed,
            boolean authenticatedAndApproved,
            String opEndpoint,
            boolean signNow) {
		log.info("Association handle in AuthRequest : " + authReq.getHandle());
		return super.authResponse(authReq, userSelId, userSelClaimed, authenticatedAndApproved, opEndpoint, signNow);
	}
}

