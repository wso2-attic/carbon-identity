package org.wso2.carbon.identity.authenticator.saml2.sso.ui.session;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;

import javax.servlet.http.HttpSession;
import java.util.UUID;

public class SessionClusterMessage extends ClusteringMessage {

    private static final long serialVersionUID = -711222207322463831L;

    private String sessionIndex;
    private UUID messageId;

    @Override
    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {

        SSOSessionManager sessionManager = SSOSessionManager.getInstance();
        HttpSession session = sessionManager.getSession(sessionIndex);

        if (session != null) {
            sessionManager.handleLogout(sessionIndex);
        }
    }

    @Override
    public ClusteringCommand getResponse() {
        return null;
    }

    public void setSessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public String toString() {
        return "SSOSessionInvalidationClusterMessage{sessionIndex=" + sessionIndex + "}";
    }
}