/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.apacheds.impl;

/**
 * Class which encapsulates, most of the configuration parameters used in apacheds implementation.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ConfigurationConstants {

    public static final String SIMPLE_AUTHENTICATION = "simple";

    public static final String USER_SUB_CONTEXT = "ou=Users";

    public static final String GROUP_SUB_CONTEXT = "ou=Groups";

    public static final String SERVER_PRINCIPLE = "Service";

    public static final String KDC_SERVER_COMMON_NAME = "KDC Service";

    public static final String KDC_SERVER_UID = "krbtgt";

    public static final String LDAP_SERVER_COMMON_NAME = "LDAP Service";

    public static final String LDAP_SERVER_UID = "ldap";

    public static final String LDAP_INITIAL_CONTEXT_FACTORY =
            "org.apache.directory.server.core.jndi.CoreContextFactory";

    //=======================KDC Configurations================================//

    public static final String DEFAULT_KDC_NAME = "DefaultKrbServer";

    public static final String DEFAULT_SYS_ADMIN_PASSWORD = "secret";

    public static final String DEFAULT_KDC_HOST_ADDRESS = "localhost";

    public static final int DEFAULT_PORT_VALUE = -1;

    public static final int DEFAULT_NUMBER_OF_THREADS = 3;

    public static final int DEFAULT_BACK_LOG_COUNT = 50;

    public static final long DEFAULT_TICKET_LIFETIME = 86400000;

    public static final long DEFAULT_RENEWABLE_LIFE_TIME = 604800000;

    public static final String ADMIN_PASSWORD_ALGORITHM = "SHA";

    private ConfigurationConstants() {
    }
}
