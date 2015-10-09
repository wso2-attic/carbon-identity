/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.carbon.identity.totp.TOTPDTO;
import org.wso2.carbon.identity.totp.TOTPManager;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import org.wso2.carbon.identity.application.common.util.CharacterEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/keygen")
public class TOTPKeyGeneratorEndpoint {

	private static Log log = LogFactory.getLog(TOTPKeyGeneratorEndpoint.class);

	@POST
	@Path("/local")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	public Response totpKeyGenLocal(@Context HttpServletRequest request) {

		TOTPManager totpManager = (TOTPManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService
				(TOTPManager.class);
		if (totpManager != null) {
			String username = CharacterEncoder.getSafeText(request.getParameter("username"));
			TOTPDTO totpdto = null;
			try {
				totpdto = totpManager.generateTOTPKeyLocal(username);
				String json = createJsonPayload(totpdto.getSecretkey(), totpdto.getQRCodeURL());
				return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
			} catch (TOTPException e) {
				log.error("TOTPKeyGenerator endpoint could not generate keys for local user", e);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error when generating the key")
						.type(MediaType.APPLICATION_JSON_TYPE).build();
			}
		} else {
			log.error("TOTPKeyGenerator endpoint could not generate keys for local user");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error when generating the key")
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}


	@POST
	@Path("/external")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	public Response totpKeyGenExternal(@Context HttpServletRequest request) {

		TOTPManager totpManager = (TOTPManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService
				(TOTPManager.class);
		if (totpManager != null) {
			String username = CharacterEncoder.getSafeText(request.getParameter("username"));
			TOTPDTO totpdto = null;
			try {
				totpdto = totpManager.generateTOTPKey(username);
				String json = createJsonPayload(totpdto.getSecretkey(), totpdto.getQRCodeURL());
				return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
			} catch (TOTPException e) {
				log.error("TOTPKeyGenerator endpoint could not generate the key for the user : " + username,e);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error when generating the key")
						.type(MediaType.APPLICATION_JSON_TYPE).build();
			}
			
		} else {
			log.error("TOTPKeyGenerator endpoint could not generate the key");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error when generating the key")
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}


	private String createJsonPayload(String secretKey, String qrCodeURL) {

		return "{\"secretkey\":\"" + secretKey + "\"," +
		       "\"qrcode\":\"" + qrCodeURL + "\"}";
	}


}
