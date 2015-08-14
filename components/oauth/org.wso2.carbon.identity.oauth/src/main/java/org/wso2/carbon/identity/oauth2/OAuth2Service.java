/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.OAuthAppDO;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.exception.InvalidOAuthClientException;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDAO;
import org.wso2.carbon.identity.oauth2.authz.AuthorizationHandlerManager;
import org.wso2.carbon.identity.oauth2.dao.TokenMgtDAO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeReqDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AuthorizeRespDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuthRevocationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuthRevocationResponseDTO;
import org.wso2.carbon.identity.oauth2.model.AccessTokenDO;
import org.wso2.carbon.identity.oauth2.model.RefreshTokenValidationDataDO;
import org.wso2.carbon.identity.oauth2.token.AccessTokenIssuer;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 Service which is used to issue authorization codes or access tokens upon authorizing by the
 * user and issue/validateGrant access tokens.
 */
@SuppressWarnings("unused")
public class OAuth2Service extends AbstractAdmin {

    public static final String REFRESH_TOKEN = "refresh_token";
    private static Log log = LogFactory.getLog(OAuth2Service.class);

    /**
     * Process the authorization request and issue an authorization code or access token depending
     * on the Response Type available in the request.
     *
     * @param oAuth2AuthorizeReqDTO <code>OAuth2AuthorizeReqDTO</code> containing information about the authorization
     *                              request.
     * @return <code>OAuth2AuthorizeRespDTO</code> instance containing the access token/authorization code
     * or an error code.
     */
    public OAuth2AuthorizeRespDTO authorize(OAuth2AuthorizeReqDTO oAuth2AuthorizeReqDTO) {

        if (log.isDebugEnabled()) {
            log.debug("Authorization Request received for user : " + oAuth2AuthorizeReqDTO.getUsername() +
                    ", Client ID : " + oAuth2AuthorizeReqDTO.getConsumerKey() +
                    ", Authorization Response Type : " + oAuth2AuthorizeReqDTO.getResponseType() +
                    ", Requested callback URI : " + oAuth2AuthorizeReqDTO.getCallbackUrl() +
                    ", Requested Scope : " + OAuth2Util.buildScopeString(
                    oAuth2AuthorizeReqDTO.getScopes()));
        }

        try {
            AuthorizationHandlerManager authzHandlerManager =
                    AuthorizationHandlerManager.getInstance();
            return authzHandlerManager.handleAuthorization(oAuth2AuthorizeReqDTO);
        } catch (Exception e) {
            log.error("Error occurred when processing the authorization request. Returning an error back to client.",
                    e);
            OAuth2AuthorizeRespDTO authorizeRespDTO = new OAuth2AuthorizeRespDTO();
            authorizeRespDTO.setErrorCode(OAuth2ErrorCodes.SERVER_ERROR);
            authorizeRespDTO.setErrorMsg("Error occurred when processing the authorization " +
                    "request. Returning an error back to client.");
            authorizeRespDTO.setCallbackURI(oAuth2AuthorizeReqDTO.getCallbackUrl());
            return authorizeRespDTO;
        }
    }

