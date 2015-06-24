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

package org.wso2.carbon.identity.scim.provider.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.jaxrs.designator.PATCH;
import org.wso2.carbon.identity.scim.provider.impl.IdentitySCIMManager;
import org.wso2.carbon.identity.scim.provider.util.JAXRSResponseBuilder;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.charon.core.encoder.Encoder;
import org.wso2.charon.core.exceptions.BadRequestException;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.FormatNotSupportedException;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.protocol.ResponseCodeConstants;
import org.wso2.charon.core.protocol.SCIMResponse;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;
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

@Path("/")
public class UserResource extends AbstractResource {
    private static Log logger = LogFactory.getLog(UserResource.class);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                            @HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
                            @HeaderParam(SCIMConstants.AUTHENTICATION_TYPE_HEADER) String authMechanism,
                            @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {

        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // defaults to application/json.
            format = identifyOutputFormat(format);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));

            // obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();

            SCIMResponse scimResponse = userResourceEndpoint.get(id, format, userManager);
            // needs to check the code of the response and return 200 0k or other error codes
            // appropriately.
            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

    @POST
    public Response createUser(@HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                               String resourceString) {

        Encoder encoder = null;
        try {
            // obtain default charon manager
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // content-type header is compulsory in post request.
            if (inputFormat == null) {
                String error = SCIMConstants.CONTENT_TYPE_HEADER
                        + " not present in the request header";
                throw new FormatNotSupportedException(error);
            }
            // identify input format
            inputFormat = identifyInputFormat(inputFormat);
            // set the format in which the response should be encoded, if not specified in the
            // request,
            // defaults to application/json.
            outputFormat = identifyOutputFormat(outputFormat);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));

            // obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();

            SCIMResponse response = userResourceEndpoint.create(resourceString, inputFormat,
                    outputFormat, userManager);

            return new JAXRSResponseBuilder().buildResponse(response);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteUser(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {
        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // defaults to application/json.
            if (format == null) {
                format = SCIMConstants.APPLICATION_JSON;
            }
            // set the format in which the response should be encoded, if not specified in the
            // request,
            // defaults to application/json.
            format = identifyOutputFormat(format);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));

            // obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();

            SCIMResponse scimResponse = userResourceEndpoint.delete(id, userManager, format);
            // needs to check the code of the response and return 200 0k or other error codes
            // appropriately.
            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
                            @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                            @QueryParam("attributes") String searchAttribute, @QueryParam("filter") String filter,
                            @QueryParam("startIndex") String startIndex, @QueryParam("count") String count,
                            @QueryParam("sortBy") String sortBy, @QueryParam("sortOrder") String sortOrder) {
        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // defaults to application/json.
            format = identifyOutputFormat(format);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));

            // obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();
            SCIMResponse scimResponse = null;
            if (searchAttribute != null) {
                scimResponse = userResourceEndpoint.listByAttribute(searchAttribute, userManager,
                        format);
            } else if (filter != null) {
                scimResponse = userResourceEndpoint.listByFilter(filter, userManager, format);
            } else if (startIndex != null && count != null) {
                scimResponse = userResourceEndpoint.listWithPagination(Integer.valueOf(startIndex),
                        Integer.valueOf(count), userManager, format);
            } else if (sortBy != null) {
                scimResponse = userResourceEndpoint.listBySort(sortBy, sortOrder, userManager,
                        format);
            } else if (searchAttribute == null && filter == null && startIndex == null
                    && count == null && sortBy == null) {
                scimResponse = userResourceEndpoint.list(userManager, format);
            } else {
                // bad request
                throw new BadRequestException(ResponseCodeConstants.DESC_BAD_REQUEST_GET);
            }

            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (BadRequestException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

    @PUT
    @Path("{id}")
    public Response updateUser(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                               @HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                               String resourceString) {
        Encoder encoder = null;
        try {
            // obtain default charon manager
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // content-type header is compulsory in post request.
            if (inputFormat == null) {
                String error = SCIMConstants.CONTENT_TYPE_HEADER
                        + " not present in the request header";
                throw new FormatNotSupportedException(error);
            }
            // identify input format
            inputFormat = identifyInputFormat(inputFormat);
            // set the format in which the response should be encoded, if not specified in the
            // request,
            // defaults to application/json.
            outputFormat = identifyOutputFormat(outputFormat);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));

            // obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();

            SCIMResponse response = userResourceEndpoint.updateWithPUT(id, resourceString,
                    inputFormat, outputFormat, userManager);

            return new JAXRSResponseBuilder().buildResponse(response);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

    @PATCH
    @Path("{id}")
    public Response updateUserPATCH(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                                    @HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                                    @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                                    @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                                    String resourceString) {
        Encoder encoder = null;
        try {
            // obtain default charon manager
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();

            // content-type header is compulsory in post request.
            if (StringUtils.isEmpty(inputFormat)) {
                String error = SCIMConstants.CONTENT_TYPE_HEADER
                        + " not present in the request header";
                throw new FormatNotSupportedException(error);
            }
            // identify input format
            inputFormat = identifyInputFormat(inputFormat);
            // set the format in which the response should be encoded, if not specified in the
            // request,
            // defaults to application/json.
            outputFormat = identifyOutputFormat(outputFormat);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));

            // obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();

            SCIMResponse response = userResourceEndpoint.updateWithPATCH(id, resourceString,
                    inputFormat, outputFormat, userManager);

            return new JAXRSResponseBuilder().buildResponse(response);

        } catch (CharonException e) {
            logger.error("Charon Exception raised while executing PATCH request.", e);
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            logger.debug("Request format is not supported.", e);
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuthorizedUser(
            @HeaderParam(SCIMConstants.ACCEPT_HEADER) String format,
            @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {
        Encoder encoder = null;
        try {
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();
            String filter = "userNameEq" + MultitenantUtils.getTenantAwareUsername(authorization);

            // defaults to application/json.
            format = identifyOutputFormat(format);
            // obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(format));

            // obtain the user store manager
            String SCIM_LIST_USER_PERMISSION = "/permission/admin/login";
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization, SCIM_LIST_USER_PERMISSION);

            // create charon-SCIM user endpoint and hand-over the request.
            UserResourceEndpoint userResourceEndpoint = new UserResourceEndpoint();
            SCIMResponse scimResponse = null;
            scimResponse = userResourceEndpoint.listByFilter(filter, userManager, format);

            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            // create SCIM response with code as the same of exception and message as error message
            // of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(AbstractResourceEndpoint
                    .encodeSCIMException(encoder, e));
        }
    }

}
