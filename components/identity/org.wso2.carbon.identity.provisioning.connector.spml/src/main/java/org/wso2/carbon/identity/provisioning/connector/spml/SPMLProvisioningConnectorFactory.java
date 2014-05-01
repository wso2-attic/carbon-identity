/*
 *  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provisioning.connector.spml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConnectorFactory;

public class SPMLProvisioningConnectorFactory implements
		IdentityProvisioningConnectorFactory {

	private static final Log log = LogFactory
			.getLog(SPMLProvisioningConnectorFactory.class);
	private static Map<String, SPMLProvisioningConnector> connectorList = new HashMap<String, SPMLProvisioningConnector>();

	public SPMLProvisioningConnector buildConnector(String connectorName,
			boolean isEnabled, Properties configs) {
		if (!connectorList.containsKey(connectorName)) {
			connectorList.put(connectorName, new SPMLProvisioningConnector(
					connectorName, isEnabled, configs));
			if (log.isDebugEnabled()) {
				log.debug("Created new connector : " + connectorName
						+ " of type : "
						+ SPMLProvisioningConnector.class.toString());
			}
		}
		return connectorList.get(connectorName);
	}

	public SPMLProvisioningConnector getConnector(String connectorName) {
		if (connectorName != null && connectorList.containsKey(connectorName)) {
			return connectorList.get(connectorName);
		}
		return null;
	}

	public String getConnectorType() {
		return SPMLProvisioningConnector.class.getName();
	}

}
