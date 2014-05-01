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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.provisioning.internal.IdentityProvisionServiceComponent;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractUserOperationEventListener;

public class IdentityProvisioningEventListener extends AbstractUserOperationEventListener {

	private static final Log log = LogFactory.getLog(IdentityProvisioningEventListener.class);
//	private IdentityProvisioningConfig config;
//	private List<AbstractIdentityProvisioningConnector> connectors;
	private static final int maxConnectors = 100;
	
	public IdentityProvisioningEventListener() throws IdentityProvisioningException {
//		this.config = IdentityProvisioningConfig.getInstance();
//		this.connectors = new ArrayList<AbstractIdentityProvisioningConnector>();
//		
//		List<String> registeredConnectors = this.config.getRegistoredConnectors();
//
//		Iterator<String> iterator = registeredConnectors.iterator();
//		while (iterator.hasNext()) {
//			String connectorName = iterator.next();
//			boolean connectorIsEnabled = config.isConnectorEnabled(connectorName);
//			if (connectorIsEnabled) {
//				Properties connectorProperties = config.getConnectorProperties(connectorName);
//				String className = config.getConnectorClassName(connectorName);
//				try {
//						
//						Class<?> clazz = Class.forName(className);
//						Constructor<?> ctor = clazz.getConstructor(String.class, boolean.class, Properties.class);
//						AbstractIdentityProvisioningConnector connector = (AbstractIdentityProvisioningConnector)ctor.newInstance(new Object[] { connectorName, connectorIsEnabled, connectorProperties});
//						
//						connectors.add(connector);
//						if (log.isDebugEnabled()) {
//							log.debug("Registored connector : " + connectorName + " successfully.");
//						}
//				} catch (SecurityException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (IllegalArgumentException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (ClassNotFoundException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (NoSuchMethodException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (InstantiationException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (IllegalAccessException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (InvocationTargetException e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				} catch (Exception e) {
//					throw new IdentityProvisioningException("Error while creating connector object : "+connectorName+" which has type : " + className,e);
//				}
//			}
//		}
	}
	
