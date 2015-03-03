/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.core.util.AdminServicesUtil;
import org.wso2.carbon.identity.base.IdentityConstants.ServerConfig;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
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

    protected Log log = LogFactory.getLog(IdentityProviderService.class);


    /**
     * @param userName
     * @return
     * @throws Exception
     * @throws
     */
    public String getPrimaryOpenID(String userName) throws Exception {
        userName = getUserNameWithDomain(userName);
        validateInputParameters(new String[]{userName}, "Invalid parameters provided to getOpenID");
        checkUserAuthorization(userName, "getOpenID");
        return IdentityUtil.getProperty(ServerConfig.OPENID_USER_PATTERN) + userName;
    }

    /**
     * @param userName
     * @return
     * @throws Exception
     */
    public String[] getAllOpenIDs(String userName) throws Exception {
        validateInputParameters(new String[]{userName},
                "Invalid parameters provided to getAllOpenIDs");
        // checkUserAuthorization(extractPrimaryUserName(userName), "getAllOpenIDs");

        IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();

        // Get all External OpenIDs of an user
        String[] externalOpenIDs = persistenceManager.getOpenIDsForUser(IdentityTenantUtil.getRegistry()
                , AdminServicesUtil.getUserRealm(), userName);

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
            persistenceManager.removeOpenIDSignUp(IdentityTenantUtil.getRegistry(),
                    AdminServicesUtil.getUserRealm(), openID);

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
    private void checkUserAuthorization(String username, String operation)
            throws IdentityProviderException {
        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgContext
                .getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession(false);

        if (username.contains("@")) {
            if (MultitenantUtils.isEmailUserName()) {
                String[] partitionedUserName = username.trim().split("@");
                username = partitionedUserName[0] + "@" + partitionedUserName[1];
            } else {
                username = username.substring(0, username.indexOf("@"));
            }
        }

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

    private String getUserNameWithDomain(String userName) {
        if (userName == null) {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
        }
        if (MultitenantUtils.getTenantDomain(userName) == null) {
            if (CarbonContext.getThreadLocalCarbonContext().getTenantDomain() != null) {
                userName = userName + "@" + CarbonContext.getThreadLocalCarbonContext().getTenantDomain(); //TODO no constant for @
            }
        }

        return userName;
    }
}