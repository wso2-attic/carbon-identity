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
package org.wso2.carbon.identity.provisioning.salesforce.scim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.provisioning.AbstractIdentityProvisioningConnector;
import org.wso2.carbon.identity.provisioning.IdentityProvisioningException;
import org.wso2.carbon.identity.provisioning.salesforce.SalesforceConnectorConstants;
import org.wso2.carbon.identity.provisioning.salesforce.SalesforceProvisioningConnectorConfig;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;
import org.wso2.carbon.user.api.Permission;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.charon.core.client.SCIMClient;
import org.wso2.charon.core.exceptions.AbstractCharonException;
import org.wso2.charon.core.exceptions.BadRequestException;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.protocol.ResponseCodeConstants;
import org.wso2.charon.core.schema.SCIMConstants;

public class SalesforceSCIMProvisioningConnector extends AbstractIdentityProvisioningConnector{

	private static final Log log = LogFactory
			.getLog(SalesforceSCIMProvisioningConnector.class);
	
	public static final String DOMAIN_NAME = "https://wso2is-dev-ed.my.salesforce.com";
	// SCIM Endpoints
	public static final String SCHEMAS_ENDPOINT = DOMAIN_NAME + "/services/apexrest/scim/v1/Schemas";
	public static final String SP_CONFIGS_ENDPOINT = DOMAIN_NAME + "/services/apexrest/scim/v1/ServiceProviderConfigs";
	public static final String USERS_ENDPOINT = DOMAIN_NAME + "/services/apexrest/scim/v1/Users";
	public static final String GROUPS_ENDPOINT = DOMAIN_NAME + "/services/apexrest/scim/v1/Groups";
	public static final String ENTITLEMENTS_ENDPOINT = DOMAIN_NAME + "/services/apexrest/scim/v1/Entitlements";

	public static final String CLAIM_URI_EMAIL = "http://wso2.org/claims/emailaddress";
//	public static final String CLAIM_URI_FIRSTNAME = "http://wso2.org/claims/givenname";
	public static final String CLAIM_URI_LASTNAME = "http://wso2.org/claims/lastname";
	
	private SalesforceProvisioningConnectorConfig configBuilder;

