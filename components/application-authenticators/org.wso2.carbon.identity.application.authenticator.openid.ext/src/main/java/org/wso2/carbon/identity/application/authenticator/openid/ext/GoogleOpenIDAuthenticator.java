package org.wso2.carbon.identity.application.authenticator.openid.ext;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GoogleOpenIDAuthenticator extends OpenIDAuthenticator {

    /**
     *
     */
    private static final long serialVersionUID = -5947608036809026467L;

    @Override
    public String getFriendlyName() {
        return "google";
    }

    @Override
    public String getName() {
        return "GoogleOpenIDAuthenticator";
    }

    protected String getOpenIDServerUrl() {
        return "https://www.google.com/accounts/o8/id";
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {
        super.processAuthenticationResponse(request, response, context);

        String subject = super.getSubjectFromUserIDClaimURI(context);
        if (subject != null) {
            context.getSubject().setAuthenticatedSubjectIdentifier(subject);
        }
    }

}
