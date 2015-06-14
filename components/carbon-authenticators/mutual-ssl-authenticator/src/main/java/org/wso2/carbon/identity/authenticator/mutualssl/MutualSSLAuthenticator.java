/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authenticator.mutualssl;

import org.apache.axiom.om.util.Base64;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;
import org.wso2.carbon.core.services.authentication.CarbonServerAuthenticator;
import org.wso2.carbon.core.services.util.CarbonAuthenticationUtil;
import org.wso2.carbon.identity.authenticator.mutualssl.internal.MutualSSLAuthenticatorServiceComponent;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.AuthenticationObserver;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Authenticator for certificate based two-way authentication
 */
public class MutualSSLAuthenticator implements CarbonServerAuthenticator {

    private static final int DEFAULT_PRIORITY_LEVEL = 5;
    private static final String AUTHENTICATOR_NAME = "MutualSSLAuthenticator";
    private static final String MUTUAL_SSL_URL = "http://mutualssl.carbon.wso2.org";

    /**
     * Header name of the username for mutual ssl authentication
     */
    private static final String USERNAME_HEADER = "UsernameHeader";

    /**
     * Configuration parameter name for trusted certificates list
     */
    private static final String WHITE_LIST = "WhiteList";

    /**
     * Configuration parameter name for enabling and disabling the trusted certificates list
     */
    private static final String WHITE_LIST_ENABLED = "WhiteListEnabled";

    /**
     * Attribute name for reading client certificate in the request
     */
    private static final String JAVAX_SERVLET_REQUEST_CERTIFICATE = "javax.servlet.request.X509Certificate";

    /**
     * Character encoding for Base64 to String conversions
     */
    private static final String CHARACTER_ENCODING = "UTF-8";

    /**
     * Logger for the class
     */
    private static final Log log = LogFactory.getLog(MutualSSLAuthenticator.class);

    private static String usernameHeaderName = "UserName";
    private static String[] whiteList;
    private static boolean whiteListEnabled = false;
    private static boolean authenticatorInitialized = false;


    /**
     * Initialize Mutual SSL Authenticator Configuration
     */
    private synchronized static void init() {

        AuthenticatorsConfiguration authenticatorsConfiguration = AuthenticatorsConfiguration.getInstance();

        // Read configuration for mutual ssl authenticator
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);

