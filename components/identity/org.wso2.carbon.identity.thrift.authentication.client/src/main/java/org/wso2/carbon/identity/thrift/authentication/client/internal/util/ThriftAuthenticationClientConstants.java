/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.thrift.authentication.client.internal.util;

/**
 * This class holds the constants related to thrift based authentication client
 */
public class ThriftAuthenticationClientConstants {

    public static final String HOSTNAME_AND_PORT_SEPARATOR = ":";
    public static final String SEPARATOR = ",";

    public static final int DEFAULT_MAX_TRANSPORT_POOL_SIZE = 250;

    public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 250;

    public static final long DEFAULT_EVICTION_IDLE_TIME_IN_POOL = 5500;

    // 2 min
    public static final long DEFAULT_MAX_WAIT_TIME = 60000 * 2;

    public static final long DEFAULT_MIN_EVICTION_TIME_IN_POOL = 5000;

    public static final int DEFAULT_TEST_PER_EVICTION_RUN = 10;


}
