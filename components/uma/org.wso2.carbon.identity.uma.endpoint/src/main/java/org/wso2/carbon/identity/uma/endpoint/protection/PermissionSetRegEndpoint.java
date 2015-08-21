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
import org.wso2.carbon.identity.uma.beans.protection.PermissionTicketReqBean;
import org.wso2.carbon.identity.uma.dto.UmaPermissionSetRegRequest;
import org.wso2.carbon.identity.uma.dto.UmaPermissionSetRegResponse;
import org.wso2.carbon.identity.uma.dto.UmaResponse;
import org.wso2.carbon.identity.uma.endpoint.util.EndpointUtil;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/protect/permission")
@Produces("application/json")
public class PermissionSetRegEndpoint {

    private static final Log log = LogFactory.getLog(PermissionSetRegEndpoint.class);

    @POST
    @Path("/")
    public Response issuePermissionTicket
            (@Context HttpServletRequest httpServletRequest, PermissionTicketReqBean ticketReqBean) {

        // check the validity of the PAT
        try {
            validateAuthorization(httpServletRequest);
        } catch (IdentityUMAException e) {
            // build and error message and return
            return EndpointUtil.buildOAuthErrorMessage(e.getMessage());
        }

        // create the UMA permission set request
        UmaPermissionSetRegRequest umaPermissionSetRegRequest =
                new UmaPermissionSetRegRequest(httpServletRequest,ticketReqBean);

        // call the backend service
        UmaResponse umaPermissionTicketReqResponse =
                EndpointUtil.getUMAService().createPermissionTicket(umaPermissionSetRegRequest);


        return EndpointUtil.buildResponse(umaPermissionTicketReqResponse);
    }


    private void validateAuthorization(HttpServletRequest httpServletRequest) throws IdentityUMAException {
        EndpointUtil.checkAuthorization(httpServletRequest, UMAConstants.UMA_PROTECTION_API_SCOPE);
    }

}
