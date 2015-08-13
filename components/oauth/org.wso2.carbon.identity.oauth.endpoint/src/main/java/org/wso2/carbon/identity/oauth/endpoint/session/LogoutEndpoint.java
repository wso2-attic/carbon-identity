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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.session;

import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.CookieParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;


/**
 * This class consists OpenID Connect logout Endpoint
 */

@Path("/end_session_endpoint")
public class LogoutEndpoint {
    private static String sessionDataKey = null;

    /**
     * @param request
     * @param logoutNotification The logout notification coming from the RP
     * @param id_token_hint      The id_token_hint of RP
     */
    @GET
    @Path("/")
    @Produces("text/html")
    public String logout(@Context HttpServletRequest request, @QueryParam("logoutNotification") String logoutNotification,
                         @QueryParam("id_token_hint") String id_token_hint, @CookieParam("statusCookie") javax.ws.rs.core.Cookie cookie, @Context HttpServletResponse resp) {
        Response response;
        response = Response.status(OAuthConstants.STATUS_OK).entity(logoutNotification).build();
        if (OAuthConstants.RP_STATUS_LOGOUT.equalsIgnoreCase(response.getEntity().toString())) {
            return OAuthConstants.LOGOUT_HTML.replace("*", " " + EndpointUtil.getRpInitiatedLogoutUrl() + getSessionDataKey() + OAuthConstants.ID_TOKEN_HINT + id_token_hint);
        } else if (OAuthConstants.RP_STATUS_LOGGED.equalsIgnoreCase(response.getEntity().toString())) {
            return OAuthConstants.LOGOUT_HTML.replace("*", " " + EndpointUtil.getErrorPageURL(OAuth2ErrorCodes.INVALID_REQUEST, OAuthConstants.MSG, null, null));
        } else {
            return OAuthConstants.EMPTY_STRING;
        }
    }

    public void setSessionDataKey(String sessionDataKey) {
        this.sessionDataKey = sessionDataKey;
    }

    public String getSessionDataKey() {
        return sessionDataKey;
    }


}
