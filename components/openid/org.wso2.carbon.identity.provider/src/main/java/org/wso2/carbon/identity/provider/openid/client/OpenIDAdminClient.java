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
package org.wso2.carbon.identity.provider.openid.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.OpenIDProviderService;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthRequestDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDAuthResponseDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDClaimDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDParameterDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDProviderInfoDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDRememberMeDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDUserProfileDTO;
import org.wso2.carbon.identity.provider.dto.OpenIDUserRPDTO;
import org.wso2.carbon.identity.provider.openid.OpenIDConstants;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenIDAdminClient {

    public static final String OPENID_ADMIN_COOKIE = "OPENID_ADMIN_COOKIE";
    public static final String OPENID_PROVIDER_SERVICE = "OpenIDProviderService";
    private static Integer sessionTimeout = null;
    private static final Log log = LogFactory.getLog(OpenIDAdminClient.class);
    private String newCookieValue;
    private boolean isUserApprovalBypassEnabled;
    private OpenIDProviderService openIDProviderService;

    public OpenIDAdminClient(ConfigurationContext context, String backendServerURL, String cookie)
            throws AxisFault {

        openIDProviderService = new OpenIDProviderService();
        isUserApprovalBypassEnabled = openIDProviderService.isOpenIDUserApprovalBypassEnabled();

        if (sessionTimeout == null) {
            sessionTimeout = OpenIDProviderService.getOpenIDSessionTimeout();
        }
    }

    public String getNewCookieValue() {
        return newCookieValue;
    }

    public void setNewCookieValue(String newCookieValue) {
        this.newCookieValue = newCookieValue;
    }

    /**
     * Returns info such as OpenID server URL, OpenID Identifier pattern etc
     *
     * @param userName
     * @param openID
     * @return
     * @throws IdentityProviderException
     */
    public OpenIDProviderInfoDTO getOpenIDProviderInfo(String userName, String openID)
            throws IdentityProviderException {
        return openIDProviderService.getOpenIDProviderInfo(userName, openID);
    }

    /**
     * Authenticates the OpenID User with the password
     *
     * @param openId
     * @param password
     * @param session
     * @param request
     * @param response
     * @param useRememberMe
     * @return
     */
    public boolean authenticateWithOpenID(String openId, String password, HttpSession session,
                                          HttpServletRequest request, HttpServletResponse response,
                                          boolean useRememberMe) {
        boolean isAuthenticated = false;
        OpenIDRememberMeDTO dto = null;
        try {
            // Check whether the remember me option is set
            Cookie[] cookies = request.getCookies();
            String token = null;

            if (cookies != null) {
                Cookie curCookie = null;
                for (int x = 0; x < cookies.length; x++) {
                    curCookie = cookies[x];
                    if (curCookie.getName().equalsIgnoreCase(OpenIDConstants.Cookie.OPENID_TOKEN)) {
                        token = curCookie.getValue();
                        break;
                    }
                }
            }

            if ((token != null && !"null".equals(token)) || useRememberMe) {
                dto = openIDProviderService
                        .authenticateWithOpenIDRememberMe(openId.trim(), password, request.getRemoteAddr(), token);
                if (dto != null && dto.isAuthenticated()) {
                    newCookieValue = dto.getNewCookieValue();
                }
                isAuthenticated = dto.isAuthenticated();
            } else {
                isAuthenticated = openIDProviderService.authenticateWithOpenID(openId.trim(), password);
            }

        } catch (Exception e) {
            log.error("Failed to authenticate with Open ID " + openId, e);
            return false;
        }
        return isAuthenticated;
    }

    public Map<String, OpenIDClaimDTO> getClaimValues(String openId, String profileId, ParameterList requiredClaims)
            throws IdentityProviderException {

        List list = requiredClaims.getParameters();
        OpenIDParameterDTO[] params = new OpenIDParameterDTO[list.size()];
        int i = 0;
        for (Object object : list) {
            Parameter param = (Parameter) object;
            OpenIDParameterDTO openIDParameterDTO = new OpenIDParameterDTO();
            openIDParameterDTO.setName(param.getKey());
            openIDParameterDTO.setValue(param.getValue());
            params[i++] = openIDParameterDTO;
        }

        OpenIDClaimDTO[] claims = openIDProviderService.getClaimValues(openId.trim(), profileId, params);

        Map<String, OpenIDClaimDTO> map = new HashMap<String, OpenIDClaimDTO>();
        if (claims != null) {
            for (int j = 0; j < claims.length; j++) {
                if (claims[j] != null) {
                    map.put(claims[j].getClaimUri(), claims[j]);
                }
            }
        }

        return map;
    }

    public OpenIDAuthResponseDTO getOpenIDAuthResponse(OpenIDAuthRequestDTO authRequest)
            throws IdentityProviderException {
        return openIDProviderService.getOpenIDAuthResponse(authRequest);
    }

    /**
     * Gets association response strings from the backend.
     *
     * @param params
     * @return
     */
    public String getOpenIDAssociationResponse(OpenIDParameterDTO[] params) {
        return openIDProviderService.getOpenIDAssociationResponse(params);
    }

    /**
     * Verifies the Response message in OpenID Dumb Mode
     *
     * @param params
     * @return
     * @throws IdentityProviderException
     */
    public String verify(OpenIDParameterDTO[] params) throws IdentityProviderException {
        return openIDProviderService.verify(params);
    }

    /**
     * Do multi-factor authentication for an user
     *
     * @param userId
     * @return
     * @throws Exception
     */
    public boolean doxmppBasedMultiFactorAuthForInfoCards(String userId) throws IdentityProviderException {
        return openIDProviderService.doXMPPBasedMultiFactorAuthForInfocard(userId);
    }

    /**
     * @param openid
     * @return
     * @throws IdentityProviderException
     */
    public OpenIDUserProfileDTO[] getUserProfiles(String openid, ParameterList requredClaims)
            throws IdentityProviderException {
        OpenIDParameterDTO[] params = null;
        List list = null;
        list = requredClaims.getParameters();
        params = new OpenIDParameterDTO[list.size()];
        int i = 0;
        for (Object object : list) {
            Parameter param = (Parameter) object;
            OpenIDParameterDTO openIDParameterDTO = new OpenIDParameterDTO();
            openIDParameterDTO.setName(param.getKey());
            openIDParameterDTO.setValue(param.getValue());
            params[i++] = openIDParameterDTO;
        }
        return openIDProviderService.getUserProfiles(openid, params);
    }

    /**
     * Allow this relying party to retrieve user attributes without user
     * permission next time
     *
     * @param rpUrl
     * @param isTrustedAlways
     * @param defaultProfileName
     * @param openID
     * @throws Exception
     */
    public void updateOpenIDUserRPInfo(String rpUrl, boolean isTrustedAlways,
                                       String defaultProfileName, String openID) throws IdentityProviderException {

        if (isUserApprovalBypassEnabled) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Updating RP " + rpUrl + "info for " + openID);
        }

        OpenIDUserRPDTO rpdto = new OpenIDUserRPDTO();
        rpdto.setRpUrl(rpUrl);
        rpdto.setTrustedAlways(isTrustedAlways);
        rpdto.setDefaultProfileName(defaultProfileName);
        rpdto.setOpenID(openID);

        openIDProviderService.updateOpenIDUserRPInfo(rpdto);
    }

    /**
     * Returns RP DTOs for the given OpenID
     *
     * @param openID
     * @return openIDUserRPDTOs
     * @throws Exception
     */
    public OpenIDUserRPDTO[] getOpenIDUserRPs(String openID) throws IdentityProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Getting OpenID User RP DTOs for " + openID);
        }

        return openIDProviderService.getOpenIDUserRPs(openID);
    }

    /**
     * Return the RPDTO object for a given OpenID and RP URL
     *
     * @param openID
     * @param rpUrl
     * @return openIDUserRPDTO
     * @throws IdentityProviderException
     */
    public OpenIDUserRPDTO getOpenIDUserRPDTO(String openID, String rpUrl) throws IdentityProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Getting OpenID User RP DTO for " + openID + "for RP " + rpUrl);
        }

        return openIDProviderService.getOpenIDUserRPInfo(openID, rpUrl);
    }

    /**
     * Return a string array of RP info. The first values of the array contains
     * isAlwaysSpecified boolean value
     *
     * @param openID
     * @param rpUrl
     * @return rpInfo[]to.getDefaultProfileName())
     * @throws IdentityProviderException
     */
    public String[] getOpenIDUserRPInfo(String openID, String rpUrl) throws IdentityProviderException {

        OpenIDUserRPDTO rpdto = null;
        String[] rpInfo = new String[7];

        if (!isUserApprovalBypassEnabled) {
            rpdto = openIDProviderService.getOpenIDUserRPInfo(openID, rpUrl);
        }

        if (rpdto != null) {
            // do not change the order
            rpInfo[0] = Boolean.toString(rpdto.isTrustedAlways());
            rpInfo[1] = rpdto.getDefaultProfileName();
            rpInfo[2] = rpdto.getOpenID();
            rpInfo[3] = rpdto.getRpUrl();
            rpInfo[4] = rpdto.getUserName();
            rpInfo[5] = Integer.toString(rpdto.getVisitCount());
            if (rpdto.getLastVisit() == null) {
                rpdto.setLastVisit(new Date());
            }
            rpInfo[6] = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(rpdto.getLastVisit());

        } else {
            rpInfo[0] = "false";
            rpInfo[1] = "default";
        }

        return rpInfo;
    }

    /**
     * Check if the user approval bypass setting has made in the identity.xml
     *
     * @return
     */
    public boolean isOpenIDUserApprovalBypassEnabled() {
        return isUserApprovalBypassEnabled;
    }

    /**
     * Gets the OPENID_SESSION_TIMEOUT value for the OpenID provider
     *
     * @return OpenID session timeout value
     * @throws IdentityException
     */
    public int getOpenIDSessionTimeout() throws IdentityException {
        return sessionTimeout;
    }

}
