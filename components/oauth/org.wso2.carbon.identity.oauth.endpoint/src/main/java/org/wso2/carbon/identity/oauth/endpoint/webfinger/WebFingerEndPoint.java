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
package org.wso2.carbon.identity.oauth.endpoint.webfinger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.webfinger.WebFingerEndPointException;
import org.wso2.carbon.identity.webfinger.WebFingerProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

//Finalize this.
//Do we need an extended endpoint? Can't we provide all the methods in the same end point? Verify.
@Path("/webfinger")
public class WebFingerEndPoint {
    private static final Log log = LogFactory.getLog(WebFingerEndPoint.class);
    public static final String OPENID_CONNETCT_ISSUER_REL = "http://openid.net/specs/connect/1.0/issuer";

    @GET
    @Path("/.well-known/webfinger?resource={resource}&rel="+OPENID_CONNETCT_ISSUER_REL)
    @Produces("application/json")
    public Response getOIDProviderIssuer(@Context HttpServletRequest request, @PathParam("resource") String
            resource) {
        WebFingerProcessor processor = EndpointUtil.getWebFingerService();
        String response = null;
        try {
            processor.validateRequest(request, resource, OPENID_CONNETCT_ISSUER_REL);
            processor.getWebFingerResponse();
        }catch(WebFingerEndPointException e){


        }
//        try {
//            processor.validateRequest(request, resource, rel);
//            OIDProviderResponseBuilder responseBuilder = new OIDProviderJSONResponseBuilder();
//             //response = responseBuilder.getOIDProviderConfigString(processor.getOIDProviderConfig
//             //       ());
//        } catch (OIDCDiscoveryEndPointException e) {
//            //Response.ResponseBuilder errorResponse = Response.status(processor.handleError(e));
//            //return errorResponse.entity(e.getMessage()).build();
//        } catch (ServerConfigurationException e) {
//            Response.ResponseBuilder errorResponse = Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            return errorResponse.entity("Error in reading configuration.").build();
//        }
        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_OK);
        return responseBuilder.entity(response).build();
    }
}
