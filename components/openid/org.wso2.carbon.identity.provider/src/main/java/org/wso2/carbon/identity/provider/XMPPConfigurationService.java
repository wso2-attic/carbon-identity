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

package org.wso2.carbon.identity.provider;

import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.util.AdminServicesUtil;
import org.wso2.carbon.identity.provider.dto.XMPPSettingsDTO;
import org.wso2.carbon.identity.provider.xmpp.XMPPConfigurator;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * This services has the functionality related to XMPP configuration functionality in the UI. To
 * access this service, users should have 'login' permission. Only the functionality related to
 * logged in user can be performed.
 */
public class XMPPConfigurationService {

    private static final Log log = LogFactory.getLog(XMPPConfigurationService.class);

    /**
     * Add XMPP Settings
     *
     * @param dto
     * @return success of the operation
     * @throws IdentityProviderException
     */
    public boolean addUserXmppSettings(XMPPSettingsDTO dto) throws IdentityProviderException {
        String message = "Invalid parameters provided to addUserXmppSettings";
        if (dto == null) {
            throw new IllegalArgumentException(message);
        } else {
            if (dto.getUserId() == null) {
                throw new IllegalArgumentException(message);
            }
        }
        validateInputParameters(new String[] { dto.getUserId(), dto.getXmppServer(),
                                               dto.getXmppUserName(), dto.getUserCode() }, message);
        checkUserAuthorization(dto.getUserId(), "addUserXmppSettings");

        XMPPConfigurator provider = new XMPPConfigurator();
        provider.addXmppSettings(dto);
        return true;
    }

    /**
     * Edit XMPP Settings of a user Edit XMPP Settings.
     *
     * @param dto
     * @throws IdentityProviderException
     */
    public void editXmppSettings(XMPPSettingsDTO dto) throws IdentityProviderException {
        String message = "Invalid parameters provided to editXmppSettings";
        if (dto == null) {
            throw new IllegalArgumentException(message);
        } else {
            if (dto.getUserId() == null) {
                throw new IllegalArgumentException(message);
            }
        }
        validateInputParameters(new String[] { dto.getUserId(), dto.getXmppServer(),
                                               dto.getXmppUserName(), dto.getUserCode() }, message);
        checkUserAuthorization(dto.getUserId(), "editXmppSettings");
        XMPPConfigurator provider = new XMPPConfigurator();
        provider.editXmppSettings(dto);
    }

    /**
     * Get XMPP Settings of a user
     *
     * @param userId
     * @return XmppSettingsDTO instance containing XMPP properties
     * @throws IdentityProviderException
     */
    public XMPPSettingsDTO getXmppSettings(String userId) throws IdentityProviderException {
        String message = "Invalid parameters provided to getXmppSettings";
        validateInputParameters(new String[] { userId }, message);
        checkUserAuthorization(userId, "getXmppSettings");
        XMPPConfigurator provider = new XMPPConfigurator();
        return provider.getXmppSettings(userId);
    }

    /**
     * Check whether the user has enabled XMPP based multi-factor authentication.
     *
     * @param userId
     * @return
     * @throws IdentityProviderException
     */
    public boolean isXMPPSettingsEnabled(String userId) throws IdentityProviderException {
        String message = "Invalid parameters provided to isXMPPSettingsEnabled";
        validateInputParameters(new String[] { userId }, message);
        checkUserAuthorization(userId, "isXMPPSettingsEnabled");
        XMPPConfigurator provider = new XMPPConfigurator();
        return provider.isXmppSettingsEnabled(userId);
    }

    /**
     * Whether the user has added XMPP settings.
     *
     * @param userId
     * @return
     * @throws IdentityProviderException
     */
    public boolean hasXMPPSettings(String userId) throws IdentityProviderException {
        String message = "Invalid parameters provided to hasXMPPSettings";
        validateInputParameters(new String[] { userId }, message);
        checkUserAuthorization(userId, "hasXMPPSettings");
        XMPPConfigurator provider = new XMPPConfigurator();
        return provider.hasXMPPSettings(userId);
    }

    /**
     * Get the IM Address of an user to populate the IM field of XMPP Configuration page.
     *
     * @param userId
     * @return
     * @throws IdentityProviderException
     */
    public String getUserIM(String userId) throws IdentityProviderException {
        String message = "Invalid parameters provided to hasXMPPSettings";
        validateInputParameters(new String[] { userId }, message);
        checkUserAuthorization(userId, "hasXMPPSettings");

        Map<String, String> claimValues = null;
        try {
            UserStoreManager userStore = AdminServicesUtil.getUserRealm().getUserStoreManager();
            String[] imClaim = { UserCoreConstants.ClaimTypeURIs.IM };
            claimValues = userStore.getUserClaimValues(userId, imClaim, UserCoreConstants.DEFAULT_PROFILE);
        } catch (UserStoreException | CarbonException e) {
            throw new IdentityProviderException("Failed to get claims for user " + userId);
        }

        if (claimValues.containsKey(UserCoreConstants.ClaimTypeURIs.IM)) {
            return claimValues.get(UserCoreConstants.ClaimTypeURIs.IM);
        } else {
            return null;
        }
    }

    /**
     * @param username
     * @param operation
     * @throws IdentityProviderException
     */
    private void checkUserAuthorization(String username, String operation)
            throws IdentityProviderException {
        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            String userName = (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
            if (!username.equals(userName)) {
                throw new IdentityProviderException("Unauthorised action by user " + username
                                                    + " to access " + operation);
            }
            return;
        }
        throw new IdentityProviderException("Unauthorised action by user " + username
                                            + " to access " + operation);
    }

    /**
     * @param params
     * @param message
     */
    private void validateInputParameters(String[] params, String message) {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null || params[i].trim().length() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(message);
                }
                throw new IllegalArgumentException(message);
            }
        }
    }
}