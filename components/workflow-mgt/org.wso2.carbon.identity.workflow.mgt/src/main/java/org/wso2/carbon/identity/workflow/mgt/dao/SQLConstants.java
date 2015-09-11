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
    public static final String DESCRIPTION_COLUMN = "DESCRIPTION";

    public static final String ID_COLUMN = "ID";
    public static final String WF_NAME_COLUMN = "WF_NAME";
    public static final String TEMPLATE_ID_COLUMN = "TEMPLATE_ID";
    public static final String TEMPLATE_IMPL_ID_COLUMN = "IMPL_ID";
    public static final String TENANT_ID_COLUMN = "TENANT_ID";
    public static final String CONDITION_COLUMN = "ASSOC_CONDITION";
    public static final String PROFILE_NAME_COLUMN = "PROFILE_NAME";
    public static final String HOST_URL_COLUMN = "HOST_URL";
    public static final String USERNAME_COLUMN = "USERNAME";
    public static final String PASSWORD_COLUMN = "PASSWORD";
    public static final String CALLBACK_USER_COLUMN = "CALLBACK_USERNAME";
    public static final String CALLBACK_PASSWORD_COLUMN = "CALLBACK_PASSWORD";

    public static final String WORKFLOW_ID_COLUMN = "WORKFLOW_ID";
    public static final String EVENT_ID_COLUMN = "EVENT_ID";
    public static final String PARAM_NAME_COLUMN = "PARAM_NAME";
    public static final String PARAM_VALUE_COLUMN = "PARAM_VALUE";
    public static final String ASSOCIATION_NAME_COLUMN = "ASSOC_NAME";
    public static final String ENTITY_NAME_COLUMN = "ENTITY_NAME";
    public static final String ASSOCIATION_IS_ENABLED = "IS_ENABLED";

    public static final String REQUEST_UUID_COLUMN = "UUID";
    public static final String REQUEST_OPERATION_TYPE_COLUMN = "OPERATION_TYPE";
    public static final String REQUEST_CREATED_AT_COLUMN = "CREATED_AT";
    public static final String REQUEST_UPDATED_AT_COLUMN = "UPDATED_AT";
    public static final String REQUEST_STATUS_COLUMN = "STATUS";
    public static final String REQUEST_ID_COLUMN = "REQUEST_ID";
    public static final String CREATED_BY_COLUMN = "CREATED_BY";


    public static final String ADD_WORKFLOW_REQUEST_QUERY =
            "INSERT INTO WF_REQUEST(UUID, CREATED_BY, OPERATION_TYPE, CREATED_AT, UPDATED_AT, REQUEST, " +
                    "STATUS, TENANT_ID) VALUES" +
            " (?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String GET_WORKFLOW_REQUEST_QUERY = "SELECT UUID, REQUEST, STATUS, CREATED_BY FROM WF_REQUEST" +
            " WHERE UUID = ?";

    public static final String UPDATE_STATUS_OF_REQUEST = "UPDATE WF_REQUEST SET STATUS = ? , UPDATED_AT = ? WHERE " +
            "UUID = ?";

    public static final String UPDATE_UPDATED_AT_OF_REQUEST = "UPDATE WF_REQUEST SET UPDATED_AT = ? WHERE UUID = ?";

    public static final String ADD_WORKFLOW_QUERY = "INSERT INTO WF_WORKFLOW(ID, WF_NAME, DESCRIPTION, TEMPLATE_ID, " +
            "IMPL_ID, TENANT_ID) VALUES (?,?, ?, ?, ?, ?)";

    public static final String ADD_WORKFLOW_PARAMS_QUERY = "INSERT INTO WF_WORKFLOW_CONFIG_PARAM(WORKFLOW_ID, " +
            "PARAM_NAME, PARAM_VALUE) VALUES(?, ?, ?)";

    public static final String GET_ASSOCIATIONS_FOR_EVENT_QUERY = "SELECT WF_WORKFLOW_ASSOCIATION.WORKFLOW_ID, " +
            "WF_WORKFLOW.TEMPLATE_ID, WF_WORKFLOW.IMPL_ID, WF_WORKFLOW_ASSOCIATION.ASSOC_CONDITION FROM WF_WORKFLOW, " +
            "WF_WORKFLOW_ASSOCIATION WHERE WF_WORKFLOW_ASSOCIATION.EVENT_ID = ? AND WF_WORKFLOW_ASSOCIATION" +
            ".WORKFLOW_ID = WF_WORKFLOW.ID AND WF_WORKFLOW.TENANT_ID = ? AND WF_WORKFLOW_ASSOCIATION.IS_ENABLED = '1'";

    public static final String GET_ASSOCIATIONS_FOR_WORKFLOW_QUERY = "SELECT WF_WORKFLOW.WF_NAME, " +
                                                                     "WF_WORKFLOW_ASSOCIATION.ID, " +
                                                                     "WF_WORKFLOW_ASSOCIATION.ASSOC_NAME, " +
                                                                     "WF_WORKFLOW_ASSOCIATION.EVENT_ID, " +
                                                                     "WF_WORKFLOW_ASSOCIATION.ASSOC_CONDITION, " +
                                                                     "WF_WORKFLOW_ASSOCIATION.IS_ENABLED FROM " +
                                                                     "WF_WORKFLOW, WF_WORKFLOW_ASSOCIATION WHERE " +
                                                                     "WF_WORKFLOW.ID =" +
                                                                     " WF_WORKFLOW_ASSOCIATION.WORKFLOW_ID AND " +
                                                                     "WF_WORKFLOW.ID = ?";

    public static final String GET_ALL_ASSOCIATIONS_QUERY = "SELECT WF_WORKFLOW.WF_NAME, WF_WORKFLOW_ASSOCIATION.ID, " +
                                                            "WF_WORKFLOW_ASSOCIATION.ASSOC_NAME, " +
                                                            "WF_WORKFLOW_ASSOCIATION.EVENT_ID, " +
                                                            "WF_WORKFLOW_ASSOCIATION" +
                                                            ".ASSOC_CONDITION , WF_WORKFLOW_ASSOCIATION.IS_ENABLED " +
                                                            "FROM WF_WORKFLOW,WF_WORKFLOW_ASSOCIATION WHERE " +
                                                            "WF_WORKFLOW.ID = WF_WORKFLOW_ASSOCIATION" +
                                                            ".WORKFLOW_ID";

    public static final String DELETE_ASSOCIATION_QUERY = "DELETE FROM WF_WORKFLOW_ASSOCIATION WHERE ID = ?";

    public static final String GET_ASSOCIATION_FOR_ASSOC_ID_QUERY =
            "SELECT WF_WORKFLOW.WF_NAME, WF_WORKFLOW_ASSOCIATION.ID, WF_WORKFLOW_ASSOCIATION.ASSOC_NAME, " +
            "WF_WORKFLOW_ASSOCIATION.EVENT_ID," +
            " WF_WORKFLOW_ASSOCIATION.ASSOC_CONDITION , WF_WORKFLOW_ASSOCIATION.IS_ENABLED, WF_WORKFLOW_ASSOCIATION" +
            ".WORKFLOW_ID FROM WF_WORKFLOW, WF_WORKFLOW_ASSOCIATION WHERE WF_WORKFLOW_ASSOCIATION.ID = ? AND  " +
            "WF_WORKFLOW.ID = WF_WORKFLOW_ASSOCIATION.WORKFLOW_ID";

    public static final String GET_WORKFLOW_PARAMS = "SELECT PARAM_NAME, PARAM_VALUE FROM WF_WORKFLOW_CONFIG_PARAM " +
            "WHERE WORKFLOW_ID = ?";

    public static final String GET_WORKFLOW =
            "SELECT WF_WORKFLOW.WF_NAME, WF_WORKFLOW.DESCRIPTION, WF_WORKFLOW.TEMPLATE_ID, WF_WORKFLOW.IMPL_ID, " +
            "WF_WORKFLOW.TENANT_ID FROM WF_WORKFLOW WHERE WF_WORKFLOW.ID = ?";


    public static final String DELETE_WORKFLOW_QUERY = "DELETE FROM WF_WORKFLOW WHERE ID = ?";

    public static final String ASSOCIATE_WF_TO_EVENT = "INSERT INTO WF_WORKFLOW_ASSOCIATION(EVENT_ID, " +
            "ASSOC_NAME, ASSOC_CONDITION, WORKFLOW_ID) VALUES (?, ?, ?, ?)";

    public static final String UPDATE_ASSOCIATE_WF_TO_EVENT = "UPDATE WF_WORKFLOW_ASSOCIATION SET EVENT_ID=?, " +
                                                              "ASSOC_NAME=?, ASSOC_CONDITION=?, WORKFLOW_ID=?, " +
                                                              "IS_ENABLED=? WHERE ID = ? ";

    public static final String LIST_WORKFLOWS_QUERY = "SELECT ID, WF_NAME, DESCRIPTION, TEMPLATE_ID, IMPL_ID FROM " +
            "WF_WORKFLOW WHERE TENANT_ID = ? ORDER BY WF_NAME";

    //queries for BPS profiles
    public static final String ADD_BPS_PROFILE_QUERY =
            "INSERT INTO WF_BPS_PROFILE(PROFILE_NAME, HOST_URL, USERNAME, PASSWORD, CALLBACK_USERNAME," +
                    " CALLBACK_PASSWORD, TENANT_ID) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public static final String UPDATE_BPS_PROFILE_QUERY =
            "UPDATE WF_BPS_PROFILE SET HOST_URL=?, USERNAME=?, PASSWORD=?, CALLBACK_USERNAME=?, " +
            "CALLBACK_PASSWORD=? WHERE TENANT_ID=? AND PROFILE_NAME=?";

    public static final String GET_BPS_PROFILE_QUERY = "SELECT HOST_URL, USERNAME, PASSWORD, " +
            "CALLBACK_USERNAME, CALLBACK_PASSWORD FROM WF_BPS_PROFILE WHERE PROFILE_NAME = ?";

    public static final String GET_BPS_PROFILE_FOR_TENANT_QUERY = "SELECT HOST_URL, USERNAME,PASSWORD,  " +
                                                                  "CALLBACK_USERNAME, CALLBACK_PASSWORD FROM " +
                                                                  "WF_BPS_PROFILE WHERE PROFILE_NAME = ? AND " +
                                                                  "TENANT_ID = ? ";

    public static final String LIST_BPS_PROFILES_QUERY =
            "SELECT PROFILE_NAME, HOST_URL, USERNAME, CALLBACK_USERNAME FROM " +
                    "WF_BPS_PROFILE WHERE TENANT_ID = ?";

    public static final String DELETE_BPS_PROFILES_QUERY = "DELETE FROM WF_BPS_PROFILE WHERE PROFILE_NAME = ?";

    public static final String ADD_REQUEST_ENTITY_RELATIONSHIP = "INSERT INTO WF_REQUEST_ENTITY_RELATIONSHIP" +
            "(REQUEST_ID, ENTITY_NAME, " +
            "ENTITY_TYPE, TENANT_ID) VALUES (?, ?, ?, ?)";

    public static final String DELETE_REQUEST_ENTITY_RELATIONSHIP = "DELETE FROM WF_REQUEST_ENTITY_RELATIONSHIP WHERE" +
            " REQUEST_ID = ?";

    public static final String GET_PENDING_RELATIONSHIPS_OF_ENTITY = "SELECT WF_REQUEST_ENTITY_RELATIONSHIP" +
            ".ENTITY_NAME,WF_REQUEST_ENTITY_RELATIONSHIP.REQUEST_ID  FROM WF_REQUEST,WF_REQUEST_ENTITY_RELATIONSHIP " +
            "WHERE WF_REQUEST.UUID = WF_REQUEST_ENTITY_RELATIONSHIP.REQUEST_ID  AND WF_REQUEST_ENTITY_RELATIONSHIP" +
            ".ENTITY_TYPE = ? AND WF_REQUEST_ENTITY_RELATIONSHIP.ENTITY_NAME = ? AND WF_REQUEST.STATUS = ? AND " +
            "WF_REQUEST_ENTITY_RELATIONSHIP.TENANT_ID = ?";

    public static final String GET_PENDING_RELATIONSHIPS_OF_GIVEN_TYPE_FOR_ENTITY = "SELECT " +
            "WF_REQUEST_ENTITY_RELATIONSHIP.ENTITY_NAME,WF_REQUEST_ENTITY_RELATIONSHIP.REQUEST_ID  FROM WF_REQUEST," +
            "WF_REQUEST_ENTITY_RELATIONSHIP WHERE WF_REQUEST.UUID = WF_REQUEST_ENTITY_RELATIONSHIP.REQUEST_ID  AND " +
            "WF_REQUEST_ENTITY_RELATIONSHIP.ENTITY_TYPE = ? AND WF_REQUEST_ENTITY_RELATIONSHIP.ENTITY_NAME = ? AND " +
            "WF_REQUEST.STATUS = ? AND WF_REQUEST.OPERATION_TYPE = ? AND WF_REQUEST_ENTITY_RELATIONSHIP.TENANT_ID = ?";

    public static final String GET_REQUESTS_OF_TWO_ENTITIES = "SELECT TABLE1.REQUEST_ID from " +
            "WF_REQUEST_ENTITY_RELATIONSHIP AS TABLE1, WF_REQUEST_ENTITY_RELATIONSHIP AS TABLE2 WHERE TABLE1" +
            ".REQUEST_ID = TABLE2.REQUEST_ID AND TABLE1.ENTITY_NAME = ? AND TABLE1.ENTITY_TYPE= ? AND TABLE2" +
            ".ENTITY_NAME = ? AND TABLE2.ENTITY_TYPE = ? AND TABLE1.TENANT_ID = ? AND TABLE2.TENANT_ID = ?";

    public static final String GET_REQUEST_ENTITY_NAMES = "SELECT WF_REQUEST_ENTITY_RELATIONSHIP.ENTITY_NAME FROM " +
            "WF_REQUEST,WF_REQUEST_ENTITY_RELATIONSHIP WHERE WF_REQUEST.UUID = WF_REQUEST_ENTITY_RELATIONSHIP.REQUEST_ID"
            + " AND WF_REQUEST.OPERATION_TYPE = ? AND  WF_REQUEST.STATUS  = ? AND " +
            "WF_REQUEST_ENTITY_RELATIONSHIP.ENTITY_TYPE = ? AND WF_REQUEST_ENTITY_RELATIONSHIP.TENANT_ID = ? ";

    public static final String GET_REQUESTS_OF_USER = "SELECT UUID, OPERATION_TYPE, CREATED_AT, UPDATED_AT, STATUS, " +
            "REQUEST FROM WF_REQUEST WHERE CREATED_BY = ? AND TENANT_ID = ?";

    public static final String GET_REQUESTS_OF_USER_FILTER_FROM_CREATED_TIME = "SELECT UUID, OPERATION_TYPE, " +
            "CREATED_AT, UPDATED_AT, STATUS, REQUEST, CREATED_BY FROM WF_REQUEST WHERE CREATED_BY = ? AND CREATED_AT > ? AND " +
            "CREATED_AT < ? AND TENANT_ID = ? LIMIT ?";

    public static final String GET_REQUESTS_OF_USER_FILTER_FROM_UPDATED_TIME = "SELECT UUID, OPERATION_TYPE, " +
            "CREATED_AT, UPDATED_AT, STATUS, REQUEST, CREATED_BY FROM WF_REQUEST WHERE CREATED_BY = ? AND UPDATED_AT > ? AND " +
            "UPDATED_AT < ? AND TENANT_ID = ? LIMIT ?";

    public static final String GET_REQUESTS_FILTER_FROM_CREATED_TIME = "SELECT UUID, OPERATION_TYPE, " +
            "CREATED_AT, UPDATED_AT, STATUS, REQUEST, CREATED_BY FROM WF_REQUEST WHERE CREATED_AT > ? AND CREATED_AT < ? AND " +
            "TENANT_ID = ? LIMIT ?";

    public static final String GET_REQUESTS_FILTER_FROM_UPDATED_TIME = "SELECT UUID, OPERATION_TYPE, " +
            "CREATED_AT, UPDATED_AT, STATUS, REQUEST, CREATED_BY FROM WF_REQUEST WHERE UPDATED_AT > ? AND UPDATED_AT < ? AND " +
            "TENANT_ID = ? LIMIT ?";

    public static final String ADD_WORKFLOW_REQUEST_RELATIONSHIP = "INSERT INTO WORKFLOW_REQUEST_RELATION " +
            "(RELATIONSHIP_ID, WORKFLOW_ID, REQUEST_ID, UPDATED_AT, STATUS) VALUES (?, ?, ?, ?, ?)";

    public static final String GET_REQUEST_ID_OF_RELATIONSHIP = "SELECT REQUEST_ID FROM " +
            "WORKFLOW_REQUEST_RELATION WHERE RELATIONSHIP_ID = ?";

    public static final String GET_STATES_OF_REQUEST = "SELECT STATUS FROM " +
            "WORKFLOW_REQUEST_RELATION WHERE REQUEST_ID = ?";

    public static final String UPDATE_STATUS_OF_RELATIONSHIP = "UPDATE WORKFLOW_REQUEST_RELATION SET STATUS = " +
            "? , UPDATED_AT = ? WHERE RELATIONSHIP_ID = ?";

    public static final String GET_STATUS_OF_RELATIONSHIP = "SELECT STATUS FROM " +
            "WORKFLOW_REQUEST_RELATION WHERE RELATIONSHIP_ID = ?";

    public static final String GET_WORKFLOWS_OF_REQUEST = "SELECT WF_WORKFLOW.ID, WF_WORKFLOW.WF_NAME , " +
            "WORKFLOW_REQUEST_RELATION.STATUS, WORKFLOW_REQUEST_RELATION.UPDATED_AT FROM WF_WORKFLOW, " +
            "WORKFLOW_REQUEST_RELATION WHERE WF_WORKFLOW.ID = WORKFLOW_REQUEST_RELATION.WORKFLOW_ID AND" +
            " WORKFLOW_REQUEST_RELATION.REQUEST_ID = ?";

    public static final int maxResultsPerRequest = 1000;

    private SQLConstants() {

    }

}
