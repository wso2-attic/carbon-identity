/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * /
 */

package org.wso2.carbon.identity.uma;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.uma.dao.ResourceSetMgtDAO;
import org.wso2.carbon.identity.uma.dto.*;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;
import org.wso2.carbon.identity.uma.model.ResourceSetDO;
import org.wso2.carbon.identity.uma.util.OAuthIntrospectConstants;
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


    public UmaResponse createResourceSet
            (UmaResourceSetRegistrationRequest umaResourceSetRegistrationRequest){

        UmaResponse.UmaResponseBuilder builder;

        // generate a unique string
        String resourceSetID = UUID.randomUUID().toString().replace("-","");

        ResourceSetDO resourceSetDO = new ResourceSetDO(
                umaResourceSetRegistrationRequest.getResourceSetDescriptionBean()
        );

        resourceSetDO.setConsumerKey(umaResourceSetRegistrationRequest.getConsumerKey());
        resourceSetDO.setResourceSetId(resourceSetID);
        resourceSetDO.setCreatedTime(new Timestamp(new Date().getTime()));

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        try {
            resourceSetMgtDAO.saveResourceSetDescription(resourceSetDO,null);

             builder = UmaResourceSetRegistrationResponse.status(HttpServletResponse.SC_CREATED)
                            .setParam(UMAConstants.OAuthResourceRegistration.RESOURCE_SET_ID,
                                    resourceSetDO.getResourceSetId());

        } catch (IdentityUMAException e) {
           log.error(e.getMessage(),e);

            builder =  UmaResourceSetRegistrationResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError("invalid_request");
        }


        return builder.buildJSONResponse();

    }


    /**
     *
     * @param umaResourceSetRegRequest
     * @return
     */
    public UmaResponse getResoucreSetIds(UmaResourceSetRegistrationRequest umaResourceSetRegRequest){

        UmaResponse.UmaResponseBuilder builder;

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();
        String consumerKey = umaResourceSetRegRequest.getConsumerKey();
        String userStoreDomain = null;

        try {
            List<String> ids = resourceSetMgtDAO.retrieveResourceSetIDs(consumerKey, userStoreDomain);
            builder = UmaResourceSetRegistrationResponse.status(HttpServletResponse.SC_OK);

            // set the resource set id list
            ((UmaResourceSetRegistrationResponse.UmaResourceSetRegRespBuilder)builder).setResourceSetIds(ids);


        } catch (IdentityUMAException e) {
            log.error("Error when retrieving registered resource sets for consumerKey : "+consumerKey,e);

            builder =
                    UmaResourceSetRegistrationResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return builder.buildJSONResponse();
    }

    /**
     *
     * @param umaResourceSetRegRequest
     * @return
     */
    public UmaResponse getResourceSet(UmaResourceSetRegistrationRequest umaResourceSetRegRequest){
        UmaResponse.UmaResponseBuilder builder;

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        String resourceSetId = umaResourceSetRegRequest.getResourceId();
        String consumerKey = umaResourceSetRegRequest.getConsumerKey();
        String userStoreDomain = null;

        try {
            ResourceSetDO resourceSetDO =
                    resourceSetMgtDAO.retrieveResourceSet(resourceSetId, consumerKey, userStoreDomain);

            if (resourceSetDO == null){
                builder = UmaResponse.errorResponse(HttpServletResponse.SC_NOT_FOUND)
                          .setError(UMAProtectionConstants.ERROR_RESOURCE_SET_NOT_FOUND);
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
                   +" for consumerKey : "+consumerKey;

            log.error(errorMsg);

            builder =
                    UmaResourceSetRegistrationResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }


        return builder.buildJSONResponse();
    }


    public UmaResponse deleteResourceSet(UmaResourceSetRegistrationRequest umaResourceSetRegRequest){
        UmaResponse.UmaResponseBuilder builder;

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        String resourceSetId = umaResourceSetRegRequest.getResourceId();
        String consumerKey = umaResourceSetRegRequest.getConsumerKey();
        String userStoreDomain = null;

        try {
            boolean isSuccessFul =resourceSetMgtDAO.removeResourceSet(resourceSetId, consumerKey, userStoreDomain);

            if (isSuccessFul){
                builder = UmaResponse.status(HttpServletResponse.SC_NO_CONTENT);
            }else{
                builder = UmaResponse.errorResponse(HttpServletResponse.SC_NOT_FOUND)
                                     .setError(UMAProtectionConstants.ERROR_RESOURCE_SET_NOT_FOUND);
            }

        } catch (IdentityUMAException e) {
            String errorMsg =  "Error when deleting resource set description with resourceSetId : "+resourceSetId
                    +" for consumerKey : "+consumerKey;

            log.error(errorMsg,e);
            builder =
                    UmaResourceSetRegistrationResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return builder.buildJSONResponse();
    }


    public UmaResponse updateResourceSet(UmaResourceSetRegistrationRequest umaResourceSetRegRequest){
        UmaResponse.UmaResponseBuilder builder;

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        String resourceSetId = umaResourceSetRegRequest.getResourceId();
        String consumerKey = umaResourceSetRegRequest.getConsumerKey();
        String userStoreDomain = null;


        ResourceSetDO newResourceSetDO  =
                new ResourceSetDO(
                        umaResourceSetRegRequest.getResourceSetDescriptionBean()
                );


        try {
            boolean isSuccessFul =
                    resourceSetMgtDAO.updateResourceSet(resourceSetId, newResourceSetDO, consumerKey, userStoreDomain);

            if (isSuccessFul){
                builder = UmaResponse.status(HttpServletResponse.SC_NO_CONTENT)
                            .setParam(UMAProtectionConstants.RESOURCE_SET_ID, resourceSetId);
            }else{
                // we could not find the resource to update, hence this error message
                builder = UmaResponse.errorResponse(HttpServletResponse.SC_NOT_FOUND)
                        .setError(UMAProtectionConstants.ERROR_RESOURCE_SET_NOT_FOUND);
            }

        } catch (IdentityUMAException e) {
            String errorMsg =  "Error when deleting resource set description with resourceSetId : "+resourceSetId
                    +" for consumerKey : "+consumerKey;

            log.error(errorMsg,e);
            builder =
                    UmaResourceSetRegistrationResponse.errorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return builder.buildJSONResponse();
    }

    public UmaResponse introspectToken(UmaRequest umaOAuthIntrospectRequest) throws IdentityUMAException {

        UmaOAuthIntropectResponse.UmaOAuthIntrospectRespBuilder builder =
                (UmaOAuthIntropectResponse.UmaOAuthIntrospectRespBuilder)
                        UmaOAuthIntropectResponse.status(HttpServletResponse.SC_OK);

        // retrieve the token identifier sent in the request
        String tokenIdentifier =
                umaOAuthIntrospectRequest.getHttpServletRequest().getParameter(OAuthIntrospectConstants.TOKEN);

        // if the token identifier is not available or empty throw an exception
        if (tokenIdentifier == null || StringUtils.isEmpty(tokenIdentifier)){
            IdentityUMAException identityUMAException =
                    new IdentityUMAException("Token Identifier not found in the request");
            identityUMAException.setErrorStatus(HttpServletResponse.SC_BAD_REQUEST);

            throw identityUMAException;
        }

        // Retrieve the token type hint if one was provided by the client
        String tokenTypeHint =
                umaOAuthIntrospectRequest.getHttpServletRequest().getParameter(OAuthIntrospectConstants.TOKEN_TYPE_HINT);


        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();

        try {
            AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveAccessToken(tokenIdentifier, true);

            // if the token is not found or if the token is not active
            // we send a response with the active state set to false;
            if (accessTokenDO == null ||
                    !StringUtils.equals(accessTokenDO.getTokenState(), OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE)){

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
                builder.setClientId(accessTokenDO.getAccessToken());
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
                builder.setTokenId(accessTokenDO.getTokenId());
            }

            // build the response
            return builder.buildJSONResponse();

        } catch (IdentityOAuth2Exception e) {
            throw new IdentityUMAException(e.getMessage(),e);
        }

    }

}
