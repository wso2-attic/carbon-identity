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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openspml.v2.client.Spml2Client;
import org.openspml.v2.msg.spml.AddRequest;
import org.openspml.v2.msg.spml.AddResponse;
import org.openspml.v2.msg.spml.DeleteRequest;
import org.openspml.v2.msg.spml.DeleteResponse;
import org.openspml.v2.msg.spml.Extensible;
import org.openspml.v2.msg.spml.PSO;
import org.openspml.v2.msg.spml.PSOIdentifier;
import org.openspml.v2.msg.spml.ReturnData;
import org.openspml.v2.msg.spml.StatusCode;
import org.openspml.v2.profiles.dsml.DSMLAttr;
import org.openspml.v2.util.Spml2Exception;
import org.wso2.carbon.identity.provisioning.AbstractIdentityProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

public class SPMLProvisioningConnector extends
		AbstractIdentityProvisioningConnector {

	private SPMLProvisioningConnectorConfig configBuilder;
	private static final Log log = LogFactory
			.getLog(SPMLProvisioningConnector.class);

	public SPMLProvisioningConnector(String name, boolean isEnabled,
			Properties configs) {
		super(name, isEnabled, configs);
		this.configBuilder = new SPMLProvisioningConnectorConfig(configs);
	}

	@Override
	public String createUser(String userName, Object credential,
			String[] roleList, Map<String, String> claims, String profile,
			UserStoreManager userStoreManager) throws UserStoreException {

		if (log.isTraceEnabled()) {
			log.trace("Starting createUser() of "
					+ SPMLProvisioningConnector.class);
		}

		boolean isDebugEnabled = log.isDebugEnabled();

		String spmlRoleName = "spml";
		// Check whether spml role assigned to the user
		boolean isUserHavingSpmlRole = false;

		if (roleList != null) {
			for (String role : roleList) {
				if (role != null && role.equals(spmlRoleName)) {
					isUserHavingSpmlRole = true;
					break;
				}
			}
		}

		String psoIdString = "";
		if (isUserHavingSpmlRole) {

			if (userName.length() > 15) {
				if (isDebugEnabled) {
					log.debug("Username Should be less than 16 charaters lenght "
							+ SPMLProvisioningConnector.class);
				}
			}

			 Map<String, String> requiredAttributes = getRequiredAttributes(userName, credential, roleList, claims, profile, userStoreManager);
			 if (isDebugEnabled) {
					log.debug("Required Attributes for createUser "+requiredAttributes);
				}
			try {
				Spml2Client spml2Client = new Spml2Client(
						SPMLConnectorConstants.SPML_PROVIDER_ENDPOINT);
				AddRequest req = new AddRequest();
				req.setReturnData(ReturnData.EVERYTHING);
				Extensible attrs = new Extensible();
				attrs.addOpenContentElement(new DSMLAttr("objectclass",
						SPMLConnectorConstants.SPML_SERVICE_OBJECT_CLASS));
				attrs.addOpenContentElement(new DSMLAttr("accountId", userName));
				attrs.addOpenContentElement(new DSMLAttr("credentials",
						credential.toString()));
				req.setData(attrs);

				AddResponse res = (AddResponse) spml2Client.send(req);
				if (res.getStatus().equals(StatusCode.SUCCESS)) {
					System.out.println("Received positive add response.");
					if (isDebugEnabled) {
						log.debug("Recived positive add response of  "	+ userName);
					}

					PSO pso = res.getPso();
					PSOIdentifier psoId = pso.getPsoID();
					psoIdString = psoId.getID();

				}
			} catch (Spml2Exception e) {
				if (isDebugEnabled) {
					log.debug("Spml2Exception occured in"
							+ SPMLProvisioningConnector.class, e);
				}

			}

		} else {
			if (isDebugEnabled) {
				log.debug("User does not have spml role, hence proceed with not provisioning");
			}
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending createUser() of "	+ SPMLProvisioningConnector.class);
		}
		return psoIdString;
	}

	@Override
	public String deleteUser(String userName, UserStoreManager userStoreManager)
			throws UserStoreException {

		if (log.isTraceEnabled()) {
			log.trace("Starting deleteUser() of "
					+ SPMLProvisioningConnector.class);
		}
		
		String result = null;
		boolean isDebugEnabled = log.isDebugEnabled();

		String[] roleList = userStoreManager.getRoleListOfUser(userName);
		String spmlRoleName = "spml";

		boolean isUserHavingSpmlRole = false;

		if (roleList != null) {
			for (String role : roleList) {
				if (role != null && role.equals(spmlRoleName)) {
					isUserHavingSpmlRole = true;
					break;
				}
			}
		}

		if (isUserHavingSpmlRole) {
			try {
				Spml2Client spml2Client = new Spml2Client(
						SPMLConnectorConstants.SPML_PROVIDER_ENDPOINT);
				PSOIdentifier psoId = new PSOIdentifier(userName, null, null);
				DeleteRequest deleteRequest = new DeleteRequest();
				deleteRequest.setPsoID(psoId);
				DeleteResponse deleteResponse = (DeleteResponse) spml2Client
						.send(deleteRequest);

				if (deleteResponse.getStatus().equals(StatusCode.SUCCESS)) {
					result = "Deleted user :" + userName;
					if (isDebugEnabled) {
						log.debug(result);
					}
				}
			} catch (Spml2Exception e) {
				if (isDebugEnabled) {
					log.debug("Spml2Exception occured in"
							+ SPMLProvisioningConnector.class, e);
				}
			}
		} else {
			if (isDebugEnabled) {
				log.debug("User does not have spml role, hence proceed without de-provisioning of "+ SPMLProvisioningConnector.class);
			}
		}
		
		if (log.isTraceEnabled()) {
			log.trace("Ending DeleteUser() of "	+ SPMLProvisioningConnector.class);
		}
		return result;
	}

	@Override
	public boolean updateUserListOfRole(String roleName, String[] deletedUsers,
			String[] newUsers, UserStoreManager userStoreManager)
			throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updateRoleListOfUser(String userName, String[] deletedRoles,
			String[] newRoles, UserStoreManager userStoreManager)
			throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addRole(String roleName, String[] userList,
			Permission[] permissions, UserStoreManager userStoreManager)
			throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteRole(String roleName, UserStoreManager userStoreManager)
			throws UserStoreException {
		// TODO Auto-generated method stub
		return false;
	}

	private Map<String, String> getRequiredAttributes(String userName,
			Object credential, String[] roleList, Map<String, String> claims,
			String profile, UserStoreManager userStoreManager)
			throws UserStoreException {

		if (log.isTraceEnabled()) {
			log.trace("Starting getRequiredAttributes() of "
					+ SPMLProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		List<String> requiredAttributeNameList = configBuilder
				.getRequiredAttributeNames();
		if (isDebugEnabled) {
			log.debug(" Required attributes for spml connector : "
					+ requiredAttributeNameList.toString());
		}

		Map<String, String> requiredAttributeValueMap = new HashMap<String, String>();
		for (String attribute : requiredAttributeNameList) {
			String value = null;
			// Find from claims
			String claim = this
					.getProperty(SPMLConnectorConstants.PropertyConfig.REQUIRED_CLAIM_PREFIX
							+ attribute);
			if (claim != null && !claim.isEmpty()) {
				if (claims != null && !claims.isEmpty()
						&& claims.containsKey(claim)) {
					value = claims.get(claim);
				} else {
					// Search for claim from user store
					try {
						String clamValue = userStoreManager.getUserClaimValue(
								userName, claim, null);
						if (clamValue != null && !clamValue.isEmpty()) {
							value = clamValue;
						}
					} catch (org.wso2.carbon.user.api.UserStoreException e) {
						throw new UserStoreException(e);
					}
				}

			} 

			// Still attribute doesn't has a value
			if (value == null) {
				log.error("Required attribute : "
						+ attribute
						+ " cannot mapped to a value. Either required claim doesn't specified' or dont have a default value to attribute.");
				throw new IdentityProvisioningException("Required attribute : "
						+ attribute + " cannot mapped to a value.");
			}

			requiredAttributeValueMap.put(attribute, value);
			if (isDebugEnabled) {
				log.debug("Required attribute : " + attribute
						+ " mapped to value : " + value);
			}
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending getRequiredAttributes() of "
					+ SPMLProvisioningConnector.class);
		}

		return requiredAttributeValueMap;
	}

}
