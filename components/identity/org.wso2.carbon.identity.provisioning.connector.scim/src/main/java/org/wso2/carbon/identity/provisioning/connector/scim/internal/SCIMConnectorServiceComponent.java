package org.wso2.carbon.identity.provisioning.connector.scim.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.connector.scim.SCIMProvisioningConnectorFactory;

/**
 * @scr.component name=
 *                "org.wso2.carbon.identity.provisioning.connector.scim.internal.SCIMConnectorServiceComponent"
 *                immediate="true"
 */
public class SCIMConnectorServiceComponent {
	private static Log log = LogFactory.getLog(SCIMConnectorServiceComponent.class);

	protected void activate(ComponentContext context) {

		if (log.isDebugEnabled()) {
			log.debug("Activating SCIMConnectorServiceComponent");
		}
		
    	try {
    		SCIMProvisioningConnectorFactory scimProvisioningConnectorFactory = new SCIMProvisioningConnectorFactory();    	
    		context.getBundleContext().registerService(AbstractProvisioningConnectorFactory.class.getName(), scimProvisioningConnectorFactory, null);
    		if(log.isDebugEnabled()) {
    			log.debug("SCIM Provisioning Connector bundle is activated");
    		}
    	} catch (Throwable e) {
    		log.fatal(" Error while activating SCIM Provisioning Connector ", e);
    	}
	}
}
