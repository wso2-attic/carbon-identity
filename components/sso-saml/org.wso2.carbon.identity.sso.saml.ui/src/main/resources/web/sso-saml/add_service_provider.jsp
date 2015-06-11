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
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page
        import="org.wso2.carbon.CarbonError" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOServiceProviderInfoDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOUIConstants" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.client.SAMLSSOConfigServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page
        import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page
        import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="org.wso2.carbon.security.keystore.service.CertData" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<jsp:useBean id="samlSsoServuceProviderConfigBean"
             type="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
             class="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConfigBean"
             scope="session"/>
<jsp:setProperty name="samlSsoServuceProviderConfigBean" property="*"/>
<jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle
        basename="org.wso2.carbon.identity.sso.saml.ui.i18n.Resources">
<carbon:breadcrumb label="sso.configuration"
                   resourceBundle="org.wso2.carbon.identity.sso.saml.ui.i18n.Resources"
                   topPage="true" request="<%=request%>"/>

<script type="text/javascript" src="global-params.js"></script>
<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

<script type="text/javascript">
/* function validateAndSubmit(){
	if($('#existingIssuers').length > 0 && $('#existingIssuers').val().length > 0 && $('#issuer').val().length > 0){
		var existingIssuers = $('#existingIssuers').val().split(",");
		var issuer = $('#issuer').val();
		if(existingIssuers.length > 0){
			var isValid = true;
			$.each(existingIssuers, function(){
				if(this.length > 0 && this == issuer){
					isValid = false;
					return false;
				}
			});
			if(!isValid){
		        CARBON.showWarningDialog('Service Provider with Issuer "'+issuer+'" is already registered.', null, null);
		        return false;
			}
		}
	}
	if(doValidation()){
		document.getElementById("addServiceProvider").submit();
	}
} */

function doValidation() {
    var fld = document.getElementsByName("issuer")[0];
    var value = fld.value;
    if (value.length == 0) {
        CARBON.showWarningDialog(
                "<fmt:message key='sp.enter.valid.issuer'/>", null,
                null);
        return false;
    } else if(value.indexOf("@") > -1){
        CARBON.showWarningDialog(
                "<fmt:message key='sp.entity.id.cannot.have.at'/>", null,
                null);
        return false;
    }
	
    var fld2 = document.getElementsByName("assrtConsumerURL")[0];
    var value = fld2.value;
    var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
    if (value.length == 0) {
        CARBON.showWarningDialog(
                "<fmt:message key='sp.enter.valid.endpoint.address'/>",
                null, null);
        return false;

    }

    if (!regexp.test(value)) {
        CARBON.showWarningDialog(
                "<fmt:message key='sp.enter.valid.endpoint.address'/>",
                null, null);
        return false;
    }

    value = value.replace(/^\s+/, "");
    if (value.length == 0) {
        CARBON.showWarningDialog(
                "<fmt:message key='sp.enter.valid.endpoint.address'/>",
                null, null);
        return false;

    }

    var fld3 = document.getElementsByName("logoutURL")[0];
    var value = fld3.value;
    var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
    if (value.length != 0) {
        value = value.replace(/^\s+/, "");
        if (value.length != 0) {
            if (!regexp.test(value)) {
                CARBON.showWarningDialog(
                        "<fmt:message key='sp.enter.valid.logout.endpoint.address'/>",
                        null, null);
                return false;
            }
        }
    }

    //if (document.getElementsByName("subjectType")[1].checked) {
    //    var claimVal = document.getElementsByName("claimID")[0].value;
    //    if (claimVal.length == 0) {
    //        CARBON.showWarningDialog(
    //                "<fmt:message key='sp.enter.valid.claimID'/>",
    //                null, null);
    //        return false;
    //    }
    //}
    return true;
}

function doValidationUrl() {
    var fld = document.getElementsByName("metadataFromUrl")[0];
        var value = fld.value;
        var regexp = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
            if (value.length != 0) {
                value = value.replace(/^\s+/, "");
                if (value.length != 0) {
                    if (!regexp.test(value)) {
                        CARBON.showWarningDialog(
                                "<fmt:message key='sp.enter.valid.url'/>",
                                null, null);
                        return false;
                    }
                }
            }else{
                CARBON.showWarningDialog("<fmt:message key='sp.enter.valid.url'/>",null, null);
                return false;
            }

            return true;
}

function edit(issuer) {
    location.href = "manage_service_providers.jsp?region=region1&item=manage_saml_sso&SPAction=editServiceProvider&issuer=" + issuer;
}

function removeItem(issuer) {
    CARBON.showConfirmationDialog(
            "<fmt:message key='remove.message1'/>" + " " + issuer
                    + "<fmt:message key='remove.message2'/>",
            function () {
                location.href = "remove_service_providers.jsp?issuer="
                        + issuer;
            }, null);
}

