/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.certificateauthority.endpoint.cert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.certificateauthority.CaException;
import org.wso2.carbon.identity.certificateauthority.CertificateService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/certificate")
public class CertificateRetriever {
    private static final Log log = LogFactory.getLog(CertificateRetriever.class);

    @GET
    @Path("/{serial}.crt")
    @Produces("application/octet-string")
    public Response getCertificate(@PathParam("serial") String serial) {
        CertificateService service = new CertificateService();
        try {
            String certificate = service.getCertificate(serial);
            return Response.ok().type("application/x-x509-user-cert").entity(certificate).build();
        } catch (CaException e) {
            log.error("Error occurred retrieving certificate", e);
            return Response.serverError().build();
        }
    }
}
