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

package org.wso2.carbon.identity.uma.endpoint.authorization;

import org.apache.amber.oauth2.as.response.OAuthASResponse;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.uma.beans.authz.UmaRptRequestPayloadBean;
import org.wso2.carbon.identity.uma.dto.UmaRptRequest;
import org.wso2.carbon.identity.uma.dto.UmaRptResponse;
import org.wso2.carbon.identity.uma.endpoint.util.EndpointUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Enumeration;

@Path("/rpt")
public class UmaAuthorizationEndpoint {

    private static final Log log = LogFactory.getLog(UmaAuthorizationEndpoint.class);

    private static final String OAUTH_TOKEN_VALIDATION_RESPONSE = "oauth.access.token.validation.response";

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public Response issueRPT(@Context HttpServletRequest request,UmaRptRequestPayloadBean payloadBean) throws OAuthSystemException {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            // log the RPT Token Request
            if (log.isDebugEnabled()) {
                logRptRequest(request, payloadBean);
            }

            // if the access token validation is not present either the
            // token validation valve is not engaged or their was some error in validating
            if (request.getAttribute(OAUTH_TOKEN_VALIDATION_RESPONSE) == null){
                return handleOAuthAccessTokenValidationError();
            }

            // retrieve the token validation response
            OAuth2ClientApplicationDTO applicationDTO =
                        (OAuth2ClientApplicationDTO)request.getAttribute(OAUTH_TOKEN_VALIDATION_RESPONSE);

            OAuth2TokenValidationResponseDTO tokenValidationResponseDTO =
                        applicationDTO.getAccessTokenValidationResponse();

            // check whether the token is valid
            if (!tokenValidationResponseDTO.isValid()){
            // since token is not valid send an OAuth error
               return handleInvalidAccessToken(tokenValidationResponseDTO.getErrorMsg());
            }

            // create the UmaRptRequest
            try{

                // create UMA RPT Request object
                UmaRptRequest rptRequest = new UmaRptRequest(request,payloadBean);

                // get the RPT Response
                UmaRptResponse umaRptResponse = getRequestPartyToken(rptRequest);

                // TODO process based on the response


            }catch(Exception ex){


                // TODO handle exceptions and send error messages
            }


            return Response.status(HttpServletResponse.SC_OK).build();
        }finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }


    private Response handleOAuthAccessTokenValidationError() throws OAuthSystemException {
        OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                .setError(OAuth2ErrorCodes.ACCESS_DENIED)
                .setErrorDescription("Access Token Validation failed").buildJSONMessage();

        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                        .entity(response.getBody());

        // build the message body
        return responseBuilder.build();
    }



    private Response handleInvalidAccessToken(String errorMsg) throws OAuthSystemException {
        OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                .setError(OAuth2ErrorCodes.ACCESS_DENIED)
                .setErrorDescription(errorMsg).buildJSONMessage();

        Response.ResponseBuilder responseBuilder =
                Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                .entity(response.getBody());

        // build the message body
        return responseBuilder.build();
    }


    /**
     * Log the received token request
     * @param request
     */
    private void logRptRequest(HttpServletRequest request, UmaRptRequestPayloadBean payloadBean){

        StringBuilder builder = new StringBuilder();

        if (log.isDebugEnabled()){
            builder.append("Received a request : ").append(request.getRequestURI()).append("\n");
            //log.debug("Received a request : " + request.getRequestURI());
            // log the headers.
            builder.append("----------logging request headers.----------").append("\n");
            //log.debug("----------logging request headers.----------");

            Enumeration headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                Enumeration headers = request.getHeaders(headerName);
                while (headers.hasMoreElements()) {
                    builder.append(headerName).append(" : ").append(headers.nextElement()).append("\n");
           //         log.debug(headerName + " : " + headers.nextElement());
                }
            }

            log.debug(builder.toString());

            builder.setLength(0);

            // log the parameters.
            builder.append("----------logging request payload----------").append("\n");
           // log.debug("----------logging request payload----------");
            try {
                builder.append(new ObjectMapper().writeValueAsString(payloadBean)).append("\n");
             //   log.debug(new ObjectMapper().writeValueAsString(payloadBean));
            } catch (IOException e) {
                log.error("Cannot convert the payload to a JSON");
            }

            log.debug(builder.toString());
        }

    }


    /**
     *
     * @param rptRequest
     * @return
     */
    private UmaRptResponse getRequestPartyToken(UmaRptRequest rptRequest){
        // set parameters in the request if necessary

        return EndpointUtil.getUMAService().issueRPT(rptRequest);
    }
}
