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

package org.wso2.carbon.identity.certificateauthority.endpoint.crl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.certificateauthority.CRLService;
import org.wso2.carbon.identity.certificateauthority.Constants;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

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
            } catch (Exception e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId, e);
            }
        } else if (Constants.REQUEST_TYPE_DELTA_CRL.equals(command)) {
            //todo : fetch delta crl
            CRLService crlService = new CRLService();
            try {
                return Response.ok().type("application/pkix-crl").entity(crlService.getLatestDeltaCrl(tenantId)).build();
            } catch (Exception e) {
                log.error("error whilte trying to get CRL for the tenant :" + tenantId, e);
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).build();
    }


}