        if (authenticatorConfig != null) {
            Map<String, String> configParameters = authenticatorConfig.getParameters();

            if (configParameters != null) {

                if (configParameters.containsKey(USERNAME_HEADER)) {
                    usernameHeaderName = configParameters.get(USERNAME_HEADER);
                }

                if (configParameters.containsKey(WHITE_LIST_ENABLED)) {
                    whiteListEnabled = Boolean.parseBoolean(configParameters.get(WHITE_LIST_ENABLED));

                    if (log.isDebugEnabled()) {
                        log.debug("Enabling trusted client certificates list : " + whiteListEnabled);
                    }
                }

                if (whiteListEnabled) {
                    // List of trusted thumbprints for clients is enabled
                    if (configParameters.containsKey(WHITE_LIST)) {
                        whiteList = configParameters.get(WHITE_LIST).trim().split(",");
                        int index = 0;
                        // Remove whitespaces in the thumbprints of white list
                        for (String thumbprint : whiteList) {
                            thumbprint = thumbprint.trim();
                            whiteList[index] = thumbprint;

                            if (log.isDebugEnabled()) {
                                log.debug("Client thumbprint " + thumbprint + " added to the white list");
                            }
                            index++;
                        }
                    } else {
                        log.error("Trusted client certificates list is enabled but empty");
                        return;
                    }
                }
                authenticatorInitialized = true;
            }

        } else {
            if (log.isDebugEnabled()) {
                log.debug(AUTHENTICATOR_NAME + " configuration is not set for initialization");
            }
        }
    }

    @Override
    public int getPriority() {
        AuthenticatorsConfiguration authenticatorsConfiguration =
                AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null && authenticatorConfig.getPriority() > 0) {
            return authenticatorConfig.getPriority();
        }
        return DEFAULT_PRIORITY_LEVEL;
    }

    @Override
    public boolean isDisabled() {
        AuthenticatorsConfiguration authenticatorsConfiguration =
                AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null) {
            return authenticatorConfig.isDisabled();
        }
        return false;
    }

    @Override
    public boolean authenticateWithRememberMe(MessageContext msgCxt) {
        return false;
    }

    @Override
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

    @Override
    public boolean isAuthenticated(MessageContext msgCxt) {
        boolean isAuthenticated = false;
        HttpServletRequest request = (HttpServletRequest) msgCxt.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        Object certObject = request.getAttribute(JAVAX_SERVLET_REQUEST_CERTIFICATE);
        try {
            if (certObject != null) {
                if (!authenticatorInitialized) {
                    init();
                }
                if (!authenticatorInitialized) {
                    log.error(AUTHENTICATOR_NAME + " failed initialization");
                    return false;
                }

                // <m:UserName xmlns:m="http://mutualssl.carbon.wso2.org"
                // soapenv:mustUnderstand="0">234</m:UserName>
                boolean trustedThumbprint = false;
                String thumbprint = null;

                if (certObject instanceof X509Certificate[]) {
                    X509Certificate[] cert = (X509Certificate[]) certObject;

                    if (whiteListEnabled && whiteList != null) {
                        // Client certificate is always in the index 0
                        thumbprint = getThumbPrint(cert[0]);

                        if (log.isDebugEnabled()) {
                            log.debug("Client certificate thumbprint is " + thumbprint);
                        }

                        for (String whiteThumbprint : whiteList) {
                            if (thumbprint.equals(whiteThumbprint)) {
                                // Thumbprint of the client certificate is in the trusted list
                                trustedThumbprint = true;

                                if (log.isDebugEnabled()) {
                                    log.debug("Client certificate thumbprint matched with the white list");
                                }
                                break;
                            }
                        }
                    }
                }

                if (!whiteListEnabled || trustedThumbprint) {

                    // WhiteList is disabled or client certificate is in the trusted list
                    String userName = null;
                    String usernameInHeader = request.getHeader(usernameHeaderName);
                    boolean validHeader = false;

                    if (StringUtils.isNotEmpty(usernameInHeader)) {
                        //username is received in HTTP header encoded in base64
                        byte[] base64DecodedByteArray = Base64.decode(usernameInHeader);
                        userName = new String(base64DecodedByteArray, CHARACTER_ENCODING);
                        validHeader = true;

                        if (log.isDebugEnabled()) {
                            log.debug("Username for Mutual SSL : " + userName);
                        }
                    }

                    if (StringUtils.isEmpty(userName)) {
                        // Username is not received in HTTP Header. Check for SOAP header
                        SOAPEnvelope envelope = msgCxt.getEnvelope();
                        SOAPHeader header = envelope.getHeader();

                        if (header != null) {
                            List<SOAPHeaderBlock> headers = header.getHeaderBlocksWithNSURI(MUTUAL_SSL_URL);

                            if (headers != null) {
                                for (SOAPHeaderBlock soapHeaderBlock : headers) {
                                    if (usernameHeaderName.equals(soapHeaderBlock.getLocalName())) {
                                        // Username is received in SOAP header
                                        userName = soapHeaderBlock.getText();
                                        validHeader = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (!validHeader && log.isDebugEnabled()) {
                        log.debug("'" + usernameHeaderName + "'" + " header is not received in HTTP or SOAP header");
                    }

                    if (StringUtils.isNotEmpty(userName)) {
                        String tenantDomain = MultitenantUtils.getTenantDomain(userName);
                        userName = MultitenantUtils.getTenantAwareUsername(userName);
                        TenantManager tenantManager =
                                MutualSSLAuthenticatorServiceComponent.getRealmService().getTenantManager();
                        int tenantId = tenantManager.getTenantId(tenantDomain);

                        handleAuthenticationStarted(tenantId);

                        UserStoreManager userstore =
                                MutualSSLAuthenticatorServiceComponent.getRealmService().getTenantUserRealm(tenantId)
                                        .getUserStoreManager();

                        if (userstore.isExistingUser(userName)) {
                            // Username used for mutual ssl authentication is a valid user
                            isAuthenticated = true;
                        }

                        if (isAuthenticated) {
                            CarbonAuthenticationUtil.onSuccessAdminLogin(request.getSession(), userName, tenantId,
                                    tenantDomain, "Mutual SSL Authentication");
                            handleAuthenticationCompleted(tenantId, true);
                            isAuthenticated = true;
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Authentication rquest is rejected. User " + userName +
                                        " does not exist in userstore");
                            }
                            CarbonAuthenticationUtil.onFailedAdminLogin(request.getSession(), userName, tenantId,
                                    "Mutual SSL Authentication", "User does not exist in userstore");
                            handleAuthenticationCompleted(tenantId, false);
                            isAuthenticated = false;
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Client Thumbprint " + thumbprint + " is not in the White List of " + AUTHENTICATOR_NAME);
                    }
                }

            } else {
                throw new IllegalStateException("The certificate cannot be empty");
            }
        } catch (Exception e) {
            log.error("Error authenticating the user " + e.getMessage(), e);
        }
        return isAuthenticated;
    }

    @Override
    public boolean isHandle(MessageContext msgCxt) {
        boolean canHandle = false;

        if (!isDisabled()) {

            if (!authenticatorInitialized) {
                init();
                if (!authenticatorInitialized) {
                    return canHandle;
                }
            }

            HttpServletRequest request = (HttpServletRequest) msgCxt.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
            String authorizationHeader = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
            // This authenticator should kickin only if authorization headers are null
            if (authorizationHeader == null) {
                Object certObject = request.getAttribute(JAVAX_SERVLET_REQUEST_CERTIFICATE);
                if (certObject != null) {
                    SOAPEnvelope envelope = msgCxt.getEnvelope();
                    SOAPHeader header = envelope.getHeader();
                    boolean validHeader = false;

                    if (header != null) {
                        List<SOAPHeaderBlock> headers = header.getHeaderBlocksWithNSURI(MUTUAL_SSL_URL);
                        if (headers != null) {
                            for (SOAPHeaderBlock soapHeaderBlock : headers) {
                                if (usernameHeaderName.equals(soapHeaderBlock.getLocalName())) {
                                    //Username can be in SOAP Header
                                    canHandle = true;
                                    validHeader = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!canHandle && StringUtils.isNotEmpty(request.getHeader(usernameHeaderName))) {
                        validHeader = true;
                        // Username is received in HTTP Header
                        canHandle = true;
                    }

                    if (!validHeader && log.isDebugEnabled()) {
                        log.debug("'" + usernameHeaderName + "'" + " header is not received in HTTP or SOAP header");
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Server is not picking up the client certificate. Mutual SSL authentication is not" +
                                "done");
                    }
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("MutualSSLAuthenticator is Disabled.");
            }
        }
        return canHandle;
    }

    /**
     * Helper method to retrieve the thumbprint of a X509 certificate
     *
     * @param cert X509 certificate
     * @return Thumbprint of the X509 certificate
     * @throws NoSuchAlgorithmException
     * @throws CertificateEncodingException
     */
    private String getThumbPrint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] certEncoded = cert.getEncoded();
        md.update(certEncoded);
        return hexify(md.digest());
    }

    /**
     * Helper method to hexify a byte array.
     *
     * @param bytes Bytes of message digest
     * @return Hexadecimal representation
     */
    private String hexify(byte bytes[]) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        for (byte byteValue : bytes) {
            builder.append(hexDigits[(byteValue & 0xf0) >> 4]).append(hexDigits[byteValue & 0x0f]);
        }
        return builder.toString();
    }

    private void handleAuthenticationStarted(int tenantId) {
        BundleContext bundleContext = MutualSSLAuthenticatorServiceComponent.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                    new ServiceTracker(bundleContext,
                            AuthenticationObserver.class.getName(), null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).startedAuthentication(tenantId);
                }
            }
            tracker.close();
        }
    }

    private void handleAuthenticationCompleted(int tenantId, boolean isSuccessful) {
        BundleContext bundleContext = MutualSSLAuthenticatorServiceComponent.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                    new ServiceTracker(bundleContext,
                            AuthenticationObserver.class.getName(), null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).completedAuthentication(
                            tenantId, isSuccessful);
                }
            }
            tracker.close();
        }
    }

}
