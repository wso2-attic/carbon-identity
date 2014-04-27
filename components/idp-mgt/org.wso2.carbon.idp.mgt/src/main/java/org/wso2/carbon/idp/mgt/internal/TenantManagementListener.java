/*
 *Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.idp.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.ResidentIdentityProvider;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.keystore.mgt.KeyStoreGenerator;
import org.wso2.carbon.keystore.mgt.KeyStoreMgtException;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

public class TenantManagementListener implements TenantMgtListener {

    private static Log log = LogFactory.getLog(TenantManagementListener.class);

    private static final int EXEC_ORDER = 21;

    /**
     * Generate the idp-mgt-truststore when a new tenant is registered.
     * Add the default Resident Identity Provider entry when a new tenant is registered.
     * @param tenantInfo Information about the newly created tenant
     */
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
        try {
            String tenantDomain = tenantInfo.getTenantDomain();
            String trustStoreName = tenantDomain.trim().replace(".", "-") + "-idp-mgt-truststore.jks";
            KeyStoreGenerator ksGenerator = new KeyStoreGenerator(tenantInfo.getTenantId());
            ksGenerator.generateTrustStore(trustStoreName);
            ResidentIdentityProvider identityProvider = new ResidentIdentityProvider();
            identityProvider.setHomeRealmId("localhost");
            IdentityProviderManager.getInstance().addResidentIdP(identityProvider, tenantDomain);
        } catch (KeyStoreMgtException e) {
            String message = "Error when generating the idp-mgt-trust store for tenant " + tenantInfo.getTenantDomain();
            log.error(message, e);
            throw new StratosException(message);
        } catch (IdentityApplicationManagementException e) {
            String message = "Error when adding Resident Identity Provider entry for tenant " +
                    tenantInfo.getTenantDomain();
            log.error(message, e);
            throw new StratosException(message);
        }
    }

    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
        // It is not required to implement this method for IdP mgt.
    }

    public void onTenantRename(int tenantId, String oldDomainName,
                               String newDomainName) throws StratosException {
        // It is not required to implement this method for IdP mgt.
    }

    public int getListenerOrder() {
        return EXEC_ORDER;
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for IdP mgt.
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        // It is not required to implement this method for IdP mgt.
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        // It is not required to implement this method for IdP mgt.
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, String newPlan) throws StratosException {
        // It is not required to implement this method for IdP mgt.
    }

}
