
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
package org.wso2.carbon.identity.oauth.endpoint.session;

import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
/**
 * This class consists OpenID Connect Endpoint which returns the login status of the IS server
 * */
@Path("/check_session_iframe")
public class OIDCSessionMgtEndpoint {
    private static String callBackurl = null;

    @GET
    @Path("/")
    @Produces("text/html")
    public String getLoginStatus(@Context HttpServletRequest request, @Context HttpServletResponse resp) {

        callBackurl = getCallBackUrl();
        return "<html><head><script src=\"https://crypto-js.googlecode.com/svn/tags/3.0.2/build/rollups/sha256.js\"></script>" +
                "<script> window.addEventListener ('message', OnMessage, false); " +
                "function OnMessage (event) {\n" +
                "if (!event.origin.indexOf('" + callBackurl + "')<0){"+
                "return;" +
                "}"+
                "var salt = event.data.split('.')[1].split(' ')[0];" +
                " var client_id=event.data.split(' ')[1];" +
                " var opss='" + getCookie(request) +
                "';var saltMaker=client_id + event.origin + opss + salt;" +
                "var stat;" +
                "console.log(\"Receiving the Polling Message From RP Client:.............\"+saltMaker);\n" +
                " var ss = CryptoJS.SHA256(saltMaker) + \".\" + salt+\" \"+client_id;" +
                "   if (event.data == ss) {\n" +
                "      stat = 'unchanged';\n" +
                "    } else {\n" +
                "      stat = 'changed';\n" +
                "    }" +
                " console.log (\"Sending Response to the RP Client\");\n" +
                "                parent.postMessage(stat, '" + callBackurl + "');\n" +
                "        }</script></head><body></body></html>";
    }

    private static String getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if (OAuthConstants.STATUSCOOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static void setCallBackUrl(OAuth2AuthorizeReqDTO authzReqDTO) {
        callBackurl = authzReqDTO.getCallbackUrl();
    }

    private String getCallBackUrl() {
               return callBackurl;
            }

}
