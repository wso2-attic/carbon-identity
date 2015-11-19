package org.wso2.carbon.identity.oauth2.internal;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.dao.OAuthConsumerDAO;

public class OAuthApplicationMgtListener extends AbstractApplicationMgtListener {

    public static final String OAUTH2 = "oauth2";
    public static final String OAUTH2_CONSUMER_SECRET = "oauthConsumerSecret";
    private String clientSecret;

    @Override
    public int getDefaultOrderId() {
        return 11;
    }

    public boolean doPreCreateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {
        removeClientSecret(serviceProvider);
        return true;
    }

    public boolean doPreUpdateApplication(ServiceProvider serviceProvider, String tenantDomain, String userName)
            throws IdentityApplicationManagementException {
        removeClientSecret(serviceProvider);
        return true;
    }

    public boolean doPostGetApplication(ServiceProvider serviceProvider, String applicationName, String tenantDomain)
            throws IdentityApplicationManagementException {
        try {
            addClientSecret(serviceProvider, applicationName, tenantDomain);
        } catch (IdentityOAuthAdminException e) {
            throw new IdentityApplicationManagementException("Injecting client secret failed.", e);
        }
        return true;
    }

    public boolean doPostGetApplicationByClientId(ServiceProvider serviceProvider, String clientId, String clientType,
                                                  String tenantDomain) throws IdentityApplicationManagementException {

        try {
            addClientSecret(serviceProvider, clientType, tenantDomain);
        } catch (IdentityOAuthAdminException e) {
            throw new IdentityApplicationManagementException("Injecting client secret failed.", e);
        }
        return true;
    }

    private void removeClientSecret(ServiceProvider serviceProvider) {
        InboundAuthenticationRequestConfig[] inboundRequestConfigs = serviceProvider.getInboundAuthenticationConfig().
                getInboundAuthenticationRequestConfigs();
        for (InboundAuthenticationRequestConfig inboundRequestConfig : inboundRequestConfigs) {
            if (inboundRequestConfig.getInboundAuthType().equals(OAUTH2)) {
                Property[] props = inboundRequestConfig.getProperties();
                for (Property prop : props) {
                    if (prop.getName().equalsIgnoreCase(OAUTH2_CONSUMER_SECRET)) {
                        prop.setValue("");
                        continue;   //we are interested only on this property
                    } else {
                        //ignore
                    }
                }
                continue;// we are interested only on oauth2 config. Only one will be present.
            } else {
                //ignore
            }
        }
    }

    private ServiceProvider addClientSecret(ServiceProvider serviceProvider, String clientType,
                                            String tenantDomain) throws IdentityOAuthAdminException {

        InboundAuthenticationRequestConfig[] inboundRequestConfigs = serviceProvider.getInboundAuthenticationConfig().
                getInboundAuthenticationRequestConfigs();
        for (InboundAuthenticationRequestConfig inboundRequestConfig : inboundRequestConfigs) {
            if (inboundRequestConfig.getInboundAuthType().equals(OAUTH2)) {
                Property[] props = inboundRequestConfig.getProperties();
                for (Property prop : props) {
                    if (prop.getName().equalsIgnoreCase(OAUTH2_CONSUMER_SECRET)) {
                        prop.setValue(getClientSecret(inboundRequestConfig.getInboundAuthKey()));
                        continue;   //we are interested only on this property
                    } else {
                        //ignore
                    }
                }
                continue;// we are interested only on oauth2 config. Only one will be present.
            } else {
                //ignore
            }
        }
        return serviceProvider;
    }

    private String getClientSecret(String inboundAuthKey) throws IdentityOAuthAdminException {
        OAuthConsumerDAO dao = new OAuthConsumerDAO();
        return dao.getOAuthConsumerSecret(inboundAuthKey);
    }
}
