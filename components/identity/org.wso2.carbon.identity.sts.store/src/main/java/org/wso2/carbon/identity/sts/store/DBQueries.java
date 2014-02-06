/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.sts.store;


public class DBQueries {

    public static final String ADD_TOKEN = "INSERT INTO IDN_STS_STORE (TOKEN_ID, TOKEN_CONTENT,CREATE_DATE," +
            " EXPIRE_DATE,STATE)  VALUES (?,?,?,?,?)";

    public static final String UPDATE_TOKEN = "UPDATE  IDN_STS_STORE SET TOKEN_CONTENT = ? ,CREATE_DATE = ?," +
            "EXPIRE_DATE = ?, STATE = ?  WHERE TOKEN_ID = ?";

    public static final String REMOVE_TOKEN = "DELETE FROM  IDN_STS_STORE WHERE TOKEN_ID = ?";

    public static final String ALL_TOKEN_KEYS = "SELECT TOKEN_ID  FROM  IDN_STS_STORE";

    public static final String GET_TOKEN = "SELECT TOKEN_CONTENT  FROM  IDN_STS_STORE  WHERE TOKEN_ID = ?";

    public static final String GET_ALL_TOKENS = "SELECT *  FROM  IDN_STS_STORE";

    public static final String VALID_TOKENS = "SELECT *  FROM  IDN_STS_STORE WHERE STATE =? OR STATE =?";

    public static final String GET_TOKENS_BY_STATE = "SELECT *  FROM  IDN_STS_STORE WHERE STATE = ?";

    public static final String TOKENS_EXISTS = "SELECT 1  FROM  IDN_STS_STORE";
}