function showHideTxtBox(radioBtn) {
    var claimIdRow = document.getElementById('claimIdRow');
    var nameIdRow = document.getElementById('nameIdRow');
    if (radioBtn.checked && radioBtn.value == "useClaimId") {
        claimIdRow.style.display = "";
        nameIdRow.style.display = "";
    } else {
        claimIdRow.style.display = "none";
        nameIdRow.style.display = "none";
    }
}
function disableCertAlias(chkbx) {
    document.addServiceProvider.alias.disabled = (chkbx.checked||document.addServiceProvider.enableEncAssertion.checked) ? false
            : true;
}
function disableEncCertAlias(chkbx) {
    document.addServiceProvider.alias.disabled = (chkbx.checked||document.addServiceProvider.enableSigValidation.checked) ? false
            : true;
}

function disableLogoutUrl(chkbx) {
    document.addServiceProvider.logoutURL.disabled = (chkbx.checked) ? false
            : true;
}

function disableFullQualifiedUsername(chkbx) {
    document.addServiceProvider.useFullQualifiedUsername.value = (chkbx.checked) ? true
            : false;
    document.addServiceProvider.enableNameIdClaimUriHidden.value  =  (chkbx.checked) ? false
            : true;

    if (chkbx.checked) {
        document.getElementById("enableNameIdClaimUri").checked = 'false';
    }

}

function disableResponseSignature(chkbx) {
    document.addServiceProvider.enableResponseSignature.value = (chkbx.checked) ? true
            : false;
}
function disableAssertionSignature(chkbx) {
    document.addServiceProvider.enableAssertionSignature.value = (chkbx.checked) ? true
            : false;
}
function disableAttributeProfile(chkbx) {
    if (!(chkbx.checked)){
    document.addServiceProvider.enableDefaultAttributeProfile.checked = false;
    }
    document.addServiceProvider.enableDefaultAttributeProfile.value = chkbx.checked;
    document.addServiceProvider.enableDefaultAttributeProfile.disabled = (chkbx.checked) ? false
            : true;

}

function disableNameIdClaimUri(chkbx) {
    if (chkbx.checked) {
        document.addServiceProvider.enableNameIdClaimUriHidden.value = "true";
        document.addServiceProvider.useFullQualifiedUsername.value = "false";
        document.getElementById("useFullQualifiedUsername").checked = 'false';
    } else {
        document.addServiceProvider.enableNameIdClaimUriHidden.value = "false";
    }

}

function disableDefaultAttributeProfile(chkbx) {
    if (chkbx.checked) {
        document.addServiceProvider.enableDefaultAttributeProfileHidden.value = "true";
    } else {
        document.addServiceProvider.enableDefaultAttributeProfileHidden.value = "false";
    }

}
function disableAudienceRestriction(chkbx) {
    document.addServiceProvider.audience.disabled = (chkbx.checked) ? false
            : true;
    document.addServiceProvider.addAudience.disabled = (chkbx.checked) ? false
            : true;
}
function disableRecipients(chkbx) {
    document.addServiceProvider.recipient.disabled = (chkbx.checked) ? false
            : true;
    document.addServiceProvider.addRecipient.disabled = (chkbx.checked) ? false
            : true;
}
function addClaim() {
    var propertyCount = document.getElementById("claimPropertyCounter");

    var i = propertyCount.value;
    var currentCount = parseInt(i);

    currentCount = currentCount + 1;
    propertyCount.value = currentCount;

    document.getElementById('claimTableId').style.display = '';
    var claimTableTBody = document.getElementById('claimTableTbody');

    var claimRow = document.createElement('tr');
    claimRow.setAttribute('id', 'claimRow' + i);

    var claim = document.getElementById('claim').value;
    var claimPropertyTD = document.createElement('td');
    claimPropertyTD.setAttribute('style', 'padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;');
    claimPropertyTD.innerHTML = "" + claim + "<input type='hidden' name='claimPropertyName" + i + "' id='claimPropertyName" + i + "'  value='" + claim + "'/> ";

    var claimRemoveTD = document.createElement('td');
    claimRemoveTD.innerHTML = "<a href='#' class='icon-link' style='background-image: url(../admin/images/delete.gif)' onclick='removeClaim(" + i + ");return false;'>" + "Delete" + "</a>";

    claimRow.appendChild(claimPropertyTD);
    claimRow.appendChild(claimRemoveTD);

    claimTableTBody.appendChild(claimRow);
}
function addAudienceFunc() {
    var propertyCount = document.getElementById("audiencePropertyCounter");

    var i = propertyCount.value;
    var currentCount = parseInt(i);

    currentCount = currentCount + 1;
    propertyCount.value = currentCount;

    document.getElementById('audienceTableId').style.display = '';
    var audienceTableTBody = document.getElementById('audienceTableTbody');

    var audienceRow = document.createElement('tr');
    audienceRow.setAttribute('id', 'audienceRow' + i);

    var audience = document.getElementById('audience').value;
    var audiencePropertyTD = document.createElement('td');
    audiencePropertyTD.setAttribute('style', 'padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;');
    audiencePropertyTD.innerHTML = "" + audience + "<input type='hidden' name='audiencePropertyName" + i + "' id='audiencePropertyName" + i + "'  value='" + audience + "'/> ";

    var audienceRemoveTD = document.createElement('td');
    audienceRemoveTD.innerHTML = "<a href='#' class='icon-link' style='background-image: url(../admin/images/delete.gif)' onclick='removeAudience(" + i + ");return false;'>" + "Delete" + "</a>";

    audienceRow.appendChild(audiencePropertyTD);
    audienceRow.appendChild(audienceRemoveTD);

    audienceTableTBody.appendChild(audienceRow);
}
function addRecipientFunc() {
    var propertyCount = document.getElementById("recipientPropertyCounter");

    var i = propertyCount.value;
    var currentCount = parseInt(i);

    currentCount = currentCount + 1;
    propertyCount.value = currentCount;

    document.getElementById('recipientTableId').style.display = '';
    var recipientTableTBody = document.getElementById('recipientTableTbody');

    var recipientRow = document.createElement('tr');
    recipientRow.setAttribute('id', 'recipientRow' + i);

    var recipient = document.getElementById('recipient').value;
    var recipientPropertyTD = document.createElement('td');
    recipientPropertyTD.setAttribute('style', 'padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;');
    recipientPropertyTD.innerHTML = "" + recipient + "<input type='hidden' name='recipientPropertyName" + i + "' id='recipientPropertyName" + i + "'  value='" + recipient + "'/> ";

    var recipientRemoveTD = document.createElement('td');
    recipientRemoveTD.innerHTML = "<a href='#' class='icon-link' style='background-image: url(../admin/images/delete.gif)' onclick='removeRecipient(" + i + ");return false;'>" + "Delete" + "</a>";

    recipientRow.appendChild(recipientPropertyTD);
    recipientRow.appendChild(recipientRemoveTD);

    recipientTableTBody.appendChild(recipientRow);
}

