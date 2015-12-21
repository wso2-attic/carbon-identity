/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.common.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.scim.common.utils.BasicAuthUtil;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonConstants;
import org.wso2.charon.core.client.SCIMClient;
import org.wso2.charon.core.config.SCIMConfigConstants;
import org.wso2.charon.core.config.SCIMProvider;
import org.wso2.charon.core.exceptions.AbstractCharonException;
import org.wso2.charon.core.exceptions.BadRequestException;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.objects.AbstractSCIMObject;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.ListedResource;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.schema.SCIMConstants;
import org.wso2.charon.core.util.CopyUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * This class implements logic to initiate SCIM provisioning operations to other SCIM provider endpoints.
 * Since SCIM provisioning operations are usually run asynchronously, this runs in a separate thread.
 */
public class ProvisioningClient implements Runnable {

    private static Log logger = LogFactory.getLog(ProvisioningClient.class.getName());
    private final String USER_FILTER = "filter=userName%20Eq%20";
    private final String GROUP_FILTER = "filter=displayName%20Eq%20";
    SCIMObject scimObject;
    SCIMProvider provider;
    int provisioningMethod;
    private int objectType;
    private Map<String, Object> additionalProvisioningInformation;

    /**
     * Initialize parameters to be used in the SCIM User operation which will be invoked by the run operation
     * of the thread.
     *
     * @param scimProvider
     * @param user
     * @param httpMethod
     */
    public ProvisioningClient(SCIMProvider scimProvider, User user, int httpMethod,
                              Map<String, Object> additionalInformation) {
        this.objectType = SCIMConstants.USER_INT;
        provider = scimProvider;
        scimObject = user;
        provisioningMethod = httpMethod;
        additionalProvisioningInformation = additionalInformation;
    }

    /**
     * Initialize parameters to be used in the SCIM Group operation which will be invoked by the run operation
     * of the thread.
     *
     * @param scimProvider
     * @param group
     * @param httpMethod
     */
    public ProvisioningClient(SCIMProvider scimProvider, Group group, int httpMethod,
                              Map<String, Object> additionalInformation) {
        this.objectType = SCIMConstants.GROUP_INT;
        provider = scimProvider;
        scimObject = group;
        provisioningMethod = httpMethod;
        additionalProvisioningInformation = additionalInformation;
    }

