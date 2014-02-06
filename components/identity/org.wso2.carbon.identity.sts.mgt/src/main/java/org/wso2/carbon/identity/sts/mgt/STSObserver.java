/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sts.mgt;

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
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.sts.mgt.admin.STSConfigAdmin;
import org.wso2.carbon.utils.ServerConstants;

public class STSObserver implements AxisObserver {

    private final static Log log = LogFactory.getLog(STSObserver.class);

    public void init(AxisConfiguration arg0) {
        // TODO Auto-generated method stub

    }

    public void moduleUpdate(AxisEvent arg0, AxisModule arg1) {
        // TODO Auto-generated method stub

    }

    public void serviceGroupUpdate(AxisEvent arg0, AxisServiceGroup arg1) {
        // TODO Auto-generated method stub

    }

    public void serviceUpdate(AxisEvent axisEvent, AxisService service) {
        int eventType = axisEvent.getEventType();
        if (eventType == AxisEvent.SERVICE_DEPLOY) {
            try {
                STSConfigAdmin.configureService(service.getName());
                if (ServerConstants.STS_NAME.equals(service.getName())) {
                    STSConfigAdmin.configureGenericSTS();
                }
            } catch (IdentityProviderException e) {
                log.error(e);
            }
        }

    }
    public void addParameter(Parameter arg0) throws AxisFault {
        // TODO Auto-generated method stub

    }

    public void deserializeParameters(OMElement arg0) throws AxisFault {
        // TODO Auto-generated method stub

    }

    public Parameter getParameter(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public ArrayList getParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isParameterLocked(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeParameter(Parameter arg0) throws AxisFault {
        // TODO Auto-generated method stub

    }

}
