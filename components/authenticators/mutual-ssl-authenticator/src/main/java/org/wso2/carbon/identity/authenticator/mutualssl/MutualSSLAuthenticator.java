package org.wso2.carbon.identity.authenticator.mutualssl;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
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

public class MutualSSLAuthenticator implements CarbonServerAuthenticator {

    private static final int DEFAULT_PRIORITY_LEVEL = 5;
    private static final String AUTHENTICATOR_NAME = "MutualSSLAuthenticator";
    private static final String MUTUAL_SSL_URL = "http://mutualssl.carbon.wso2.org";

    private static final Log log = LogFactory.getLog(MutualSSLAuthenticator.class);

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
        HttpServletRequest request =
                                     (HttpServletRequest) msgCxt.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        Object certObject = request.getAttribute("javax.servlet.request.X509Certificate");
        try {
            if (certObject != null) {

                // <m:UserName xmlns:m="http://mutualssl.carbon.wso2.org"
                // soap:mustUnderstand="0">234</m:UserName>

                SOAPEnvelope envelope = msgCxt.getEnvelope();
                SOAPHeader header = envelope.getHeader();
                if (header != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<SOAPHeaderBlock> headers =
                                                         header.getHeaderBlocksWithNSURI(MUTUAL_SSL_URL);
                    if (headers.size() != 1) {
                        throw new Exception("UseName SOAP header count should be exactly 1. Cannot process authentication because header count is " +
                                                    headers.size());
                    }
                    
                    SOAPHeaderBlock block = headers.get(0);
                    String userName = block.getText();
                    String tenantDomain = MultitenantUtils.getTenantDomain(userName);
                    userName = MultitenantUtils.getTenantAwareUsername(userName);                    
                    TenantManager tenantManager =
                    		MutualSSLAuthenticatorServiceComponent.getRealmService().getTenantManager();
                    int tenantId = tenantManager.getTenantId(tenantDomain);
                    
                    handleAuthenticationStarted(tenantId);                
                    
                    UserStoreManager userstore = MutualSSLAuthenticatorServiceComponent.getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
                    if (userstore.isExistingUser(userName)) {
                        isAuthenticated = true;    
                    }
                    
                    if (isAuthenticated) {
                    	CarbonAuthenticationUtil.onSuccessAdminLogin(request.getSession(), userName,
                                tenantId, tenantDomain, "Mutual SSL Authentication");
                        handleAuthenticationCompleted(tenantId, true);
                        return true;
                    } else {
                        log.error("Authentication Request is rejected. User doesnot exists in userstore");
                        CarbonAuthenticationUtil.onFailedAdminLogin(request.getSession(), userName,tenantId,
                                                                    "Mutual SSL Authentication", "User doesnot exists in userstore");
                        handleAuthenticationCompleted(tenantId, false);
                        return false;
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
        HttpServletRequest request =
                                     (HttpServletRequest) msgCxt.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        String authorizationHeader = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
        // this authenticator should kickin only if authorization headers are null
        if (authorizationHeader == null) {
            Object certObject = request.getAttribute("javax.servlet.request.X509Certificate");
            if (certObject != null) {
                SOAPEnvelope envelope = msgCxt.getEnvelope();
                SOAPHeader header = envelope.getHeader();
                if (header != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<SOAPHeaderBlock> headers =
                                                         header.getHeaderBlocksWithNSURI(MUTUAL_SSL_URL);
                    if (headers.size() == 1) {
                        canHandle = true;
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("MutualSSL is not configured properly. Server is not picking " +
                    		"up the client certificate. So no Mutual SSL authentication can happen");
                }
            }
        }
        return canHandle;
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
