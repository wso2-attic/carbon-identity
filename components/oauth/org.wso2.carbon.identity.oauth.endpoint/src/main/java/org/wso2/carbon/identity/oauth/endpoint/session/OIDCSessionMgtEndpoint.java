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

import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
/**
 * This class consists OpenID Connect Endpoint which returns the login status of the IS server
 */
@Path("/loginstatus")
public class OIDCSessionMgtEndpoint {
    static String callBackurl = null;

    @GET
    @Path("/")
    @Produces("text/html")
    public String getLoginStatus(@Context HttpServletRequest request) {
        String commonAuthCookie;
        String loginStatus = OAuthConstants.OP_STATUS_LOGOUT;
        SessionContext sessionContext = null;
        String callBackUrl = null;
        if (FrameworkUtils.getAuthCookie(request) != null) {
            commonAuthCookie = FrameworkUtils.getAuthCookie(request).getValue();
            if (commonAuthCookie != null) {
                sessionContext = FrameworkUtils.getSessionContextFromCache(commonAuthCookie);
            }
            callBackUrl = getCallBackUrl();
            //when the IS server logs out the sessionContext becomes null
            if (sessionContext != null) {
                loginStatus = OAuthConstants.OP_STATUS_LOGGED;
            } else {
                loginStatus = OAuthConstants.OP_STATUS_LOGOUT;
            }
        }
        if (callBackUrl != null) {
            return "<html><head><script> window.addEventListener ('message', OnMessage, false); function OnMessage (event) {\n" +
                    "            console.log(\"Receiving the Polling Message From RP Client:.............\");\n" +
                    "            if (event.origin.indexOf('" + callBackUrl + "')<0) {\n" +

                    "                parent.postMessage('" + loginStatus + "', '" + callBackUrl + "');\n" +
                    "            }\n" +
                    "            else{\n" +
                    "                return;\n" +
                    "            }\n" +
                    "\n" +
                    "        }</script></head><body></body></html>";
        } else {
            return "<html><head><script>window.addEventListener ('message', OnMessage, false); function OnMessage (event){\n" +
                    "console.log(\"Please log IS server to communicate:.............\");\n" +
                    "} </script></head> </html>";
        }
    }

    /**
     *
     * @param authzReqDTO which contains Authorize details
     */
    public void setCallBackUrl(OAuth2AuthorizeReqDTO authzReqDTO) {
        this.callBackurl = authzReqDTO.getCallbackUrl();
    }

    /**
     *
     * @return callBackurl of the RP
     */
    public String getCallBackUrl() {
        return callBackurl;
    }
}