function removeClaim(i) {
    var propRow = document.getElementById("claimRow" + i);
    if (propRow != undefined && propRow != null) {
        var parentTBody = propRow.parentNode;
        if (parentTBody != undefined && parentTBody != null) {
            parentTBody.removeChild(propRow);
            if (!isContainRaw(parentTBody)) {
                var propertyTable = document.getElementById("claimTableId");
                propertyTable.style.display = "none";

            }
        }
    }
}

function removeAudience(i) {
    var propRow = document.getElementById("audienceRow" + i);
    if (propRow != undefined && propRow != null) {
        var parentTBody = propRow.parentNode;
        if (parentTBody != undefined && parentTBody != null) {
            parentTBody.removeChild(propRow);
            if (!isContainRaw(parentTBody)) {
                var propertyTable = document.getElementById("audienceTableId");
                propertyTable.style.display = "none";
            }
        }
    }
}

function removeRecipient(i) {
    var propRow = document.getElementById("recipientRow" + i);
    if (propRow != undefined && propRow != null) {
        var parentTBody = propRow.parentNode;
        if (parentTBody != undefined && parentTBody != null) {
            parentTBody.removeChild(propRow);
            if (!isContainRaw(parentTBody)) {
                var propertyTable = document.getElementById("recipientTableId");
                propertyTable.style.display = "none";
            }
        }
    }
}

function disableIdPInitSSO(chkbx) {
    document.addServiceProvider.disableIdPInitSSO.value = (chkbx.checked) ? true
            : false;
}

function isContainRaw(tbody) {
    if (tbody.childNodes == null || tbody.childNodes.length == 0) {
        return false;
    } else {
        for (var i = 0; i < tbody.childNodes.length; i++) {
            var child = tbody.childNodes[i];
            if (child != undefined && child != null) {
                if (child.nodeName == "tr" || child.nodeName == "TR") {
                    return true;
                }
            }
        }
    }
    return false;
}

function clearAll() {
    document.addServiceProvider.action = "update_claims.jsp?action=clear";
    document.addServiceProvider.submit();
}
</script>

<%
    String cookie;
    String serverURL;
    ConfigurationContext configContext;
    SAMLSSOConfigServiceClient spConfigClient = (SAMLSSOConfigServiceClient) session.getAttribute(SAMLSSOUIConstants.CONFIG_CLIENT);
    ArrayList<String> aliasSet = null;
    String[] claimUris = null;
    String configPath = null;
    CertData certData = null;
    
    String applicationSPName = request.getParameter("spName");
    session.setAttribute("application-sp-name", applicationSPName);

    SAMLSSOServiceProviderInfoDTO serviceProviderInfoDTO = null;
    ArrayList<SAMLSSOServiceProviderDTO> providers =
            new ArrayList<SAMLSSOServiceProviderDTO>();
    String reload = null;

    try {
        reload = request.getParameter("reload");
        serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        configContext =
                (ConfigurationContext) config.getServletContext()
                        .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        if (spConfigClient == null) {

            spConfigClient =
                    new SAMLSSOConfigServiceClient(cookie, serverURL,
                            configContext);
            if (spConfigClient.getRegisteredServiceProviders().getServiceProviders() != null) {
                session.setAttribute(SAMLSSOUIConstants.CONFIG_CLIENT, spConfigClient);
            }

        }

        serviceProviderInfoDTO = spConfigClient.getRegisteredServiceProviders();
        if (serviceProviderInfoDTO.getServiceProviders() != null) {

            Collections.addAll(providers, serviceProviderInfoDTO.getServiceProviders());
        }
        aliasSet = spConfigClient.getCertAlias();
        claimUris = spConfigClient.getClaimURIs();
    } catch (AxisFault e) {
        CarbonError error = new CarbonError();
        error.addError(e.getMessage());
        request.getSession().setAttribute(CarbonError.ID, error);
%>
<script type="text/javascript">
    location.href = '../admin/error.jsp';
</script>
<%
    }
