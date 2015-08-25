<!--
~ Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIConstants" %>
<%@page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIUtil" %>
<%@page import="org.wso2.carbon.identity.sso.saml.ui.client.SAMLSSOConfigServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>

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
    boolean status = false;
    String attributeConsumingServiceIndex;

    backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    configContext = (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String BUNDLE = "org.wso2.carbon.identity.sso.saml.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
    SAMLSSOServiceProviderDTO serviceProviderDTO = null;
    try {
        client = new SAMLSSOConfigServiceClient(cookie, backendServerURL, configContext);

        serviceProviderDTO = new SAMLSSOServiceProviderDTO();
        boolean isEditingSP = false; 
        if ("editServiceProvider".equals(SAMLSSOUIUtil.getSafeInput(request, "SPAction"))) {
            isEditingSP = true;
            serviceProviderDTO.setIssuer(SAMLSSOUIUtil.getSafeInput(request, "hiddenIssuer"));
        } else {
            serviceProviderDTO.setIssuer(SAMLSSOUIUtil.getSafeInput(request, "issuer"));
        }

        serviceProviderDTO.setAssertionConsumerUrls(SAMLSSOUIUtil.getSafeInput(request, "assertionConsumerURLs")
                                                            .split(","));
        serviceProviderDTO.setDefaultAssertionConsumerUrl(SAMLSSOUIUtil.getSafeInput(request,
                                                                                     "defaultAssertionConsumerURL"));

        if ("true".equals(request.getParameter("enableSingleLogout"))) {
            serviceProviderDTO.setDoSingleLogout(true);
            if (StringUtils.isNotBlank(request.getParameter("sloResponseURL"))) {
                serviceProviderDTO.setSloResponseURL(request.getParameter("sloResponseURL"));
            }
            if (StringUtils.isNotBlank(request.getParameter("sloRequestURL"))) {
                serviceProviderDTO.setSloRequestURL(request.getParameter("sloRequestURL"));
            }
        }

        if ("true".equals(request.getParameter("enableResponseSignature"))) {
            serviceProviderDTO.setDoSignResponse(true);
        }

        if ("true".equals(request.getParameter("enableAssertionSignature"))) {
            serviceProviderDTO.setDoSignAssertions(true);
        }
        
        serviceProviderDTO.setNameIDFormat(request.getParameter("nameIdFormat"));
        
        if (serviceProviderDTO.getNameIDFormat()!=null) {
        	serviceProviderDTO.setNameIDFormat(serviceProviderDTO.getNameIDFormat().replace(":", "/"));
        }

        if (request.getParameter("enableAttributeProfile") != null) {
            serviceProviderDTO.setRequestedClaims(samlSsoServuceProviderConfigBean.getSelectedClaimsAttay());
            serviceProviderDTO.setEnableAttributeProfile(true);

            if ("true".equals(request.getParameter("enableDefaultAttributeProfileHidden"))) {
                serviceProviderDTO.setEnableAttributesByDefault(true);
            }
        }
        
        if ("true".equals(request.getParameter("enableNameIdClaimUriHidden"))) {            
                serviceProviderDTO.setNameIdClaimUri(request.getParameter("nameIdClaim"));
        }
               
               
        if ("true".equals(request.getParameter("enableAudienceRestriction"))) {
            serviceProviderDTO.setRequestedAudiences(samlSsoServuceProviderConfigBean.getSelectedAudiencesArray());
        }

        if ("true".equals(request.getParameter("enableRecipients"))) {
            serviceProviderDTO.setRequestedRecipients(samlSsoServuceProviderConfigBean.getSelectedRecipientsArray());
        }

        if (request.getParameter("loginPageURL")!=null && !"null".equals(request.getParameter("loginPageURL"))) {
            serviceProviderDTO.setLoginPageURL(request.getParameter("loginPageURL"));
        }

        if ("true".equals(request.getParameter("enableAttributeProfile"))) {

        String claimsCountParameter = SAMLSSOUIUtil.getSafeInput(request, "claimPropertyCounter");
        if (claimsCountParameter != null && !"".equals(claimsCountParameter)) {
            try {
                int claimsCount = Integer.parseInt(claimsCountParameter);
                for (int i = 0; i < claimsCount; i++) {
                    String claim = SAMLSSOUIUtil.getSafeInput(request, "claimPropertyName" + i);
                    if (claim != null && !"".equals(claim) && !"null".equals(claim)) {
                        String[] currentClaims = serviceProviderDTO.getRequestedClaims();
                        boolean isClaimAlreadyAdded = false;
                        for (String currentClaim : currentClaims) {
                            if (claim.equals(currentClaim)) {
                                isClaimAlreadyAdded = true;
                                break;
                            }
                        }

                        if (!isClaimAlreadyAdded) {
                            serviceProviderDTO.addRequestedClaims(claim);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid number", e);
            }
        }
        }

        if ("true".equals(request.getParameter("enableAudienceRestriction"))) {

        	String audiencesCountParameter = SAMLSSOUIUtil.getSafeInput(request, "audiencePropertyCounter");
        	if (audiencesCountParameter != null && !"".equals(audiencesCountParameter)) {
            	try {
                	int audiencesCount = Integer.parseInt(audiencesCountParameter);
                	for (int i = 0; i < audiencesCount; i++) {
                    	String audience = SAMLSSOUIUtil.getSafeInput(request, "audiencePropertyName" + i);
                    	if (audience != null && !"".equals(audience) && !"null".equals(audience)) {
                        	String[] currentAudiences = serviceProviderDTO.getRequestedAudiences();
                        	boolean isAudienceAlreadyAdded = false;
                        	for (String currentAudience : currentAudiences) {
                            	if (audience.equals(currentAudience)) {
                               		isAudienceAlreadyAdded = true;
                                	break;
                            	}
                        	}

                        	if (!isAudienceAlreadyAdded) {
                            	serviceProviderDTO.addRequestedAudiences(audience);
                        	}
                    	}
                	}
            	} catch (NumberFormatException e) {
                	throw new RuntimeException("Invalid number", e);
            	}
        	}
        }

        if ("true".equals(request.getParameter("enableRecipients"))) {

            String recipientCountParameter = SAMLSSOUIUtil.getSafeInput(request, "recipientPropertyCounter");
            if (recipientCountParameter != null && !"".equals(recipientCountParameter)) {
                try {
                    int recipientCount = Integer.parseInt(recipientCountParameter);
                    for (int i = 0; i < recipientCount; i++) {
                        String recipient = SAMLSSOUIUtil.getSafeInput(request, "recipientPropertyName" + i);
                        if (recipient != null && !"".equals(recipient) && !"null".equals(recipient)) {
                            String[] currentRecipients = serviceProviderDTO.getRequestedRecipients();
                            boolean isRecipientAlreadyAdded = false;
                            for (String currentRecipient : currentRecipients) {
                                if (recipient.equals(currentRecipient)) {
                                    isRecipientAlreadyAdded = true;
                                    break;
                                }
                            }

                            if (!isRecipientAlreadyAdded) {
                                serviceProviderDTO.addRequestedRecipients(recipient);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid number", e);
                }
            }
        }

        serviceProviderDTO.setAttributeConsumingServiceIndex(SAMLSSOUIUtil.getSafeInput(request, "attributeConsumingServiceIndex"));

        if ("true".equals(request.getParameter("enableIdPInitSSO"))) {
            serviceProviderDTO.setIdPInitSSOEnabled(true);
        }

        if ("true".equals(request.getParameter("enableIdPInitSLO"))) {
            serviceProviderDTO.setIdPInitSLOEnabled(true);
            String returnToUrls = SAMLSSOUIUtil.getSafeInput(request, "idpInitSLOReturnToURLs");
            if(StringUtils.isNotBlank(returnToUrls)) {
                serviceProviderDTO.setIdpInitSLOReturnToURLs(returnToUrls.split(","));
            }
        }

        if ("true".equals(request.getParameter("enableEncAssertion"))) {
            serviceProviderDTO.setDoEnableEncryptedAssertion(true);
            serviceProviderDTO.setCertAlias(SAMLSSOUIUtil.getSafeInput(request, "alias"));
        }

        if("true".equals(request.getParameter("enableSigValidation"))){
            serviceProviderDTO.setDoValidateSignatureInRequests(true);
            serviceProviderDTO.setCertAlias(SAMLSSOUIUtil.getSafeInput(request, "alias"));
        }

        if (isEditingSP) {
            client.removeServiceProvier(serviceProviderDTO.getIssuer());
        }
        status = client.addServiceProvider(serviceProviderDTO);
        attributeConsumingServiceIndex = client.getServiceProvider(serviceProviderDTO.getIssuer()).getAttributeConsumingServiceIndex();

        samlSsoServuceProviderConfigBean.clearBean();

        String message;
        if (status) {     	
			if (isEditingSP) {
				message = resourceBundle.getString("sp.updated.successfully");
			} else {
				message = resourceBundle.getString("sp.added.successfully");
			}
		} else {
			message = resourceBundle.getString("error.adding.sp");
		}

		if (status) {
			CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.INFO, request);
		} else {
			CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
		}
%>
<script>
<%
boolean applicationComponentFound = CarbonUIUtil.isContextRegistered(config, "/application/");

if (applicationComponentFound) {
	if (status) {
%>
    location.href = '../application/configure-service-provider.jsp?action=update&display=samlIssuer&spName=<%=spName%>&samlIssuer=<%=serviceProviderDTO.getIssuer()%>&attrConServIndex=<%=attributeConsumingServiceIndex%>';
<% } else { %>
	location.href = '../application/configure-service-provider.jsp?action=delete&display=samlIssuer&spName=<%=spName%>&samlIssuer=<%=serviceProviderDTO.getIssuer()%>&attrConServIndex=<%=attributeConsumingServiceIndex%>';

<% } } else { %>
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