/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.authentication.framework.config;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.config.builder.FileBasedConfigurationBuilder;
import org.wso2.carbon.identity.application.authentication.framework.config.builder.UIBasedConfigurationBuilder;
import org.wso2.carbon.identity.application.authentication.framework.config.model.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;

public class ConfigurationFacade {

    private static Log log = LogFactory.getLog(ConfigurationFacade.class);

    private static volatile ConfigurationFacade instance;

    public ConfigurationFacade() {
        // Read the default config from the files
        FileBasedConfigurationBuilder.getInstance().build();
    }

    public static ConfigurationFacade getInstance() {

        if (instance == null) {
            synchronized (ConfigurationFacade.class) {

                if (instance == null) {
                    instance = new ConfigurationFacade();
                }
            }
        }

        return instance;
    }

    public SequenceConfig getSequenceConfig(String reqType, String relyingParty, String tenantDomain)
            throws FrameworkException {

        // Get SP config from SP Management component
        return UIBasedConfigurationBuilder.getInstance().getSequence(reqType,
                relyingParty, tenantDomain);
    }

    public ExternalIdPConfig getIdPConfigByName(String idpName, String tenantDomain) {

        ExternalIdPConfig externalIdPConfig = null;
        IdentityProvider idpDO = null;

        if (log.isDebugEnabled()) {
            log.debug("Trying to find the IdP for name: " + idpName);
        }

        try {
            IdentityProviderManager idpManager = IdentityProviderManager.getInstance();
            idpDO = idpManager.getEnabledIdPByName(idpName, tenantDomain);

            if (idpDO != null) {

                if (log.isDebugEnabled()) {
                    log.debug("A registered IdP was found");
                }

                externalIdPConfig = new ExternalIdPConfig(idpDO);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("A registered IdP was not found the given name");
                }
            }
        } catch (IdentityApplicationManagementException e) {
            log.error("Exception while getting IdP by name", e);
        }

        return externalIdPConfig;
    }

    public ExternalIdPConfig getIdPConfigByRealm(String realm) {

        ExternalIdPConfig externalIdPConfig = null;
        IdentityProvider idpDO = null;

        if (log.isDebugEnabled()) {
            log.debug("Trying to find the IdP for realm: " + realm);
        }

        try {
            IdentityProviderManager idpManager = IdentityProviderManager.getInstance();
            // TODO use tenantDomain sent from client
            idpDO = idpManager
                    .getEnabledIdPByRealmId(realm, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            if (idpDO != null) {

                if (log.isDebugEnabled()) {
                    log.debug("A registered IdP was found");
                }

                externalIdPConfig = new ExternalIdPConfig(idpDO);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("A registered IdP was not found the given realm");
                }
            }
        } catch (IdentityApplicationManagementException e) {
            log.error("Exception while getting IdP by realm", e);
        }

        return externalIdPConfig;
    }

    public String getAuthenticationEndpointURL() {
        return FileBasedConfigurationBuilder.getInstance().getAuthenticationEndpointURL();
    }

    public boolean isDumbMode() {
        return FileBasedConfigurationBuilder.getInstance().isDumbMode();
    }

    public Map<String, Object> getExtensions() {
        return FileBasedConfigurationBuilder.getInstance().getExtensions();
    }

    public Map<String, String> getAuthenticatorNameMappings() {
        return FileBasedConfigurationBuilder.getInstance().getAuthenticatorNameMappings();
    }

    public Map<String, Integer> getCacheTimeouts() {
        return FileBasedConfigurationBuilder.getInstance().getCacheTimeouts();
    }

    public int getMaxLoginAttemptCount() {
        return FileBasedConfigurationBuilder.getInstance().getMaxLoginAttemptCount();
    }
}
