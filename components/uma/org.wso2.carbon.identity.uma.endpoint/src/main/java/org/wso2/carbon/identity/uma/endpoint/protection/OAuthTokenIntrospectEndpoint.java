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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.UMAConstants;
import org.wso2.carbon.identity.uma.dto.UmaOAuthIntropectResponse;
import org.wso2.carbon.identity.uma.dto.UmaRequest;
import org.wso2.carbon.identity.uma.dto.UmaResponse;
import org.wso2.carbon.identity.uma.endpoint.UmaRequestWrapper;
import org.wso2.carbon.identity.uma.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Path("/protect/introspect")
public class OAuthTokenIntrospectEndpoint {

    private static final Log log = LogFactory.getLog(OAuthTokenIntrospectEndpoint.class);


    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response introspectToken
            (@Context HttpServletRequest httpServletRequest,MultivaluedMap<String, String> paramMap){

        try {
            validateAuthorization(httpServletRequest);
        } catch (IdentityUMAException e) {
            // build and error message and return
            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
        }

        HttpServletRequestWrapper httpRequest = new UmaRequestWrapper(httpServletRequest, paramMap);

        // create the UMA Request
        UmaRequest umaIntrospectRequest = new UmaRequest(httpRequest);

        // send the request to the UMAService and get the response
        try {
            UmaResponse introspectResponse =
                    EndpointUtil.getUMAService().introspectToken(umaIntrospectRequest);

            return EndpointUtil.buildResponse(introspectResponse);

        } catch (IdentityUMAException e) {
            log.error("Error when processing the request : ",e);
            return Response.serverError().build();
        }

    }


    private void validateAuthorization(HttpServletRequest httpServletRequest) throws IdentityUMAException {
        EndpointUtil.checkAuthorization(httpServletRequest, UMAConstants.UMA_PROTECTION_API_SCOPE);
    }
}
