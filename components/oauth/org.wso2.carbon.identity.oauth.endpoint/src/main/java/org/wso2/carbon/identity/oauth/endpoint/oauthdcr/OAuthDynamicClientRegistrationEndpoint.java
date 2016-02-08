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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.dcr.DynamicClientRegistrationException;
import org.wso2.carbon.identity.oauth.dcr.DynamicClientRegistrationService;
import org.wso2.carbon.identity.oauth.dcr.OAuthApplicationInfo;
import org.wso2.carbon.identity.oauth.dcr.dto.FaultResponse;
import org.wso2.carbon.identity.oauth.dcr.profile.RegistrationProfile;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

@Path("/register")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OAuthDynamicClientRegistrationEndpoint {

    private static final Log log = LogFactory.getLog(OAuthDynamicClientRegistrationEndpoint.class);
    private static final String BASIC_AUTH_HEADER = "Basic";

    @POST
    public Response register(RegistrationProfile profile, @Context HttpServletRequest request) {
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
        boolean isClientAuthenticated = isAuthenticated(request);
        if (!isClientAuthenticated) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Unauthorized user").build();
        }
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
    public Response unRegister(@QueryParam("applicationName") String applicationName,
            @QueryParam("userId") String userId, @QueryParam("consumerKey") String consumerKey,
            @Context HttpServletRequest request) {
        Response response;
        boolean isClientAuthenticated = isAuthenticated(request);
        if (!isClientAuthenticated) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Unauthorized user").build();
        }
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

    /**
     * This method checks whether the authorization header is present in the request, and if present, whether it is
     * for Basic Authentication
     *
     * @param authorizationHeader - Authorization header sent in the request
     * @return Whether the authorization header is present and can be handled
     */
    private static boolean canHandle(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.contains(BASIC_AUTH_HEADER)) {
            return true;
        } else {
            log.error("Authorization header is either not present in the request or cannot be handled");
            return false;
        }
    }

    /**
     * This method will perform Basic Authentication for the user credentials supplied in the request
     *
     * @param request The HttpServletRequest
     * @return Whether the user is authenticated or not
     */
    private static boolean isAuthenticated(@Context HttpServletRequest request) {
        //get the authorization header from the request
        String authorizationHeader = request.getHeader(OAuthConstants.HTTP_REQ_HEADER_AUTHZ);
        if (canHandle(authorizationHeader)) {
            //then decode the header and extract the username and the password
            String[] tempArr = authorizationHeader.split(" ");
            if (tempArr.length == 2) {
                String decodedBasicAuthHeader = null;
                String userName = null;
                String password = null;
                try {
                    decodedBasicAuthHeader = new String(
                            Base64.decodeBase64(tempArr[1].getBytes(Charset.forName("UTF-8"))), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    String msg = "Unsupported Encoding";
                    log.error(msg, e);
                }
                if (decodedBasicAuthHeader != null) {
                    tempArr = decodedBasicAuthHeader.split(":");
                    if (tempArr.length == 2) {
                        userName = tempArr[0];
                        password = tempArr[1];
                    }
                }
                if (userName != null && password != null) {
                    String tenantDomain = MultitenantUtils.getTenantDomain(userName);
                    String tenantLessUserName = MultitenantUtils.getTenantAwareUsername(userName);

                    try {
                        // get super tenant context and get realm service which is an osgi service
                        RealmService realmService = (RealmService) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                .getOSGiService(RealmService.class, null);
                        if (realmService != null) {
                            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                            if (tenantId == -1) {
                                log.error("Invalid tenant domain " + tenantDomain);
                                return false;
                            }
                            // get tenant's user realm
                            UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                            boolean authenticated = userRealm.getUserStoreManager()
                                    .authenticate(tenantLessUserName, password);
                            if (authenticated) {
                                return true;
                            } else {
                                DynamicClientRegistrationException unauthorizedException = new DynamicClientRegistrationException(
                                        "Authentication failed for the user: " + tenantLessUserName + "@"
                                                + tenantDomain);
                                log.error(unauthorizedException.getErrorMessage());
                                return false;
                            }
                        } else {
                            log.error("Error in getting Realm Service for user: " + userName);
                            DynamicClientRegistrationException internalServerException = new DynamicClientRegistrationException(
                                    "Internal server error while authenticating the user: " + tenantLessUserName + "@"
                                            + tenantDomain);
                            log.error(internalServerException.getErrorMessage());
                            return false;
                        }

                    } catch (UserStoreException e) {
                        DynamicClientRegistrationException internalServerException = new DynamicClientRegistrationException(
                                "Internal server error while authenticating the user.");
                        log.error(internalServerException.getErrorMessage(), e);
                        return false;
                    }
                } else {
                    DynamicClientRegistrationException unauthorizedException = new DynamicClientRegistrationException(
                            "Authentication required for this resource. Username or password not provided.");
                    log.error(unauthorizedException.getErrorMessage());
                    return false;
                }
            } else {
                DynamicClientRegistrationException internalServerException = new DynamicClientRegistrationException(
                        "Invalid Authorization Header in the request.");
                log.error(internalServerException.getErrorMessage());
                return false;
            }
        }
        return false;
    }
}
