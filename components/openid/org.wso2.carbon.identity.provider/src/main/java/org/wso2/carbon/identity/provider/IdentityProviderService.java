/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.util.AdminServicesUtil;
import org.wso2.carbon.identity.base.IdentityConstants.ServerConfig;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.openid.OpenIDUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * This services has the functionality related to Information Cards / OpenID dash-board
 * functionality in the UI. To access this service, users should have 'login' permission. Only the
 * functionality related to logged in user can be performed.
 */
public class IdentityProviderService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(IdentityProviderService.class);

    /**
     * @param userName
     * @return
     * @throws Exception
     * @throws
     */
    public String getPrimaryOpenID(String userName) throws IdentityProviderException {
        String userNameWithDomain = getUserNameWithDomain(userName);
        validateInputParameters(new String[] { userNameWithDomain }, "Invalid parameters provided to getOpenID");
        checkUserAuthorization(userNameWithDomain, "getOpenID");
        return OpenIDUtil.getOpenIDUserPattern() + "/" + userNameWithDomain;
    }

    /**
     * @param userName
     * @return
     * @throws Exception
     */
    public String[] getAllOpenIDs(String userName) throws IdentityProviderException {
        validateInputParameters(new String[] { userName }, "Invalid parameters provided to getAllOpenIDs");

        IdentityPersistenceManager persistenceManager = null;
        String[] externalOpenIDs = null;
        try {
            persistenceManager = IdentityPersistenceManager.getPersistanceManager();

            // Get all External OpenIDs of an user
            externalOpenIDs = persistenceManager.getOpenIDsForUser(IdentityTenantUtil.getRegistry()
                    , AdminServicesUtil.getUserRealm(), userName);
        } catch (IdentityException | CarbonException e) {
            throw new IdentityProviderException("Failed to retrieve OpenID for user " + userName, e);
        }

        String[] openIDset = new String[externalOpenIDs.length + 1];
        // Index zero of the returning array would be the primary OpenID.
        openIDset[0] = getPrimaryOpenID(userName);

        // Append all the external OpenIDs to the end of the array.
        for (int i = 0; i < externalOpenIDs.length; i++) {
            openIDset[i + 1] = externalOpenIDs[i];
        }

        return openIDset;
    }

    public void removeOpenID(String openID) {
        try {

            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            persistenceManager
                    .removeOpenIDSignUp(IdentityTenantUtil.getRegistry(), AdminServicesUtil.getUserRealm(), openID);

        } catch (Exception e) {
            log.error("Error instantiating a Persistence Manager.", e);
        }
    }

    public void addOpenID(String openID) {
        try {

            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            String userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
            persistenceManager.doOpenIdSignUp(IdentityTenantUtil.getRegistry()
                    , AdminServicesUtil.getUserRealm(), openID, userName);

        } catch (Exception e) {
            log.error("Error instantiating a Persistence Manager.", e);
        }
    }

    /**
     * @param ppid
     * @return
     * @throws Exception
     */
    public String extractPrimaryUserName(String ppid) throws Exception {
        return IdentityUtil.getPPIDDisplayValue(ppid);
    }

    /**
     * @param username
     * @param operation
     * @throws IdentityProviderException
     */
    private void checkUserAuthorization(String username, String operation) throws IdentityProviderException {
        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession(false);

        String tenantFreeUsername = MultitenantUtils.getTenantAwareUsername(username);

        if (httpSession != null) {
            String loggedInUsername = (String) httpSession.getAttribute(ServerConstants.USER_LOGGED_IN);
            if (!tenantFreeUsername.equals(loggedInUsername)) {
                throw new IdentityProviderException("Unauthorised action by user " + username
                                                    + " to access " + operation);
            }
        } else {
            throw new IdentityProviderException("Unauthorised action by user " + tenantFreeUsername
                                                + " to access " + operation);
        }
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

    private String getUserNameWithDomain(String userName) {
        String usernameWithDomain;
        if (StringUtils.isBlank(userName)) {
            usernameWithDomain = CarbonContext.getThreadLocalCarbonContext().getUsername();
        } else {
            usernameWithDomain = userName;
        }
        if (MultitenantUtils.getTenantDomain(usernameWithDomain) == null &&
            CarbonContext.getThreadLocalCarbonContext().getTenantDomain() != null) {
            usernameWithDomain = usernameWithDomain + "@" + CarbonContext.getThreadLocalCarbonContext()
                                                                         .getTenantDomain();
        }

        return usernameWithDomain;
    }
}