    /**
     * Check Whether the provided client_id and the callback URL are valid.
     *
     * @param clientId    client_id available in the request, Not null parameter.
     * @param callbackURI callback_uri available in the request, can be null.
     * @return <code>OAuth2ClientValidationResponseDTO</code> bean with validity information,
     * callback, App Name, Error Code and Error Message when appropriate.
     */
    public OAuth2ClientValidationResponseDTO validateClientInfo(String clientId, String callbackURI) {
        OAuth2ClientValidationResponseDTO validationResponseDTO =
                new OAuth2ClientValidationResponseDTO();

        if (log.isDebugEnabled()) {
            log.debug("Validate Client information request for client_id : " + clientId + " and callback_uri " +
                    callbackURI);
        }

        try {
            OAuthAppDAO oAuthAppDAO = new OAuthAppDAO();
            OAuthAppDO appDO = oAuthAppDAO.getAppInformation(clientId);

            if(StringUtils.isEmpty(appDO.getGrantTypes()) || StringUtils.isEmpty(appDO.getCallbackUrl())){
                if(log.isDebugEnabled()) {
                    log.debug("Registered App found for the given Client Id : " + clientId + " ,App Name : " + appDO
                            .getApplicationName() + ", does not support the requested grant type.");
                }
                validationResponseDTO.setValidClient(false);
                validationResponseDTO.setErrorCode(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE);
                validationResponseDTO.setErrorMsg("Requested Grant type is not supported.");
                return validationResponseDTO;
            }

            OAuth2Util.setClientTenatId(appDO.getTenantId());

            // Valid Client, No callback has provided. Use the callback provided during the registration.
            if (callbackURI == null) {
                validationResponseDTO.setValidClient(true);
                validationResponseDTO.setCallbackURL(appDO.getCallbackUrl());
                validationResponseDTO.setApplicationName(appDO.getApplicationName());
                return validationResponseDTO;
            }

            if (log.isDebugEnabled()) {
                log.debug("Registered App found for the given Client Id : " + clientId + " ,App Name : " + appDO
                        .getApplicationName() + ", Callback URL : " + appDO.getCallbackUrl());
            }

            // Valid Client with a callback url in the request. Check whether they are equal.
            if (appDO.getCallbackUrl().equals(callbackURI)) {
                validationResponseDTO.setValidClient(true);
                validationResponseDTO.setApplicationName(appDO.getApplicationName());
                validationResponseDTO.setCallbackURL(callbackURI);
                return validationResponseDTO;
            } else {    // Provided callback URL does not match the registered callback url.
                log.warn("Provided Callback URL does not match with the provided one.");
                validationResponseDTO.setValidClient(false);
                validationResponseDTO.setErrorCode(OAuth2ErrorCodes.INVALID_CALLBACK);
                validationResponseDTO.setErrorMsg("Registered callback does not match with the provided url.");
                return validationResponseDTO;
            }
        } catch (InvalidOAuthClientException e) {
            // There is no such Client ID being registered. So it is a request from an invalid client.
            log.error("Error while retrieving the Application Information", e);
            validationResponseDTO.setValidClient(false);
            validationResponseDTO.setErrorCode(OAuth2ErrorCodes.INVALID_CLIENT);
            validationResponseDTO.setErrorMsg(e.getMessage());
            return validationResponseDTO;
        } catch (IdentityOAuth2Exception e) {
            log.error("Error when reading the Application Information.", e);
            validationResponseDTO.setValidClient(false);
            validationResponseDTO.setErrorCode(OAuth2ErrorCodes.SERVER_ERROR);
            validationResponseDTO.setErrorMsg("Error when processing the authorization request.");
            return validationResponseDTO;
        }
    }

