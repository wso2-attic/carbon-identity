/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.application.mgt.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.ApplicationMgtDBQueries;
import org.wso2.carbon.identity.application.mgt.dao.OAuthApplicationDAO;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class OAuthApplicationDAOImpl implements OAuthApplicationDAO {

    public static final Log log = LogFactory.getLog(OAuthApplicationDAOImpl.class);

    /**
     *
     */
    @Override
    public void removeOAuthApplication(String clientIdentifier) throws IdentityApplicationManagementException {

        Connection connection = IdentityDatabaseUtil.getDBConnection();
        PreparedStatement prepStmt = null;
        try {
            prepStmt = connection.prepareStatement(ApplicationMgtDBQueries.REMOVE_OAUTH_APPLICATION);
            prepStmt.setString(1, clientIdentifier);
            prepStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + ApplicationMgtDBQueries.REMOVE_OAUTH_APPLICATION);
            log.error(e.getMessage(), e);
            throw new IdentityApplicationManagementException("Error removing the consumer application.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

}
