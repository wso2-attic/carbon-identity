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
package org.wso2.carbon.identity.provisioning.salesforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningConstants;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;

import edu.emory.mathcs.backport.java.util.Arrays;

public class SalesforceProvisioningConnectorConfig {
	
	private static final Log log = LogFactory.getLog(SalesforceProvisioningConnectorConfig.class);
	private Properties configs;
	
	public SalesforceProvisioningConnectorConfig(Properties configs) {
		this.configs = configs;
	}

	public static final String SALESFORCE_LIST_USER_SIMPLE_QUERY = "SELECT Id, Alias, Email, LastName, Name, ProfileId, Username from User";
	public static final String SALESFORCE_LIST_USER_FULL_QUERY = "SELECT Id, Username, Name, Alias, Email, EmailEncodingKey, LanguageLocaleKey, LastName, LocaleSidKey, ProfileId, TimeZoneSidKey, UserPermissionsCallCenterAutoLogin, UserPermissionsMarketingUser, UserPermissionsOfflineUser from User";
	public static final String SALESFORCE_SERVICES_DATA = "/services/data/";
	public static final String SALESFORCE_ENDPOINT_QUERY = "/query";
	
	public static final String IDENTITY_PROVISIONING_CONNECTOR = "Identity.Provisioning.Connector.Salesforce.Domain.Name";
	
	List<String> getRequiredAttributeNames() {
		List<String> requiredAttributeList = new ArrayList<String>();
		String requiredAttributes = this.configs.getProperty(SalesforceConnectorConstants.PropertyConfig.REQUIRED_FIELDS);
		if (requiredAttributes != null && !requiredAttributes.isEmpty()) {
			requiredAttributeList = Arrays.asList(requiredAttributes.split(IdentityProvisioningConstants.PropertyConfig.DELIMATOR));
		}
		return requiredAttributeList;
	}
	
	String getUserIdClaim() throws IdentityProvisioningException {
		String userIDClaim = this.configs.getProperty(SalesforceConnectorConstants.PropertyConfig.USER_ID_CLAIM);
		if (userIDClaim == null || userIDClaim.isEmpty()) {
			log.error("Required claim for user id is not defined in config");
			throw new IdentityProvisioningException("Required claim for user id is not defined in config");
		}
		if(log.isDebugEnabled()) {
			log.debug("Mapped claim for UserId is : " + userIDClaim);
		}
		return userIDClaim;
	}

	public String getValue(String key) {
		return this.configs.getProperty(key);
	}
}
