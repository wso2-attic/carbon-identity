/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth2.token.handlers.grant.iwa.ntlm;

import org.apache.catalina.Realm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.iwa.ntlm.util.SimpleContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.iwa.ntlm.util.SimpleHttpRequest;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.iwa.ntlm.util.SimpleHttpResponse;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.iwa.ntlm.util.SimpleRealm;
import waffle.apache.NegotiateAuthenticator;
import waffle.util.Base64;
import waffle.windows.auth.IWindowsCredentialsHandle;
import waffle.windows.auth.impl.WindowsAccountImpl;
import com.sun.jna.platform.win32.Sspi;
import com.sun.jna.platform.win32.Sspi.SecBufferDesc;
import waffle.windows.auth.impl.WindowsCredentialsHandleImpl;
import waffle.windows.auth.impl.WindowsSecurityContextImpl;


public class NTLMAuthenticationGrantHandler extends AbstractAuthorizationGrantHandler {
    private static Log log = LogFactory.getLog(NTLMAuthenticationGrantHandler.class);
    NegotiateAuthenticator _authenticator = null;
    String securityPackage = "Negotiate";

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        super.validateGrant(tokReqMsgCtx);

        String token=tokReqMsgCtx.getOauth2AccessTokenReqDTO().getWindowsToken();
        boolean authenticated;
        IWindowsCredentialsHandle clientCredentials = null;
        WindowsSecurityContextImpl clientContext = null;
        if(token!=null){


            try {
                initializeNegotiateAuthenticator();
            } catch (Exception e) {
                throw new IdentityOAuth2Exception("Error while validating the NTLM authentication grant",e);
            }

            // Logging the windows authentication object
        if (log.isDebugEnabled()) {
            log.debug("Received NTLM Token : " +
                    tokReqMsgCtx.getOauth2AccessTokenReqDTO().getWindowsToken()
            );
        }
        // client credentials handle
        clientCredentials = WindowsCredentialsHandleImpl
                            .getCurrent(securityPackage);
        clientCredentials.initialize();
        // initial client security context
        clientContext = new WindowsSecurityContextImpl();
        clientContext.setToken(token.getBytes());
        clientContext.setPrincipalName(WindowsAccountImpl.getCurrentUsername());
        clientContext.setCredentialsHandle(clientCredentials.getHandle());
        clientContext.setSecurityPackage(securityPackage);
        clientContext.initialize(null, null,WindowsAccountImpl.getCurrentUsername());
        while(true){
        SimpleHttpRequest request = new SimpleHttpRequest();

        try{
        request.addHeader("Authorization", securityPackage + " "
                           + token);
        SimpleHttpResponse response = new SimpleHttpResponse();
        authenticated = _authenticator.authenticate(request, response,null);

        if (log.isDebugEnabled()) {
        if(authenticated){
        log.debug("The input NTLM token is authenticated against the windows server.");
        }else{
        log.debug("The input NTLM token is not a valid token.It cannot be authenticate against windows server.");
        }
        }

        if(authenticated){
        String resourceOwnerUserNameWithDomain=  WindowsAccountImpl.getCurrentUsername();
        String resourceOwnerUserName=  resourceOwnerUserNameWithDomain.split("\\\\")[1];
        tokReqMsgCtx.setAuthorizedUser(resourceOwnerUserName);
        break;
        }

        if(response.getHeader("WWW-Authenticate").startsWith(securityPackage + " ") && response.getStatus()==401  ){
            String continueToken = response.getHeader("WWW-Authenticate").substring(securityPackage.length() + 1);
            byte[] continueTokenBytes = Base64.decode(continueToken);
            if(continueTokenBytes.length > 0){
            SecBufferDesc continueTokenBuffer = new SecBufferDesc(Sspi.SECBUFFER_TOKEN, continueTokenBytes);
            clientContext.initialize(clientContext.getHandle(), continueTokenBuffer,WindowsAccountImpl.getCurrentUsername());
            token= Base64.encode(clientContext.getToken());
            }

        }else{
        break;
        }

        } catch (Exception e) {
          throw new IdentityOAuth2Exception("Error while validating the NTLM authentication grant",e);
        }
        }
        }else{
        if (log.isDebugEnabled()) {
        log.debug("NTLM token is null");
        }
        throw new IdentityOAuth2Exception("NTLM token is null");
        }
        return authenticated;

    }

    private void initializeNegotiateAuthenticator() throws Exception {
        _authenticator = new NegotiateAuthenticator();
        SimpleContext ctx = new SimpleContext();
        Realm realm = new SimpleRealm();
        ctx.setRealm(realm);
        _authenticator.setContainer(ctx);


    }




}
