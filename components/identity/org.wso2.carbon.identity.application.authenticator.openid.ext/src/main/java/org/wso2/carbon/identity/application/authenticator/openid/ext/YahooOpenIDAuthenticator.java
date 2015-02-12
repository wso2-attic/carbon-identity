package org.wso2.carbon.identity.application.authenticator.openid.ext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authenticator.openid.OpenIDAuthenticator;

public class YahooOpenIDAuthenticator extends OpenIDAuthenticator {

    /**
     * 
     */
    private static final long serialVersionUID = -782801773114711699L;

    @Override
    public String getFriendlyName() {
        return "yahoo";
    }

    @Override
    public String getName() {
        return "YahooOpenIDAuthenticator";
    }

    protected String getOpenIDServerUrl() {
        return "https://me.yahoo.com/";
    }
    
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
            HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {
        super.processAuthenticationResponse(request, response, context);

        String subject = super.getSubjectFromUserIDClaimURI(context);
        if (subject != null) {
            context.setSubject(subject);
        }
    }
}
