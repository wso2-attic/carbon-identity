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

package org.wso2.carbon.identity.workflow.mgt.dao;

/**
 * Holds the SQL queries and constants
 */
public class SQLConstants {

    public static final String REQUEST_COLUMN = "REQUEST";
    public static final String ALIAS_COLUMN = "ALIAS";
    public static final String WS_ACTION_COLUMN = "WS_ACTION";
    public static final String SERVICE_EP_COLUMN = "SERVICE_EP";
    public static final String PRIORITY_COLUMN = "PRIORITY";
    public static final String USERNAME_COLUMN = "USERNAME";
    public static final String PASSWORD_COLUMN = "PASSWORD";
    public static final String CONDITION_COLUMN = "CONDITION";


    public static final String ADD_WORKFLOW_REQUEST_QUERY = "INSERT INTO WF_REQUESTS(UUID, CREATED_AT, UPDATED_AT, " +
            "REQUEST, STATUS) VALUES (?, ?, ?, ?, ?)";

    public static final String GET_WORKFLOW_REQUEST_QUERY = "SELECT UUID, REQUEST, STATUS FROM WF_REQUESTS WHERE UUID" +
            " = ?";

    public static final String ADD_WS_SERVICE_QUERY = "INSERT INTO WF_WS_SERVICES(ALIAS, WS_ACTION, SERVICE_EP, " +
            "USERNAME, PASSWORD) VALUES (?, ?, ?, ?, ?)";

    public static final String GET_WS_SERVICES_FOR_EVENT_QUERY = "SELECT WF_WS_SERVICES.ALIAS, " +
            "WF_WS_SERVICES.WS_ACTION, WF_WS_SERVICES.SERVICE_EP, WF_EVENT_SUBSCRIPTIONS.PRIORITY, WF_WS_SERVICES.USERNAME, " +
            "WF_WS_SERVICES.PASSWORD, WF_EVENT_SUBSCRIPTIONS.CONDITION FROM WF_WS_SERVICES, " +
            "WF_EVENT_SUBSCRIPTIONS WHERE WF_EVENT_SUBSCRIPTIONS.SERVICE_ALIAS = WF_WS_SERVICES.ALIAS AND " +
            "WF_EVENT_SUBSCRIPTIONS.EVENT = ?";

    public static final String DELETE_WS_SERVICE_QUERY = "DELETE FROM WF_WS_SERVICES WHERE ALIAS = ?";

    public static final String ASSOCIATE_SERVICE_TO_ACTION = "INSERT INTO WF_EVENT_SUBSCRIPTIONS(SERVICE_ALIAS, " +
            "EVENT, CONDITION, PRIORITY) VALUES (?, ?, ?, ?)";

    private SQLConstants(){
    }
}
