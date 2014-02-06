/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.agent.saml;

import org.wso2.carbon.identity.sso.agent.bean.SSOAgentSessionBean;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConstants;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SSOAgentHttpSessionListener implements HttpSessionListener{

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {

        // No need to invalidate session here, as it is going to be invalidated soon

        SSOAgentSessionBean sessionBean = (SSOAgentSessionBean)httpSessionEvent.getSession()
                .getAttribute(SSOAgentConfigs.getSessionBeanName());

        if(sessionBean != null && sessionBean.getSAMLSSOSessionBean() != null &&
                sessionBean.getSAMLSSOSessionBean().getIdPSessionIndex() != null){

            SSOAgentSessionManager.getSsoSessions().remove(sessionBean.getSAMLSSOSessionBean().getIdPSessionIndex());
        }

    }
}
