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
<carbon:breadcrumb label="breadcrumb.advanced.auth.step.config" resourceBundle="org.wso2.carbon.identity.application.mgt.ui.i18n.Resources"
                    topPage="true" request="<%=request%>" />
<jsp:include page="../dialog/display_messages.jsp"/>


<script type="text/javascript" src="../admin/js/main.js"></script>

<%
	String spName = appBean.getServiceProvider().getApplicationName();
	Map<String, String> claimMapping = appBean.getClaimMapping();
		
	LocalAuthenticator[] localAuthenticators = appBean.getLocalAuthenticators();
	FederatedIdentityProvider[] federatedIdPs = appBean.getFederatedIdentityProviders();
	
	StringBuffer localAuthTypes = new StringBuffer();
	String startOption = "<option value=\"";
	String middleOption = "\">";
	String endOPtion = "</option>";	
	
	if (localAuthenticators!=null && localAuthenticators.length>0) {	
		for(LocalAuthenticator auth : localAuthenticators) {
			localAuthTypes.append(startOption + auth.getName() + middleOption + auth.getName() + endOPtion);
		}
	}
	
	StringBuffer idpType = new StringBuffer();
	
	Map<String,StringBuffer> IdPAuthenticators = new HashMap<String,StringBuffer>();
	StringBuffer fedAuthenticatorType = null;
	StringBuffer fedAuthType = null;

	
%>
<script type="text/javascript" >
var authMap = {};
</script>

<%

	Map<String, String> idpAuthenticators = new HashMap<String, String>();

	if (federatedIdPs!=null && federatedIdPs.length>0) {
		for(FederatedIdentityProvider idp : federatedIdPs) {
			if (idp.getFederatedAuthenticators()!=null && idp.getFederatedAuthenticators().length>0){
				fedAuthenticatorType = new StringBuffer();
				fedAuthType = new StringBuffer();

				int i =1;
				
				for (FederatedAuthenticator fedAuth : idp.getFederatedAuthenticators()){
					if (i==idp.getFederatedAuthenticators().length){
						fedAuthenticatorType.append(fedAuth.getName());
					}else{
						fedAuthenticatorType.append(fedAuth.getName() +",");
					}
					fedAuthType.append(startOption + fedAuth.getName() + middleOption + fedAuth.getName() + endOPtion);

					i++;
				}
				
				idpAuthenticators.put(idp.getIdentityProviderName(), fedAuthType.toString());								
				idpType.append(startOption + idp.getIdentityProviderName() + "\" data=\""+fedAuthenticatorType.toString() + "\" >" + idp.getIdentityProviderName() + endOPtion);
 
			} else {				
				idpType.append(startOption + idp.getIdentityProviderName() + middleOption + idp.getIdentityProviderName() + endOPtion);
			}
			
		}
	}
	
	
	AuthenticationStep[] steps = appBean.getServiceProvider().getLocalAndOutBoundAuthenticationConfig().getAuthenticationSteps();
	Map<String,String> stepIdpAuthenticators = new HashMap<String,String>();
	
	if (steps!=null && steps.length>0){
		for (AuthenticationStep step : steps) {
			FederatedIdentityProvider[] stepFedIdps = step.getFederatedIdentityProviders();
			if(stepFedIdps != null && stepFedIdps.length>0){
				for (FederatedIdentityProvider idp : stepFedIdps){
					if (idp==null) continue;
					FederatedAuthenticator fedAuth = idp.getDefaultAuthenticator();
					String options = idpAuthenticators.get(idp.getIdentityProviderName());
					if (fedAuth!=null) {
						String authName = fedAuth.getName();
						String oldOption = startOption + fedAuth.getName() + middleOption + fedAuth.getName() + endOPtion;
						String newOption = startOption + fedAuth.getName() + "\" selected=\"selected" + middleOption + fedAuth.getName() + endOPtion;
						options = options.replace(oldOption, newOption);
						stepIdpAuthenticators.put(step.getStepOrder()+"_"+idp.getIdentityProviderName(), options);
					}else {
						stepIdpAuthenticators.put(step.getStepOrder()+"_"+idp.getIdentityProviderName(), options);
					}
				}
			}
		}
	}
	
