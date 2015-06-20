/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * /
 */

package org.wso2.carbon.identity.uma.endpoint.authorization;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.uma.endpoint.UmaRequestWrapper;
import org.wso2.carbon.identity.uma.endpoint.beans.UmaRptRequestPayloadBean;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Path("/rpt")
public class UmaAuthorizationEndpoint {

    private static final Log log = LogFactory.getLog(UmaAuthorizationEndpoint.class);

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public Response issueRPT(@Context HttpServletRequest request,UmaRptRequestPayloadBean payloadBean){

        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);


        HttpServletRequestWrapper httpServletRequestWrapper =
                new UmaRequestWrapper(request,new MetadataMap<String,String>());

        // log the RPT Token Request
        if(log.isDebugEnabled()){
            logRptRequest(httpServletRequestWrapper);
        }

        return null;
    }


    private void logRptRequest(HttpServletRequest httpServletRequest){

        StringBuilder builder = new StringBuilder();


    }
}
