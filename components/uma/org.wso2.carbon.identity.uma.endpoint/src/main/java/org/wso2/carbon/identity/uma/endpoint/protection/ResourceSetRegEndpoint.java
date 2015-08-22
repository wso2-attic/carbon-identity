/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma.endpoint.protection;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.UMAConstants;
import org.wso2.carbon.identity.uma.beans.protection.ResourceSetDescriptionBean;
import org.wso2.carbon.identity.uma.dto.UmaResourceSetRegRequest;
import org.wso2.carbon.identity.uma.dto.UmaResponse;
import org.wso2.carbon.identity.uma.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/protect/resource_reg")
@Produces("application/json")
public class ResourceSetRegEndpoint {

    private static final Log log = LogFactory.getLog(ResourceSetRegEndpoint.class);

    @POST
    @Path("/")
    @Consumes("application/json")
    public Response createResourceSet
            (@Context HttpServletRequest httpServletRequest, ResourceSetDescriptionBean resourceSetDescription) {

        try {
            //  Check Authorization to access the API
            validateAuthorization(httpServletRequest);
        } catch (IdentityUMAException e) {
            // build and error message and return
            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
        }

        // create UMAResourceSetRegistration Request
        UmaResourceSetRegRequest umaResourceSetRegRequest =
                new UmaResourceSetRegRequest(httpServletRequest, resourceSetDescription);

        // get the response
        UmaResponse umaResponse;
        try {
            umaResponse = EndpointUtil.getUMAService().createResourceSet(umaResourceSetRegRequest);
        } catch (IdentityUMAException e) {
            umaResponse = UmaResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                    .setError(e.getMessage()).buildJSONResponse();
        }

        // build Servlet Response from UMAResponse
        return EndpointUtil.buildResponse(umaResponse);
    }

    @GET
    @Path("/{rsid}")
    public Response getResourceSet
            (@Context HttpServletRequest httpServletRequest, @PathParam("rsid") String resourceSetId) {


        try {
            //  Check Authorization to access the API
            validateAuthorization(httpServletRequest);

        } catch (IdentityUMAException e) {
            // build and error message and return
            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
        }

        // create UMAResourceSetRegistration Request
        UmaResourceSetRegRequest umaResourceSetRegRequest = new UmaResourceSetRegRequest(httpServletRequest);
        umaResourceSetRegRequest.setResourceId(resourceSetId);

        UmaResponse umaResponse;
        try {
            umaResponse = EndpointUtil.getUMAService().getResourceSet(umaResourceSetRegRequest);
        } catch (IdentityUMAException e) {
            umaResponse = UmaResponse.errorResponse(e.getErrorStatus()).setError(e.getMessage()).buildJSONResponse();
        }

        return EndpointUtil.buildResponse(umaResponse);
    }

//    @PUT
//    @Path("/{rsid}")
//    @Consumes("application/json")
//    public Response updateResourceSet
//            (@Context HttpServletRequest httpServletRequest,
//             ResourceSetDescriptionBean resourceSetDescriptionBean,
//             @PathParam("rsid") String resourceSetId) {
//
//        try {
//            //  Check Authorization to access the API
//            validateAuthorization(httpServletRequest);
//
//        } catch (IdentityUMAException e) {
//            // build and error message and return
//            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
//        }
//
//
//        // create UMAResourceSetRegistration Request
//        UmaResourceSetRegRequest umaResourceSetRegRequest =
//                new UmaResourceSetRegRequest(httpServletRequest, resourceSetDescriptionBean);
//        umaResourceSetRegRequest.setResourceId(resourceSetId);
//
//
//        UmaResponse umaResponse = EndpointUtil.getUMAService().updateResourceSet(umaResourceSetRegRequest);
//        return EndpointUtil.buildResponse(umaResponse);
//    }


    @DELETE
    @Path("/{rsid}")
    public Response deleteResourceSet
            (@Context HttpServletRequest httpServletRequest, @PathParam("rsid") String resourceSetId) {

        try {
            //  Check Authorization to access the API
            validateAuthorization(httpServletRequest);

        } catch (IdentityUMAException e) {
            // build and error message and return
            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
        }

        // create UMAResourceSetRegistration Request
        UmaResourceSetRegRequest umaResourceSetRegRequest =
                new UmaResourceSetRegRequest(httpServletRequest);

        umaResourceSetRegRequest.setResourceId(resourceSetId);

        UmaResponse umaResponse;
        try {
            umaResponse = EndpointUtil.getUMAService().deleteResourceSet(umaResourceSetRegRequest);
        } catch (IdentityUMAException e) {
            umaResponse = UmaResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .setError(e.getMessage()).buildJSONResponse();

        }
        return EndpointUtil.buildResponse(umaResponse);
    }


    @GET
    @Path("/")
    public Response listResourceSets
            (@Context HttpServletRequest httpServletRequest) {

        // check Authorization
        try {
            validateAuthorization(httpServletRequest);
        } catch (IdentityUMAException e) {
            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
        }

        UmaResourceSetRegRequest umaResourceSetRegRequest =
                new UmaResourceSetRegRequest(httpServletRequest);

        UmaResponse umaResponse;
        try {
            umaResponse = EndpointUtil.getUMAService().getResourceSetIds(umaResourceSetRegRequest);
        } catch (IdentityUMAException e) {
            umaResponse = UmaResponse.errorResponse(e.getErrorStatus()).setError(e.getMessage()).buildJSONResponse();
        }

        return EndpointUtil.buildResponse(umaResponse);
    }


    private void validateAuthorization(HttpServletRequest httpServletRequest) throws IdentityUMAException {

        EndpointUtil.checkAuthorization(httpServletRequest, UMAConstants.UMA_PROTECTION_API_SCOPE);
    }


    private void logRequest() {
    }


}
