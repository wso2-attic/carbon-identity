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
package org.wso2.carbon.identity.scim.provider.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthenticationHandler;
import org.wso2.carbon.identity.scim.provider.auth.SCIMAuthenticatorRegistry;
import org.wso2.carbon.identity.scim.provider.util.JAXRSResponseBuilder;
import org.wso2.charon.core.encoder.json.JSONEncoder;
import org.wso2.charon.core.exceptions.UnauthorizedException;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;

import javax.ws.rs.core.Response;

public class AuthenticationFilter implements RequestHandler {

    private static Log log = LogFactory.getLog(AuthenticationFilter.class);

    public Response handleRequest(Message message, ClassResourceInfo classResourceInfo) {
        if (log.isDebugEnabled()) {
            log.debug("Authenticating SCIM request..");
        }
        SCIMAuthenticatorRegistry SCIMAuthRegistry = SCIMAuthenticatorRegistry.getInstance();
        if (SCIMAuthRegistry != null) {
            SCIMAuthenticationHandler SCIMAuthHandler = SCIMAuthRegistry.getAuthenticator(
                    message, classResourceInfo);
            boolean isAuthenticated = false;
            if (SCIMAuthHandler != null) {
                isAuthenticated = SCIMAuthHandler.isAuthenticated(message, classResourceInfo);
                if (isAuthenticated) {
                    return null;
                }
            }
        }
        //if null response is not returned(i.e:message continues its way to the resource), return error & terminate.
        UnauthorizedException unauthorizedException = new UnauthorizedException(
                "Authentication failed for this resource.");
        return new JAXRSResponseBuilder().buildResponse(
                AbstractResourceEndpoint.encodeSCIMException(new JSONEncoder(), unauthorizedException));
    }
}
