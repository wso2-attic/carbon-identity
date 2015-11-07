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

import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class CommonInboundAuthenticationServlet extends HttpServlet {

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

            InboundAuthenticationResponse result = new InboundAuthenticationManager().process(request, response);
            if(result.getRedirectURL() != null){

            }
        } catch (FrameworkException ex) {
            throw new ServletException(ex);
        }
    }
}
