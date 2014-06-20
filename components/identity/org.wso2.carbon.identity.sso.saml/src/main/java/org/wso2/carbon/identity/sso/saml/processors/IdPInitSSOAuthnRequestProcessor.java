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
package org.wso2.carbon.identity.sso.saml.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Response;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.builders.ErrorResponseBuilder;
import org.wso2.carbon.identity.sso.saml.builders.ResponseBuilder;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSORespDTO;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.session.SessionInfoData;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.List;

public class IdPInitSSOAuthnRequestProcessor {

    private static Log log = LogFactory.getLog(IdPInitSSOAuthnRequestProcessor.class);

    public SAMLSSORespDTO process(SAMLSSOAuthnReqDTO authnReqDTO, String sessionId,
                                  boolean isAuthenticated, String authenticators, String authMode) throws Exception {
        try {
            SAMLSSOServiceProviderDO serviceProviderConfigs = getServiceProviderConfig(authnReqDTO);


            if (serviceProviderConfigs == null) {
                String msg =
                        "A Service Provider with the Issuer '" + authnReqDTO.getIssuer() +
                                "' is not registered." +
                                " Service Provider should be registered in advance.";
                log.warn(msg);
                return buildErrorResponse(authnReqDTO.getId(),
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, msg);
            }

            if(!serviceProviderConfigs.isIdPInitSSOEnabled()){
                String msg = "IdP initiated SSO not enabled for service provider '" + authnReqDTO.getIssuer() + "'.";
                log.debug(msg);
                return buildErrorResponse(null,
                        SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, msg);
            }

            if (serviceProviderConfigs.isEnableAttributesByDefault()) {
                if (serviceProviderConfigs.getAttributeConsumingServiceIndex() != null) {
                    authnReqDTO.setAttributeConsumingServiceIndex(Integer
                            .parseInt(serviceProviderConfigs
                                    .getAttributeConsumingServiceIndex()));
                }
            }

            // reading the service provider configs
            populateServiceProviderConfigs(serviceProviderConfigs, authnReqDTO);

            // if subject is specified in AuthnRequest only that user should be
            // allowed to logged-in
            if (authnReqDTO.getSubject() != null && authnReqDTO.getUsername() != null) {
                if (!authnReqDTO.getUsername().equals(authnReqDTO.getSubject())) {
                    String msg = "Provided username does not match with the requested subject";
                    log.warn(msg);
                    return buildErrorResponse(authnReqDTO.getId(),
                            SAMLSSOConstants.StatusCodes.AUTHN_FAILURE, msg);
                }
            }

            // persist the session
            SSOSessionPersistenceManager sessionPersistenceManager = SSOSessionPersistenceManager.getPersistenceManager();

            SAMLSSORespDTO samlssoRespDTO = null;
            String sessionIndexId = null;

            if (isAuthenticated) {
                if (sessionPersistenceManager.isExistingTokenId(sessionId)) {
                    sessionIndexId = sessionPersistenceManager.getSessionIndexFromTokenId(sessionId);
                } else {
                    sessionIndexId = UUIDGenerator.generateUUID();
                    sessionPersistenceManager.persistSession(sessionId, sessionIndexId);
                }

                if (authMode.equals(SAMLSSOConstants.AuthnModes.USERNAME_PASSWORD)) {
                    SAMLSSOServiceProviderDO spDO = new SAMLSSOServiceProviderDO();
                    spDO.setIssuer(authnReqDTO.getIssuer());
                    spDO.setAssertionConsumerUrl(authnReqDTO.getAssertionConsumerURL());
                    spDO.setCertAlias(authnReqDTO.getCertAlias());
                    spDO.setLogoutURL(authnReqDTO.getLogoutURL());
                    sessionPersistenceManager.persistSession(sessionId, sessionIndexId, authnReqDTO.getUsername(),
                            spDO, authnReqDTO.getRpSessionId(), authenticators, authnReqDTO.getUserAttributes(),
                            authnReqDTO.getTenantDomain());

                    SessionInfoData sessionInfo = sessionPersistenceManager.getSessionInfo(sessionIndexId);
                    authnReqDTO.setUsername(sessionInfo.getSubject());
                    /*authnReqDTO.setUserAttributes(sessionInfo.getAttributes());*/
                    sessionPersistenceManager.persistSession(sessionId, sessionIndexId, authnReqDTO.getIssuer(),
                            authnReqDTO.getAssertionConsumerURL(),
                            authnReqDTO.getRpSessionId());
                }

                // Build the response for the successful scenario
                ResponseBuilder respBuilder = SAMLSSOUtil.getResponseBuilder();
                Response response = respBuilder.buildResponse(authnReqDTO, sessionIndexId);
                samlssoRespDTO = new SAMLSSORespDTO();
                String samlResp = SAMLSSOUtil.marshall(response);

                if (log.isDebugEnabled()) {
                    log.debug(samlResp);
                }

                samlssoRespDTO.setRespString(SAMLSSOUtil.encode(samlResp));
                samlssoRespDTO.setSessionEstablished(true);
                samlssoRespDTO.setAssertionConsumerURL(authnReqDTO.getAssertionConsumerURL());
                samlssoRespDTO.setLoginPageURL(authnReqDTO.getLoginPageURL());
                samlssoRespDTO.setSubject(authnReqDTO.getUsername());
            }

            if (log.isDebugEnabled()) {
                log.debug(samlssoRespDTO.getRespString());
            }

            return samlssoRespDTO;
        } catch (Exception e) {
            log.error("Error processing the authentication request", e);
            SAMLSSORespDTO errorResp =
                    buildErrorResponse(authnReqDTO.getId(),
                            SAMLSSOConstants.StatusCodes.AUTHN_FAILURE,
                            "Authentication Failure, invalid username or password.");
            errorResp.setLoginPageURL(authnReqDTO.getLoginPageURL());
            return errorResp;
        }
    }


