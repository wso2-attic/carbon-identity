/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.token.handlers.grant.iwa.ntlm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.ResponseHeader;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;
import waffle.util.Base64;
import waffle.windows.auth.IWindowsSecurityContext;
import waffle.windows.auth.impl.WindowsAuthProviderImpl;

public class NTLMAuthenticationGrantHandlerWithHandshake extends AbstractAuthorizationGrantHandler  {
    private static Log log = LogFactory.getLog(NTLMAuthenticationGrantHandlerWithHandshake.class);
    private static WindowsAuthProviderImpl provider = new WindowsAuthProviderImpl();
    private static final String securityPackage = "Negotiate";

    public int getNLTMMessageType(byte[] decodedNLTMMessage) throws IdentityOAuth2Exception {
        int messageType;
        if (decodedNLTMMessage.length > 8) {
            messageType = decodedNLTMMessage[8];
        } else {
            throw new IdentityOAuth2Exception(
                    "Cannot extract message type from NLTM Token. Decoded token length is less than 8.");
        }

        //NLTM token type must be one of 1,2 or 3
        if (messageType < 1 || messageType > 3) {
            throw new IdentityOAuth2Exception(
                    "Invalid NLTM message type:" + messageType + ". Should be one of 1,2 or 3.");
        }

        return messageType;
    }

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        boolean validGrant = super.validateGrant(tokReqMsgCtx);

        if (!validGrant) {
            return false;
        }

        String token = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getWindowsToken();

        IWindowsSecurityContext serverContext = null;
        if (token != null) {

            byte[] bytesToken = Base64.decode(token);
            int tokenType = getNLTMMessageType(bytesToken);

            if (log.isDebugEnabled()) {
                log.debug("Received NTLM token Type " + tokenType + ":" + token);
            }

            if (tokenType == 1) {
                serverContext = provider.acceptSecurityToken("server-connection", bytesToken, securityPackage);
                String type2Token = Base64.encode(serverContext.getToken());
                if (log.isDebugEnabled()) {
                    log.debug("Sent NTLM token Type 2:" + type2Token);
                }
                ResponseHeader[] responseHeaders = new ResponseHeader[1];
                responseHeaders[0] = new ResponseHeader();
                responseHeaders[0].setKey("WWW-Authenticate");
                responseHeaders[0].setValue("NTLM " + type2Token);
                tokReqMsgCtx.addProperty("RESPONSE_HEADERS_PROPERTY", responseHeaders);
                return false;
            } else if (tokenType == 3) {
                serverContext = provider.acceptSecurityToken("server-connection", bytesToken, securityPackage);
                String resourceOwnerUserNameWithDomain = serverContext.getIdentity().getFqn();
                String resourceOwnerUserName = resourceOwnerUserNameWithDomain.split("\\\\")[1];
                tokReqMsgCtx.setAuthorizedUser(OAuth2Util.getUserFromUserName(resourceOwnerUserName));
                return true;
            } else {
                log.error("Unknown NTLM token, Type " + tokenType + ":" + token);
                return false;
            }

        } else {
            log.error("Received NTLM token is null");
            throw new IdentityOAuth2Exception("Received NTLM token is null");
        }
    }
}