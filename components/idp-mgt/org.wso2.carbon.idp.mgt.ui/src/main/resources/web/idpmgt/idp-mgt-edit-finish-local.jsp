<!--
~ Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticatorConfig" %>
<%@ page import="org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider" %>
<%@ page import="org.wso2.carbon.identity.application.common.model.idp.xsd.Property" %>
<%@ page import="org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants" %>
<%@ page import="org.wso2.carbon.idp.mgt.ui.client.IdentityProviderMgtServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.text.MessageFormat" %>
<%@ page import="java.util.ResourceBundle" %>

<%
    String BUNDLE = "org.wso2.carbon.idp.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    try {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        IdentityProviderMgtServiceClient client = new IdentityProviderMgtServiceClient(cookie, backendServerURL, configContext);

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setEnable(true);
        identityProvider.setPrimary(true);
        identityProvider.setIdentityProviderName(IdentityApplicationConstants.RESIDENT_IDP_RESERVED_NAME);
        identityProvider.setHomeRealmId(CharacterEncoder.getSafeText(request.getParameter("homeRealmId")));
        FederatedAuthenticatorConfig samlFedAuthn = new FederatedAuthenticatorConfig();
        samlFedAuthn.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.NAME);
        Property[] properties = new Property[1];
        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID);
        property.setValue(CharacterEncoder.getSafeText(request.getParameter("idPEntityId")));
        properties[0] = property;
        samlFedAuthn.setProperties(properties);

        FederatedAuthenticatorConfig propertyHolderConfig = new FederatedAuthenticatorConfig();
        propertyHolderConfig.setName(IdentityApplicationConstants.Authenticator.IDPProperties.NAME);
        properties = new Property[2];
        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.IDPProperties.SESSION_IDLE_TIME_OUT);
        property.setValue(CharacterEncoder.getSafeText(request.getParameter("sessionIdleTimeout")));
        properties[0] = property;
        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.IDPProperties.REMEMBER_ME_TIME_OUT);
        property.setValue(CharacterEncoder.getSafeText(request.getParameter("rememberMeTimeout")));
        properties[1] = property;
        propertyHolderConfig.setProperties(properties);
        FederatedAuthenticatorConfig[] federatedAuthenticators = new FederatedAuthenticatorConfig[2];
        federatedAuthenticators[0] = samlFedAuthn;
        federatedAuthenticators[1] = propertyHolderConfig;
        identityProvider.setFederatedAuthenticatorConfigs(federatedAuthenticators);
        client.updateResidentIdP(identityProvider);
        String message = MessageFormat.format(resourceBundle.getString("success.updating.resident.idp"),null);
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);
    } catch (Exception e) {
        String message = MessageFormat.format(resourceBundle.getString("error.updating.resident.idp"),
                new Object[]{e.getMessage()});
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
    } finally {
        session.removeAttribute("ResidentIdentityProvider");
    }
%>
<script type="text/javascript">
    location.href = "idp-mgt-list.jsp";
</script>