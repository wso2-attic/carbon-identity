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
package org.wso2.carbon.identity.thrift.authentication.client.internal;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.thrift.authentication.client.ThriftAuthenticationClient;
import org.wso2.carbon.identity.thrift.authentication.client.ThriftAuthenticationClientFactory;
import org.wso2.carbon.identity.thrift.authentication.client.internal.util.ThriftAuthenticationClientConfigParser;
import org.wso2.carbon.identity.thrift.authentication.client.internal.util.ThriftAuthenticationClientConstants;

/**
 * @scr.component name="org.wso2.carbon.identity.thrift.authentication.client.internal.ThriftAuthenticationClientComponent" immediate="true"
 */

public class ThriftAuthenticationClientComponent {

    private static Log log = LogFactory.getLog(ThriftAuthenticationClientComponent.class);

    private ServiceRegistration thriftAuthenticationService;

    protected void activate(ComponentContext compCtx) {

        try {


            //configure MaxTransportPoolSize
            int maxTransportPoolSize;
            try {
                OMElement thriftSessionDAOElement = ThriftAuthenticationClientConfigParser.getInstance()
                        .getConfigElement("MaxTransportPoolSize");
                maxTransportPoolSize = Integer.parseInt(thriftSessionDAOElement.getText());
            } catch (Throwable throwable) {
                log.error("Error in loading MaxTransportPoolSize from " + ThriftAuthenticationClientConfigParser.THRIFT_AUTHENTICATION_CLIENT_CONFIG + " hence using default:" + ThriftAuthenticationClientConstants.DEFAULT_MAX_TRANSPORT_POOL_SIZE, throwable);
                maxTransportPoolSize = ThriftAuthenticationClientConstants.DEFAULT_MAX_TRANSPORT_POOL_SIZE;
            }

            //configure MaxWaitTime
            long maxWaitTime;
            try {
                OMElement thriftSessionDAOElement = ThriftAuthenticationClientConfigParser.getInstance()
                        .getConfigElement("MaxWaitTime");
                maxWaitTime = Long.parseLong(thriftSessionDAOElement.getText());
            } catch (Throwable throwable) {
                log.error("Error in loading MaxWaitTime from " + ThriftAuthenticationClientConfigParser.THRIFT_AUTHENTICATION_CLIENT_CONFIG + " hence using default:" + ThriftAuthenticationClientConstants.DEFAULT_MAX_WAIT_TIME, throwable);
                maxWaitTime = ThriftAuthenticationClientConstants.DEFAULT_MAX_WAIT_TIME;
            }


            //configure MaxIdleConnections
            int maxIdleConnections;
            try {
                OMElement thriftSessionDAOElement = ThriftAuthenticationClientConfigParser.getInstance()
                        .getConfigElement("MaxIdleConnections");
                maxIdleConnections = Integer.parseInt(thriftSessionDAOElement.getText());
            } catch (Throwable throwable) {
                log.error("Error in loading MaxWaitTime from " + ThriftAuthenticationClientConfigParser.THRIFT_AUTHENTICATION_CLIENT_CONFIG + " hence using default:" + ThriftAuthenticationClientConstants.DEFAULT_MAX_IDLE_CONNECTIONS, throwable);
                maxIdleConnections = ThriftAuthenticationClientConstants.DEFAULT_MAX_IDLE_CONNECTIONS;
            }

            //configure EvictionIdleTimeInPool
            long evictionIdleTimeInPool;
            try {
                OMElement thriftSessionDAOElement = ThriftAuthenticationClientConfigParser.getInstance()
                        .getConfigElement("EvictionIdleTimeInPool");
                evictionIdleTimeInPool = Long.parseLong(thriftSessionDAOElement.getText());
            } catch (Throwable throwable) {
                log.error("Error in loading EvictionIdleTimeInPool from " + ThriftAuthenticationClientConfigParser.THRIFT_AUTHENTICATION_CLIENT_CONFIG + " hence using default:" + ThriftAuthenticationClientConstants.DEFAULT_EVICTION_IDLE_TIME_IN_POOL, throwable);
                evictionIdleTimeInPool = ThriftAuthenticationClientConstants.DEFAULT_EVICTION_IDLE_TIME_IN_POOL;
            }


            //configure TestPerEvictionRun
            int testPerEvictionRun;
            try {
                OMElement thriftSessionDAOElement = ThriftAuthenticationClientConfigParser.getInstance()
                        .getConfigElement("TestPerEvictionRun");
                testPerEvictionRun = Integer.parseInt(thriftSessionDAOElement.getText());
            } catch (Throwable throwable) {
                log.error("Error in loading TestPerEvictionRun from " + ThriftAuthenticationClientConfigParser.THRIFT_AUTHENTICATION_CLIENT_CONFIG + " hence using default:" + ThriftAuthenticationClientConstants.DEFAULT_TEST_PER_EVICTION_RUN, throwable);
                testPerEvictionRun = ThriftAuthenticationClientConstants.DEFAULT_TEST_PER_EVICTION_RUN;
            }

            //configure MinEvictionTimeInPool
            long minEvictionTimeInPool;
            try {
                OMElement thriftSessionDAOElement = ThriftAuthenticationClientConfigParser.getInstance()
                        .getConfigElement("MinEvictionTimeInPool");
                minEvictionTimeInPool = Long.parseLong(thriftSessionDAOElement.getText());
            } catch (Throwable throwable) {
                log.error("Error in loading MinEvictionTimeInPool from " + ThriftAuthenticationClientConfigParser.THRIFT_AUTHENTICATION_CLIENT_CONFIG + " hence using default:" + ThriftAuthenticationClientConstants.DEFAULT_MIN_EVICTION_TIME_IN_POOL, throwable);
                minEvictionTimeInPool = ThriftAuthenticationClientConstants.DEFAULT_MIN_EVICTION_TIME_IN_POOL;
            }

            ServerConfiguration serverConfig = ServerConfiguration.getInstance();

            String trustStore = serverConfig.getFirstProperty("javax.net.ssl.trustStore");
            if (trustStore == null) {
                trustStore = System.getProperty("javax.net.ssl.trustStore");
                if (trustStore == null) {
                    throw new Exception("Error in loading, javax.net.ssl.trustStore is null ");
                }
            }
            String trustStorePassword = serverConfig.getFirstProperty("javax.net.ssl.trustStorePassword");
            if (trustStorePassword == null) {
                trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
                if (trustStorePassword == null) {
                    throw new Exception("Error in loading, javax.net.ssl.trustStorePassword is null ");
                }
            }

            ThriftAuthenticationClient thriftAuthenticationClient = ThriftAuthenticationClientFactory.constructAgentAuthenticator(trustStore, trustStorePassword, maxTransportPoolSize, maxWaitTime, maxIdleConnections, true, true, evictionIdleTimeInPool, testPerEvictionRun, minEvictionTimeInPool);


            //register as an osgi service
            thriftAuthenticationService = compCtx.getBundleContext().registerService(
                    ThriftAuthenticationClient.class.getName(), thriftAuthenticationClient, null);


        } catch (RuntimeException e) {
            log.error("Error in starting Thrift Authentication Client ", e);
        } catch (Throwable e) {
            log.error("Error in starting Thrift Authentication Client ", e);
        }

    }

    protected void deactivate(ComponentContext compCtx) {
        compCtx.getBundleContext().ungetService(thriftAuthenticationService.getReference());
    }

}
