/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.application.authenticator.fido.endpoint;

import com.yubico.u2f.data.messages.AuthenticateRequestData;
import com.yubico.u2f.data.messages.AuthenticateResponse;
import com.yubico.u2f.data.messages.RegisterRequestData;
import com.yubico.u2f.data.messages.RegisterResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.fido.dto.FIDOUser;
import org.wso2.carbon.identity.application.authenticator.fido.u2f.U2FService;
import org.wso2.carbon.identity.application.authenticator.fido.util.Util;
import org.wso2.carbon.identity.base.IdentityException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class FIDORestEndpoint {
	private static Log log = LogFactory.getLog(FIDORestEndpoint.class);
	private static U2FService u2FService = U2FService.getInstance();

	@POST
	@Path("/initiate_registration")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response initiateRegistration(@Context HttpServletRequest request){
		String appID = Util.getOrigin(request);
		String username = Util.getSafeText(request.getParameter("username"));
		FIDOUser user =  new FIDOUser(username, "", "", appID);
		try {
			RegisterRequestData registerRequestData = u2FService.startRegistration(user);
			return Response.ok(registerRequestData.toJson()).build();
		} catch (IdentityException e) {
			log.error("Could not generate challenge", e);
		}
		return Response.serverError().build();
	}

	@POST
	@Path("/complete_registration")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response completeRegistration(@FormParam("tokenResponse") String response,
	                                     @FormParam("username") String username){
		//String appID = Util.getOrigin(request);
		//String response = Util.getSafeText(request.getParameter("tokenResponse"));
		//String username = Util.getSafeText(request.getParameter("username"));
		//TODO
		FIDOUser user = new FIDOUser(username, "", "", RegisterResponse.fromJson(response));
		try {
			u2FService.finishRegistration(user);
			return Response.ok().build();
		} catch (IdentityException e) {
			log.error("Could not generate challenge", e);
		}
		return Response.serverError().build();
	}

	@POST
	@Path("/initiate_Authentication")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response initiateAuthentication(@Context HttpServletRequest request){
		String appID = Util.getOrigin(request);
		String username = Util.getSafeText(request.getParameter("username"));
		FIDOUser fidoUser = new FIDOUser(username, "", "", appID);
		try {
			AuthenticateRequestData authenticateRequestData = u2FService.startAuthentication(fidoUser);
			return Response.ok(authenticateRequestData.toJson()).build();
		} catch (AuthenticationFailedException e) {
			log.error("Could not generate challenge", e);
		}
		return Response.serverError().build();
	}

	@POST
	@Path("/complete_Authentication")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response completeAuthentication(@Context HttpServletRequest request){
		String appID = Util.getOrigin(request);
		String response = Util.getSafeText(request.getParameter("tokenResponse"));
		String username = Util.getSafeText(request.getParameter("username"));
		FIDOUser fidoUser = new FIDOUser(username, "", "", AuthenticateResponse.fromJson(response));
		fidoUser.setAppID(appID);
		try {
			u2FService.finishAuthentication(fidoUser);
			return Response.ok().build();
		} catch (AuthenticationFailedException e) {
			log.error("Could not generate challenge", e);
		}
		return Response.serverError().build();
	}
}
