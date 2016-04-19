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
package org.wso2.carbon.identity.discovery.builders;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.discovery.DiscoveryConstants;
import org.wso2.carbon.identity.discovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.discovery.OIDProviderRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * DefaultOIDProviderRequestBuilder is the default OIDProviderRequestBuilder
 * provided in the discovery module.
 * DefaultOIDProviderRequestBuilder builds the OIDProviderRequest using HttpServletRequest and tenant
 */
public class DefaultOIDProviderRequestBuilder implements OIDProviderRequestBuilder {

    @Override
    public OIDProviderRequest buildRequest(HttpServletRequest request, String tenant) throws
            OIDCDiscoveryEndPointException {
        OIDProviderRequest requestObject = new OIDProviderRequest();
        requestObject.setUri(request.getRequestURI());
        if (StringUtils.isNotBlank(tenant)) {
            requestObject.setTenantDomain(tenant);
        } else {
            requestObject.setTenantDomain(DiscoveryConstants.CONFIG_DEFAULT_NAME);
        }
        return requestObject;
    }


}
