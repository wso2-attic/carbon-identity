/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.provider.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provider.stub.XMPPConfigurationServiceStub;
import org.wso2.carbon.identity.provider.stub.dto.XMPPSettingsDTO;

import java.rmi.RemoteException;

public class XMPPConfiguratorClient {

    private static final Log log = LogFactory.getLog(XMPPConfiguratorClient.class);

    private XMPPConfigurationServiceStub stub = null;

    /**
     * @param cookie
     * @param backendServerURL
     * @param configCtx
     * @throws AxisFault
     */
    public XMPPConfiguratorClient(String cookie, String backendServerURL,
            ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "XMPPConfigurationService";
        stub = new XMPPConfigurationServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * Add XMPP Settings
     * 
     * @param dto
     */
    public void addXmppSettings(XMPPSettingsDTO dto) {
        try {
            log.info("trying to add XmppSettings for user :" + dto.getUserId());
            stub.addUserXmppSettings(dto);
        } catch (Exception e) {
            log.error("Error when adding the XMPP Settings.", e);
        }
    }

    /**
     * edit xmpp settings
     * 
     * @param dto
     */
    public void editXmppSettings(XMPPSettingsDTO dto) {
        try {
            log.info("trying to edit the XmppSettings of user :" + dto.getUserId());
            stub.editXmppSettings(dto);
        } catch (Exception e) {
            log.error("Error when updating the XMPP Settings.", e);
        }
    }

    /**
     * get XMPP settings
     * 
     * @param userId
     * @return XmppSettingsDTO containing the user's XMPP config. details.
     */
    public XMPPSettingsDTO getXmppSettingsDTO(String userId) {
        try {
            log.info("trying to retrieve the XmppSettings of user :" + userId);
            return stub.getXmppSettings(userId);
        } catch (Exception e) {
            log.error("Error when retrieving the XMPP Settings.", e);
            return null;
        }
    }

    /**
     * Checks whether the user has enabled XMPP based multifactor auth.
     * 
     * @param userId
     * @return
     */
    public boolean isXmppSettingsEnabled(String userId) {
        try {
            return stub.isXMPPSettingsEnabled(userId);
        } catch (Exception e) {
            log
                    .error(
                            "Error when checking whether the XMPP based multifactor authentication is enabled.",
                            e);
        }
        return false;
    }

    /**
     * checks whether the user has added XMPP Settings.
     * 
     * @param userId
     * @return
     */
    public boolean isXMPPSettingsAvailable(String userId) {
        try {
            return stub.hasXMPPSettings(userId);
        } catch (Exception e) {
            log
                    .error(
                            "Error when checking whether the XMPP based multifactor authentication is enabled.",
                            e);
        }
        return false;
    }

    /**
     * Retrieve the IM address of an user to populate the IM field of the user.
     * 
     * @param userID
     * @return
     * @throws RemoteException
     */
    public String getUserIM(String userID) {
        try {
            return stub.getUserIM(userID);
        } catch (Exception e) {
            log.error("Error when retreiving IM", e);
        }
        return null;
    }

}
