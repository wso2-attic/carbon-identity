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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.scim.provider.impl.IdentitySCIMManager;
import org.wso2.carbon.identity.scim.provider.util.JAXRSResponseBuilder;
import org.wso2.charon.core.encoder.Encoder;
import org.wso2.charon.core.exceptions.BadRequestException;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.FormatNotSupportedException;
import org.wso2.charon.core.exceptions.UnauthorizedException;
import org.wso2.charon.core.extensions.AuthenticationInfo;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.protocol.ResponseCodeConstants;
import org.wso2.charon.core.protocol.SCIMResponse;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;
import org.wso2.charon.core.protocol.endpoints.GroupResourceEndpoint;
import org.wso2.charon.core.protocol.endpoints.UserResourceEndpoint;
import org.wso2.charon.core.schema.SCIMConstants;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class GroupResource extends AbstractResource {

    private static Log logger = LogFactory.getLog(GroupResource.class);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                             @HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
                             @HeaderParam(SCIMConstants.AUTHENTICATION_TYPE_HEADER) String authMechanism,
                             @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {

        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // defaults to application/json.
            format = identifyOutputFormat(format);
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));
            //perform authentication
            /*Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(SCIMConstants.AUTHORIZATION_HEADER, authorization);
            headerMap.put(SCIMConstants.AUTHENTICATION_TYPE_HEADER, authMechanism);
            //authenticate the request
            AuthenticationInfo authInfo = identitySCIMManager.handleAuthentication(headerMap);*/

            //obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            //create charon-SCIM group endpoint and hand-over the request.
            GroupResourceEndpoint groupResourceEndpoint = new GroupResourceEndpoint();

            SCIMResponse scimResponse = groupResourceEndpoint.get(id, format, userManager);
            //needs to check the code of the response and return 200 0k or other error codes
            // appropriately.
            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            e.printStackTrace();
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            e.printStackTrace();
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }

    @POST
    public Response createGroup(@HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                               String resourceString) {

        Encoder encoder = null;
        try {
            //obtain default charon manager
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            //content-type header is compulsory in post request.
            if (inputFormat == null) {
                String error = SCIMConstants.CONTENT_TYPE_HEADER + " not present in the request header";
                throw new FormatNotSupportedException(error);
            }
	        //identify input format
            inputFormat = identifyInputFormat(inputFormat);
            //set the format in which the response should be encoded, if not specified in the request,
            // defaults to application/json.
            outputFormat = identifyOutputFormat(outputFormat);
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));
            //perform authentication
            /*Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(SCIMConstants.AUTHORIZATION_HEADER, authorization);
            //authenticate the request
            AuthenticationInfo authInfo = identitySCIMManager.handleAuthentication(headerMap);*/

            //obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            //create charon-SCIM user endpoint and hand-over the request.
            GroupResourceEndpoint groupResourceEndpoint = new GroupResourceEndpoint();

            SCIMResponse response = groupResourceEndpoint.create(resourceString, inputFormat,
                                                                outputFormat, userManager);

            return new JAXRSResponseBuilder().buildResponse(response);

        } catch (CharonException e) {
            e.printStackTrace();
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            e.printStackTrace();
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }
    
    @DELETE
    @Path("{id}")
    public Response deleteGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {
        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // defaults to application/json.
            if (format == null) {
                format = SCIMConstants.APPLICATION_JSON;
            }
            //set the format in which the response should be encoded, if not specified in the request,
            // defaults to application/json.
            format = identifyOutputFormat(format);
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));
            //perform authentication
            /*Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(SCIMConstants.AUTHORIZATION_HEADER, authorization);
            //authenticate the request
            AuthenticationInfo authInfo = identitySCIMManager.handleAuthentication(headerMap);*/

            //obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            //create charon-SCIM group endpoint and hand-over the request.
            GroupResourceEndpoint groupResourceEndpoint = new GroupResourceEndpoint();

            SCIMResponse scimResponse = groupResourceEndpoint.delete(id, userManager, format);
            //needs to check the code of the response and return 200 0k or other error codes
            // appropriately.
            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            e.printStackTrace();
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            e.printStackTrace();
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }

    @GET
    public Response getGroup(@HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
                            @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                            @QueryParam("attributes") String searchAttribute,
                            @QueryParam("filter") String filter,
                            @QueryParam("startIndex") String startIndex,
                            @QueryParam("count") String count, @QueryParam("sortBy") String sortBy,
                            @QueryParam("sortOrder") String sortOrder) {
        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // defaults to application/json.
            format = identifyOutputFormat(format);
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));
            //perform authentication
            /*Map<String, String> headerMap = new HashMap<String, String>();
            headerMap.put(SCIMConstants.AUTHORIZATION_HEADER, authorization);
            //authenticate the request
            AuthenticationInfo authInfo = identitySCIMManager.handleAuthentication(headerMap);*/

            //obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            //create charon-SCIM user endpoint and hand-over the request.
            GroupResourceEndpoint groupResourceEndpoint = new GroupResourceEndpoint();
            SCIMResponse scimResponse = null;
            if (searchAttribute != null) {
                scimResponse = groupResourceEndpoint.listByAttribute(searchAttribute, userManager, format);
            } else if (filter != null) {
                scimResponse = groupResourceEndpoint.listByFilter(filter, userManager, format);
            } else if (startIndex != null && count != null) {
                scimResponse = groupResourceEndpoint.listWithPagination(Integer.valueOf(startIndex),
                                                                       Integer.valueOf(count),
                                                                       userManager, format);
            } else if (sortBy != null) {
                scimResponse = groupResourceEndpoint.listBySort(sortBy, sortOrder, userManager, format);
            } else if (searchAttribute == null && filter == null && startIndex == null &&
                       count == null && sortBy == null) {
                scimResponse = groupResourceEndpoint.list(userManager, format);
            } else {
                //bad request
                throw new BadRequestException(ResponseCodeConstants.DESC_BAD_REQUEST_GET);
            }

            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            e.printStackTrace();
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            e.printStackTrace();
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (BadRequestException e) {
            e.printStackTrace();
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }

    @PUT
    @Path("{id}")
    public Response updateGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                               @HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                               String resourceString) {
        Encoder encoder = null;
        try {
            //obtain default charon manager
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            //content-type header is compulsory in post request.
            if (inputFormat == null) {
                String error = SCIMConstants.CONTENT_TYPE_HEADER + " not present in the request header";
                throw new FormatNotSupportedException(error);
            }
	        //identify input format
            inputFormat = identifyInputFormat(inputFormat);
            //set the format in which the response should be encoded, if not specified in the request,
            // defaults to application/json.
            outputFormat = identifyOutputFormat(outputFormat);
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));

            //obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            //create charon-SCIM user endpoint and hand-over the request.
            GroupResourceEndpoint groupResourceEndpoint = new GroupResourceEndpoint();

            SCIMResponse response = groupResourceEndpoint.updateWithPUT(id, resourceString, inputFormat,
                                                                       outputFormat, userManager);

            return new JAXRSResponseBuilder().buildResponse(response);

        } catch (CharonException e) {
            e.printStackTrace();
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            e.printStackTrace();
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }
}
