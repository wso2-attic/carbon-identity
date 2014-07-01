package org.wso2.carbon.identity.certificateauthority.endpoint.ocsp;

import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPResp;
import org.wso2.carbon.identity.certificateauthority.CRLService;
import org.wso2.carbon.identity.certificateauthority.OCSPService;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;

@Path("/ocsp")
public class OCSPResponder {

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
            return Response.ok().type("application/ocsp-response").entity(new CRLService().getLatestCrl(tenantID)).build();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OCSPException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CRLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CertAuthException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return Response.serverError().build();
    }
}
