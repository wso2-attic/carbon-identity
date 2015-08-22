/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.uma;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.uma.dao.ResourceSetMgtDAO;
import org.wso2.carbon.identity.uma.dto.*;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;
import org.wso2.carbon.identity.uma.model.ResourceSetDO;
import org.wso2.carbon.identity.uma.dto.UmaOAuthIntropectResponse;
import org.wso2.carbon.identity.uma.util.UMAUtil;

import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class UMAService {

    private static final Log log = LogFactory.getLog(UMAService.class);

    // method to issue the RPT after validating the claims
    public UmaRptResponse issueRPT(UmaRptRequest rptRequest){

        if (log.isDebugEnabled()){
            log.debug("Request Processed by the UMAService");
        }

        // validate the permission ticket


        // retrieve the permission sets associated with the ticket ( resourceID, scopes, user consent type)


        // create the message context fill the UMARptRequest with



        // select the handlers to handle the user consent type
        // if more than one select one with higher priority



        // get the response from the handlers and send to back
        UmaRptResponse umaRptResponse = new UmaRptResponse(HttpServletResponse.SC_OK);
        umaRptResponse.setRPT("kdjfsdfhdfshjgsasdisdfyuwey83475y43undf4387437");

        return umaRptResponse;
    }


    public UmaResponse createResourceSet(UmaResourceSetRegRequest umaResourceSetRegRequest) throws IdentityUMAException {

        UmaResponse.UmaResponseBuilder builder;

        // generate a unique string
        String resourceSetID = UUID.randomUUID().toString().replace("-","");

        // create the DO using the bean from the request
        ResourceSetDO resourceSetDO = new ResourceSetDO(umaResourceSetRegRequest.getResourceSetDescriptionBean());

        resourceSetDO.setResourceSetId(resourceSetID);
        resourceSetDO.setTimeCreated(new Timestamp(new Date().getTime()));

        String accessToken =  umaResourceSetRegRequest.getAccessToken();

        // we need to set the access token ID of the access token used to create the resource set
        // so that we can associate it with the user who authorized the access token
        String accessTokenId = null;
        try {
            accessTokenId = getAccessTokenId(accessToken,false);
        } catch (IdentityOAuth2Exception e) {
            log.error("Unable to retrieve the access token ID for the access token identifier : "+accessToken);
            throw new IdentityUMAException("Unable to retrieve the token ID for the access token used to create resource");
        }

        resourceSetDO.setTokenId(accessTokenId);
        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        try {
            resourceSetMgtDAO.saveResourceSetDescription(resourceSetDO,null);

            builder = UmaResourceSetRegResponse.status(HttpServletResponse.SC_CREATED)
                            .setParam(UMAProtectionConstants.RESOURCE_SET_ID,
                                    resourceSetDO.getResourceSetId());

            if (log.isDebugEnabled()){
                log.debug("ResourceSet Successfully registered with ID : "+resourceSetID);
            }

        } catch (IdentityUMAException e) {
            log.error(e.getMessage(),e);

            IdentityUMAException identityUMAException = new IdentityUMAException("invalid_request",e);
            identityUMAException.setErrorStatus(HttpServletResponse.SC_BAD_REQUEST);
            identityUMAException.setErrorCode("invalid_request");

            throw identityUMAException;
        }


        return builder.buildJSONResponse();

    }


    /**
     *
     * @param umaResourceSetRegRequest
     * @return
     */
    public UmaResponse getResourceSetIds(UmaResourceSetRegRequest umaResourceSetRegRequest) throws IdentityUMAException {

        UmaResourceSetRegResponse.UmaResourceSetRegRespBuilder builder;


        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();
        String userStoreDomain = null;
        String accessToken = umaResourceSetRegRequest.getAccessToken();

        try {
            String accessTokenId = getAccessTokenId(accessToken,false);

            List<String> ids = resourceSetMgtDAO.retrieveResourceSetIDs(accessTokenId, userStoreDomain);

            if (log.isDebugEnabled()){
                log.debug("ResourceSet IDs retrieved successfully using tokenID: "+accessTokenId);
            }

            builder = UmaResourceSetRegResponse.status(HttpServletResponse.SC_OK);
            // set the resource set id list
            builder.setResourceSetIds(ids);

        } catch (IdentityUMAException e) {
            log.error("Error when retrieving registered resource sets for user : ", e);
            IdentityUMAException identityUMAException =
                    new IdentityUMAException("Error when retrieving registered resource sets for user : ");

            identityUMAException.setErrorStatus(HttpServletResponse.SC_BAD_REQUEST);
            identityUMAException.setErrorDetails("Unable to retrieve resource sets");
            throw identityUMAException;

        } catch (IdentityOAuth2Exception e) {
            log.error("Unable to retrieve the access token ID for the access token identifier : " + accessToken);

            IdentityUMAException identityUMAException =
                    new IdentityUMAException("Unable to retrieve the token ID for access token used to create resource");
            identityUMAException.setErrorStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            identityUMAException.setErrorDetails("Unable to retrieve resource sets");

            throw identityUMAException;
        }

        return builder.buildJSONResponse();
    }

    /**
     *
     * @param umaResourceSetRegRequest
     * @return
     */
    public UmaResponse getResourceSet(UmaResourceSetRegRequest umaResourceSetRegRequest) throws IdentityUMAException {
        UmaResponse.UmaResponseBuilder builder;

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        String resourceSetId = umaResourceSetRegRequest.getResourceId();
        String userStoreDomain = null;

        try {
            ResourceSetDO resourceSetDO =
                    resourceSetMgtDAO.retrieveResourceSet(resourceSetId, userStoreDomain);

            if (resourceSetDO == null){
                if (log.isDebugEnabled()){
                    log.debug("No ResourceSet found for resourceSetId: "+resourceSetId);
                }
                builder = UmaResponse.errorResponse(HttpServletResponse.SC_NOT_FOUND)
                          .setError(UMAProtectionConstants.ERR_RESOURCE_SET_NOT_FOUND);
            }else{
                builder = UmaResponse.status(HttpServletResponse.SC_OK)
                          .setParam(UMAProtectionConstants.RESOURCE_SET_ID, resourceSetDO.getResourceSetId())
                          .setParam(UMAProtectionConstants.RESOURCE_SET_NAME, resourceSetDO.getName())
                          .setParam(UMAProtectionConstants.RESOURCE_SET_URI, resourceSetDO.getURI())
                          .setParam(UMAProtectionConstants.RESOURCE_SET_TYPE, resourceSetDO.getType())
                          .setParam(UMAProtectionConstants.RESOURCE_SET_SCOPES, resourceSetDO.getScopes())
                          .setParam(UMAProtectionConstants.RESOURCE_SET_ICON_URI, resourceSetDO.getIconURI());
            }

        } catch (IdentityUMAException e) {
            String errorMsg =  "Error when retrieving resource set description for resourceSetId : "+resourceSetId
                   +" for consumerKey : <>";
            e.setErrorStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            throw e;
        }

        UmaResponse response = builder.buildJSONResponse();
        if (log.isDebugEnabled()){
            log.debug("ResourceSet found for resourceSetId: "+resourceSetId+"\n"+response.getBody());
        }
        return response;
    }


    public UmaResponse deleteResourceSet(UmaResourceSetRegRequest umaResourceSetRegRequest) throws IdentityUMAException {
        UmaResponse.UmaResponseBuilder builder;

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        String resourceSetId = umaResourceSetRegRequest.getResourceId();
        String userStoreDomain = null;

        try {
            boolean isSuccessFul =resourceSetMgtDAO.removeResourceSet(resourceSetId,userStoreDomain);

            if (isSuccessFul){
                builder = UmaResponse.status(HttpServletResponse.SC_NO_CONTENT);

                if (log.isDebugEnabled()){
                    log.debug("Resource with ID: "+resourceSetId+" deleted successfully");
                }
            }else{
                builder = UmaResponse.errorResponse(HttpServletResponse.SC_NOT_FOUND)
                                     .setError(UMAProtectionConstants.ERR_RESOURCE_SET_NOT_FOUND);

                if (log.isDebugEnabled()){
                    log.debug("Unable to find resource set with ID: "+resourceSetId+" to delete");
                }
            }

        } catch (IdentityUMAException e) {
            String errorMsg =  "Error when deleting resource set description with resourceSetId : "+resourceSetId;

            log.error(errorMsg,e);
            throw new IdentityUMAException(errorMsg,e);

        }

        return builder.buildJSONResponse();
    }


//    public UmaResponse updateResourceSet(UmaResourceSetRegRequest umaResourceSetRegRequest){
//        UmaResponse.UmaResponseBuilder builder;
//
//        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();
//
//        String resourceSetId = umaResourceSetRegRequest.getResourceId();
//        String consumerKey = umaResourceSetRegRequest.getConsumerKey();
//        String userStoreDomain = null;
//
//
//        ResourceSetDO newResourceSetDO  =
//                new ResourceSetDO(
//                        umaResourceSetRegRequest.getResourceSetDescriptionBean()
//                );
//
//
//        try {
//            boolean isSuccessFul =
//                    resourceSetMgtDAO.updateResourceSet(resourceSetId, newResourceSetDO, consumerKey, userStoreDomain);
//
//            if (isSuccessFul){
//                builder = UmaResponse.status(HttpServletResponse.SC_NO_CONTENT)
//                            .setParam(UMAProtectionConstants.RESOURCE_SET_ID, resourceSetId);
//            }else{
//                // we could not find the resource to update, hence this error message
//                builder = UmaResponse.errorResponse(HttpServletResponse.SC_NOT_FOUND)
//                        .setError(UMAProtectionConstants.ERR_RESOURCE_SET_NOT_FOUND);
//            }
//
//        } catch (IdentityUMAException e) {
//            String errorMsg =  "Error when deleting resource set description with resourceSetId : "+resourceSetId
//                    +" for consumerKey : "+consumerKey;
//
//            log.error(errorMsg,e);
//            builder =
//                    UmaResourceSetRegResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        }
//
//        return builder.buildJSONResponse();
//    }
//

    public UmaResponse createPermissionTicket(UmaPermissionSetRegRequest umaPermissionSetRegRequest){

        // check whether the resource exists

        // check whether requested scope is available for the resource

        // generate the permission ticket string

        // persist the permission ticket

        // return the response
        return null;
    }


    public UmaResponse introspectToken(UmaRequest umaOAuthIntrospectRequest) throws IdentityUMAException {

        UmaOAuthIntropectResponse.UmaOAuthIntrospectRespBuilder builder =
                        UmaOAuthIntropectResponse.status(HttpServletResponse.SC_OK);

        // retrieve the token identifier sent in the request
        String tokenIdentifier =
                umaOAuthIntrospectRequest.getHttpServletRequest().getParameter(UMAConstants.OAuthIntrospectConstants.TOKEN);

        // if the token identifier is not available or empty throw an exception
        if (tokenIdentifier == null || StringUtils.isEmpty(tokenIdentifier)){
            IdentityUMAException identityUMAException =
                    new IdentityUMAException("Token Identifier not found in the request");
            identityUMAException.setErrorStatus(HttpServletResponse.SC_BAD_REQUEST);

            throw identityUMAException;
        }

        // Retrieve the token type hint if one was provided by the client
        String tokenTypeHint =
                umaOAuthIntrospectRequest.getHttpServletRequest().getParameter(UMAConstants.OAuthIntrospectConstants.TOKEN_TYPE_HINT);


        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

        try {
            // retrieve the active access token
            AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveAccessToken(tokenIdentifier, false);

            // if the token is not found that means it's either inactive or revoked
            if (accessTokenDO == null){
                // since the access token is not in an active state, send a response with active property equal to
                // false as said in the spec
                if (log.isDebugEnabled()){
                    log.debug("Access Token with identifier: "+tokenIdentifier+" not found or not in active state ");
                }

                builder.setActive(false);
                return builder.buildJSONResponse();
            }

            // the token is active so we fill in the details from the token
            builder.setActive(true);

            if (accessTokenDO.getScope() != null){
                // set the scopes of the token if available
                builder.setScope(UMAUtil.buildScopeString(accessTokenDO.getScope()));
            }

            if (accessTokenDO.getConsumerKey() != null){
                builder.setClientId(accessTokenDO.getConsumerKey());
            }
            if (accessTokenDO.getAuthzUser() != null){
                String userName = accessTokenDO.getAuthzUser().getUserName();
                if (userName != null){
                    builder.setUserId(userName);
                }
            }
            if (accessTokenDO.getTokenType() != null){
                builder.setTokenType(accessTokenDO.getTokenType());
            }

            if (accessTokenDO.getIssuedTime() != null){
                long issuedTimeInSeconds = accessTokenDO.getIssuedTime().getTime()/ 1000;
                builder.setIssuedAt(issuedTimeInSeconds);

                if (accessTokenDO.getValidityPeriod() != 0){
                    long validityPeriodInSeconds = accessTokenDO.getValidityPeriod();
                    long expiryTimeInSeconds = issuedTimeInSeconds + validityPeriodInSeconds;

                    builder.setExpiraryTime(expiryTimeInSeconds);
                }
            }

            // set the string identifier of the token
            if (accessTokenDO.getTokenId() != null){
                builder.setTokenId(accessTokenDO.getAccessToken());
            }

            UmaResponse response = builder.buildJSONResponse();

            if (log.isDebugEnabled()){
                log.debug("Token with identifier: "+tokenIdentifier+" introspected successfully\n"+response.getBody());
            }

            // build the response
            return response;

        } catch (IdentityOAuth2Exception e) {
            throw new IdentityUMAException(e.getMessage(),e);
        }

    }


    /**
     *  Util method to get the AccessTokenId from an accessToken identifier String
     * @param tokenIdentifier Access Token string
     * @param includeExpired boolean flag to decide whether to include expired tokens in the the search
     * @return
     */
    private String getAccessTokenId(String tokenIdentifier, boolean includeExpired) throws IdentityOAuth2Exception {
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

        AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveAccessToken(tokenIdentifier,false);
        return accessTokenDO != null ? accessTokenDO.getTokenId() : null;
    }

}