    /**
     * Returns the configured service provider configurations. The
     * configurations are taken from the user registry or from the
     * sso-idp-config.xml configuration file. In Stratos deployment the
     * configurations are read from the sso-idp-config.xml file.
     *
     * @param authnReqDTO
     * @return
     * @throws IdentityException
     */
    private SAMLSSOServiceProviderDO getServiceProviderConfig(SAMLSSOAuthnReqDTO authnReqDTO)
            throws IdentityException {
        try {
            SSOServiceProviderConfigManager stratosIdpConfigManager = SSOServiceProviderConfigManager
                    .getInstance();
            SAMLSSOServiceProviderDO ssoIdpConfigs = stratosIdpConfigManager
                    .getServiceProvider(authnReqDTO.getIssuer());
            if (ssoIdpConfigs == null) {
                IdentityPersistenceManager persistenceManager = IdentityPersistenceManager
                        .getPersistanceManager();
                UserRegistry registry = SAMLSSOUtil.getRegistryService().getConfigSystemRegistry(
                        IdentityUtil.getTenantIdOFUser(authnReqDTO.getUsername()));
                ssoIdpConfigs = persistenceManager.getServiceProvider(registry,
                        authnReqDTO.getIssuer());
                authnReqDTO.setStratosDeployment(false); // not stratos
            } else {
                authnReqDTO.setStratosDeployment(true); // stratos deployment
            }
            return ssoIdpConfigs;
        } catch (Exception e) {
            throw new IdentityException("Error while reading Service Provider configurations");
        }
    }

    /**
     * Populate the configurations of the service provider
     *
     * @param ssoIdpConfigs
     * @param authnReqDTO
     * @throws IdentityException
     */
    private void populateServiceProviderConfigs(SAMLSSOServiceProviderDO ssoIdpConfigs,
                                                SAMLSSOAuthnReqDTO authnReqDTO)
            throws IdentityException {

        if (authnReqDTO.getAssertionConsumerURL() == null || authnReqDTO.getAssertionConsumerURL().equals("")) {
            authnReqDTO.setAssertionConsumerURL(ssoIdpConfigs.getAssertionConsumerUrl());
        }
        authnReqDTO.setLoginPageURL(ssoIdpConfigs.getLoginPageURL());
        authnReqDTO.setCertAlias(ssoIdpConfigs.getCertAlias());
        authnReqDTO.setUseFullyQualifiedUsernameAsSubject(ssoIdpConfigs.isUseFullyQualifiedUsername());
        authnReqDTO.setNameIdClaimUri(ssoIdpConfigs.getNameIdClaimUri());
        authnReqDTO.setNameIDFormat(ssoIdpConfigs.getNameIDFormat());
        authnReqDTO.setDoSingleLogout(ssoIdpConfigs.isDoSingleLogout());
        authnReqDTO.setLogoutURL(ssoIdpConfigs.getLogoutURL());
        authnReqDTO.setDoSignResponse(ssoIdpConfigs.isDoSignResponse());
        authnReqDTO.setDoSignAssertions(ssoIdpConfigs.isDoSignAssertions());
        authnReqDTO.setRequestedClaims((ssoIdpConfigs.getRequestedClaims()));
        authnReqDTO.setRequestedAudiences((ssoIdpConfigs.getRequestedAudiences()));
        authnReqDTO.setRequestedRecipients((ssoIdpConfigs.getRequestedRecipients()));
        authnReqDTO.setDoEnableEncryptedAssertion(ssoIdpConfigs.isDoEnableEncryptedAssertion());
    }

    /**
     *
     * @param id
     * @param status
     * @param statMsg
     * @return
     * @throws Exception
     */
    private SAMLSSORespDTO buildErrorResponse(String id, String status,
                                              String statMsg) throws Exception {
        SAMLSSORespDTO samlSSORespDTO = new SAMLSSORespDTO();
        ErrorResponseBuilder errRespBuilder = new ErrorResponseBuilder();
        List<String> statusCodeList = new ArrayList<String>();
        statusCodeList.add(status);
        Response resp = errRespBuilder.buildResponse(id, statusCodeList, statMsg);
        samlSSORespDTO.setRespString(SAMLSSOUtil.encode(SAMLSSOUtil.marshall(resp)));
        samlSSORespDTO.setSessionEstablished(false);
        return samlSSORespDTO;
    }
}
