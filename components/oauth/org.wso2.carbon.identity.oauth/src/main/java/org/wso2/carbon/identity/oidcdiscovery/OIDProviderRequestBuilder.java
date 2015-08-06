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


public class OIDProviderRequestBuilder implements OIDProviderRequestValidator {

    private final static String STANDARD_WELL_KNOWN_PATH = "/.well-known/openid-configuration";
    private final static String IS_OPENID_HOST = "/oauth2/oidcdiscovery";

    @Override
    public OIDProviderRequestDTO validateRequest(HttpServletRequest request) throws OIDCDiscoveryEndPointException {
        String name;
        OIDProviderRequestDTO requestDTO = new OIDProviderRequestDTO();
        String requestURI = CharacterEncoder.getSafeText(request.getRequestURI()).trim();

        if (requestURI.endsWith(STANDARD_WELL_KNOWN_PATH)) {
            requestDTO.setPath(STANDARD_WELL_KNOWN_PATH);
            requestURI = requestURI.replaceFirst(STANDARD_WELL_KNOWN_PATH, "").trim();
        } else {
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException.ERROR_CODE_NO_WELL_KNOWN_PATH,
                    "The well known path is not provided.");
        }
        if(requestURI.startsWith(IS_OPENID_HOST)){
            requestURI = requestURI.replaceFirst(IS_OPENID_HOST,"").trim();
        }
        if (!(requestURI.isEmpty())) {
            requestDTO.setSubpath(requestURI);
        }

        return requestDTO;
    }


}
