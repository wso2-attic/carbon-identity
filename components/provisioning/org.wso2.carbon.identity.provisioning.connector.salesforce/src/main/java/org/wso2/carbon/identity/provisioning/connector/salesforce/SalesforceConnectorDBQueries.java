/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.provisioning.connector.salesforce;

public class SalesforceConnectorDBQueries {

    private SalesforceConnectorDBQueries(){}

    public static final String SALESFORCE_LIST_USER_SIMPLE_QUERY = "SELECT Id, Alias, Email, LastName, Name, ProfileId, Username from User";
    public static final String SALESFORCE_LIST_USER_FULL_QUERY = "SELECT Id, Username, Name, Alias, Email, EmailEncodingKey, LanguageLocaleKey, LastName, LocaleSidKey, ProfileId, TimeZoneSidKey, UserPermissionsCallCenterAutoLogin, UserPermissionsMarketingUser, UserPermissionsOfflineUser from User";
}
