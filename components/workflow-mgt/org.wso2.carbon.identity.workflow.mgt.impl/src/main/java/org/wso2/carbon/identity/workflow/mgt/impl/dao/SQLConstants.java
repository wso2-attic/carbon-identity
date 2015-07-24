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

package org.wso2.carbon.identity.workflow.mgt.impl.dao;

public class SQLConstants {



    public static final String ADD_ENTITY_STATE_QUERY = "INSERT INTO WF_ENTITY_LOCK_STATE VALUES (?, ?, ?, ?)";

    public static final String GET_ENTITY_STATE_QUERY = "SELECT ATTRIBUTE_VALUE FROM WF_ENTITY_LOCK_STATE WHERE " +
            "ENTITY_ID=? AND ENTITY_TYPE=? AND ATTRIBUTE_KEY=? AND ATTRIBUTE_VALUE=?";

    public static final String DELETE_ENTITY_STATE_QUERY = "DELETE FROM WF_ENTITY_LOCK_STATE " +
            "WHERE ENTITY_ID=? AND ENTITY_TYPE=? AND ATTRIBUTE_KEY=? AND ATTRIBUTE_VALUE=?";

    public static final String GET_ENTITY_STATE_LIST = "SELECT ATTRIBUTE_VALUE FROM WF_ENTITY_LOCK_STATE WHERE " +
            "ENTITY_ID=? AND ENTITY_TYPE=?";


    private SQLConstants(){

    }

}