	/**
	 * 
	 */
	public SalesforceSCIMProvisioningConnector(String name, boolean isEnabled,
			Properties configs) {
		super(name, isEnabled, configs);
		this.configBuilder = new SalesforceProvisioningConnectorConfig(configs);

    	// TODO : 
    	// RegistorIdP for salesforce connector with the name : SalesforceConstants.Salesforce_Connector_IdP
    	// Get entitlement list
    	// Register role mapping for entitlements
	}
	
	
    /**
     * Provision the new user to Salesforce SCIM Provider.
     * @throws IdentityProvisioningException 
     */
    public String createUser(String userName, Object credential, String[] roleList,
            Map<String, String> claims, String profile,
            UserStoreManager userStoreManager) throws IdentityProvisioningException {

    	String emailUserName = null;
    	if (claims.containsKey(SalesforceSCIMProvisioningConnector.CLAIM_URI_EMAIL)) {
    		emailUserName = claims.get(SalesforceSCIMProvisioningConnector.CLAIM_URI_EMAIL);
    	}
    	else {
    		emailUserName = buildEmailUserName();
    	}
    	
    	String lastName = null;
    	if (claims.containsKey(SalesforceSCIMProvisioningConnector.CLAIM_URI_LASTNAME)) {
    		lastName = claims.get(SalesforceSCIMProvisioningConnector.CLAIM_URI_LASTNAME);
    	}
    	else {
    		lastName = buildLastName();
    	}  	
    	
    	String [] emails = {emailUserName};

    		
    	String[] entitlements = convertRolesToEntitlements(roleList);
    	if( entitlements == null || entitlements.length == 0 ) {
    		throw new IdentityProvisioningException("Required attribute Entitlement is empty");
        }
    	
    	
    	String accessToken = authenticate();
    	
    	
        String userId = null;
        try {
            //encode payload using SCIMClient API.
            SCIMClient scimClient = new SCIMClient();
            
            //create a user according to SCIM User Schema
            User scimUser = scimClient.createUser();
            
            // Set attributes of the user
            scimUser.setUserName(emailUserName);
            scimUser.setFamilyName(lastName);
            scimUser.setEmails(emails);
            
            // TODO : Improve charon core to be able to set entitlements
//            scimUser.setEntitlements(entitlements);
            
            String contentType = SCIMConstants.APPLICATION_JSON;
            
            //encode the user in JSON format
            String encodedUser = scimClient.encodeSCIMObject(scimUser, SCIMConstants.identifyFormat(contentType));
            log.debug("encoded JSON user : " + encodedUser);

            PostMethod postMethod = new PostMethod(SalesforceSCIMProvisioningConnector.USERS_ENDPOINT);
            postMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,	"Bearer "+accessToken);
            
            //create request entity with the payload.
            RequestEntity requestEntity = new StringRequestEntity(encodedUser, contentType, null);
            postMethod.setRequestEntity(requestEntity);

            //create http client
            HttpClient httpClient = new HttpClient();
            
            //send the request
            int responseStatus = httpClient.executeMethod(postMethod);
            log.debug("Provision salesforce user operation returned with response code: " + responseStatus);

            String response = postMethod.getResponseBodyAsString();
            if (log.isDebugEnabled()) {
                log.debug("Provision salesforce user response: " + response);
            }
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //try to decode the scim object to verify that it gets decoded without issue.
                SCIMObject client = scimClient.decodeSCIMResponse(response, SCIMConstants.identifyFormat(contentType), SCIMConstants.USER_INT);
                return response;
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(response, SCIMConstants.identifyFormat(contentType));
                log.error(exception.getDescription());
            }

        } catch (CharonException e) {
            log.error("Error in encoding the object to be provisioned.");
            throw new IdentityProvisioningException(e);
        } catch (UnsupportedEncodingException e) {
            log.error("Error in creating request for provisioning");
            throw new IdentityProvisioningException(e);
        } catch (HttpException e) {
            log.error("Error in invoking provisioning operation for the user with id: " + userId);
            throw new IdentityProvisioningException(e);
        } catch (BadRequestException e) {
            log.error("Error in invoking provisioning operation for the user with id: " + userId);
            throw new IdentityProvisioningException(e);
        } catch (IOException e) {
            log.error("Error in invoking provisioning operation for the user with id: " + userId);
            throw new IdentityProvisioningException(e);
        }
        return "";
    }
    

	@Override
	public String deleteUser(String userName, UserStoreManager userStoreManager)
			throws IdentityProvisioningException {

        try {
			String emailUserName = userStoreManager.getClaimManager().getClaim(SalesforceSCIMProvisioningConnector.CLAIM_URI_EMAIL).getValue();
		   	String lastName = userStoreManager.getClaimManager().getClaim(SalesforceSCIMProvisioningConnector.CLAIM_URI_LASTNAME).getValue();
		   	String [] emails = {emailUserName};
    	
		   	String accessToken = authenticate();
    	
    	
		   	String userId = null;
            //encode payload using SCIMClient API.
            SCIMClient scimClient = new SCIMClient();
            
            //create a user according to SCIM User Schema
            User scimUser = scimClient.createUser();
            
            // Set attributes of the user
            scimUser.setUserName(emailUserName);
            scimUser.setFamilyName(lastName);
            scimUser.setEmails(emails);
            
            String contentType = SCIMConstants.APPLICATION_JSON;
            
            //encode the user in JSON format
            String encodedUser = scimClient.encodeSCIMObject(scimUser, SCIMConstants.identifyFormat(contentType));
            log.debug("encoded JSON user : " + encodedUser);


            DeleteMethod deleteMethod = new DeleteMethod(SalesforceSCIMProvisioningConnector.USERS_ENDPOINT);
            deleteMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,	"Bearer "+accessToken);
            
            //create request entity with the payload.
            RequestEntity requestEntity = new StringRequestEntity(encodedUser, contentType, null);
//            deleteMethod.setRequestEntity(requestEntity);

            //create http client
            HttpClient httpClient = new HttpClient();
            
            //send the request
            int responseStatus = httpClient.executeMethod(deleteMethod);
            log.debug("Provision salesforce user operation returned with response code: " + responseStatus);

            String response = deleteMethod.getResponseBodyAsString();
            if (log.isDebugEnabled()) {
                log.debug("Provision salesforce user response: " + response);
            }
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //try to decode the scim object to verify that it gets decoded without issue.
                scimClient.decodeSCIMResponse(response, SCIMConstants.identifyFormat(contentType), SCIMConstants.USER_INT);
                return response;
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(response, SCIMConstants.identifyFormat(contentType));
                log.error(exception.getDescription());
            }

        } catch (UserStoreException e) {
            log.error("Error in encoding the object to be de-provisioned.");
            throw new IdentityProvisioningException(e);
        } catch (CharonException e) {
            log.error("Error in encoding the object to be de-provisioned.");
            throw new IdentityProvisioningException(e);
        } catch (UnsupportedEncodingException e) {
            log.error("Error in creating request for de-provisioning");
            throw new IdentityProvisioningException(e);
        } catch (HttpException e) {
            log.error("Error in invoking de-provisioning operation for the user : " + userName);
            throw new IdentityProvisioningException(e);
        } catch (BadRequestException e) {
            log.error("Error in invoking de-provisioning operation for the user : " + userName);
            throw new IdentityProvisioningException(e);
        } catch (IOException e) {
            log.error("Error in invoking de-provisioning operation for the user : " + userName);
            throw new IdentityProvisioningException(e);
        }
        return "";
    }
    
    private String buildLastName() throws IdentityProvisioningException{
    	// TODO : add extention point
		throw new IdentityProvisioningException("Required claim LastName is not specified");
	}

	private String buildEmailUserName() throws IdentityProvisioningException{
		// TODO : add extention point
		throw new IdentityProvisioningException("Required claim email username is not specified");
	}

	/**
     * Authenticate to Salesforce API.
     */
    private String authenticate() {
        try {
            PostMethod postMethod = new PostMethod(SalesforceConnectorConstants.OAUTH2_TOKEN_ENDPOINT);
            
            postMethod.addParameter("client_id", "3MVG9Y6d_Btp4xp5zKJnvE4G6.MiflYmrInT73uTwt5FxkDbB73v5VuD_SZnpFzvImwNJCE2tyyeWnvf1BZ3z");
            postMethod.addParameter("password", "pw`123SFfrLAKpkPpgDvSVaDL6FCBGnTs");
            postMethod.addParameter("client_secret", "6614102027206729");
            postMethod.addParameter("grant_type", "password");
            postMethod.addParameter("username", "darshanasbg@gmail.com");

            //create http client
            HttpClient httpClient = new HttpClient();
            
            //send the request
            int responseStatus = httpClient.executeMethod(postMethod);
            log.debug("Authentication to salesforce returned with response code: " + responseStatus);

            String response = postMethod.getResponseBodyAsString();
            if (log.isDebugEnabled()) {
                log.debug("Authentication salesforce response: " + response);
            }
            if (evaluateResponseStatus(responseStatus)) {
                //try to decode the scim object to verify that it gets decoded without issue.

                //decode the string into json representation
                JSONObject decodedJsonObj = new JSONObject(new JSONTokener(response));

                Object attributeValObj = decodedJsonObj.opt("access_token");
                if (attributeValObj instanceof String) {
                    if (log.isDebugEnabled()) {
                        log.debug("Access token is : " + (String)attributeValObj);
                    }
                    return (String)attributeValObj;
                }
                else {
                    //decode scim exception and extract the specific error message.
                    log.error("Authentication response type : " + attributeValObj.toString() + " is invalide");
                }
            } else {
                //decode scim exception and extract the specific error message.
                log.error("Authentication response state : " + responseStatus + " is invalide");
            }

        } catch (UnsupportedEncodingException e) {
            log.error("Error in creating request for provisioning");
            log.error(e.getMessage());
            e.printStackTrace();
        } catch (HttpException e) {
            log.error("Error in invoking authentication operation");
            log.error(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            log.error("Error in invoking authentication operation");
            log.error(e.getMessage());
            e.printStackTrace();
        } catch (JSONException e) {
            log.error("Error in decoding response to JSON");
            log.error(e.getMessage());
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
					+ SalesforceSCIMProvisioningConnector.class);
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
//							this.configBuilder.getValue(SalesforceConnectorConstants.PropertyConfig.IDP_NAME),
	            				SalesforceConnectorConstants.Salesforce_Connector_IdP,
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
    
    
    /**
     * Identify whether the response includes a success response or failure response according to the
     * response status code.
     *
     * @param statusCode
     * @return
     */
    private boolean evaluateResponseStatus(int statusCode) {
        switch (statusCode) {
            //ok
            case ResponseCodeConstants.CODE_OK:
                return true;
            case ResponseCodeConstants.CODE_CREATED:
                return true;

            case ResponseCodeConstants.CODE_NO_CONTENT:
                return true;

            case ResponseCodeConstants.CODE_UNAUTHORIZED:
                return false;

            case ResponseCodeConstants.CODE_FORMAT_NOT_SUPPORTED:
                return false;

            case ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR:
                return false;

            case ResponseCodeConstants.CODE_RESOURCE_NOT_FOUND:
                return false;

            case ResponseCodeConstants.CODE_BAD_REQUEST:
                return false;

            default:
                return false;
        }
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
