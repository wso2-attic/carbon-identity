/*
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.mgt.internal;

import java.util.ArrayList;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class IdentityMgtDeploymentInterceptor implements AxisObserver {

    private static final Log log = LogFactory.getLog(IdentityMgtDeploymentInterceptor.class);    

    @Override
    public void init(AxisConfiguration axisConfiguration) {

    }

    @Override
    public void serviceUpdate(AxisEvent axisEvent, AxisService axisService) {
          // TODO
//        int eventType = axisEvent.getEventType();
//        if (eventType == AxisEvent.SERVICE_DEPLOY &&
//                                    "UserIdentityManagementService".equals(axisService.getName())) {
//            try {
//                String serverURL = CarbonUtils.getServerConfiguration().getFirstProperty("ServerURL");
//                if(!serverURL.startsWith("local")) {
//                    axisService.applyPolicy(Utils.getSecurityPolicy());
//                    axisService.addModuleref("rampart");
//                }
//            } catch (AxisFault axisFault) {
//                log.error("Security can not be applied for UserIdentityManagementService", axisFault);
//            }
//        }
    }

    @Override
    public void serviceGroupUpdate(AxisEvent axisEvent, AxisServiceGroup axisServiceGroup) {

    }

    @Override
    public void moduleUpdate(AxisEvent axisEvent, AxisModule axisModule) {

    }

    @Override
    public void addParameter(Parameter parameter) throws AxisFault {

    }

    @Override
    public void removeParameter(Parameter parameter) throws AxisFault {

    }

    @Override
    public void deserializeParameters(OMElement omElement) throws AxisFault {

    }

    @Override
    public Parameter getParameter(String s) {
        return null;
    }

    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList();
    }

    @Override
    public boolean isParameterLocked(String s) {
        return false;
    }
}
