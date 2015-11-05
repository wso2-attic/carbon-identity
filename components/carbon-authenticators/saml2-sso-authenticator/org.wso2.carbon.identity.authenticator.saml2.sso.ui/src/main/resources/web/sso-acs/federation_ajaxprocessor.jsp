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
<%@page import="org.wso2.carbon.identity.authenticator.saml2.sso.common.SAMLConstants"%>
<%@page import="org.wso2.carbon.utils.multitenancy.MultitenantUtils"%>
<%@page import="java.net.URLDecoder" %>
<%@ page import="org.owasp.encoder.Encode" %>
<html>
<head></head>
<body>
<%

    String assertionConsumerURL = (String) request.getAttribute(SAMLConstants.ASSRTN_CONSUMER_URL);
    String assertion = (String) request.getAttribute(SAMLConstants.ASSERTION_STR);
    String relayState = (String) request.getAttribute(SAMLConstants.RELAY_STATE);
    String subject = (String) request.getAttribute(SAMLConstants.SUBJECT);
    String domain = null;
        
	if(subject != null && MultitenantUtils.getTenantDomain(subject) != null){
    	   domain = MultitenantUtils.getTenantDomain(subject);
	}
    
%>
<p>You are now redirected back to <%=assertionConsumerURL%>. If the
 redirection fails, please click the post button.</p>
<form method="post" action="<%=assertionConsumerURL%>">
    <p><input type="hidden" name="SAMLResponse" value="<%=Encode.forHtmlAttribute(assertion)%>"/>
        <input type="hidden" name="RelayState" value="<%=Encode.forHtmlAttribute(relayState)%>"/>
        <button type="submit">POST</button>
    </p>
</form>

<script type="text/javascript">
    document.forms[0].submit();
</script>

</body>
</html>