/*
 * Copyright (c) 2005-2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provider.xmpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.core.model.XMPPSettingsDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.provider.dto.XMPPSettingsDTO;

public class XMPPConfigurator {

    private static final Log log = LogFactory.getLog(XMPPConfigurator.class);

    /**
     * To add XMPP Settings.
     *
     * @param dto
     */
    public void addXmppSettings(XMPPSettingsDTO dto) {
        try {
            IdentityPersistenceManager persistentManager = IdentityPersistenceManager.getPersistanceManager();
            persistentManager.addXmppSettings(IdentityTenantUtil.getRegistry(), dto.getUserId(), dto.getXmppServer(),
                                              dto.getXmppUserName(),
                                              dto.getUserCode(), dto.isXmppEnabled(), dto.isPINEnabled());

        } catch (Exception e) {
            log.error("Error when instantiating the Persistence Manager.", e);
        }
    }

    /**
     * To edit XMPP Settings.
     *
     * @param dto
     */
    public void editXmppSettings(XMPPSettingsDTO dto) {
        try {
            IdentityPersistenceManager persistentManager = IdentityPersistenceManager.getPersistanceManager();
            persistentManager.updateXmppSettings(IdentityTenantUtil.getRegistry(), dto.getUserId(),
                                                 dto.getXmppServer(), dto.getXmppUserName(), dto.getUserCode(),
                                                 dto.isXmppEnabled(), dto.isPINEnabled());

        } catch (Exception e) {
            log.error("Error when instantiating the Persistence Manager.", e);
        }
    }

    /**
     * To get XMPP Settings
     *
     * @param userId
     * @return XmppSettingsDTO instance containing XMPP properties
     */
    public XMPPSettingsDTO getXmppSettings(String userId) {
        XMPPSettingsDTO xmppSettingsDTO = null;
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            XMPPSettingsDO xmppSettingsDO =
                    persistenceManager.getXmppSettings(IdentityTenantUtil.getRegistry(), userId);
            xmppSettingsDTO = new XMPPSettingsDTO();
            xmppSettingsDTO.setXmppServer(xmppSettingsDO.getXmppServer());
            xmppSettingsDTO.setXmppUserName(xmppSettingsDO.getXmppUserName());
            xmppSettingsDTO.setUserCode(xmppSettingsDO.getUserCode());
            xmppSettingsDTO.setXmppEnabled(xmppSettingsDO.isXmppEnabled());
            xmppSettingsDTO.setPINEnabled(xmppSettingsDO.isPINEnabled());

        } catch (Exception e) {
            log.error("Error when instantiating the Persistence Manager.", e);
        }

        return xmppSettingsDTO;
    }

    /**
     * checks whether the user as enabled multifactor auth.
     *
     * @param userId
     * @return
     */
    public boolean isXmppSettingsEnabled(String userId) {
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            return persistenceManager.isXmppSettingsEnabled(IdentityTenantUtil.getRegistry(), userId);
        } catch (Exception e) {
            log.error("Error when instantiating the Persistence Manager.", e);
        }
        return false;
    }

    public boolean hasXMPPSettings(String userId) {
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            return persistenceManager.hasXMPPSettings(IdentityTenantUtil.getRegistry(), userId);
        } catch (Exception e) {
            log.error("Error when instantiating the Persistence Manager.", e);
        }
        return false;
    }
}
