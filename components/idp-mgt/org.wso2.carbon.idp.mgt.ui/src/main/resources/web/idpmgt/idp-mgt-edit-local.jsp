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

<%@page import="org.wso2.carbon.identity.application.common.model.idp.xsd.*"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"%>

<carbon:breadcrumb label="identity.providers" resourceBundle="org.wso2.carbon.idp.mgt.ui.i18n.Resources"
                   topPage="true" request="<%=request%>" />
<jsp:include page="../dialog/display_messages.jsp"/>

<script type="text/javascript" src="../admin/js/main.js"></script>

<%
    ResidentIdentityProvider residentIdentityProvider =
            (ResidentIdentityProvider)session.getAttribute("ResidentIdentityProvider");
    String homeRealmId = residentIdentityProvider.getHomeRealmId();
    String openIdRealm = residentIdentityProvider.getOpenIdRealm();
    String openidUrl = residentIdentityProvider.getOpenIDUrl();
    String idPEntityId = residentIdentityProvider.getIdpEntityId();
    String samlSSOUrl = residentIdentityProvider.getSaml2SSOUrl();
    String samlSLOUrl = residentIdentityProvider.getLogoutRequestUrl();
    String authzUrl = residentIdentityProvider.getAuthzEndpointUrl();
    String tokenUrl = residentIdentityProvider.getTokenEndpointUrl();
    String userUrl = residentIdentityProvider.getUserInfoEndpointUrl();
    String passiveSTSRealm = residentIdentityProvider.getPassiveSTSRealm();
    String passiveSTSUrl = residentIdentityProvider.getPassiveSTSUrl();
    session.setAttribute("returnToPath", "../idpmgt/idp-mgt-edit-local.jsp");
    session.setAttribute("cancelLink", "../idpmgt/idp-mgt-edit-local.jsp");
    session.setAttribute("backLink", "../idpmgt/idp-mgt-edit-local.jsp");
%>
<script>
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
        reason = validateEmpty("openIdRealm");
        if (reason != "") {
            CARBON.showWarningDialog("Resident OpenID Realm cannot be empty");
            return false;
        }
        reason = validateEmpty("idPEntityId");
        if (reason != "") {
            CARBON.showWarningDialog("Resident IdP Entity ID cannot be empty");
            return false;
        }
        reason = validateEmpty("passiveSTSRealm");
        if (reason != "") {
            CARBON.showWarningDialog("Resident Passive STS Realm cannot be empty");
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
                <div class="sectionSeperator togglebleTitle"><fmt:message key='resident.realm.config'/></div>
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
                    <div class="sectionSeperator togglebleTitle"><fmt:message key='openid.config'/></div>
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='openid.realm'/></td>
                            <td>
                                <input id="openIdRealm" name="openIdRealm" type="text" value="<%=openIdRealm%>"/>
                                <div class="sectionHelp">
                                    <fmt:message key='openid.realm.help'/>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='openid.url'/></td>
                            <td><%=openidUrl%></td>
                        </tr>
                    </table>
                    <div class="sectionSeperator togglebleTitle"><fmt:message key='saml2.web.sso.config'/></div>
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='idp.entity.id'/></td>
                            <td>
                                <input id="idPEntityId" name="idPEntityId" type="text" value="<%=idPEntityId%>"/>
                                <div class="sectionHelp">
                                    <fmt:message key='idp.entity.id.help'/>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='sso.url'/></td>
                            <td><%=samlSSOUrl%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='logout.url'/></td>
                            <td><%=samlSLOUrl%></td>
                        </tr>
                    </table>
                    <div class="sectionSeperator togglebleTitle"><fmt:message key='oidc.config'/></div>
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='authz.endpoint'/></td>
                            <td><%=authzUrl%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='token.endpoint'/></td>
                            <td><%=tokenUrl%></td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='user.endpoint'/></td>
                            <td><%=userUrl%></td>
                        </tr>
                    </table>
                    <div class="sectionSeperator togglebleTitle"><fmt:message key='passive.sts.config'/></div>
                    <table class="carbonFormTable">
                        <tr>
                            <td>
                                <div style="height:30px;">
                                    <a href="javascript:document.location.href='../securityconfig/index.jsp?serviceName=wso2carbon-sts'"
                                       class="icon-link"
                                       style="background-image:url(images/configure.gif);margin-left: 0"><fmt:message key='apply.security.policy'/></a>
                                </div>
                            </td>
                            <td>
                                <div style="height:30px;">
                                    <a href="javascript:document.location.href='<%=passiveSTSUrl+"?wsdl"%>'"
                                       class="icon-link"
                                       style="background-image:url(images/sts.gif);margin-left: 0"><%=passiveSTSUrl%>
                                    </a>
                                </div>
                            </td>
                            <td>
                                <div style="height:30px;">
                                    <a href="javascript:document.location.href='../generic-sts/passive-sts.jsp'"
                                       class="icon-link"
                                       style="background-image:url(images/sts.gif);margin-left: 0"><fmt:message key='passive.sts.config'/></a>
                                    </a>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='passive.sts.realm'/></td>
                            <td>
                                <input id="passiveSTSRealm" name="passiveSTSRealm" type="text" value="<%=passiveSTSRealm%>"/>
                                <div class="sectionHelp">
                                    <fmt:message key='passive.sts.realm.help'/>
                                </div>
                            </td>
                        </tr>
                    </table>
                </div>
                <div class="buttonRow">
                    <input type="button" value="<fmt:message key='update'/>" onclick="idpMgtUpdate();"/>
                    <input type="button" value="<fmt:message key='cancel'/>" onclick="idpMgtCancel();"/>
                </div>
            </form>
        </div>
    </div>
</fmt:bundle>