    /**
     * Issue access token in exchange to an Authorization Grant.
     *
     * @param tokenReqDTO <Code>OAuth2AccessTokenReqDTO</Code> representing the Access Token request
     * @return <Code>OAuth2AccessTokenRespDTO</Code> representing the Access Token response
     */
    public OAuth2AccessTokenRespDTO issueAccessToken(OAuth2AccessTokenReqDTO tokenReqDTO) {

        if (log.isDebugEnabled()) {
            log.debug("Access Token request received for Client ID " +
                    tokenReqDTO.getClientId() + ", User ID " + tokenReqDTO.getResourceOwnerUsername() +
                    ", Scope : " + Arrays.toString(tokenReqDTO.getScope()) + " and Grant Type : " + tokenReqDTO.getGrantType());
        }

        try {
            AccessTokenIssuer tokenIssuer = AccessTokenIssuer.getInstance();
            return tokenIssuer.issue(tokenReqDTO);
        } catch (InvalidOAuthClientException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error occurred while issuing access token for Client ID : " +
                        tokenReqDTO.getClientId() + ", User ID: " + tokenReqDTO.getResourceOwnerUsername() +
                        ", Scope : " + Arrays.toString(tokenReqDTO.getScope()) + " and Grant Type : " + tokenReqDTO.getGrantType(), e);
            }
            OAuth2AccessTokenRespDTO tokenRespDTO = new OAuth2AccessTokenRespDTO();
            tokenRespDTO.setError(true);
            tokenRespDTO.setErrorCode(OAuth2ErrorCodes.INVALID_CLIENT);
            tokenRespDTO.setErrorMsg("Invalid Client");
            return tokenRespDTO;
        } catch (Exception e) { // in case of an error, consider it as a system error
            log.error("Error occurred while issuing the access token for Client ID : " +
                    tokenReqDTO.getClientId() + ", User ID " + tokenReqDTO.getResourceOwnerUsername() +
                    ", Scope : " + Arrays.toString(tokenReqDTO.getScope()) + " and Grant Type : " + tokenReqDTO.getGrantType(), e);
            OAuth2AccessTokenRespDTO tokenRespDTO = new OAuth2AccessTokenRespDTO();
            tokenRespDTO.setError(true);
            tokenRespDTO.setErrorCode(OAuth2ErrorCodes.SERVER_ERROR);
            tokenRespDTO.setErrorMsg("Server Error");
            return tokenRespDTO;
        }
    }

    /**
     * Revoke tokens issued to OAuth clients
     *
     * @param revokeRequestDTO DTO representing consumerKey, consumerSecret and tokens[]
     * @return revokeRespDTO DTO representing success or failure message
     */
    public OAuthRevocationResponseDTO revokeTokenByOAuthClient(OAuthRevocationRequestDTO revokeRequestDTO) {
        //fix here remove associated cache entry
        TokenMgtDAO tokenMgtDAO = new TokenMgtDAO();
        OAuthRevocationResponseDTO revokeResponseDTO = new OAuthRevocationResponseDTO();
        try {
            if (StringUtils.isNotEmpty(revokeRequestDTO.getConsumerKey()) &&
                    StringUtils.isNotEmpty(revokeRequestDTO.getToken())) {
                if (!OAuth2Util.authenticateClient(revokeRequestDTO.getConsumerKey(), revokeRequestDTO.getConsumerSecret())) {
                    OAuthRevocationResponseDTO revokeRespDTO = new OAuthRevocationResponseDTO();
                    revokeRespDTO.setError(true);
                    revokeRespDTO.setErrorCode(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
                    revokeRespDTO.setErrorMsg("Unauthorized Client");
                    return revokeRespDTO;
                }
                boolean refreshTokenFirst = false;
                if (revokeRequestDTO.getToken_type() != null && REFRESH_TOKEN.equals(revokeRequestDTO.getToken_type())) {
                    refreshTokenFirst = true;
                }
                if (refreshTokenFirst) {
                    RefreshTokenValidationDataDO refreshTokenDO = tokenMgtDAO.validateRefreshToken(
                            revokeRequestDTO.getConsumerKey(), revokeRequestDTO.getToken());
                    if (refreshTokenDO != null && StringUtils.isNotEmpty(refreshTokenDO.getRefreshTokenState()) &&
                            (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(
                                    refreshTokenDO.getRefreshTokenState()) ||
                                    OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED.equals(
                                            refreshTokenDO.getRefreshTokenState()))) {
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                revokeRequestDTO.getConsumerKey(), refreshTokenDO.getAuthorizedUser(),
                                OAuth2Util.buildScopeString(refreshTokenDO.getScope()));
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                revokeRequestDTO.getConsumerKey(), refreshTokenDO.getAuthorizedUser());
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(refreshTokenDO.getAccessToken());
                        tokenMgtDAO.revokeTokens(new String[]{refreshTokenDO.getAccessToken()});
                        addRevokeResponseHeaders(revokeResponseDTO, refreshTokenDO.getAccessToken(),
                                revokeRequestDTO.getToken(), refreshTokenDO.getAuthorizedUser());
                    } else {
                        AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveAccessToken(
                                revokeRequestDTO.getToken(), true);
                        if (accessTokenDO != null) {
                            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                    revokeRequestDTO.getConsumerKey(), accessTokenDO.getAuthzUser(),
                                    OAuth2Util.buildScopeString(accessTokenDO.getScope()));
                            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                    revokeRequestDTO.getConsumerKey(), accessTokenDO.getAuthzUser());
                            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(revokeRequestDTO.getToken());
                            tokenMgtDAO.revokeTokens(new String[]{revokeRequestDTO.getToken()});
                            addRevokeResponseHeaders(revokeResponseDTO, accessTokenDO.getAccessToken(),
                                    accessTokenDO.getRefreshToken(), refreshTokenDO.getAuthorizedUser());
                        }
                    }
                } else {
                    AccessTokenDO accessTokenDO = tokenMgtDAO.retrieveAccessToken(
                            revokeRequestDTO.getToken(), true);
                    if (accessTokenDO != null) {
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                revokeRequestDTO.getConsumerKey(), accessTokenDO.getAuthzUser(),
                                OAuth2Util.buildScopeString(accessTokenDO.getScope()));
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                revokeRequestDTO.getConsumerKey(), accessTokenDO.getAuthzUser());
                        org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(revokeRequestDTO.getToken());
                        tokenMgtDAO.revokeTokens(new String[]{revokeRequestDTO.getToken()});
                        addRevokeResponseHeaders(revokeResponseDTO, revokeRequestDTO.getToken(),
                                accessTokenDO.getRefreshToken(), accessTokenDO.getAuthzUser().toString());
                    } else {
                        RefreshTokenValidationDataDO refreshTokenDO = tokenMgtDAO.validateRefreshToken(
                                revokeRequestDTO.getConsumerKey(), revokeRequestDTO.getToken());
                        if (refreshTokenDO != null && StringUtils.isNotEmpty(refreshTokenDO.getRefreshTokenState()) &&
                                (OAuthConstants.TokenStates.TOKEN_STATE_ACTIVE.equals(
                                        refreshTokenDO.getRefreshTokenState()) ||
                                        OAuthConstants.TokenStates.TOKEN_STATE_EXPIRED.equals(
                                                refreshTokenDO.getRefreshTokenState()))) {
                            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                    revokeRequestDTO.getConsumerKey(), refreshTokenDO.getAuthorizedUser(),
                                    OAuth2Util.buildScopeString(refreshTokenDO.getScope()));
                            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(
                                    revokeRequestDTO.getConsumerKey(), refreshTokenDO.getAuthorizedUser());
                            org.wso2.carbon.identity.oauth.OAuthUtil.clearOAuthCache(refreshTokenDO.getAccessToken());
                            tokenMgtDAO.revokeTokens(new String[]{refreshTokenDO.getAccessToken()});
                            addRevokeResponseHeaders(revokeResponseDTO, refreshTokenDO.getAccessToken(),
                                    revokeRequestDTO.getToken(), refreshTokenDO.getAuthorizedUser());
                        }
                    }
                }
                return revokeResponseDTO;
            } else {
                revokeResponseDTO.setError(true);
                revokeResponseDTO.setErrorCode(OAuth2ErrorCodes.INVALID_REQUEST);
                revokeResponseDTO.setErrorMsg("Invalid revocation request");
                return revokeResponseDTO;
            }

        } catch (IdentityException e) {
            log.error("Error occurred while revoking authorization grant for applications", e);
            OAuthRevocationResponseDTO revokeRespDTO = new OAuthRevocationResponseDTO();
            revokeRespDTO.setError(true);
            revokeRespDTO.setErrorCode(OAuth2ErrorCodes.SERVER_ERROR);
            revokeRespDTO.setErrorMsg("Error occurred while revoking authorization grant for applications");
            return revokeRespDTO;
        } catch (InvalidOAuthClientException e) {
            log.error("Unauthorized Client", e);
            OAuthRevocationResponseDTO revokeRespDTO = new OAuthRevocationResponseDTO();
            revokeRespDTO.setError(true);
            revokeRespDTO.setErrorCode(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
            revokeRespDTO.setErrorMsg("Unauthorized Client");
            return revokeRespDTO;
        }
    }

    /**
     * Returns an array of claims of the authorized user. This is for the
     * OpenIDConnect user-end-point implementation.
     * <p/>
     * TODO : 1. Should return the userinfo response instead.
     * TODO : 2. Should create another service API for userinfo endpoint
     *
     * @param accessTokenIdentifier
     * @return
     * @throws IdentityException
     */
    public Claim[] getUserClaims(String accessTokenIdentifier) {

        OAuth2TokenValidationRequestDTO reqDTO = new OAuth2TokenValidationRequestDTO();
        OAuth2TokenValidationRequestDTO.OAuth2AccessToken accessToken = reqDTO.new OAuth2AccessToken();
        accessToken.setTokenType("bearer");
        accessToken.setIdentifier(accessTokenIdentifier);
        reqDTO.setAccessToken(accessToken);
        OAuth2TokenValidationResponseDTO respDTO =
                new OAuth2TokenValidationService().validate(reqDTO);

        String username = respDTO.getAuthorizedUser();
        if (username == null) { // invalid token
            log.debug(respDTO.getErrorMsg());
            return new Claim[0];
        }
        String[] scope = respDTO.getScope();
        boolean isOICScope = false;
        for (String curScope : scope) {
            if ("openid".equals(curScope)) {
                isOICScope = true;
            }
        }
        if (!isOICScope) {
            log.error("AccessToken does not have the openid scope");
            return new Claim[0];
        }

        // TODO : this code is ugly
        String profileName = "default"; // TODO : configurable
        String tenantDomain = MultitenantUtils.getTenantDomain(username);
        String tenatUser = MultitenantUtils.getTenantAwareUsername(username);

        List<Claim> claimsList = new ArrayList<Claim>();

        // MUST claim
        // http://openid.net/specs/openid-connect-basic-1_0-22.html#id_res
        Claim subClaim = new Claim();
        subClaim.setClaimUri("sub");
        subClaim.setValue(username);
        claimsList.add(subClaim);

        try {
            UserStoreManager userStore =
                    IdentityTenantUtil.getRealm(tenantDomain, tenatUser)
                            .getUserStoreManager();
            // externel configured claims
            String[] claims = OAuthServerConfiguration.getInstance().getSupportedClaims();
            if (claims != null) {
                Map<String, String> extClaimsMap =
                        userStore.getUserClaimValues(username, claims,
                                profileName);
                for (Map.Entry<String, String> entry : extClaimsMap.entrySet()){
                    Claim curClaim = new Claim();
                    curClaim.setClaimUri(entry.getKey());
                    curClaim.setValue(entry.getValue());
                    claimsList.add(curClaim);
                }
            }
            // default claims
            String[] defaultClaims = new String[3];
            defaultClaims[0] = "http://wso2.org/claims/emailaddress";
            defaultClaims[1] = "http://wso2.org/claims/givenname";
            defaultClaims[2] = "http://wso2.org/claims/lastname";
            String emailAddress = null;
            String firstName = null;
            String lastName = null;
            Map<String, String> defClaimsMap =
                    userStore.getUserClaimValues(username,
                            defaultClaims,
                            profileName);
            if (defClaimsMap.get(defaultClaims[0]) != null) {
                emailAddress = defClaimsMap.get(defaultClaims[0]);
                Claim email = new Claim();
                email.setClaimUri("email");
                email.setValue(emailAddress);
                claimsList.add(email);
                Claim prefName = new Claim();
                prefName.setClaimUri("preferred_username");
                prefName.setValue(emailAddress.split("@")[0]);
                claimsList.add(prefName);
            }
            if (defClaimsMap.get(defaultClaims[1]) != null) {
                firstName = defClaimsMap.get(defaultClaims[1]);
                Claim givenName = new Claim();
                givenName.setClaimUri("given_name");
                givenName.setValue(firstName);
                claimsList.add(givenName);
            }
            if (defClaimsMap.get(defaultClaims[2]) != null) {
                lastName = defClaimsMap.get(defaultClaims[2]);
                Claim familyName = new Claim();
                familyName.setClaimUri("family_name");
                familyName.setValue(lastName);
                claimsList.add(familyName);
            }
            if (firstName != null && lastName != null) {
                Claim name = new Claim();
                name.setClaimUri("name");
                name.setValue(firstName + " " + lastName);
                claimsList.add(name);
            }

        } catch (Exception e) {
            log.error("Error while reading user claims ", e);
        }

        Claim[] allClaims = new Claim[claimsList.size()];
        for (int i = 0; i < claimsList.size(); i++) {
            allClaims[i] = claimsList.get(i);
        }
        return allClaims;
    }

    private void addRevokeResponseHeaders(OAuthRevocationResponseDTO revokeResponseDTP, String accessToken, String refreshToken, String authorizedUser) {

        List<ResponseHeader> respHeaders = new ArrayList<>();
        ResponseHeader header = new ResponseHeader();
        header.setKey("RevokedAccessToken");
        header.setValue(accessToken);
        respHeaders.add(header);
        header = new ResponseHeader();
        header.setKey("AuthorizedUser");
        header.setValue(authorizedUser);
        respHeaders.add(header);
        header = new ResponseHeader();
        header.setKey("RevokedRefreshToken");
        header.setValue(refreshToken);
        respHeaders.add(header);
        revokeResponseDTP.setResponseHeaders(respHeaders.toArray(new ResponseHeader[respHeaders.size()]));
    }
}
