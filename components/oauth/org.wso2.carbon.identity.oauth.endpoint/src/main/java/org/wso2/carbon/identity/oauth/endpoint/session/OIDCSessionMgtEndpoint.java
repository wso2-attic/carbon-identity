/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.impl.DefaultRequestCoordinator;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;


@Path("/session")
public class OIDCSessionMgtEndpoint {
    private static Log log = LogFactory.getLog(OIDCSessionMgtEndpoint.class);

    @GET
    @Path("/")
    @Produces("text/plain")

    public boolean getLoginStatus(@Context HttpServletRequest request) throws URISyntaxException {
        String commonAuthCookie = null;
        boolean loginStatus = false;
        org.wso2.carbon.identity.application.authentication.framework.context.SessionContext sessionContext = null;

       if (FrameworkUtils.getAuthCookie(request) != null) {
            commonAuthCookie = FrameworkUtils.getAuthCookie(request).getValue();
        }
        if (commonAuthCookie != null) {
            sessionContext = FrameworkUtils.getSessionContextFromCache(commonAuthCookie);
        }
        if (sessionContext != null) {
            Map<String, AuthenticatedIdPData> authenticatedIDPMap = sessionContext.getAuthenticatedIdPs();
            if (authenticatedIDPMap.size() > 0) {
                Iterator iterator = authenticatedIDPMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry pair = (Map.Entry) iterator.next();
                    AuthenticatedIdPData authenticatedIdPData = (AuthenticatedIdPData) pair.getValue();
                    if (authenticatedIdPData.getIdpName().equals(OAuthConstants.IDP_NAME)) {

                        loginStatus = true;
                        break;
                    } else {

                        loginStatus = false;
                    }
                }
            }
        }
        return loginStatus;
    }

}