%>

<div id="middle">
<h2>
    <fmt:message key="saml.sso.register.service.provider"/>
</h2>

<div id="workArea">
<%
    SAMLSSOServiceProviderDTO provider = null;
    String spAction = request.getParameter("SPAction");
    String claimTableStyle = "display:none";
    String audienceTableStyle = "display:none";
    String recipientTableStyle = "display:none";
    String issuer = request.getParameter("issuer");
    String attributeConsumingServiceIndex = "";
    boolean isEditSP = false;
    if (spAction != null && "editServiceProvider".equals(spAction)) {
        if (providers.size() > 0) {
            for (SAMLSSOServiceProviderDTO sp : providers) {
                if (issuer.equals(sp.getIssuer())) {
                    isEditSP = true;
                    provider = sp;
                    claimTableStyle = provider.getRequestedClaims().length > 0 ? "" : "display:none";
                    audienceTableStyle = provider.getRequestedAudiences().length > 0 ? "" : "display:none";
                    recipientTableStyle = provider.getRequestedRecipients().length > 0 ? "" : "display:none";
                    if(provider.getAttributeConsumingServiceIndex() != null){
                        attributeConsumingServiceIndex = provider.getAttributeConsumingServiceIndex();
                    }
                }
            }
        }
    }

/*     String existingIssuers = "";
    if (!isEditSP) {
    	if (providers.size() > 0) {
    		for (SAMLSSOServiceProviderDTO sp : providers) {
    			existingIssuers += sp.getIssuer()+",";
    		}
    	}
    } */
%>

<form method="POST" action="add_service_provider_finish.jsp?SPAction=<%=spAction%>"
      id="addServiceProvider" name="addServiceProvider" target="_self"
      onsubmit="return doValidation();">
<%--     <%
        if (!isEditSP) {
    %>
		<input type="hidden" id="existingIssuers" value="<%=existingIssuers%>">
	<%
        }
	%> --%>
<table class="styledLeft" width="100%">
<thead>
<tr>
    <%
        if (isEditSP) {
    %>
    <th><fmt:message key="saml.sso.edit.service.provider"/><%=provider.getIssuer()%>)</th>
    <%
    } else {
    %>
    <th><fmt:message key="saml.sso.new.service.provider"/></th>
    <%
        }
    %>
</tr>
</thead>
<tbody>
<tr>
<td class="formRow">
<table class="normal" cellspacing="0" style="width: 100%;">
<tr>
    <td style="width: 300px;">
        <fmt:message key="sp.issuer"/>
        <font color="red">*</font>
    </td>
    <td><input type="text" id="issuer" name="issuer" maxlength="30"
               class="text-box-big"
               value="<%=isEditSP? provider.getIssuer():""%>" <%=isEditSP ? "disabled=\"disabled\"" : ""%>/>
        <input type="hidden" id="hiddenIssuer" name="hiddenIssuer"
               value="<%=isEditSP? provider.getIssuer():""%>"/>
    </td>
</tr>
<tr>
    <td>
        <fmt:message key="sp.assertionConsumerURL"/>
        <font color="red">*</font>
    </td>
    <td>
        <input type="text" id="assrtConsumerURL"
               name="assrtConsumerURL" class="text-box-big"
               value="<%=isEditSP?provider.getAssertionConsumerUrl():""%>"/>
    </td>
</tr>

<!-- NameID format -->

<tr>
    <td>
        <fmt:message key="sp.nameIDFormat"/>
    </td>
    <td>
        <input type="text" id="nameIdFormat"
               name="nameIdFormat" class="text-box-big"
               value="<%=isEditSP?provider.getNameIDFormat().replace("/", ":"):"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"%>"/>
    </td>
</tr>

<!-- UseFullQualifiedUsername -->
<tr>
    <td colspan="2" style="padding-top: 25px">
        <input type="checkbox" name="useFullQualifiedUsername" value="true"
               onclick="disableFullQualifiedUsername(this);"
                <%=(isEditSP && provider.getUseFullyQualifiedUsername() ? "checked=\"checked\"" : "")%> />
        <fmt:message key="use.fullqualified.username"/>
    </td>
</tr>

