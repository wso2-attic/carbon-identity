/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.endpoint.oauthdcr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.dcr.DynamicClientRegistrationException;
import org.wso2.carbon.identity.oauth.dcr.DynamicClientRegistrationService;
import org.wso2.carbon.identity.oauth.dcr.OAuthApplicationInfo;
import org.wso2.carbon.identity.oauth.dcr.dto.FaultResponse;
import org.wso2.carbon.identity.oauth.dcr.profile.RegistrationProfile;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/oauthdcr")
public class OAuthDynamicClientRegistrationEndpoint {

    private static final Log log = LogFactory.getLog(OAuthDynamicClientRegistrationEndpoint.class);

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegistrationProfile profile) {
        /**
         * sample message to this method
         * {
         * "callbackUrl": "www.google.lk",
         * "clientName": "mdm",
         * "tokenScope": "Production",
         * "owner": "admin",
         * "grantType": "password refresh_token",
         * "saasApp": true
         *}
         */
        Response response;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext()
                    .setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            DynamicClientRegistrationService dynamicClientRegistrationService = EndpointUtil
                    .getDynamicClientRegistrationService();
            if (dynamicClientRegistrationService != null) {
                OAuthApplicationInfo info = dynamicClientRegistrationService.registerOAuthApplication(profile);
                return Response.status(Response.Status.CREATED).entity(info.toString()).build();
            }
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity("Dynamic Client Registration Service not available.").build();
        } catch (DynamicClientRegistrationException e) {
            String msg = "Error occurred while registering client '" + profile.getClientName() + "'";
            log.error(msg, e);
            response = Response.status(Response.Status.BAD_REQUEST)
                    .entity(new FaultResponse(DynamicClientRegistrationService.ErrorCode.INVALID_CLIENT_METADATA, msg))
                    .build();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return response;
    }

    @DELETE
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unRegister(@QueryParam("applicationName") String applicationName,
            @QueryParam("userId") String userId, @QueryParam("consumerKey") String consumerKey) {
        Response response;
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext()
                    .setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            DynamicClientRegistrationService dynamicClientRegistrationService = EndpointUtil.
                    getDynamicClientRegistrationService();
            if (dynamicClientRegistrationService != null) {
                boolean status = dynamicClientRegistrationService
                        .unregisterOAuthApplication(userId, applicationName, consumerKey);
                if (status) {
                    return Response.status(Response.Status.OK).build();
                }
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity("Dynamic Client Registration Service not available.").build();
        } catch (DynamicClientRegistrationException e) {
            String msg = "Error occurred while un-registering client '" + applicationName + "'";
            log.error(msg, e);
            response = Response.serverError()
                    .entity(new FaultResponse(DynamicClientRegistrationService.ErrorCode.INVALID_CLIENT_METADATA, msg))
                    .build();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return response;
    }

}
