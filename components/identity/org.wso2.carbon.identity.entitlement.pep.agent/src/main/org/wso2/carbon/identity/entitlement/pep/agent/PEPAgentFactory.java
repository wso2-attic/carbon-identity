/*
 * Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.entitlement.pep.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.entitlement.pep.agent.exception.EntitlementAgentException;
import org.wso2.carbon.identity.entitlement.pep.agent.soap.authenticationAdmin.SOAPEntitlementServiceClient;
import org.wso2.carbon.identity.entitlement.pep.agent.soap.basicAuth.BasicAuthEntitlementServiceClient;
import org.wso2.carbon.identity.entitlement.pep.agent.thrift.ThriftEntitlementServiceClient;
import org.wso2.carbon.identity.entitlement.pep.agent.wsxacml.WSXACMLEntitlementServiceClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PEPAgentFactory {

    private static final Log log = LogFactory.getLog(PEPAgentFactory.class);

    public static Map<String, AbstractEntitlementServiceClient> getAppToPDPClientMap
            (Map<String, Map<String,String>> appToPDPClientConfigMap) throws EntitlementAgentException {

        Map<String, AbstractEntitlementServiceClient> appToPDPClientMap =
                new HashMap<String, AbstractEntitlementServiceClient>();

        Set<String> appList = appToPDPClientConfigMap.keySet();
        Iterator appListItr = appList.iterator();
        while (appListItr.hasNext()) {
            String appId = (String) appListItr.next();
            Map<String,String> appConfig = appToPDPClientConfigMap.get(appId);
            String client = appConfig.get("client");
            if (client == null || client.trim().length() == 0) {
                log.debug("Using BasicAuthEntitlementServiceClient by default");
                client = "basicAuth";
            }
            if (!ProxyConstants.JSON.equals(client)
                    && !ProxyConstants.SOAP.equals(client)
                    && !ProxyConstants.THRIFT.equals(client)
                    && !ProxyConstants.BASIC_AUTH.equals(client) && !ProxyConstants.WS_XACML.equals(client)) {
                throw new EntitlementAgentException("Invalid client. Should be json, soap, thrift or wsXacml");
            }

            if (ProxyConstants.SOAP.equals(client)) {
                if(appConfig.get("serverUrl") == null || appConfig.get("serverUrl").length() == 0){
                    throw new EntitlementAgentException("serverUrl cannot be null or empty");
                }
                String serverUrl = appConfig.get("serverUrl").trim();
                if (!serverUrl.endsWith("/")) {
                    serverUrl += "/";
                }
                if(appConfig.get("userName") == null || appConfig.get("userName").length() == 0){
                    throw new EntitlementAgentException("userName cannot be null or empty");
                }
                if(appConfig.get("password") == null || appConfig.get("password").length() == 0){
                    throw new EntitlementAgentException("password cannot be null or empty");
                }
                boolean reuseSession = true;
                if(appConfig.get("reuseSession") != null){
                    reuseSession = Boolean.parseBoolean(appConfig.get("reuseSession"));
                }
                appToPDPClientMap.put(appId, new SOAPEntitlementServiceClient(serverUrl,appConfig.get("userName"),appConfig.get("password"),reuseSession));
            } else if(ProxyConstants.BASIC_AUTH.equals(client)){
                if(appConfig.get("serverUrl") == null || appConfig.get("serverUrl").length() == 0){
                    throw new EntitlementAgentException("serverUrl cannot be null or empty");
                }
                String serverUrl = appConfig.get("serverUrl").trim();
                if (!serverUrl.endsWith("/")) {
                    serverUrl += "/";
                }
                if(appConfig.get("userName") == null || appConfig.get("userName").length() == 0){
                    throw new EntitlementAgentException("userName cannot be null or empty");
                }
                if(appConfig.get("password") == null || appConfig.get("password").length() == 0){
                    throw new EntitlementAgentException("password cannot be null or empty");
                }
                appToPDPClientMap.put(appId, new BasicAuthEntitlementServiceClient(serverUrl,appConfig.get("userName"),appConfig.get("password")));
            } else if (ProxyConstants.THRIFT.equals(client)) {
                if(appConfig.get("serverUrl") == null || appConfig.get("serverUrl").length() == 0){
                    throw new EntitlementAgentException("serverUrl cannot be null or empty");
                }
                String serverUrl = appConfig.get("serverUrl").trim();
                if (!serverUrl.endsWith("/")) {
                    serverUrl += "/";
                }
                if(appConfig.get("userName") == null || appConfig.get("userName").length() == 0){
                    throw new EntitlementAgentException("userName cannot be null or empty");
                }
                if(appConfig.get("password") == null || appConfig.get("password").length() == 0){
                    throw new EntitlementAgentException("password cannot be null or empty");
                }
                if(appConfig.get("thriftHost") == null || appConfig.get("thriftHost").length() == 0){
                    throw new EntitlementAgentException("thriftHost cannot be null or empty");
                }
                int thriftPort;
                if(appConfig.get("thriftPort") == null || appConfig.get("thriftPort").length()==0){
                    thriftPort = ProxyConstants.DEFAULT_THRIFT_PORT;
                }else{
                    thriftPort = Integer.parseInt(appConfig.get("thriftPort"));
                }
                boolean reuseSession = true;
                if(appConfig.get("reuseSession") != null){
                    reuseSession = Boolean.parseBoolean(appConfig.get("reuseSession"));
                }
                appToPDPClientMap.put(appId, new ThriftEntitlementServiceClient(serverUrl,appConfig.get("userName"),appConfig.get("password"),appConfig.get("thriftHost"),thriftPort,reuseSession));
            }else if (ProxyConstants.WS_XACML.equals(client)) {
                if(appConfig.get("serverUrl") == null || appConfig.get("serverUrl").length() == 0){
                    throw new EntitlementAgentException("serverUrl cannot be null or empty");
                }
                String serverUrl = appConfig.get("serverUrl").trim();
                if (!serverUrl.endsWith("/")) {
                    serverUrl += "/";
                }
                if(appConfig.get("userName") == null || appConfig.get("userName").length() == 0){
                    throw new EntitlementAgentException("userName cannot be null or empty");
                }
                if(appConfig.get("password") == null || appConfig.get("password").length() == 0){
                    throw new EntitlementAgentException("password cannot be null or empty");
                }
                appToPDPClientMap.put(appId, new WSXACMLEntitlementServiceClient(serverUrl,appConfig.get("userName"),appConfig.get("password")));
            } else if (ProxyConstants.JSON.equals(client)) {

            }
        }
        return appToPDPClientMap;
    }

}
