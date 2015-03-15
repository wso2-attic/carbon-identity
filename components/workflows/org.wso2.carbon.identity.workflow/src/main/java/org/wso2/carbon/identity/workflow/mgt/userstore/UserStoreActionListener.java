/*
 * Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.mgt.userstore;

import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;
import org.wso2.carbon.workflow.mgt.WorkflowException;

import java.util.Map;

public class UserStoreActionListener extends AbstractUserOperationEventListener {

    private AddUserWFRequestHandler addUserWFRequestHandler;

    @Override
    public int getExecutionOrderId() {
        return 0;
    }

    @Override
    public boolean doPreAddUser(String s, Object o, String[] strings, Map<String, String> map, String s1,
                                UserStoreManager userStoreManager) throws UserStoreException {
        try {
            return new AddUserWFRequestHandler().startAddUserFlow(s, o, strings, map, s1);
        } catch (WorkflowException e) {
            e.printStackTrace(); //todo
        }
        return false;
    }

    @Override
    public boolean doPreUpdateCredential(String s, Object o, Object o1, UserStoreManager userStoreManager) throws
            UserStoreException {
        return false;
    }

    @Override
    public boolean doPreUpdateCredentialByAdmin(String s, Object o, UserStoreManager userStoreManager) throws
            UserStoreException {
        return false;
    }

    @Override
    public boolean doPreDeleteUser(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreSetUserClaimValue(String s, String s1, String s2, String s3, UserStoreManager
            userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreSetUserClaimValues(String s, Map<String, String> map, String s1, UserStoreManager
            userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreDeleteUserClaimValues(String s, String[] strings, String s1, UserStoreManager
            userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreDeleteUserClaimValue(String s, String s1, String s2, UserStoreManager userStoreManager)
            throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreAddRole(String s, String[] strings, Permission[] permissions, UserStoreManager
            userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreDeleteRole(String s, UserStoreManager userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreUpdateRoleName(String s, String s1, UserStoreManager userStoreManager) throws
            UserStoreException {
        return false;
    }

    @Override
    public boolean doPreUpdateUserListOfRole(String s, String[] strings, String[] strings1, UserStoreManager
            userStoreManager) throws UserStoreException {
        return false;
    }

    @Override
    public boolean doPreUpdateRoleListOfUser(String s, String[] strings, String[] strings1, UserStoreManager
            userStoreManager) throws UserStoreException {
        return false;
    }
}
