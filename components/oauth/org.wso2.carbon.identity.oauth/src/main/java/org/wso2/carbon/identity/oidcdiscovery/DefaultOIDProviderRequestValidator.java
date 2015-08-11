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
package org.wso2.carbon.identity.oidcdiscovery;

import org.wso2.carbon.ui.util.CharacterEncoder;

import javax.servlet.http.HttpServletRequest;


public class DefaultOIDProviderRequestValidator implements OIDProviderRequestValidator {

    public static final String OPENID_CONFIGURATION_RESOURCE = "/.well-known/openid-configuration";

    @Override
    public OIDProviderRequest validateRequest(HttpServletRequest request, String tenant) throws
            OIDCDiscoveryEndPointException {
        try {
            OIDProviderRequest requestObject = new OIDProviderRequest();
            requestObject.setUri(CharacterEncoder.getSafeText(request.getRequestURI()).trim());
            requestObject.setTenant(tenant);
            return requestObject;
        } catch (Exception e) {
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException.ERROR_CODE_SERVER_ERROR,
                    "Internal error occured. Please try again.");
        }

    }


}
