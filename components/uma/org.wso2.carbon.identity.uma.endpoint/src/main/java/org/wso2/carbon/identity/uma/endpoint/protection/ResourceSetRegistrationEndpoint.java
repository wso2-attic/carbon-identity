/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.endpoint.protection;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.UMAService;
import org.wso2.carbon.identity.uma.beans.protection.ResourceSetDescriptionBean;
import org.wso2.carbon.identity.uma.dto.UmaResourceSetRegistrationRequest;
import org.wso2.carbon.identity.uma.dto.UmaResourceSetRegistrationResponse;
import org.wso2.carbon.identity.uma.dto.UmaResponse;
import org.wso2.carbon.identity.uma.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.uma.util.UMAUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/protect/resource_reg")
public class ResourceSetRegistrationEndpoint {

    private static final Log log = LogFactory.getLog(ResourceSetRegistrationEndpoint.class);

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createResourceSet
            (@Context HttpServletRequest httpServletRequest,ResourceSetDescriptionBean resourceSetDescription){


        UmaResourceSetRegistrationRequest umaResourceSetRegistrationRequest =
                new UmaResourceSetRegistrationRequest(httpServletRequest);

        umaResourceSetRegistrationRequest.setResourceSetDescription(resourceSetDescription);
        umaResourceSetRegistrationRequest.setConsumerKey(UMAUtil.getConsumerKey(httpServletRequest));


        UmaResponse response =
                EndpointUtil.getUMAService().createResourceSet(umaResourceSetRegistrationRequest);

        // build Servlet Response from UMAResponse
        return UMAUtil.buildResponse(response);
    }

    @GET
    @Path("/{rsid}")
    @Produces("application/json")
    public Response getResourceSet
            (@Context HttpServletRequest httpServletRequest,@PathParam("rsid") int resourceSetId){
        log.info("Hit the get resource set method ----- id ---> "+resourceSetId);
        return null;
    }

    @PUT
    @Path("/{rsid}")
    @Produces("application/json")
    public Response updateResourceSet
            (@Context HttpServletRequest httpServletRequest,@PathParam("rsid") int resourceSetId){
        log.info("Hit the update resource set method ----- id ---> "+resourceSetId);
        return null;
    }

    @DELETE
    @Path("/{rsid}")
    public Response deleteResourceSet
            (@Context HttpServletRequest httpServletRequest, @PathParam("rsid") int resourceSetId){
        log.info("Hit the delete resource set method ----- id ---> "+resourceSetId);
        return null;
    }

    @GET
    @Path("/")
    @Produces("application/json")
    public Response listResourceSets
            (@Context HttpServletRequest httpServletRequest){
        log.info("Hit Listing All Resource Sets");
        return null;
    }

    /**
     * Method to validate the PAT(Protection API Token) ie. OAuth Access token with a scope "uma_authorization"
     * @param httpServletRequest
     * @return boolean true if the PAT is valid false otherwise
     */
    private boolean checkAuthorization(HttpServletRequest httpServletRequest){
        return true;
    }



    private void logRequest(){
    }


}
