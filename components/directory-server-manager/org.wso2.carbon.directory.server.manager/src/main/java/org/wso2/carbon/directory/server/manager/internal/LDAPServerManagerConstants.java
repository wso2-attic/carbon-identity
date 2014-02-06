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

package org.wso2.carbon.directory.server.manager.internal;

/**
 * This class defines static constants used in LDAP server manager component.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class LDAPServerManagerConstants {

    static final String SERVER_PRINCIPAL_ATTRIBUTE_VALUE = "Service";
    static final String SERVER_PRINCIPAL_ATTRIBUTE_NAME = "sn";
    static final String PASSWORD_HASH_METHOD = "passwordHashMethod";
    static final String KRB5_PRINCIPAL_NAME_ATTRIBUTE = "krb5PrincipalName";
    static final String KRB5_KEY_VERSION_NUMBER_ATTRIBUTE = "krb5KeyVersionNumber";
    static final String PASSWORD_HASH_METHOD_PLAIN_TEXT = "PlainText";
    static final String KERBEROS_TGT = "krbtgt";

    //LDAP constants
    static final String LDAP_UID = "uid";
    static final String LDAP_PASSWORD = "userPassword";
    static final String LDAP_OBJECT_CLASS = "objectClass";
    static final String LDAP_INTET_ORG_PERSON = "inetOrgPerson";
    static final String LDAP_ORG_PERSON = "organizationalPerson";
    static final String LDAP_PERSON = "person";
    static final String LDAP_TOP = "top";
    static final String LDAP_KRB5_PRINCIPLE = "krb5principal";
    static final String LDAP_KRB5_KDC = "krb5kdcentry";
    static final String LDAP_SUB_SCHEMA = "subschema";
    static final String LDAP_COMMON_NAME = "cn";

    public static final String SERVICE_PASSWORD_REGEX_PROPERTY = "ServicePasswordJavaRegEx";
    public static final String SERVICE_PRINCIPLE_NAME_REGEX_PROPERTY = "ServiceNameJavaRegEx";
    public static final String DEFAULT_PASSWORD_REGULAR_EXPRESSION = "[\\\\S]{5,30}";
    public static final String DEFAULT_SERVICE_NAME_REGULAR_EXPRESSION = "[a-zA-Z\\d]{2,10}/[a-zA-Z]{2,30}";

    // For back-end we have to use following default values
    static final String DEFAULT_BE_PASSWORD_REGULAR_EXPRESSION = "[\\S]{5,30}";
    static final String DEFAULT_BE_SERVICE_NAME_REGULAR_EXPRESSION = DEFAULT_SERVICE_NAME_REGULAR_EXPRESSION;
}
