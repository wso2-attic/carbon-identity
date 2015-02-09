package org.wso2.carbon.identity.application.authenticator.totp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;
import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.totp.internal.TOTPAuthenticatorServiceComponent;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TOTPAuthenticator extends AbstractApplicationAuthenticator
        implements LocalApplicationAuthenticator {

    private static Log log = LogFactory.getLog(TOTPAuthenticator.class);

    
    @Override
    public boolean canHandle(HttpServletRequest request) {

        String token = request.getParameter("token");//getSafeText
        String action = request.getParameter("sendToken");
        if (token != null || action!=null) {
            return true;
        }
        return false;
    }

    @Override
    public AuthenticatorFlowStatus process(HttpServletRequest request,
                                           HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException, LogoutFailedException {

        if (context.isLogoutRequest()) {//return incompleteStatus if action != null
            return AuthenticatorFlowStatus.SUCCESS_COMPLETED;
        }else if(request.getParameter("sendToken")!=null){
            generateTOTPToken(context);
            return AuthenticatorFlowStatus.INCOMPLETE;
        }
        else {
            return super.process(request, response, context);
        }
    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        String loginPage = ConfigurationFacade.getInstance().getAuthenticationEndpointURL();
        String retryParam = "";
        String username = getLoggedInUser(context);
        
        String queryParams = FrameworkUtils.getQueryStringWithFrameworkContextId(context.getQueryParams(),
                                                                                 context.getCallerSessionKey(),
                                                                                 context.getContextIdentifier());
        try {
            if (context.isRetrying()) {
                retryParam = "&authFailure=true&authFailureMsg=login.fail.message";
            }
            
            boolean isTOTPEnabled = TOTPAuthenticatorServiceComponent.getTotpManager().isTOTPEnabledForLocalUser(username);
            if(isTOTPEnabled) {
                response.sendRedirect(response.encodeRedirectURL(loginPage + ("?sessionDataKey=" + request.getParameter("sessionDataKey")))
                                      + "&authenticators=" + getName() + "&type=totp" + retryParam + "&username=" + username);
            }else{
                response.sendRedirect(response.encodeRedirectURL(loginPage+ ("?sessionDataKey=" + request.getParameter("sessionDataKey")))
                                      + "&authenticators=" + getName() + "&type=totp_error"+retryParam + "&username=" + username);
            }
        } catch (IOException e) {
            e.printStackTrace(); //don't
        } catch (TOTPException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void processAuthenticationResponse(HttpServletRequest request,
                                                 HttpServletResponse response, AuthenticationContext context)
            throws AuthenticationFailedException {

        String token = request.getParameter("token");
        String username = getLoggedInUser(context);

        if(token!=null){
            try {
                int tokenvalue = Integer.parseInt(token);

                if (!TOTPAuthenticatorServiceComponent.getTotpManager().isValidTokenLocalUser(tokenvalue, username)) {
                    log.info("Authentication failed .. ");
                    throw new AuthenticationFailedException();
                }
            } catch(Exception ex){
                throw new AuthenticationFailedException();
            }
        }
    }

    @Override
    protected boolean retryAuthenticationEnabled() {
        return true;
    }

    @Override
    public String getContextIdentifier(HttpServletRequest request) {
        return request.getParameter("sessionDataKey");
    }

    @Override
    public String getFriendlyName() {
        return TOTPAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
    }

    @Override
    public String getName() {
        return TOTPAuthenticatorConstants.AUTHENTICATOR_NAME;
    }

    private String getLoggedInUser(AuthenticationContext context){
        String username = "";
        for (int i = context.getSequenceConfig().getStepMap().size() - 1; i >= 0; i--) {
            if (context.getSequenceConfig().getStepMap().get(i).getAuthenticatedUser() != null &&
                context.getSequenceConfig().getStepMap().get(i).getAuthenticatedAutenticator()
                        .getApplicationAuthenticator() instanceof LocalApplicationAuthenticator) {
                username = context.getSequenceConfig().getStepMap().get(i).getAuthenticatedUser();
                if (log.isDebugEnabled()) {
                    log.debug("username :" + username);
                }
                break;
            }
        }
        return username;
    }
    
    
    private void generateTOTPToken(AuthenticationContext context){
        String username = getLoggedInUser(context);
        try {
            TOTPAuthenticatorServiceComponent.getTotpManager().generateTOTPTokenLocal(username);
            if(log.isDebugEnabled()){
                log.debug("TOTP Token is generated");
            }
        } catch (TOTPException e) {
            log.error("Error when generating the totp token");
        }
    }

}
