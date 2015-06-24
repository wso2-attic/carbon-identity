/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.scim.provider.impl;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonConstants;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonUtils;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.charon.core.encoder.Decoder;
import org.wso2.charon.core.encoder.Encoder;
import org.wso2.charon.core.encoder.json.JSONDecoder;
import org.wso2.charon.core.encoder.json.JSONEncoder;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.FormatNotSupportedException;
import org.wso2.charon.core.exceptions.UnauthorizedException;
import org.wso2.charon.core.extensions.AuthenticationHandler;
import org.wso2.charon.core.extensions.AuthenticationInfo;
import org.wso2.charon.core.extensions.CharonManager;
import org.wso2.charon.core.extensions.TenantDTO;
import org.wso2.charon.core.extensions.TenantManager;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.protocol.ResponseCodeConstants;
import org.wso2.charon.core.protocol.endpoints.AbstractResourceEndpoint;
import org.wso2.charon.core.schema.SCIMConstants;

import java.util.HashMap;
import java.util.Map;

public class IdentitySCIMManager implements CharonManager {
    private static final String INSTANCE = "instance";
    //private TenantManager tenantManager;
    private static Log log = LogFactory.getLog(IdentitySCIMManager.class);
    private static volatile IdentitySCIMManager identitySCIMManager;
    private static Map<String, Encoder> encoderMap = new HashMap<String, Encoder>();
    private static Map<String, Decoder> decoderMap = new HashMap<String, Decoder>();
    private static Map<String, Map> authenticators = new HashMap<String, Map>();
    private static Map<String, String> endpointURLs = new HashMap<String, String>();

    private IdentitySCIMManager() throws CharonException {
        init();
    }

    /**
     * Should return the static instance of CharonManager implementation.
     * Read the config and initialize extensions as specified in the config.
     *
     * @return
     */
    public static IdentitySCIMManager getInstance() throws CharonException {
        if (identitySCIMManager == null) {
            synchronized (IdentitySCIMManager.class) {
                if (identitySCIMManager == null) {
                    identitySCIMManager = new IdentitySCIMManager();
                    return identitySCIMManager;
                } else {
                    return identitySCIMManager;
                }
            }
        } else {
            return identitySCIMManager;
        }
    }

    /**
     * Perform initialization at the deployment of the webapp.
     */
    private void init() throws CharonException {
        //TODO:read config and init stuff, if nothing in config, make sure to initialize default stuff.

        //if no encoder/decoders provided by the configuration, register defaults.
        encoderMap.put(SCIMConstants.JSON, new JSONEncoder());
        decoderMap.put(SCIMConstants.JSON, new JSONDecoder());

        //register encoder,decoders in AbstractResourceEndpoint, since they are called with in the API
        registerCoders();

        //Define endpoint urls to be used in Location Header
        endpointURLs.put(SCIMConstants.USER_ENDPOINT, SCIMCommonUtils.getSCIMUserURL());
        endpointURLs.put(SCIMConstants.GROUP_ENDPOINT, SCIMCommonUtils.getSCIMGroupURL());

        //register endpoint URLs in AbstractResourceEndpoint since they are called with in the API
        registerEndpointURLs();
    }

    @Override
    public Encoder getEncoder(String format) throws FormatNotSupportedException {
        if (!encoderMap.containsKey(format)) {
            //Error is logged by the caller.
            throw new FormatNotSupportedException(ResponseCodeConstants.CODE_FORMAT_NOT_SUPPORTED,
                    ResponseCodeConstants.DESC_FORMAT_NOT_SUPPORTED);
        }
        return encoderMap.get(format);
    }

    @Override
    public Decoder getDecoder(String format) throws FormatNotSupportedException {
        if (!decoderMap.containsKey(format)) {
            //Error is logged by the caller.
            throw new FormatNotSupportedException(ResponseCodeConstants.CODE_FORMAT_NOT_SUPPORTED,
                    ResponseCodeConstants.DESC_FORMAT_NOT_SUPPORTED);
        }
        return decoderMap.get(format);

    }