    /**
     * Provision the SCIM User Object passed to the provisioning client in the constructor, to the
     * SCIM Provider whose details are also sent at the initialization.
     */
    public void provisionCreateUser() throws IdentitySCIMException {
        String userName = null;
        try {
            //encode payload using SCIMClient API.
            SCIMClient scimClient = new SCIMClient();

            //get provider details
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            String encodedUser = scimClient.encodeSCIMObject((AbstractSCIMObject) scimObject,
                    SCIMConstants.identifyFormat(contentType));
            if (logger.isDebugEnabled()) {
                logger.debug("User to provision : useName" + userName);
            }

            PostMethod postMethod = new PostMethod(userEPURL);
            //add basic auth header
            postMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
            //create request entity with the payload.
            RequestEntity requestEntity = new StringRequestEntity(encodedUser,
                    contentType, null);
            postMethod.setRequestEntity(requestEntity);

            //create http client
            HttpClient httpClient = new HttpClient();
            //send the request
            int responseStatus = httpClient.executeMethod(postMethod);

            logger.info("SCIM - create user operation returned with response code: " + responseStatus);

            String response = postMethod.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("Create User Response: " + response);
            }
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //try to decode the scim object to verify that it gets decoded without issue.
                scimClient.decodeSCIMResponse(response, SCIMConstants.identifyFormat(contentType),
                        objectType);
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }

        } catch (CharonException e) {
            throw new IdentitySCIMException(
                    "Error in encoding the object to be provisioned for user with id: " + userName, e);
        } catch (UnsupportedEncodingException e) {
            throw new IdentitySCIMException("Error in creating request for provisioning the user with id: " + userName,
                                            e);
        } catch (IOException | BadRequestException e) {
            throw new IdentitySCIMException(
                    "Error in invoking provisioning operation for the user with id: " + userName, e);
        }
    }

    public void provisionDeleteUser() throws IdentitySCIMException {
        String userName = null;

        try {
            //get provider details
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            /*get the userId of the user being provisioned from the particular provider by filtering
              with user name*/
            GetMethod getMethod = new GetMethod(userEPURL);
            //add filter query parameter
            getMethod.setQueryString(USER_FILTER + ((User) scimObject).getUserName());
            //add authorization headers
            getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));

            //create http client
            HttpClient httpFilterClient = new HttpClient();
            //send the request
            int responseStatus = httpFilterClient.executeMethod(getMethod);

            String response = getMethod.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("SCIM - filter operation inside 'delete user' provisioning " +
                        "returned with response code: " + responseStatus);
                logger.debug("Filter User Response: " + response);
            }
            SCIMClient scimClient = new SCIMClient();
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //decode the response to extract the userId.
                ListedResource listedResource = scimClient.decodeSCIMResponseWithListedResource(
                        response, SCIMConstants.identifyFormat(contentType), objectType);
                List<SCIMObject> users = listedResource.getScimObjects();
                String userId = null;
                //we expect only one user in the list
                for (SCIMObject user : users) {
                    userId = ((User) user).getId();
                }
                String url = userEPURL + "/" + userId;
                //now send the delete request.
                DeleteMethod deleteMethod = new DeleteMethod(url);
                deleteMethod.addRequestHeader(
                        SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
                HttpClient httpDeleteClient = new HttpClient();
                int deleteResponseStatus = httpDeleteClient.executeMethod(deleteMethod);
                String deleteResponse = deleteMethod.getResponseBodyAsString();
                logger.info("SCIM - delete user operation returned with response code: " +
                        deleteResponseStatus);
                if (!scimClient.evaluateResponseStatus(deleteResponseStatus)) {
                    //decode scim exception and extract the specific error message.
                    AbstractCharonException exception =
                            scimClient.decodeSCIMException(
                                    deleteResponse, SCIMConstants.identifyFormat(contentType));
                    logger.error(exception.getDescription());
                }
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(
                                response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }
        } catch (CharonException | IOException | BadRequestException e) {
            throw new IdentitySCIMException("Error in provisioning 'delete user' operation for user :" + userName, e);
        }
    }

    public void provisionUpdateUser() throws IdentitySCIMException {
        String userName = null;
        try {
            //get provider details
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            /*get the userId of the user being provisioned from the particular provider by filtering
              with user name*/
            GetMethod getMethod = new GetMethod(userEPURL);
            //add filter query parameter
            getMethod.setQueryString(USER_FILTER + ((User) scimObject).getUserName());
            //add authorization headers
            getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));

            //create http client
            HttpClient httpFilterClient = new HttpClient();
            //send the request
            int responseStatus = httpFilterClient.executeMethod(getMethod);

            String response = getMethod.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("SCIM - filter operation inside 'delete user' provisioning " +
                        "returned with response code: " + responseStatus);
                logger.debug("Filter User Response: " + response);
            }
            SCIMClient scimClient = new SCIMClient();
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //decode the response to extract the userId.
                ListedResource listedResource = scimClient.decodeSCIMResponseWithListedResource(
                        response, SCIMConstants.identifyFormat(contentType), objectType);
                List<SCIMObject> users = listedResource.getScimObjects();
                String userId = null;
                //we expect only one user in the list
                for (SCIMObject user : users) {
                    userId = ((User) user).getId();
                    if (userId == null) {
                        logger.error("Trying to update a user entry which doesn't support SCIM. " +
                                "Usually internal carbon User entries such as admin role doesn't support SCIM attributes.");
                        return;
                    }
                }
                String url = userEPURL + "/" + userId;
                //now send the update request.
                PutMethod putMethod = new PutMethod(url);
                putMethod.addRequestHeader(
                        SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
                String encodedUser = scimClient.encodeSCIMObject(
                        (AbstractSCIMObject) scimObject, SCIMConstants.identifyFormat(contentType));
                RequestEntity putRequestEntity = new StringRequestEntity(
                        encodedUser, contentType, null);
                putMethod.setRequestEntity(putRequestEntity);

                HttpClient httpUpdateClient = new HttpClient();
                int updateResponseStatus = httpUpdateClient.executeMethod(putMethod);
                String updateResponse = putMethod.getResponseBodyAsString();
                logger.info("SCIM - update user operation returned with response code: " +
                        updateResponseStatus);
                if (!scimClient.evaluateResponseStatus(updateResponseStatus)) {
                    //decode scim exception and extract the specific error message.
                    AbstractCharonException exception =
                            scimClient.decodeSCIMException(
                                    updateResponse, SCIMConstants.identifyFormat(contentType));
                    logger.error(exception.getDescription());
                }
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(
                                response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }
        } catch (CharonException e) {
            throw new IdentitySCIMException("Error in provisioning 'update user' operation for user :" + userName);
        } catch (HttpException e) {
            throw new IdentitySCIMException("Error in provisioning 'update user' operation for user :" + userName);
        } catch (IOException e) {
            throw new IdentitySCIMException("Error in provisioning 'update user' operation for user :" + userName);
        } catch (BadRequestException e) {
            throw new IdentitySCIMException("Error in provisioning 'update user' operation for user :" + userName);
        }

    }

    public void provisionPatchUser() throws IdentitySCIMException {
        String userName = null;
        try {
            //get provider details
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (StringUtils.isEmpty(contentType)) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            /*get the userId of the user being provisioned from the particular provider by filtering
              with user name*/
            GetMethod getMethod = new GetMethod(userEPURL);
            //add filter query parameter
            getMethod.setQueryString(USER_FILTER + ((User) scimObject).getUserName());
            //add authorization headers
            getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));

            //create http client
            HttpClient httpFilterClient = new HttpClient();
            //send the request
            int responseStatus = httpFilterClient.executeMethod(getMethod);

            String response = getMethod.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("SCIM - filter operation inside 'delete user' provisioning " +
                        "returned with response code: " + responseStatus);
                logger.debug("Filter User Response: " + response);
            }
            SCIMClient scimClient = new SCIMClient();
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //decode the response to extract the userId.
                ListedResource listedResource = scimClient.decodeSCIMResponseWithListedResource(
                        response, SCIMConstants.identifyFormat(contentType), objectType);
                List<SCIMObject> users = listedResource.getScimObjects();
                String userId = null;
                //we expect only one user in the list
                for (SCIMObject user : users) {
                    userId = ((User) user).getId();
                    if (StringUtils.isEmpty(userId)) {
                        logger.error("Trying to update a user entry which doesn't support SCIM. " +
                                "Usually internal carbon User entries such as admin role doesn't support SCIM attributes.");
                        return;
                    }
                }
                String url = userEPURL + "/" + userId;
                PostMethod patchMethod = new PostMethod(url) {
                    @Override
                    public String getName() {
                        return "PATCH";
                    }
                };

                patchMethod.addRequestHeader(
                        SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
                String encodedUser = scimClient.encodeSCIMObject(
                        (AbstractSCIMObject) scimObject, SCIMConstants.identifyFormat(contentType));
                RequestEntity putRequestEntity = new StringRequestEntity(
                        encodedUser, contentType, null);
                patchMethod.setRequestEntity(putRequestEntity);

                HttpClient httpUpdateClient = new HttpClient();
                int updateResponseStatus = httpUpdateClient.executeMethod(patchMethod);
                String updateResponse = patchMethod.getResponseBodyAsString();
                logger.info("SCIM - update user operation returned with response code: " +
                        updateResponseStatus);
                if (!scimClient.evaluateResponseStatus(updateResponseStatus)) {
                    //decode scim exception and extract the specific error message.
                    AbstractCharonException exception =
                            scimClient.decodeSCIMException(
                                    updateResponse, SCIMConstants.identifyFormat(contentType));
                    logger.error(exception.getDescription());
                }
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(
                                response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }
        } catch (CharonException | IOException | BadRequestException e) {
            throw new IdentitySCIMException("Error in provisioning 'update user' operation for user :" + userName, e);
        }
    }

    public void provisionCreateGroup() throws IdentitySCIMException {
        String userName = null;
        try {
            //get provider details
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            String groupEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_GROUP_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }
            SCIMClient scimClient = new SCIMClient();
            //get list of users in the group, if any, by userNames
            List<String> users = ((Group) scimObject).getMembersWithDisplayName();

            Group copiedGroup = null;

            if (CollectionUtils.isNotEmpty(users)) {
                //create a deep copy of the group since we are going to update the member ids
                copiedGroup = (Group) CopyUtil.deepCopy(scimObject);
                //delete existing members in the group since we are going to update it with
                copiedGroup.deleteAttribute(SCIMConstants.GroupSchemaConstants.MEMBERS);
                //create http client
                HttpClient httpFilterUserClient = new HttpClient();
                //create get method for filtering
                GetMethod getMethod = new GetMethod(userEPURL);
                getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(
                                userName, password));
                //get corresponding userIds
                for (String user : users) {
                    String filter = USER_FILTER + user;
                    getMethod.setQueryString(filter);
                    int responseCode = httpFilterUserClient.executeMethod(getMethod);
                    String response = getMethod.getResponseBodyAsString();
                    if (logger.isDebugEnabled()) {
                        logger.debug("SCIM - 'filter user' operation inside 'create group' provisioning " +
                                "returned with response code: " + responseCode);
                        logger.debug("Filter User Response: " + response);
                    }
                    //check for success of the response
                    if (scimClient.evaluateResponseStatus(responseCode)) {
                        ListedResource listedUserResource =
                                scimClient.decodeSCIMResponseWithListedResource(
                                        response, SCIMConstants.identifyFormat(contentType),
                                        SCIMConstants.USER_INT);
                        List<SCIMObject> filteredUsers = listedUserResource.getScimObjects();
                        String userId = null;
                        for (SCIMObject filteredUser : filteredUsers) {
                            //we expect only one result here
                            userId = ((User) filteredUser).getId();
                        }
                        copiedGroup.setGroupMember(userId, user);
                    } else {
                        //decode scim exception and extract the specific error message.
                        AbstractCharonException exception =
                                scimClient.decodeSCIMException(
                                        response, SCIMConstants.identifyFormat(contentType));
                        logger.error(exception.getDescription());
                    }
                }
            }

            //provision create group operation
            HttpClient httpCreateGroupClient = new HttpClient();

            PostMethod postMethod = new PostMethod(groupEPURL);
            //add basic auth header
            postMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
            //encode group
            String encodedGroup = null;
            if (copiedGroup != null) {
                encodedGroup = scimClient.encodeSCIMObject(copiedGroup,
                        SCIMConstants.identifyFormat(contentType));
            } else {
                encodedGroup = scimClient.encodeSCIMObject((AbstractSCIMObject) scimObject,
                        SCIMConstants.identifyFormat(contentType));
            }

            //create request entity with the payload.
            RequestEntity requestEntity = new StringRequestEntity(encodedGroup, contentType, null);
            postMethod.setRequestEntity(requestEntity);

            //send the request
            int responseStatus = httpCreateGroupClient.executeMethod(postMethod);

            logger.info("SCIM - create group operation returned with response code: " + responseStatus);

            String postResponse = postMethod.getResponseBodyAsString();

            if (logger.isDebugEnabled()) {
                logger.debug("Create Group Response: " + postResponse);
            }
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //try to decode the scim object to verify that it gets decoded without issue.
                scimClient.decodeSCIMResponse(postResponse, SCIMConstants.JSON,
                        objectType);
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(postResponse, SCIMConstants.JSON);
                logger.error(exception.getDescription());
            }
        } catch (BadRequestException | IOException | CharonException e) {
            throw new IdentitySCIMException("Error in provisioning 'create group' operation for user :" + userName, e);
        }
    }

    public void provisionDeleteGroup() throws IdentitySCIMException {
        String userName = null;
        try {
            //get provider details
            String groupEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_GROUP_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            /*get the groupId of the group being provisioned from the particular provider by filtering
              with display name*/
            GetMethod getMethod = new GetMethod(groupEPURL);
            //add filter query parameter
            getMethod.setQueryString(GROUP_FILTER + ((Group) scimObject).getDisplayName());
            //add authorization headers
            getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));

            //create http client
            HttpClient httpFilterClient = new HttpClient();
            //send the request
            int responseStatus = httpFilterClient.executeMethod(getMethod);
            String response = getMethod.getResponseBodyAsString();

            if (logger.isDebugEnabled()) {
                logger.debug("SCIM - filter operation inside 'delete group' provisioning " +
                        "returned with response code: " + responseStatus);
                logger.debug("Filter Group Response: " + response);
            }
            SCIMClient scimClient = new SCIMClient();
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //decode the response to extract the groupId.
                ListedResource listedResource = scimClient.decodeSCIMResponseWithListedResource(
                        response, SCIMConstants.identifyFormat(contentType), objectType);
                List<SCIMObject> groups = listedResource.getScimObjects();
                String groupId = null;
                //we expect only one user in the list
                for (SCIMObject group : groups) {
                    groupId = ((Group) group).getId();
                }
                String url = groupEPURL + "/" + groupId;
                //now send the delete request.
                DeleteMethod deleteMethod = new DeleteMethod(url);
                deleteMethod.addRequestHeader(
                        SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
                HttpClient httpDeleteClient = new HttpClient();
                int deleteResponseStatus = httpDeleteClient.executeMethod(deleteMethod);
                String deleteResponse = deleteMethod.getResponseBodyAsString();
                logger.info("SCIM - delete group operation returned with response code: " +
                        deleteResponseStatus);
                if (!scimClient.evaluateResponseStatus(deleteResponseStatus)) {
                    //decode scim exception and extract the specific error message.
                    AbstractCharonException exception =
                            scimClient.decodeSCIMException(
                                    deleteResponse, SCIMConstants.identifyFormat(contentType));
                    logger.error(exception.getDescription());
                }
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(
                                response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }
        } catch (CharonException | IOException | BadRequestException e) {
            throw new IdentitySCIMException("Error in provisioning 'delete group' operation for user :" + userName, e);
        }
    }

    public void provisionUpdateGroup() throws IdentitySCIMException {
        String userName = null;
        try {
            //get provider details
            String groupEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_GROUP_ENDPOINT);
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            /*get the groupId of the group being provisioned from the particular provider by filtering
              with display name*/
            GetMethod getMethod = new GetMethod(groupEPURL);
            //add filter query parameter
            //check if role name is updated
            if (additionalProvisioningInformation != null && (Boolean) additionalProvisioningInformation.get(
                    SCIMCommonConstants.IS_ROLE_NAME_CHANGED_ON_UPDATE)) {
                getMethod.setQueryString(
                        GROUP_FILTER + additionalProvisioningInformation.get(SCIMCommonConstants.OLD_GROUP_NAME));
            } else {
                getMethod.setQueryString(GROUP_FILTER + ((Group) scimObject).getDisplayName());
            }
            //add authorization headers
            getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                    BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));

            //create http client
            HttpClient httpFilterClient = new HttpClient();
            //send the request
            int responseStatus = httpFilterClient.executeMethod(getMethod);

            String response = getMethod.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("SCIM - filter operation inside 'update group' provisioning " +
                        "returned with response code: " + responseStatus);
                logger.debug("Filter Group Response: " + response);
            }
            SCIMClient scimClient = new SCIMClient();
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //decode the response to extract the groupId.
                ListedResource listedResource = scimClient.decodeSCIMResponseWithListedResource(
                        response, SCIMConstants.identifyFormat(contentType), objectType);
                List<SCIMObject> groups = listedResource.getScimObjects();
                String groupId = null;
                //we expect only one user in the list
                for (SCIMObject group : groups) {
                    groupId = ((Group) group).getId();
                }

                String url = groupEPURL + "/" + groupId;

                //now start sending the update request.

                //get list of users in the group, if any, by userNames
                List<String> users = ((Group) scimObject).getMembersWithDisplayName();

                Group copiedGroup = null;

                if (CollectionUtils.isNotEmpty(users)) {
                    //create a deep copy of the group since we are going to update the member ids
                    copiedGroup = (Group) CopyUtil.deepCopy(scimObject);
                    //delete existing members in the group since we are going to update it with
                    copiedGroup.deleteAttribute(SCIMConstants.GroupSchemaConstants.MEMBERS);
                    //create http client
                    HttpClient httpFilterUserClient = new HttpClient();
                    //create get method for filtering
                    GetMethod getUserMethod = new GetMethod(userEPURL);
                    getUserMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                            BasicAuthUtil.getBase64EncodedBasicAuthHeader(
                                    userName, password));
                    //get corresponding userIds
                    for (String user : users) {
                        String filter = USER_FILTER + user;
                        getUserMethod.setQueryString(filter);
                        int responseCode = httpFilterUserClient.executeMethod(getUserMethod);
                        String filterUserResponse = getUserMethod.getResponseBodyAsString();
                        if (logger.isDebugEnabled()) {
                            logger.debug("SCIM - 'filter user' operation inside 'update group' provisioning " +
                                    "returned with response code: " + responseCode);
                            logger.debug("Filter User Response: " + filterUserResponse);
                        }
                        //check for success of the response
                        if (scimClient.evaluateResponseStatus(responseCode)) {
                            ListedResource listedUserResource =
                                    scimClient.decodeSCIMResponseWithListedResource(
                                            filterUserResponse, SCIMConstants.identifyFormat(contentType),
                                            SCIMConstants.USER_INT);
                            List<SCIMObject> filteredUsers = listedUserResource.getScimObjects();
                            String userId = null;
                            for (SCIMObject filteredUser : filteredUsers) {
                                //we expect only one result here
                                userId = ((User) filteredUser).getId();
                            }
                            copiedGroup.setGroupMember(userId, user);
                        } else {
                            //decode scim exception and extract the specific error message.
                            AbstractCharonException exception =
                                    scimClient.decodeSCIMException(
                                            filterUserResponse, SCIMConstants.identifyFormat(contentType));
                            logger.error(exception.getDescription());
                        }
                    }
                }

                //now send the update request.
                PutMethod putMethod = new PutMethod(url);
                putMethod.addRequestHeader(
                        SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
                String encodedGroup = scimClient.encodeSCIMObject(
                        (AbstractSCIMObject) scimObject, SCIMConstants.identifyFormat(contentType));
                RequestEntity putRequestEntity = new StringRequestEntity(
                        encodedGroup, contentType, null);
                putMethod.setRequestEntity(putRequestEntity);

                HttpClient httpUpdateClient = new HttpClient();
                int updateResponseStatus = httpUpdateClient.executeMethod(putMethod);
                String updateResponse = putMethod.getResponseBodyAsString();

                logger.info("SCIM - update group operation returned with response code: " +
                        updateResponseStatus);
                if (!scimClient.evaluateResponseStatus(updateResponseStatus)) {
                    //decode scim exception and extract the specific error message.
                    AbstractCharonException exception = scimClient.decodeSCIMException(
                            updateResponse, SCIMConstants.identifyFormat(contentType));
                    logger.error(exception.getDescription());
                }
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(
                                response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }
        } catch (CharonException | IOException | BadRequestException e) {
            throw new IdentitySCIMException("Error in provisioning 'delete group' operation for user :" + userName, e);
        }
    }

    public void provisionPatchGroup() throws IdentitySCIMException {
        String userName = null;
        try {
            //get provider details
            String groupEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_GROUP_ENDPOINT);
            String userEPURL = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USER_ENDPOINT);
            userName = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_USERNAME);
            String password = provider.getProperty(SCIMConfigConstants.ELEMENT_NAME_PASSWORD);
            String contentType = provider.getProperty(SCIMConstants.CONTENT_TYPE_HEADER);

            if (contentType == null) {
                contentType = SCIMConstants.APPLICATION_JSON;
            }

            /*get the groupId of the group being provisioned from the particular provider by filtering
              with display name*/
            GetMethod getMethod = new GetMethod(groupEPURL);
            //add filter query parameter
            //check if role name is updated
            if (additionalProvisioningInformation != null && (Boolean) additionalProvisioningInformation.get(
                    SCIMCommonConstants.IS_ROLE_NAME_CHANGED_ON_UPDATE)) {
                getMethod.setQueryString(
                        GROUP_FILTER + additionalProvisioningInformation.get(SCIMCommonConstants.OLD_GROUP_NAME));
            } else {
                getMethod.setQueryString(GROUP_FILTER + ((Group) scimObject).getDisplayName());
            }
            //add authorization headers
            getMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                                       BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));

            //create http client
            HttpClient httpFilterClient = new HttpClient();
            //send the request
            int responseStatus = httpFilterClient.executeMethod(getMethod);

            String response = getMethod.getResponseBodyAsString();
            if (logger.isDebugEnabled()) {
                logger.debug("SCIM - filter operation inside 'update group' provisioning " +
                             "returned with response code: " + responseStatus);
                logger.debug("Filter Group Response: " + response);
            }
            SCIMClient scimClient = new SCIMClient();
            if (scimClient.evaluateResponseStatus(responseStatus)) {
                //decode the response to extract the groupId.
                ListedResource listedResource = scimClient.decodeSCIMResponseWithListedResource(
                        response, SCIMConstants.identifyFormat(contentType), objectType);
                List<SCIMObject> groups = listedResource.getScimObjects();
                String groupId = null;
                //we expect only one user in the list
                for (SCIMObject group : groups) {
                    groupId = ((Group) group).getId();
                }

                String url = groupEPURL + "/" + groupId;

                //now start sending the update request.

                //get list of users in the group, if any, by userNames
                List<String> users = ((Group) scimObject).getMembersWithDisplayName();

                Group copiedGroup = null;

                if (CollectionUtils.isNotEmpty(users)) {
                    //create a deep copy of the group since we are going to update the member ids
                    copiedGroup = (Group) CopyUtil.deepCopy(scimObject);
                    //delete existing members in the group since we are going to update it with
                    copiedGroup.deleteAttribute(SCIMConstants.GroupSchemaConstants.MEMBERS);
                    //create http client
                    HttpClient httpFilterUserClient = new HttpClient();
                    //create get method for filtering
                    GetMethod getUserMethod = new GetMethod(userEPURL);
                    getUserMethod.addRequestHeader(SCIMConstants.AUTHORIZATION_HEADER,
                                                   BasicAuthUtil.getBase64EncodedBasicAuthHeader(
                                                           userName, password));
                    //get corresponding userIds
                    for (String user : users) {
                        String filter = USER_FILTER + user;
                        getUserMethod.setQueryString(filter);
                        int responseCode = httpFilterUserClient.executeMethod(getUserMethod);
                        String filterUserResponse = getUserMethod.getResponseBodyAsString();
                        if (logger.isDebugEnabled()) {
                            logger.debug("SCIM - 'filter user' operation inside 'update group' provisioning " +
                                         "returned with response code: " + responseCode);
                            logger.debug("Filter User Response: " + filterUserResponse);
                        }
                        //check for success of the response
                        if (scimClient.evaluateResponseStatus(responseCode)) {
                            ListedResource listedUserResource =
                                    scimClient.decodeSCIMResponseWithListedResource(
                                            filterUserResponse, SCIMConstants.identifyFormat(contentType),
                                            SCIMConstants.USER_INT);
                            List<SCIMObject> filteredUsers = listedUserResource.getScimObjects();
                            String userId = null;
                            for (SCIMObject filteredUser : filteredUsers) {
                                //we expect only one result here
                                userId = ((User) filteredUser).getId();
                            }
                            copiedGroup.setGroupMember(userId, user);
                        } else {
                            //decode scim exception and extract the specific error message.
                            AbstractCharonException exception =
                                    scimClient.decodeSCIMException(
                                            filterUserResponse, SCIMConstants.identifyFormat(contentType));
                            logger.error(exception.getDescription());
                        }
                    }
                }

                //now send the update request.
                PostMethod patchMethod = new PostMethod(url){
                    @Override
                    public String getName() {
                        return "PATCH";
                    }
                };
                patchMethod.addRequestHeader(
                        SCIMConstants.AUTHORIZATION_HEADER,
                        BasicAuthUtil.getBase64EncodedBasicAuthHeader(userName, password));
                String encodedGroup = scimClient.encodeSCIMObject(
                        (AbstractSCIMObject) scimObject, SCIMConstants.identifyFormat(contentType));
                RequestEntity putRequestEntity = new StringRequestEntity(
                        encodedGroup, contentType, null);
                patchMethod.setRequestEntity(putRequestEntity);

                HttpClient httpUpdateClient = new HttpClient();
                int updateResponseStatus = httpUpdateClient.executeMethod(patchMethod);
                String updateResponse = patchMethod.getResponseBodyAsString();

                logger.info("SCIM - update group operation returned with response code: " +
                            updateResponseStatus);
                if (!scimClient.evaluateResponseStatus(updateResponseStatus)) {
                    //decode scim exception and extract the specific error message.
                    AbstractCharonException exception = scimClient.decodeSCIMException(
                            updateResponse, SCIMConstants.identifyFormat(contentType));
                    logger.error(exception.getDescription());
                }
            } else {
                //decode scim exception and extract the specific error message.
                AbstractCharonException exception =
                        scimClient.decodeSCIMException(
                                response, SCIMConstants.identifyFormat(contentType));
                logger.error(exception.getDescription());
            }
        } catch (CharonException | IOException | BadRequestException e) {
            throw new IdentitySCIMException("Error in provisioning 'delete group' operation for user : " + userName, e);
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            if (SCIMConstants.USER_INT == objectType) {
                switch (provisioningMethod) {
                    case SCIMConstants.DELETE:
                        provisionDeleteUser();
                        break;
                    case SCIMConstants.POST:
                        provisionCreateUser();
                        break;
                    case SCIMConstants.PUT:
                        provisionUpdateUser();
                        break;
                    default:
                        break;
                }
            } else if (SCIMConstants.GROUP_INT == objectType) {
                switch (provisioningMethod) {
                    case SCIMConstants.DELETE:
                        provisionDeleteGroup();
                        break;
                    case SCIMConstants.POST:
                        provisionCreateGroup();
                        break;
                    case SCIMConstants.PUT:
                        provisionUpdateGroup();
                        break;
                    default:
                        break;
                }
            }
        } catch (IdentitySCIMException e) {
            logger.error("Error occurred while user provisioning", e);
        }
    }
}
