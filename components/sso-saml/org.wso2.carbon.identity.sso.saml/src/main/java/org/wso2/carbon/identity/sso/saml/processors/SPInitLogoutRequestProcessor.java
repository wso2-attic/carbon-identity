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
import org.wso2.carbon.identity.application.common.util.IdentityApplicationManagementUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.builders.SingleLogoutMessageBuilder;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.dto.SingleLogoutRequestDTO;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.session.SessionInfoData;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.user.api.UserStoreException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SPInitLogoutRequestProcessor {

    private static Log log = LogFactory.getLog(SPInitLogoutRequestProcessor.class);

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
            String defaultSigningAlgoUri = IdentityApplicationManagementUtil.getSigningAlgoURIByConfig();
            String defaultDigestAlgoUri = IdentityApplicationManagementUtil.getDigestAlgoURIByConfig();

            // Get the sessions from the SessionPersistenceManager and prepare
            // the logout responses
            SSOSessionPersistenceManager ssoSessionPersistenceManager = SSOSessionPersistenceManager
                    .getPersistenceManager();
            if (StringUtils.isBlank(sessionId)) {
                String message = "Session was already Expired";
                log.error(message);
                return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                        message, logoutRequest.getDestination(), defaultSigningAlgoUri, defaultDigestAlgoUri);
            }
            String sessionIndex = ssoSessionPersistenceManager.getSessionIndexFromTokenId(sessionId);

            if (StringUtils.isBlank(sessionId)) {
                String message = "Session index value not found in the request";
                log.error(message);
                reqValidationResponseDTO = buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes
                        .REQUESTOR_ERROR, message, null, defaultSigningAlgoUri, defaultDigestAlgoUri);
                reqValidationResponseDTO.setLogoutFromAuthFramework(true);
                return reqValidationResponseDTO;
            }
            // Only if the logout request is received.
            if (logoutRequest != null) {
                if (logoutRequest.getIssuer() == null) {
                    String message = "Issuer should be mentioned in the Logout Request";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                            message, logoutRequest.getDestination(), defaultSigningAlgoUri, defaultDigestAlgoUri);
                }

                // TODO : Check for BaseID and EncryptedID as well.
                if (logoutRequest.getNameID() != null) {
                    NameID nameID = logoutRequest.getNameID();
                    subject = nameID.getValue();
                } else {
                    String message = "Subject Name should be specified in the Logout Request";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                            message, logoutRequest.getDestination(), defaultSigningAlgoUri, defaultDigestAlgoUri);
                }

                if (logoutRequest.getSessionIndexes() == null) {
                    String message = "At least one Session Index should be present in the Logout Request";
                    log.error(message);
                    return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR,
                            message, logoutRequest.getDestination(), defaultSigningAlgoUri, defaultDigestAlgoUri);
                }

                SessionInfoData sessionInfoData = ssoSessionPersistenceManager.getSessionInfo(sessionIndex);

                if (sessionInfoData == null) {
                    String message = "No Established Sessions corresponding to Session Indexes provided.";
                    log.error(message);
                    reqValidationResponseDTO = buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes
                            .REQUESTOR_ERROR, message, null, defaultSigningAlgoUri, defaultDigestAlgoUri);
                    reqValidationResponseDTO.setLogoutFromAuthFramework(true);
                    return reqValidationResponseDTO;
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
                        SAMLSSOServiceProviderDO serviceProvider = sessionInfoData.getServiceProviderList().get(issuer);
                        if (serviceProvider != null) {
                            SAMLSSOUtil.setTenantDomainInThreadLocal(
                                    sessionInfoData.getServiceProviderList().get(issuer).getTenantDomain());
                        } else {
                            throw new IdentityException("Service provider :" + issuer + " does not exist in session " +
                                    "info data.");
                        }
                    }
                } else {
                    SAMLSSOServiceProviderDO serviceProvider = sessionInfoData.getServiceProviderList().get(issuer);
                    if (serviceProvider != null) {
                        SAMLSSOUtil.setTenantDomainInThreadLocal(
                                sessionInfoData.getServiceProviderList().get(issuer).getTenantDomain());
                    } else {
                        throw new IdentityException("Service provider :" + issuer + " does not exist in session info " +
                                "data.");
                    }
                }
                subject = sessionInfoData.getSubject(issuer);

                Map<String, SAMLSSOServiceProviderDO> sessionsList = sessionInfoData
                        .getServiceProviderList();
                SAMLSSOServiceProviderDO logoutReqIssuer = sessionsList.get(issuer);

                if (logoutReqIssuer.isDoSingleLogout()) {
                    //validate session index
                    SessionIndex requestSessionIndex = logoutRequest.getSessionIndexes().size() > 0 ? logoutRequest
                            .getSessionIndexes().get(0) : null;

                    if (requestSessionIndex == null || !sessionIndex.equals(requestSessionIndex.getSessionIndex())) {
                        String message = "Session Index validation for Logout Request failed. " +
                                         "Received: [" + (requestSessionIndex == null ? "null" : requestSessionIndex
                                .getSessionIndex()) + "]." + " Expected: [" + sessionIndex + "]";
                        log.error(message);
                        return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes
                                .REQUESTOR_ERROR, message, logoutRequest.getDestination(), logoutReqIssuer
                                .getSigningAlgorithmUri(), logoutReqIssuer.getDigestAlgorithmUri());
                    }
                }

                if (logoutReqIssuer.isDoValidateSignatureInRequests()) {

                    // Validate 'Destination'
                    List<String> idpUrlSet = SAMLSSOUtil.getDestinationFromTenantDomain(SAMLSSOUtil
                            .getTenantDomainFromThreadLocal());

                    if (logoutRequest.getDestination() == null
                            || !idpUrlSet.contains(logoutRequest.getDestination())) {
                        String message = "Destination validation for Logout Request failed. " +
                                "Received: [" + logoutRequest.getDestination() +
                                "]." + " Expected: [" + StringUtils.join(idpUrlSet, ',') + "]";
                        log.error(message);
                        return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes
                                .REQUESTOR_ERROR, message, logoutRequest.getDestination(), logoutReqIssuer
                                .getSigningAlgorithmUri(), logoutReqIssuer.getDigestAlgorithmUri());
                    }

                    // Validate Signature
                    boolean isSignatureValid = SAMLSSOUtil.validateLogoutRequestSignature(logoutRequest,
                            logoutReqIssuer.getCertAlias(),
                            subject,
                            queryString);
                    if (!isSignatureValid) {
                        String message = "Signature validation for Logout Request failed";
                        log.error(message);
                        return buildErrorResponse(logoutRequest.getID(), SAMLSSOConstants.StatusCodes
                                .REQUESTOR_ERROR, message, logoutRequest.getDestination(), logoutReqIssuer
                                .getSigningAlgorithmUri(), logoutReqIssuer.getDigestAlgorithmUri());
                    }
                }

                SingleLogoutMessageBuilder logoutMsgBuilder = new SingleLogoutMessageBuilder();
                Map<String, String> rpSessionsList = sessionInfoData.getRPSessionsList();
                List<SingleLogoutRequestDTO> singleLogoutReqDTOs = new ArrayList<SingleLogoutRequestDTO>();

                for (Map.Entry<String, SAMLSSOServiceProviderDO> entry : sessionsList.entrySet()) {
                    String key = entry.getKey();
                    SAMLSSOServiceProviderDO value = entry.getValue();

                    if (!key.equals(issuer)) {
                        SingleLogoutRequestDTO logoutReqDTO = new SingleLogoutRequestDTO();
                        if (StringUtils.isNotBlank(value.getSloRequestURL())) {
                            logoutReqDTO.setAssertionConsumerURL(value.getSloRequestURL());
                        } else if (StringUtils.isNotBlank(value.getSloResponseURL())) {
                            logoutReqDTO.setAssertionConsumerURL(value.getSloResponseURL());
                        } else {
                            logoutReqDTO.setAssertionConsumerURL(value.getAssertionConsumerUrl());
                        }

                        LogoutRequest logoutReq = logoutMsgBuilder.buildLogoutRequest(sessionInfoData.getSubject(key)
                                , sessionIndex, SAMLSSOConstants.SingleLogoutCodes.LOGOUT_USER, logoutReqDTO
                                .getAssertionConsumerURL(), value.getNameIDFormat(), value.getTenantDomain(), value
                                .getSigningAlgorithmUri(), value.getDigestAlgorithmUri());
                        String logoutReqString = SAMLSSOUtil.encode(SAMLSSOUtil.marshall(logoutReq));
                        logoutReqDTO.setLogoutResponse(logoutReqString);
                        logoutReqDTO.setRpSessionId(rpSessionsList.get(key));
                        singleLogoutReqDTOs.add(logoutReqDTO);
                    } else {
                        reqValidationResponseDTO.setIssuer(value.getIssuer());
                        reqValidationResponseDTO.setDoSignResponse(value.isDoSignResponse());
                        reqValidationResponseDTO.setSigningAlgorithmUri(value.getSigningAlgorithmUri());
                        reqValidationResponseDTO.setDigestAlgorithmUri(value.getDigestAlgorithmUri());
                        if (StringUtils.isNotBlank(value.getSloResponseURL())) {
                            reqValidationResponseDTO.setAssertionConsumerURL(value.getSloResponseURL());
                        } else {
                            reqValidationResponseDTO.setAssertionConsumerURL(value.getAssertionConsumerUrl());
                        }
                    }
                }

                reqValidationResponseDTO.setLogoutRespDTO(singleLogoutReqDTOs.toArray(
                        new SingleLogoutRequestDTO[singleLogoutReqDTOs.size()]));

                LogoutResponse logoutResponse = logoutMsgBuilder.buildLogoutResponse(
                        logoutRequest.getID(),
                        SAMLSSOConstants.StatusCodes.SUCCESS_CODE,
                        null,
                        reqValidationResponseDTO.getAssertionConsumerURL(),
                        reqValidationResponseDTO.isDoSignResponse(),
                        SAMLSSOUtil.getTenantDomainFromThreadLocal(),
                        reqValidationResponseDTO.getSigningAlgorithmUri(),
                        reqValidationResponseDTO.getDigestAlgorithmUri());

                reqValidationResponseDTO.setLogoutResponse(SAMLSSOUtil.encode(SAMLSSOUtil.marshall(logoutResponse)));
                reqValidationResponseDTO.setValid(true);
            }

            return reqValidationResponseDTO;
        } catch (UserStoreException | IdentityException e) {
            throw new IdentityException("Error Processing the Logout Request", e);
        }
    }

    /**
     * Builds the SAML error response and sets the compressed value to the reqValidationResponseDTO
     *
     * @param id
     * @param status
     * @param statMsg
     * @param destination
     * @return
     * @throws IdentityException
     */
    private SAMLSSOReqValidationResponseDTO buildErrorResponse(String id, String status, String statMsg, String
            destination, String responseSigningAlgorithmUri, String responseDigestAlgorithmUri)
            throws IdentityException {
        SAMLSSOReqValidationResponseDTO reqValidationResponseDTO = new SAMLSSOReqValidationResponseDTO();
        LogoutResponse logoutResp = new SingleLogoutMessageBuilder().buildLogoutResponse(id, status, statMsg,
                destination, false, null, responseSigningAlgorithmUri, responseDigestAlgorithmUri);
        reqValidationResponseDTO.setLogOutReq(true);
        reqValidationResponseDTO.setValid(false);
        try {
            reqValidationResponseDTO.setResponse(SAMLSSOUtil.compressResponse(SAMLSSOUtil.marshall(logoutResp)));
        } catch (IOException e) {
            throw new IdentityException("Error while creating logout response", e);
        }
        return reqValidationResponseDTO;
    }
}
