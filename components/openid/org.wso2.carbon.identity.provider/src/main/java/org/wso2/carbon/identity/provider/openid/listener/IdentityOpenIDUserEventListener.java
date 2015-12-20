/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider.openid.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.AbstractIdentityUserOperationEventListener;
import org.wso2.carbon.identity.core.model.OpenIDUserRPDO;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.dao.OpenIDUserRPDAO;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

public class IdentityOpenIDUserEventListener extends AbstractIdentityUserOperationEventListener {

    private static final Log log = LogFactory.getLog(IdentityOpenIDUserEventListener.class);

    @Override
    public int getExecutionOrderId() {
        int orderId = getOrderId();
        if (orderId != IdentityCoreConstants.EVENT_LISTENER_ORDER_ID) {
            return orderId;
        }
        return 70;
    }

    @Override
    public boolean doPreDeleteUser(String userName, UserStoreManager userStoreManager) throws UserStoreException {

        if (!isEnable()) {
            return true;
        }

        if (log.isDebugEnabled()) {
            log.debug("Clearing OpenID related information of the user : " + userName);
        }

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            deleteUsersRPs(userName, tenantId);
        } catch (IdentityException e) {
            throw new UserStoreException("Error occurred while deleting RP entries in the DB related to the user", e);
        }
        return true;
    }

    /**
     * Function to delete all RPs registered to the user in the database
     *
     * @param userName username of the user which need to clear RPs
     * @throws org.wso2.carbon.identity.base.IdentityException if error occurred while retrieving or deleting user RPs from the DB
     */
    private void deleteUsersRPs(String userName, int tenantId) throws IdentityException {
        //get all RPs
        OpenIDUserRPDAO dao = new OpenIDUserRPDAO();

        //retrieve all RP entries in the DB
        OpenIDUserRPDO[] userRPs = dao.getOpenIDUserRPs(userName, tenantId);

        if (log.isDebugEnabled()) {
            log.debug("Clearing " + userRPs.length + " RPs in DB related to the user : " + userName);
        }
        //delete each RP entry in the DB related to the user
        for (int i = 0; i < userRPs.length; i++) {
            //delete each relying party entries in database
            dao.delete(userRPs[i], tenantId);
        }
    }

}
