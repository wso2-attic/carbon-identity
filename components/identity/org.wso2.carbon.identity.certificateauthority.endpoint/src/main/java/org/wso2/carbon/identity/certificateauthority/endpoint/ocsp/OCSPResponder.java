package org.wso2.carbon.identity.certificateauthority.endpoint.ocsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPResp;
import org.wso2.carbon.identity.certificateauthority.OCSPService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("/ocsp")
public class OCSPResponder {

    private static Log log = LogFactory.getLog(OCSPResponder.class);

    @POST
    @Path("/{tenantID}")
    @Consumes("application/ocsp-request")
    @Produces("application/ocsp-response")
    public Response handleOCSPRequest(@Context HttpServletRequest request, @PathParam("tenantID") String tenant) {
        try {
            int tenantID = Integer.parseInt(tenant);
            OCSPReq ocspReq = new OCSPReq(request.getInputStream());
            OCSPService ocspService = new OCSPService();
            OCSPResp ocspResp = ocspService.handleOCSPRequest(ocspReq, tenantID);
            return Response.ok().type("application/ocsp-response").entity(ocspResp.getEncoded()).build();
        } catch (IOException e) {
            log.error(e);
        } catch (OCSPException e) {
            log.error(e);
        }
        return Response.serverError().build();
    }
}
