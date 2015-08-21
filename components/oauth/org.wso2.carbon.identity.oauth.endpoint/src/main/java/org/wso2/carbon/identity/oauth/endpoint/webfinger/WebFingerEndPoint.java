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
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.oauth.endpoint.webfinger.impl.JSOnResponseBuilder;
import org.wso2.carbon.identity.webfinger.WebFingerEndPointException;
import org.wso2.carbon.identity.webfinger.WebFingerProcessor;
import org.wso2.carbon.identity.webfinger.builders.ResponseBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

//Finalize this.
//Do we need an extended endpoint? Can't we provide all the methods in the same end point? Verify.
@Path("/webfinger")
public class WebFingerEndPoint {
    private static final Log log = LogFactory.getLog(WebFingerEndPoint.class);

    @GET
    @Path("/.well-known/webfinger")
    @Produces("application/json")
    public Response getOIDProviderIssuer(@Context HttpServletRequest request) {
        WebFingerProcessor processor = EndpointUtil.getWebFingerService();
        String response;
        try {
            ResponseBuilder responseBuilder = new JSOnResponseBuilder();
            response = responseBuilder.getOIDProviderIssuerString(processor.getResponse(request));
        } catch (WebFingerEndPointException e) {
            Response.ResponseBuilder errorResponse = Response.status(processor.handleError(e));
            return errorResponse.entity(e.getErrorMessage()).build();
        } catch (ServerConfigurationException e) {
            Response.ResponseBuilder errorResponse = Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return errorResponse.entity("Error in reading configuration.").build();
        }

        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_OK);
        return responseBuilder.entity(response).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
}
