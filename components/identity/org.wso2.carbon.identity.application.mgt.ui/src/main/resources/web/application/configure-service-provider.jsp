<!--
~ Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

<%@ page import="org.wso2.carbon.identity.application.common.model.xsd.*"%>

<%@page import="java.util.HashMap"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Arrays" %>
<%@page import="org.wso2.carbon.identity.application.mgt.ui.ApplicationBean"%>
<link href="css/idpmgt.css" rel="stylesheet" type="text/css" media="all"/>
<jsp:useBean id="appBean" class="org.wso2.carbon.identity.application.mgt.ui.ApplicationBean" scope="session"/>   
<carbon:breadcrumb label="breadcrumb.service.provider" resourceBundle="org.wso2.carbon.identity.application.mgt.ui.i18n.Resources"
                    topPage="true" request="<%=request%>" />
<jsp:include page="../dialog/display_messages.jsp"/>


<script type="text/javascript" src="../admin/js/main.js"></script>



<%
if (appBean.getServiceProvider()==null || appBean.getServiceProvider().getApplicationName()==null){
// if appbean is not set properly redirect the user to list-service-provider.jsp.
%>
<script>
location.href = 'list-service-provider.jsp';
</script>
<% 
}
	String spName = appBean.getServiceProvider().getApplicationName();
	
	List<String> permissions = null;		
	permissions = appBean.getPermissions();
	
	String[] allClaimUris = appBean.getClaimUris();	
	Map<String, String> claimMapping = appBean.getClaimMapping();
	Map<String, String> roleMapping = appBean.getRoleMapping();
    String idPName = request.getParameter("idPName");
    String action = request.getParameter("action");

    
    String authTypeReq =  request.getParameter("authType");
    if (authTypeReq!=null && authTypeReq.trim().length()>0){
    	appBean.setAuthenticationType(authTypeReq);
    }
    
    String samlIssuerName = request.getParameter("samlIssuer");


    if (samlIssuerName!=null && "update".equals(action)){
    	appBean.setSAMLIssuer(samlIssuerName);
    }
    
    if (samlIssuerName==null && "delete".equals(action)){
    	appBean.setSAMLIssuer(null);
    }
    
	samlIssuerName = appBean.getSAMLIssuer();

    
    String oauthapp = request.getParameter("oauthapp");
    
    if (oauthapp!=null && "update".equals(action)){
    	appBean.setOIDCAppName(oauthapp);
    }
    
    if (oauthapp==null && "delete".equals(action)){
    	appBean.setOIDCAppName(null);
    }
    
    oauthapp = appBean.getOIDCClientId();
   
    String wsTrust = request.getParameter("serviceName");
    
    if (wsTrust!=null && "update".equals(action)){
    	appBean.setWstrustEp(wsTrust);
    }
    
    if (wsTrust==null && "delete".equals(action)){
    	appBean.setWstrustEp(null);
    }
    
    wsTrust  = appBean.getWstrustSP();
    
    String display = request.getParameter("display");
    
    
    if(idPName != null && idPName.equals("")){
        idPName = null;
    }
    
    String authType = appBean.getAuthenticationType();
    
	StringBuffer localAuthTypes = new StringBuffer();
	String startOption = "<option value=\"";
	String middleOption = "\">";
	String endOPtion = "</option>";	
    
	StringBuffer requestPathAuthTypes = new StringBuffer();
	RequestPathAuthenticator[] requestPathAuthenticators = appBean.getRequestPathAuthenticators();

	if (requestPathAuthenticators!=null && requestPathAuthenticators.length>0){
		for(RequestPathAuthenticator reqAuth : requestPathAuthenticators) {
			requestPathAuthTypes.append(startOption + reqAuth.getName() + middleOption + reqAuth.getName() + endOPtion);
		}
	}
	
	Map<String, String> idpAuthenticators = new HashMap<String, String>();
	FederatedIdentityProvider[] federatedIdPs = appBean.getFederatedIdentityProviders();
	Map<String, String> proIdpConnector = new HashMap<String, String>();
	Map<String, String> selectedProIdpConnectors = new HashMap<String, String>();

	StringBuffer idpType = null;
	StringBuffer connType = null;

	
	if (federatedIdPs!=null && federatedIdPs.length>0) {
		idpType = new StringBuffer();
		StringBuffer provisioningConnectors = null;
		for(FederatedIdentityProvider idp : federatedIdPs) {
			if (idp.getProvisoningConnectors()!=null && idp.getProvisoningConnectors().length>0){	
				ProvisioningConnector[] connectors =  idp.getProvisoningConnectors();
				int i = 1;
				connType = new StringBuffer();
				provisioningConnectors = new StringBuffer();
				for (ProvisioningConnector proConnector : connectors){
					if (i == connectors.length ){
						provisioningConnectors.append(proConnector.getName());
					} else {
						provisioningConnectors.append(proConnector.getName() + ",");
					}
					connType.append(startOption + proConnector.getName() + middleOption + proConnector.getName() + endOPtion);
					i++;
				}
				proIdpConnector.put(idp.getIdentityProviderName(), connType.toString());
				idpType.append(startOption + idp.getIdentityProviderName() + "\" data=\""+provisioningConnectors.toString() + "\" >" + idp.getIdentityProviderName() + endOPtion); 
			} 
			
		}
		
		if (appBean.getServiceProvider().getOutboundProvisioningConfiguration() != null
				&& appBean.getServiceProvider().getOutboundProvisioningConfiguration() .getProvisioningIdentityProviders()!=null 
			&& appBean.getServiceProvider().getOutboundProvisioningConfiguration() .getProvisioningIdentityProviders().length>0) {
		
			FederatedIdentityProvider[]  proIdps = appBean.getServiceProvider().getOutboundProvisioningConfiguration() .getProvisioningIdentityProviders();			
		    for (FederatedIdentityProvider idp : proIdps){
				ProvisioningConnector proIdp = idp.getDefaultProvisioinongConnector();
				String options = proIdpConnector.get(idp.getIdentityProviderName());
				if (proIdp!=null) {
					String conName = proIdp.getName();
					String oldOption = startOption + proIdp.getName() + middleOption + proIdp.getName() + endOPtion;
					String newOption = startOption + proIdp.getName() + "\" selected=\"selected" + middleOption + proIdp.getName() + endOPtion;
					options = options.replace(oldOption, newOption);
					selectedProIdpConnectors.put(idp.getIdentityProviderName(), options);
				}else {
					selectedProIdpConnectors.put(idp.getIdentityProviderName(), options);
				}
			}
		}
		
	}
