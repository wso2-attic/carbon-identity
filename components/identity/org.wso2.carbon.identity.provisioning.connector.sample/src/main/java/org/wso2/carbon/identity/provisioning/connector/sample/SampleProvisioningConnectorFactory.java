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
package org.wso2.carbon.identity.provisioning.connector.sample;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.provisioning.AbstractOutboundProvisioningConnector;
import org.wso2.carbon.identity.provisioning.AbstractProvisioningConnectorFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;

/**
 * 
 *
 */
public class SampleProvisioningConnectorFactory extends AbstractProvisioningConnectorFactory {

    private static final Log log = LogFactory.getLog(SampleProvisioningConnectorFactory.class);
    private static final String SAMPLE = "sample";

    @Override
    /**
     * 
     */
    protected AbstractOutboundProvisioningConnector buildConnector(
            Property[] provisioningProperties) throws IdentityProvisioningException {
        SampleProvisioningConnector salesforceConnector = new SampleProvisioningConnector();
        salesforceConnector.init(provisioningProperties);

        if (log.isDebugEnabled()) {
            log.debug("Salesforce provisioning connector created successfully.");
        }

        return salesforceConnector;
    }

    @Override
    /**
     * 
     */
    public String getConnectorType() {
        return SAMPLE;
    }

	@Override
	public List<Property> getConfigurationProperties() {

		List<Property> configProperties = new ArrayList<Property>();

		Property serverUrl = new Property();
		serverUrl.setDisplayName("Server Url");
		serverUrl.setName("server-url");
		serverUrl
				.setDescription("Enter value corresponding to the authetication server.");
		//configProperties.
		configProperties.add(serverUrl);
		return configProperties;

	}

}
