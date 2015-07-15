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

package org.wso2.carbon.identity.uma.endpoint.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.uma.UMAService;
import org.wso2.carbon.identity.uma.dto.UmaResponse;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Map;

public class EndpointUtil {

    private static final String OAUTH_TOKEN_VALIDATION_RESPONSE = "oauth.access.token.validation.response";

    private static final Log log = LogFactory.getLog(EndpointUtil.class);

    private EndpointUtil() {

    }

    /**
     * Returns the {@code UMAService} instance
     *
     * @return UMAService instance
     */
    public static UMAService getUMAService(){

        return (UMAService)PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(UMAService.class, null);
    }



    public static void checkAuthorization(HttpServletRequest httpServletRequest, String scope) throws IdentityUMAException {
        boolean isAuthorized = false;

        if (httpServletRequest.getAttribute(OAUTH_TOKEN_VALIDATION_RESPONSE) != null){
            OAuth2ClientApplicationDTO applicationDTO =
                    (OAuth2ClientApplicationDTO)httpServletRequest.getAttribute(OAUTH_TOKEN_VALIDATION_RESPONSE);

            OAuth2TokenValidationResponseDTO oAuth2TokenValidationResponseDTO =
                    applicationDTO.getAccessTokenValidationResponse();

            boolean isTokenValid = oAuth2TokenValidationResponseDTO.isValid();

            boolean hasRequiredScope = false;

            // we only need to consider about the scope if the token is valid
            if (isTokenValid) {
                // check if the required scope is present
                for (String tokenScope : oAuth2TokenValidationResponseDTO.getScope()) {
                    if (StringUtils.equalsIgnoreCase(tokenScope.trim(), scope.trim())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Required scope " + scope + " found in the access token");
                        }
                        hasRequiredScope = true;
                        break;
                    }
                }

                if (!hasRequiredScope) {
                    String errorMsg = "Required scope '" + scope + "' not found in the access token";
                    log.error(errorMsg);
                    throw new IdentityUMAException(errorMsg);
                }

            }else{
                // log the reason for the token to be invalid
                String errorMsg = oAuth2TokenValidationResponseDTO.getErrorMsg();
                log.error("Invalid Access Token : "+errorMsg);
                throw new IdentityUMAException(errorMsg);
            }

        }else{
            // the access token validation has not been set by the tomcat valve
            log.error(OAUTH_TOKEN_VALIDATION_RESPONSE +" attribute not set by the OAuth Access Token Validation Valve");
            throw new IdentityUMAException("OAuth Token Cannot be validated");
        }

    }



    /**
     * Build a JAX-RS Response Object from UmaResponse DTO object
     * @param umaResponse UMAResponse
     * @return
     */
    public static Response buildResponse(UmaResponse umaResponse){
        // building the response
        Response.ResponseBuilder responseBuilder =
                Response.status(umaResponse.getResponseStatus());
        responseBuilder.entity(umaResponse.getBody());

        // adding the headers to the response
        Map<String, String> headers = umaResponse.getHeaders();
        for(Map.Entry<String,String> header : headers.entrySet() ){
            if (header.getKey() != null && header.getValue() != null){
                responseBuilder.header(header.getKey(),header.getValue());
            }
        }

        return responseBuilder.build();
    }


    public static Response buildOAuthErrorMessage(String errorMsg){

        UmaResponse.UmaErrorResponseBuilder errorResponseBuilder =
                UmaResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                          .setError(OAuth2ErrorCodes.ACCESS_DENIED);

        if (errorMsg!= null && StringUtils.isNotEmpty(errorMsg)){
                          errorResponseBuilder.setErrorDescription(errorMsg);
        }

        UmaResponse umaResponse = errorResponseBuilder.buildJSONResponse();

        return Response.status(umaResponse.getResponseStatus()).entity(umaResponse.getBody()).build();
    }
}
