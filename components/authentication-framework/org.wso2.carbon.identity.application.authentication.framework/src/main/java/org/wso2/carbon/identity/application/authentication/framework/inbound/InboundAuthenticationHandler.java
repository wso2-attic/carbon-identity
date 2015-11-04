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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class InboundAuthenticationHandler {

    public static final String SESSION_DATA_KEY = "sessionDataKey";
    private static final Log log = LogFactory.getLog(InboundAuthenticationHandler.class);

    /**
     * Get
     * @return Inbound authentication request processor
     */
    private InboundAuthenticatorRequestProcessor getInboundRequestProcessor(HttpServletRequest req,
            HttpServletResponse resp) {
        List<InboundAuthenticatorRequestProcessor> requestProcessors = FrameworkServiceDataHolder.getInstance()
                .getInboundRequestProcessors();

        for (InboundAuthenticatorRequestProcessor requestProcessor : requestProcessors) {
            if (requestProcessor.canHandle(req, resp)) {
                return requestProcessor;
            }
        }
        return null;
    }

    private InboundAuthenticatorResponseBuilder getInboundResponseBuilder(HttpServletRequest req,
            HttpServletResponse resp) {
        List<InboundAuthenticatorResponseBuilder> responseBuilders = FrameworkServiceDataHolder.getInstance()
                .getInboundResponseBuilders();

        for (InboundAuthenticatorResponseBuilder responseBuilder : responseBuilders) {
            if (responseBuilder.canHandle(req, resp)) {
                return responseBuilder;
            }
        }
        return null;
    }

    //buildAuthenticationRequest() new method InboundAutheticationRequest, need to have a different class with can handle, name, priotity to get builder

    public void process(HttpServletRequest req, HttpServletResponse resp) throws FrameworkException {

        String sessionDataKey = req.getParameter(SESSION_DATA_KEY);
        if (sessionDataKey == null) {
            InboundAuthenticatorRequestProcessor requestProcessors = getInboundRequestProcessor(req, resp);
            if (requestProcessors != null) {
                requestProcessors.process(req, resp);
            } else {
                log.warn("No request processor available for process");
            }
        } else {
            //Response from common authentication framework.
            InboundAuthenticatorResponseBuilder responseBuilder = getInboundResponseBuilder(req, resp);
            if (responseBuilder != null) {
                responseBuilder.buildResponse(req, resp, sessionDataKey);
            } else {
                log.warn("No response processor available for process");
            }
        }

    }
}
