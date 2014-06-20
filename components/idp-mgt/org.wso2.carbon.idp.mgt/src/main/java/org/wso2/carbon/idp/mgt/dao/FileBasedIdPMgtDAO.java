package org.wso2.carbon.idp.mgt.dao;

import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.idp.mgt.internal.IdPManagementServiceComponent;

import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.application.common.model.Property;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class FileBasedIdPMgtDAO {

    /**
     * 
     * @param idPName
     * @param tenantDomain
     * @return
     */
    public IdentityProvider getIdPByName(String idPName, String tenantDomain) {
        return IdPManagementServiceComponent.getFileBasedIdPs().get(idPName);
    }

    /**
     *
     * @param property
     * @param value
     * @param tenantDomain
     * @return
     */
    public IdentityProvider getIdPByAuthenticatorPropertyValue(String property, String value, String tenantDomain) {

        Map<String, IdentityProvider> identityProviders = IdPManagementServiceComponent.getFileBasedIdPs();
        for (Iterator<Entry<String, IdentityProvider>> iterator = identityProviders.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<String, IdentityProvider> entry = iterator.next();
            FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = entry.getValue().getFederatedAuthenticatorConfigs();
            // Get SAML2 Web SSO authenticator
            FederatedAuthenticatorConfig samlAuthenticatorConfig = IdentityApplicationManagementUtil.getFederatedAuthenticator(
                    federatedAuthenticatorConfigs, IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);

            if(samlAuthenticatorConfig != null){
                Property samlProperty = IdentityApplicationManagementUtil.getProperty(samlAuthenticatorConfig.getProperties(),
                        property);
                if (samlProperty != null) {
                    if(value.equalsIgnoreCase(samlProperty.getValue())){
                       return entry.getValue();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param realmId
     * @param tenantDomain
     * @return
     */
    public IdentityProvider getIdPByRealmId(String realmId, String tenantDomain) {

        Map<String, IdentityProvider> map = IdPManagementServiceComponent.getFileBasedIdPs();

        for (Iterator<Entry<String, IdentityProvider>> iterator = map.entrySet().iterator(); iterator
                .hasNext();) {
            Entry<String, IdentityProvider> entry = iterator.next();
            if (entry.getValue().getHomeRealmId() != null
                    && entry.getValue().getHomeRealmId().equals(realmId)) {
                return entry.getValue();
            }

        }

        return null;
    }

}
