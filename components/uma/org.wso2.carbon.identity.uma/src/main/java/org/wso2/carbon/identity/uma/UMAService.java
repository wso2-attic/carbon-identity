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

package org.wso2.carbon.identity.uma;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.dao.ResourceSetMgtDAO;
import org.wso2.carbon.identity.uma.dto.*;
import org.wso2.carbon.identity.uma.exceptions.IdentityUMAException;
import org.wso2.carbon.identity.uma.model.ResourceSetDO;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

public class UMAService {

    private static final Log log = LogFactory.getLog(UMAService.class);

    // method to issue the RPT after validating the claims
    public UmaRptResponse issueRPT(UmaRptRequest rptRequest){

        if (log.isDebugEnabled()){
            log.debug("Request Processed by the UMAService");
        }

        // validate the permission ticket


        // retrieve the permission sets associated with the ticket ( resourceID, scopes, user consent type)


        // create the message context fill the UMARptRequest with



        // select the handlers to handle the user consent type
        // if more than one select one with higher priority



        // get the response from the handlers and send to back
        UmaRptResponse umaRptResponse = new UmaRptResponse(HttpServletResponse.SC_OK);
        umaRptResponse.setRPT("kdjfsdfhdfshjgsasdisdfyuwey83475y43undf4387437");

        return umaRptResponse;
    }


    public UmaResponse createResourceSet
            (UmaResourceSetRegistrationRequest umaResourceSetRegistrationRequest){

        UmaResponse.UmaResponseBuilder builder =  null;

        // generate a unique string
        String resourceSetID = UUID.randomUUID().toString();

        ResourceSetDO resourceSetDO = new ResourceSetDO(
                umaResourceSetRegistrationRequest.getResourceSetName(),
                umaResourceSetRegistrationRequest.getResourceSetURI(),
                umaResourceSetRegistrationRequest.getResourceSetType(),
                umaResourceSetRegistrationRequest.getResourceSetScopes(),
                umaResourceSetRegistrationRequest.getResouceSetIconURI()
        );

        resourceSetDO.setConsumerKey(umaResourceSetRegistrationRequest.getConsumerKey());
        resourceSetDO.setResourceSetId(resourceSetID);

        ResourceSetMgtDAO resourceSetMgtDAO = new ResourceSetMgtDAO();

        try {
            resourceSetMgtDAO.saveResourceSetDescription(resourceSetDO, null,resourceSetDO.getConsumerKey(),null);

             builder = UmaResourceSetRegistrationResponse.status(HttpServletResponse.SC_OK)
                            .setParam(UMAConstants.OAuthResourceRegistration.RESOURCE_REG_RESPONSE_ID,
                                    resourceSetDO.getResourceSetId());

        } catch (IdentityUMAException e) {
           log.error(e.getMessage(),e);

            builder =  UmaResourceSetRegistrationResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                            .setError("invalid_request");

        }finally {

            return builder.buildJSONResponse();
        }

    }

    public UmaResourceSetRegistrationResponse updateResourceSet
            (UmaResourceSetRegistrationRequest umaResourceSetRegistrationRequest){

        return null;
    }

    public UmaResourceSetRegistrationResponse deleteResourceSet
            (UmaResourceSetRegistrationRequest umaResourceSetRegistrationRequest){
        return null;
    }




}
