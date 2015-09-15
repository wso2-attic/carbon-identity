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
package org.wso2.carbon.identity.discovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.discovery.builders.DefaultOIDProviderRequestBuilder;
import org.wso2.carbon.identity.discovery.builders.OIDProviderRequestBuilder;
import org.wso2.carbon.identity.discovery.builders.ProviderConfigBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultOIDCProcessor implements OIDCProcessor {
    private static Log log = LogFactory.getLog(DefaultOIDCProcessor.class);
    private static DefaultOIDCProcessor defaultOidcProcessor = new DefaultOIDCProcessor();


    private DefaultOIDCProcessor() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing DefaultOIDCProcessor for OpenID connect discovery processor.");
        }
    }

    public static DefaultOIDCProcessor getInstance() {
        return defaultOidcProcessor;
    }

    public OIDProviderConfigResponse getResponse(HttpServletRequest request, String tenant) throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderRequestBuilder requestBuilder = new DefaultOIDProviderRequestBuilder();
        OIDProviderRequest requestObject = requestBuilder.buildRequest(request, tenant);
        ProviderConfigBuilder responseBuilder = new ProviderConfigBuilder();
        return responseBuilder.buildOIDProviderConfig(requestObject);
    }

    public int handleError(OIDCDiscoveryEndPointException error) {
        if (log.isDebugEnabled()) {
            log.debug(error);
        }
        String errorCode = error.getErrorCode();
        if (errorCode.equals(OIDCDiscoveryEndPointException.ERROR_CODE_NO_OPENID_PROVIDER_FOUND)) {

        } else if (errorCode.equals(OIDCDiscoveryEndPointException.ERROR_CODE_INVALID_REQUEST)) {

        } else if (errorCode.equals(OIDCDiscoveryEndPointException.ERROR_CODE_INVALID_TENANT)) {

        } else if (errorCode.equals(OIDCDiscoveryEndPointException.ERROR_CODE_JSON_EXCEPTION)) {

        } else {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }


}
