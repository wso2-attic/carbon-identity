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
package org.wso2.carbon.identity.scim.common.utils;

/**
 * Class to hold Identity SCIM Constants.
 */
public class SCIMCommonConstants {
    public static final String IS_ROLE_NAME_CHANGED_ON_UPDATE = "ISRoleNameChangedOnUpdate";
    public static final String OLD_GROUP_NAME = "OldGroupName";
    //public static final String PROVISIONING_ADMIN_PERMISSION = "/permission/admin/configure/security";
    /*This permission is required to see the 'my scim providers ui' to configure providers per account
    * and also to authorize provisioning requests.*/
    public static final String PROVISIONING_ADMIN_PERMISSION = "/permission/admin/configure/security/usermgt/provisioning";
    /*This permission is required to see the tenant space providers*/
    public static final String PROVISIONING_ADMIN_GLOBAL_CONFIG_PERMISSION = "/permission/admin/configure/security";
    /*Users with this permission can provision their profile updates*/
    public static final String PROVISIONING_USER_PERMISSION = "/permission/admin/login";
    public static final String RESOURCE_TO_BE_AUTHORIZED = "ui.execute";

    //cache id
    public static final String SCIM_CACHE = "SCIM_CACHE";

}

