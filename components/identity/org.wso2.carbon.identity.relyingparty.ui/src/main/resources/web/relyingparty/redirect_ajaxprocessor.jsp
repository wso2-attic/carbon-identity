<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@page import="org.opensaml.saml2.core.AuthnRequest" %>
<%@page import="org.opensaml.saml2.core.LogoutRequest" %>
<%@ page import="org.wso2.carbon.identity.authenticator.saml2.sso.ui.SAML2SSOAuthenticatorConstants" %>
<%@ page import="org.wso2.carbon.identity.authenticator.saml2.sso.ui.SSOSessionManager" %>
<%@ page import="org.wso2.carbon.identity.authenticator.saml2.sso.ui.builders.AuthenticationRequestBuilder" %>
<%@ page import="org.wso2.carbon.identity.authenticator.saml2.sso.ui.builders.LogoutRequestBuilder" %>
<%@ page
        import="org.wso2.carbon.identity.authenticator.saml2.sso.ui.Util" %>
<%@ page import="org.wso2.carbon.registry.core.utils.UUIDGenerator" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<html>
<head></head>
<body>
<%
    String encodedReq = null;
    String relayState = "";
    String domain = null;
        AuthenticationRequestBuilder authnReqGenerator = new AuthenticationRequestBuilder();
        String username = (String)request.getAttribute("gapp.openid.username");
        AuthnRequest authRequest = authnReqGenerator.buildAuthenticationRequest(username);
        encodedReq = Util.encode(Util.marshall(authRequest));
        relayState = UUIDGenerator.generateUUID();
        domain = (String)request.getAttribute(MultitenantConstants.TENANT_DOMAIN);

    // add the relay state to Session Manager
    SSOSessionManager.addAuthnRequest(relayState);

%>
 <p>You are now redirected to <%=Util.getIdentityProviderSSOServiceURL()%>. If the
 redirection fails, please click the post button.</p>
<form method="post" action="<%=Util.getIdentityProviderSSOServiceURL()%>">
    <p><input type="hidden" name="<%=SAML2SSOAuthenticatorConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ%>"
              value="<%= encodedReq %>"/>
        <input type="hidden" name="RelayState" value="<%= relayState %>"/>
        <input type="hidden" name="<%= MultitenantConstants.TENANT_DOMAIN %>" value="<%= domain %>"/>
        <input type="hidden" name="<%= MultitenantConstants.SSO_AUTH_SESSION_ID %>" value="<%= session.getId() %>"/>
        <input type="hidden" name="authMode" value="openIDBasedAuthn"/>
        <button type="submit">POST</button>
    </p>
</form>

<script type="text/javascript">
    document.forms[0].submit();
</script>

</body>
</html>