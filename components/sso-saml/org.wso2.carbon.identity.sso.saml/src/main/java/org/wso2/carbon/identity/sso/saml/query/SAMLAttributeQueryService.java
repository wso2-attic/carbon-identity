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

package org.wso2.carbon.identity.sso.saml.query;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.AssertionIDRequest;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.xml.XMLObject;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.query.builders.SAMLAttributeQueryResponseBuilder;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

import javax.xml.stream.XMLStreamException;


/**
 *  Service that handlers SAML attribute queries/requests
 */
public class SAMLAttributeQueryService {
    private static Log log = LogFactory.getLog(SAMLAttributeQueryService.class);

    /**
     * Method to handle Soap Messages with <AttributeQuery> element
     * @param omElement
     * @return Response for the AttributeQuery
     */
    public OMElement AttributeQuery(OMElement omElement) throws IdentityException, XMLStreamException {
        String xmlString = omElement.toString();
        if(log.isDebugEnabled()){
            log.debug("Received AttributeQuery: " + xmlString);
        }

        AttributeQuery attributeQuery = (AttributeQuery) SAMLSSOUtil.unmarshall(xmlString);


        SAMLAttributeQueryResponseBuilder responseBuilder = new SAMLAttributeQueryResponseBuilder();
        XMLObject attributeQueryResponse = responseBuilder.buildAttributeQueryResponse(attributeQuery);

        String responseXML = SAMLSSOUtil.marshall(attributeQueryResponse);
        OMElement responseOMElement = AXIOMUtil.stringToOM(responseXML);

        return responseOMElement;
    }

    // AssertionIDRequest
    public OMElement AssertionIDRequest(OMElement omElement) throws IdentityException {

        String xmlString = omElement.toString();

        if(log.isDebugEnabled()){
            log.debug("Received AssertionID Request: " + xmlString);
        }

        AssertionIDRequest assertionIDRequest = (AssertionIDRequest) SAMLSSOUtil.unmarshall(xmlString);
       // assertionIDRequest.validate();
        return omElement;
    }


    // SubjectQuery
    public OMElement SubjectQuery(OMElement omElement){
        if(log.isDebugEnabled()){
            log.debug("Received SubjectQuery: " + omElement.toString());
        }
        return omElement;
    }

    // AuthnQuery
    public OMElement AuthnQuery(OMElement omElement){
        if(log.isDebugEnabled()){
            log.debug("Received AuthnQuery: " + omElement.toString());
        }
        return omElement;
    }



    // AuthzDecisionQuery
    public OMElement AuthzDecisionQuery(OMElement omElement){
        if(log.isDebugEnabled()){
            log.debug("Received AuthzDecisionQuery: " + omElement.toString());
        }
        return omElement;
    }


}
