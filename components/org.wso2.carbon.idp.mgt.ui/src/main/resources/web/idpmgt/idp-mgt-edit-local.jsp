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

<%@page import="org.wso2.carbon.identity.application.common.model.idp.xsd.ProvisioningConnectorConfig"%>
<%@ page import="org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider" %>
<%@ page import="org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticatorConfig" %>
<%@ page import="org.wso2.carbon.identity.application.common.model.idp.xsd.Property" %>
<%@ page import="org.wso2.carbon.idp.mgt.ui.util.IdPManagementUIUtil" %>
<%@ page import="org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"%>

<carbon:breadcrumb label="identity.providers" resourceBundle="org.wso2.carbon.idp.mgt.ui.i18n.Resources"
                   topPage="true" request="<%=request%>" />
<jsp:include page="../dialog/display_messages.jsp"/>

<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    IdentityProvider residentIdentityProvider =
            (IdentityProvider)session.getAttribute("ResidentIdentityProvider");
    String homeRealmId = residentIdentityProvider.getHomeRealmId();
    String openidUrl = null;
    String idPEntityId = null;
    String samlSSOUrl = null;
    String samlSLOUrl = null;
    String authzUrl = null;
    String tokenUrl = null;
    String userInfoUrl = null;
    String passiveSTSUrl = null;
    FederatedAuthenticatorConfig[] federatedAuthenticators = residentIdentityProvider.getFederatedAuthenticatorConfigs();
    for(FederatedAuthenticatorConfig federatedAuthenticator : federatedAuthenticators){
        Property[] properties = federatedAuthenticator.getProperties();
        if(IdentityApplicationConstants.Authenticator.OpenID.NAME.equals(federatedAuthenticator.getName())){
            openidUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.OpenID.OPEN_ID_URL).getValue();
        } else if(IdentityApplicationConstants.Authenticator.SAML2SSO.NAME.equals(federatedAuthenticator.getName())){
            idPEntityId = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.SAML2SSO.IDP_ENTITY_ID).getValue();
            samlSSOUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.SAML2SSO.SSO_URL).getValue();
            samlSLOUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.SAML2SSO.LOGOUT_REQ_URL).getValue();
        } else if(IdentityApplicationConstants.Authenticator.OIDC.NAME.equals(federatedAuthenticator.getName())){
            authzUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL).getValue();
            tokenUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL).getValue();
            userInfoUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.OIDC.USER_INFO_URL).getValue();
        } else if(IdentityApplicationConstants.Authenticator.PassiveSTS.NAME.equals(federatedAuthenticator.getName())){
            passiveSTSUrl = IdPManagementUIUtil.getProperty(properties,
                    IdentityApplicationConstants.Authenticator.PassiveSTS.PASSIVE_STS_URL).getValue();
        }
    }
    String scimUserEp = null;
    String scimGroupEp = null;
    ProvisioningConnectorConfig[] provisioningConnectors = residentIdentityProvider.getProvisioningConnectorConfigs();
    for(ProvisioningConnectorConfig provisioningConnector : provisioningConnectors){
        if(provisioningConnector.getName().equals("scim")){
            Property[] provisioningProperties = provisioningConnector.getProvisioningProperties();
            if(provisioningProperties == null){
                provisioningProperties = new Property[0];
            }
            for(Property property : provisioningProperties){
                if(property.getName().equals("scimUserEndpoint")){
                    scimUserEp = property.getValue();
                } else if(property.getName().equals("scimGroupEndpoint")){
                    scimGroupEp = property.getValue();
                }
            }
        }
    }
    session.setAttribute("returnToPath", "../idpmgt/idp-mgt-edit-local.jsp");
    session.setAttribute("cancelLink", "../idpmgt/idp-mgt-edit-local.jsp");
    session.setAttribute("backLink", "../idpmgt/idp-mgt-edit-local.jsp");
%>
<script>


jQuery(document).ready(function(){

    jQuery('h2.trigger').click(function(){
        if (jQuery(this).next().is(":visible")) {
            this.className = "active trigger";
        } else {
            this.className = "trigger";
        }
        jQuery(this).next().slideToggle("fast");
        return false; //Prevent the browser jump to the link anchor
    })
    
})

    initSections("");
    function idpMgtUpdate(){
        if(doValidation()){
            jQuery('#idp-mgt-edit-local-form').submit();
        }
    }
    function idpMgtCancel(){
        location.href = "idp-mgt-list.jsp"
    }
    function doValidation() {
        var reason = "";
        reason = validateEmpty("homeRealmId");
        if (reason != "") {
            CARBON.showWarningDialog("Resident Home Realm ID cannot be empty");
            return false;
        }
        reason = validateEmpty("idPEntityId");
        if (reason != "") {
            CARBON.showWarningDialog("Resident IdP Entity ID cannot be empty");
            return false;
        }
        return true;
    }
