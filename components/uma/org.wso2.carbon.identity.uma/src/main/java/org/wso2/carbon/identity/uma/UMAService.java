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
import org.wso2.carbon.identity.uma.dto.UmaRptRequest;
import org.wso2.carbon.identity.uma.dto.UmaRptResponse;

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
        UmaRptResponse umaRptResponse = new UmaRptResponse();
        umaRptResponse.setRPT("kdjfsdfhdfshjgsasdisdfyuwey83475y43undf4387437");

        return umaRptResponse;
    }


}
