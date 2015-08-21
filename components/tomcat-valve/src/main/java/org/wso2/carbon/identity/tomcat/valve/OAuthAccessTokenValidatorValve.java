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

package org.wso2.carbon.identity.tomcat.valve;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import static org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO.*;

public class OAuthAccessTokenValidatorValve extends ValveBase{

    private static final Log log = LogFactory.getLog(OAuthAccessTokenValidatorValve.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_HEADER_KEYWORD = "Bearer";
    public static final String BEARER_TOKEN_TYPE = "bearer";

    public static final String OAUTH_VALIDATION_RESPONSE = "oauth.access.token.validation.response";
    public static final String OAUTH_ACCESS_TOKEN = "oauth.access.token";



    // By Default No endpoint is protected
    private String endpoints = "";
    private ArrayList<String> protectedEndpoints;

    public OAuthAccessTokenValidatorValve(){
        // async support
        super(true);
    }


    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;

        protectedEndpoints = new ArrayList<>();
        // split the comma delimited string and add to the list of protected endpoints
        for (String endpoint : endpoints.split(",")){
            String temp = endpoint.trim();
            if (!temp.isEmpty()){
                if (temp.startsWith("/")) {
                    temp = temp.substring(1);
                }
                protectedEndpoints.add(temp);
            }
        }
    }


    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if(log.isDebugEnabled()){
            log.debug("OAuth REST Endpoint protection valve is initialized");
        }
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        try {
            // check whether the requested endpoint is protected
            if (checkEndPointIsProtected(request.getRequestURI())) {
                if (log.isDebugEnabled()){
                    log.debug("Request to OAuth protected Endpoint validated : "
                            +request.getHost().getName()+request.getRequestURI());
                }

                // check for the Authorization Header
                if (checkForAuthorizationHeader(request)) {
                    // get the authorization header
                    String authzHeader = request.getHeader(AUTHORIZATION_HEADER).trim();
                    if (log.isDebugEnabled()) {
                        log.debug("Authorization Header : " + authzHeader);
                    }

                    // check whether Bearer keyword is there
                    if (authzHeader.startsWith(BEARER_HEADER_KEYWORD)) {
                        String accessToken = extractAccessToken(authzHeader);

                        OAuth2ClientApplicationDTO clientApplicationDTO = validateAccessToken(accessToken);


                        // set the OAuth access token validation response and the consumer key
                        // within the OAuth2ClientApplicationDTO as an attribute
                        request.setAttribute(OAUTH_VALIDATION_RESPONSE, clientApplicationDTO);
                        request.setAttribute(OAUTH_ACCESS_TOKEN,accessToken);

                        if (log.isDebugEnabled()) {
                            log.debug("OAuth access token validation completed for " + accessToken);
                        }

                    } else {
                        log.error("Bearer keyword not found in the header");
                    }

                } else {
                    log.error("Authorization Header not found in the request");
                }
            }else{
                // since the End Point is not protected by OAuth we simply pass it on to the next valve
//                if (log.isDebugEnabled()){
//                    log.debug("Requested Endpoint "+request.getHost().getName()+request.getRequestURI()
//                            +" is not protected, Passing on to the next valve");
//                }
            }
        }catch (Exception ex){
            log.error("Could not handle the request",ex);
        }finally {

            // invoke the next valve
            getNext().invoke(request, response);
        }

    }


    private boolean checkEndPointIsProtected(String requestedURI){
        // substring to remove the preceding path separators
        String URI = requestedURI.trim();
        boolean isProtected = false;

        // check whether the trimmed String is not empty
        if(StringUtils.isNotEmpty(URI)){
            // check whether there is a preceeding / character
            if (StringUtils.startsWith(URI,"/")){
                URI = URI.substring(1);
            }

            for (String endpoint : protectedEndpoints){
                if (StringUtils.equals(endpoint,URI) || StringUtils.startsWith(URI,endpoint)){
                    isProtected = true;
                }
            }

        }
        return isProtected;
    }

    /**
     * Method to validate the access token and return the details about the token and the consumer
     * @param accessTokenIdentifier
     * @return
     */
    private OAuth2ClientApplicationDTO validateAccessToken(String accessTokenIdentifier){

        // create an OAuth token validating request DTO
        OAuth2TokenValidationRequestDTO oauthValidationRequest = new OAuth2TokenValidationRequestDTO();

        // create access token object to validate and populate it
        OAuth2TokenValidationRequestDTO.OAuth2AccessToken accessToken = oauthValidationRequest.new OAuth2AccessToken();
        accessToken.setTokenType(BEARER_TOKEN_TYPE);
        accessToken.setIdentifier(accessTokenIdentifier);

        // set the token to the validation request
        oauthValidationRequest.setAccessToken(accessToken);


        OAuth2TokenValidationService oauthValidationService = new OAuth2TokenValidationService();
        OAuth2ClientApplicationDTO oauthValidationResponse = oauthValidationService
                .findOAuthConsumerIfTokenIsValid(oauthValidationRequest);

        return oauthValidationResponse;
    }


    /**
     * Method to check whether the Authorization Header is present in the request
     * @param request
     * @return
     */
    private boolean checkForAuthorizationHeader(Request request){
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()){
            if(AUTHORIZATION_HEADER.equalsIgnoreCase(headers.nextElement().trim())){
                return true;
            }
        }
        return false;
    }


    /**
     * Method to extract the access token string from the Authorization header
     * @param authorizationHeader String Authorization header value
     * @return String returns the access token
     */
    private String extractAccessToken(String authorizationHeader){

        String accessToken = authorizationHeader.trim().substring(7);

        if (log.isDebugEnabled()){
            log.debug("Extracted Access Token from header : "+accessToken);
        }
        return accessToken;
    }
}
