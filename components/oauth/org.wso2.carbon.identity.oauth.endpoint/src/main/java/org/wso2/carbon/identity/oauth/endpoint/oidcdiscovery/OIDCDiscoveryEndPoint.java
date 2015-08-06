/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.oidcdiscovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.oauth.endpoint.oidcdiscovery.impl.OIDProviderJSONResponseBuilder;
import org.wso2.carbon.identity.oidcdiscovery.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/oidcdiscovery")
public class OIDCDiscoveryEndPoint {

    public static final String WELL_KNOWN_RESOURCE = "/.well-known";
    public static final String OPENID_CONFIGURATION_RESOURCE = WELL_KNOWN_RESOURCE + "/openid-configuration";
    public static final String WEBFINGER_RESOURCE = WELL_KNOWN_RESOURCE + "/webfinger";
    private static final Log log = LogFactory.getLog(OIDCDiscoveryEndPoint.class);

    @GET
    @Path(OPENID_CONFIGURATION_RESOURCE)
    @Produces("application/json")
    public Response getOIDProviderConfiguration(@Context HttpServletRequest request) {
        String response = null;
        log.warn("I am in the config.");
        OIDCService processor = OIDCService.getInstance();
        try {
            OIDProviderRequestValidator requestBuilder = new OIDProviderRequestBuilder();
            OIDProviderRequestDTO providerRequest = requestBuilder.validateRequest(request);
            OIDProviderResponseBuilder responseBuilder = new OIDProviderJSONResponseBuilder();
            response = responseBuilder.getOIDProviderConfigString(processor.getOIDProviderConfig
                    (providerRequest));
        } catch (OIDCDiscoveryEndPointException e) {
            return processor.handleError(e);
        } catch (ServerConfigurationException e){

        }
        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_OK);
        return responseBuilder.entity(response).build();
    }


}
