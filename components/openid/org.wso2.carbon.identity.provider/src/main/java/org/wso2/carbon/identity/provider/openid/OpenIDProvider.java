/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.openid4java.server.ServerManager;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * This class is the WSO2 Implementation of OpenID Provider role.
 * This is a singleton class which handles all OpenID Server related
 * configurations and this class initiates the <code>ServerManager</code>
 *
 * @author WSO2 Inc
 */
public class OpenIDProvider {

    private static final Log log = LogFactory.getLog(OpenIDProvider.class);
    // Guaranteed to be thread safe
    private static volatile OpenIDProvider provider;
    // Instantiate a ServerManager object.
    private ServerManager manager = new OpenIDServerManager();
    private String opAddress;

    /**
     * Configure the OpenID Provider's end-point URL
     */
    private OpenIDProvider() {
        // This is the OpenID provider server URL
        opAddress = OpenIDUtil.getOpenIDServerURL();
        // The URL which accepts OpenID Authentication requests, obtained by
        // performing discovery on the the User-Supplied Identifier. This value
        // must be an absolute URL
        manager.setOPEndpointUrl(opAddress);

        // default association expiry time is set to 15 minutes
        int assocExpiryTime = 15;
        String expiryTime = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_ASSOCIATION_EXPIRY_TIME);
        if (expiryTime != null && !expiryTime.trim().isEmpty()) {
            try {
                assocExpiryTime = Integer.parseInt(expiryTime);
            } catch (NumberFormatException e) {
                log.warn("Error while setting association expiry time as " + expiryTime
                        +  ". Setting association expiry time to default ("+assocExpiryTime+")", e);
            }
        }
        manager.setExpireIn(assocExpiryTime);
    }

    /**
     * @return an instance of the OpenIDProvider
     */
    public static OpenIDProvider getInstance(){
        if (provider == null) {
            synchronized (OpenIDProvider.class) {
                if (provider == null) {
                    provider = new OpenIDProvider();
                }
            }
        }
        return provider;
    }

    /**
     * @return OpenID Provider server URL.
     */
    public String getOpAddress() {
        return opAddress;
    }

    /**
     * @return ServerManager instance.
     */
    public ServerManager getManager() {
        return manager;
    }

}
