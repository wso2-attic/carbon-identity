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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.handler.request.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.context.SessionContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.handler.request.RequestCoordinator;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedIdPData;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ACCOUNT_DISABLED_CLAIM_URI;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ACCOUNT_LOCKED_CLAIM_URI;
import static org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants.ACCOUNT_UNLOCK_TIME_CLAIM_URI;

/**
 * Request Coordinator
 */
public class DefaultRequestCoordinator implements RequestCoordinator {

    private static final Log log = LogFactory.getLog(DefaultRequestCoordinator.class);
    private static volatile DefaultRequestCoordinator instance;

    public static DefaultRequestCoordinator getInstance() {

        if (instance == null) {
            synchronized (DefaultRequestCoordinator.class) {
                if (instance == null) {
                    instance = new DefaultRequestCoordinator();
                }
            }
        }

        return instance;
    }

    /**
     * Get authentication request cache entry
     * @param request Http servlet request
     * @return Authentication request cache entry
     */
    private AuthenticationRequestCacheEntry getAuthenticationRequestFromRequest(HttpServletRequest request) {

        return (AuthenticationRequestCacheEntry) request.getAttribute(FrameworkConstants.RequestAttribute.AUTH_REQUEST);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            AuthenticationContext context;
            AuthenticationRequestCacheEntry authRequest = null;
            String sessionDataKey = request.getParameter("sessionDataKey");

            boolean returning = false;
            // Check whether this is the start of the authentication flow.
            // 'type' parameter should be present if so. This parameter contains
            // the request type (e.g. samlsso) set by the calling servlet.
            // TODO: use a different mechanism to determine the flow start.
            if (request.getParameter("type") != null) {
                // Retrieve AuthenticationRequestCache Entry which is stored stored from servlet.
                if (sessionDataKey != null) {
                    log.debug("retrieving authentication request from cache..");

                    authRequest = getAuthenticationRequest(request, sessionDataKey);

                    if (authRequest == null) {
                        // authRequest cannot be retrieved from cache. Cache
                        throw new FrameworkException("Invalid authentication request. Session data key : " + sessionDataKey);
                    }

                } else if (!Boolean.parseBoolean(request.getParameter(FrameworkConstants.LOGOUT))) {

                    // sessionDataKey is null and not a logout request
                    if (log.isDebugEnabled()) {
                        log.debug("Session data key is null in the request and not a logout request.");
                    }

                    FrameworkUtils.sendToRetryPage(request, response);
                }

                // if there is a cache entry, wrap the original request with params in cache entry
                if (authRequest != null) {
                    request = FrameworkUtils.getCommonAuthReqWithParams(request, authRequest);
                    FrameworkUtils.removeAuthenticationRequestFromCache(sessionDataKey);
                }
                context = initializeFlow(request, response);
            } else {
                returning = true;
                context = FrameworkUtils.getContextData(request);
            }

            if (context != null) {
                context.setReturning(returning);

                // if this is the flow start, store the original request in the context
                if (!context.isReturning() && authRequest != null) {
                    context.setAuthenticationRequest(authRequest.getAuthenticationRequest());
                }


                if (!context.isLogoutRequest()) {
                    FrameworkUtils.getAuthenticationRequestHandler().handle(request, response,
                                                                            context);
                } else {
                    FrameworkUtils.getLogoutRequestHandler().handle(request, response, context);
                }
            } else {
                if (log.isDebugEnabled()) {
                    String key = request.getParameter("sessionDataKey");
                    if (key == null) {
                        log.debug("Session data key is null in the request");
                    } else {
                        log.debug("Session data key  :  " + key);
                    }
                }
                log.error("Context does not exist. Probably due to invalidated cache");
                FrameworkUtils.sendToRetryPage(request, response);
            }
        } catch (Throwable e) {
            log.error("Exception in Authentication Framework", e);
            FrameworkUtils.sendToRetryPage(request, response);
        }
    }

    /**
     * When cache removed authentication request stored as request attribute, then taking request from request or
     * otherwise getting authentication request from cache
     *
     * @param request
     * @param sessionDataKey
     * @return
     */
    private AuthenticationRequestCacheEntry getAuthenticationRequest(HttpServletRequest request,
            String sessionDataKey) {

        AuthenticationRequestCacheEntry authRequest = getAuthenticationRequestFromRequest(request);
        if (authRequest == null) {
            authRequest = FrameworkUtils.getAuthenticationRequestFromCache(sessionDataKey);
        }
        return authRequest;
    }

    /**
     * Handles the initial request (from the calling servlet)
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     * @throws
     */
    protected AuthenticationContext initializeFlow(HttpServletRequest request,
                                                   HttpServletResponse response) throws FrameworkException {

        if (log.isDebugEnabled()) {
            log.debug("Initializing the flow");
        }


        // "sessionDataKey" - calling servlet maintains its state information
        // using this
        String callerSessionDataKey = request.getParameter(FrameworkConstants.SESSION_DATA_KEY);

        // "commonAuthCallerPath" - path of the calling servlet. This is the url
        // response should be sent to
        String callerPath = getCallerPath(request);

        // "type" - type of the request. e.g. samlsso, openid, oauth, passivests
        String requestType = request.getParameter(FrameworkConstants.RequestParams.TYPE);

        // "relyingParty"
        String relyingParty = request.getParameter(FrameworkConstants.RequestParams.ISSUER);

        // tenant domain
        String tenantDomain = getTenantDomain(request);

        // Store the request data sent by the caller
        AuthenticationContext context = new AuthenticationContext();
        context.setCallerSessionKey(callerSessionDataKey);
        context.setCallerPath(callerPath);
        context.setRequestType(requestType);
        context.setRelyingParty(relyingParty);
        context.setTenantDomain(tenantDomain);

        // generate a new key to hold the context data object
        String contextId = UUIDGenerator.generateUUID();
        context.setContextIdentifier(contextId);

        if (log.isDebugEnabled()) {
            log.debug("Framework contextId: " + contextId);
        }


        // if this a logout request from the calling servlet
        if (request.getParameter(FrameworkConstants.RequestParams.LOGOUT) != null) {

            if (log.isDebugEnabled()) {
                log.debug("Starting a logout flow");
            }

            context.setLogoutRequest(true);

            if (context.getRelyingParty() == null || context.getRelyingParty().trim().length() == 0) {

                if (log.isDebugEnabled()) {
                    log.debug("relyingParty param is null. This is a possible logout scenario.");
                }

                Cookie cookie = FrameworkUtils.getAuthCookie(request);

                if (cookie != null) {
                    context.setSessionIdentifier(cookie.getValue());
                }

                return context;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Starting an authentication flow");
            }
        }

        findPreviousAuthenticatedSession(request, context);
        buildOutboundQueryString(request, context);

        return context;
    }

    private String getCallerPath(HttpServletRequest request) throws FrameworkException {
        String callerPath = request.getParameter(FrameworkConstants.RequestParams.CALLER_PATH);
        try {
            if (callerPath != null) {
                callerPath = URLDecoder.decode(callerPath, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            throw new FrameworkException(e.getMessage(), e);
        }
        return callerPath;
    }

    private String getTenantDomain(HttpServletRequest request) throws FrameworkException {
        String tenantDomain = request.getParameter(FrameworkConstants.RequestParams.TENANT_DOMAIN);

        if (tenantDomain == null || tenantDomain.isEmpty() || "null".equals(tenantDomain)) {

            String tenantId = request.getParameter(FrameworkConstants.RequestParams.TENANT_ID);

            if (tenantId != null && !"-1234".equals(tenantId)) {
                try {
                    Tenant tenant = FrameworkServiceComponent.getRealmService().getTenantManager()
                            .getTenant(Integer.parseInt(tenantId));
                    if (tenant != null) {
                        tenantDomain = tenant.getDomain();
                    }
                } catch (Exception e) {
                    throw new FrameworkException(e.getMessage(), e);
                }
            } else {
                tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }
        }
        return tenantDomain;
    }

    protected void findPreviousAuthenticatedSession(HttpServletRequest request,
                                                    AuthenticationContext context) throws FrameworkException {

        // Get service provider chain
        SequenceConfig sequenceConfig = ConfigurationFacade.getInstance().getSequenceConfig(
                context.getRequestType(),
                request.getParameter(FrameworkConstants.RequestParams.ISSUER),
                context.getTenantDomain());

        Cookie cookie = FrameworkUtils.getAuthCookie(request);

        // if cookie exists user has previously authenticated
        if (cookie != null) {

            if (log.isDebugEnabled()) {
                log.debug(FrameworkConstants.COMMONAUTH_COOKIE
                          + " cookie is available with the value: " + cookie.getValue());
            }

            // get the authentication details from the cache
            SessionContext sessionContext = FrameworkUtils.getSessionContextFromCache(cookie
                                                                                              .getValue());

            if (sessionContext != null) {
                context.setSessionIdentifier(cookie.getValue());
                String appName = sequenceConfig.getApplicationConfig().getApplicationName();

                if (log.isDebugEnabled()) {
                    log.debug("Service Provider is: " + appName);
                }

                SequenceConfig previousAuthenticatedSeq = sessionContext
                        .getAuthenticatedSequences().get(appName);

                if (previousAuthenticatedSeq != null) {

                    if (log.isDebugEnabled()) {
                        log.debug("A previously authenticated sequence found for the SP: "
                                  + appName);
                    }

                    context.setPreviousSessionFound(true);
                    sequenceConfig = previousAuthenticatedSeq;
                    AuthenticatedUser authenticatedUser = sequenceConfig.getAuthenticatedUser();
                    String authenticatedUserTenantDomain = sequenceConfig.getAuthenticatedUser().getTenantDomain();

                    if (authenticatedUser != null) {
                        if (isUserAllowedToLogin(authenticatedUser)) {
                            // set the user for the current authentication/logout flow
                            context.setSubject(authenticatedUser);
                            if (log.isDebugEnabled()) {
                                log.debug("Already authenticated by username: " +
                                        authenticatedUser.getAuthenticatedSubjectIdentifier());
                            }
                            if (authenticatedUserTenantDomain != null) {
                                // set the user tenant domain for the current authentication/logout flow
                                context.setProperty("user-tenant-domain", authenticatedUserTenantDomain);

                                if (log.isDebugEnabled()) {
                                    log.debug("Authenticated user tenant domain: " + authenticatedUserTenantDomain);
                                }
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("User %s is not allowed to authenticate from previous session.",
                                        authenticatedUser.toString()));
                            }
                            context.setPreviousSessionFound(false);
                            FrameworkUtils.removeSessionContextFromCache(cookie.getValue());
                            sessionContext.setAuthenticatedIdPs(new HashMap<String, AuthenticatedIdPData>());
                        }
                    }
                }

                context.setPreviousAuthenticatedIdPs(sessionContext.getAuthenticatedIdPs());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to find the SessionContext from the cache. Possible cache timeout.");
                }
            }
        }

        context.setServiceProviderName(sequenceConfig.getApplicationConfig().getApplicationName());

        // set the sequence for the current authentication/logout flow
        context.setSequenceConfig(sequenceConfig);
    }

    private void buildOutboundQueryString(HttpServletRequest request, AuthenticationContext context)
            throws FrameworkException {

        // Build the outbound query string that will be sent to the authentication endpoint and
        // federated IdPs
        StringBuilder outboundQueryStringBuilder = new StringBuilder();
        outboundQueryStringBuilder.append(FrameworkUtils.getQueryStringWithConfiguredParams(request));

        if (StringUtils.isNotEmpty(outboundQueryStringBuilder.toString())) {
            outboundQueryStringBuilder.append("&");
        }

        try {
            outboundQueryStringBuilder.append("sessionDataKey=").append(context.getContextIdentifier())
                    .append("&relyingParty=").append(URLEncoder.encode(context.getRelyingParty(), "UTF-8")).append("&type=")
                    .append(context.getRequestType()).append("&sp=")
                    .append(URLEncoder.encode(context.getServiceProviderName(), "UTF-8")).append("&isSaaSApp=")
                    .append(context.getSequenceConfig().getApplicationConfig().isSaaSApp());
        } catch (UnsupportedEncodingException e) {
            throw new FrameworkException("Error while URL Encoding", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Outbound Query String: " + outboundQueryStringBuilder.toString());
        }

        context.setContextIdIncludedQueryParams(outboundQueryStringBuilder.toString());
        context.setOrignalRequestQueryParams(outboundQueryStringBuilder.toString());
    }

    /**
     * Checks whether AuthenticatedUser object contains a valid user for authentication.
     *
     * @param user Current authenticated user.
     * @return Returns false if user verification is failed.
     * @throws FrameworkException
     */
    private boolean isUserAllowedToLogin(AuthenticatedUser user) {

        if (user.isFederatedUser()) {
            return true;
        }

        int tenantId = IdentityTenantUtil.getTenantId(user.getTenantDomain());
        try {
            UserRealm userRealm = (UserRealm) FrameworkServiceComponent.getRealmService().getTenantUserRealm(tenantId);
            UserStoreManager userStoreManager = userRealm.getUserStoreManager().
                    getSecondaryUserStoreManager(user.getUserStoreDomain());

            if (userStoreManager == null) {
                userStoreManager = userRealm.getUserStoreManager();
            }
            if (userStoreManager.isExistingUser(user.getUserName())) {
                return !(isUserDisabled(userStoreManager, user) || isUserLocked(userStoreManager, user));
            } else {
                log.error("Trying to authenticate non existing user.");
            }
        } catch (UserStoreException e) {
            log.error("Error while checking existence of user: " + user.getUserName(), e);
        } catch (FrameworkException e) {
            log.error("Error while validating user: " + user.getUserName(), e);
        }
        return false;
    }

    /**
     * Checks whether the given user is disabled and returns true for disabled users.
     *
     * @param userStoreManager Instance of user store manager called.
     * @param user             Current authenticated user.
     * @return User is disabled or not, returns true for disabled users.
     * @throws FrameworkException
     */
    private boolean isUserDisabled(UserStoreManager userStoreManager, AuthenticatedUser user)
            throws FrameworkException {

        if (!ConfigurationFacade.getInstance().isAuthPolicyAccountDisableCheck()) {
            return false;
        }

        String accountDisabledClaimValue = getClaimValue(
                user.getUserName(), userStoreManager, ACCOUNT_DISABLED_CLAIM_URI);
        return Boolean.parseBoolean(accountDisabledClaimValue);
    }

    /**
     * Checks whether the given user is locked and returns true for locked users.
     *
     * @param userStoreManager Instance of user store manager called.
     * @param user             Current authenticated user.
     * @return User account is locked or not returns true for locked users.
     * @throws FrameworkException
     */
    private boolean isUserLocked(UserStoreManager userStoreManager, AuthenticatedUser user) throws FrameworkException {

        if (!ConfigurationFacade.getInstance().isAuthPolicyAccountLockCheck()) {
            return false;
        }

        String accountLockedClaimValue = getClaimValue(user.getUserName(), userStoreManager, ACCOUNT_LOCKED_CLAIM_URI);
        boolean accountLocked = Boolean.parseBoolean(accountLockedClaimValue);

        if (accountLocked) {
            long unlockTime = 0;
            String accountUnlockTimeClaimValue = getClaimValue(
                    user.getUserName(), userStoreManager, ACCOUNT_UNLOCK_TIME_CLAIM_URI);

            if (NumberUtils.isNumber(accountUnlockTimeClaimValue)) {
                unlockTime = Long.parseLong(accountUnlockTimeClaimValue);
            }

            if (unlockTime != 0 && System.currentTimeMillis() >= unlockTime) {
                return false;
            }
        }
        return accountLocked;
    }

    /**
     * This method retrieves requested claim value from the user store
     *
     * @param username         Current authenticated users' username.
     * @param userStoreManager Instance of user store manager called.
     * @param claimURI         The claim URI.
     * @return Claim value as a String.
     * @throws FrameworkException
     */
    private String getClaimValue(String username, UserStoreManager userStoreManager, String claimURI) throws
            FrameworkException {

        try {
            Map<String, String> values = userStoreManager.getUserClaimValues(username, new String[]{claimURI},
                    UserCoreConstants.DEFAULT_PROFILE);
            if (log.isDebugEnabled()) {
                log.debug(String.format("%s claim value of user %s is set to: " + values.get(claimURI),
                        claimURI, username));
            }
            return values.get(claimURI);

        } catch (UserStoreException e) {
            throw new FrameworkException("Error occurred while retrieving claim: " + claimURI, e);
        }
    }
}