<% if(applicationSPName == null || applicationSPName.isEmpty()){ %>

<!-- UseUserClaimValueInNameID -->

<% if (isEditSP && provider.getNameIdClaimUri()!=null) {
%>
<tr>
    <td colspan="2">
        <input type="checkbox"
               name="enableNameIdClaimUri" value="true" checked="checked"
               onclick="disableNameIdClaimUri(this);"/>
        <input type="hidden" id="enableNameIdClaimUriHidden" name="enableNameIdClaimUriHidden" value="true" />

        <fmt:message
                key='define.nameid'/>
    </td>
</tr>
<tr>
    <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <select id="nameIdClaim" name="nameIdClaim">
            <%
                if (claimUris != null) {
                    for (String claimUri : claimUris) {
                        if (claimUri != null) {
                            if (claimUri.equals(provider.getNameIdClaimUri())) {

            %>
            <option selected="selected" value="<%=claimUri%>"><%=claimUri%></option>
            <% } else { %>
            <option value="<%=claimUri%>"><%=claimUri%></option>
            <%
                            }
                        }
                    }
                }
            %>
        </select>
    </td>
</tr>
<% } else {%>
<tr>
    <td colspan="2">
        <input type="checkbox"
               name="enableNameIdClaimUri" value="true"
               onclick="disableNameIdClaimUri(this);"/>
        <fmt:message
                key='define.nameid'/>
        <input type="hidden" id="enableNameIdClaimUriHidden" name="enableNameIdClaimUriHidden" />

    </td>
</tr>
<tr>
    <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <select id="nameIdClaim" name="nameIdClaim">
            <%
                if (claimUris != null) {
                    for (String claimUri : claimUris) {
                        if (claimUri != null) {
            %>
            <option value="<%=claimUri%>"><%=claimUri%></option>
            <%
                        }
                    }
                }
            %>
        </select>
    </td>
</tr>
<%		}
	}%>

<tr>
    <td colspan="2">
        <input type="checkbox" name="enableResponseSignature" value="true"
               onclick="disableResponseSignature(this);"
                <%=(isEditSP && provider.getDoSignResponse() ? "checked=\"checked\"" : "")%> />
            <%--<input type="hidden" name="enableResponseSignature" value="true"/>--%>
        <fmt:message key="do.response.signature"/>
    </td>
</tr>

<tr>
    <td colspan="2">
        <input type="checkbox" name="enableAssertionSignature" value="true"
               onclick="disableAssertionSignature(this);"
                <%=(isEditSP && provider.getDoSignAssertions() ? "checked=\"checked\"" : "")%> />
            <%--<input type="hidden" name="enableAssertionSignature" value="true"/>--%>
        <fmt:message key="do.assertion.signature"/>
    </td>
</tr>
<!-- enableSigValidation -->
<%
    if (isEditSP && provider.isDoValidateSignatureInRequestsSpecified() && provider.getDoValidateSignatureInRequests()) {
%>
<tr>
    <td colspan="2"  style="padding-top: 25px">
        <input type="checkbox" id="enableSigValidation"
               name="enableSigValidation" value="true" checked="checked"
               onclick="disableCertAlias(this);"/>
        <fmt:message
                key='validate.signature'/>
    </td>
</tr>
<% } else {%>
<tr>
    <td colspan="2"  style="padding-top: 25px">
        <input type="checkbox" id="enableSigValidation"
               name="enableSigValidation" value="true"
               onclick="disableCertAlias(this);"/>
        <fmt:message
                key='validate.signature'/>
    </td>
</tr>
<%}%>

<!-- Enable Encrypted assertion -->
<% if (isEditSP && provider.isDoEnableEncryptedAssertionSpecified() && provider.getDoEnableEncryptedAssertion()) {
%>
<tr>
    <td colspan="2">
        <input type="checkbox" id="enableEncAssertion"
               name="enableEncAssertion" value="true" checked="checked"
               onclick="disableEncCertAlias(this);"/>
        <fmt:message
                key='encrypted.assertion'/>
    </td>
</tr>
<% } else {%>
<tr>
    <td colspan="2">
        <input type="checkbox" id="enableEncAssertion"
               name="enableEncAssertion" value="true"
               onclick="disableEncCertAlias(this);"/>
        <fmt:message
                key='encrypted.assertion'/>
    </td>
</tr>
<%}%>

<!-- Certificate Alias -->

<% if (isEditSP && 
		((provider.isDoEnableEncryptedAssertionSpecified() && provider.getDoEnableEncryptedAssertion())
		||(provider.isDoValidateSignatureInRequestsSpecified() && provider.getDoValidateSignatureInRequests()))) {
%>
<tr>
    <td  style="padding-left: 33px ! important;padding-bottom: 25px">
        <fmt:message key="sp.certAlias"/>
    </td>
    <td style="padding-left: 33px ! important;padding-bottom: 25px">
    <select id="alias" name="alias">
        <%
            if (aliasSet != null) {
                for (String alias : aliasSet) {
                    if (alias != null) {
                        if (alias.equals(provider.getCertAlias())) {
        %>
        <option selected="selected" value="<%=alias%>"><%=alias%>
        </option>
        <%
        } else {
        %>
        <option value="<%=alias%>"><%=alias%>
        </option>
        <%
                        }
                    }
                }
            }
        %>
    </select></td>
</tr>
<% } else {%>
<tr>
    <td  style="padding-left: 33px ! important;padding-bottom: 25px">
        <fmt:message key="sp.certAlias"/>
    </td>
    <td style="padding-left: 33px ! important;padding-bottom: 25px">
    <select id="alias" name="alias" disabled="disabled">
        <%
            if (aliasSet != null) {
                for (String alias : aliasSet) {
                    if (alias != null) {
                        if (alias.equals(samlSsoServuceProviderConfigBean.getCertificateAlias())) {
        %>
        <option selected="selected" value="<%=alias%>"><%=alias%>
        </option>
        <%
        } else {
        %>
        <option value="<%=alias%>"><%=alias%>
        </option>
        <%
                        }
                    }
                }
            }
        %>
    </select></td>
</tr>
<%}%>
<!-- EnableSingleLogout -->
<%
    if (isEditSP && provider.getDoSingleLogout()) {
%>
<tr>
    <td colspan="2"><input type="checkbox"
                           name="enableSingleLogout" value="true"
                           onclick="disableLogoutUrl(this);" checked="checked"/> <fmt:message
            key="enable.single.logout"/></td>
</tr>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="logout.url"/>
    </td>
    <td><input type="text" id="logoutURL" name="logoutURL"
               value="<%=(provider.getLogoutURL().equals(provider.getAssertionConsumerUrl()))?"":provider.getLogoutURL()%>"
               class="text-box-big"></td>
</tr>
<% } else {%>
<tr>
    <td colspan="2"><input type="checkbox"
                           name="enableSingleLogout" value="true"
                           onclick="disableLogoutUrl(this);"/> <fmt:message
            key="enable.single.logout"/></td>
</tr>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="logout.url"/>
    </td>
    <td><input type="text" id="logoutURL" name="logoutURL"
               value="<%=samlSsoServuceProviderConfigBean.getSingleLogoutUrl()%>"
               class="text-box-big" disabled="disabled"></td>
</tr>
<% } %>

