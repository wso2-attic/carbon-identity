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
	String spName = appBean.getServiceProvider().getApplicationName();
	      
	StringBuffer localAuthTypes = new StringBuffer();
	String startOption = "<option value=\"";
	String middleOption = "\">";
	String endOPtion = "</option>";	
    
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



	function createAppOnclick() {

			document.getElementById("configure-sp-form").submit();
		
	}
   
    jQuery(document).ready(function(){
        jQuery('#outboundProvisioning').hide();
        jQuery('#inboundProvisioning').hide();  
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
            <fmt:message key='title.local.service.providers'/>
        </h2>
        <div id="workArea">
            <form id="configure-sp-form" method="post" name="configure-sp-form" method="post" action="configure-service-provider-finish.jsp" >
            <input type="hidden" value="wso2carbon-local-sp" name="spName">
            <input type="hidden" value="<%=appBean.getServiceProvider().getDescription() %>" name="sp-description">
            
            
            <h2 id="inbound_provisioning_head" class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="inbound.provisioning.head"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="inboundProvisioning">
            
             <h2 id="scim-inbound_provisioning_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="scim.inbound.provisioning.head"/></a>
             </h2>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="scim-inbound-provisioning-div">
                <table class="carbonFormTable">
                  <tr><td>SCIM/SOAP provisioning is protected via HTTP Basic Authentication.You must use a privileged local account to invoke the API.<br/>
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