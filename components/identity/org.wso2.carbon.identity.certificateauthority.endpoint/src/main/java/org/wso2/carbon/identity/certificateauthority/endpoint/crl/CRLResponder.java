package org.wso2.carbon.identity.certificateauthority.endpoint.crl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.certificateauthority.CRLService;
import org.wso2.carbon.identity.certificateauthority.Constants;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;

@Path("/crl")
public class CRLResponder {
    Log log = LogFactory.getLog(CRLResponder.class);

    @GET
    @Path("/{tenantID}")
    @Produces("application/pkix-crl")
    public Response getCRL(@QueryParam(Constants.CRL_COMMAND) String command, @PathParam("tenantID") int tenantId) {
        if (Constants.REQUEST_TYPE_CRL.equals(command)) {
            CRLService crlService = new CRLService();
            try {
                byte[] crlBytes = crlService.getLatestCrl(tenantId);
                return Response.ok().type("application/pkix-crl").entity(crlBytes).build();
            } catch (CertificateException e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId);
            } catch (CertAuthException e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId);
            } catch (CRLException e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId);
            }
        } else if (Constants.REQUEST_TYPE_DELTA_CRL.equals(command)) {
            //todo : fetch delta crl
            CRLService crlService = new CRLService();
            try {
                return Response.ok().type("application/pkix-crl").entity(crlService.getLatestDeltaCrl(tenantId)).build();
            } catch (CertificateException e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId);
            } catch (CertAuthException e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId);
            } catch (CRLException e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId);
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }


}
