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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import java.util.Iterator;

public class OIDCService {
    private static Log log = LogFactory.getLog(OIDCService.class);
    private static OIDCService oidcProcessor = new OIDCService();
    private static final String CONFIG_ELEM_OIDC = "OpenIDConnectDiscovery";
    private static final String CONFIG_ELEM_OIDCCONFIG = "Configuration";

    private OIDCService() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing OIDCProcessor for OpenID connect discovery processor.");
        }
    }

    public static OIDCService getInstance() {
        return oidcProcessor;
    }

    public Response handleError(OIDCDiscoveryEndPointException error) {
        if (log.isDebugEnabled()) {
            log.debug(error);
        }
        String response = error.getErrorMessage();
        Response.ResponseBuilder responseBuilder = null;
        if (error.getErrorCode().equals(OIDCDiscoveryEndPointException.ERROR_CODE_NO_WELL_KNOWN_PATH)) {
            responseBuilder =
                    Response.status(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            responseBuilder = Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        return responseBuilder.entity(response).build();

    }

    public OIDProviderConfigDTO getOIDProviderConfig(OIDProviderRequestDTO request) throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderConfigDTO providerConfig = new OIDProviderConfigDTO();
        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement oidcElement = configParser.getConfigElement(CONFIG_ELEM_OIDC);
        if (oidcElement == null) {
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException
                    .ERROR_CODE_NO_OPENID_PROVIDER_FOUND, "No OpendID provider found at the path.");
        }
        OMElement oidcUserConfig =null;
        Iterator<OMElement> configurations = oidcElement.getChildrenWithName(getQNameWithIdentityNS(CONFIG_ELEM_OIDCCONFIG));
        if (request.getSubpath() != null && !request.getSubpath().isEmpty()) {
            while(configurations.hasNext()){
                OMElement configuration = configurations.next();
                String userName = configuration.getAttributeValue(getQNameWithIdentityNS("name"));
                if(userName.equals(request.getSubpath())){
                    oidcUserConfig = configuration;
                }
            }
            if (oidcUserConfig == null) {
                throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException.ERROR_CODE_INVALID_PATH, "No " +
                        "OpenID provider for the given sub path.");
            }
        } else {
            oidcUserConfig = oidcElement.getFirstChildWithName(getQNameWithIdentityNS(CONFIG_ELEM_OIDCCONFIG));
        }


        providerConfig.setIssuer(oidcUserConfig.getFirstChildWithName(getQNameWithIdentityNS(OIDProviderConfigDTO
                .ISSUER)).getText());

        return providerConfig;
    }

    private QName getQNameWithIdentityNS(String localPart) {
        return new QName(IdentityConfigParser.IDENTITY_DEFAULT_NAMESPACE, localPart);
    }
}
