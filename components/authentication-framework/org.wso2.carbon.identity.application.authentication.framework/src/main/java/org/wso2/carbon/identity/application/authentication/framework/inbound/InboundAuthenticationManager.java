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
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCache;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheKey;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceDataHolder;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class InboundAuthenticationManager {

    private static final Log log = LogFactory.getLog(InboundAuthenticationManager.class);

    /**
     * Get
     * @return Inbound authentication request processor
     */
    private InboundAuthenticationRequestProcessor getInboundRequestProcessor(HttpServletRequest req,
            HttpServletResponse resp) throws FrameworkException{
        List<InboundAuthenticationRequestProcessor> requestProcessors = FrameworkServiceDataHolder.getInstance()
                .getInboundRequestProcessors();

        for (InboundAuthenticationRequestProcessor requestProcessor : requestProcessors) {
            if (requestProcessor.canHandle(req, resp)) {
                return requestProcessor;
            }
        }
        return null;
    }

    private InboundAuthenticationResponseBuilder getInboundResponseBuilder(HttpServletRequest req,
            HttpServletResponse resp) throws FrameworkException{
        List<InboundAuthenticationResponseBuilder> responseBuilders = FrameworkServiceDataHolder.getInstance()
                .getInboundResponseBuilders();

//        for (InboundAuthenticationResponseBuilder responseBuilder : responseBuilders) {
//            if (responseBuilder.canHandle(req, resp)) {
//                return responseBuilder;
//            }
//        }
        return null;
    }

    //buildAuthenticationRequest() new method InboundAutheticationRequest, need to have a different class with can handle, name, priotity to get builder

    public InboundAuthenticationResponse process(HttpServletRequest req, HttpServletResponse resp) throws FrameworkException {

        String sessionDataKey = req.getParameter(FrameworkConstants.SESSION_DATA_KEY);
        if (StringUtils.isEmpty(sessionDataKey)) {
            InboundAuthenticationRequestProcessor requestProcessors = getInboundRequestProcessor(req, resp);
            if (requestProcessors != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Starting to process inbound authentication : " + requestProcessors.getName());
                }
                return requestProcessors.process(req, resp);
            } else {
                log.warn("No request processor available for process");
            }
        } else {
            //Response from common authentication framework.
            InboundAuthenticationResponseBuilder responseBuilder = getInboundResponseBuilder(req, resp);
            if (responseBuilder != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Starting to process inbound authentication : " + responseBuilder.getName());
                }
//                return responseBuilder.buildResponse(req, resp, sessionDataKey);
            } else {
                log.warn("No response processor available for process");
            }
        }
        return null;
    }

    /**
     *
     * @param sessionDataKey
     * @return
     */
    protected AuthenticationResult getAuthenticationResultFromCache(String sessionDataKey) {

        AuthenticationResultCacheKey authResultCacheKey = new AuthenticationResultCacheKey(sessionDataKey);
        CacheEntry cacheEntry = AuthenticationResultCache.getInstance().getValueFromCache(authResultCacheKey);
        AuthenticationResult authResult = null;

        if (cacheEntry != null) {
            AuthenticationResultCacheEntry authResultCacheEntry = (AuthenticationResultCacheEntry) cacheEntry;
            authResult = authResultCacheEntry.getResult();
        } else {
            log.error("Cannot find AuthenticationResult from the cache");
        }

        return authResult;
    }
}
