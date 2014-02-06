/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.openid4java.server.ServerManager;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

/**
 * This class is the WSO2 Implementation of OpenID Provider role.
 * This is a singleton class which handles all OpenID Server related
 * configurations and this class initiates the <code>ServerManager</code>
 * 
 * @author WSO2 Inc
 * 
 */
public class OpenIDProvider {

    // Instantiate a ServerManager object.
    private ServerManager manager = new OpenIDServerManager();

    private String opAddress;

    // Guaranteed to be thread safe
    private static OpenIDProvider provider = new OpenIDProvider();

    /**
     * Configure the OpenID Provider's end-point URL
     */
    private OpenIDProvider() {
        // This is the OpenID provider server URL
        opAddress = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_SERVER_URL);
        // The URL which accepts OpenID Authentication requests, obtained by
        // performing discovery on the the User-Supplied Identifier. This value
        // must be an absolute URL
        manager.setOPEndpointUrl(opAddress);
    }

    /**
     * @return an instance of the OpenIDProvider
    */
    public static OpenIDProvider getInstance() {
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
