/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.scim.provider.resources;

import org.wso2.carbon.identity.scim.provider.impl.IdentitySCIMManager;
import org.wso2.carbon.identity.scim.provider.util.JAXRSResponseBuilder;
import org.wso2.charon.core.encoder.Encoder;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.FormatNotSupportedException;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.protocol.ResponseCodeConstants;
import org.wso2.charon.core.protocol.SCIMResponse;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;
import org.wso2.charon.core.protocol.endpoints.BulkResourceEndpoint;
import org.wso2.charon.core.schema.SCIMConstants;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class BulkResource {
    @POST
    public Response createUser(@HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                               String resourceString) {
        Encoder encoder = null;
        try {
            //obtain default charon manager
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            //content-type header is compulsory in post request.
            if (inputFormat == null) {
                String error = SCIMConstants.CONTENT_TYPE_HEADER + " not present in the request header.";
                throw new FormatNotSupportedException(error);
            }
            //set the format in which the response should be encoded, if not specified in the request,
            // defaults to application/json.
            if (!outputFormat.equals("application/json")) {
                outputFormat = SCIMConstants.APPLICATION_JSON;
            }
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));
            /*//perform authentication
            Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(SCIMConstants.AUTHORIZATION_HEADER, authorization);
            //authenticate the request
            AuthenticationInfo authInfo = identitySCIMManager.handleAuthentication(headerMap);*/

            //obtain the user store manager
            UserManager userManager = identitySCIMManager.getInstance().getUserManager(
                    authorization);

            BulkResourceEndpoint bulkResourceEndpoint = new BulkResourceEndpoint();
            SCIMResponse responseString = bulkResourceEndpoint.processBulkData(resourceString,
                                                                               inputFormat,
                                                                               outputFormat,
                                                                               userManager);


            return new JAXRSResponseBuilder().buildResponse(responseString);

        } catch (CharonException e) {
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }
}
