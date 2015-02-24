package org.wso2.carbon.identity.application.authenticator.openid.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.common.model.Property;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class SampleAuthenticator extends AbstractApplicationAuthenticator
        implements FederatedApplicationAuthenticator {

    private static final long serialVersionUID = -8097512332218044859L;

    private static Log log = LogFactory.getLog(SampleAuthenticator.class);

    @Override
    public boolean canHandle(HttpServletRequest request) {

        String protocolIdentifier = request
                .getParameter("my-custom-authentication-protocl");

        if (protocolIdentifier != null) {
            log.info("Sample SSO Authenticator : " + protocolIdentifier);
            return true;
        }
        return false;
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        log.info("Sample SSO Authenticator : " + context.getContextIdentifier());
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

    }

    @Override
    protected void initiateLogoutRequest(HttpServletRequest request,
                                         HttpServletResponse response, AuthenticationContext context)
            throws LogoutFailedException {

    }

    @Override
    protected void processLogoutResponse(HttpServletRequest request,
                                         HttpServletResponse response, AuthenticationContext context)
            throws LogoutFailedException {
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getFriendlyName() {
        return "sampleauth";
    }

    @Override
    public String getName() {
        return "sampleauth";
    }

    @Override
    public List<Property> getConfigurationProperties() {

        List<Property> configProperties = new ArrayList<Property>();

        Property serverUrl = new Property();
        serverUrl.setDisplayName("Server Url");
        serverUrl.setName("server-url");
        serverUrl
                .setDescription("Enter value corresponding to the authetication server.");
        //configProperties.
        configProperties.add(serverUrl);
        return configProperties;

    }

}
