/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider;

import net.minidev.json.JSONArray;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.AssociationException;
import org.openid4java.message.AuthFailure;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.DirectError;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.server.ServerException;
import org.openid4java.server.ServerManager;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityConstants.ServerConfig;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.IdentityClaimManager;
import org.wso2.carbon.identity.core.model.OpenIDRememberMeDO;
import org.wso2.carbon.identity.core.model.OpenIDUserRPDO;
import org.wso2.carbon.identity.core.model.XMPPSettingsDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityCoreConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthRequestDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthResponseDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDClaimDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDParameterDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDProviderInfoDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDRememberMeDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDUserProfileDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDUserRPDTO;
import org.wso2.carbon.identity.provider.openid.OpenIDProvider;
import org.wso2.carbon.identity.provider.openid.OpenIDRememberMeTokenManager;
import org.wso2.carbon.identity.provider.openid.OpenIDServerConstants;
import org.wso2.carbon.identity.provider.openid.OpenIDUtil;
import org.wso2.carbon.identity.core.dao.OpenIDUserRPDAO;
import org.wso2.carbon.identity.provider.openid.extensions.OpenIDExtension;
import org.wso2.carbon.identity.provider.openid.handlers.OpenIDAuthenticationRequest;
import org.wso2.carbon.identity.provider.openid.handlers.OpenIDExtensionFactory;
import org.wso2.carbon.identity.provider.xmpp.MPAuthenticationProvider;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The OpenID Provider service class.
 *
 * @author WSO2Inc
 */
public class OpenIDProviderService {

    private static final Log log = LogFactory.getLog(OpenIDProviderService.class);
    private String userAttributeSeparator = IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR_DEFAULT;

