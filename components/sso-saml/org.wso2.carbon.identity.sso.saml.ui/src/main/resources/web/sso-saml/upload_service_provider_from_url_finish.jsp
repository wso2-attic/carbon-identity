<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIConstants" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.client.SAMLSSOConfigServiceClient" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIUtil" %>

<jsp:useBean id="samlSsoServuceProviderConfigBean"
             type="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
             class="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
             scope="session"/>
<jsp:setProperty name="samlSsoServuceProviderConfigBean" property="*"/>

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>


<%
	String backendServerURL;
    ConfigurationContext configContext;
    String cookie;
    String user = null;
    SAMLSSOConfigServiceClient client;
    session.setAttribute(SAMLSSOUIConstants.CONFIG_CLIENT, null);
    String spName = (String) session.getAttribute("application-sp-name");
    session.removeAttribute("application-sp-name");
    String attributeConsumingServiceIndex;

    backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    configContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String BUNDLE = "org.wso2.carbon.identity.sso.saml.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    SAMLSSOServiceProviderDTO serviceProviderDTO = null;
    try {
        client = new SAMLSSOConfigServiceClient(cookie, backendServerURL, configContext);

        String url = request.getParameter("metadataFromUrl");
        serviceProviderDTO = client.uploadServiceProviderFromUrl(url);

        if(serviceProviderDTO.getAttributeConsumingServiceIndex() != null){
            attributeConsumingServiceIndex = serviceProviderDTO.getAttributeConsumingServiceIndex();
        }else{
            attributeConsumingServiceIndex = "";
        }


        samlSsoServuceProviderConfigBean.clearBean();

        String message = resourceBundle.getString("sp.added.successfully");

		CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);

%>
<script>
<%
boolean applicationComponentFound = CarbonUIUtil.isContextRegistered(config, "/application/");

if (applicationComponentFound) { %>
    location.href = '../application/configure-service-provider.jsp?action=update&display=samlIssuer&spName=<%=spName%>&samlIssuer=<%=serviceProviderDTO.getIssuer()%>&attrConServIndex=<%=attributeConsumingServiceIndex%>';
<%} else { %>
    location.href = 'manage_service_providers.jsp?region=region1&item=manage_saml_sso';
<% } %>
</script>
<%

} catch (Exception e) {
    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
</script>
<%
        return;
    }
%>