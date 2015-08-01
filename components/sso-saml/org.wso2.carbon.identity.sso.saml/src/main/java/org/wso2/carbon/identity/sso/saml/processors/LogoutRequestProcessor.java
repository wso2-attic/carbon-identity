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
package org.wso2.carbon.identity.sso.saml.processors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.SessionIndex;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.builders.SingleLogoutMessageBuilder;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.dto.SingleLogoutRequestDTO;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.session.SessionInfoData;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Map;

public class LogoutRequestProcessor {

    private static Log log = LogFactory.getLog(LogoutRequestProcessor.class);

    /**
     * @param logoutRequest
     * @param sessionId
     * @param queryString
     * @return
     * @throws IdentityException
     */
    public SAMLSSOReqValidationResponseDTO process(LogoutRequest logoutRequest, String sessionId,
                                                   String queryString) throws IdentityException {

        try {
            SAMLSSOReqValidationResponseDTO reqValidationResponseDTO = new SAMLSSOReqValidationResponseDTO();
            reqValidationResponseDTO.setLogOutReq(true);

            String subject = null;
            String issuer = null;

            // Get the sessions from the SessionPersistenceManager and prepare
            // the logout responses
            SSOSessionPersistenceManager ssoSessionPersistenceManager = SSOSessionPersistenceManager
                    .getPersistenceManager();
            if (StringUtils.isBlank(sessionId)) {
                String message = "Session was already Expired";
                log.error(message);
                return buildErrorResponse(logoutRequest.getID(),
                                          SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, message,
                                          logoutRequest.getDestination());
            }
            String sessionIndex = ssoSessionPersistenceManager.getSessionIndexFromTokenId(sessionId);

            // Only if the logout request is received.
            if (logoutRequest != null) {
                if (logoutRequest.getIssuer() == null) {
                    String message = "Issuer should be mentioned in the Logout Request";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(),
                            SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, message, logoutRequest.getDestination());
                }

                // TODO : Check for BaseID and EncryptedID as well.
                if (logoutRequest.getNameID() != null) {
                    NameID nameID = logoutRequest.getNameID();
                    subject = nameID.getValue();
                } else {
                    String message = "Subject Name should be specified in the Logout Request";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(),
                            SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, message, logoutRequest.getDestination());
                }

                if (logoutRequest.getSessionIndexes() == null) {
                    String message = "At least one Session Index should be present in the Logout Request";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(),
                            SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, message, logoutRequest.getDestination());
                }

                SessionInfoData sessionInfoData = ssoSessionPersistenceManager.getSessionInfo(sessionIndex);

