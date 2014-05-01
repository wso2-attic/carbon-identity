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

public class SPMLConnectorConstants {
	
	public static final String SPML_PROVIDER_ENDPOINT = "http://10.100.5.57:8080/idm/servlet/openspml2";
	public static final String SPML_SERVICE_OBJECT_CLASS = "spml2Person";
	public static final String SPML_SERVICE_USERNAME = "configurator";
	public static final String SPML_SERVICE_PASSWORD = "configurator";
	
    public class PropertyConfig {
   
    	public static final String REQUIRED_FIELDS = "Identity.Provisioning.Connector.SPML.Required.Fields";
    	public static final String REQUIRED_CLAIM_PREFIX = "Identity.Provisioning.Connector.SPML.Required.Field.Claim.";
    	public static final String REQUIRED_DEFAULT_PREFIX = "Identity.Provisioning.Connector.SPML.Required.Field.Default.";
	
    }

}