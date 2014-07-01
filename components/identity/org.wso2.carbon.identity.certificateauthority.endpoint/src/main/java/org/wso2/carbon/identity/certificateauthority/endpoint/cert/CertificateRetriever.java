package org.wso2.carbon.identity.certificateauthority.endpoint.cert;

import org.apache.log4j.Logger;
import org.wso2.carbon.identity.certificateauthority.CertificateService;
import org.wso2.carbon.identity.certificateauthority.data.CertAuthException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/certificate")
public class CertificateRetriever {
    protected static Logger log = Logger.getLogger(CertificateRetriever.class);

    @GET
    @Path("/{serial}.crt")
    @Produces("application/octet-string")
    public Response getCertificate(@PathParam("serial") String serial) {
        CertificateService service = new CertificateService();
        try {
            String certificate = service.getCertificate(serial);
            return Response.ok().type("application/octet-string").entity(certificate).build();
        } catch (CertAuthException e) {
            log.error("Error occurred retrieving certificate", e);
            return Response.serverError().build();
        }
    }
}
