package org.wso2.carbon.identity.totp.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.totp.TOTPManager;
import org.wso2.carbon.identity.totp.exception.TOTPException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/tokengen")
public class TOTPTokenGeneratorEndpoint {

	private static Log log = LogFactory.getLog(TOTPTokenGeneratorEndpoint.class);

	@POST
	@Path("/local")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	public Response generateBuyUsername(@Context HttpServletRequest request) {

		TOTPManager totpManager = (TOTPManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService
				(TOTPManager.class);
		String username = request.getParameter("username");
		String token = "";
		try {
			token = totpManager.generateTOTPTokenLocal(username);
			return Response.status(Response.Status.CREATED).build();
		} catch (TOTPException e) {
			log.error("TOTPKeyGenerator endpoint could not generate the token", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error when generating the token")
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}

	@POST
	@Path("/external")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json")
	public Response generateBuyKey(@Context HttpServletRequest request) {

		TOTPManager totpManager = (TOTPManager) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService
				(TOTPManager.class);
		String secretKey = request.getParameter("secretKey");
		String token = null;
		try {
			token = totpManager.generateTOTPToken(secretKey);
			return Response.ok("{\"token\":\"" + token + "\"}", MediaType.APPLICATION_JSON_TYPE).build();
		} catch (TOTPException e) {
			log.error("TOTPKeyGenerator endpoint could not generate the token", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error when generating the token")
					.type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}


}