<!-- EnableAttributeProfile -->
<% 
boolean show = false;
if (applicationSPName == null || applicationSPName.isEmpty()) {
	show = provider.getRequestedClaims().length > 0 && provider.getRequestedClaims()[0] != null;
} else {
	show = true;
}

if (isEditSP && show) {
%>
<tr>
    <td colspan="2">
        <% if (StringUtils.isNotEmpty(provider.getAttributeConsumingServiceIndex())) { %>
        <input type="checkbox"
               name="enableAttributeProfile" id="enableAttributeProfile" checked="checked" value="true"
               onclick="disableAttributeProfile(this);"/>
        <% } else { %>
        <input type="checkbox"
               name="enableAttributeProfile" id="enableAttributeProfile"
               onclick="disableAttributeProfile(this);"/>
        <% } %>
        <fmt:message
                key="enable.attribute.profile"/></td>
</tr>
<% if(applicationSPName == null || applicationSPName.isEmpty()){ %>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="sp.claim"/>
    </td>
    <td>
        <select id="claim" name="claim">
            <%
                if (claimUris != null) {
                    for (String claimUri : claimUris) {
                        if (claimUri != null) {
            %>
            <option value="<%=claimUri%>"><%=claimUri%>
            </option>
            <%
                        }
                    }
                }
            %>
        </select> <input id="addClaims" name="addClaims" type="button"
                         value="<fmt:message key="saml.sso.add.claim"/>"
                         onclick="addClaim()"/>
    </td>
</tr>
<% 		} %>
<tr>
    <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;" colspan="2">
        <% if (StringUtils.isNotEmpty(provider.getAttributeConsumingServiceIndex())) {
            if (provider.getEnableAttributesByDefault()) {
        %> <input type="checkbox"
                  name="enableDefaultAttributeProfile" id="enableDefaultAttributeProfile" checked="checked" value="true"
                  onclick="disableDefaultAttributeProfile(this);"/>
        <input type="hidden" id="enableDefaultAttributeProfileHidden" name="enableDefaultAttributeProfileHidden"
               value="true"/>
        <%} else {%>
        <input type="checkbox"
               name="enableDefaultAttributeProfile" id="enableDefaultAttributeProfile"
               onclick="disableDefaultAttributeProfile(this);"/>
        <input type="hidden" id="enableDefaultAttributeProfileHidden" name="enableDefaultAttributeProfileHidden"
                />
        <% }
        } else {%>
        <input type="hidden" id="enableDefaultAttributeProfileHidden" name="enableDefaultAttributeProfileHidden"/>
        <input type="checkbox"
               name="enableDefaultAttributeProfile" id="enableDefaultAttributeProfile" disabled="disabled"
               onclick="disableDefaultAttributeProfile(this);"/>
        <% } %>
        <fmt:message key="enable.default.attribute.profile"/>
    </td>
</tr>

<% } else {%>
<tr>
    <td colspan="2">
        <input type="checkbox"
               name="enableAttributeProfile" id="enableAttributeProfile" value="true"
               onclick="disableAttributeProfile(this);"/>
        <fmt:message key="enable.attribute.profile"/>
    </td>
</tr>
<% if(applicationSPName == null || applicationSPName.isEmpty()){ %>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="sp.claim"/>
    </td>
    <td>
        <select id="claim" name="claim" disabled="disabled">
            <%
                if (claimUris != null) {
                    for (String claimUri : claimUris) {
                        if (claimUri != null) {
            %>
            <option value="<%=claimUri%>"><%=claimUri%>
            </option>
            <%
                        }
                    }
                }
            %>
        </select> <input id="addClaims" name="addClaims" type="button"
                         disabled="disabled" value="<fmt:message key="saml.sso.add.claim"/>"
                         onclick="addClaim()"/>
    </td>
</tr>
<%		} %>
<tr>
    <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;" colspan="2">
        <input type="hidden" id="enableDefaultAttributeProfileHidden" name="enableDefaultAttributeProfileHidden" />
        <input type="checkbox"
               name="enableDefaultAttributeProfile" id="enableDefaultAttributeProfile"  onclick="disableDefaultAttributeProfile(this);" />
        <fmt:message key="enable.default.attribute.profile"/>
    </td>
</tr>

<%} %>
<tr>
    <td>
        <table id="claimTableId" style="<%=claimTableStyle%>" class="styledInner">
            <tbody id="claimTableTbody">
            <%
                int i = 0;
                if (isEditSP && provider.getRequestedClaims().length > 0) {
            %>
            <%
                for (String claim : provider.getRequestedClaims()) {
                    if (claim != null && !"null".equals(claim)) {
            %>
            <tr id="claimRow<%=i%>">
                <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
                    <input type="hidden" name="claimPropertyName<%=i%>" id="claimPropertyName<%=i%>"
                           value="<%=claim%>"/><%=claim%>
                </td>
                <td>
                    <a onclick="removeClaim('<%=i%>');return false;"
                       href="#" class="icon-link"
                       style="background-image: url(../admin/images/delete.gif)">Delete
                    </a>
                </td>
            </tr>
            <%
                        i++;
                    }
                }
            %>
            <%
                }
            %>
            <input type="hidden" name="claimPropertyCounter" id="claimPropertyCounter"
                   value="<%=i%>"/>
            </tbody>
        </table>
    </td>
