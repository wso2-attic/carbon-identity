/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.authenticator.iwa.util;

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
import org.apache.neethi.Policy;
import org.wso2.carbon.identity.base.IdentityBaseUtil;

import java.util.ArrayList;

public class IWADeploymentInterceptor implements AxisObserver {

    private static final String IWA_SERVICE_NAME = "IWAAuthenticator";

    private static final Log log = LogFactory.getLog(IWADeploymentInterceptor.class);

    /**
     * Updates RelyingPartyService with Crypto information
     *
     * @param config AxisConfiguration
     * @throws Exception
     */
    public static void populateRampartConfig(AxisConfiguration config) throws Exception {

        AxisService service;

        // Get the RelyingParty Service to update security policy with keystore information
        service = config.getService(IWA_SERVICE_NAME);
        if (service == null) {
            String msg = IWA_SERVICE_NAME + " is not available in the Configuration Context";
            log.error(msg);
        }

        // Create a Rampart Config with default crypto information
        //Policy rampartConfig = IdentityBaseUtil.getDefaultRampartConfig();
        Policy rampartConfig = IdentityBaseUtil.getDefaultRampartConfig();
        // Add the RampartConfig to service policy
        service.getPolicySubject().attachPolicy(rampartConfig);

    }

    /**
     * {@inheritDoc}
     */
    public void serviceUpdate(AxisEvent event, AxisService service) {
        if (event.getEventType() == AxisEvent.SERVICE_DEPLOY
                && IWA_SERVICE_NAME.equals(service.getName())) {
            try {
                populateRampartConfig(service.getAxisConfiguration());
            } catch (Exception e) {
                log.error("Error while updating " + IWA_SERVICE_NAME
                        + " in IWADeploymentInterceptor", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init(AxisConfiguration arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void moduleUpdate(AxisEvent arg0, AxisModule arg1) {
    }

    /**
     * {@inheritDoc}
     */
    public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup group) {
    }

    /**
     * {@inheritDoc}
     */
    public void addParameter(Parameter arg0) throws AxisFault {
    }

    /**
     * {@inheritDoc}
     */
    public void deserializeParameters(OMElement arg0) throws AxisFault {
    }

    /**
     * {@inheritDoc}
     */
    public Parameter getParameter(String arg0) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList getParameters() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isParameterLocked(String arg0) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void removeParameter(Parameter arg0) throws AxisFault {

    }


}
