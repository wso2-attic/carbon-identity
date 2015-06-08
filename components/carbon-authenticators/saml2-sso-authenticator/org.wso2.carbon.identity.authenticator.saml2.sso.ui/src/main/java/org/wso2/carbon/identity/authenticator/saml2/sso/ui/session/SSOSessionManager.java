package org.wso2.carbon.identity.authenticator.saml2.sso.ui.session;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authenticator.saml2.sso.ui.internal.SAML2SSOAuthFEDataHolder;
import org.wso2.carbon.ui.CarbonSSOSessionManager;
import org.wso2.carbon.ui.CarbonSecuredHttpContext;
import org.wso2.carbon.ui.CarbonUIAuthenticator;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SSOSessionManager {

    public static final Log log = LogFactory.getLog(SSOSessionManager.class);

    private static volatile SSOSessionManager instance;
    private Map sessionMap = new ConcurrentHashMap<String, HttpSession>();

    public static SSOSessionManager getInstance() {

        if (instance == null) {
            synchronized (SSOSessionManager.class) {
                if (instance == null) {
                    instance = new SSOSessionManager();
                }
            }
        }

        return instance;
    }

    public void addSession(String sessionIndex, HttpSession httpSession) {
        sessionMap.put(sessionIndex, httpSession);
    }

    public void removeSession(String sessionIndex) {
        if (sessionMap.containsKey(sessionIndex)) {
            sessionMap.remove(sessionIndex);
        }
    }

    public HttpSession getSession(String sessionIndex) {

        Object session = sessionMap.get(sessionIndex);

        if (session != null) {
            return (HttpSession) session;
        }

        return null;
    }

    public void handleLogout(String sessionIndex) {

        HttpSession session = (HttpSession) sessionMap.get(sessionIndex);

        if (session == null) {
            //send cluster message
            sendSessionInvalidationClusterMessage(sessionIndex);
            return;
        }

        CarbonSSOSessionManager ssoSessionManager = SAML2SSOAuthFEDataHolder.getInstance()
                .getCarbonSSOSessionManager();

        // mark this session as invalid.
        ssoSessionManager.makeSessionInvalid(sessionIndex);

        String username = (String) session.getAttribute(CarbonSecuredHttpContext.LOGGED_USER);
        log.info("Invalidating session for user " + username);

        // invalidating backend session
        try {
            CarbonUIAuthenticator authenticator =
                    (CarbonUIAuthenticator) session.getAttribute(CarbonSecuredHttpContext.CARBON_AUTHNETICATOR);
            if (authenticator != null) {
                authenticator.unauthenticate(session);
                log.debug("Backend session invalidated");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        // clearing front end session
        session.setAttribute("authenticated", false);
        session.removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);
        session.getServletContext().removeAttribute(CarbonSecuredHttpContext.LOGGED_USER);

        removeSession(sessionIndex);

        try {
            session.invalidate();
        } catch (Exception ignored) {
            log.error(ignored.getMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("Cleared authenticated session " + session.getId());
        }

    }

    public void sendSessionInvalidationClusterMessage(String sessionIndex) {

        SessionClusterMessage clusterMessage = new SessionClusterMessage();
        clusterMessage.setMessageId(UUID.randomUUID());
        clusterMessage.setSessionIndex(sessionIndex);

        ClusteringAgent clusteringAgent = SAML2SSOAuthFEDataHolder.getInstance()
                .getConfigurationContextService().getServerConfigContext().getAxisConfiguration()
                .getClusteringAgent();

        if (clusteringAgent != null) {
            int numberOfRetries = 0;

            while (numberOfRetries < 60) {
                try {
                    clusteringAgent.sendMessage(clusterMessage, true);
                    log.info("Sent [" + clusterMessage + "]");
                    break;
                } catch (ClusteringFault e) {
                    numberOfRetries++;

                    if (numberOfRetries < 60) {
                        log.warn(
                                "Could not send SSOSessionInvalidationClusterMessage. Retry will be attempted in 2s. Request: "
                                        + clusterMessage, e);
                    } else {
                        log.error(
                                "Could not send SSOSessionInvalidationClusterMessage. Several retries failed. Request:"
                                        + clusterMessage, e);
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}