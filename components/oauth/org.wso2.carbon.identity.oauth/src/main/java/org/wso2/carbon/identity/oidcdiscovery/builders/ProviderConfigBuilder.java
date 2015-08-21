package org.wso2.carbon.identity.oidcdiscovery.builders;


import org.apache.axiom.om.OMElement;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.oidcdiscovery.DiscoveryConstants;
import org.wso2.carbon.identity.oidcdiscovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.oidcdiscovery.OIDProviderConfigResponse;
import org.wso2.carbon.identity.oidcdiscovery.OIDProviderRequest;
import org.wso2.carbon.identity.application.common.model.Property;

import javax.xml.namespace.QName;
import java.util.Iterator;

public class ProviderConfigBuilder {

    public OIDProviderConfigResponse buildOIDProviderConfig(OIDProviderRequest request) throws
            OIDCDiscoveryEndPointException, ServerConfigurationException {
        OIDProviderConfigResponse providerConfig = new OIDProviderConfigResponse();
        String tenantName = request.getTenant();
        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement oidcElement = configParser.getConfigElement(DiscoveryConstants.CONFIG_ELEM_OIDC);
        if (oidcElement == null) {
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException
                    .ERROR_CODE_NO_OPENID_PROVIDER_FOUND, "No OpendID provider found at the path.");
        }

        OMElement oidcTenantConfig = null;
        Iterator<OMElement> configurations = oidcElement.getChildrenWithName(configParser.getQNameWithIdentityNS
                (DiscoveryConstants.CONFIG_ELEM_OIDCCONFIG));

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
        setParmaters(configParser, providerConfig, oidcTenantConfig);

        return providerConfig;
    }

    /**
     * This method sets all the configuration parameters accordingly.
     * Use this method to assign newly introduced parameters.
     */
    private void setParmaters(IdentityConfigParser configParser, OIDProviderConfigResponse providerConfig, OMElement
            oidcTenantConfig) {
        IdentityProvider identityProvider = new IdentityProvider();
        FederatedAuthenticatorConfig[] federatedAuthenticators = identityProvider.getFederatedAuthenticatorConfigs();
        for(FederatedAuthenticatorConfig federatedAuthenticator : federatedAuthenticators) {
            Property[] properties = federatedAuthenticator.getProperties();
            if (IdentityApplicationConstants.Authenticator.OIDC.NAME.equals(federatedAuthenticator.getName())){
                providerConfig.setAuthorization_endpoint(this.getProperty(properties,
                        IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL).getValue());
                providerConfig.setToken_endpoint(this.getProperty(properties,
                        IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL).getValue());
                providerConfig.setUserinfo_endpoint(this.getProperty(properties,
                        IdentityApplicationConstants.Authenticator.OIDC.USER_INFO_URL).getValue());
            }
        }
        providerConfig.setIssuer(oidcTenantConfig.getFirstChildWithName(configParser.getQNameWithIdentityNS
                (DiscoveryConstants.ISSUER)).getText());
        providerConfig.setRegistration_endpoint(oidcTenantConfig.getFirstChildWithName(configParser
                .getQNameWithIdentityNS(DiscoveryConstants.REGISTRATION_ENDPOINT)).getText());
    }

    private Property getProperty(Property[] properties, String propertyName) {
        for (Property property : properties) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }
        return null;
    }
}
