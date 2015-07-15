/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.uma.dto.UmaResponse;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;
import org.wso2.carbon.identity.uma.internal.UMAServiceComponentHolder;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class UMAUtil {

    private static final String OAUTH_TOKEN_VALIDATION_RESPONSE = "oauth.access.token.validation.response";

    /**
     * Util method to get the tenant ID given the tenantDomain
     * @param tenantDomain
     * @return
     * @throws IdentityUMAException
     */
    public static int getTenantId(String tenantDomain) throws IdentityUMAException {
        RealmService realmService = UMAServiceComponentHolder.getRealmService();
        try {
            return realmService.getTenantManager().getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String error = "Error in obtaining tenant ID from tenant domain : " + tenantDomain;
            throw new IdentityUMAException(error, e);
        }
    }

    /**
     * Build a comma separated list of scopes passed as a String set by Amber.
     *
     * @param scopes set of scopes
     * @return Comma separated list of scopes
     */
    public static String buildScopeString(String[] scopes) {
        if (scopes != null) {
            StringBuilder scopeString = new StringBuilder("");
            Arrays.sort(scopes);
            for (int i = 0; i < scopes.length; i++) {
                scopeString.append(scopes[i].trim());
                if (i != scopes.length - 1) {
                    scopeString.append(",");
                }
            }
            return scopeString.toString();
        }
        return null;
    }

    public static String[] buildScopeArray(String scopeStr) {
        if (StringUtils.isNotBlank(scopeStr)) {
            scopeStr = scopeStr.trim();
            return scopeStr.split("\\s");
        }
        return new String[0];
    }


    public static String hashScopes(String scope){
        if (StringUtils.isNotBlank(scope)) {
            //first converted to an array to sort the scopes
            return DigestUtils.md5Hex(buildScopeString(buildScopeArray(scope)));
        } else {
            return null;
        }
    }


    /**
     * Util method to get the consumer key from the HttpServletRequest
     * Note : The OAuthTokenValidationValve sets the token validation response in the
     * attribute "oauth.access.token.validation.response" in the HttpServletRequest
     *
     * @param httpServletRequest
     * @return
     */
    public static String getConsumerKey(HttpServletRequest httpServletRequest){
        String consumerKey = null;

        if (httpServletRequest.getAttribute(OAUTH_TOKEN_VALIDATION_RESPONSE) != null){
            OAuth2ClientApplicationDTO applicationDTO =
                    (OAuth2ClientApplicationDTO)httpServletRequest.getAttribute(OAUTH_TOKEN_VALIDATION_RESPONSE);
            consumerKey = applicationDTO.getConsumerKey();

        }
        return consumerKey;
    }


    /**
     * Build a JAX-RS Response Object from UmaResponse DTO object
     * @param response UMAResponse
     * @return
     */
    public static Response buildResponse(UmaResponse response){
        // building the response
        Response.ResponseBuilder responseBuilder =
                Response.status(response.getResponseStatus());
        responseBuilder.entity(response.getBody());

        // adding the headers to the response
        Map<String, String> headers = response.getHeaders();
        for(Map.Entry<String,String> header : headers.entrySet() ){
            if (header.getKey() != null && header.getValue() != null){
                responseBuilder.header(header.getKey(),header.getValue());
            }
        }

        return responseBuilder.build();
    }


    private UMAUtil(){}
}
