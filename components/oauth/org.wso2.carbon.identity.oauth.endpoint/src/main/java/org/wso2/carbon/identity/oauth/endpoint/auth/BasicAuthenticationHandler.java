/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.oauth.endpoint.auth;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.charon.core.exceptions.InternalServerException;
import org.wso2.charon.core.exceptions.UnauthorizedException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is the default BASIC-Auth authentication handler.
 */
public class BasicAuthenticationHandler implements OAuthAuthenticationHandler {

    private static Log log = LogFactory.getLog(BasicAuthenticationHandler.class);
    /* constants specific to this authenticator */
    private final String BASIC_AUTH_HEADER = "Basic";
    private final int DEFAULT_PRIORITY = 5;
    private final String PROPERTY_NAME_PRIORITY = "Priority";
    /* property map */
    private Map<String, String> properties;
    /* properties specific to this authenticator */
    private int priority;

    public void setDefaultPriority() {
        priority = DEFAULT_PRIORITY;
    }

    /**
     * Ideally this should be configurable. For the moment, hard code the priority.
     *
     * @return
     */
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean canHandle(Message message, ClassResourceInfo classResourceInfo) {
        // check the "Authorization" header and if "Basic" is there, can be handled.

        // get the map of protocol headers
        Map protocolHeaders = (TreeMap) message.get(Message.PROTOCOL_HEADERS);
        // get the value for Authorization Header
        List authzHeaders = (ArrayList) protocolHeaders.get(OAuthConstants.HTTP_REQ_HEADER_AUTHZ);
        if (authzHeaders != null) {
            // get the authorization header value, if provided
            String authzHeader = (String) authzHeaders.get(0);
            if (authzHeader != null && authzHeader.contains(BASIC_AUTH_HEADER)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthenticated(Message message, ClassResourceInfo classResourceInfo) {
        // extract authorization header and authenticate.

        // get the map of protocol headers
        Map protocolHeaders = (TreeMap) message.get(Message.PROTOCOL_HEADERS);
        // get the value for Authorization Header
        List authzHeaders = (ArrayList) protocolHeaders.get(OAuthConstants.HTTP_REQ_HEADER_AUTHZ);
        if (authzHeaders != null) {
            // get the authorization header value, if provided
            String authzHeader = (String) authzHeaders.get(0);

            // decode it and extract username and password
            byte[] decodedAuthHeader = Base64.decode(authzHeader.split(" ")[1]);
            String authHeader = new String(decodedAuthHeader, Charset.defaultCharset());
            String userName = authHeader.split(":")[0];
            String password = authHeader.split(":")[1];
            if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
                String tenantDomain = MultitenantUtils.getTenantDomain(userName);
                String tenantLessUserName = MultitenantUtils.getTenantAwareUsername(userName);

                try {
                    // get super tenant context and get realm service which is an osgi service
                    RealmService realmService = (RealmService) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                            .getOSGiService(RealmService.class, null);
                    if (realmService != null) {
                        int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
                        if (tenantId == -1) {
                            log.error("Invalid tenant domain " + tenantDomain);
                            return false;
                        }
                        // get tenant's user realm
                        UserRealm userRealm = realmService.getTenantUserRealm(tenantId);
                        boolean authenticated = userRealm.getUserStoreManager()
                                .authenticate(tenantLessUserName, password);
                        if (authenticated) {
                            //start the tenant flow from the user specified in the authorization header
                            PrivilegedCarbonContext.startTenantFlow();
                            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext
                                    .getThreadLocalCarbonContext();
                            carbonContext.setUsername(tenantLessUserName);
                            carbonContext.setTenantId(tenantId);
                            carbonContext.setTenantDomain(tenantDomain);
                            return true;
                        } else {
                            UnauthorizedException unauthorizedException = new UnauthorizedException(
                                    "Authentication failed for the user: " + tenantLessUserName + "@" + tenantDomain);
                            log.error(unauthorizedException.getDescription());
                            return false;
                        }
                    } else {
                        log.error("Error in getting Realm Service for user: " + userName);
                        InternalServerException internalServerException = new InternalServerException(
                                "Internal server error while authenticating the user: " + tenantLessUserName + "@"
                                        + tenantDomain);
                        log.error(internalServerException.getDescription());
                        return false;
                    }

                } catch (UserStoreException e) {
                    InternalServerException internalServerException = new InternalServerException(
                            "Internal server error while authenticating the user.");
                    log.error(internalServerException.getDescription(), e);
                    return false;
                }
            } else {
                UnauthorizedException unauthorizedException = new UnauthorizedException(
                        "Authentication required for this resource. Username or password not provided.");
                log.error(unauthorizedException.getDescription());
                return false;
            }
        } else {
            UnauthorizedException unauthorizedException = new UnauthorizedException(
                    "Authentication required for this resource. Authorization header not present in the request.");
            log.error(unauthorizedException.getDescription());
            return false;
        }

    }

    /**
     * To set the properties specific to each authenticator
     *
     * @param authenticatorProperties
     */
    public void setProperties(Map<String, String> authenticatorProperties) {
        // set the priority read from config
        this.properties = authenticatorProperties;
        String priorityString = properties.get(PROPERTY_NAME_PRIORITY);
        if (priorityString != null) {
            priority = Integer.parseInt(properties.get(PROPERTY_NAME_PRIORITY));
        } else {
            priority = DEFAULT_PRIORITY;
        }
    }

}
