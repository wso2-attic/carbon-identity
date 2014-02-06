/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authorization.core.jdbc;

/**
 * 
 */
public class JDBCConstants {

    public static final String GET_PERMISSIONS_OF_USER = "select UM_PERMISSION_ID, UM_IS_ALLOWED from UM_USER_PERMISSION where UM_USER_NAME=? AND UM_TENANT_ID=?";

    public static final String GET_RESOURCE_ID = "select UM_RESOURCE_ID, UM_ACTION from UM_PERMISSION where UM_ID=? AND UM_TENANT_ID=?"; 

    public static final String GET_PERMISSIONS_OF_ROLE = "select UM_PERMISSION_ID, UM_IS_ALLOWED from UM_ROLE_PERMISSION where UM_ROLE_NAME=? AND UM_TENANT_ID=?";

}
