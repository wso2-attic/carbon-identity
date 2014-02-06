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
package org.wso2.carbon.identity.thrift.authentication.client;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.wso2.carbon.identity.thrift.authentication.client.internal.pool.SecureClientPool;
import org.wso2.carbon.identity.thrift.authentication.client.internal.pool.SecureClientPoolFactory;
import org.wso2.carbon.identity.thrift.authentication.client.internal.util.ThriftAuthenticationClientConstants;

/**
 * ThriftAuthenticator factory to create ThriftAuthenticator for both OSGi and Standalone services
 */
public class ThriftAuthenticationClientFactory {


    public static ThriftAuthenticationClient constructAgentAuthenticator(String trustStore, String trustStorePassword,
                                                                  int maxActive,
                                                                  long maxWait,
                                                                  int maxIdle,
                                                                  boolean testOnBorrow,
                                                                  boolean testOnReturn,
                                                                  long timeBetweenEvictionRunsMillis,
                                                                  int numTestsPerEvictionRun,
                                                                  long minEvictableIdleTimeMillis) {
        GenericKeyedObjectPool secureTransportPool = new SecureClientPool().getClientPool(
                new SecureClientPoolFactory(trustStorePassword, trustStore), maxActive, maxWait, maxIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis);
        return new ThriftAuthenticationClient(secureTransportPool);
    }

    public static ThriftAuthenticationClient constructAgentAuthenticator() {

        GenericKeyedObjectPool secureTransportPool = new SecureClientPool().getClientPool(
                new SecureClientPoolFactory(null, null), ThriftAuthenticationClientConstants.DEFAULT_MAX_TRANSPORT_POOL_SIZE, ThriftAuthenticationClientConstants.DEFAULT_MAX_WAIT_TIME, ThriftAuthenticationClientConstants.DEFAULT_MAX_IDLE_CONNECTIONS, true, true, ThriftAuthenticationClientConstants.DEFAULT_EVICTION_IDLE_TIME_IN_POOL, ThriftAuthenticationClientConstants.DEFAULT_TEST_PER_EVICTION_RUN, ThriftAuthenticationClientConstants.DEFAULT_MIN_EVICTION_TIME_IN_POOL);
        return new ThriftAuthenticationClient(secureTransportPool);
    }
}
