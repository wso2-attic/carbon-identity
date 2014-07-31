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

package org.wso2.carbon.identity.certificateauthority.endpoint.ocsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPResp;
import org.wso2.carbon.identity.certificateauthority.OCSPService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
        } catch (Exception e) {
            log.error(e);
        }
        return Response.serverError().build();
    }
}
