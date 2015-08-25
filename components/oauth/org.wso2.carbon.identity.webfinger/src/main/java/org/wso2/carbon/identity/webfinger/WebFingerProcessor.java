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
package org.wso2.carbon.identity.webfinger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.webfinger.builders.DefaultWebFingerRequestBuilder;
import org.wso2.carbon.identity.webfinger.builders.WebFingerRequestBuilder;
import org.wso2.carbon.identity.webfinger.builders.WebFingerResponseBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Singleton class to process the webfinger request.
 */
public class WebFingerProcessor {
    private static Log log = LogFactory.getLog(WebFingerProcessor.class);
    private static WebFingerProcessor webFingerProcessor = new WebFingerProcessor();

    private WebFingerProcessor() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing OIDCProcessor for OpenID connect discovery processor.");
        }
    }

    public static WebFingerProcessor getInstance() {
        return webFingerProcessor;
    }

    public WebFingerResponse getResponse(HttpServletRequest request) throws WebFingerEndPointException,
            ServerConfigurationException {
        WebFingerRequestBuilder requestBuilder = new DefaultWebFingerRequestBuilder();
        WebFingerRequest requestObject = requestBuilder.buildRequest(request);
        WebFingerResponseBuilder responseBuilder = new WebFingerResponseBuilder();
        return responseBuilder.buildWebFingerResponse(requestObject);
    }

    public int handleError(WebFingerEndPointException error) {
        if (log.isDebugEnabled()) {
            log.debug(error);
        }
        String errorCode = error.getErrorCode();
        if (errorCode.equals(WebFingerConstants.ERROR_CODE_INVALID_REQUEST)) {
            return HttpServletResponse.SC_BAD_REQUEST;
        } else if (errorCode.equals(WebFingerConstants.ERROR_CODE_INVALID_RESOURCE)) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else if (errorCode.equals(WebFingerConstants.ERROR_CODE_JSON_EXCEPTION)) {
            return HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;
        } else if (errorCode.equals(WebFingerConstants.ERROR_CODE_NO_WEBFINGER_CONFIG)) {
            return HttpServletResponse.SC_NOT_FOUND;
        } else {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

    }


}
