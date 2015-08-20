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
package org.wso2.carbon.apacheds;

import org.wso2.carbon.apacheds.impl.ApacheKDCServer;
import org.wso2.carbon.apacheds.impl.ApacheLDAPServer;

/**
 * A factory class to instantiate LDAPServer and KDCServer instances.
 */
public class DirectoryServiceFactory {

    /**
     * Creates a LDAP server instance.
     *
     * @param serverType Type of the LDAP server. Currently we only have apacheds implementation.
     * @return An instance of LDAPServer
     */
    public static LDAPServer createLDAPServer(LDAPServerType serverType) {
        if (serverType == LDAPServerType.APACHE_DIRECTORY_SERVICE) {
            return new ApacheLDAPServer();
        } else {
            throw new IllegalArgumentException("Does not support LDAP server type " +
                    serverType.name());
        }
    }

    /**
     * Creates a KDC server instance.
     *
     * @param serverType Type of the LDAP server. Currently we only have apacheds based
     *                   implementation.
     * @return An instance of KDCServer
     */
    public static KDCServer createKDCServer(LDAPServerType serverType) {
        if (serverType == LDAPServerType.APACHE_DIRECTORY_SERVICE) {
            return new ApacheKDCServer();
        } else {
            throw new IllegalArgumentException("Does not support LDAP server type " +
                    serverType.name());
        }
    }

    public enum LDAPServerType {
        APACHE_DIRECTORY_SERVICE
    }

}