%>

<script>


<% if(claimMapping != null) {%>
var claimMappinRowID = <%=claimMapping.size() -1 %>;
<%} else {%>
var claimMappinRowID = -1;
<%}%>

var reqPathAuth = 0;

<%if(appBean.getServiceProvider().getRequestPathAuthenticators() != null){%>
var reqPathAuth = <%=appBean.getServiceProvider().getRequestPathAuthenticators().length%>;
<%} else {%>
var reqPathAuth = 0;
<%}%>

<% if(roleMapping != null) {%>
var roleMappinRowID = <%=roleMapping.size() -1 %>;
<% } else { %>
var roleMappinRowID = -1;
<% } %>

	function createAppOnclick() {
		var spName = document.getElementById("spName").value;
		if( spName == '') {
			CARBON.showWarningDialog('<fmt:message key="alert.please.provide.service.provider.id"/>');
			location.href = '#';
		} else {
			var numberOfPermissions = document.getElementById("permissionAddTable").rows.length;
			document.getElementById('number_of_permissions').value=numberOfPermissions;
			
			var numberOfRoleMappings = document.getElementById("roleMappingAddTable").rows.length;
			document.getElementById('number_of_rolemappings').value=numberOfRoleMappings;
			
			// number_of_claimmappings
			var numberOfClaimMappings = document.getElementById("claimMappingAddTable").rows.length;
			document.getElementById('number_of_claimmappings').value=numberOfClaimMappings;
			
			document.getElementById("configure-sp-form").submit();
		}
	}

    function onSamlSsoClick() {
		var spName = document.getElementById("spName").value;
		if( spName != '') {
			document.getElementById("saml_link").href="../sso-saml/add_service_provider.jsp?spName="+spName;
		} else {
			CARBON.showWarningDialog('<fmt:message key="alert.please.provide.service.provider.id"/>');
			document.getElementById("saml_link").href="#"
		}
	}
    
	function onOauthClick() {
		var spName = document.getElementById("spName").value;
		if( spName != '') {
			document.getElementById("oauth_link").href="../oauth/add.jsp?spName=" + spName;
		} else {
			CARBON.showWarningDialog('<fmt:message key="alert.please.provide.service.provider.id"/>');
			document.getElementById("oauth_link").href="#"
		}
	}
	
	function onSTSClick() {
		var spName = document.getElementById("spName").value;
		if( spName != '') {
			document.getElementById("sts_link").href="../generic-sts/sts.jsp?spName=" + spName;
		} else {
			CARBON.showWarningDialog('<fmt:message key="alert.please.provide.service.provider.id"/>');
			document.getElementById("sts_link").href="#"
		}
	}
	
	function deleteReqPathRow(obj){
    	reqPathAuth--;
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
	
	function onAdvanceAuthClick() {
		location.href="configure-authentication-flow.jsp"
	}
    
    jQuery(document).ready(function(){
        jQuery('#authenticationConfRow').hide();
        jQuery('#outboundProvisioning').hide();
        jQuery('#inboundProvisioning').hide();  
        jQuery('#ReqPathAuth').hide();        
        jQuery('#permissionConfRow').hide();
        jQuery('#claimsConfRow').hide();
        jQuery('h2.trigger').click(function(){
            if (jQuery(this).next().is(":visible")) {
                this.className = "active trigger";
            } else {
                this.className = "trigger";
            }
            jQuery(this).next().slideToggle("fast");
            return false; //Prevent the browser jump to the link anchor
        })
        jQuery('#permissionAddLink').click(function(){
            jQuery('#permissionAddTable').append(jQuery('<tr><td><input type="text" id="app_permission" name="app_permission"/></td>' +
                    '<td><a onclick="deletePermissionRow(this)" class="icon-link" '+
                    'style="background-image: url(images/delete.gif)">'+
                    'Delete'+
                    '</a></td></tr>'));
        })
        jQuery('#claimMappingAddLink').click(function(){
        	var selectedIDPClaimName = $('select[name=idpClaimsList]').val();
        	claimMappinRowID++;
        	jQuery('#claimMappingAddTable').append(jQuery('<tr>'+
        	        '<td><input type="text" value="' + selectedIDPClaimName + '" id="idpClaim_'+ claimMappinRowID +'" name="idpClaim_'+ claimMappinRowID +'" readonly="readonly"/></td>' +
                    '<td><input type="text" id="spClaim_' + claimMappinRowID + '" name="spClaim_' + claimMappinRowID + '"/></td> '+
                    '<td><input type="checkbox"  name="spClaim_req_' + claimMappinRowID + '"  id="spClaim_req_' + claimMappinRowID + '"/></td>' + 
                    '<td><a onclick="deletePermissionRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete</a></td>' + 

                    '</tr>'));

        })
        jQuery('#roleMappingAddLink').click(function(){
        	roleMappinRowID++;
        	jQuery('#roleMappingAddTable').append(jQuery('<tr><td><input type="text" id="idpRole_'+ roleMappinRowID +'" name="idpRole_'+ roleMappinRowID +'"/></td>' +
                    '<td><input type="text" id="spRole_' + roleMappinRowID + '" name="spRole_' + roleMappinRowID + '"/></td> '+
                    '<td><a onclick="deletePermissionRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete</a>' + 
                    '</td></tr>'));
        })
         jQuery('#reqPathAuthenticatorAddLink').click(function(){
        	reqPathAuth++;
    		var selectedRePathAuthenticator =jQuery(this).parent().children()[0].value;
    		jQuery(this)
    				.parent()
    				.parent()
    				.parent()
    				.parent()
    				.append(
    						jQuery('<tr><td><input name="req_path_auth' + '" id="req_path_auth" type="hidden" value="' + selectedRePathAuthenticator + '" />'+selectedRePathAuthenticator +'</td><td class="leftCol-small" ><a onclick="deleteReqPathRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a></td></tr>'));	
    		
        })
    })
    
    function deletePermissionRow(obj){
    jQuery(obj).parent().parent().remove();

    }
    
    var deletePermissionRows = [];
    function deletePermissionRowOld(obj){
        if(jQuery(obj).parent().prev().children()[0].value != ''){
        	deletePermissionRows.push(jQuery(obj).parent().prev().children()[0].value);
        }
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
    
    function addIDPRow(obj) {
		var selectedObj = jQuery(obj).prev().find(":selected");

		var selectedIDPName = selectedObj.val(); 
		//var stepID = jQuery(obj).parent().children()[1].value;
		var dataArray =  selectedObj.attr('data').split(',');
		var newRow = '<tr><td><input name="provisioning_idp" id="" type="hidden" value="' + selectedIDPName + '" />' + selectedIDPName + ' </td><td> <select name="provisioning_con_idp_' + selectedIDPName + '" style="float: left; min-width: 150px;font-size:13px;">';
		for(var i=0;i<dataArray.length;i++){
			newRow+='<option>'+dataArray[i]+'</option>';	
		}
		newRow+='</select></td><td class="leftCol-small" ><a onclick="deleteIDPRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a></td></tr>';
		jQuery(obj)
				.parent()
				.parent()
				.parent()
				.parent()
				.append(
						jQuery(newRow));	
		}	
    
    function deleteIDPRow(obj){
        jQuery(obj).parent().parent().remove();
    }
</script>

<fmt:bundle basename="org.wso2.carbon.identity.application.mgt.ui.i18n.Resources">
    <div id="middle">
        <h2>
            <fmt:message key='title.service.providers'/>
        </h2>
        <div id="workArea">
            <form id="configure-sp-form" method="post" name="configure-sp-form" method="post" action="configure-service-provider-finish.jsp" >
            <div class="sectionSeperator togglebleTitle"><fmt:message key='title.config.app.basic.config'/></div>
            <div class="sectionSub">
                <table class="carbonFormTable">
                    <tr>
                        <td style="width:15%" class="leftCol-med labelField"><fmt:message key='config.application.info.basic.name'/>:<span class="required">*</span></td>
                        <td>
                            <input style="width:50%" id="spName" name="spName" type="text" value="<%=spName%>" autofocus/>
                            <div class="sectionHelp">
                                <fmt:message key='help.name'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                       <td style="width:15%" class="leftCol-med labelField">Description:<span class="required">*</span></td>                   
                     <td>
                        <textarea style="width:50%" type="text" name="sp-description" id="sp-description" class="text-box-big"><%=appBean.getServiceProvider().getDescription() %></textarea>
                        <div class="sectionHelp">
                                <fmt:message key='help.desc'/>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>

            <h2 id="app_authentication_head"  class="sectionSeperator trigger active">	
                <a href="#"><fmt:message key="title.config.app.authentication"/></a>
            </h2>
            
            <%if (display!=null && (display.equals("oauthapp") || display.equals("samlIssuer")  || display.equals("serviceName") )) { %>
                  <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="inbound_auth_request_div">
            <%} else { %>
                  <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="inbound_auth_request_div">           
            <%} %>
            <h2 id="saml.config.head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="title.config.saml2.web.sso.config"/></a>
            </h2>
            
           <%if (display!=null && display.equals("samlIssuer")) { %>            
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="saml.config.div">
          <% } else { %>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="saml.config.div">          
          <% } %>
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField">
                        <%
                        	if(appBean.getSAMLIssuer() == null) {
                        %>
                            <a id="saml_link" href="javascript:document.location.href='#'" class="icon-link" onclick="onSamlSsoClick()"><fmt:message
									key='auth.configure' /></a>
						 <%
						 	} else {
						 %>
						 		<div style="clear:both"></div>
							 	<table class="styledLeft" id="samlTable">
                                <thead><tr><th class="leftCol-med"><fmt:message key='title.table.saml.config.issuer'/></th><th><fmt:message key='application.info.saml2sso.action'/></th></tr></thead>
                                <tbody>
                                <tr><td><%=appBean.getSAMLIssuer()%></td><td style="white-space: nowrap;"><a
										title="Edit Service Providers" href="../sso-saml/add_service_provider.jsp?SPAction=editServiceProvider&issuer=<%=appBean.getSAMLIssuer()%>"  class="icon-link"
								 style="background-image: url(../admin/images/edit.gif)">Edit</a></td></tr>
                                </tbody>
                                </table>		
						 <%
						    }
						 %>
							<div style="clear:both"></div>
                        </td>
                    </tr>
                    </table>
                    
                    </div>
                      <h2 id="oauth.config.head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="title.config.oauth2.oidc.config"/></a>
            </h2>
            <%if (display!=null && display.equals("oauthapp")) { %>                        
                <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="oauth.config.div">
            <%} else { %>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="oauth.config.div">
            <%} %>
                <table class="carbonFormTable">
                    <tr>
                    	<td>
	                    	<%
	                    		if(appBean.getOIDCClientId() == null) {
	                    	%>
			                        <a id="oauth_link" href="javascript:document.location.href='#'" class="icon-link" onclick="onOauthClick()">
									<fmt:message key='auth.configure' /></a>
							 <%
							 	} else {
							 %>
							 <div style="clear:both"></div>
							 <table class="styledLeft" id="samlTable">
                                <thead><tr><th class="leftCol-med">OAuth Client Key</th><th><fmt:message key='application.info.oauthoidc.action'/></th></tr></thead>
                                <tbody>
                                <tr><td><%=appBean.getOIDCClientId()%></td><td style="white-space: nowrap;"><a
										title="Edit Service Providers" href="../oauth/edit.jsp?appName=<%=spName%>"  class="icon-link"
										style="background-image: url(../admin/images/edit.gif)">Edit</a></td></tr>
                                </tbody>
                                </table>
							 <%
							 	}
							 %>
							<div style="clear:both"></div>
                        </td>
                    </tr>
                    </table>
                    </div>
                     <h2 id="wst.config.head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="title.config.sts.config"/></a>
            </h2>
            <%if (display!=null && display.equals("serviceName")) { %>                                    
               <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="wst.config.div">
            <% } else { %>
               <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="wst.config.div">            
            <%} %>
                  <table class="carbonFormTable">
                    
                    <tr>
                    	<td>
                    	    <%
	                    		if(appBean.getWstrustSP() == null) {
	                    	%>
	                        <a id="sts_link" href="javascript:document.location.href='#'" class="icon-link" onclick="onSTSClick()">
							 <fmt:message key='auth.configure' /></a>
							 <%
							 	} else {
							 %>
							 <div style="clear:both"></div>
							 <table class="styledLeft" id="samlTable">
                                <thead><tr><th class="leftCol-med">Audience</th><th><fmt:message key='application.info.oauthoidc.action'/></th></tr></thead>
                                <tbody>
                                <tr><td><%=appBean.getWstrustSP()%></td><td style="white-space: nowrap;"><a
										title="Edit Audience" href="../generic-sts/sts.jsp?spName=<%=spName%>&&spAudience=<%=appBean.getWstrustSP()%>&spAction=spEdit"  class="icon-link"
										style="background-image: url(../admin/images/edit.gif)">Edit</a></td></tr>
                                </tbody>
                                </table>
							 <%
							 	}
							 %>
							<div style="clear:both"></div>
                        </td>
                    </tr>
                   
                  </table>
            </div>
            
             <h2 id="passive.sts.config.head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#">WS-Federation (Passive) Configuration</a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="passive.config.div">
                  <table class="carbonFormTable">
                    
                    <tr>
                    	<td>
                    	    <%
	                    		if(appBean.getPassiveSTSRealm() != null) {
	                    	%>	                    
                            <input style="width:50%" id="passiveSTSRealm" name="passiveSTSRealm" type="text" value="<%=appBean.getPassiveSTSRealm()%>" autofocus/>
                            <% } else { %>
                            <input style="width:50%" id="passiveSTSRealm" name="passiveSTSRealm" type="text" value="<%=spName%>" autofocus/>
                            <% } %>
                          <div class="sectionHelp">
                                <fmt:message key='help.passive.sts'/>
                            </div>
                        </td>
                        
                    </tr>
                   
                  </table>
            </div>
            
            <h2 id="openid.config.head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#">OpenID Configuration</a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="openid.config.div">
                  <table class="carbonFormTable">
                    
                    <tr>
                    	<td>
                    	    <%
	                    		if(appBean.getOpenIDRealm() != null) {
	                    	%>	                    
                            <input style="width:50%" id="openidRealm" name="openidRealm" type="text" value="<%=appBean.getOpenIDRealm()%>" autofocus/>
                            <% } else { %>
                            <input style="width:50%" id="openidRealm" name="openidRealm" type="text" value="<%=appBean.getServiceProvider().getApplicationName()%>" autofocus/>
                            <% } %>
                          <div class="sectionHelp">
                                <fmt:message key='help.openid'/>
                            </div>
                        </td>
                        
                    </tr>
                   
                  </table>
            </div>
                  
            </div>
            
             <h2 id="app_authentication_advance_head"  class="sectionSeperator trigger active">
               		<a href="#"><fmt:message key="outbound.title.config.app.authentication.type"/></a>
           		  </h2>
           		  <%if (display!=null && "auth_config".equals(display)) {%>
           		    <div class="toggle_container sectionSub" style="margin-bottom:10px;display:block;" id="advanceAuthnConfRow">
           		  <% } else { %>
                    <div class="toggle_container sectionSub" style="margin-bottom:10px;display:none;" id="advanceAuthnConfRow">
                   <% } %>
                   	<table class="carbonFormTable">
                    	<tr>
                    		<td class="leftCol-med labelField"><fmt:message key='config.application.info.authentication.advance.type'/>:<span class="required">*</span>
                    		</td>
                        	<td class="leftCol-med">
                        	<% if(ApplicationBean.AUTH_TYPE_DEFAULT.equals(appBean.getAuthenticationType())) { %>
                        		<input type="radio" name="auth_type" value="default" checked><fmt:message key="config.authentication.type.default"/>
                        		<% } else { %>
                        		<input type="radio" name="auth_type" value="default" ><fmt:message key="config.authentication.type.default"/>
                        	<% } %>
                        	</td>
                        	<td/>
                    	</tr>   
                  		  	<tr>
                    		<td style="width:15%" class="leftCol-med labelField"/>
                        	<td>
                        	<% if(ApplicationBean.AUTH_TYPE_LOCAL.equals(appBean.getAuthenticationType())) { %>
                        		<input type="radio" name="auth_type" value="local" checked><fmt:message key="config.authentication.type.local"/>
                        		<% } else { %>
                        		<input type="radio" name="auth_type" value="local"><fmt:message key="config.authentication.type.local"/>
                        		<% } %>
                        	</td>
                        	<td>
                        			<select name="local_authenticator" id="local_authenticator">
                        			<%
                        			if(appBean.getLocalAuthenticators() != null) {
                        				LocalAuthenticator[] localAuthenticators = appBean.getLocalAuthenticators();
                        			    for(LocalAuthenticator authenticator : localAuthenticators) {
                        			%>
	                        				<% if(authenticator.getName().equals(appBean.getStepZeroAuthenticatorName(ApplicationBean.AUTH_TYPE_LOCAL))) { %>
												<option value="<%=authenticator.getName()%>" selected><%=authenticator.getName()%></option>	
											<% } else { %>
												<option value="<%=authenticator.getName()%>"><%=authenticator.getName()%></option>	
											<% } %>
										<% } %>
									<% } %>
									</select>
                        	</td>
                    	</tr>   
                    	<% 
                    	
                    	if(appBean.getFederatedIdentityProviders()  != null && appBean.getFederatedIdentityProviders().length > 0) {%>
                    	<tr>
                    		<td class="leftCol-med labelField"/>
                        	<td>
                        	<% if(ApplicationBean.AUTH_TYPE_FEDERATED.equals(appBean.getAuthenticationType())) { %>
                        		<input type="radio" name="auth_type" value="federated" checked><fmt:message key="config.authentication.type.federated"/>
                        	<% } else { %>
                        		<input type="radio" name="auth_type" value="federated"><fmt:message key="config.authentication.type.federated"/>
                        	<% } %>
                        	</td>
                        	<td>
                        			<select name="fed_idp" id="fed_idp">
                        			<% FederatedIdentityProvider[] idps = appBean.getFederatedIdentityProviders();
                        				for(FederatedIdentityProvider idp : idps) {
                        			%>
	                        			<% if(idp.getIdentityProviderName().equals(appBean.getStepZeroAuthenticatorName(ApplicationBean.AUTH_TYPE_FEDERATED))) { %>
											<option value="<%=idp.getIdentityProviderName()%>" selected><%=idp.getIdentityProviderName() %></option>
											<% } else { %>
											<option value="<%=idp.getIdentityProviderName() %>"><%=idp.getIdentityProviderName()%></option>
										<%  } %>
									<%  } %>
									</select>
                        	</td>
                    	</tr> 
                    	<% } %>
                    	<tr>
                    		<td class="leftCol-med labelField"/>
                        	<td>
                        	<% if(ApplicationBean.AUTH_TYPE_FLOW.equals(appBean.getAuthenticationType())) { %>
                        		<input type="radio" name="auth_type" value="flow" onclick="onAdvanceAuthClick()" checked><fmt:message key="config.authentication.type.flow"/>
                        	<% } else { %>
                        		<input type="radio" name="auth_type" value="flow" onclick="onAdvanceAuthClick()"><fmt:message key="config.authentication.type.flow"/>
                        		<% } %>
                        	</td>
                    	</tr>               
                  </table>
                  
                   <h2 id="req_path_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="title.req.config.authentication.steps"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="ReqPathAuth">
                    <table class="styledLeft" width="100%" id="req_path_auth_table">
                    	<thead>
                    	<tr>
                    		<td>
                    			<select name="reqPathAuthType" style="float: left; min-width: 150px;font-size:13px;"><%=requestPathAuthTypes.toString()%></select>
                    			<a id="reqPathAuthenticatorAddLink" class="icon-link" style="background-image:url(images/add.gif);"></a>
                    		</td>
                    	</tr>
                    	</thead>
                    	
                    	<%
                    	 if(appBean.getServiceProvider().getRequestPathAuthenticators() != null && appBean.getServiceProvider().getRequestPathAuthenticators().length>0){
                    		 int x = 0;
                    		 for (RequestPathAuthenticator reqAth : appBean.getServiceProvider().getRequestPathAuthenticators()) {
                    			 if (reqAth!=null) {
                    			 %>
                    			 <tr>
                    			 <td>
                    			 	<input name="req_path_auth" id="req_path_auth" type="hidden" value="<%=reqAth.getName()%>" />
                    			 	<%=reqAth.getName()%>
                    			 </td>
                    			 <td class="leftCol-small" >
                    			 	<a onclick="deleteReqPathRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a>
                    			 </td>
                    			 </tr>	      			 
                    			 <%  
                    			 }
                    		 }
                    	 }
                    	
                    	%>
                    </table> 
            </div>
                  
            </div>
            
            <h2 id="inbound_provisioning_head" class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="inbound.provisioning.head"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="inboundProvisioning">
            
             <h2 id="scim-inbound_provisioning_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="scim.inbound.provisioning.head"/></a>
             </h2>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="scim-inbound-provisioning-div">
                <table class="carbonFormTable">
                  <tr><td>Service provider based SCIM provisioning is protected via OAuth 2.0. 
                  Your service provider must have a valid OAuth 2.0 client key and a client secret to invoke the SCIM API.
                  To create OAuth 2.0 key/secret : Inbound Authentication Configuration -> OAuth/OpenID Connect Configuration.<br/>
                  </td></tr>
                   <tr>
                        <td >
                        <% if(appBean.getServiceProvider().getInboundProvisioningConfiguration() != null
                              && appBean.getServiceProvider().getInboundProvisioningConfiguration().getProvisioningUserStore()!=null) { %>
                            <input style="width:50%" id="scim-inbound-userstore" name="scim-inbound-userstore" type="text" value="<%=appBean.getServiceProvider().getInboundProvisioningConfiguration().getProvisioningUserStore()%>" autofocus/>
                            <% } else { %>
                            <input style="width:50%" id="scim-inbound-userstore" name="scim-inbound-userstore" type="text" value="" autofocus/>
                            <% } %>
                          <div class="sectionHelp">
                                <fmt:message key='help.inbound.scim'/>
                            </div>
                        </td>
                    </tr>
                    </table>
                </div>
            
            
            </div>
            
            <h2 id="outbound_provisioning_head" class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="outbound.provisioning.head"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="outboundProvisioning">
             <table class="styledLeft" width="100%" id="fed_auth_table">
            
		      <% if (idpType!=null) {%>
		       <thead> 
		       
					<tr>
						<td>				             	  
							 <select name="provisioning_idps" style="float: left; min-width: 150px;font-size:13px;">
							             			<%=idpType.toString()%>
							 </select>
						     <a id="provisioningIdpAdd" onclick="addIDPRow(this);return false;" class="icon-link" style="background-image:url(images/add.gif);"></a>
						</td>
		            </tr>
		           
	           </thead>
	            <% } else { %>
	           <table class="carbonFormTable" width="100%" id="fed_auth_table">
	            
		              <tr><td>There are no provisioning enabled identity providers defined in the system.</td></tr>
		        <%} %>
							                 
	           <%
	           	   if (idpType!=null && appBean.getServiceProvider().getInboundProvisioningConfiguration() != null ) {							      
				   			FederatedIdentityProvider[] fedIdps = appBean.getServiceProvider().getOutboundProvisioningConfiguration().getProvisioningIdentityProviders();
							      if (fedIdps!=null && fedIdps.length>0){
							      			for(FederatedIdentityProvider idp:fedIdps) {
							      				if (idp!=null) {
	           %>
							      
							      	       <tr>
							      	      	   <td>
							      	      		<input name="provisioning_idp" id="" type="hidden" value="<%=idp.getIdentityProviderName()%>" />
							      	      			<%=idp.getIdentityProviderName()%>
							      	      		</td>
							      	      		<td> 
							      	      			<select name="provisioning_con_idp_<%=idp.getIdentityProviderName()%>" style="float: left; min-width: 150px;font-size:13px;"><%=selectedProIdpConnectors.get(idp.getIdentityProviderName())%></select>
							      	      		</td>
							      	      		<td class="leftCol-small" >
							      	      		<a onclick="deleteIDPRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a>
							      	      		</td>
							      	       </tr>						      
			    <%
							      		}							      			
							      	}								      	
						  }
	           	    }
				%>
			  </table>
            
            </div>          

            <h2 id="authorization_permission_head" class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="title.config.app.authorization.permission"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="permissionConfRow">
            <h2 id="permission_mapping_head" class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#">Permissions</a>
            		</h2>
            	   <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="appPermissionRow">
                <table class="carbonFormTable">
                   <tr>
                        <td>
                            <a id="permissionAddLink" class="icon-link" style="background-image:url(images/add.gif);margin-left:0;"><fmt:message key='button.add.permission'/></a>
                            <div style="clear:both"></div>
                           	
                            <table class="styledLeft" id="permissionAddTable" >
                                <thead>
                                </thead>
                                <tbody>
                                <% if(permissions != null && !permissions.isEmpty()){ %>
                               
                                <% for(int i = 0; i < permissions.size(); i++){ 
                                if (permissions.get(i)!=null){
                                %>
                                
                                <tr>
                                    <td><input type="text" value="<%=permissions.get(i)%>" id="app_permission" name="app_permission" readonly="readonly"/></td>
                                    <td>
                                        <a title="<fmt:message key='alert.info.delete.permission'/>"
                                           onclick="deletePermissionRow(this);return false;"
                                           href="#"
                                           class="icon-link"
                                           style="background-image: url(images/delete.gif)">
                                            <fmt:message key='link.delete'/>
                                        </a>
                                    </td>
                                </tr>
                                <% } } %>
                                <% } %>
                                </tbody>
                            </table>
                            <div style="clear:both"/>
                            <input type="hidden" name="number_of_permissions" id="number_of_permissions" value="1">
                        </td>
                    </tr>
                    
					</table>
					</div>
					<h2 id="role_mapping_head" class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#">Role Mapping</a>
            		</h2>
            	   <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="roleMappingRowRow">
                    <table>
                    <tr>
						<td>
							<a id="roleMappingAddLink" class="icon-link" style="background-image: url(images/add.gif);margin-left:0;"><fmt:message key='button.add.role.mapping' /></a>
							<div style="clear:both"/>
                            <div class="sectionHelp">
                                <fmt:message key='help.role.mapping'/>
                            </div>
						</td>
					</tr>
                    </table>
					<table class="styledLeft" id="roleMappingAddTable" style="display:none">
                              <thead><tr><th class="leftCol-med"><fmt:message key='title.table.role.idp.role'/></th><th class="leftCol-big"><fmt:message key='title.table.role.sp.role'/></th><th><fmt:message key='config.application.authz.permissions.action'/></th></tr></thead>
                              <tbody>
                              <% if(roleMapping != null && !roleMapping.isEmpty()){ %>
                              <script>
                                  $(jQuery('#roleMappingAddTable')).toggle();
                              </script>
                               <% 
                              	int i = -1;
                               for(Map.Entry<String, String> entry : roleMapping.entrySet()){ 
                            	   i++;
                               %>
                               <tr>
                               	<td >
                               		<input type="text" value="<%=entry.getKey()%>" id="idpRole_<%=i%>" name="idpRole_<%=i%>" readonly="readonly"/>
                               	</td>
                                   <td><input type="text" value="<%=entry.getValue()%>" id="spRole_<%=i%>" name="spRole_<%=i%>" readonly="readonly"/></td>
                                   <td>
                                       <a title="<fmt:message key='alert.info.delete.rolemap'/>"
                                          onclick="deletePermissionRow(this);return false;"
                                          href="#"
                                          class="icon-link"
                                          style="background-image: url(images/delete.gif)">
                                           <fmt:message key='link.delete'/>
                                       </a>
                                   </td>
                               </tr>
                               <% } %>
                              <% } %>
						</tbody>
                      </table>
					<input type="hidden" name="number_of_rolemappings" id="number_of_rolemappings" value="1">
					</div>
            </div>
            
            <h2 id="claims_head" class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="title.config.app.claim"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="claimsConfRow">
                <table class="carbonFormTable">
                   <tr>
                        <td >
                        <% if(appBean.getRoleClaimUri() != null) { %>
                            <input style="width:50%" id="roleClaim" name="roleClaim" type="text" value="<%=appBean.getRoleClaimUri()%>" autofocus/>
                            <% } else { %>
                            <input style="width:50%" id="roleClaim" name="roleClaim" type="text" value="" autofocus/>
                            <% } %>
                          <div class="sectionHelp">
                                <fmt:message key='help.role.claim'/>
                            </div>
                        </td>
                    </tr>
                    </table>
                    
                    <h2 id="claims_mapping_head" class="sectionSeperator trigger active" style="background-color: beige;">
                		<a href="#">Claim Mapping</a>
            		</h2>
            	   <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="claimsMappingRowRow">
                    
                    <table>
					<tr>
						<td>
							<select id="idpClaimsList" name="idpClaimsList" style="float:left;">							
								<% String[] localClaims = appBean.getClaimUris();
								
								for(String localClaimName : localClaims) { %>
									<option value="<%=localClaimName%>"> <%=localClaimName%></option>
								<% } %>
							</select>
							<a id="claimMappingAddLink" class="icon-link" style="background-image: url(images/add.gif);"><fmt:message key='button.add.claim.mapping' /></a>
							<div style="clear:both"/>
                            <div class="sectionHelp">
                                <fmt:message key='help.claim.mapping'/>
                            </div>
						</td>
					</tr>
					</table>
					<table class="styledLeft" id="claimMappingAddTable" >
                              <thead><tr>
                              <th class="leftCol-med"><fmt:message key='title.table.claim.idp.claim'/></th>
                              <th class="leftCol-big"><fmt:message key='title.table.claim.sp.claim'/></th>
                              <th class="leftCol-mid">Requested Claim</th>
                              
                              <th><fmt:message key='config.application.authz.permissions.action'/></th></tr></thead>
                              <tbody>
                              <% if(claimMapping != null && !claimMapping.isEmpty()){ %>
                          
                               <% 
                               int i = -1;
                               for(Map.Entry<String, String> entry : claimMapping.entrySet()){ 
                            	   i++;
                               %>
                               <tr>
                               	<td>
                               		<input type="text" value="<%=entry.getKey()%>" id="idpClaim_<%=i%>" name="idpClaim_<%=i%>" readonly="readonly"/>
                               	</td>
                                   <td><input type="text" value="<%=entry.getValue()%>" id="spClaim_<%=i%>" name="spClaim_<%=i%>" readonly="readonly"/></td>
                                   <td>
                                   <% if ("true".equals(appBean.getRequestedClaims().get(entry.getValue()))){%>                                 
                                   <input type="checkbox"  id="spClaim_req_<%=i%>" name="spClaim_req_<%=i%>" checked/>
                                   <%} else { %>
                                    <input type="checkbox"  id="spClaim_req_<%=i%>" name="spClaim_req_<%=i%>" />
                                   <%}%>
                                   </td>
                                  
                                   <td>
                                       <a title="<fmt:message key='alert.info.delete.permission'/>"
                                          onclick="deletePermissionRow(this);return false;"
                                          href="#"
                                          class="icon-link"
                                          style="background-image: url(images/delete.gif)">
                                           <fmt:message key='link.delete'/>
                                       </a>
                                   </td>
                               </tr>
                               <% } %>
                              <% } %>
                              </tbody>
                      </table>
                      <input type="hidden" name="number_of_claimmappings" id="number_of_claimmappings" value="1">
                      </div>
            		</div>

			<div style="clear:both"/>
            <!-- sectionSub Div -->
            <div class="buttonRow">
                <input type="button" value="<fmt:message key='button.update.service.provider'/>" onclick="createAppOnclick();"/>
                <input type="button" value="<fmt:message key='button.cancel'/>" onclick="javascript:location.href='list-service-providers.jsp'"/>
            </div>
            </form>
        </div>
    </div>

</fmt:bundle>