</tr>

<!-- EnableAudienceRestriction -->
<% if (isEditSP && provider.getRequestedAudiences().length > 0 && provider.getRequestedAudiences()[0] != null) {
%>
<tr>
    <td colspan="2"><input type="checkbox"
                           name="enableAudienceRestriction" id="enableAudienceRestriction"
                           value="true" checked="checked"
                           onclick="disableAudienceRestriction(this);"/> <fmt:message
            key="enable.audience.restriction"/></td>
</tr>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="sp.audience"/>
    </td>
    <td>
        <input type="text" id="audience" name="audience"
               class="text-box-big"/>
        <input id="addAudience" name="addAudience" type="button"
               value="<fmt:message key="saml.sso.add.audience"/>"
               onclick="addAudienceFunc()"/>
    </td>
</tr>
<% } else {%>
<tr>
    <td colspan="2">
        <input type="checkbox"
               name="enableAudienceRestriction" id="enableAudienceRestriction" value="true"
               onclick="disableAudienceRestriction(this);"/>
        <fmt:message key="enable.audience.restriction"/>
    </td>
</tr>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="sp.audience"/>
    </td>
    <td>
        <input type="text" id="audience" name="audience"
               class="text-box-big"/>
        <input id="addAudience" name="addAudience" type="button"
               disabled="disabled" value="<fmt:message key="saml.sso.add.audience"/>"
               onclick="addAudienceFunc()"/>
    </td>
</tr>
<%} %>
<tr>
    <td>
        <table id="audienceTableId" style="<%=audienceTableStyle%>" class="styledInner">
            <tbody id="audienceTableTbody">
            <%
                int j = 0;
                if (isEditSP && provider.getRequestedAudiences().length > 0) {
            %>
            <%
                for (String audience : provider.getRequestedAudiences()) {
                    if (audience != null && !"null".equals(audience)) {
            %>
            <tr id="audienceRow<%=j%>">
                <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
                    <input type="hidden" name="audiencePropertyName<%=j%>"
                           id="audiencePropertyName<%=j%>" value="<%=audience%>"/><%=audience%>
                </td>
                <td>
                    <a onclick="removeAudience('<%=j%>');return false;"
                       href="#" class="icon-link"
                       style="background-image: url(../admin/images/delete.gif)">Delete
                    </a>
                </td>
            </tr>
            <%
                        j++;
                    }
                }
            %>
            <%
                }
            %>
            <input type="hidden" name="audiencePropertyCounter" id="audiencePropertyCounter"
                   value="<%=j%>"/>
            </tbody>
        </table>
    </td>
</tr>

