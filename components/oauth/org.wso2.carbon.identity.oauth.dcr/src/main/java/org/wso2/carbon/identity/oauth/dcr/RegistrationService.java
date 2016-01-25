/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.dcr;

import org.wso2.carbon.identity.oauth.dcr.dto.RegistrationProfile;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface RegistrationService {

    enum ErrorCode {
        INVALID_URI("invalid_redirect_uri"), INVALID_CLIENT_METADATA("invalid_client_metadata");

        private String value;
        ErrorCode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * This method is used to dynamically register an OAuth application.
     *
     * @param profile contains the necessary attributes that are
     *                needed in order to register an app.
     * @return Status 200 if success including consumerKey and consumerSecret.
     */
    @POST
    Response register(RegistrationProfile profile);

    /**
     * This method is used to remove an already registered OAuth application.
     *
     * @param applicationName name of the application.
     * @param userId name of the application owner.
     * @param consumerKey provided consumerKey for the registered application.
     * @return Status 200 if success.
     */
    @DELETE
    Response unRegister(@QueryParam("applicationName") String applicationName,
            @QueryParam("userId") String userId, @QueryParam("consumerKey") String consumerKey);

  //--------------------------------------------------------------------------------------------------------
    //The following method declarations can be removed from this RegistrationService interface if required
  //--------------------------------------------------------------------------------------------------------

    /**
     * This method will register a new OAuth application using the data provided by
     * RegistrationProfile.
     *
     * @param profile - RegistrationProfile of the OAuth application to be created.
     * @return OAuthApplicationInfo object which holds the necessary data of created OAuth app.
     * @throws DynamicClientRegistrationException
     *
     */
    OAuthApplicationInfo registerOAuthApplication(
            RegistrationProfile profile) throws DynamicClientRegistrationException;

    /**
     * This method will unregister a created OAuth application.
     *
     * @param userName        - Username of the owner
     * @param applicationName - OAuth application name
     * @param consumerKey     - ConsumerKey of the OAuth application
     * @return The status of the operation
     * @throws DynamicClientRegistrationException
     *
     */
    boolean unregisterOAuthApplication(String userName, String applicationName,
            String consumerKey) throws DynamicClientRegistrationException;

    /**
     * This method will check the existence of an OAuth application provided application-name.
     *
     * @param applicationName - OAuth application name
     * @return The status of the operation
     * @throws DynamicClientRegistrationException
     *
     */
    boolean isOAuthApplicationAvailable(String applicationName) throws DynamicClientRegistrationException;

}