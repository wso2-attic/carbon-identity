<!--
 ~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page import="org.owasp.encoder.Encode" %>

<jsp:useBean id="samlSsoServuceProviderConfigBean"
	type="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
	class="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
	scope="session" />
<jsp:setProperty name="samlSsoServuceProviderConfigBean" property="*" />

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<%
	String BUNDLE = "org.wso2.carbon.identity.sso.saml.ui.i18n.Resources";
	ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
		 
	String issuer = request.getParameter("issuer");
	String assertionConsumerUrl = request.getParameter("assrtConsumerURL");
	String enableSignatureTrigger = request.getParameter("enableSignatureTrigger");
	String enableSignatureValidation = request.getParameter("enableSigValidation");
	String certificateAlias = request.getParameter("alias");
	String enableSingleLogout = request.getParameter("enableSingleLogout");
	String singleLogoutUrl = request.getParameter("logoutURL");
	if(singleLogoutUrl == null){
		singleLogoutUrl = "";
	}
	String enableClaims = request.getParameter("enableAttributeProfile");
	String claim = request.getParameter("claim");
	String action = request.getParameter("action");
    String enableIdPInitSSO = request.getParameter("enableIdPInitSSO");
	
	samlSsoServuceProviderConfigBean.setIssuer(issuer);
	samlSsoServuceProviderConfigBean.setAssertionConsumerUrl(assertionConsumerUrl);
	samlSsoServuceProviderConfigBean.setEnableAssertionSigning(enableSignatureTrigger);
	samlSsoServuceProviderConfigBean.setEnableSignatureValidation(enableSignatureValidation);
	samlSsoServuceProviderConfigBean.setCertificateAlias(certificateAlias);
	samlSsoServuceProviderConfigBean.setEnableSingleLogout(enableSingleLogout);
	samlSsoServuceProviderConfigBean.setSingleLogoutUrl(singleLogoutUrl);
	samlSsoServuceProviderConfigBean.setEnableClaims(enableClaims);
    samlSsoServuceProviderConfigBean.setIdPInitSSOEnabled(enableIdPInitSSO);
	
	if(action.equals("add")){	
		if(!samlSsoServuceProviderConfigBean.addClaimToList(claim)){
			String message = resourceBundle.getString("error.adding.claim");
	        CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.ERROR, request);
		}
	} else if (action.equals("delete")){
		samlSsoServuceProviderConfigBean.removeClaimFromList(claim);
	} else if (action.equals("clear")){
		samlSsoServuceProviderConfigBean.clearBean();
	}
%>

<script type="text/javascript">
        location.href = "../application/configure-service-provider.jsp?spName=<%=Encode.forUriComponent(request.getParameter("spName"))%>";
</script>