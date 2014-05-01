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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provisioning.internal.IdentityProvisionServiceComponent;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.CarbonUtils;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * encapsulates recovery configuration data
 */
public class IdentityProvisioningConfig {

	private static final Log log = LogFactory
			.getLog(IdentityProvisioningConfig.class);

	private static IdentityProvisioningConfig identityProvisionConfig;
	
	private Properties configProperties;
	private boolean isProvisionEnable;

	public IdentityProvisioningConfig() throws IdentityProvisioningException{

		Properties properties = new Properties();
		InputStream inStream = null;

		File provisioningConfig = new File(
				CarbonUtils.getCarbonSecurityConfigDirPath(),
				IdentityProvisioningConstants.PropertyConfig.CONFIG_FILE_NAME);

		if (provisioningConfig.exists()) {
			try {
				inStream = new FileInputStream(provisioningConfig);
				properties.load(inStream);
				this.configProperties = properties;
			} catch (FileNotFoundException e) {
				log.error(
						"Can not find "
								+ IdentityProvisioningConstants.PropertyConfig.CONFIG_FILE_NAME
								+ " file from "
								+ provisioningConfig.getAbsolutePath(), e);
			} catch (IOException e) {
				log.error(
						"Can not load "
								+ IdentityProvisioningConstants.PropertyConfig.CONFIG_FILE_NAME
								+ " file from "
								+ provisioningConfig.getAbsolutePath(), e);
			} finally {
				if (inStream != null) {
					try {
						inStream.close();
					} catch (IOException e) {
						log.error("Error while closing stream ", e);
					}
				}
			}
		}
		
		try {
			String provisionEnable = this.configProperties
					.getProperty(IdentityProvisioningConstants.PropertyConfig.IDENTITY_PROVISION_ENABLE);
			if (provisionEnable != null) {
				this.isProvisionEnable = Boolean.parseBoolean(provisionEnable
						.trim());
			}

		} catch (Exception e) {
			log.error("Error while loading identity mgt configurations", e);
		}
		
		// Persisting config to registry
		persistConfigFileToRegistry();

	}
	
	private void persistConfigFileToRegistry() throws IdentityProvisioningException {
		try {
			Registry registry = IdentityProvisionServiceComponent.getRegistryService().getConfigSystemRegistry();

			String connectorBaseConfigPath = IdentityProvisioningConstants.IDENTITY_PROVISIONING_REG_PATH;
	        if(!registry.resourceExists(connectorBaseConfigPath)){
	            Collection identityProvisioningConfigCollection = registry.newCollection();
	            registry.put(connectorBaseConfigPath, identityProvisioningConfigCollection);
	        }

			String baseConfigFile = connectorBaseConfigPath + RegistryConstants.PATH_SEPARATOR + "BaseConfiguration";
	        if(!registry.resourceExists(baseConfigFile)) {
	        	Resource baseConfiguration = registry.newResource();

	    		Set<Object> keySet =  this.configProperties.keySet();
	    		Iterator<Object> iterator = keySet.iterator();
	    		while (iterator.hasNext()) {
	    			String key = (String)iterator.next();
	    			if(key != null && !key.isEmpty()) {
	    				baseConfiguration.addProperty(key, this.getValue(key));
	    			}
	    			if(log.isDebugEnabled()) {
	    				log.debug("Adding property : " + key + ", value :" + this.getValue(key) + " to  " + baseConfigFile);
	    			}
	    		}
	            registry.put(baseConfigFile, baseConfiguration);
	        }
	        
	        // Store connector specifc configs
	        // Get initial connector list, register connector type, store connector config
	        
			List<String> registeredConnectors = this.getRegistoredConnectors();

			Iterator<String> iterator = registeredConnectors.iterator();
			while (iterator.hasNext()) {
				String connectorName = iterator.next();
				boolean connectorIsEnabled = this.isConnectorEnabled(connectorName);
				
				String className = this.getConnectorClassName(connectorName);
				String connectorClassPath = connectorBaseConfigPath + RegistryConstants.PATH_SEPARATOR + className;
		        if(!registry.resourceExists(connectorClassPath)){
		            registry.put(connectorClassPath, registry.newCollection());
		        }
		        
				String connectorConfigFile = connectorClassPath + RegistryConstants.PATH_SEPARATOR + connectorName;
		        if(!registry.resourceExists(connectorConfigFile)) {
		        	Resource connectorConfiguration = registry.newResource();

		        	//
		        	String isEnableKey = IdentityProvisioningConstants.PropertyConfig.PREFIX_IDENTITY_PROVISIONING_CONNECTOR_ENABLE+connectorName;
	    			connectorConfiguration.addProperty(isEnableKey, this.getValue(isEnableKey));

//					Properties connectorProperties = this.configProperties;
					Properties connectorProperties = this.getConnectorProperties(connectorName);
					Set<Object> keySet =  connectorProperties.keySet();
					Iterator<Object> iterator2 = keySet.iterator();
		    		while (iterator2.hasNext()) {
		    			String key = (String)iterator2.next();

//						if(key != null && !key.isEmpty() && key.startsWith(IdentityProvisionConstants.PropertyConfig.PREFIX_IDENTITY_PROVISIONING_CONNECTOR+connectorName)) {
		    			if(key != null && !key.isEmpty()) {
		    				connectorConfiguration.addProperty(key, this.getValue(key));
		    			}
		    			if(log.isDebugEnabled()) {
		    				log.debug("Adding property : " + key + ", value :" + this.getValue(key) + " to  " + connectorConfigFile);
		    			}
		    		}
		            registry.put(connectorConfigFile, connectorConfiguration);
		        }
			}
		} catch (RegistryException e) {
			throw new IdentityProvisioningException("Error while persisting configurations to registry", e);
		}
	}
	
	
	public Properties getConnectorProperties(String type, String connectorName) {
//		Registry registry = IdentityProvisionServiceComponent.getRegistryService().getConfigSystemRegistry();
//
//		String connectorConfigPath = IdentityProvisionConstants.IDENTITY_PROVISION_PATH
//				+ RegistryConstants.PATH_SEPARATOR
//				+ type
//				+ RegistryConstants.PATH_SEPARATOR
//				+ connectorName;
//		
//        Resource configResource = registry.get(connectorConfigPath);
//        
//        if(configResource != null) {
//        	return configResource.getProperties();
//        }
//        else {
//        	throw new IdentityProvisionException("Cannot find configurations for connector : " + connectorName + " of the type :" + type);
//        }
				
//		try {
//			String provisionEnable = this.configProperties
//					.getProperty(IdentityProvisionConstants.PropertyConfig.IDENTITY_PROVISION_ENABLE);
//			if (provisionEnable != null) {
//				this.isProvisionEnable = Boolean.parseBoolean(provisionEnable
//						.trim());
//			}
//		} catch (Exception e) {
//			log.error("Error while loading identity mgt configurations", e);
//		}
		return null;
		
	}
	
