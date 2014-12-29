/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.scim.provider.resources;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.scim.provider.impl.IdentitySCIMManager;
import org.wso2.carbon.identity.scim.provider.util.JAXRSResponseBuilder;
import org.wso2.carbon.identity.scim.provider.util.SCIMProviderConstants;
import org.wso2.carbon.webapp.ext.jaxrs.designator.PATCH;
import org.wso2.charon.core.encoder.Encoder;
import org.wso2.charon.core.exceptions.BadRequestException;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.FormatNotSupportedException;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.protocol.ResponseCodeConstants;
import org.wso2.charon.core.protocol.SCIMResponse;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;
import org.wso2.charon.core.protocol.endpoints.GroupResourceEndpoint;
import org.wso2.charon.core.schema.SCIMConstants;

import java.util.HashMap;

@Path("/")
public class GroupResource extends AbstractResource {

    private static Log logger = LogFactory.getLog(GroupResource.class);

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                             @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                             @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {
        HashMap<String, String> requestAttributes = new HashMap<String, String>();
        requestAttributes.put(SCIMProviderConstants.ID, id);
        requestAttributes.put(SCIMProviderConstants.OUTPUT_FORMAT, outputFormat);
        requestAttributes.put(SCIMProviderConstants.AUTHORIZATION, authorization);
        requestAttributes.put(SCIMProviderConstants.HTTP_VERB, GET.class.getSimpleName());
        return processRequest(requestAttributes);
    }

    @POST
    public Response createGroup(@HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                                @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                                @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                                String resourceString) {
        HashMap<String, String> requestAttributes = new HashMap<String, String>();
        requestAttributes.put(SCIMProviderConstants.INPUT_FORMAT, inputFormat);
        requestAttributes.put(SCIMProviderConstants.OUTPUT_FORMAT, outputFormat);
        requestAttributes.put(SCIMProviderConstants.AUTHORIZATION, authorization);
        requestAttributes.put(SCIMProviderConstants.HTTP_VERB, POST.class.getSimpleName());
        return processRequest(requestAttributes);
    }