</script>

<fmt:bundle basename="org.wso2.carbon.idp.mgt.ui.i18n.Resources">
    <div id="middle">
        <h2>
            <fmt:message key='resident.idp'/>
        </h2>
        <div id="workArea">
            <form id="idp-mgt-edit-local-form" name="idp-mgt-edit-local-form" method="post" action="idp-mgt-edit-finish-local.jsp">
                <div class="sectionSeperator "><fmt:message key='resident.realm.config'/></div>
                <div class="sectionSub">
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='home.realm.id'/>:</td>
                            <td>
                                <input id="homeRealmId" name="homeRealmId" type="text" value="<%=homeRealmId%>" autofocus/>
                                <div class="sectionHelp">
                                    <fmt:message key='home.realm.id.resident.help'/>
                                </div>
                            </td>
                        </tr>
                    </table>
                    
                    <h2 id="authenticationconfighead"  class="sectionSeperator trigger active" >
                		<a href="#">Inbound Authentication Configuration</a>
            		</h2>
            		<div class="toggle_container sectionSub" style="margin-bottom:10px;display:none" id="authenticationconfig">
                    
                    <h2 id="openidconfighead"  class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#"><fmt:message key='openid.config'/></a>
            		</h2>
            		<div class="toggle_container sectionSub" style="margin-bottom:10px;display:none" id="openidconfig">
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='openid.url'/>:</td>
                            <td><%=openidUrl%></td>
                        </tr>
                    </table>
                    </div>
                    
                    <h2 id="saml2confighead"  class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#"><fmt:message key='saml2.web.sso.config'/></a>
            		</h2>
            		<div class="toggle_container sectionSub" style="margin-bottom:10px;display:none" id="saml2config">
                    
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='idp.entity.id'/>:</td>
                            <td>
                                <input id="idPEntityId" name="idPEntityId" type="text" value="<%=idPEntityId%>"/>
                                <div class="sectionHelp">
                                    <fmt:message key='idp.entity.id.help'/>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='sso.url'/>:</td>
                            <td><%=samlSSOUrl%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='logout.url'/>:</td>
                            <td><%=samlSLOUrl%></td>
                        </tr>
                    </table>
                    </div>
                    
                    <h2 id="oidcconfighead"  class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#"><fmt:message key='oidc.config'/></a>
            		</h2>
            		<div class="toggle_container sectionSub" style="margin-bottom:10px;display:none" id="oidcconfig">
                    
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='authz.endpoint'/>:</td>
                            <td><%=authzUrl%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='token.endpoint'/>:</td>
                            <td><%=tokenUrl%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='user.endpoint'/>:</td>
                            <td><%=userInfoUrl%></td>
                        </tr>
                    </table>
                    </div>
                    
                     <h2 id="passivestsconfighead"  class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#"><fmt:message key='passive.sts.local.config'/></a>
            		</h2>
            		<div class="toggle_container sectionSub" style="margin-bottom:10px;display:none" id="passivestsconfig">
                    
                    <table class="carbonFormTable">
                        <tr>
                            <td>
                                <div style="height:30px;">
                                    <a href="javascript:document.location.href='../securityconfig/index.jsp?serviceName=wso2carbon-sts'"
                                       class="icon-link"
                                       style="background-image:url(images/configure.gif);margin-left: 0"><fmt:message key='apply.security.policy'/></a>
                                </div>
                            </td>
                            
                            </tr>
                            <tr>
                            <td>
                                <div style="height:30px;">
                                    <a href="javascript:document.location.href='<%=passiveSTSUrl+"?wsdl"%>'"
                                       class="icon-link"
                                       style="background-image:url(images/sts.gif);margin-left: 0"><%=passiveSTSUrl%>
                                    </a>
                                </div>
                            </td>
       
                        </tr>
              
                    </table>
                    </div>
                </div>
                
                    <h2 id="inboundprovisioningconfighead"  class="sectionSeperator trigger active">
                		<a href="#">Inbound Provisioning Configuration</a>
            		</h2>
            		<div class="toggle_container sectionSub" style="margin-bottom:10px;display:none" id="inboundprovisioningconfig"> 
            		  <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='scim.user.endpoint'/>:</td>
                            <td><%=scimUserEp%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='scim.group.endpoint'/>:</td>
                            <td><%=scimGroupEp%></td>
                        </tr>
                    </table>
            		
            		</div>               
                </div>
                <div class="buttonRow">
                    <input type="button" value="<fmt:message key='update'/>" onclick="idpMgtUpdate();"/>
                    <input type="button" value="<fmt:message key='cancel'/>" onclick="idpMgtCancel();"/>
                </div>
            </form>
        </div>
    </div>
</fmt:bundle>