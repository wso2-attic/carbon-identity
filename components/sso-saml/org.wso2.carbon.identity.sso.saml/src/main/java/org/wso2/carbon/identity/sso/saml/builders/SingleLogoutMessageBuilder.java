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
import org.apache.xml.security.signature.XMLSignature;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.SessionIndex;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml2.core.impl.LogoutResponseBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.SessionIndexBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.sso.saml.session.SessionInfoData;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;

public class SingleLogoutMessageBuilder {

    private static Log log = LogFactory.getLog(SingleLogoutMessageBuilder.class);

    static {
        SAMLSSOUtil.doBootstrap();
    }

    public LogoutRequest buildLogoutRequest(String subject, String sessionId, String reason,
                                            String destination, String nameIDFormat) throws IdentityException {
        LogoutRequest logoutReq = new LogoutRequestBuilder().buildObject();
        logoutReq.setID(SAMLSSOUtil.createID());

        DateTime issueInstant = new DateTime();
        logoutReq.setIssueInstant(issueInstant);
        logoutReq.setIssuer(SAMLSSOUtil.getIssuer());
        logoutReq.setNotOnOrAfter(new DateTime(issueInstant.getMillis() + 5 * 60 * 1000));

        NameID nameId = new NameIDBuilder().buildObject();
        nameId.setFormat(nameIDFormat);
        nameId.setValue(subject);
        logoutReq.setNameID(nameId);

        SessionIndex sessionIndex = new SessionIndexBuilder().buildObject();
        sessionIndex.setSessionIndex(sessionId);
        logoutReq.getSessionIndexes().add(sessionIndex);

        if (destination != null) {
            logoutReq.setDestination(destination);
        }

        logoutReq.setReason(reason);

        return logoutReq;
    }

    public LogoutResponse buildLogoutResponse(String id, String status, String statMsg, SessionInfoData sessionInfoData,
                                              boolean isDoSignResponse, String acsURL) throws IdentityException {

        //This generate logout response with destination parameter
        LogoutResponse logoutResp = new LogoutResponseBuilder().buildObject();
        logoutResp.setID(SAMLSSOUtil.createID());
        logoutResp.setInResponseTo(id);
        logoutResp.setIssuer(SAMLSSOUtil.getIssuer());
        logoutResp.setStatus(buildStatus(status, statMsg));
        logoutResp.setIssueInstant(new DateTime());
        logoutResp.setDestination(acsURL);
        if (isDoSignResponse && sessionInfoData != null) {
            SAMLSSOUtil.setSignature(logoutResp, XMLSignature.ALGO_ID_SIGNATURE_RSA, new SignKeyDataHolder(null));
        }

        return logoutResp;
    }

    public LogoutResponse buildLogoutResponse(String id, String status, String statMsg, SessionInfoData sessionInfoData,
                                              boolean isDoSignResponse) throws IdentityException {
        // This generate logout response without destination parameter
        LogoutResponse logoutResp = new LogoutResponseBuilder().buildObject();
        logoutResp.setID(SAMLSSOUtil.createID());
        logoutResp.setInResponseTo(id);
        logoutResp.setIssuer(SAMLSSOUtil.getIssuer());
        logoutResp.setStatus(buildStatus(status, statMsg));
        logoutResp.setIssueInstant(new DateTime());
        if (isDoSignResponse && sessionInfoData != null) {
            SAMLSSOUtil.setSignature(logoutResp, XMLSignature.ALGO_ID_SIGNATURE_RSA, new SignKeyDataHolder(null));
        }
        return logoutResp;
    }

    private Status buildStatus(String status, String statMsg) {

        Status stat = new StatusBuilder().buildObject();

        //Set the status code
        StatusCode statCode = new StatusCodeBuilder().buildObject();
        statCode.setValue(status);
        stat.setStatusCode(statCode);

        //Set the status Message
        if (statMsg != null) {
            StatusMessage statMesssage = new StatusMessageBuilder().buildObject();
            statMesssage.setMessage(statMsg);
            stat.setStatusMessage(statMesssage);
        }

        return stat;
    }

}