    @DELETE
    @Path("{id}")
    public Response deleteGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                                @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                                @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization) {
        HashMap<String, String> requestAttributes = new HashMap<String, String>();
        requestAttributes.put(SCIMProviderConstants.ID, id);
        requestAttributes.put(SCIMProviderConstants.OUTPUT_FORMAT, outputFormat);
        requestAttributes.put(SCIMProviderConstants.AUTHORIZATION, authorization);
        requestAttributes.put(SCIMProviderConstants.HTTP_VERB, DELETE.class.getSimpleName());
        return processRequest(requestAttributes);
    }

    @PUT
    @Path("{id}")
    public Response updateGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                                @HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                                @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                                @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                                String resourceString) {
        HashMap<String, String> requestAttributes = new HashMap<String, String>();
        requestAttributes.put(SCIMProviderConstants.ID, id);
        requestAttributes.put(SCIMProviderConstants.INPUT_FORMAT, inputFormat);
        requestAttributes.put(SCIMProviderConstants.OUTPUT_FORMAT, outputFormat);
        requestAttributes.put(SCIMProviderConstants.AUTHORIZATION, authorization);
        requestAttributes.put(SCIMProviderConstants.HTTP_VERB, PUT.class.getSimpleName());
        return processRequest(requestAttributes);
    }

    @PATCH
    @Path("{id}")
    public Response patchGroup(@PathParam(SCIMConstants.CommonSchemaConstants.ID) String id,
                               @HeaderParam(SCIMConstants.CONTENT_TYPE_HEADER) String inputFormat,
                               @HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                               @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                               String resourceString) {
        HashMap<String, String> requestAttributes = new HashMap<String, String>();
        requestAttributes.put(SCIMProviderConstants.ID, id);
        requestAttributes.put(SCIMProviderConstants.INPUT_FORMAT, inputFormat);
        requestAttributes.put(SCIMProviderConstants.OUTPUT_FORMAT, outputFormat);
        requestAttributes.put(SCIMProviderConstants.AUTHORIZATION, authorization);
        requestAttributes.put(SCIMProviderConstants.HTTP_VERB, SCIMProviderConstants.PATCH);
        return processRequest(requestAttributes);
    }

    @GET
    public Response getGroup(@HeaderParam(SCIMConstants.ACCEPT_HEADER) String outputFormat,
                             @HeaderParam(SCIMConstants.AUTHORIZATION_HEADER) String authorization,
                             @QueryParam("attributes") String searchAttribute,
                             @QueryParam("filter") String filter,
                             @QueryParam("startIndex") String startIndex,
                             @QueryParam("count") String count, @QueryParam("sortBy") String sortBy,
                             @QueryParam("sortOrder") String sortOrder) {
        HashMap<String, String> requestAttributes = new HashMap<String, String>();
        requestAttributes.put(SCIMProviderConstants.OUTPUT_FORMAT, outputFormat);
        requestAttributes.put(SCIMProviderConstants.AUTHORIZATION, authorization);
        requestAttributes.put(SCIMProviderConstants.HTTP_VERB, GET.class.getSimpleName());
        requestAttributes.put(SCIMProviderConstants.SEARCH_ATTRIBUTE, searchAttribute);
        requestAttributes.put(SCIMProviderConstants.FILTER, filter);
        requestAttributes.put(SCIMProviderConstants.START_INDEX, startIndex);
        requestAttributes.put(SCIMProviderConstants.COUNT, count);
        requestAttributes.put(SCIMProviderConstants.SORT_BY, sortBy);
        requestAttributes.put(SCIMProviderConstants.SORT_ORDER, sortOrder);
        return processRequest(requestAttributes);
    }

    /**
     *
     * @param requestAttributes
     * @return
     */
    private Response processRequest(final HashMap<String, String> requestAttributes) {

        String id = requestAttributes.get(SCIMProviderConstants.ID);
        String inputFormat = requestAttributes.get(SCIMProviderConstants.INPUT_FORMAT);
        String outputFormat = requestAttributes.get(SCIMProviderConstants.OUTPUT_FORMAT);
        String authorization = requestAttributes.get(SCIMProviderConstants.AUTHORIZATION);
        String httpVerb = requestAttributes.get(SCIMProviderConstants.HTTP_VERB);
        String resourceString = requestAttributes.get(SCIMProviderConstants.RESOURCE_STRING);
        Encoder encoder = null;
        try {
            outputFormat = identifyOutputFormat(outputFormat);
            if (inputFormat != null) {
                inputFormat = identifyInputFormat(inputFormat);
            }
            IdentitySCIMManager identitySCIMManager = IdentitySCIMManager.getInstance();
            //obtain the encoder at this layer in case exceptions needs to be encoded.
            encoder = identitySCIMManager.getEncoder(SCIMConstants.identifyFormat(outputFormat));
            //obtain the user store manager
            UserManager userManager = IdentitySCIMManager.getInstance().getUserManager(
                    authorization);

            //create charon-SCIM group endpoint and hand-over the request.
            GroupResourceEndpoint groupResourceEndpoint = new GroupResourceEndpoint();
            SCIMResponse scimResponse = null;
            if (GET.class.getSimpleName().equals(httpVerb)) {
                scimResponse = groupResourceEndpoint.get(id, outputFormat, userManager);
            } else if (POST.class.getSimpleName().equals(httpVerb)) {
                scimResponse = groupResourceEndpoint.create(resourceString, inputFormat, outputFormat, userManager);
            } else if (PUT.class.getSimpleName().equals(httpVerb)) {
                scimResponse =
                        groupResourceEndpoint.updateWithPUT(id, resourceString, inputFormat, outputFormat, userManager);
            } else if (SCIMProviderConstants.PATCH.equals(httpVerb)) {
                scimResponse = groupResourceEndpoint
                        .updateWithPATCH(id, resourceString, inputFormat, outputFormat, userManager);
            } else if (DELETE.class.getSimpleName().equals(httpVerb)) {
                scimResponse = groupResourceEndpoint.delete(id, userManager, outputFormat);
            } else if (GET.class.getSimpleName().equals(httpVerb) && id == null) {
                String searchAttribute = requestAttributes.get(SCIMProviderConstants.SEARCH_ATTRIBUTE);
                String filter = requestAttributes.get(SCIMProviderConstants.FILTER);
                String startIndex = requestAttributes.get(SCIMProviderConstants.START_INDEX);
                String count = requestAttributes.get(SCIMProviderConstants.COUNT);
                String sortBy = requestAttributes.get(SCIMProviderConstants.SORT_BY);
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
            }

            return new JAXRSResponseBuilder().buildResponse(scimResponse);

        } catch (CharonException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            //create SCIM response with code as the same of exception and message as error message of the exception
            if (e.getCode() == -1) {
                e.setCode(ResponseCodeConstants.CODE_INTERNAL_SERVER_ERROR);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (FormatNotSupportedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        } catch (BadRequestException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
            return new JAXRSResponseBuilder().buildResponse(
                    AbstractResourceEndpoint.encodeSCIMException(encoder, e));
        }
    }
}
