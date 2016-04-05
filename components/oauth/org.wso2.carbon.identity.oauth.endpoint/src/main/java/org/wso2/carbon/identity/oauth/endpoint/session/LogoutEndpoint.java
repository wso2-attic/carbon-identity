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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.CommonAuthenticationHandler;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;
import org.wso2.carbon.identity.application.authentication.framework.model.CommonAuthRequestWrapper;
import org.wso2.carbon.identity.application.authentication.framework.model.CommonAuthResponseWrapper;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.oauth.cache.SessionDataCache;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheEntry;
import org.wso2.carbon.identity.oauth.cache.SessionDataCacheKey;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.Enumeration;

@Path("/end_session_endpoint")
public class LogoutEndpoint {
    private static Log log = LogFactory.getLog(LogoutEndpoint.class);

    @GET
    @Path("/")
    @Produces("text/html")
    public String logout(@Context HttpServletRequest request, @QueryParam("id_token_hint") String id_token_hint,
                         @Context HttpServletResponse resp, @QueryParam("isLogout") String isLogoutRequest) {
        try {
            if (("true").equals(isLogoutRequest)) {
                removeStatusCookie(request, resp);
                sendFrameworkForLogout(request, resp, true);
            } else if (("false").equals(isLogoutRequest)) {
                sendResponseToRP();
            } else {
                return OAuthConstants.LOGOUT_HTML.replace("*", " " + EndpointUtil.getRpInitiatedLogoutUrl());
            }

        } catch (ServletException e) {
            log.error("Error when sending the logout request to the Framework");
        } catch (IOException e) {
            log.error("Error when sending the logout request to the Framework");
        }
        return StringUtils.EMPTY;
    }
    public static void removeStatusCookie(HttpServletRequest req, HttpServletResponse resp) {

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(OAuthConstants.STATUSCOOKIE)) {
                    cookie.setMaxAge(0);
                    resp.addCookie(cookie);
                    break;
                }
            }
        }
    }

    private void sendFrameworkForLogout(HttpServletRequest request, HttpServletResponse response,
                                          boolean isPost) throws ServletException, IOException {

        String sessionDataKey = UUIDGenerator.generateUUID();
        addSessionDataToCache(sessionDataKey);
        String selfPath = "/oauth2/end_session_endpoint?logout=false";

        //Add all parameters to authentication context before sending to authentication
        // framework
        AuthenticationRequest authenticationRequest = new
                AuthenticationRequest();
        authenticationRequest.addRequestQueryParam(FrameworkConstants.RequestParams.LOGOUT,
                new String[]{"true"});
        authenticationRequest.setRequestQueryParams(request.getParameterMap());
        authenticationRequest.setCommonAuthCallerPath(selfPath);
        authenticationRequest.setPost(isPost);
        authenticationRequest.appendRequestQueryParams(request.getParameterMap());
        //Add headers to AuthenticationRequestContext
        for (Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
            String headerName = e.nextElement().toString();
            authenticationRequest.addHeader(headerName, request.getHeader(headerName));
        }

        AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry
                (authenticationRequest);
        addAuthenticationRequestToRequest(request, authRequest);
        sendRequestToFramework(request, response, sessionDataKey, FrameworkConstants.RequestType.CLAIM_TYPE_SAML_SSO);
    }

    private void addAuthenticationRequestToRequest(HttpServletRequest request,AuthenticationRequestCacheEntry authRequest){
        request.setAttribute(FrameworkConstants.RequestAttribute.AUTH_REQUEST, authRequest);
    }

    private void sendRequestToFramework(HttpServletRequest request, HttpServletResponse response, String sessionDataKey,
                                        String type)
            throws ServletException, IOException {

        CommonAuthenticationHandler commonAuthenticationHandler = new CommonAuthenticationHandler();

        CommonAuthRequestWrapper requestWrapper = new CommonAuthRequestWrapper(request);
        requestWrapper.setParameter(FrameworkConstants.SESSION_DATA_KEY, sessionDataKey);
        requestWrapper.setParameter(FrameworkConstants.RequestParams.TYPE, type);

        CommonAuthResponseWrapper responseWrapper = new CommonAuthResponseWrapper(response);
        commonAuthenticationHandler.doGet(requestWrapper, responseWrapper);

        Object object = request.getAttribute(FrameworkConstants.RequestParams.FLOW_STATUS);
        if (object != null) {
            AuthenticatorFlowStatus status = (AuthenticatorFlowStatus) object;
            if (status == AuthenticatorFlowStatus.INCOMPLETE) {
                response.sendRedirect(responseWrapper.getRedirectURL());
            }
        }
    }

    private void addSessionDataToCache(String sessionDataKey) {
        SessionDataCacheKey cacheKey = new SessionDataCacheKey(sessionDataKey);
        SessionDataCacheEntry cacheEntry = new SessionDataCacheEntry();
        SessionDataCache.getInstance().addToCache(cacheKey, cacheEntry);
    }

    private void sendResponseToRP(){
    //todo: The logout response should be sent to the RP
    }

}
