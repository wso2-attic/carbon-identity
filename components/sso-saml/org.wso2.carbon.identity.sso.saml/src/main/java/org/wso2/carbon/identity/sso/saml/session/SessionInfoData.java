/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.session;

import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionInfoData implements Serializable {

    private static final long serialVersionUID = -7219077849244588172L;

    private Map<String, String> rpSessionList = new ConcurrentHashMap<>();
    private Map<String, SAMLSSOServiceProviderDO> serviceProviderList = new ConcurrentHashMap<>();
    private Map<String, String> issuerSubjectMap = new ConcurrentHashMap<>();

    public Map<String, SAMLSSOServiceProviderDO> getServiceProviderList() {
        return serviceProviderList;
    }

    public void addServiceProvider(String issuer, SAMLSSOServiceProviderDO spDO, String rpSessionId) {
        this.serviceProviderList.put(issuer, spDO);
        if (rpSessionId != null) {
            this.rpSessionList.put(issuer, rpSessionId);
        }
    }

    public void removeServiceProvider(String issuer) {
        serviceProviderList.remove(issuer);
        rpSessionList.remove(issuer);
    }

    public Map<String, String> getRPSessionsList() {
        return rpSessionList;
    }

    public String getSubject(String issuer) {
        return issuerSubjectMap.get(issuer);
    }

    public void setSubject(String issuer, String subject) {
        issuerSubjectMap.put(issuer, subject);
    }
}

