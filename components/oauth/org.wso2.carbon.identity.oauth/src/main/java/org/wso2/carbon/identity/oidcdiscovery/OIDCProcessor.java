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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.util.Iterator;

public class OIDCProcessor {
    private static Log log = LogFactory.getLog(OIDCProcessor.class);
    private static OIDCProcessor oidcProcessor = new OIDCProcessor();
    private static final String CONFIG_ELEM_OIDC = "OpenIDConnectDiscovery";
    private static final String CONFIG_ELEM_OIDCCONFIG = "Configuration";
    private static final String CONFIG_DEFAULT_NAME = "default";
    private MessageContext context;

    private OIDCProcessor() {
        this.context = new MessageContext();
        if (log.isDebugEnabled()) {
            log.debug("Initializing OIDCProcessor for OpenID connect discovery processor.");
        }
    }


    public static OIDCProcessor getInstance() {
        return oidcProcessor;
    }


    public void validateRequest(HttpServletRequest request,String tenant) throws OIDCDiscoveryEndPointException {
        OIDProviderRequestValidator requestBuilder = new DefaultOIDProviderRequestValidator();
        this.context.setRequest(requestBuilder.validateRequest(request,tenant));
    }

    //Do we have to synchronize this method since we initialize MessageContext.
    public OIDProviderConfig getOIDProviderConfig() throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderRequest request = this.context.getRequest();
        OIDProviderConfig providerConfig = this.context.getConfigurations();
        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement oidcElement = configParser.getConfigElement(CONFIG_ELEM_OIDC);
        if (oidcElement == null) {
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException
                    .ERROR_CODE_NO_OPENID_PROVIDER_FOUND, "No OpendID provider found at the path.");
        }
        OMElement oidcTenantConfig = null;
        String tenantName = "";
        if (request.getTenant() != null && !request.getTenant().isEmpty()) {
            tenantName = request.getTenant();

        } else {
            tenantName = CONFIG_DEFAULT_NAME;
        }
        Iterator<OMElement> configurations = oidcElement.getChildrenWithName(getQNameWithIdentityNS
                (CONFIG_ELEM_OIDCCONFIG));
        while (configurations.hasNext()) {
            OMElement configuration = configurations.next();
            String userName = configuration.getAttributeValue(new QName("name"));
            if (userName.equals(tenantName)) {
                oidcTenantConfig = configuration;
            }
        }
        if (oidcTenantConfig == null) {
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException.ERROR_CODE_INVALID_TENANT, "No " +
                    "OpenID provider for the given tenant.");
        }
        setParmaters(providerConfig,oidcTenantConfig);

        return providerConfig;
    }

    public MessageContext getContext() {
        return context;
    }

    public void setContext(MessageContext context) {
        this.context = context;
    }

    public int handleError(OIDCDiscoveryEndPointException error) {
        if (log.isDebugEnabled()) {
            log.debug(error);
        }
        return  HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private void setParmaters(OIDProviderConfig providerConfig,OMElement oidcTenantConfig){

        providerConfig.setIssuer(oidcTenantConfig.getFirstChildWithName(getQNameWithIdentityNS(DiscoveryConstants
                .ISSUER)).getText());

    }
    private QName getQNameWithIdentityNS(String localPart) {
        return new QName(IdentityConfigParser.IDENTITY_DEFAULT_NAMESPACE, localPart);
    }
}
