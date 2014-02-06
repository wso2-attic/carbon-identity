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
package org.wso2.carbon.identity.thrift.authentication.client.internal.pool;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * Client connection pool for thrift authentication based on the server url.
 */
public class SecureClientPool {

    private static volatile GenericKeyedObjectPool socketPool = null;

    public GenericKeyedObjectPool getClientPool(KeyedPoolableObjectFactory factory,
                                                int maxActive,
                                                long maxWait,
                                                int maxIdle,
                                                boolean testOnBorrow,
                                                boolean testOnReturn,
                                                long timeBetweenEvictionRunsMillis,
                                                int numTestsPerEvictionRun,
                                                long minEvictableIdleTimeMillis) {
        if (socketPool == null) {
            synchronized (SecureClientPool.class) {
                if (socketPool == null) {
                    socketPool = new GenericKeyedObjectPoolImpl(factory, maxActive, GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, maxWait, maxIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, false);
                }
            }
        }
        return socketPool;
    }

    static class GenericKeyedObjectPoolImpl extends GenericKeyedObjectPool {

        GenericKeyedObjectPoolImpl(KeyedPoolableObjectFactory factory, int maxActive, byte whenExhaustedAction, long maxWait, int maxIdle, boolean testOnBorrow, boolean testOnReturn, long timeBetweenEvictionRunsMillis, int numTestsPerEvictionRun, long minEvictableIdleTimeMillis, boolean testWhileIdle) {
            super(factory, maxActive, whenExhaustedAction, maxWait, maxIdle, testOnBorrow, testOnReturn, timeBetweenEvictionRunsMillis, numTestsPerEvictionRun, minEvictableIdleTimeMillis, testWhileIdle);
        }

        @Override
        public void close() throws Exception {
            super.close();
        }


    }

}
