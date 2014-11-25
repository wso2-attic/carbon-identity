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
