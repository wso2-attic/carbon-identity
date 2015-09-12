/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.identity.sso.saml.query.builders;

import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.StatusCode;


public class SAMLAttributeQueryResponseBuilder extends AbstractAttributeQueryResponseBuilder{

    /**
     * Builds the <Response> element for the SAML Attribute Query received
     * @param attributeQuery
     * @return Returns the attribute query response as a <Response> element
     */
    public Response buildAttributeQueryResponse(RequestAbstractType attributeQuery){

        // get instance of a Response object filled with basic
        //
        super.buildAttributeQueryResponse(attributeQuery);


        // get instance of a <Status> element for the response
        response.getStatus().getStatusCode().setValue(StatusCode.SUCCESS_URI);


        // TODO fill the response with assertions using the processor




        return response;
    }

}