    /*This method is no longer used.*/
    @Override
    public AuthenticationHandler getAuthenticationHandler(String authMechanism)
            throws CharonException {
        if (MapUtils.isNotEmpty(authenticators)) {
            Map authenticatorProperties = authenticators.get(authMechanism);
            if (MapUtils.isNotEmpty(authenticatorProperties)) {
                return (AuthenticationHandler) authenticatorProperties.get(INSTANCE);
            }
        }
        String error = "Requested authentication mechanism is not supported.";
        throw new CharonException(error);
    }

    @Override
    public UserManager getUserManager(String userName) throws CharonException {
        SCIMUserManager scimUserManager = null;
        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        String tenantLessUserName = MultitenantUtils.getTenantAwareUsername(userName);

        try {
            //get super tenant context and get realm service which is an osgi service
            RealmService realmService = (RealmService)
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService(RealmService.class);
            if (realmService != null) {
                int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                //get tenant's user realm
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                ClaimManager claimManager;
                if (userRealm != null) {
                    //get claim manager for manipulating attributes
                    claimManager = (ClaimManager) userRealm.getClaimManager();
                    //if tenantless username doesn't contain a domain, add domain to user name in order to comply with multiple user store feature. 
                    if (tenantLessUserName.indexOf(CarbonConstants.DOMAIN_SEPARATOR) < 0) {
                        String domain = UserCoreUtil.getDomainFromThreadLocal();
                        if (domain != null) {
                            tenantLessUserName = domain + CarbonConstants.DOMAIN_SEPARATOR + tenantLessUserName;
                        }
                    }

                    //check whether the user who is trying to obtain the realm is authorized
                    boolean isUserAuthorized = userRealm.getAuthorizationManager().isUserAuthorized(
                            tenantLessUserName, SCIMCommonConstants.PROVISIONING_ADMIN_PERMISSION,
                            SCIMCommonConstants.RESOURCE_TO_BE_AUTHORIZED);
                    if (!isUserAuthorized) {
                        String error = "User is not authorized to perform provisioning";
                        log.error(error);
                        throw new CharonException(error);
                    }
                    /*if the authenticated & authorized user is not set in the carbon context, set it,
                    coz we are going to refer it later to identify the SCIM providers registered for a particular consumer.*/
                    String authenticatedUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                    if (authenticatedUser == null) {
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(tenantLessUserName);
                        if (log.isDebugEnabled()) {
                            log.debug("User read from carbon context is null, hence setting " +
                                    "authenticated user: " + tenantLessUserName);
                        }
                    }
                    scimUserManager = new SCIMUserManager((UserStoreManager) userRealm.getUserStoreManager(),
                            userName, claimManager);
                }
            } else {
                String error = "Can not obtain carbon realm service..";
                throw new CharonException(error);
            }
            //get user store manager
        } catch (UserStoreException e) {
            String error = "Error obtaining user realm for the user: " + userName;
            throw new CharonException(error, e);
        }
        return scimUserManager;
    }

    public UserManager getUserManager(String userName, String accessPermission) throws CharonException {
        SCIMUserManager scimUserManager = null;
        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
        String tenantLessUserName = MultitenantUtils.getTenantAwareUsername(userName);

        try {
            //get super tenant context and get realm service which is an osgi service
            RealmService realmService = (RealmService)
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService(RealmService.class);
            if (realmService != null) {
                int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                //get tenant's user realm
                UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                ClaimManager claimManager;
                if (userRealm != null) {
                    //get claim manager for manipulating attributes
                    claimManager = (ClaimManager) userRealm.getClaimManager();
                    //if tenantless username doesn't contain a domain, add domain to user name in order to comply with multiple user store feature.
                    if (tenantLessUserName.indexOf(CarbonConstants.DOMAIN_SEPARATOR) < 0) {
                        String domain = UserCoreUtil.getDomainFromThreadLocal();
                        if (domain != null) {
                            tenantLessUserName = domain + CarbonConstants.DOMAIN_SEPARATOR + tenantLessUserName;
                        }
                    }

                    //check whether the user who is trying to obtain the realm is authorized
                    boolean isUserAuthorized = userRealm.getAuthorizationManager().isUserAuthorized(
                            tenantLessUserName, accessPermission,
                            SCIMCommonConstants.RESOURCE_TO_BE_AUTHORIZED);
                    if (!isUserAuthorized) {
                        String error = "User is not authorized to perform provisioning";
                        log.error(error);
                        throw new CharonException(error);
                    }
                    /*if the authenticated & authorized user is not set in the carbon context, set it,
                    coz we are going to refer it later to identify the SCIM providers registered for a particular consumer.*/
                    String authenticatedUser = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
                    if (authenticatedUser == null) {
                        PrivilegedCarbonContext.getThreadLocalCarbonContext().setUsername(tenantLessUserName);
                        if (log.isDebugEnabled()) {
                            log.debug("User read from carbon context is null, hence setting " +
                                    "authenticated user: " + tenantLessUserName);
                        }
                    }
                    scimUserManager = new SCIMUserManager((UserStoreManager) userRealm.getUserStoreManager(),
                            userName, claimManager);
                }
            } else {
                String error = "Can not obtain carbon realm service..";
                throw new CharonException(error);
            }
            //get user store manager
        } catch (UserStoreException e) {
            String error = "Error obtaining user realm for the user: " + userName;
            throw new CharonException(error, e);
        }
        return scimUserManager;
    }

