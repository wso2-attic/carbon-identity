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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.provisioning.AbstractIdentityProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

public class SalesforceProvisioningConnector extends
		AbstractIdentityProvisioningConnector {

	private static final Log log = LogFactory
			.getLog(SalesforceProvisioningConnector.class);
	
	private SalesforceProvisioningConnectorConfig configBuilder;

	/**
	 * 
	 */
	public SalesforceProvisioningConnector(String name, boolean isEnabled,
			Properties configs) {
		super(name, isEnabled, configs);
		this.configBuilder = new SalesforceProvisioningConnectorConfig(configs);
		initializeSalesforceConnector();
	}

	/**
	 * Convert Identity server roles into Salesforce entitlements
	 */
	private void initializeSalesforceConnector() {

		if (log.isTraceEnabled()) {
			log.trace("Starting initializeSalesforceConnector() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();
		// TODO :
		// RegistorIdP for salesforce connector with the name :
		// SalesforceConstants.Salesforce_Connector_IdP
		// Get entitlement list
		// Register role mapping for entitlements

		if (log.isTraceEnabled()) {
			log.trace("Ending initializeSalesforceConnector() of "
					+ SalesforceProvisioningConnector.class);
		}
	}

	/**
	 * 
	 * @return
	 * @throws IdentityProvisioningException
	 */
	public String listUsers(String query) throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting listUsers() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		if (query == null || query.isEmpty()) {
			query = SalesforceProvisioningConnectorConfig.SALESFORCE_LIST_USER_SIMPLE_QUERY;
			// SalesforceProvisioningConnectorConfig.SALESFORCE_LIST_USER_FULL_QUERY;
		}

		HttpClient httpclient = new HttpClient();
		GetMethod get = new GetMethod(this.getDataQueryEndpoint());
		setAuthorizationHeader(get);

		// set the SOQL as a query param
		NameValuePair[] params = new NameValuePair[1];
		params[0] = new NameValuePair("q", query);
		get.setQueryString(params);

		StringBuilder sb = new StringBuilder();
		try {
			httpclient.executeMethod(get);
			if (get.getStatusCode() == HttpStatus.SC_OK) {
				
				JSONObject response = new JSONObject(new JSONTokener(
						new InputStreamReader(get.getResponseBodyAsStream())));
				if (isDebugEnabled) {
					log.debug("Query response: " + response.toString(2));
				}

				// Build the returning string
				sb.append(response.getString("totalSize")
						+ " record(s) returned\n\n");
				JSONArray results = response.getJSONArray("records");
				for (int i = 0; i < results.length(); i++) {
					sb.append(results.getJSONObject(i).getString("Id")
							+ ", "

							+ results.getJSONObject(i).getString("Alias")
							+ ", "
							+ results.getJSONObject(i).getString("Email")
							+ ", "
							+ results.getJSONObject(i).getString("LastName")
							+ ", "
							+ results.getJSONObject(i).getString("Name")
							+ ", "
							+ results.getJSONObject(i).getString("ProfileId")
							+ ", "

							// +
							// results.getJSONObject(i).getString("EmailEncodingKey")
							// + ", "
							// +
							// results.getJSONObject(i).getString("LanguageLocaleKey")
							// + ", "
							// +
							// results.getJSONObject(i).getString("LocaleSidKey")
							// + ", "
							// +
							// results.getJSONObject(i).getString("TimeZoneSidKey")
							// + ", "
							// +
							// results.getJSONObject(i).getString("UserPermissionsCallCenterAutoLogin")
							// + ", "
							// +
							// results.getJSONObject(i).getString("UserPermissionsMarketingUser")
							// + ", "
							// +
							// results.getJSONObject(i).getString("UserPermissionsOfflineUser")
							// + ", "

							+ results.getJSONObject(i).getString("Username")
							+ "\n");
				}
				sb.append("\n");
			} else {
				log.error("recieved response status code:"
						+ get.getStatusCode() + " text : "
						+ get.getStatusText());
			}
		} catch (JSONException e) {
			log.error("Error in decoding response to JSON");
			throw new IdentityProvisioningException(e);
		} catch (HttpException e) {
			log.error("Error in invoking provisioning operation for the user listing");
			throw new IdentityProvisioningException(e);
		} catch (IOException e) {
			throw new IdentityProvisioningException(e);
		} finally {
			get.releaseConnection();
		}

		if (isDebugEnabled) {
			log.debug("Returning string : " + sb.toString());
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending listUsers() of "
					+ SalesforceProvisioningConnector.class);
		}
		return sb.toString();
	}

	/**
	 * Provision the new user to Salesforce.
	 * 
	 * @throws IdentityProvisioningException
	 */
	public String createUser(String userName, Object credential,
			String[] roleList, Map<String, String> claims, String profile,
			UserStoreManager userStoreManager)
			throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting createUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		HttpClient httpclient = new HttpClient();
		JSONObject user = new JSONObject();
		try {
			// Mandetory properties : 12 and this will vary according to API
			// Version
			// Alias, Email, EmailEncodingKey, LanguageLocaleKey, LastName,
			// LocaleSidKey, ProfileId, TimeZoneSidKey, Username,
			// UserPermissionsCallCenterAutoLogin, UserPermissionsMarketingUser,
			// UserPermissionsOfflineUser

			Map<String, String> requiredAttributes = getRequiredAttributes(userName, credential,
					roleList, claims, profile, userStoreManager);
			Iterator<Entry<String,String>> iterator = requiredAttributes.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<String,String> mapEntry = iterator.next();
				if(mapEntry.getValue().equals("true")) {
					user.put(mapEntry.getKey(),true);
				}
				else if (mapEntry.getValue().equals("false")) {
					user.put(mapEntry.getKey(),false);
				}
				else {
					user.put(mapEntry.getKey(),mapEntry.getValue());
				}
				if(isDebugEnabled) {
					log.debug("The key is: " + mapEntry.getKey() + ",value is :" + mapEntry.getValue());
				}
			}
			if (isDebugEnabled) {
				log.debug("JSON object of User\n" + user.toString(2));
			}
			
		} catch (JSONException e) {
			log.error("Error while creating JSON body");
			throw new IdentityProvisioningException(e);
		}

		PostMethod post = new PostMethod(this.getUserObjectEndpoint());
		setAuthorizationHeader(post);

		try {
			post.setRequestEntity(new StringRequestEntity(user.toString(),
					SalesforceConnectorConstants.CONTENT_TYPE_APPLICATION_JSON,
					null));
		} catch (UnsupportedEncodingException e) {
			log.error("Error in encoding provisioning request");
			throw new IdentityProvisioningException(e);
		}

		String userId = null;
		try {
			httpclient.executeMethod(post);
			
			if(isDebugEnabled) {
				log.debug("HTTP status " + post.getStatusCode() + " creating user");
			}

			if (post.getStatusCode() == HttpStatus.SC_CREATED) {
				JSONObject response = new JSONObject(new JSONTokener(
						new InputStreamReader(post.getResponseBodyAsStream())));
				if (isDebugEnabled) {
					log.debug("Create response: " + response.toString(2));
				}

				if (response.getBoolean("success")) {
					userId = response.getString("id");
					if(isDebugEnabled) {
						log.debug("New record id " + userId);
					}
				}

				// persisting user id to a claim
				try {
					userStoreManager.getClaimManager().getClaim(configBuilder.getUserIdClaim()).setValue(userId);
				} catch (UserStoreException e) {
					log.error("Required claim value for user id is not stored in user store");
					throw new IdentityProvisioningException("Required claim value for user id is not stored in user store", e);
				}
			} else {
				log.error("recieved response status code :"
						+ post.getStatusCode() + " text : "
						+ post.getStatusText());
			}
		} catch (HttpException e) {
			log.error("Error in invoking provisioning operation for the user");
			throw new IdentityProvisioningException(e);
		} catch (IOException e) {
			log.error("Error in invoking provisioning operation for the user");
			throw new IdentityProvisioningException(e);
		} catch (JSONException e) {
			log.error("Error in decoding response to JSON");
			throw new IdentityProvisioningException(e);
		} finally {
			post.releaseConnection();
		}

		if (isDebugEnabled) {
			log.debug("Returning created user's ID : " + userId);
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending createUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		return userId;
	}

	private Map<String, String> getRequiredAttributes(String userName,
			Object credential, String[] roleList, Map<String, String> claims,
			String profile, UserStoreManager userStoreManager) throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting getRequiredAttributes() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();
		
		List<String> requiredAttributeNameList = configBuilder.getRequiredAttributeNames();
		if(isDebugEnabled) {
			log.debug(" Required attributes for salesforce connector : " + requiredAttributeNameList.toString());
		}
		
		Map<String,String> requiredAttributeValueMap = new HashMap<String,String>();
		for(String attribute : requiredAttributeNameList) {
			String value = null;
			// Find from claims
			String claim = this.getProperty(SalesforceConnectorConstants.PropertyConfig.REQUIRED_CLAIM_PREFIX + attribute);
			if (claim != null && !claim.isEmpty()) {
				if(claims.containsKey(claim)) {
					value = claims.get(claim);
				}
				// check for email username also
				else if (claim.equals("username")) {
					value = userName;
				}
				// TODO : change defining way username pick, apart from special value from claims
				// check for email username also
				else if (claim.equals("username")) {
					value = userName;
				}
				// TODO : change defining way profile pick, apart from special value from claims
				else if (attribute.equals("ProfileId") && claim.equals("roles")) {
					String[] entitlements = convertRolesToEntitlements(roleList);
					if (entitlements == null || entitlements.length == 0) {
						throw new IdentityProvisioningException(
								"Required attribute Entitlement is empty");
					}
					else {
						// TODO : add multiples values for profile
						value = entitlements[0];
						//value = "[{\"00e90000001STRnAAO\"},{\"00e90000001STRhAAO\"}]";
					}
				}
			}
			else {
				// Find from default value
				String defaultValue = this.getProperty(SalesforceConnectorConstants.PropertyConfig.REQUIRED_DEFAULT_PREFIX + attribute);
				if (defaultValue != null && !defaultValue.isEmpty()) {
					value = defaultValue;
				}
			}
			
			// Still attribute doesn't has a value
			if(value == null) {
				log.error("Required attribute : " + attribute + " cannot mapped to a value. Either required claim doesn't specified' or dont have a default value to attribute.");
				throw new IdentityProvisioningException("Required attribute : " + attribute + " cannot mapped to a value.");
			}
			
			
			requiredAttributeValueMap.put(attribute, value);
			if(isDebugEnabled) {
				log.debug("Required attribute : " + attribute + " mapped to value : " + value);
			}
		}
		
		if (log.isTraceEnabled()) {
			log.trace("Ending createUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		return requiredAttributeValueMap;
	}

	public String getUser(String userId) throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting getUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		HttpClient httpclient = new HttpClient();
		GetMethod get = new GetMethod(this.getUserObjectEndpoint() + userId);

		setAuthorizationHeader(get);

		StringBuilder sb = new StringBuilder();
		try {
			httpclient.executeMethod(get);
			if (get.getStatusCode() == HttpStatus.SC_OK) {
				JSONObject response = new JSONObject(new JSONTokener(
						new InputStreamReader(get.getResponseBodyAsStream())));
				if (isDebugEnabled) {
					log.debug("Query response: " + response.toString(2));
				}

				sb.append("User content\n\n");
				Iterator iterator = response.keys();
				while (iterator.hasNext()) {
					String key = (String) iterator.next();
					String value = response.getString(key);
					sb.append(key + ":" + (value != null ? value : "") + "\n");
				}
				sb.append("\n");
			} else {
				log.error("recieved response status code :"
						+ get.getStatusCode() + " text : "
						+ get.getStatusText());
			}
		} catch (HttpException e) {
			log.error("Error in invoking provisioning operation for the user");
			throw new IdentityProvisioningException(e);
		} catch (IOException e) {
			log.error("Error in invoking provisioning operation for the user");
			throw new IdentityProvisioningException(e);
		} catch (JSONException e) {
			log.error("Error in decoding response to JSON");
			throw new IdentityProvisioningException(e);
		} finally {
			get.releaseConnection();
		}

		if (isDebugEnabled) {
			log.debug("Returning string : " + sb.toString());
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending getUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		return sb.toString();
	}

	public String updateUser(String userName,
			UserStoreManager userStoreManager, boolean isActive)
			throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting updateUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		log.debug("Calling update user method()");
		HttpClient httpclient = new HttpClient();
		JSONObject update = new JSONObject();

		try {
			update.put("IsActive", isActive);
		} catch (JSONException e) {
			log.error("Error while creating JSON body");
			throw new IdentityProvisioningException(e);
		}

		String userId;
		StringBuilder sb = new StringBuilder();
		try {
			userId = userStoreManager.getClaimManager().getClaim(configBuilder.getUserIdClaim()).getValue();
			
			if(userId != null && !userId.isEmpty()) {
				PostMethod patch = new PostMethod(this.getUserObjectEndpoint() + userId) {
					@Override
					public String getName() {
						return "PATCH";
					}
				};
				setAuthorizationHeader(patch);

				patch.setRequestEntity(new StringRequestEntity(update.toString(),
						"application/json", null));
				
				try {
					httpclient.executeMethod(patch);
					if (patch.getStatusCode() == HttpStatus.SC_OK
							|| patch.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
						sb.append("HTTP status " + patch.getStatusCode()
								+ " updating user " + userId + "\n\n");
					} else {
						log.error("recieved response status code :"
								+ patch.getStatusCode() + " text : "
								+ patch.getStatusText());
					}
					
					if (isDebugEnabled) {
						log.debug("Returning string : " + sb.toString());
					}

					
				} catch (HttpException e) {
					log.error("Error in invoking provisioning request");
					throw new IdentityProvisioningException(e);
				} catch (IOException e) {
					log.error("Error in invoking provisioning request");
					throw new IdentityProvisioningException(e);
				} finally {
					patch.releaseConnection();
				}

				
			} else {
				throw new IdentityProvisioningException("Cannot update user, userId is invalide.");
			}
		} catch (UserStoreException e) {
			log.error("Required claim value for user id is not stored in user store");
			throw new IdentityProvisioningException("Required claim value for user id is not stored in user store", e);
		} catch (UnsupportedEncodingException e) {
			log.error("Error in encoding provisioning request");
			throw new IdentityProvisioningException(e);
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending updateUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		return sb.toString();
	}

	public String deleteUser(String userName, UserStoreManager userStoreManager) throws IdentityProvisioningException {

		if (log.isTraceEnabled()) {
			log.trace("Starting deleteUser() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();
		
		String result = updateUser(userName, userStoreManager, false);

//		HttpClient httpclient = new HttpClient();
//
//		DeleteMethod delete = new DeleteMethod(this.getUserObjectEndpoint()+ userId);
//
//		// set the token in the header
//		String accessToken = authenticate();
//		delete.setRequestHeader("Authorization", "OAuth " + accessToken);
//
//		StringBuilder sb = new StringBuilder();
//		try {
//			httpclient.executeMethod(delete);
//			if (delete.getStatusCode() == HttpStatus.SC_OK
//					|| delete.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
//				sb.append("HTTP status " + delete.getStatusCode()
//						+ " deleting user " + userId + "\n\n");
//			} else {
//				log.error("recieved response status code :"
//						+ delete.getStatusCode() + " text : "
//						+ delete.getStatusText());
//			}
//		} catch (HttpException e) {
//			log.error("Error in invoking provisioning operation for delete");
//			throw new IdentityProvisionException(e);
//		} catch (IOException e) {
//			throw new IdentityProvisionException(e);
//		} finally {
//			delete.releaseConnection();
//		}
//
//		if (isDebugEnabled) {
//			log.debug("Returning string : " + sb.toString());
//		}
		if (isDebugEnabled) {
			log.debug("Returning string : " + result);
		}
		if (log.isTraceEnabled()) {
			log.trace("Ending deleteUser() of "
					+ SalesforceProvisioningConnector.class);
		}
//		return sb.toString();
		return result;
	}

	/**
	 * Builds Salesforce user end point using configurations
	 * @return
	 */
	private String getUserObjectEndpoint() {
		if (log.isTraceEnabled()) {
			log.trace("Starting getUserObjectEndpoint() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		String url = this.getProperty(SalesforceConnectorConstants.PropertyConfig.DOMAIN_NAME)
				+ SalesforceConnectorConstants.CONTEXT_SERVICES_DATA
				+ this.getProperty(SalesforceConnectorConstants.PropertyConfig.API_VERSION)
				+ SalesforceConnectorConstants.CONTEXT_SOOBJECTS_USER;
		if (isDebugEnabled) {
			log.debug("Built user endpoint url : " + url);
		}

		return url;
	}

	/**
	 * Builds Salesforce query point using configurations
	 * @return
	 */
	private String getDataQueryEndpoint() {
		if (log.isTraceEnabled()) {
			log.trace("Starting getDataQueryEndpoint() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		String url = this.getProperty(SalesforceConnectorConstants.PropertyConfig.DOMAIN_NAME)
				+ SalesforceConnectorConstants.CONTEXT_SERVICES_DATA
				+ this.getProperty(SalesforceConnectorConstants.PropertyConfig.API_VERSION)
				+ SalesforceConnectorConstants.CONTEXT_QUERY;
		if (isDebugEnabled) {
			log.debug("Built query endpoint url : " + url);
		}

		return url;
	}

	/**
	 * Adding OAuth authorization headers to a httpMethod
	 * 
	 * @param httpMethod
	 *            method which wants to add Authorization header
	 */
	private void setAuthorizationHeader(HttpMethodBase httpMethod) throws IdentityProvisioningException{
		if (log.isTraceEnabled()) {
			log.trace("Starting setAuthorizationHeader() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		String accessToken = authenticate();
		if (accessToken != null && !accessToken.isEmpty()) {
			httpMethod.setRequestHeader(
					SalesforceConnectorConstants.AUTHORIZATION_HEADER_NAME,
					SalesforceConnectorConstants.AUTHORIZATION_HEADER_OAUTH + " "
							+ accessToken);

			if (isDebugEnabled) {
				log.debug("Setting authorization header for method : "
						+ httpMethod.getName() + " as follows,");
				Header authorizationHeader = httpMethod
						.getRequestHeader(SalesforceConnectorConstants.AUTHORIZATION_HEADER_NAME);
				log.debug(authorizationHeader.getName() + ": "
						+ authorizationHeader.getValue());
			}
		}
		else {
			throw new IdentityProvisioningException("Authentication failed");
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending setAuthorizationHeader() of "
					+ SalesforceProvisioningConnector.class);
		}
	}

	/**
	 * Authenticate to Salesforce API.
	 */
	private String authenticate() throws IdentityProvisioningException {
		if (log.isTraceEnabled()) {
			log.trace("Starting authenticate() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		HttpClient httpclient = new HttpClient();

		PostMethod post = new PostMethod(
				SalesforceConnectorConstants.OAUTH2_TOKEN_ENDPOINT);

		post.addParameter(SalesforceConnectorConstants.CLIENT_ID,this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.CLIENT_ID));
		post.addParameter(SalesforceConnectorConstants.CLIENT_SECRET, this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.CLIENT_SECRET));
		post.addParameter(SalesforceConnectorConstants.PASSWORD, this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.PASSWORD));
		post.addParameter(SalesforceConnectorConstants.GRANT_TYPE, this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.GRANT_TYPE));
		post.addParameter(SalesforceConnectorConstants.USERNAME, this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.USERNAME));

		StringBuilder sb = new StringBuilder();
		try {
			// send the request
			int responseStatus = httpclient.executeMethod(post);
			if (isDebugEnabled) {
				log.debug("Authentication to salesforce returned with response code: "
						+ responseStatus);
			}

			sb.append("HTTP status " + post.getStatusCode()
					+ " creating user\n\n");

			if (post.getStatusCode() == HttpStatus.SC_OK) {
				JSONObject response = new JSONObject(new JSONTokener(
						new InputStreamReader(post.getResponseBodyAsStream())));
				if (isDebugEnabled) {
					log.debug("Authenticate response: " + response.toString(2));
				}

				Object attributeValObj = response.opt("access_token");
				if (attributeValObj instanceof String) {
					if (isDebugEnabled) {
						log.debug("Access token is : "
								+ (String) attributeValObj);
					}
					return (String) attributeValObj;
				} else {
					log.error("Authentication response type : "
							+ attributeValObj.toString() + " is invalide");
				}
			} else {
				log.error("recieved response status code :"
						+ post.getStatusCode() + " text : "
						+ post.getStatusText());
			}
		} catch (JSONException e) {
			throw new IdentityProvisioningException("Error in decoding response to JSON", e);
		} catch (IOException e) {
			throw new IdentityProvisioningException("Error in invoking authentication operation. Check your network connection", e);
		} finally {
			post.releaseConnection();
		}

		if (log.isTraceEnabled()) {
			log.trace("Ending authentication() of "
					+ SalesforceProvisioningConnector.class);
		}
		return "";
	}

	/**
	 * Convert Identity server roles into Salesforce entitlements
	 * @throws IdentityProvisioningException 
	 */
	private String[] convertRolesToEntitlements(String[] roles) throws IdentityProvisioningException {
		if (log.isTraceEnabled()) {
			log.trace("Starting convertRolesToEntitlements() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		List<String> entitlements = new ArrayList<String>();
		if (roles == null || roles.length == 0) {
			// TODO : Get default entitlements from file
		} else {
			List<LocalRole> localRoleList = new ArrayList<LocalRole>();
			for(String role : roles) {
				localRoleList.add(new LocalRole(role));
			}
			LocalRole[] localRoles = localRoleList.toArray(new LocalRole[0]);

			Set<RoleMapping> roleMappingSet;
            try {
	            roleMappingSet = IdentityProviderManager
	            		.getInstance()
	            		.getMappedIdPRoles(
	            				this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.IDP_NAME),
	            				CarbonContext.getThreadLocalCarbonContext()
	            						.getTenantDomain(), localRoles);
            } catch (IdentityApplicationManagementException e) {
                throw new IdentityProvisioningException("Error while converting IS roles to external roles", e);
            }


	        Iterator<RoleMapping> itr = roleMappingSet.iterator();
	        while(itr.hasNext()){
	        	RoleMapping mapping = itr.next();
				if (isDebugEnabled) {
					log.debug("Converted Identity Server Role : "
							+ mapping.getLocalRole().toString()
							+ " into Salesforce entitlement : " + mapping.getRemoteRole());
				}
				entitlements.add(mapping.getRemoteRole());
	        }
		}
		return entitlements.toArray(new String[entitlements.size()]);
	}

	private String buildLastName() throws IdentityProvisioningException {
		if (log.isTraceEnabled()) {
			log.trace("Starting buildLastName() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		// TODO : add extention point
		throw new IdentityProvisioningException(
				"Required claim LastName is not specified");
	}

	private String buildEmailUserName() throws IdentityProvisioningException {
		if (log.isTraceEnabled()) {
			log.trace("Starting buildLastName() of "
					+ SalesforceProvisioningConnector.class);
		}
		boolean isDebugEnabled = log.isDebugEnabled();

		// TODO : add extention point
		throw new IdentityProvisioningException(
				"Required claim email username is not specified");
	}


	@Override
	public boolean addRole(String roleName, String[] userList,
			Permission[] permissions, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		return true;
	}


	@Override
	public boolean deleteRole(String roleName, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		return true;
	}


	@Override
	public boolean updateUserListOfRole(String roleName, String[] deletedUsers,
			String[] newUsers, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		return true;
	}


	@Override
	public boolean updateRoleListOfUser(String userName, String[] deletedRoles,
			String[] newRoles, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {
		return true;
	}
}
