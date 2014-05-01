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
package org.wso2.carbon.identity.provisioning;

import java.util.Properties;

public interface IdentityProvisioningConnectorFactory {

	/**
	 * Connector instance builder
	 * @param connectorName
	 * @param isEnabled
	 * @param configs
	 * @return An instance of Identity Provisioning Connector
	 */
	public AbstractIdentityProvisioningConnector buildConnector(String connectorName, boolean isEnabled, Properties configs);
	
	/**
	 * Get a previously built connector instance of given name
	 * @param connectorName
	 * @return An instance of Identity Provisioning Connector if already created connector exists 
	 * or returns null there airn't already created connector of that name
	 */
	public AbstractIdentityProvisioningConnector getConnector(String connectorName);
	
	/**
	 * Show what type if connectors this factory builds
	 * @return class type of the connector built by this factory
	 */
	public String getConnectorType();
}