                if (sessionInfoData == null) {
                    String message = "No Established Sessions corresponding to Session Indexes provided.";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(),
                            SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, message, logoutRequest.getDestination());
                }

                issuer = logoutRequest.getIssuer().getValue();

                if (issuer.contains("@")) {
                    String[] splitIssuer = issuer.split("@");
                    if (StringUtils.isNotEmpty(splitIssuer[0]) && StringUtils.isNotEmpty(splitIssuer[1])) {
                        issuer = splitIssuer[0];
                        SAMLSSOUtil.setTenantDomainInThreadLocal(splitIssuer[1]);
                        if (log.isDebugEnabled()) {
                            log.debug("Tenant Domain :" + " " + splitIssuer[1] + " " + "&" + " " +
                                    "Issuer name :" + splitIssuer[0] + " " + "has being spilt");
                        }
                    } else {
                        SAMLSSOUtil.setTenantDomainInThreadLocal(
                                sessionInfoData.getServiceProviderList().get(issuer).getTenantDomain());
                    }
                } else {
                    SAMLSSOUtil.setTenantDomainInThreadLocal(
                            sessionInfoData.getServiceProviderList().get(issuer).getTenantDomain());
                }
                subject = sessionInfoData.getSubject(issuer);

                Map<String, SAMLSSOServiceProviderDO> sessionsList = sessionInfoData
                        .getServiceProviderList();
                SAMLSSOServiceProviderDO logoutReqIssuer = sessionsList.get(issuer);

                if (logoutReqIssuer.isDoSingleLogout()) {
                    //validate session index
                    SessionIndex requestSessionIndex = logoutRequest.getSessionIndexes().get(0);

                    if (requestSessionIndex == null || !sessionIndex.equals(requestSessionIndex.getSessionIndex())) {
                        String message = "Session Index validation for Logout Request failed. " +
                                "Received: [" + requestSessionIndex == null ? "null" : requestSessionIndex.getSessionIndex() +
                                "]." + " Expected: [" + sessionIndex + "]";
                        log.error(message);
                        return buildErrorResponse(logoutRequest.getID(),
                                SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                                message, logoutRequest.getDestination());
                    }
                }

                if (logoutReqIssuer.isDoValidateSignatureInRequests()) {

                    // Validate 'Destination'
                    String idpUrl = IdentityUtil.getProperty(IdentityConstants.ServerConfig.SSO_IDP_URL);

                    if (logoutRequest.getDestination() == null ||
                            !idpUrl.equals(logoutRequest.getDestination())) {
                        String message = "Destination validation for Logout Request failed. " +
                                "Received: [" + logoutRequest.getDestination() +
                                "]." + " Expected: [" + idpUrl + "]";
                        log.error(message);
                        return buildErrorResponse(logoutRequest.getID(),
                                SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                                message, logoutRequest.getDestination());
                    }

                    // Validate Signature
                    boolean isSignatureValid = SAMLSSOUtil.validateLogoutRequestSignature(logoutRequest,
                            logoutReqIssuer.getCertAlias(),
                            subject,
                            queryString);
                    if (!isSignatureValid) {
                        String message = "Signature validation for Logout Request failed";
                        log.error(message);
                        return buildErrorResponse(logoutRequest.getID(),
                                SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                                message, logoutRequest.getDestination());
                    }
                }

                SingleLogoutMessageBuilder logoutMsgBuilder = new SingleLogoutMessageBuilder();
                Map<String, String> rpSessionsList = sessionInfoData.getRPSessionsList();
                SingleLogoutRequestDTO[] singleLogoutReqDTOs = new SingleLogoutRequestDTO[sessionsList
                        .size() - 1];
                int index = 0;
                for (Map.Entry<String, SAMLSSOServiceProviderDO> entry : sessionsList.entrySet()) {
                    String key = entry.getKey();
                    SAMLSSOServiceProviderDO value = entry.getValue();
                    if (!key.equals(issuer)) {
                        SingleLogoutRequestDTO logoutReqDTO = new SingleLogoutRequestDTO();
                        logoutReqDTO.setAssertionConsumerURL(value.getLogoutURL());
                        if (StringUtils.isBlank(value.getLogoutURL())) {
                            logoutReqDTO.setAssertionConsumerURL(value.getAssertionConsumerUrl());
                        }
                        LogoutRequest logoutReq = logoutMsgBuilder.buildLogoutRequest(subject, sessionIndex,
                                SAMLSSOConstants.SingleLogoutCodes.LOGOUT_USER, logoutReqDTO.getAssertionConsumerURL(), value.getNameIDFormat());
                        logoutReq.setIssuer(SAMLSSOUtil.getIssuer());
                        String logoutReqString = SAMLSSOUtil.encode(SAMLSSOUtil.marshall(logoutReq));
                        logoutReqDTO.setLogoutResponse(logoutReqString);
                        logoutReqDTO.setRpSessionId(rpSessionsList.get(key));
                        singleLogoutReqDTOs[index] = logoutReqDTO;
                        index++;
                    } else {
                        reqValidationResponseDTO.setIssuer(value.getIssuer());
                        reqValidationResponseDTO.setAssertionConsumerURL(value.getAssertionConsumerUrl());
                        if (!StringUtils.isBlank(value.getLogoutURL())) {
                            reqValidationResponseDTO.setAssertionConsumerURL(value.getLogoutURL());
                        }
                    }
                }
                reqValidationResponseDTO.setLogoutRespDTO(singleLogoutReqDTOs);

                String tenantDomain = logoutReqIssuer.getTenantDomain();
                if(StringUtils.isEmpty(tenantDomain)){
                    tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
                }
                try {
                    int tenantId = SAMLSSOUtil.getRealmService().getTenantManager()
                            .getTenantId(tenantDomain);
                    PrivilegedCarbonContext.startTenantFlow();
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
                    LogoutResponse logoutResponse = logoutMsgBuilder.buildLogoutResponse(
                            logoutRequest.getID(), SAMLSSOConstants.StatusCodes.SUCCESS_CODE, null,
                            sessionInfoData, logoutReqIssuer.isDoSignResponse(),
                            reqValidationResponseDTO.getAssertionConsumerURL());
                    reqValidationResponseDTO.setLogoutResponse(SAMLSSOUtil.encode(SAMLSSOUtil
                            .marshall(logoutResponse)));
                    reqValidationResponseDTO.setValid(true);
                } finally {
                    PrivilegedCarbonContext.endTenantFlow();
                }
            }

            return reqValidationResponseDTO;
        } catch (Exception e) {
            log.error("Error Processing the Logout Request", e);
            throw new IdentityException("Error Processing the Logout Request", e);
        }
    }

    private SAMLSSOReqValidationResponseDTO buildErrorResponse(String id, String status,
                                                               String statMsg, String destination) throws Exception {
        SAMLSSOReqValidationResponseDTO reqValidationResponseDTO = new SAMLSSOReqValidationResponseDTO();
        LogoutResponse logoutResp = new SingleLogoutMessageBuilder().buildLogoutResponse(id,
                status, statMsg, null, false, destination);
        reqValidationResponseDTO.setLogOutReq(true);
        reqValidationResponseDTO.setValid(false);
        reqValidationResponseDTO.setResponse(SAMLSSOUtil.compressResponse(SAMLSSOUtil.marshall(logoutResp)));
        return reqValidationResponseDTO;
    }
}