	public static IdentityProvisioningConfig getInstance() throws IdentityProvisioningException {
		if (identityProvisionConfig == null) {
			identityProvisionConfig = new IdentityProvisioningConfig();
		}
		return identityProvisionConfig;
	}

	public boolean isProvisioningEnable() {
		return isProvisionEnable;
	}

	public String getValue(String key) {
		return this.configProperties.getProperty(key);
	}

	public List<String> getRegistoredConnectors() {
		// This list doesn't get stored as we can dynamically add configs of second connector
		List<String> connectorNameList = new ArrayList<String>();
		String registoredConnectors = this.configProperties.getProperty(IdentityProvisioningConstants.PropertyConfig.IDENTITY_PROVISIONING_REGISTORED_CONNECTORS);
		if (registoredConnectors != null && !registoredConnectors.isEmpty()) {
			connectorNameList = Arrays.asList(registoredConnectors.split(IdentityProvisioningConstants.PropertyConfig.DELIMATOR));
		}
		return connectorNameList;
	}

	public Properties getConnectorProperties(String connectorName) {
		Properties connectorProperties = new Properties();

		Set<Object> keySet =  this.configProperties.keySet();
		Iterator<Object> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String key = (String)iterator.next();
			if(key != null && !key.isEmpty() && key.startsWith(IdentityProvisioningConstants.PropertyConfig.PREFIX_IDENTITY_PROVISIONING_CONNECTOR+connectorName)) {
				connectorProperties.put(key, this.getValue(key));
			}
			if(log.isDebugEnabled()) {
				log.debug("Adding property : " + key + ", value :" + this.getValue(key) + " for connector " + connectorName);
			}
		}
		return connectorProperties;
	}

	public String getConnectorClassName(String connectorName) throws IdentityProvisioningException{
		String connectorClassName = "";

		String className = this.getValue(IdentityProvisioningConstants.PropertyConfig.PREFIX_IDENTITY_PROVISIONING_CONNECTOR_CLASS+connectorName);
			
		if (className != null && !className.isEmpty()) {
			connectorClassName = className;
		}
		else {
			throw new IdentityProvisioningException("Class name not defined for the connector : " + connectorName);
		}

		if(log.isDebugEnabled()) {
			log.debug("Class name of the connector : " + connectorName + " is : " + connectorClassName);
		}
		return connectorClassName;
	}

	public boolean isConnectorEnabled(String connectorName) throws IdentityProvisioningException{
		boolean isConnectorEnable = false;

		try {
			String isEnable = this.getValue(IdentityProvisioningConstants.PropertyConfig.PREFIX_IDENTITY_PROVISIONING_CONNECTOR_ENABLE+connectorName);
			
			if (isEnable != null) {
				isConnectorEnable = Boolean.parseBoolean(isEnable.trim());
			}

		} catch (Exception e) {
			throw new IdentityProvisioningException("Error while reading isEnable property of the connector : " + connectorName, e);
		}
		return isConnectorEnable;
	}
}
