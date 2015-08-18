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
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.oidcdiscovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.oidcdiscovery.OIDCProcessor;
import org.wso2.carbon.identity.oidcdiscovery.OIDProviderResponseBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/oidcdiscovery")
public class OIDCDiscoveryEndPoint {

    private static final Log log = LogFactory.getLog(OIDCDiscoveryEndPoint.class);

    @GET
    @Path("{tenant}/.well-known/openid-configuration")
    @Produces("application/json")
    public Response getOIDProviderConfiguration(@Context HttpServletRequest request,@PathParam("tenant") String tenant) {
        String response = null;
        OIDCProcessor processor = EndpointUtil.getOIDCService();
        try {
            processor.validateRequest(request,tenant);
            OIDProviderResponseBuilder responseBuilder = new OIDProviderJSONResponseBuilder();
            response = responseBuilder.getOIDProviderConfigString(processor.getOIDProviderConfig
                    ());
        } catch (OIDCDiscoveryEndPointException e) {
            Response.ResponseBuilder errorResponse = Response.status(processor.handleError(e));
            return errorResponse.entity(e.getMessage()).build();
        } catch (ServerConfigurationException e) {
            Response.ResponseBuilder errorResponse = Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return errorResponse.entity("Error in reading configuration.").build();
        }
        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_OK);
        return responseBuilder.entity(response).build();
    }
    @GET
    @Path("/.well-known/openid-configuration")
    @Produces("application/json")
    public Response getOIDProviderConfiguration(@Context HttpServletRequest request) {
        String response;
        OIDCProcessor processor = EndpointUtil.getOIDCService();
        try {
            processor.validateRequest(request,null);
            OIDProviderResponseBuilder responseBuilder = new OIDProviderJSONResponseBuilder();
            response = responseBuilder.getOIDProviderConfigString(processor.getOIDProviderConfig
                    ());
        } catch (OIDCDiscoveryEndPointException e) {
            Response.ResponseBuilder errorResponse = Response.status(processor.handleError(e));
            return errorResponse.entity(e.getErrorMessage()).build();
        } catch (ServerConfigurationException e) {
            Response.ResponseBuilder errorResponse = Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return errorResponse.entity("Error in reading configuration.").build();
        }
        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_OK);
        return responseBuilder.entity(response).build();
    }

}
