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

import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;


public abstract class AbstractAttributeQueryResponseBuilder {
    Response response;


    public Response buildAttributeQueryResponse(RequestAbstractType attributeQuery){
        response = createEmptyResponse();

        // set the required attributes of the <Response> element :- Issuer, Version, IssueInstant
        response.setID(SAMLSSOUtil.createID());
        response.setVersion(SAMLVersion.VERSION_20);
        response.setIssueInstant(new DateTime());

        // set optional attributes of the <Response> element:- InResponseTo,Destination,Consent
        response.setInResponseTo(attributeQuery.getID());
        // TODO response.setDestination();
        response.setConsent(attributeQuery.getConsent());

        // set the issuer element

        // set the signature of the SAML Authority


        return response;
    }

    /**
     * Method to create an empty <Response> object
     * @return
     */
    private Response createEmptyResponse(){
        Response response = new ResponseBuilder().buildObject();
        Status status = new StatusBuilder().buildObject();
        StatusCode statusCode = new StatusCodeBuilder().buildObject();
        status.setStatusCode(statusCode);
        response.setStatus(status);
        return response;
    }





}
