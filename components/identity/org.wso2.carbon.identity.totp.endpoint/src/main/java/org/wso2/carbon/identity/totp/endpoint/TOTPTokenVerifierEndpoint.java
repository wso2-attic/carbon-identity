/*
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.totp.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.totp.TOTPManager;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/verify")
public class TOTPTokenVerifierEndpoint {

    private static Log log = LogFactory.getLog(TOTPTokenVerifierEndpoint.class);


    @GET
    @Path("/local")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response isValidToken(@Context HttpServletRequest request){

        TOTPManager totpManager = (TOTPManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService(TOTPManager.class);
        String username = request.getParameter("username");
        int token = Integer.parseInt(request.getParameter("token"));
        boolean isvalid = false;
        try {
            isvalid = totpManager.isValidTokenLocalUser(token, username);
            if(isvalid){
                log.info("TOTP token is accepted");
                return Response.status(Response.Status.ACCEPTED).type(MediaType.APPLICATION_JSON_TYPE)
                        .entity("Token is verified").build();
            }else{
                log.info("TOTP token is not valid");
                return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                        .entity("Token is not valid").build();
            }
        } catch (TOTPException e) {
            log.error("Error when validating the token",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity("Error while validating the token").build();
        }
        
    }

    @GET
    @Path("/external")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response isValidTokenByKey(@Context HttpServletRequest request){

        TOTPManager totpManager = (TOTPManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService(TOTPManager.class);
        String secretKey = request.getParameter("secretKey");
        int token = Integer.parseInt(request.getParameter("token"));
        if(totpManager.isValidToken(token, secretKey)){
            log.info("TOTP token is accepted");
            return Response.status(Response.Status.ACCEPTED).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity("Token is a verified").build();
        }else{
            log.info("TOTP token is not valid");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity("Token is not valid").build();
        }
    }

}