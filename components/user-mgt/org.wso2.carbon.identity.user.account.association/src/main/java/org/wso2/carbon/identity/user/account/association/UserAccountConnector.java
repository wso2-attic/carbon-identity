/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.account.association;

import org.wso2.carbon.identity.user.account.association.dto.UserAccountAssociationDTO;
import org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException;

public interface UserAccountConnector {

    /**
     * Create new user account association
     *
     * @param userName1
     * @param userName2
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    void createUserAccountAssociation(String userName1, String userName2) throws UserAccountAssociationException;

    /**
     * Delete an existing user account association
     *
     * @param userName
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    void deleteUserAccountAssociation(String userName) throws UserAccountAssociationException;

    /**
     * Get all associated accounts of the logged in user
     *
     * @param userName userName to get account list of
     * @return
     * @throws UserAccountAssociationException
     */
    UserAccountAssociationDTO[] getAccountAssociationsOfUser(String userName) throws UserAccountAssociationException;

    /**
     * Switch logged in user account to the required associated user account
     *
     * @param userName
     * @return
     * @throws org.wso2.carbon.identity.user.account.association.exception.UserAccountAssociationException
     */
    boolean switchLoggedInUser(String userName) throws UserAccountAssociationException;

}