<!-- EnableRecipientValidation -->
<% if (isEditSP && provider.getRequestedRecipients().length > 0 && provider.getRequestedRecipients()[0] != null) {
%>
<tr>
    <td colspan="2"><input type="checkbox"
                           name="enableRecipients" id="enableRecipients"
                           value="true" checked="checked"
                           onclick="disableRecipients(this);"/> <fmt:message
            key="enable.recipient.validation"/></td>
</tr>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="sp.recipient"/>
    </td>
    <td>
        <input type="text" id="recipient" name="recipient"
               class="text-box-big"/>
        <input id="addRecipient" name="addRecipient" type="button"
               value="<fmt:message key="saml.sso.add.recipient"/>"
               onclick="addRecipientFunc()"/>
    </td>
</tr>
<% } else {%>
<tr>
    <td colspan="2">
        <input type="checkbox"
               name="enableRecipients" id="enableRecipients" value="true"
               onclick="disableRecipients(this);"/>
        <fmt:message key="enable.recipient.validation"/>
    </td>
</tr>
<tr>
    <td
            style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
        <fmt:message key="sp.recipient"/>
    </td>
    <td>
        <input type="text" id="recipient" name="recipient"
               class="text-box-big"/>
        <input id="addRecipient" name="addRecipient" type="button"
               disabled="disabled" value="<fmt:message key="saml.sso.add.recipient"/>"
               onclick="addRecipientFunc()"/>
    </td>
</tr>
<%} %>
<tr>
    <td>
        <table id="recipientTableId" style="<%=recipientTableStyle%>" class="styledInner">
            <tbody id="recipientTableTbody">
            <%
                int k = 0;
                if (isEditSP && provider.getRequestedRecipients().length > 0) {
            %>
            <%
                for (String recipient : provider.getRequestedRecipients()) {
                    if (recipient != null && !"null".equals(recipient)) {
            %>
            <tr id="recipientRow<%=k%>">
                <td style="padding-left: 40px ! important; color: rgb(119, 119, 119); font-style: italic;">
                    <input type="hidden" name="recipientPropertyName<%=k%>"
                           id="recipientPropertyName<%=k%>" value="<%=recipient%>"/><%=recipient%>
                </td>
                <td>
                    <a onclick="removeRecipient('<%=k%>');return false;"
                       href="#" class="icon-link"
                       style="background-image: url(../admin/images/delete.gif)">Delete
                    </a>
                </td>
            </tr>
            <%
                        k++;
                    }
                }
            %>
            <%
                }
            %>
            <input type="hidden" name="recipientPropertyCounter" id="recipientPropertyCounter"
                   value="<%=k%>"/>
            </tbody>
        </table>
    </td>
</tr>

<!-- IdP-Initiated SSO -->
<tr>
    <td colspan="2">
        <input type="checkbox" name="enableIdPInitSSO" value="true"
               onclick="disableIdPInitSSO(this);"
                <%=(isEditSP && provider.getIdPInitSSOEnabled() ? "checked=\"checked\"" : "")%> />
        <fmt:message key="enable.idp.init.sso"/>
    </td>
</tr>


</table>
</td>
</tr>
<tr>
    <td class="buttonRow">
        <%
            if (isEditSP) {
        %>
        <input class="button" type="submit" value="<fmt:message key="saml.sso.edit"/>"/>
        <%
        } else {
        %>
        <input class="button" type="submit" value="<fmt:message key="saml.sso.register"/>"/>
        <%
            }
        %>
        <input class="button" type="button" onclick="clearAll()"
               value="<fmt:message key="saml.sso.cancel"/>"/>
    </td>
</tr>

</tbody>
</table>
<input type="hidden" id="attributeConsumingServiceIndex" name="attributeConsumingServiceIndex" value="<%=attributeConsumingServiceIndex%>"/>
</form>


    <%
        if (!isEditSP) {
    %>
    <form method="POST" action="../../fileupload/service"
          id="uploadServiceProvider" name="uploadServiceProvider" target="_self" enctype="multipart/form-data"
          onsubmit="return doValidation();">
          <br/>
        <table class="styledLeft" width="100%">
        <thead>
            <tr>
                <th><fmt:message key="saml.sso.upload.service.provider.metadata"/></th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td><span>File Location: </span><input type="file" id="metadataFromFileSystem"
                                                   name="metadataFromFileSystem" size="50" /></td>
            </tr>
            <tr><td>
                <input type="button" value="<fmt:message key='saml.sso.upload'/>" class="button" onclick="doSubmit();"/>
                <input class="button" type="reset" value="<fmt:message key='saml.sso.cancel'/>" onclick="doCancel();"/></td>
                </tr>
            </tbody>
        </table>
    </form>
    <br/>
    <form method="POST" action="upload_service_provider_from_url_finish.jsp?SPAction=<%=spAction%>"
              id="uploadServiceProviderFromUrl" name="uploadServiceProviderFromUrl" target="_self" onsubmit="return doValidationUrl();">
              <br/>
            <table class="styledLeft" width="100%">
            <thead>
                <tr>
                    <th><fmt:message key="saml.sso.upload.service.provider.metadata.from.url"/></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><span>URL: </span><input type="url" class="text-box-big" id="metadataFromUrl" name="metadataFromUrl"/></td>
                </tr>
                <tr><td>
                    <input type="submit" class="button" value="<fmt:message key='saml.sso.upload'/>" class="button"/>
                    <input class="button" type="reset" value="<fmt:message key='saml.sso.cancel'/>" onclick="doCancel();"/></td>
                    </tr>
                </tbody>
            </table>
        </form>
    <%
    }
    %>

<script type="text/javascript">

                function doSubmit(){
                    var policy;
                    policy = document.uploadServiceProvider.metadataFromFileSystem.value;


                    if (policy == '') {
                        CARBON.showWarningDialog("<fmt:message key='sp.enter.valid.issuer'/>");
                        return;
                    }

                    document.uploadServiceProvider.submit();
                }

                function doCancel(){
                    location.href = 'index.jsp';
                }

            </script>
</div>
</div>
</fmt:bundle>