    public static int getOpenIDSessionTimeout() {
        if (StringUtils.isNotBlank(IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_SESSION_TIMEOUT))) {
            return Integer
                    .parseInt(IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_SESSION_TIMEOUT).trim());
        } else {
            return 36000;
        }
    }

    /**
     * Authenticates users with their OpenID Identifier and password
     *
     * @param openID
     * @param password
     * @return
     * @throws Exception
     */
    public boolean authenticateWithOpenID(String openID, String password) throws IdentityProviderException {

        String userName = null;
        try {
            userName = OpenIDUtil.getUserName(openID);
        } catch (MalformedURLException e) {
            throw new IdentityProviderException("Failed to get username from OpenID " + openID, e);
        }
        String domainName = MultitenantUtils.getDomainNameFromOpenId(openID);
        String tenantUser = MultitenantUtils.getTenantAwareUsername(userName);

        boolean isAutheticated = false;
        boolean multiFactAuthnStatus = false;
        try {
            isAutheticated = IdentityTenantUtil.getRealm(domainName, userName).getUserStoreManager().authenticate(
                    tenantUser, password);
            boolean useMultiFactAuthn =
                    Boolean.parseBoolean(IdentityUtil.getProperty(ServerConfig.OPENID_USE_MULTIFACTOR_AUTHENTICATION));
            multiFactAuthnStatus = true;

            if (useMultiFactAuthn) {
                multiFactAuthnStatus = authenticateWithXMPP(tenantUser, tenantUser, tenantUser, isAutheticated);
                if (log.isDebugEnabled() && multiFactAuthnStatus) {
                    log.debug("XMPP Multifactor Authentication was completed Successfully.");
                }
            }
        } catch (UserStoreException | IdentityException e) {
            throw new IdentityProviderException("Error while authenticating with OpenID " + openID, e);
        }

        if (multiFactAuthnStatus && isAutheticated) {
            MessageContext msgContext = MessageContext.getCurrentMessageContext();
            if (msgContext != null) {
                HttpServletRequest request =
                        (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
                HttpSession httpSession = request.getSession(false);
                if (httpSession != null) {
                    httpSession.setAttribute(OpenIDServerConstants.OPENID_LOGGEDIN_USER, userName);
                }
            }
        }

        return multiFactAuthnStatus && isAutheticated;
    }

    /**
     * Authenticate the user with XMPP
     *
     * @param userName
     * @param tenantUser
     * @param domainName
     * @param isAutheticated
     * @return
     * @throws IdentityException
     */
    private boolean authenticateWithXMPP(String userName, String tenantUser, String domainName,
                                         boolean isAutheticated) throws IdentityException {

        IdentityPersistenceManager manager = IdentityPersistenceManager.getPersistanceManager();
        Registry registry = IdentityTenantUtil.getRegistry(domainName, userName);
        XMPPSettingsDO xmppSettingsDO = manager.getXmppSettings(registry, tenantUser);

        if (xmppSettingsDO != null && xmppSettingsDO.isXmppEnabled() && isAutheticated) {
            MPAuthenticationProvider mpAuthnProvider = new MPAuthenticationProvider(xmppSettingsDO);
            return mpAuthnProvider.authenticate();
        }
        return true;
    }

    /**
     * Authenticates with the remember me token given in previous authentication.
     *
     * @param openID
     * @param password
     * @param ipaddress
     * @param cookie
     * @return
     * @throws Exception
     */
    public OpenIDRememberMeDTO authenticateWithOpenIDRememberMe(String openID, String password,
                                                                String ipaddress, String cookie)
            throws Exception {
        String userName = OpenIDUtil.getUserName(openID);
        boolean isAutheticated = false;
        String hmac = null;
        OpenIDRememberMeDTO dto = new OpenIDRememberMeDTO();
        dto.setAuthenticated(false);

        if (password != null && password.trim().length() > 0) {
            isAutheticated = authenticateWithOpenID(openID, password);
            if (!isAutheticated) {
                return dto;
            }
        } else {
            if (cookie == null || "null".equals(cookie) || ipaddress == null) {
                return dto;
            }
        }

        OpenIDRememberMeDO rememberMe = new OpenIDRememberMeDO();
        rememberMe.setOpenID(openID);
        rememberMe.setUserName(userName);

        OpenIDRememberMeTokenManager tokenManager = new OpenIDRememberMeTokenManager();
        String token = null;

        if (ipaddress != null) {
            if (cookie != null && !"null".equals(cookie)) {
                hmac = IdentityUtil.getHMAC(ipaddress, cookie);
                token = tokenManager.getToken(rememberMe);
                // if the authentication failed and no valid rememberMe cookie found, then failed.
                if (!isAutheticated && (token == null || !token.equals(hmac))) {
                    return dto;
                }
            } else if (!isAutheticated) {
                return dto;
            }

            cookie = IdentityUtil.generateUUID();
            hmac = IdentityUtil.getHMAC(ipaddress, cookie);
            rememberMe.setToken(hmac);
            tokenManager.updateToken(rememberMe);
            dto.setNewCookieValue(cookie);
            dto.setAuthenticated(true);

            MessageContext msgContext = MessageContext.getCurrentMessageContext();

            if (msgContext != null) {
                HttpServletRequest request =
                        (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
                HttpSession httpSession = request.getSession(false);

                if (httpSession != null) {
                    httpSession.setAttribute(OpenIDServerConstants.OPENID_LOGGEDIN_USER, userName);
                }
            }

            return dto;
        }

        return dto;
    }

    public OpenIDRememberMeDTO authenticateWithRememberMeCookie(String openID, String ipaddress, String cookie)
            throws Exception {
        String userName = OpenIDUtil.getUserName(openID);
        boolean isAutheticated = false;
        String hmac = null;
        OpenIDRememberMeDTO dto = new OpenIDRememberMeDTO();
        dto.setAuthenticated(false);

        if (cookie == null || "null".equals(cookie) || ipaddress == null) {
            return dto;
        }

        OpenIDRememberMeDO rememberMe = new OpenIDRememberMeDO();
        rememberMe.setOpenID(openID);
        rememberMe.setUserName(userName);

        OpenIDRememberMeTokenManager tokenManager = new OpenIDRememberMeTokenManager();
        String token = null;

        hmac = IdentityUtil.getHMAC(ipaddress, cookie);
        token = tokenManager.getToken(rememberMe);

        // if the authentication failed and no valid rememberMe cookie found, then failed.
        if (!isAutheticated && (token == null || !token.equals(hmac))) {
            return dto;
        }

        cookie = IdentityUtil.generateUUID();
        hmac = IdentityUtil.getHMAC(ipaddress, cookie);
        rememberMe.setToken(hmac);
        tokenManager.updateToken(rememberMe);
        dto.setNewCookieValue(cookie);
        dto.setAuthenticated(true);

        MessageContext msgContext = MessageContext.getCurrentMessageContext();

        if (msgContext != null) {
            HttpServletRequest request =
                    (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            HttpSession httpSession = request.getSession(false);

            if (httpSession != null) {
                httpSession.setAttribute(OpenIDServerConstants.OPENID_LOGGEDIN_USER, userName);
            }
        }

        return dto;
    }

    public OpenIDRememberMeDTO handleRememberMe(String openID, String ipaddress) throws Exception {

        String userName = OpenIDUtil.getUserName(openID);
        String hmac = null;
        OpenIDRememberMeDTO dto = new OpenIDRememberMeDTO();
        dto.setAuthenticated(false);
        String cookie = null;

        OpenIDRememberMeDO rememberMe = new OpenIDRememberMeDO();
        rememberMe.setOpenID(openID);
        rememberMe.setUserName(userName);

        OpenIDRememberMeTokenManager tokenManager = new OpenIDRememberMeTokenManager();

        if (ipaddress != null) {
            cookie = IdentityUtil.generateUUID();
            hmac = IdentityUtil.getHMAC(ipaddress, cookie);
            rememberMe.setToken(hmac);
            tokenManager.updateToken(rememberMe);
            dto.setNewCookieValue(cookie);
            dto.setAuthenticated(true);

            MessageContext msgContext = MessageContext.getCurrentMessageContext();

            if (msgContext != null) {
                HttpServletRequest request =
                        (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
                HttpSession httpSession = request.getSession(false);

                if (httpSession != null) {
                    httpSession.setAttribute(OpenIDServerConstants.OPENID_LOGGEDIN_USER, userName);
                }
            }
        }

        return dto;
    }

    /**
     * @param userName
     * @return
     * @throws IdentityProviderException
     */
    public OpenIDProviderInfoDTO getOpenIDProviderInfo(String userName, String openid)
            throws IdentityProviderException {

        OpenIDProviderInfoDTO providerInfo = new OpenIDProviderInfoDTO();
        String domain = null;
        UserRealm realm = null;

        try {
            domain = MultitenantUtils.getDomainNameFromOpenId(openid);
            realm = IdentityTenantUtil.getRealm(domain, userName);
        } catch (IdentityException e) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring IdentityException", e);
            }
        }
        if (realm == null) {
            return providerInfo;
        }

        providerInfo.setSubDomain(domain);
        String tenantFreeUsername = MultitenantUtils.getTenantAwareUsername(userName);

        providerInfo.setOpenIDProviderServerUrl(OpenIDUtil.getOpenIDServerURL());
        providerInfo.setOpenID(OpenIDUtil.getOpenIDUserPattern() + "/" + tenantFreeUsername);

        return providerInfo;
    }

    /**
     * @param openId
     * @param profileId
     * @param requredClaims
     * @return
     * @throws IdentityProviderException
     */
    public OpenIDClaimDTO[] getClaimValues(String openId, String profileId,
                                           OpenIDParameterDTO[] requredClaims) throws IdentityProviderException {
        List<String> claimList = null;
        ParameterList paramList = null;
        AuthRequest authReq = null;

        String message = "Invalid parameters provided to getClaimValues";
        validateInputParameters(new String[] { openId, profileId }, message);
        try {
            checkUserAuthorization(OpenIDUtil.getUserName(openId), "getClaimValues");
        } catch (MalformedURLException e) {
            throw new IdentityProviderException("Failed to get username from OpenID " + openId, e);
        }

        paramList = getParameterList(requredClaims);
        try {
            authReq = AuthRequest
                    .createAuthRequest(paramList, OpenIDProvider.getInstance().getManager().getRealmVerifier());
            claimList = getRequestedAttributes(authReq);
        } catch (MessageException | IdentityException e) {
            throw new IdentityProviderException("Failed to get requested attribute set", e);
        }

        return getOpenIDClaimValues(openId, profileId, claimList);
    }

    /**
     * @param params
     * @return
     * @throws Exception
     */
    public String getOpenIDAssociationResponse(OpenIDParameterDTO[] params) {
        Message message = null;
        ParameterList paramList = null;

        paramList = getParameterList(params);
        message = OpenIDProvider.getInstance().getManager().associationResponse(paramList);
        return message.keyValueFormEncoding();
    }

    /**
     * The verify method used by the OpenID Provider when using the OpenID Dumb
     * Mode
     *
     * @param params
     * @return
     * @throws Exception
     */
    public String verify(OpenIDParameterDTO[] params) throws IdentityProviderException {
        String disableDumbMode = IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_DISABLE_DUMB_MODE);

        if ("true".equalsIgnoreCase(disableDumbMode)) {
            throw new IdentityProviderException("OpenID relying parties with dumb mode not supported");
        }

        ParameterList paramList = getParameterList(params);
        Message message = OpenIDProvider.getInstance().getManager().verify(paramList);
        return message.keyValueFormEncoding();
    }

    /**
     * @param requestDTO
     * @return
     * @throws Exception
     */
    public OpenIDAuthResponseDTO getOpenIDAuthResponse(OpenIDAuthRequestDTO requestDTO)
            throws IdentityProviderException {
        ParameterList paramList = null;
        Message message = null;
        paramList = getParameterList(requestDTO.getParams());
        String destinationUrl = null;
        AuthRequest authReq = null;
        ServerManager manager = null;
        OpenIDAuthResponseDTO response = null;

        response = new OpenIDAuthResponseDTO();
        manager = OpenIDProvider.getInstance().getManager();
        try {
            authReq = AuthRequest.createAuthRequest(paramList, manager.getRealmVerifier());
        } catch (MessageException e) {
            throw new IdentityProviderException("Error while creating authentication request", e);
        }
        message =
                manager.authResponse(paramList, requestDTO.getOpLocalId(),
                                     requestDTO.getUserSelectedClaimedId(),
                                     requestDTO.isAuthenticated());

        if (message instanceof DirectError || message instanceof AuthFailure) {
            // Validation fails - returns 'cancel'.
            destinationUrl = message.getDestinationUrl(true);
            response.setDestinationUrl(destinationUrl);
            response.setValidated(false);
        } else {
            OpenIDExtension extension = null;
            OpenIDAuthenticationRequest req = null;
            req = new OpenIDAuthenticationRequest();

            if (requestDTO.isPhishiingResistanceAuthRequest()) {
                // Relying party requests phishing-resistant login.
                req.setPhishingResistanceLogin(true);
            }
            if (requestDTO.isMultiFactorAuthRequested()) {
                // Relying party requests phishing-resistant login.
                req.setMultifactorLogin(true);
            }

            req.setAuthRequest(authReq);

            // A given OpenID authentication request can contain multiple
            // extensions.
            // OpenIDProvider is not aware of extensions - we simply delegate
            // the extension
            // processing logic to a subclass of OpenIDExtension.
            try {
                for (Object alias : authReq.getExtensions()) {
                    req.setExtensionAlias((String) alias);

                    // Get the corresponding OpenIDExtension instance from the
                    // OpenIDExtensionFactory.
                    extension = OpenIDExtensionFactory.getInstance().getExtension(req);
                    if (extension != null) {
                        MessageExtension messageExtension = null;
                        messageExtension = extension.getMessageExtension(requestDTO.getOpenID(),
                                                                         requestDTO.getProfileName(), requestDTO);
                        if (messageExtension != null) {
                            message.addExtension(messageExtension);
                            AuthSuccess authSuccess = (AuthSuccess) message;
                            authSuccess.addSignExtension((String) alias);
                            manager.sign(authSuccess);
                        }
                    }
                }
            } catch (IdentityException | MessageException | ServerException | AssociationException e) {
                throw new IdentityProviderException("Error while creating authentication request", e);
            }

            // We only have SReg extensions.
            destinationUrl = message.getDestinationUrl(true);
            response.setDestinationUrl(destinationUrl);
            response.setValidated(true);
        }
        return response;
    }

    /**
     * @param authRequest
     * @return
     * @throws IdentityException
     */
    private List<String> getRequestedAttributes(AuthRequest authRequest) throws IdentityException {
        OpenIDAuthenticationRequest req = null;
        OpenIDExtension extension = null;
        List<String> requiredAttributes = null;

        req = new OpenIDAuthenticationRequest();
        req.setAuthRequest(authRequest);
        requiredAttributes = new ArrayList<String>();

        for (Object alias : authRequest.getExtensions()) {
            req.setExtensionAlias((String) alias);
            extension = OpenIDExtensionFactory.getInstance().getExtension(req);
            if (extension != null) {
                extension.addRequiredAttributes(requiredAttributes);
            }
        }

        return requiredAttributes;
    }

    /**
     * @param params
     * @return
     */
    private ParameterList getParameterList(OpenIDParameterDTO[] params) {
        ParameterList paramList = null;
        Map<String, String> paramMap = null;
        paramMap = new HashMap<String, String>();

        for (OpenIDParameterDTO param : params) {
            paramMap.put(param.getName(), param.getValue());
        }

        paramList = new ParameterList(paramMap);
        return paramList;
    }

    /**
     * A new method to do XMPP based authentication for a given user
     *
     * @param userId
     * @return
     * @throws IdentityProviderException
     */
    public boolean doXMPPBasedMultiFactorAuthForInfocard(String userId) throws IdentityProviderException {

        boolean authenticationStatus = true;
        XMPPSettingsDO xmppSettingsDO = null;
        try {
            IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
            xmppSettingsDO = persistenceManager.getXmppSettings(IdentityTenantUtil.getRegistry(null, userId),
                                                                MultitenantUtils.getTenantAwareUsername(userId));
        } catch (IdentityException e) {
            throw new IdentityProviderException("Error while retriving XMPP settings", e);
        }

        // attempts to do multi-factor authentication, if the user has enabled
        // it.
        if (xmppSettingsDO != null && xmppSettingsDO.isXmppEnabled()) {
            MPAuthenticationProvider mpAuthenticationProvider = new MPAuthenticationProvider(xmppSettingsDO);
            authenticationStatus = mpAuthenticationProvider.authenticate();
        }

        if (log.isInfoEnabled()) {
            log.info("XMPP Multifactor Authentication was completed Successfully.");
        }

        return authenticationStatus;
    }

    /**
     * Get Profile details of an user
     *
     * @param openId
     * @return
     * @throws IdentityProviderException
     */
    public OpenIDUserProfileDTO[] getUserProfiles(String openId, OpenIDParameterDTO[] requredClaims)
            throws IdentityProviderException {
        String userName = null;
        UserRealm realm = null;
        UserStoreManager reader = null;
        String tenatUser = null;
        String domainName = null;

        try {
            userName = OpenIDUtil.getUserName(openId);
            tenatUser = MultitenantUtils.getTenantAwareUsername(userName);
            domainName = MultitenantUtils.getDomainNameFromOpenId(openId);
            realm = IdentityTenantUtil.getRealm(domainName, userName);
            reader = realm.getUserStoreManager();
            String[] profileNames = reader.getProfileNames(tenatUser);
            OpenIDUserProfileDTO[] profileDtoSet = new OpenIDUserProfileDTO[profileNames.length];

            List<String> claimList = null;
            ParameterList paramList = getParameterList(requredClaims);
            AuthRequest authReq =
                    AuthRequest.createAuthRequest(paramList, OpenIDProvider.getInstance()
                                                                           .getManager()
                                                                           .getRealmVerifier());

            claimList = getRequestedAttributes(authReq);

            for (int i = 0; i < profileNames.length; i++) {
                OpenIDUserProfileDTO profileDTO = new OpenIDUserProfileDTO();
                OpenIDClaimDTO[] claimSet =
                        getOpenIDClaimValues(openId, profileNames[i], claimList);
                profileDTO.setProfileName(profileNames[i]);
                profileDTO.setClaimSet(claimSet);
                profileDtoSet[i] = profileDTO;
            }
            return profileDtoSet;
        } catch (MalformedURLException | UserStoreException | MessageException | IdentityException e) {
            throw new IdentityProviderException("Error while retrieving user profiles", e);
        }
    }

    /**
     * This method tracks RPs
     *
     * @param rpdto
     * @throws IdentityProviderException
     */
    public void updateOpenIDUserRPInfo(OpenIDUserRPDTO rpdto) throws IdentityProviderException {

        String userName = null;
        try {
            userName = OpenIDUtil.getUserName(rpdto.getOpenID());
        } catch (MalformedURLException e) {
            throw new IdentityProviderException("Failed to get username from OpenID " + rpdto.getOpenID(), e);
        }
        String domainName = MultitenantUtils.getDomainNameFromOpenId(rpdto.getOpenID());
        int tenantId = IdentityTenantUtil.getTenantId(domainName);

        OpenIDUserRPDO rpdo = new OpenIDUserRPDO();
        OpenIDUserRPDAO dao = new OpenIDUserRPDAO();

        try {
            rpdo.setUserName(userName);
            rpdo.setRpUrl(rpdto.getRpUrl());
            rpdo.setTrustedAlways(rpdto.isTrustedAlways());
            rpdo.setDefaultProfileName(rpdto.getDefaultProfileName());

            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha.digest((userName + ":" + rpdto.getRpUrl()).getBytes());
            rpdo.setUuid(new String(Hex.encodeHex(digest)));

            dao.createOrUpdate(rpdo, tenantId);
        } catch (NoSuchAlgorithmException e) {
            throw new IdentityProviderException("Error while updating DAO for " + domainName, e);
        }

    }

    /**
     * Returns RP DTO objects for the given OpenID
     *
     * @param openID
     * @return openIDUserDTOs
     * @throws IdentityProviderException
     */
    public OpenIDUserRPDTO[] getOpenIDUserRPs(String openID) throws IdentityProviderException {

        String username = null;
        try {
            username = OpenIDUtil.getUserName(openID);
        } catch (MalformedURLException e) {
            throw new IdentityProviderException("Error while getting username for OpenID " + username, e);
        }
        String domainName = MultitenantUtils.getDomainNameFromOpenId(openID);
        int tenantId = IdentityTenantUtil.getTenantId(domainName);

        OpenIDUserRPDO[] rpdos = null;
        OpenIDUserRPDAO dao = new OpenIDUserRPDAO();
        rpdos = dao.getOpenIDUserRPs(username, tenantId);
        if (rpdos == null) {
            return new OpenIDUserRPDTO[0];
        }

        OpenIDUserRPDTO[] rpdto = new OpenIDUserRPDTO[rpdos.length];
        int i = 0;
        for (OpenIDUserRPDO rpdo : rpdos) {
            rpdto[i] = new OpenIDUserRPDTO(rpdo);
            i++;
        }
        return rpdto;
    }

    /**
     * Returns RP DTO for the given OpenID and RP
     *
     * @param openID
     * @param rpUrl
     * @return openIDUserRPDTO
     * @throws IdentityProviderException
     */
    public OpenIDUserRPDTO getOpenIDUserRPInfo(String openID, String rpUrl) throws IdentityProviderException {

        String userName = null;
        try {
            userName = OpenIDUtil.getUserName(openID);
        } catch (MalformedURLException e) {
            throw new IdentityProviderException("Failed to get username from OpenID " + openID, e);
        }
        String domainName = MultitenantUtils.getTenantDomain(userName);
        int tenantId = IdentityTenantUtil.getTenantId(domainName);

        OpenIDUserRPDO rpdo = null;
        OpenIDUserRPDAO dao = new OpenIDUserRPDAO();
        rpdo = dao.getOpenIDUserRP(userName, rpUrl, tenantId);
        if (rpdo == null) {
            return null;
        }
        return new OpenIDUserRPDTO(rpdo);
    }

    /**
     * Checks if user approval has to be bypassed based on identity.xml config
     *
     * @return boolean
     */
    public boolean isOpenIDUserApprovalBypassEnabled() {
        return Boolean.parseBoolean(IdentityUtil.getProperty(IdentityConstants.ServerConfig.OPENID_SKIP_USER_CONSENT));
    }

    /**
     * @param openId
     * @param profileId
     * @param claimList
     * @return
     * @throws IdentityProviderException
     */
    private OpenIDClaimDTO[] getOpenIDClaimValues(String openId, String profileId,
                                                  List<String> claimList) throws IdentityProviderException {
        UserStoreManager userStore = null;
        Map<String, String> claimValues = null;
        OpenIDClaimDTO[] claims = null;
        OpenIDClaimDTO dto = null;
        IdentityClaimManager claimManager = null;
        Claim[] claimData = null;
        String[] claimArray = new String[claimList.size()];
        String userName = null;
        String domainName = null;
        String tenantUser;
        UserRealm realm = null;

        try {
            userName = OpenIDUtil.getUserName(openId);
        } catch (MalformedURLException e) {
            throw new IdentityProviderException("Failed to get username from OpenID " + openId, e);
        }
        domainName = MultitenantUtils.getDomainNameFromOpenId(openId);
        tenantUser = MultitenantUtils.getTenantAwareUsername(userName);

        try {
            realm = IdentityTenantUtil.getRealm(domainName, userName);
            userStore = realm.getUserStoreManager();
            claimValues =
                    userStore.getUserClaimValues(tenantUser, claimList.toArray(claimArray),
                                                 profileId);
        } catch (IdentityException | UserStoreException e) {
            throw new IdentityProviderException("Failed to get claims of user " + tenantUser, e);
        }

        String claimSeparator = claimValues.get(IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR);
        if (StringUtils.isNotBlank(claimSeparator)) {
            userAttributeSeparator = claimSeparator;
        }
        claimValues.remove(IdentityCoreConstants.MULTI_ATTRIBUTE_SEPARATOR);

        int i = 0;
        JSONArray values;
        claims = new OpenIDClaimDTO[claimValues.size()];

        try {
            claimManager = IdentityClaimManager.getInstance();
            claimData = claimManager.getAllSupportedClaims(realm);
        } catch (IdentityException e) {
            throw new IdentityProviderException("Failed load all supported claims", e);
        }

        for (Claim element : claimData) {
            if (claimValues.containsKey(element.getClaimUri())) {
                dto = new OpenIDClaimDTO();
                values = new JSONArray();
                dto.setClaimUri(element.getClaimUri());

                String value = claimValues.get(element.getClaimUri());
                if (userAttributeSeparator != null && value.contains(userAttributeSeparator)) {
                    StringTokenizer st = new StringTokenizer(value, userAttributeSeparator);
                    while (st.hasMoreElements()) {
                        String attValue = st.nextElement().toString();
                        if (StringUtils.isNotBlank(attValue)) {
                            values.add(attValue);
                        }
                    }
                } else {
                    values.add(value);
                }
                dto.setClaimValue(values.toJSONString());
                dto.setDisplayTag(element.getDisplayTag());
                dto.setDescription(element.getDescription());
                claims[i++] = dto;
            }
        }
        return claims;
    }

    /**
     * @param username
     * @param operation
     * @throws IdentityProviderException
     */
    private void checkUserAuthorization(String username, String operation) throws IdentityProviderException {
        MessageContext msgContext = MessageContext.getCurrentMessageContext();
        HttpServletRequest request = (HttpServletRequest) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            String userName = (String) httpSession.getAttribute(OpenIDServerConstants.OPENID_LOGGEDIN_USER);
            if (!username.equals(userName)) {
                throw new IdentityProviderException("Unauthorised action by user " + username +
                                                    " to access " + operation);
            }
            return;
        }
        throw new IdentityProviderException("Unauthorised action by user " + username +
                                            " to access " + operation);
    }

    /**
     * @param params
     * @param message
     */
    private void validateInputParameters(String[] params, String message) {
        for (String param : params) {
            if (param == null || param.trim().length() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug(message);
                }
                throw new IllegalArgumentException(message);
            }
        }
    }

}