%>

<script>
var stepOrder = 0;
<%if(steps != null){%>
var stepOrder = <%=steps.length%>;
<%} else {%>
var stepOrder = 0;
var img = "";
<%}%>



	var idpNumber = 0; 
	var reqPathAuth  = 0;
	var localAuthNumber = 0;

	function createAppOnclick() {
		
			document.getElementById("configure-auth-flow-form").submit();
	}

    jQuery(document).ready(function(){
        jQuery('#ReqPathAuth').hide();
        jQuery('#authenticationConfRow').hide();
        jQuery('#advanceAuthnConfRow').hide();
        jQuery('#permissionConfRow').hide();
        jQuery('h2.trigger').click(function(){
            if (jQuery(this).next().is(":visible")) {
                this.className = "active trigger";
            } else {
                this.className = "trigger";
            }
            jQuery(this).next().slideToggle("fast");
            return false; //Prevent the browser jump to the link anchor
        })
        jQuery('#stepsAddLink').click(function(){
        	stepOrder++;
        	jQuery('#stepsConfRow').append(jQuery('<h2 id="step_head_'+stepOrder+'" class="sectionSeperator trigger active" style="background-color: beige;"><input type="hidden" value="'+stepOrder+'" name="auth_step" id="auth_step"><a href="#">Step '+stepOrder+'</a><a onclick="deleteStep(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif);float:right;width: 9px;"></a></h2><div class="toggle_container sectionSub" style="margin-bottom:10px;" id="step_dev-'+stepOrder+'"><h2 id="local_auth_head-'+stepOrder+'" class="sectionSeperator trigger active" style="background-color: floralwhite;"><a href="#">Local Authenticators</a></h2><div class="toggle_container sectionSub" style="margin-bottom:10px;" id="local_auth_head_dev-'+stepOrder+'"><table class="styledLeft" width="100%" id="local_auth_table-'+stepOrder+'"><thead><tr><td><select name="local_auth_'+stepOrder+'" style="float: left; min-width: 150px;font-size:13px;"><%=localAuthTypes.toString()%></select><a id="claimMappingAddLinkss" onclick="addLocalRow(this,'+stepOrder+');return false;" class="icon-link" style="background-image:url(images/add.gif);"></a></td></tr></thead></table> </div><%if (federatedIdPs!=null && federatedIdPs.length>0) { %> <h2 id="fed_auth_head_'+stepOrder+'" class="sectionSeperator trigger active" style="background-color: floralwhite;"><a href="#">Federated Authenticators</a></h2><div class="toggle_container sectionSub" style="margin-bottom:10px;" id="fed_auth_head_dev_'+stepOrder+'"><table class="styledLeft" width="100%" id="local_auth_table"><thead> <tr><td><select name="idpAuthType_"'+stepOrder+'" style="float: left; min-width: 150px;font-size:13px;"><%=idpType.toString()%></select><a id="claimMappingAddLinkss" onclick="addIDPRow(this,'+stepOrder+');return false;" class="icon-link" style="background-image:url(images/add.gif);"></a></td></tr></thead></table></div><%}%></div>'));
        	if($(jQuery('#stepsConfRow tr')).length == 2){
                $(jQuery('#stepsConfRow')).toggle();
            }
        })
       
    })
     
    var deletePermissionRows = [];
    function deletePermissionRow(obj){
        if(jQuery(obj).parent().prev().children()[0].value != ''){
        	deletePermissionRows.push(jQuery(obj).parent().prev().children()[0].value);
        }
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
    
    function deleteStepRow(obj){
    	stepOrder--;
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
    
    function deleteIDPRow(obj){
    	idpNumber--;
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
    
    function deleteStep(obj){
    	idpNumber--;
        jQuery(obj).parent().next().remove();
        jQuery(obj).parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
    
    
    
    function deleteLocalAuthRow(obj){
    	localAuthNumber--;
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#permissionAddTable tr')).length == 1){
            $(jQuery('#permissionAddTable')).toggle();
        }
    }
    
    
    function addLocalRow(obj,stepId) {
    	//var stepId = jQuery(obj).parent().children()[0].value;
    	var selectedObj = jQuery(obj).prev().find(":selected");
		var selectedAuthenticatorName =selectedObj.val(); 
		jQuery(obj)
				.parent()
				.parent()
				.parent()
				.parent()
				.append(
						jQuery('<tr><td><input name="step_'+ stepId +'_local_auth" id="" type="hidden" value="' + selectedAuthenticatorName + '" />'+selectedAuthenticatorName +'</td><td class="leftCol-small" ><a onclick="deleteLocalAuthRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a></td></tr>'));	}	
	
    
    
	function addIDPRow(obj, stepID) {
		var selectedObj = jQuery(obj).prev().find(":selected");

		var selectedIDPName = selectedObj.val(); 
		//var stepID = jQuery(obj).parent().children()[1].value;
		var dataArray =  selectedObj.attr('data').split(',');
		var newRow = '<tr><td><input name="step_'+ stepID +'_fed_auth" id="" type="hidden" value="' + selectedIDPName + '" />' + selectedIDPName + ' </td><td> <select name="step_'+ stepID +'_idp_'+selectedIDPName+'_fed_authenticator" style="float: left; min-width: 150px;font-size:13px;">';
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
						jQuery(newRow));	}	
	
</script>

<fmt:bundle basename="org.wso2.carbon.identity.application.mgt.ui.i18n.Resources">
    <div id="middle">
        <h2>
            <fmt:message key='breadcrumb.advanced.auth.step.config.for'/><%=spName%>
        </h2>
        <div id="workArea">
            <form id="configure-auth-flow-form" method="post" name="configure-auth-flow-form" method="post" action="configure-authentication-flow-finish.jsp" >        
           
          
           
            <h2 id="authentication_step_config_head" class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="title.config.authentication.steps"/></a>
            </h2>
            
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="stepsConfRow">
            <table>
            <tr>
              <td><a id="stepsAddLink" class="icon-link" style="background-image:url(images/add.gif);margin-left:0"><fmt:message key='button.add.step'/></a></td>        
            </tr>
            </table>
             

							<%
								if(steps != null && steps.length>0) {
										for (AuthenticationStep step : steps) {
							%>
							
							<h2 id="step_head_<%=step.getStepOrder()%>" class="sectionSeperator trigger active" style="background-color: beige;">
							       <a href="#">Step <%=step.getStepOrder()%></a></h2>
							       
							<div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="step_dev-<%=step.getStepOrder()%>">
							
							           <h2 id="local_auth_head_<%=step.getStepOrder()%>" class="sectionSeperator trigger active" style="background-color: floralwhite;">
							                <a href="#">Local Authenticators</a>
							           </h2>
							      <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="local_auth_head_dev_<%=step.getStepOrder()%>">
							                <table class="styledLeft" width="100%" id="local_auth_table-<%=step.getStepOrder()%>">	             
							                <thead>
							             	<tr>
							             		<td>
							             		    <input type="hidden" value="<%=step.getStepOrder()%>" name="auth_step" id="auth_step" />
							             		    
							             			<select name="step_<%=step.getStepOrder()%>_local_oauth_select"  style="float: left; min-width: 150px;font-size:13px;">
							             				<%=localAuthTypes.toString()%>
							             			</select>
							             			<a id="claimMappingAddLinkss" onclick="addLocalRow(this,'<%=step.getStepOrder()%>');return false;" class="icon-link" style="background-image:url(images/add.gif);">
							             			</a>
							             		</td>
							             	</tr>
							             	</thead>
							             	<%LocalAuthenticator[] lclAuthenticators = step.getLocalAuthenticators();
							             	
							             	if (lclAuthenticators!=null && lclAuthenticators.length>0 ) {
							             		int i = 0;
							             		for(LocalAuthenticator lclAuthenticator : lclAuthenticators) {
							             			if (lclAuthenticator!=null) {
							             		%>
							             		
							             		<tr>
							             	        <td>
							             	        	<input name="step_<%=step.getStepOrder()%>_local_auth" id="" type="hidden" value="<%=lclAuthenticator.getName()%>" />
							             	        		<%=lclAuthenticator.getName()%>
							             	        </td>
							             	        <td class="leftCol-small" >
							             	            <a onclick="deleteLocalAuthRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a>
							             	        </td>
							             	    </tr>		
							                <%		
							             			}
							                     }
							             	}
							             	%>							              
							             </table>
							      </div>
							      
							      <%if (federatedIdPs!=null && federatedIdPs.length>0) { %> 
							      <h2 id="fed_auth_head_<%=step.getStepOrder()%>" class="sectionSeperator trigger active" style="background-color: floralwhite;">
							             <a href="#">Federated Authenticators</a>
							      </h2>
							      						    							      
							      <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="fed_auth_head_dev_<%=step.getStepOrder()%>">
							             <table class="styledLeft" width="100%" id="fed_auth_table">
							             	<thead> 
							             	 <tr>
							             	  <td>
							             	  	<input type="hidden" value="<%=step.getStepOrder()%>" id="auth_step">
							             	  
							             		<select name="idpAuthType_<%=step.getStepOrder()%>" style="float: left; min-width: 150px;font-size:13px;">
							             			<%=idpType.toString()%>
							             		</select>
							             		<a id="claimMappingAddLinkss" onclick="addIDPRow(this,'<%=step.getStepOrder()%>');return false;" class="icon-link" style="background-image:url(images/add.gif);"></a>
							             	  </td>
							                 </tr>
							                </thead>
							                 
							              <%
							      
							      	       FederatedIdentityProvider[] fedIdps = step.getFederatedIdentityProviders();
							      			if (fedIdps!=null && fedIdps.length>0){
							      			int j = 0;
							      			for(FederatedIdentityProvider idp:fedIdps) {
							      				if (idp!=null) {
							              %>
							      
							      	       <tr>
							      	      	   <td>
							      	      		<input name="step_<%=step.getStepOrder()%>_fed_auth" id="" type="hidden" value="<%=idp.getIdentityProviderName()%>" />
							      	      			<%=idp.getIdentityProviderName()%>
							      	      		</td>
							      	      		<td> 
							      	      			<select   name="step_<%=step.getStepOrder()%>_idp_<%=idp.getIdentityProviderName()%>_fed_authenticator" style="float: left; min-width: 150px;font-size:13px;"><%=stepIdpAuthenticators.get(step.getStepOrder() +"_"+ idp.getIdentityProviderName())%></select>
							      	      		</td>
							      	      		<td class="leftCol-small" >
							      	      		<a onclick="deleteIDPRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete </a>
							      	      		</td>
							      	      </tr>						      
							              <%
							      				    }							      			
							      	             }								      	
							      	         }
							              %>
							             </table>
							       </div>
							       <% } %>
							      				
							  </div>
							  							     							      							       
							       <%  }} %>
   
            </div>
			<div style="clear:both"></div>
            <!-- sectionSub Div -->
            <div class="buttonRow">
                <input type="button" value="<fmt:message key='button.update.service.provider'/>" onclick="createAppOnclick();"/>
                <input type="button" value="<fmt:message key='button.cancel'/>" onclick="javascript:location.href='configure-service-provider.jsp?display=auth_config'"/>
            </div>
            </form>
        </div>
    </div>

</fmt:bundle>