	private List<AbstractIdentityProvisioningConnector> getConnectorList() {
		List<AbstractIdentityProvisioningConnector> connectors = new ArrayList<AbstractIdentityProvisioningConnector>();

		List<IdentityProvisioningConnectorFactory> registoredConnectorFactories = IdentityProvisionServiceComponent.connectors;
		for (IdentityProvisioningConnectorFactory factory : registoredConnectorFactories) {
			String connectorClassName = factory.getConnectorType();
    		if(log.isDebugEnabled()) {
    			log.debug("Creating provisioning connector instances from type :" + connectorClassName);
    		}
			
			String connectorClassPath = IdentityProvisioningConstants.IDENTITY_PROVISIONING_REG_PATH + RegistryConstants.PATH_SEPARATOR + connectorClassName;
			try {
				Registry registry = IdentityProvisionServiceComponent.getRegistryService().getConfigSystemRegistry();
		        if(registry.resourceExists(connectorClassPath)) {
		        	Collection collection = registry.get(connectorClassPath, 0, maxConnectors);
		        	String[] configPathArray = collection.getChildren();
		        	for(String configPath : configPathArray) {
		        		String connectorName = configPath.substring((connectorClassPath+RegistryConstants.PATH_SEPARATOR).length());
		        		if(log.isDebugEnabled()) {
		        			log.debug("Found connector config : " + connectorName + " of type :" + connectorClassName);
		        		}
		        		AbstractIdentityProvisioningConnector connector = null;
		        		
		        		// Look for already created connector
		        		if (factory.getConnector(connectorName) != null) {
							connector =  factory.getConnector(connectorName);
		        		}
		        		else {
			        		Resource config = registry.get(configPath);
			        		Properties connectorProperties = new Properties();
			        		Iterator<Entry<Object,Object>> itr = config.getProperties().entrySet().iterator();
			        		while(itr.hasNext()) {
			        			Entry<Object,Object> entry = itr.next();
			        			String key = entry.getKey().toString();
			        			String value = entry.getValue().toString().substring(1, entry.getValue().toString().length()-1);
			        			connectorProperties.setProperty(key, value);
	
				        		if(log.isDebugEnabled()) {
				        			log.debug("Adding property for connector config : " + connectorName + " key :" + key + " with value : " + value);
				        		}
			        		}
	
				        	String isEnable = connectorProperties.getProperty(IdentityProvisioningConstants.PropertyConfig.PREFIX_IDENTITY_PROVISIONING_CONNECTOR_ENABLE+connectorName);
				    		boolean isConnectorEnabled = false;
							if (isEnable != null) {
								isConnectorEnabled = Boolean.parseBoolean(isEnable.trim());
							}
							
	
			        		if(log.isDebugEnabled()) {
			        			log.debug("Creating connector instance of type :" + connectorClassName + " having name : " + connectorName + " with enabled : " + isConnectorEnabled);
			        		}
							connector =  factory.buildConnector(connectorName, isConnectorEnabled, connectorProperties);
		        		}

		        		connectors.add(connector);
		        	}
		        }
		        else {
	        		if(log.isDebugEnabled()) {
	        			log.debug("Cannot find any connector configuration of type :" + connectorClassName + " in path : " + connectorClassPath);
	        		}
		        	
		        }
			} catch (RegistryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return connectors;
	}
	
	@Override
	public boolean doPreAddUser(String userName, Object credential, String[] roleList,
	                            Map<String, String> claims, String profile,
	                            UserStoreManager userStoreManager) throws UserStoreException {

		if (log.isDebugEnabled()) {
			log.debug("Pre add user is called in IdentityMgtEventListener");
		}
		
		boolean isUserProvisioned = true;
		for(AbstractIdentityProvisioningConnector connector : getConnectorList()) {
			try {
				if(connector.getIsEnabled()) {
					String result = connector.createUser(userName, credential, roleList, claims,  profile, userStoreManager);
					
					if(result != null && !result.isEmpty()) {
						if(log.isDebugEnabled()) {
							log.debug("response user create result : " + result);
							log.debug(connector.getName() + " connector provisioned user successfully");
						}
					}
					else {
						// if(forceProvisioning == true) this check has to be done
//						isUserProvisioned = false;
					}
				}
				else {
					if(log.isDebugEnabled()) {
						log.debug(connector.getName() + " is disabled. Continue with rest of connectors");
					}
				}
			} catch (UserStoreException e) {
				isUserProvisioned = false;
				throw new UserStoreException("Error while provisioning user", e);
			}
		}
		
		if (isUserProvisioned) {
			if(log.isDebugEnabled()) {
				log.debug("Provisioning successfully finished.");
			}
			return true;
		}
		else {
			if(log.isDebugEnabled()) {
				log.debug("Provisioning failed...");
			}
			return false;
		}
	}

	/**
	 * Deleting user from the identity database. What are the registry keys ?
	 */
	@Override
	public boolean doPreDeleteUser(String userName, UserStoreManager userStoreManager)
	                                                                                   throws UserStoreException {
		if (log.isDebugEnabled()) {
			log.debug("Pre delete user is called in IdentityMgtEventListener");
		}
		
		
		boolean isUserDeleted = true;
		for(AbstractIdentityProvisioningConnector connector : getConnectorList()) {
			try {
				if(connector.getIsEnabled()) {
					String result = connector.deleteUser(userName, userStoreManager);
					if(result != null && !result.isEmpty()) {
						if(log.isDebugEnabled()) {
							log.debug("response user delete result : " + result);
							log.debug(connector.getName() + " connector de-provisioned user successfully");
						}
					}
					else {
						isUserDeleted = false;
					}
				}
				else {
					if(log.isDebugEnabled()) {
						log.debug(connector.getName() + " is disabled. Continue with rest of connectors");
					}
				}
			} catch (IdentityProvisioningException e) {
				isUserDeleted = false;
				throw new UserStoreException("Error while de-provisioning user", e);
			}
		}
		
		if (isUserDeleted) {
			if (log.isDebugEnabled()) {
				log.debug("De-Provisioning success...");
			}
			return true;
		}
		else {
			if (log.isDebugEnabled()) {
				log.debug("De-Provisioning failed...");
			}
			return false;
		}
	}

    public boolean doPreUpdateUserListOfRole(String roleName, String[] deletedUsers,
                                             String[] newUsers, UserStoreManager userStoreManager)
            throws UserStoreException {
		if (log.isDebugEnabled()) {
			log.debug("doPreUpdateUserListOfRole is called in IdentityMgtEventListener");
		}
		
		
		for(AbstractIdentityProvisioningConnector connector : getConnectorList()) {
			try {
				if(connector.getIsEnabled()) {
					boolean result = connector.updateUserListOfRole(roleName, deletedUsers, newUsers, userStoreManager);
				}
				else {
					if(log.isDebugEnabled()) {
						log.debug(connector.getName() + " is disabled. Continue with rest of connectors");
					}
				}
			} catch (IdentityProvisioningException e) {
				throw new UserStoreException("Error while doPreUpdateUserListOfRole user", e);
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("doPreUpdateUserListOfRole successed...");
		}
		return true;
    }
    

    public boolean doPreUpdateRoleListOfUser(String userName, String[] deletedRoles,
                                             String[] newRoles, UserStoreManager userStoreManager)
            throws UserStoreException {
		if (log.isDebugEnabled()) {
			log.debug("doPreUpdateRoleListOfUser is called in IdentityMgtEventListener");
		}
		
		
		for(AbstractIdentityProvisioningConnector connector : getConnectorList()) {
			try {
				if(connector.getIsEnabled()) {
					boolean result = connector.updateRoleListOfUser(userName, deletedRoles, newRoles, userStoreManager);
				}
				else {
					if(log.isDebugEnabled()) {
						log.debug(connector.getName() + " is disabled. Continue with rest of connectors");
					}
				}
			} catch (IdentityProvisioningException e) {
				throw new UserStoreException("Error while doPreUpdateRoleListOfUser user", e);
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("doPreUpdateRoleListOfUser successed...");
		}
		return true;
    }

    public boolean doPreAddRole(String roleName, String[] userList, Permission[] permissions,
                                UserStoreManager userStoreManager) throws UserStoreException {
		if (log.isDebugEnabled()) {
			log.debug("doPreAddRole is called in IdentityMgtEventListener");
		}
		
		
		for(AbstractIdentityProvisioningConnector connector : getConnectorList()) {
			try {
				if(connector.getIsEnabled()) {
					boolean result = connector.addRole(roleName, userList, permissions, userStoreManager);
				}
				else {
					if(log.isDebugEnabled()) {
						log.debug(connector.getName() + " is disabled. Continue with rest of connectors");
					}
				}
			} catch (IdentityProvisioningException e) {
				throw new UserStoreException("Error while doPreAddRole user", e);
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("doPreAddRole successed...");
		}
		return true;
    }
    

    public boolean doPreDeleteRole(String roleName, UserStoreManager userStoreManager)
            throws UserStoreException {
		if (log.isDebugEnabled()) {
			log.debug("doPreDeleteRole is called in IdentityMgtEventListener");
		}
		
		
		for(AbstractIdentityProvisioningConnector connector : getConnectorList()) {
			try {
				if(connector.getIsEnabled()) {
					boolean result = connector.deleteRole(roleName, userStoreManager);
				}
				else {
					if(log.isDebugEnabled()) {
						log.debug(connector.getName() + " is disabled. Continue with rest of connectors");
					}
				}
			} catch (IdentityProvisioningException e) {
				throw new UserStoreException("Error while doPreAddRole user", e);
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("doPreDeleteRole successed...");
		}
		return true;
    }
    
    

}
