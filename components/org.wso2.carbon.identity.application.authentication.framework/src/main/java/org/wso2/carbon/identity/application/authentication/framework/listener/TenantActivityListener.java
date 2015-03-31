/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class TenantActivityListener implements TenantMgtListener {

    private static final Log log = LogFactory.getLog(TenantActivityListener.class);
    private List<String> tenantDataReceiveURLs;
    private TenantManager tenantManager;
    private boolean isInitialized;
    private String serverURL;

    private void init() {
        try {
            tenantDataReceiveURLs = ConfigurationFacade.getInstance().getTenantDataEndpointURLs();

            if (tenantDataReceiveURLs != null && tenantDataReceiveURLs.size() > 0) {

                serverURL = CarbonUIUtil.getAdminConsoleURL("/").replace("/carbon/", "");
                int index = 0;
                for (String tenantDataReceiveUrl : tenantDataReceiveURLs) {
                    URI tenantDataReceiveURI = new URI(tenantDataReceiveUrl);
                    if (!tenantDataReceiveURI.isAbsolute()) {
                        tenantDataReceiveURLs.set(index, serverURL + tenantDataReceiveUrl);
                    }
                    index++;
                }
                RealmService realmService = (RealmService)
                        PrivilegedCarbonContext.getThreadLocalCarbonContext()
                                .getOSGiService(RealmService.class);
                if (realmService != null) {
                    tenantManager = realmService.getTenantManager();
                }
                isInitialized = true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("TenantDataListenerURLs are not set in configuration");
                }
            }
        } catch (URISyntaxException e) {
            log.error("Error while getting TenantDataListenerURLs", e);
        }
    }

    private void sendTenantList() {
        try {

            if (!isInitialized) {
                init();
            }

            if (tenantDataReceiveURLs != null && tenantDataReceiveURLs.size() > 0) {
                Tenant[] tenants = tenantManager.getAllTenants();
                StringBuilder builder = new StringBuilder();
                for (Tenant tenant : tenants) {
                    if (tenant.isActive()) {
                        builder.append(tenant.getDomain() + ",");
                    }
                }
                if (builder.toString().length() > 0) {
                    builder.deleteCharAt(builder.toString().length() - 1);
                }

                String params = "?tenantList=" + builder.toString();

                for (String tenantDataReceiveURL : tenantDataReceiveURLs) {
                    try {
                        new URL(tenantDataReceiveURL + params).openStream();
                    } catch (Exception e) {
                        log.error("Sending tenant domain list to " + tenantDataReceiveURL + " failed.", e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Sending tenant domain list to authentication endpoint failed", e);
        }
    }

    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {

    }

    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {

    }

    @Override
    public void onTenantRename(int tenantId, String oldDomainName,
                               String newDomainName) throws StratosException {

    }

    @Override
    public void onTenantInitialActivation(int tenantId) throws StratosException {
        sendTenantList();
    }

    @Override
    public void onTenantActivation(int tenantId) throws StratosException {
        sendTenantList();
    }

    @Override
    public void onTenantDeactivation(int tenantId) throws StratosException {
        sendTenantList();
    }

    @Override
    public void onSubscriptionPlanChange(int tenentId, String oldPlan, String newPlan)
            throws StratosException {

    }

    @Override
    public int getListenerOrder() {
        return 0;
    }
}
