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
package org.wso2.carbon.identity.application.authentication.framework.inbound;

import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CommonInboundAuthenticationServlet extends HttpServlet {

    private InboundAuthenticationManager inboundAuthenticationManager = new InboundAuthenticationManager();

    private InboundAuthenticationRequestBuilder getInboundRequestBuilder(HttpServletRequest req,
            HttpServletResponse resp) throws FrameworkException {
        List<InboundAuthenticationRequestBuilder> requestBuilders = FrameworkServiceDataHolder.getInstance()
                .getInboundAuthenticationRequestBuilders();

        for (InboundAuthenticationRequestBuilder requestBuilder : requestBuilders) {
            if (requestBuilder.canHandle(req, resp)) {
                return requestBuilder;
            }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {

            InboundAuthenticationRequestBuilder requestBuilder = getInboundRequestBuilder(request, response);
            InboundAuthenticationRequest authenticationRequest = requestBuilder.buildRequest(request, response);

            if(request.getPathInfo().contains(InboundAuthenticationConstants.HTTP_PATH_PARAM_REQUEST)){
                InboundAuthenticationResponse result = inboundAuthenticationManager.processRequest(
                        authenticationRequest);
            }else if(request.getPathInfo().contains(InboundAuthenticationConstants.HTTP_PATH_PARAM_RESPONSE)){
                InboundAuthenticationResponse result = inboundAuthenticationManager.processResponse(
                        authenticationRequest);
                if(result.getRedirectURL() != null){

                }
            }

        } catch (FrameworkException ex) {
            throw new ServletException(ex);
        }
    }

    protected void sendToFrameworkForAuthentication(HttpServletRequest req, HttpServletResponse resp,
            Map<String, String[]> newParams, InboundAuthenticationContext context,
            InboundAuthenticationRequest inboundAuthenticationRequest,
            InboundAuthenticationResponse inboundAuthenticationResponse) throws ServletException,
            IOException, IdentityApplicationManagementException, FrameworkException {

        String sessionDataKey = UUIDGenerator.generateUUID();
        String authName = inboundAuthenticationResponse.getInboundAuthenticationRequestProcessor().getName();
        String relyingParty = inboundAuthenticationResponse.getInboundAuthenticationRequestProcessor().getRelyingPartyId();
        String callbackPath = inboundAuthenticationResponse.getInboundAuthenticationRequestProcessor().getCallbackPath(context);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();

        Map<String, String[]> OldParams = req.getParameterMap();
        Iterator<Map.Entry<String, String[]>> iterator = OldParams.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String[]> pair = iterator.next();
            newParams.put(pair.getKey(), pair.getValue());
        }

        newParams.put(FrameworkConstants.SESSION_DATA_KEY, new String[] { sessionDataKey });
        newParams.put("type", new String[] { authName });

        authenticationRequest.appendRequestQueryParams(newParams);

        for (@SuppressWarnings("rawtypes")
            Enumeration e = req.getHeaderNames(); e.hasMoreElements();) {
            String headerName = e.nextElement().toString();
            authenticationRequest.addHeader(headerName, req.getHeader(headerName));
        }

        authenticationRequest.setRelyingParty(relyingParty);
        authenticationRequest.setType(authName);
        authenticationRequest.setCommonAuthCallerPath(URLEncoder.encode(callbackPath, "UTF-8"));

        AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry(authenticationRequest);
        FrameworkUtils.addAuthenticationRequestToCache(sessionDataKey, authRequest);
        String queryParams = "?sessionDataKey=" + sessionDataKey + "&" + "type" + "=" + authName;

        String commonAuthURL = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH, true);

        if (inboundAuthenticationResponse.getInboundAuthenticationRequestProcessor().isDirectResponseRequired()) {
            FrameworkUtils.getRequestCoordinator().handle(req, resp);
        } else {
            resp.sendRedirect(commonAuthURL + queryParams);
        }
    }
}
