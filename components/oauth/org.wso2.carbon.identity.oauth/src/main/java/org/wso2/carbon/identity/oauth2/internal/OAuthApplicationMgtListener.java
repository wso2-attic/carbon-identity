package org.wso2.carbon.identity.oauth2.internal;

import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.listener.AbstractApplicationMgtListener;

public class OAuthApplicationMgtListener extends AbstractApplicationMgtListener {

    public static final String OAUTH2 = "oauth2";
    public static final String OAUTH2_CONSUMER_SECRET = "oauthConsumerSecret";

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
        addClientSecret(serviceProvider, applicationName, tenantDomain);
        return true;
    }

    public boolean doPostGetApplicationByClientId(ServiceProvider serviceProvider, String clientId, String clientType,
                                                  String tenantDomain) throws IdentityApplicationManagementException {

        addClientSecret(serviceProvider, clientType, tenantDomain);
        return true;
    }

    private void removeClientSecret(ServiceProvider serviceProvider) {
        InboundAuthenticationRequestConfig[] inboundRequestConfigs = serviceProvider.getInboundAuthenticationConfig().
                getInboundAuthenticationRequestConfigs();
        for (InboundAuthenticationRequestConfig inboundRequestConfig : inboundRequestConfigs) {
            if(inboundRequestConfig.getInboundAuthType().equals(OAUTH2)){
                Property[] props = inboundRequestConfig.getProperties();
                for (Property prop : props) {
                    if(prop.getName().equalsIgnoreCase(OAUTH2_CONSUMER_SECRET)){
                        prop.setValue("");
                        continue;   //we are interested only on this property
                    } else{
                        //ignore
                    }
                }
                continue;// we are interested only on oauth2 config. Only one will be present.
            }else{
                //ignore
            }
        }
    }

    private ServiceProvider addClientSecret(ServiceProvider serviceProvider, String clientType,
                                            String tenantDomain) {

        InboundAuthenticationRequestConfig[] inboundRequestConfigs = serviceProvider.getInboundAuthenticationConfig().
                getInboundAuthenticationRequestConfigs();
        for (InboundAuthenticationRequestConfig inboundRequestConfig : inboundRequestConfigs) {
            if(inboundRequestConfig.getInboundAuthType().equals(OAUTH2)){
                Property[] props = inboundRequestConfig.getProperties();
                for (Property prop : props) {
                    if(prop.getName().equalsIgnoreCase(OAUTH2_CONSUMER_SECRET)){
//                        prop.setValue(getClientSecret());
                        continue;   //we are interested only on this property
                    } else{
                        //ignore
                    }
                }
                continue;// we are interested only on oauth2 config. Only one will be present.
            }else{
                //ignore
            }
        }
        return serviceProvider;
    }

}
