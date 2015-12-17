/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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
 */
package org.wso2.carbon.identity.sso.saml.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

import java.util.List;

public class ErrorResponseBuilder {

    private static Log log = LogFactory.getLog(ErrorResponseBuilder.class);
    private Response response;

    //Do the bootstrap first
    static {
        SAMLSSOUtil.doBootstrap();
    }


    public ErrorResponseBuilder() {
        ResponseBuilder responseBuilder = new ResponseBuilder();
        this.response = responseBuilder.buildObject();
    }

    /**
     * Build the error response
     *
     * @param inResponseToID
     * @param statusCodes
     * @param statusMsg
     * @return
     */
    public Response buildResponse(String inResponseToID, List<String> statusCodes, String statusMsg,
                                  String destination) throws IdentityException {
        if (statusCodes == null || statusCodes.isEmpty()) {
            throw IdentityException.error("No Status Values");
        }
        response.setIssuer(SAMLSSOUtil.getIssuer());
        Status status = new StatusBuilder().buildObject();
        StatusCode statusCode = null;
        for (String statCode : statusCodes) {
            statusCode = buildStatusCode(statCode, statusCode);
        }
        status.setStatusCode(statusCode);
        buildStatusMsg(status, statusMsg);
        response.setStatus(status);
        response.setVersion(SAMLVersion.VERSION_20);
        response.setID(SAMLSSOUtil.createID());
        if (inResponseToID != null) {
            response.setInResponseTo(inResponseToID);
        }
        if (destination != null) {
            response.setDestination(destination);
        }
        response.setIssueInstant(new DateTime());
        return response;
    }

    /**
     * Build the StatusCode for Status of Response
     *
     * @param parentStatusCode
     * @param childStatusCode
     * @return
     */
    private StatusCode buildStatusCode(String parentStatusCode, StatusCode childStatusCode) throws IdentityException {

        if (parentStatusCode == null) {
            throw IdentityException.error("Invalid SAML Response Status Code");
        }

        StatusCode statusCode = new StatusCodeBuilder().buildObject();
        statusCode.setValue(parentStatusCode);

        //Set the status Message
        if (childStatusCode != null) {
            statusCode.setStatusCode(childStatusCode);
            return statusCode;
        } else {
            return statusCode;
        }
    }

    /**
     * Set the StatusMessage for Status of Response
     *
     * @param statusMsg
     * @return
     */
    private Status buildStatusMsg(Status status, String statusMsg) {
        if (statusMsg != null) {
            StatusMessage statusMesssage = new StatusMessageBuilder().buildObject();
            statusMesssage.setMessage(statusMsg);
            status.setStatusMessage(statusMesssage);
        }
        return status;
    }

}
