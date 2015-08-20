/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
package org.wso2.carbon.identity.sts.passive;

import org.wso2.carbon.identity.sts.passive.processors.AttributeRequestProcessor;
import org.wso2.carbon.identity.sts.passive.processors.PseudonymRequestProcessor;
import org.wso2.carbon.identity.sts.passive.processors.RequestProcessor;
import org.wso2.carbon.identity.sts.passive.processors.SigningRequestProcessor;
import org.wso2.carbon.identity.sts.passive.processors.SignoutRequestProcessor;

public class RequestProcessorFactory {

    private static RequestProcessorFactory factory = new RequestProcessorFactory();

    private RequestProcessorFactory() {
    }

    public static RequestProcessorFactory getInstance() {
        return factory;
    }

    public RequestProcessor getRequestProcessor(String action) {

        if (PassiveRequestorConstants.REQUESTOR_ACTION_SIGNIN_10.equals(action)) {
            return new SigningRequestProcessor();
        }

        if (PassiveRequestorConstants.REQUESTOR_ACTION_SIGNOUT_10.equals(action)) {
            return new SignoutRequestProcessor();
        }

        if (PassiveRequestorConstants.REQUESTOR_ACTION_ATTRIBUTES_10.equals(action)) {
            return new AttributeRequestProcessor();
        }

        if (PassiveRequestorConstants.REQUESTOR_ACTION_PSEUDONYM_10.equals(action)) {
            return new PseudonymRequestProcessor();
        }

        return null;
    }
}