    @Override
    public TenantManager getTenantManager() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AuthenticationInfo registerTenant(TenantDTO tenantDTO) throws CharonException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isAuthenticationSupported(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /*This method is no longer used..*/
    @Override
    public AuthenticationInfo handleAuthentication(Map<String, String> authHeaderMap)
            throws UnauthorizedException {
        try {
            String authType = identifyAuthType(authHeaderMap);
            Map authPropertyMap = authenticators.get(authType);
            if (authHeaderMap != null) {
                AuthenticationHandler authHandler = (AuthenticationHandler) authPropertyMap.get(INSTANCE);
                if (authHandler != null) {
                    authHandler.setCharonManager(this);
                    authHandler.isAuthenticated(authHeaderMap);
                    return authHandler.getAuthenticationInfo();
                }
            }
        } catch (CharonException e) {
            if (log.isDebugEnabled()) {
                log.debug("CharonException in handle authentication. ", e);
            }
            throw new UnauthorizedException("Error in handling authentication");
        }
        throw new UnauthorizedException();
    }

    /**
     * Register encoders and decoders in AbstractResourceEndpoint.
     */
    private void registerCoders() throws CharonException {
        if (!encoderMap.isEmpty()) {
            for (Map.Entry<String, Encoder> encoderEntry : encoderMap.entrySet()) {
                AbstractResourceEndpoint.registerEncoder(encoderEntry.getKey(), encoderEntry.getValue());
            }
        }
        if (!encoderMap.isEmpty()) {
            for (Map.Entry<String, Decoder> decoderEntry : decoderMap.entrySet()) {
                AbstractResourceEndpoint.registerDecoder(decoderEntry.getKey(), decoderEntry.getValue());
            }
        }

    }

    private void registerEndpointURLs() {
        if (MapUtils.isNotEmpty(endpointURLs)) {
            AbstractResourceEndpoint.registerResourceEndpointURLs(endpointURLs);
        }
    }

    /**
     * Identify the authentication mechanism, given the http headers sent in the SCIM API access request.
     *
     * @param authHeaders
     * @return
     * @throws CharonException
     */
    public String identifyAuthType(Map<String, String> authHeaders)
            throws CharonException, UnauthorizedException {
        String authorizationHeader = authHeaders.get(SCIMConstants.AUTHORIZATION_HEADER);
        String authenticationType = null;
        if (authorizationHeader != null) {
            authenticationType = authorizationHeader.split(" ")[0];
        } else {
            String error = "No Authorization header found";
            log.error(error);
            throw new UnauthorizedException();
        }
        if (SCIMConstants.AUTH_TYPE_BASIC.equals(authenticationType)) {
            return SCIMConstants.AUTH_TYPE_BASIC;
        } else if (SCIMConstants.AUTH_TYPE_OAUTH.equals(authenticationType)) {
            return SCIMConstants.AUTH_TYPE_OAUTH;
        } else if (authHeaders.get(SCIMConstants.AUTHENTICATION_TYPE_HEADER) != null) {
            return authHeaders.get(SCIMConstants.AUTHENTICATION_TYPE_HEADER);
        } else {
            String error = "Provided authentication headers do not contain supported authentication headers.";
            throw new CharonException(error);
        }
    }
}
