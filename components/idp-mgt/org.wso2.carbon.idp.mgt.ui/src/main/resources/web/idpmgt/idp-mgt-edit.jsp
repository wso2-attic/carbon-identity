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

<%@page import="com.sun.org.apache.bcel.internal.generic.INSTANCEOF"%>
<%@page import="org.wso2.carbon.identity.application.common.model.idp.xsd.*"%>

<%@page import="org.wso2.carbon.identity.application.common.model.CertData"%>
<%@page import="org.wso2.carbon.identity.application.common.IdentityApplicationManagementUtil"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"%>
<%@ page import="org.wso2.carbon.idp.mgt.ui.util.IdPManagementUIUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.idp.mgt.ui.client.IdentityProviderMgtServiceClient" %>

<carbon:breadcrumb label="identity.providers" resourceBundle="org.wso2.carbon.idp.mgt.ui.i18n.Resources"
                    topPage="true" request="<%=request%>" />
<jsp:include page="../dialog/display_messages.jsp"/>

<script type="text/javascript" src="../admin/js/main.js"></script>

<%
	String idPName = request.getParameter("idPName");
    if(idPName != null && idPName.equals("")){
        idPName = null;
    }
    boolean primary = false;
    String realmId = null;
    CertData certData = null;
    Claim[] identityProviderClaims = null;
    String userIdClaimURI = null;
    String roleClaimURI = null;
    String provisioningUserStoreIdClaimURI = null;
    ClaimMapping[] claimMappings = null;
    String[] roles = null;
    RoleMapping[] roleMappings = null;
    String tokenEndpointAlias = null;
    boolean isProvisioningEnabled = false;
    String provisioningUserStoreId = null;
    boolean isOpenIdEnabled = false;
    boolean isOpenIdDefault = false;
    String openIdUrl = null;
    boolean isOpenIdUserIdInClaims = false;
    boolean isSAML2SSOEnabled = false;
    boolean isSAMLSSODefault = false;
    String idPEntityId = null;
    String spEntityId = null;
    String ssoUrl = null;
    boolean isAuthnRequestSigned = false;
    boolean isSLOEnabled = false;
    boolean isLogoutRequestSigned = false;
    String logoutUrl = null;
    boolean isAuthnResponseSigned = false;
    boolean isSAMLSSOUserIdInClaims = false;
    boolean isOIDCEnabled = false;
    boolean isOIDCDefault = false;
    String clientId = null;
    String clientSecret = null;
    String authzUrl = null;
    String tokenUrl = null;
    boolean isOIDCUserIdInClaims = false;
    boolean isPassiveSTSEnabled = false;
    boolean isPassiveSTSDefault = false;
    String passiveSTSRealm = null;
    String passiveSTSUrl = null;
    boolean isPassiveSTSUserIdInClaims = false;
    String[] userStoreDomains = null;
	boolean isFBAuthEnabled = false;
    boolean isFBAuthDefault = false;
    String fbClientId = null;
    String fbClientSecret = null;
    boolean isFBUserIdInClaims = false;
    
    // Claims
    String[] claimUris = new String[0];

    // Provisioning
    boolean isGoogleProvEnabled = false;
    boolean isGoogleProvDefault = false;
    String googleDomainName = null;
    String googleUserIDClaim = null;
    String googleUserIDDefaultValue = null;
    String googleFamilyNameClaim = null;
    String googleFamilyNameDefaultValue = null;
    String googleGivenNameClaim = null;
    String googleGivenNameDefaultValue = null;
    String googlePasswordClaim = null;
    String googlePasswordDefaultValue = null;
    String googlePrimaryEmailClaim = null;
    String googlePrimaryEmailDefaultValue = null;
    String googleProvServiceAccEmail = null;
    String googleProvAdminEmail = null;
    String googleProvApplicationName = null;
    CertData googleProvPrivateKeyData = null;
    String[] idpClaims = new String[]{"admin", "Internal/everyone"};//appBean.getSystemClaims();

    FederatedIdentityProvider identityProvider = (FederatedIdentityProvider)session.getAttribute("identityProvider");
    List<FederatedIdentityProvider> identityProvidersList = (List<FederatedIdentityProvider>)session.getAttribute("identityProviderList");
    if(identityProvidersList == null){
%>
        <script type="text/javascript">
            location.href = "idp-mgt-list-load.jsp?callback=idp-mgt-edit.jsp";
        </script>
    <%
    	return;
        }
        if(idPName != null && identityProvider != null){
            idPName = identityProvider.getIdentityProviderName();
            primary = identityProvider.getPrimary();
            realmId = identityProvider.getHomeRealmId();
            if(identityProvider.getCertificate() != null){
                certData = IdentityApplicationManagementUtil.getCertData(identityProvider.getCertificate());
            }
            
            identityProviderClaims = identityProvider.getClaimConfiguration().getIdpClaims();
            
            userIdClaimURI = identityProvider.getClaimConfiguration().getUserClaimURI();
            roleClaimURI = identityProvider.getClaimConfiguration().getRoleClaimURI();
            provisioningUserStoreIdClaimURI = identityProvider.getJustInTimeProvisioningConfiguration().getUserStoreClaimUri();

            claimMappings = identityProvider.getClaimConfiguration().getClaimMappings();
            roles = identityProvider.getPermissionAndRoleConfiguration().getIdpRoles();
            roleMappings = identityProvider.getPermissionAndRoleConfiguration().getRoleMappings();
            
            FederatedAuthenticator[] federatedAuthenticators = identityProvider.getFederatedAuthenticators();
            
            OpenIDFederatedAuthenticator openidAuthenticator = new OpenIDFederatedAuthenticator();
            FacebookFederatedAuthenticator facebookAuthenticator = new FacebookFederatedAuthenticator();
            PassiveSTSFederatedAuthenticator passiveSTS = new PassiveSTSFederatedAuthenticator();
            OpenIDConnectFederatedAuthenticator oidc = new OpenIDConnectFederatedAuthenticator();
            SAMLFederatedAuthenticator saml = new SAMLFederatedAuthenticator();
            
            if (federatedAuthenticators!= null && federatedAuthenticators.length>0){
            	for(FederatedAuthenticator federatedAuthenticator: federatedAuthenticators){
            		if (federatedAuthenticator instanceof OpenIDFederatedAuthenticator){
            			openidAuthenticator = (OpenIDFederatedAuthenticator)federatedAuthenticator;
            		}else if(federatedAuthenticator instanceof FacebookFederatedAuthenticator){
            			facebookAuthenticator = (FacebookFederatedAuthenticator)federatedAuthenticator;
            		}else if(federatedAuthenticator instanceof PassiveSTSFederatedAuthenticator){
            			passiveSTS = (PassiveSTSFederatedAuthenticator)federatedAuthenticator;
            		}else if(federatedAuthenticator instanceof OpenIDConnectFederatedAuthenticator){
            			oidc = (OpenIDConnectFederatedAuthenticator)federatedAuthenticator;
            		}else if(federatedAuthenticator instanceof SAMLFederatedAuthenticator){
            			saml = (SAMLFederatedAuthenticator)federatedAuthenticator;
            		}             		
            	}
            }
            
            
            tokenEndpointAlias = identityProvider.getAlias();
            isProvisioningEnabled = identityProvider.getJustInTimeProvisioningConfiguration().getProvisioningEnabled();
            provisioningUserStoreId = identityProvider.getJustInTimeProvisioningConfiguration().getProvisioningUserStore();
           
            isOpenIdEnabled = openidAuthenticator.getEnabled();
            
            if (identityProvider.getDefaultAuthenticator()!=null 
            		&& identityProvider.getDefaultAuthenticator().getName()!=null) {
            	isOpenIdDefault = identityProvider.getDefaultAuthenticator().getName().equals(openidAuthenticator.getName());
            }else{
            	isOpenIdDefault = false;
            }
            
            openIdUrl = openidAuthenticator.getOpenIDServerUrl();
            isOpenIdUserIdInClaims = openidAuthenticator.getUsetIdInClaim();
            
            
            isSAML2SSOEnabled = saml.getEnabled();
            
            if (identityProvider.getDefaultAuthenticator()!=null 
            		&& identityProvider.getDefaultAuthenticator().getName()!=null) {
            	isSAMLSSODefault = identityProvider.getDefaultAuthenticator().getName().equals(saml.getName());
            }
            
            idPEntityId = saml.getIdpEntityId();
            spEntityId = saml.getSpEntityId();
            ssoUrl = saml.getSaml2SSOUrl();
            isAuthnRequestSigned = saml.getAuthnRequestSigned();
            isSLOEnabled = saml.getLogoutEnabled();
            logoutUrl = saml.getLogoutRequestUrl();
            isLogoutRequestSigned = saml.getLogoutRequestSigned();
            isAuthnResponseSigned = saml.getAuthnResponseSigned();
            isSAMLSSOUserIdInClaims = saml.getUsetIdInClaim();
            
            isOIDCEnabled = oidc.getEnabled();
            
            if (identityProvider.getDefaultAuthenticator()!=null 
            		&& identityProvider.getDefaultAuthenticator().getName()!=null) {
            	isOIDCDefault = identityProvider.getDefaultAuthenticator().getName().equals(oidc.getName());
            }
            
            authzUrl = oidc.getAuthzEndpointUrl();
            tokenUrl = oidc.getTokenEndpointUrl();
            clientId = oidc.getClientId();
            clientSecret = oidc.getClientSecret();
            isOIDCUserIdInClaims = oidc.isUsetIdInClaimSpecified();
            
            isPassiveSTSEnabled = passiveSTS.getEnabled();
            
            if (identityProvider.getDefaultAuthenticator()!=null 
            		&& identityProvider.getDefaultAuthenticator().getName()!=null) {
            	isPassiveSTSDefault = identityProvider.getDefaultAuthenticator().getName().equals(passiveSTS.getName());
            }
            
            passiveSTSRealm = passiveSTS.getPassiveSTSRealm();
            passiveSTSUrl = passiveSTS.getPassiveSTSUrl();
            isPassiveSTSUserIdInClaims = passiveSTS.getUsetIdInClaim();
            
    		isFBAuthEnabled = facebookAuthenticator.getEnabled();
    		
    		if (identityProvider.getDefaultAuthenticator()!=null 
            		&& identityProvider.getDefaultAuthenticator().getName()!=null) {
            	isFBAuthDefault = identityProvider.getDefaultAuthenticator().getName().equals(facebookAuthenticator.getName());
    		}
            
            fbClientId = facebookAuthenticator.getClientId();
            fbClientSecret = facebookAuthenticator.getClientSecret();
            isFBUserIdInClaims = facebookAuthenticator.getUsetIdInClaim();
            
            // Provisioning
            isGoogleProvEnabled = false;
            isGoogleProvDefault = false;
            googleDomainName = "";
            googleUserIDClaim = "";
            googleUserIDDefaultValue = "";
            googleFamilyNameClaim = "";
            googleFamilyNameDefaultValue = "";
            googleGivenNameClaim = "";
            googleGivenNameDefaultValue = "";
            googlePasswordClaim = "";
            googlePasswordDefaultValue = "";
            googlePrimaryEmailClaim = "";
            googlePrimaryEmailDefaultValue = "";
            googleProvServiceAccEmail = "";
            googleProvAdminEmail = "";
            googleProvApplicationName = "";
            //if(identityProvider.getCertificate() != null){
            //    googleProvPrivateKeyData = IdPMgtUtil.getCertData(identityProvider.getCertificate());
            //}
            //idpClaims = identityProvider.getSystemClaims();
        }
        if(idPName == null){
            idPName = "";
        }
        String primaryDisabled = "", primaryChecked = "";
        if(identityProvider != null){
            if(primary){
                primaryChecked = "checked=\'checked\'";
                primaryDisabled = "disabled=\'disabled\'";
            }
        } else {
            if(identityProvidersList.size() > 0){
                if(primary){
                    primaryDisabled = "disabled=\'disabled\'";
                    primaryChecked = "checked=\'checked\'";
                }
            } else {
                primaryDisabled = "disabled=\'disabled\'";
                primaryChecked = "checked=\'checked\'";
            }
        }
        if(realmId == null){
            realmId = "";
        }
        if(tokenEndpointAlias == null){
            tokenEndpointAlias = IdPManagementUIUtil.getOAuth2TokenEPURL(request);;
        }
        String provisionStaticDropdownDisabled = "";
        String provisionDynamicDropdownDisabled = "";
        if(!isProvisioningEnabled){
            provisionStaticDropdownDisabled = "disabled=\'disabled\'";
            provisionDynamicDropdownDisabled = "disabled=\'disabled\'";
        } else if(isProvisioningEnabled && provisioningUserStoreId != null){
            provisionDynamicDropdownDisabled = "disabled=\'disabled\'";
        } else if(isProvisioningEnabled && provisioningUserStoreId == null){
            provisionStaticDropdownDisabled = "disabled=\'disabled\'";
        }
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        IdentityProviderMgtServiceClient client = new IdentityProviderMgtServiceClient(cookie, backendServerURL, configContext);
        userStoreDomains = client.getUserStoreDomains();
        
        claimUris = client.getAllLocalClaimUris();
        
        String openIdEnabledChecked = "";
        String openIdDefaultDisabled = "";
        if(identityProvider != null){
            if(isOpenIdEnabled){
                openIdEnabledChecked = "checked=\'checked\'";
            } else {
                openIdDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        String openIdDefaultChecked = "";

        if(identityProvider != null){
            if(isOpenIdDefault){
                openIdDefaultChecked = "checked=\'checked\'";
                openIdDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        if(openIdUrl == null){
            openIdUrl = IdPManagementUIUtil.getOpenIDUrl(request);
        }
        String saml2SSOEnabledChecked = "";
        String saml2SSODefaultDisabled = "";
        if(identityProvider != null){
            if(isSAML2SSOEnabled){
                saml2SSOEnabledChecked = "checked=\'checked\'";
            } else {
                saml2SSODefaultDisabled = "disabled=\'disabled\'";
            }
        }
        String saml2SSODefaultChecked = "";
        if(identityProvider != null){
            if(isSAMLSSODefault){
                saml2SSODefaultChecked = "checked=\'checked\'";
                saml2SSODefaultDisabled = "disabled=\'disabled\'";
            }
        }
        if(idPEntityId == null){
            idPEntityId = "";
        }
        if(spEntityId == null){
            spEntityId = "";
        }
        if(ssoUrl == null) {
            ssoUrl = IdPManagementUIUtil.getSAML2SSOUrl(request);
        }
        String authnRequestSignedChecked = "";
        if(identityProvider != null){
            if(isAuthnRequestSigned){
                authnRequestSignedChecked = "checked=\'checked\'";
            }
        }
        String sloEnabledChecked = "";
        if(identityProvider != null){
            if(isSLOEnabled){
                sloEnabledChecked = "checked=\'checked\'";
            }
        }
        if(logoutUrl == null) {
            logoutUrl = "";
        }
        String logoutRequestSignedChecked = "";
        if(identityProvider != null){
            if(isLogoutRequestSigned){
                logoutRequestSignedChecked = "checked=\'checked\'";
            }
        }
        String authnResponseSignedChecked = "";
        if(identityProvider != null){
            if(isAuthnResponseSigned){
                authnResponseSignedChecked = "checked=\'checked\'";
            }
        }
        String oidcEnabledChecked = "";
        String oidcDefaultDisabled = "";
        if(identityProvider != null){
            if(isOIDCEnabled){
                oidcEnabledChecked = "checked=\'checked\'";
            } else {
                oidcDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        String oidcDefaultChecked = "";

        if(identityProvider != null){
            if(isOIDCDefault){
                oidcDefaultChecked = "checked=\'checked\'";
                oidcDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        if(authzUrl == null){
            authzUrl = IdPManagementUIUtil.getOAuth2AuthzEPURL(request);
        }
        if(tokenUrl == null){
            tokenUrl = IdPManagementUIUtil.getOAuth2TokenEPURL(request);
        }
        if(clientId == null){
            clientId = "";
        }
        if(clientSecret == null){
            clientSecret = "";
        }
        String passiveSTSEnabledChecked = "";
        String passiveSTSDefaultDisabled = "";
        if(identityProvider != null){
            if(isPassiveSTSEnabled){
                passiveSTSEnabledChecked = "checked=\'checked\'";
            } else {
                passiveSTSDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        String passiveSTSDefaultChecked = "";
        if(identityProvider != null){
            if(isPassiveSTSDefault){
                passiveSTSDefaultChecked = "checked=\'checked\'";
                passiveSTSDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        if(passiveSTSRealm == null){
            passiveSTSRealm = "";
        }
        if(passiveSTSUrl == null){
            passiveSTSUrl = IdPManagementUIUtil.getPassiveSTSURL(request);
        }
    	String fbAuthEnabledChecked = "";
        String fbAuthDefaultDisabled = "";
        if(identityProvider != null){
            if(isFBAuthEnabled){
            	fbAuthEnabledChecked = "checked=\'checked\'";
            } else {
                fbAuthDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        String fbAuthDefaultChecked = "";
        if(identityProvider != null){
            if(isFBAuthDefault){
            	fbAuthDefaultChecked = "checked=\'checked\'";
                fbAuthDefaultDisabled = "disabled=\'disabled\'";
            }
        }
        if(fbClientId == null){
        	fbClientId = "";
        }
        if(fbClientSecret == null){
        	fbClientSecret = "";
        }  
        String fbUserIdInClaims = "";
        if(identityProvider != null){
            if(isFBUserIdInClaims){
            	fbUserIdInClaims = "checked=\'checked\'";
            }
        }
        if(openIdEnabledChecked.equals("") && saml2SSOEnabledChecked.equals("") && oidcEnabledChecked.equals("") &&
                passiveSTSEnabledChecked.equals("") && fbAuthEnabledChecked.equals("")){
            openIdEnabledChecked = "checked=\'checked\'";
        }
        if(openIdDefaultChecked.equals("") && saml2SSODefaultChecked.equals("") && oidcDefaultChecked.equals("") &&
                passiveSTSDefaultChecked.equals("") && fbAuthDefaultChecked.equals("")){
            openIdDefaultChecked = "checked=\'checked\'";
            openIdDefaultDisabled = "disabled=\'disabled\'";
        }
        
        // Out-bound Provisioning    
        String googleProvEnabledChecked = "";
        String googleProvDefaultDisabled = "";
        String googleProvDefaultChecked = "disabled=\'disabled\'";
        if(identityProvider != null){
            if(isGoogleProvEnabled){
                googleProvEnabledChecked = "checked=\'checked\'";
                googleProvDefaultChecked = "";
                if(isGoogleProvDefault){
                    googleProvDefaultChecked = "checked=\'checked\'";
                }
            }
        }
        
        if(googleDomainName == null){
            googleDomainName = "";
        }
        if(googleUserIDClaim == null){
            googleUserIDClaim = "";
        }
        if(googleUserIDDefaultValue == null){
            googleUserIDDefaultValue = "";
        }
        if(googlePrimaryEmailClaim == null){
            googlePrimaryEmailClaim = "";
        }
        if(googlePrimaryEmailDefaultValue == null){
            googlePrimaryEmailDefaultValue = "";
        }
        if(googlePasswordClaim == null){
            googlePasswordClaim = "";
        }
        if(googlePasswordDefaultValue == null){
            googlePasswordDefaultValue = "";
        }
        if(googleGivenNameDefaultValue == null){
            googleGivenNameDefaultValue = "";
        }
        if(googleFamilyNameClaim == null){
            googleFamilyNameClaim = "";
        }
        if(googleFamilyNameDefaultValue == null){
            googleFamilyNameDefaultValue = "";
        }
        if(googleProvServiceAccEmail == null){
            googleProvServiceAccEmail = "";
        }
        if(googleProvAdminEmail== null){
            googleProvAdminEmail = "";
        }
        if(googleProvApplicationName == null){
            googleProvApplicationName = "";
        }
    %>

<script>

    var claimMappinRowID = -1;
    var roleRowId = -1;
    
    <% if(identityProviderClaims != null){ %>
    var claimRowId = <%=identityProviderClaims.length-1%>;
    <% } else { %>
    var claimRowId = -1;
    <% } %>
    <% if(roles != null){ %>
        roleRowId = <%=roles.length-1%>;
    <% } %>

    var claimURIDropdownPopulator = function(){
        var $user_id_claim_dropdown = jQuery('#user_id_claim_dropdown');
        var $role_claim_dropdown = jQuery('#role_claim_dropdown');
        var $provision_dynamic_dropdown = jQuery('#provision_dynamic_dropdown');

        $user_id_claim_dropdown.empty();
        $role_claim_dropdown.empty();
        $provision_dynamic_dropdown.empty();

        if('<%=userIdClaimURI%>' == ''){
            $user_id_claim_dropdown.append('<option>--- Select Claim URI ---</option>');
        } else {
            $user_id_claim_dropdown.append('<option selected="selected">--- Select Claim URI ---</option>');
        }

        if('<%=roleClaimURI%>' == ''){
            $role_claim_dropdown.append('<option>--- Select Claim URI ---</option>');
        } else {
            $role_claim_dropdown.append('<option selected="selected">--- Select Claim URI ---</option>');
        }

        if('<%=provisioningUserStoreIdClaimURI%>' == ''){
            $provision_dynamic_dropdown.append('<option>--- Select Claim URI ---</option>');
        } else {
            $provision_dynamic_dropdown.append('<option selected="selected">--- Select Claim URI ---</option>');
        }

        jQuery('#claimAddTable .claimrow').each(function(){
            if($(this).val().trim() != ""){
                var val =  $(this).val();
                if(val == '<%=userIdClaimURI%>'){
                    $user_id_claim_dropdown.append('<option selected="selected">'+val+'</option>');
                } else {
                    $user_id_claim_dropdown.append('<option>'+val+'</option>');
                }
                if(val == '<%=roleClaimURI%>'){
                    $role_claim_dropdown.append('<option selected="selected">'+val+'</option>');
                } else {
                    $role_claim_dropdown.append('<option>'+val+'</option>');
                }
                if(val == '<%=provisioningUserStoreIdClaimURI%>'){
                    $provision_dynamic_dropdown.append('<option selected="selected">'+val+'</option>');
                } else {
                    $provision_dynamic_dropdown.append('<option>'+val+'</option>');
                }
            }
        })
    };
    
    function deleteRow(obj){
        jQuery(obj).parent().parent().remove();

     }
   

    jQuery(document).ready(function(){
    	 jQuery('#outBoundAuth').hide();
    	 jQuery('#inBoundProvisioning').hide();
    	 jQuery('#outBoundProvisioning').hide();
    	 jQuery('#roleConfig').hide();
    	 jQuery('#claimConfig').hide(); 
        jQuery('#openIdLinkRow').hide();
        jQuery('#saml2SSOLinkRow').hide();
        jQuery('#oauth2LinkRow').hide();
        jQuery('#passiveSTSLinkRow').hide();
		jQuery('#fbAuthLinkRow').hide();
        jQuery('h2.trigger').click(function(){
            if (jQuery(this).next().is(":visible")) {
                this.className = "active trigger";
            } else {
                this.className = "trigger";
            }
            jQuery(this).next().slideToggle("fast");
            return false; //Prevent the browser jump to the link anchor
        })
        jQuery('#publicCertDeleteLink').click(function(){
            $(jQuery('#publicCertDiv')).toggle();
            var input = document.createElement('input');
            input.type = "hidden";
            input.name = "deletePublicCert";
            input.id = "deletePublicCert";
            input.value = "true";
            document.forms['idp-mgt-edit-form'].appendChild(input);
        })
        jQuery('#claimAddLink').click(function(){
            claimRowId++;
            var newrow = jQuery('<tr><td><input class="claimrow" type="text" id="claimrowid_'+claimRowId+'" name="claimrowname_'+claimRowId+'"/></td>' +
                    '<td><a onclick="deleteClaimRow(this)" class="icon-link" '+
                    'style="background-image: url(images/delete.gif)">'+
                    'Delete'+
                    '</a></td></tr>');
            jQuery('.claimrow',newrow).blur(function(){
                claimURIDropdownPopulator();
            });
            jQuery('#claimAddTable').append(newrow);;
            if($(jQuery('#claimAddTable tr')).length == 2){
                $(jQuery('#claimAddTable')).toggle();
            }

        })
        jQuery('#claimAddTable .claimrow').blur(function(){
            claimURIDropdownPopulator();
        });
        jQuery('#claimMappingDeleteLink').click(function(){
            $(jQuery('#claimMappingDiv')).toggle();
            var input = document.createElement('input');
            input.type = "hidden";
            input.name = "deleteClaimMappings";
            input.id = "deleteClaimMappings";
            input.value = "true";
            document.forms['idp-mgt-edit-form'].appendChild(input);
        })
        jQuery('#roleAddLink').click(function(){
            roleRowId++;
            jQuery('#roleAddTable').append(jQuery('<tr><td><input type="text" id="rolerowid_'+roleRowId+'" name="rolerowname_'+roleRowId+'"/></td>' +
                    '<td><a onclick="deleteRoleRow(this)" class="icon-link" '+
                    'style="background-image: url(images/delete.gif)">'+
                    'Delete'+
                    '</a></td></tr>'));
        })
        jQuery('#roleMappingDeleteLink').click(function(){
            $(jQuery('#roleMappingDiv')).toggle();
            var input = document.createElement('input');
            input.type = "hidden";
            input.name = "deleteRoleMappings";
            input.id = "deleteRoleMappings";
            input.value = "true";
            document.forms['idp-mgt-edit-form'].appendChild(input);
        })
        jQuery('#provision_disabled').click(function(){
            jQuery('#provision_static_dropdown').attr('disabled','disabled');
            jQuery('#provision_dynamic_dropdown').attr('disabled','disabled');
        });
        jQuery('#provision_static').click(function(){
            jQuery('#provision_static_dropdown').removeAttr('disabled');
            jQuery('#provision_dynamic_dropdown').attr('disabled','disabled');
        });
        jQuery('#provision_dynamic').click(function(){
            jQuery('#provision_dynamic_dropdown').removeAttr('disabled');
            jQuery('#provision_static_dropdown').attr('disabled','disabled');
        });
        jQuery('#claimMappingAddLink').click(function(){
        	var selectedIDPClaimName = $('select[name=idpClaimsList]').val();
        	claimMappinRowID++;
        	jQuery('#claimMappingAddTable').append(jQuery('<tr>'+
        	        '<td><input type="text" value="' + selectedIDPClaimName + '" id="idpClaim_'+ claimMappinRowID +'" name="idpClaim_'+ claimMappinRowID +'" readonly="readonly"/></td>' +
                    '<td><input type="text" id="spClaim_' + claimMappinRowID + '" name="spClaim_' + claimMappinRowID + '"/></td> '+
                    '<td><input type="text" id="def_val_' + claimMappinRowID + '" name="def_val_' + claimMappinRowID + '"/></td> '+
                    '<td><a onclick="deleteRow(this);return false;" href="#" class="icon-link" style="background-image: url(images/delete.gif)"> Delete</a></td>' + 

                    '</tr>'));

        });
        claimURIDropdownPopulator();
    })
    var deleteClaimRows = [];
    function deleteClaimRow(obj){
        if(jQuery(obj).parent().prev().children()[0].value != ''){
            deleteClaimRows.push(jQuery(obj).parent().prev().children()[0].value);
        }
        jQuery(obj).parent().parent().remove();
        if($(jQuery('#claimAddTable tr')).length == 1){
            $(jQuery('#claimAddTable')).toggle();
        }
        claimURIDropdownPopulator();
    }
    var deletedRoleRows = [];
    function deleteRoleRow(obj){
       if(jQuery(obj).parent().prev().children()[0].value != ''){
            deletedRoleRows.push(jQuery(obj).parent().prev().children()[0].value);
        }
        jQuery(obj).parent().parent().remove();

    }

    function checkEnabled(obj){

        if(jQuery(obj).attr('checked')) {
            if(jQuery(obj).attr('id') == 'openIdEnabled'){
                jQuery('#openIdDefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'saml2SSOEnabled'){
                jQuery('#saml2SSODefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'oidcEnabled'){
                jQuery('#oidcDefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'passiveSTSEnabled'){
                jQuery('#passiveSTSDefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'fbAuthEnabled'){
                jQuery('#fbAuthDefault').removeAttr('disabled');
            }
        } else {
            if(jQuery(obj).attr('id') == 'openIdEnabled'){
                if(!jQuery('#saml2SSOEnabled').attr('checked') &&
                        !jQuery('#oidcEnabled').attr('checked') &&
                        !jQuery('#passiveSTSEnabled').attr('checked') &&
                        !jQuery('#fbAuthEnabled').attr('checked')){
                    CARBON.showWarningDialog("At least one authentication protocol must be enabled. Enable another authentication protocol before disabling OpenID");
                    jQuery(obj).attr('checked','checked');
                } else if(jQuery('#openIdDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable OpenID authentication protocol since it is set as default. Change default protocol before disabling");
                    jQuery(obj).attr('checked','checked');
                }
                jQuery('#openIdDefault').attr('disabled','disabled');
            } else if(jQuery(obj).attr('id') == 'saml2SSOEnabled'){
                if(!jQuery('#openIdEnabled').attr('checked') &&
                        !jQuery('#oidcEnabled').attr('checked') &&
                        !jQuery('#passiveSTSEnabled').attr('checked') &&
                        !jQuery('#fbAuthEnabled').attr('checked')){
                    CARBON.showWarningDialog("At least one authentication protocol must be enabled. Enable another authentication protocol before disabling SAML2 Web SSO");
                    jQuery(obj).attr('checked','checked');
                } else if(jQuery('#saml2SSODefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable SAML2 Web SSO authentication protocol since it is set as default. Change default protocol before disabling");
                    jQuery(obj).attr('checked','checked');
                }
                jQuery('#saml2SSODefault').attr('disabled','disabled');
            } else if(jQuery(obj).attr('id') == 'oidcEnabled'){
                if(!jQuery('#openIdEnabled').attr('checked') &&
                        !jQuery('#saml2SSOEnabled').attr('checked') &&
                        !jQuery('#passiveSTSEnabled').attr('checked') &&
                        !jQuery('#fbAuthEnabled').attr('checked')){
                    CARBON.showWarningDialog("At least one authentication protocol must be enabled. Enable another authentication protocol before disabling OpenID Connect");
                    jQuery(obj).attr('checked','checked');
                } else if(jQuery('#oidcDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable OpenID Connect authentication protocol since it is set as default. Change default protocol before disabling");
                    jQuery(obj).attr('checked','checked');
                }
                jQuery('#oidcDefault').attr('disabled','disabled');
            } else if(jQuery(obj).attr('id') == 'passiveSTSEnabled'){
                if(!jQuery('#openIdEnabled').attr('checked') &&
                        !jQuery('#saml2SSOEnabled').attr('checked') &&
                        !jQuery('#oidcEnabled').attr('checked') &&
                        !jQuery('#fbAuthEnabled').attr('checked')){
                    CARBON.showWarningDialog("At least one authentication protocol must be enabled. Enable another authentication protocol before disabling Passive STS");
                    jQuery(obj).attr('checked','checked');
                } else if(jQuery('#passiveSTSDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable Passive STS authentication protocol since it is set as default. Change default protocol before disabling");
                    jQuery(obj).attr('checked','checked');
                }
                jQuery('#passiveSTSDefault').attr('disabled','disabled');
            } else if(jQuery(obj).attr('id') == 'fbAuthEnabled'){
                if(!jQuery('#openIdEnabled').attr('checked') &&
                        !jQuery('#saml2SSOEnabled').attr('checked') &&
                        !jQuery('#oidcEnabled').attr('checked') &&
                        !jQuery('#passiveSTSEnabled').attr('checked')){
                    CARBON.showWarningDialog("At least one authentication protocol must be enabled. Enable another authentication protocol before disabling FaceBook authentication");
                    jQuery(obj).attr('checked','checked');
                } else if(jQuery('#fbAuthDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable FaceBook authentication protocol since it is set as default. Change default protocol before disabling");
                    jQuery(obj).attr('checked','checked');
                }
                jQuery('#fbAuthDefault').attr('disabled','disabled');
            }
        }
    }

    function checkDefault(obj){
        if(jQuery(obj).attr('id') == 'openIdDefault'){
            jQuery('#saml2SSODefault').removeAttr('checked');
            jQuery('#oidcDefault').removeAttr('checked');
            jQuery('#passiveSTSDefault').removeAttr('checked');
            jQuery('#fbAuthDefault').removeAttr('checked');
            if(jQuery('#saml2SSOEnabled').attr('checked')){
                jQuery('#saml2SSODefault').removeAttr('disabled');
            }
            if(jQuery('#oidcEnabled').attr('checked')){
                jQuery('#oidcDefault').removeAttr('disabled');
            }
            if(jQuery('#passiveSTSEnabled').attr('checked')){
                jQuery('#passiveSTSDefault').removeAttr('disabled');
            }
            if(jQuery('#fbAuthEnabled').attr('checked')){
                jQuery('#fbAuthDefault').removeAttr('disabled');
            }
            jQuery('#openIdDefault').attr('disabled','disabled');
        } else if(jQuery(obj).attr('id') == 'saml2SSODefault'){
            jQuery('#openIdDefault').removeAttr('checked');
            jQuery('#oidcDefault').removeAttr('checked');
            jQuery('#passiveSTSDefault').removeAttr('checked');
            jQuery('#fbAuthDefault').removeAttr('checked');
            if(jQuery('#openIdEnabled').attr('checked')){
                jQuery('#openIdDefault').removeAttr('disabled');
            }
            if(jQuery('#oidcEnabled').attr('checked')){
                jQuery('#oidcDefault').removeAttr('disabled');
            }
            if(jQuery('#passiveSTSEnabled').attr('checked')){
                jQuery('#passiveSTSDefault').removeAttr('disabled');
            }
            if(jQuery('#fbAuthEnabled').attr('checked')){
                jQuery('#fbAuthDefault').removeAttr('disabled');
            }
            jQuery('#saml2SSODefault').attr('disabled','disabled');
        } else if(jQuery(obj).attr('id') == 'oidcDefault'){
            jQuery('#openIdDefault').removeAttr('checked');
            jQuery('#saml2SSODefault').removeAttr('checked');
            jQuery('#passiveSTSDefault').removeAttr('checked');
            jQuery('#fbAuthDefault').removeAttr('checked');
            if(jQuery('#openIdEnabled').attr('checked')){
                jQuery('#openIdDefault').removeAttr('disabled');
            }
            if(jQuery('#saml2SSOEnabled').attr('checked')){
                jQuery('#saml2SSODefault').removeAttr('disabled');
            }
            if(jQuery('#passiveSTSEnabled').attr('checked')){
                jQuery('#passiveSTSDefault').removeAttr('disabled');
            }
            if(jQuery('#fbAuthEnabled').attr('checked')){
                jQuery('#fbAuthDefault').removeAttr('disabled');
            }
            jQuery('#oidcDefault').attr('disabled','disabled');
        } else if(jQuery(obj).attr('id') == 'passiveSTSDefault'){
            jQuery('#openIdDefault').removeAttr('checked');
            jQuery('#saml2SSODefault').removeAttr('checked');
            jQuery('#oidcDefault').removeAttr('checked');
            jQuery('#fbAuthDefault').removeAttr('checked');
            if(jQuery('#openIdEnabled').attr('checked')){
                jQuery('#openIdDefault').removeAttr('disabled');
            }
            if(jQuery('#saml2SSOEnabled').attr('checked')){
                jQuery('#saml2SSODefault').removeAttr('disabled');
            }
            if(jQuery('#oidcEnabled').attr('checked')){
                jQuery('#oidcDefault').removeAttr('disabled');
            }
            if(jQuery('#fbAuthEnabled').attr('checked')){
                jQuery('#fbAuthDefault').removeAttr('disabled');
            }
            jQuery('#passiveSTSDefault').attr('disabled','disabled');
        } else if(jQuery(obj).attr('id') == 'fbAuthDefault'){
            jQuery('#openIdDefault').removeAttr('checked');
            jQuery('#saml2SSODefault').removeAttr('checked');
            jQuery('#oidcDefault').removeAttr('checked');
            jQuery('#passiveSTSDefault').removeAttr('checked');
            if(jQuery('#openIdEnabled').attr('checked')){
                jQuery('#openIdDefault').removeAttr('disabled');
            }
            if(jQuery('#saml2SSOEnabled').attr('checked')){
                jQuery('#saml2SSODefault').removeAttr('disabled');
            }
            if(jQuery('#oidcEnabled').attr('checked')){
                jQuery('#oidcDefault').removeAttr('disabled');
            }
            if(jQuery('#passiveSTSEnabled').attr('checked')){
                jQuery('#passiveSTSDefault').removeAttr('disabled');
            }
            jQuery('#fbAuthDefault').attr('disabled','disabled');
        }
    }

    function checkProvEnabled(obj){

        if(jQuery(obj).attr('checked')) {
            if(jQuery(obj).attr('id') == 'googleProvEnabled'){
                jQuery('#googleProvDefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'salesforceProvEnabled'){
                jQuery('#salesforceProvDefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'scimProvEnabled'){
                jQuery('#scimProvDefault').removeAttr('disabled');
            } else if(jQuery(obj).attr('id') == 'spmlProvEnabled'){
                jQuery('#spmlProvDefault').removeAttr('disabled');
            }
        } else {
            if(jQuery(obj).attr('id') == 'googleProvEnabled'){
                if(jQuery('#googleProvDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable default provisioning connector.");
                    jQuery(obj).attr('checked','checked');
                }
                else {
                    jQuery('#googleProvDefault').attr('disabled','disabled');
                }
            } else if(jQuery(obj).attr('id') == 'salesforceProvEnabled'){
                if(jQuery('#salesforceProvDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable default provisioning connector.");
                    jQuery(obj).attr('checked','checked');
                }
                else {
                    jQuery('#salesforceProvDefault').attr('disabled','disabled');
                }
            } else if(jQuery(obj).attr('id') == 'scimProvEnabled'){
                if(jQuery('#scimProvDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable default provisioning connector.");
                    jQuery(obj).attr('checked','checked');
                }
                else {
                    jQuery('#scimProvDefault').attr('disabled','disabled');
                }
            } else if(jQuery(obj).attr('id') == 'spmlProvEnabled'){
                if(jQuery('#spmlProvDefault').attr('checked')){
                    CARBON.showWarningDialog("Cannot disable default provisioning connector.");
                    jQuery(obj).attr('checked','checked');
                }
                else {
                    jQuery('#spmlProvDefault').attr('disabled','disabled');
                }
            }
        }
    }

    function checkProvDefault(obj){
    }

    function idpMgtUpdate(){
        if(doValidation()){
            var allDeletedClaimStr = "";
            for(var i = 0;i<deleteClaimRows.length;i++){
                if(i < deleteClaimRows.length-1){
                    allDeletedClaimStr += deleteClaimRows[i] + ", ";
                } else {
                    allDeletedClaimStr += deleteClaimRows[i] + "?";
                }
            }
            var allDeletedRoleStr = "";
            for(var i = 0;i<deletedRoleRows.length;i++){
                if(i < deletedRoleRows.length-1){
                    allDeletedRoleStr += deletedRoleRows[i] + ", ";
                } else {
                    allDeletedRoleStr += deletedRoleRows[i] + "?";
                }
            }

            if(jQuery('#deletePublicCert').val() == 'true'){
                var confirmationMessage = 'Are you sure you want to delete the public certificate of ' +
                        jQuery('#idPName').val() + '?';
                if(jQuery('#certFile').val() != ''){
                    confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                }
                CARBON.showConfirmationDialog(confirmationMessage,
                        function (){
                            if(allDeletedClaimStr != "") {
                                CARBON.showConfirmationDialog('Are you sure you want to delete the claim URI(s) ' +
                                        allDeletedClaimStr,
                                        function(){
                                            if(allDeletedRoleStr != "") {
                                                CARBON.showConfirmationDialog('Are you sure you want to delete the ' +
                                                        'role(s) ' + allDeletedRoleStr,
                                                        function(){
                                                            if(jQuery('#deleteClaimMappings').val() == 'true'){
                                                                var confirmationMessage = 'Are you sure you want to ' +
                                                                        'delete the Claim URI Mappings of ' +
                                                                        jQuery('#idPName').val() + '?';
                                                                if(jQuery('#claimMappingFile').val() != ''){
                                                                    confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                                }
                                                                CARBON.showConfirmationDialog(confirmationMessage,
                                                                        function(){
                                                                            if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                                                var confirmationMessage = 'Are you sure you want to ' +
                                                                                        'delete the Role Mappings of ' +
                                                                                        jQuery('#idPName').val() + '?';
                                                                                if(jQuery('#roleMappingFile').val() != ''){
                                                                                    confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                                                }
                                                                                CARBON.showConfirmationDialog(confirmationMessage,
                                                                                        function(){
                                                                                            doEditFinish();
                                                                                        },
                                                                                        function(){
                                                                                            location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                                        });
                                                                            } else {
                                                                                doEditFinish();
                                                                            }
                                                                        },
                                                                        function(){
                                                                            location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                        });
                                                            } else {
                                                                if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                                    var confirmationMessage = 'Are you sure you want to ' +
                                                                            'delete the Role Mappings of ' +
                                                                            jQuery('#idPName').val() + '?';
                                                                    if(jQuery('#roleMappingFile').val() != ''){
                                                                        confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                                    }
                                                                    CARBON.showConfirmationDialog(confirmationMessage,
                                                                            function(){
                                                                                doEditFinish();
                                                                            },
                                                                            function(){
                                                                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                            });
                                                                } else {
                                                                    doEditFinish();
                                                                }
                                                            }
                                                        },
                                                        function(){
                                                            location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                        });
                                            } else {
                                                if(jQuery('#deleteClaimMappings').val() == 'true'){
                                                    var confirmationMessage = 'Are you sure you want to ' +
                                                            'delete the Claim URI mappings of ' +
                                                            jQuery('#idPName').val() + '?';
                                                    if(jQuery('#claimMappingFile').val() != ''){
                                                        confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                    }
                                                    CARBON.showConfirmationDialog(confirmationMessage,
                                                            function(){

                                                            },
                                                            function(){
                                                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                            });
                                                } else {
                                                    if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                        var confirmationMessage = 'Are you sure you want to ' +
                                                                'delete the Role Mappings of ' +
                                                                jQuery('#idPName').val() + '?';
                                                        if(jQuery('#roleMappingFile').val() != ''){
                                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                        }
                                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                                function(){
                                                                    doEditFinish();
                                                                },
                                                                function(){
                                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                });
                                                    } else {
                                                        doEditFinish();
                                                    }
                                                }
                                            }
                                        },
                                        function(){
                                            location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                        });
                            } else {
                                if(allDeletedRoleStr != "") {
                                    CARBON.showConfirmationDialog('Are you sure you want to delete the ' +
                                            'role(s) ' + allDeletedRoleStr,
                                            function(){
                                                if(jQuery('#deleteClaimMappings').val() == 'true'){
                                                    var confirmationMessage = 'Are you sure you want to ' +
                                                            'delete the Claim URI mappings of ' +
                                                            jQuery('#idPName').val() + '?';
                                                    if(jQuery('#claimMappingFile').val() != ''){
                                                        confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                    }
                                                    CARBON.showConfirmationDialog(confirmationMessage,
                                                            function(){

                                                            },
                                                            function(){
                                                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                            });
                                                } else {
                                                    if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                        var confirmationMessage = 'Are you sure you want to ' +
                                                                'delete the Role Mappings of ' +
                                                                jQuery('#idPName').val() + '?';
                                                        if(jQuery('#roleMappingFile').val() != ''){
                                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                        }
                                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                                function(){
                                                                    doEditFinish();
                                                                },
                                                                function(){
                                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                });
                                                    } else {
                                                        doEditFinish();
                                                    }
                                                }
                                            },
                                            function(){
                                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                            });
                                } else {
                                    if(jQuery('#deleteClaimMappings').val() == 'true'){
                                        var confirmationMessage = 'Are you sure you want to ' +
                                                'delete the Claim URI mappings of ' +
                                                jQuery('#idPName').val() + '?';
                                        if(jQuery('#claimMappingFile').val() != ''){
                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                        }
                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                function(){
                                                    if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                        var confirmationMessage = 'Are you sure you want to ' +
                                                                'delete the Role Mappings of ' +
                                                                jQuery('#idPName').val() + '?';
                                                        if(jQuery('#roleMappingFile').val() != ''){
                                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                        }
                                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                                function(){
                                                                    doEditFinish();
                                                                },
                                                                function(){
                                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                });
                                                    } else {
                                                        doEditFinish();
                                                    }
                                                },
                                                function(){
                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                });
                                    } else {
                                        if(jQuery('#deleteRoleMappings').val() == 'true'){
                                            var confirmationMessage = 'Are you sure you want to ' +
                                                    'delete the Role Mappings of ' +
                                                    jQuery('#idPName').val() + '?';
                                            if(jQuery('#roleMappingFile').val() != ''){
                                                confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                            }
                                            CARBON.showConfirmationDialog(confirmationMessage,
                                                    function(){
                                                        doEditFinish();
                                                    },
                                                    function(){
                                                        location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                    });
                                        } else {
                                            doEditFinish();
                                        }
                                    }
                                }
                            }
                        },
                        function(){
                            location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                        });
            } else {
                if(allDeletedClaimStr != "") {
                    CARBON.showConfirmationDialog('Are you sure you want to delete the claim URI(s) ' +
                            allDeletedClaimStr,
                            function(){
                                if(allDeletedRoleStr != "") {
                                    CARBON.showConfirmationDialog('Are you sure you want to delete the ' +
                                            'role(s) ' + allDeletedRoleStr,
                                            function(){
                                                if(jQuery('#deleteClaimMappings').val() == 'true'){
                                                    var confirmationMessage = 'Are you sure you want to ' +
                                                            'delete the Claim URI mappings of ' +
                                                            jQuery('#idPName').val() + '?';
                                                    if(jQuery('#claimMappingFile').val() != ''){
                                                        confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                    }
                                                    CARBON.showConfirmationDialog(confirmationMessage,
                                                            function(){

                                                            },
                                                            function(){
                                                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                            });
                                                } else {
                                                    if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                        var confirmationMessage = 'Are you sure you want to ' +
                                                                'delete the Role Mappings of ' +
                                                                jQuery('#idPName').val() + '?';
                                                        if(jQuery('#roleMappingFile').val() != ''){
                                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                        }
                                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                                function(){
                                                                    doEditFinish();
                                                                },
                                                                function(){
                                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                });
                                                    } else {
                                                        doEditFinish();
                                                    }
                                                }
                                            },
                                            function(){
                                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                            });
                                } else {
                                    if(jQuery('#deleteClaimMappings').val() == 'true'){
                                        var confirmationMessage = 'Are you sure you want to ' +
                                                'delete the Claim URI mappings of ' +
                                                jQuery('#idPName').val() + '?';
                                        if(jQuery('#claimMappingFile').val() != ''){
                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                        }
                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                function(){
                                                    if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                        var confirmationMessage = 'Are you sure you want to ' +
                                                                'delete the Role Mappings of ' +
                                                                jQuery('#idPName').val() + '?';
                                                        if(jQuery('#roleMappingFile').val() != ''){
                                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                        }
                                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                                function(){
                                                                    doEditFinish();
                                                                },
                                                                function(){
                                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                });
                                                    } else {
                                                        doEditFinish();
                                                    }
                                                },
                                                function(){
                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                });
                                    } else {
                                        if(jQuery('#deleteRoleMappings').val() == 'true'){
                                            var confirmationMessage = 'Are you sure you want to ' +
                                                    'delete the Role Mappings of ' +
                                                    jQuery('#idPName').val() + '?';
                                            if(jQuery('#roleMappingFile').val() != ''){
                                                confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                            }
                                            CARBON.showConfirmationDialog(confirmationMessage,
                                                    function(){
                                                        doEditFinish();
                                                    },
                                                    function(){
                                                        location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                    });
                                        } else {
                                            doEditFinish();
                                        }
                                    }
                                }
                            },
                            function(){
                                location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                            });
                } else {
                    if(allDeletedRoleStr != "") {
                        CARBON.showConfirmationDialog('Are you sure you want to delete the ' +
                                'role(s) ' + allDeletedRoleStr,
                                function(){
                                    if(jQuery('#deleteClaimMappings').val() == 'true'){
                                        var confirmationMessage = 'Are you sure you want to ' +
                                                'delete the Claim URI mappings of ' +
                                                jQuery('#idPName').val() + '?';
                                        if(jQuery('#claimMappingFile').val() != ''){
                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                        }
                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                function(){
                                                    if(jQuery('#deleteRoleMappings').val() == 'true'){
                                                        var confirmationMessage = 'Are you sure you want to ' +
                                                                'delete the Role Mappings of ' +
                                                                jQuery('#idPName').val() + '?';
                                                        if(jQuery('#roleMappingFile').val() != ''){
                                                            confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                                        }
                                                        CARBON.showConfirmationDialog(confirmationMessage,
                                                                function(){
                                                                    doEditFinish();
                                                                },
                                                                function(){
                                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                                });
                                                    } else {
                                                        doEditFinish();
                                                    }
                                                },
                                                function(){
                                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                });
                                    } else {
                                        if(jQuery('#deleteRoleMappings').val() == 'true'){
                                            var confirmationMessage = 'Are you sure you want to ' +
                                                    'delete the Role Mappings of ' +
                                                    jQuery('#idPName').val() + '?';
                                            if(jQuery('#roleMappingFile').val() != ''){
                                                confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                            }
                                            CARBON.showConfirmationDialog(confirmationMessage,
                                                    function(){
                                                        doEditFinish();
                                                    },
                                                    function(){
                                                        location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                    });
                                        } else {
                                            doEditFinish();
                                        }
                                    }
                                },
                                function(){
                                    location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                });
                    } else {
                        if(jQuery('#deleteClaimMappings').val() == 'true'){
                            var confirmationMessage = 'Are you sure you want to ' +
                                    'delete the Claim URI mappings of ' +
                                    jQuery('#idPName').val() + '?';
                            if(jQuery('#claimMappingFile').val() != ''){
                                confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                            }
                            CARBON.showConfirmationDialog(confirmationMessage,
                                    function(){
                                        if(jQuery('#deleteRoleMappings').val() == 'true'){
                                            var confirmationMessage = 'Are you sure you want to ' +
                                                    'delete the Role Mappings of ' +
                                                    jQuery('#idPName').val() + '?';
                                            if(jQuery('#roleMappingFile').val() != ''){
                                                confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                            }
                                            CARBON.showConfirmationDialog(confirmationMessage,
                                                    function(){
                                                        doEditFinish();
                                                    },
                                                    function(){
                                                        location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                                    });
                                        } else {
                                            doEditFinish();
                                        }
                                    },
                                    function(){
                                        location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                    });
                        } else {
                            if(jQuery('#deleteRoleMappings').val() == 'true'){
                                var confirmationMessage = 'Are you sure you want to ' +
                                        'delete the Role Mappings of ' +
                                        jQuery('#idPName').val() + '?';
                                if(jQuery('#roleMappingFile').val() != ''){
                                    confirmationMessage = confirmationMessage.replace("delete", "re-upload");
                                }
                                CARBON.showConfirmationDialog(confirmationMessage,
                                        function(){
                                            doEditFinish();
                                        },
                                        function(){
                                            location.href = "idp-mgt-edit.jsp?idPName=<%=idPName%>";
                                        });
                            } else {
                                doEditFinish();
                            }
                        }
                    }
                }
            }
        }
    }

    function doEditFinish(){
        jQuery('#primary').removeAttr('disabled');
        jQuery('#openIdEnabled').removeAttr('disabled');
        jQuery('#saml2SSOEnabled').removeAttr('disabled');
        jQuery('#oidcEnabled').removeAttr('disabled');
        jQuery('#passiveSTSEnabled').removeAttr('disabled');
        jQuery('#fbAuthEnabled').removeAttr('disabled');
        jQuery('#openIdDefault').removeAttr('disabled');
        jQuery('#saml2SSODefault').removeAttr('disabled');
        jQuery('#oidcDefault').removeAttr('disabled');
        jQuery('#passiveSTSDefault').removeAttr('disabled');
        jQuery('#fbAuthDefault').removeAttr('disabled');
        <% if(idPName == null || idPName.equals("")){ %>
        jQuery('#idp-mgt-edit-form').attr('action','idp-mgt-add-finish.jsp');
        <% } %>
        jQuery('#idp-mgt-edit-form').submit();
    }
    function idpMgtCancel(){
        location.href = "idp-mgt-list.jsp"
    }
    function doValidation() {
        var reason = "";
        reason = validateEmpty("idPName");
        if (reason != "") {
            CARBON.showWarningDialog("Name of IdP cannot be empty");
            return false;
        }
        for(var i=0; i <= claimRowId; i++){
            if(document.getElementsByName('claimrowname_'+i)[0] != null){
                reason = validateEmpty('claimrowname_'+i);
                if(reason != ""){
                    CARBON.showWarningDialog("Claim URI strings cannot be of zero length");
                    return false;
                }
            }
        }
        for(var i=0; i <= roleRowId; i++){
            if(document.getElementsByName('rolerowname_'+i)[0] != null){
                reason = validateEmpty('rolerowname_'+i);
                if(reason != ""){
                    CARBON.showWarningDialog("Role name strings cannot be of zero length");
                    return false;
                }
            }
        }
        return true;
    }
</script>

<fmt:bundle basename="org.wso2.carbon.idp.mgt.ui.i18n.Resources">
    <div id="middle">
        <h2>
            <fmt:message key='identity.providers'/>
        </h2>
        <div id="workArea">
        <form id="idp-mgt-edit-form" name="idp-mgt-edit-form" method="post" action="idp-mgt-edit-finish.jsp" enctype="multipart/form-data" >
            <div class="sectionSeperator togglebleTitle"><fmt:message key='identity.provider.info'/></div>
            <div class="sectionSub">
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='name'/>:<span class="required">*</span></td>
                        <td>
                            <input id="idPName" name="idPName" type="text" value="<%=idPName%>" autofocus/>
                            <div class="sectionHelp">
                                <fmt:message key='name.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="primary"><fmt:message key='primary'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="primary" name="primary" type="checkbox" <%=primaryDisabled%> <%=primaryChecked%>/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='primary.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='home.realm.id'/>:</td>
                        <td>
                            <input id="realmId" name="realmId" type="text" value="<%=realmId%>" autofocus/>
                            <div class="sectionHelp">
                                <fmt:message key='home.realm.id.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='certificate'/>:</td>
                        <td>
                            <input id="certFile" name="certFile" type="file" />
                            <div class="sectionHelp">
                                <fmt:message key='certificate.help'/>
                            </div>
                            <div id="publicCertDiv">
                                <% if(certData != null) { %>
                                <a id="publicCertDeleteLink" class="icon-link" style="margin-left:0;background-image:url(images/delete.gif);"><fmt:message key='public.cert.delete'/></a>
                                <div style="clear:both"></div>
                                    <table class="styledLeft">
                                        <thead><tr><th><fmt:message key='issuerdn'/></th>
                                            <th><fmt:message key='subjectdn'/></th>
                                            <th><fmt:message key='notafter'/></th>
                                            <th><fmt:message key='notbefore'/></th>
                                            <th><fmt:message key='serialno'/></th>
                                            <th><fmt:message key='version'/></th>
                                        </tr></thead>
                                        <tbody>
                                            <tr><td><%=certData.getIssuerDN()%></td>
                                                <td><%=certData.getSubjectDN()%></td>
                                                <td><%=certData.getNotAfter()%></td>
                                                <td><%=certData.getNotBefore()%></td>
                                                <td><%=certData.getSerialNumber()%></td>
                                                <td><%=certData.getVersion()%></td>
                                            </tr>
                                        </tbody>
                                    </table>
                                <% } %>
                            </div>
                        </td>
                    </tr>
                    
                 
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='token.endpoint.alias'/>:</td>
                        <td>
                            <input id="tokenEndpointAlias" name="tokenEndpointAlias" type="text" value="<%=tokenEndpointAlias%>" autofocus/>
                            <div class="sectionHelp">
                                <fmt:message key='token.endpoint.alias.help'/>
                            </div>
                        </td>
                    </tr>
                    
                </table>
            </div>
            
            <h2 id="out_bound_auth_head"  class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="out.bound.auth.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="outBoundAuth">

            <h2 id="openid_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="openid.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="openIdLinkRow">
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="openIdEnabled"><fmt:message key='openid.enabled'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="openIdEnabled" name="openIdEnabled" type="checkbox" <%=openIdEnabledChecked%> onclick="checkEnabled(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='openid.enabled.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="openIdDefault"><fmt:message key='openid.default'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="openIdDefault" name="openIdDefault" type="checkbox" <%=openIdDefaultChecked%> <%=openIdDefaultDisabled%> onclick="checkDefault(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='openid.default.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='openid.url'/>:</td>
                        <td>
                            <input id="openIdUrl" name="openIdUrl" type="text" value="<%=openIdUrl%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='openid.url.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='openid.user.id.location'/>:</td>
                        <td>
                            <label>
                                <input type="radio" value="0" name="open_id_user_id_location" <% if(!isOpenIdUserIdInClaims){ %> checked="checked" <%}%> />
                                User ID found in 'claimed_id'
                            </label>
                            <label>
                                <input type="radio" value="1" name="open_id_user_id_location" <% if(isOpenIdUserIdInClaims){ %> checked="checked" <%}%> />
                                User ID found among claims
                            </label>
                            <div class="sectionHelp">
                                <fmt:message key='openid.user.id.location.help'/>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>

            <h2 id="saml2_sso_head"  class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="saml2.web.sso.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="saml2SSOLinkRow">
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="saml2SSOEnabled"><fmt:message key='saml2.sso.enabled'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="saml2SSOEnabled" name="saml2SSOEnabled" type="checkbox" <%=saml2SSOEnabledChecked%> onclick="checkEnabled(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='saml2.sso.enabled.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="saml2SSODefault"><fmt:message key='saml2.sso.default'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="saml2SSODefault" name="saml2SSODefault" type="checkbox" <%=saml2SSODefaultChecked%> <%=saml2SSODefaultDisabled%> onclick="checkDefault(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='saml2.sso.default.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
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
                        <td class="leftCol-med labelField"><fmt:message key='sp.entity.id'/>:</td>
                        <td>
                            <input id="spEntityId" name="spEntityId" type="text" value="<%=spEntityId%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='sp.entity.id.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='sso.url'/>:</td>
                        <td>
                            <input id="ssoUrl" name="ssoUrl" type="text" value="<%=ssoUrl%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='sso.url.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="authnRequestSigned"><fmt:message key='authn.request.signed'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="authnRequestSigned" name="authnRequestSigned" type="checkbox" <%=authnRequestSignedChecked%>/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='authn.request.signed.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="sloEnabled"><fmt:message key='logout.enabled'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="sloEnabled" name="sloEnabled" type="checkbox" <%=sloEnabledChecked%>/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='logout.enabled.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='logout.url'/>:</td>
                        <td>
                            <input id="logoutUrl" name="logoutUrl" type="text" value="<%=logoutUrl%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='logout.url.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="logoutRequestSigned"><fmt:message key='logout.request.signed'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="logoutRequestSigned" name="logoutRequestSigned" type="checkbox" <%=logoutRequestSignedChecked%>/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='logout.request.signed.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="authnResponseSigned"><fmt:message key='authn.response.signed'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="authnResponseSigned" name="authnResponseSigned" type="checkbox" <%=authnResponseSignedChecked%>/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='authn.response.signed.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='saml2.sso.user.id.location'/>:</td>
                        <td>
                            <label>
                                <input type="radio" value="0" name="saml2_sso_user_id_location" <% if(!isSAMLSSOUserIdInClaims){ %> checked="checked" <%}%> />
                                User ID found in 'Name Identifier'
                            </label>
                            <label>
                                <input type="radio" value="1" name="saml2_sso_user_id_location" <% if(isSAMLSSOUserIdInClaims){ %> checked="checked" <%}%> />
                                User ID found among claims
                            </label>
                            <div class="sectionHelp">
                                <fmt:message key='saml2.sso.user.id.location.help'/>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>

            <h2 id="oauth2_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="oidc.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="oauth2LinkRow">
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="oidcEnabled"><fmt:message key='oidc.enabled'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="oidcEnabled" name="oidcEnabled" type="checkbox" <%=oidcEnabledChecked%> onclick="checkEnabled(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='oidc.enabled.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="oidcDefault"><fmt:message key='oidc.default'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="oidcDefault" name="oidcDefault" type="checkbox" <%=oidcDefaultChecked%> <%=oidcDefaultDisabled%> onclick="checkDefault(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='oidc.default.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='authz.endpoint'/>:</td>
                        <td>
                            <input id="authzUrl" name="authzUrl" type="text" value="<%=authzUrl%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='authz.endpoint.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='token.endpoint'/>:</td>
                        <td>
                            <input id="tokenUrl" name="tokenUrl" type="text" value="<%=tokenUrl%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='token.endpoint.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='client.id'/>:</td>
                        <td>
                            <input id="clientId" name="clientId" type="text" value="<%=clientId%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='client.id.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='client.secret'/>:</td>
                        <td>
                            <input id="clientSecret" name="clientSecret" type="text" value="<%=clientSecret%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='client.secret.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='oidc.user.id.location'/>:</td>
                        <td>
                            <label>
                                <input type="radio" value="0" name="oidc_user_id_location" <% if(!isOIDCUserIdInClaims){ %> checked="checked" <%}%> />
                                User ID found in 'sub' attribute
                            </label>
                            <label>
                                <input type="radio" value="1" name="oidc_user_id_location" <% if(isOIDCUserIdInClaims){ %> checked="checked" <%}%> />
                                User ID found among claims
                            </label>
                            <div class="sectionHelp">
                                <fmt:message key='oidc.user.id.location.help'/>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>

            <h2 id="passive_sts_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="passive.sts.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="passiveSTSLinkRow">
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="passiveSTSEnabled"><fmt:message key='passive.sts.enabled'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="passiveSTSEnabled" name="passiveSTSEnabled" type="checkbox" <%=passiveSTSEnabledChecked%> onclick="checkEnabled(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='passive.sts.enabled.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="passiveSTSDefault"><fmt:message key='passive.sts.default'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="passiveSTSDefault" name="passiveSTSDefault" type="checkbox" <%=passiveSTSDefaultChecked%> <%=passiveSTSDefaultDisabled%> onclick="checkDefault(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='passive.sts.default.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='passive.sts.realm'/>:</td>
                        <td>
                            <input id="passiveSTSRealm" name="passiveSTSRealm" type="text" value="<%=passiveSTSRealm%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='passive.sts.realm.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='passive.sts.url'/>:</td>
                        <td>
                            <input id="passiveSTSUrl" name="passiveSTSUrl" type="text" value="<%=passiveSTSUrl%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='passive.sts.url.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='passive.sts.user.id.location'/>:</td>
                        <td>
                            <label>
                                <input type="radio" value="0" name="passive_sts_user_id_location" <% if(!isPassiveSTSUserIdInClaims){ %> checked="checked" <%}%>/>
                                User ID found in 'Name Identifier'
                            </label>
                            <label>
                                <input type="radio" value="1" name="passive_sts_user_id_location" <% if(isPassiveSTSUserIdInClaims){ %> checked="checked" <%}%>/>
                                User ID found among claims
                            </label>
                            <div class="sectionHelp">
                                <fmt:message key='passive.sts.user.id.location.help'/>
                            </div>
                        </td>
                    </tr>
                </table>
            </div>
			
			<h2 id="fb_auth_head" class="sectionSeperator trigger active" style="background-color: beige;">
                <a href="#"><fmt:message key="fbauth.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="fbAuthLinkRow">
                <table class="carbonFormTable">
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="fbAuthEnabled"><fmt:message key='fbauth.enabled'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="fbAuthEnabled" name="fbAuthEnabled" type="checkbox" <%=fbAuthEnabledChecked%> onclick="checkEnabled(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='fbauth.enabled.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField">
                            <label for="fbAuthDefault"><fmt:message key='fbauth.default'/></label>
                        </td>
                        <td>
                            <div class="sectionCheckbox">
                                <input id="fbAuthDefault" name="fbAuthDefault" type="checkbox" <%=fbAuthDefaultChecked%> <%=fbAuthDefaultDisabled%> onclick="checkDefault(this);"/>
                                <span style="display:inline-block" class="sectionHelp">
                                    <fmt:message key='fbauth.default.help'/>
                                </span>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='client.id'/>:</td>
                        <td>
                            <input id="fbClientId" name="fbClientId" type="text" value="<%=fbClientId%>"/>
                            <div class="sectionHelp">
                                <fmt:message key='fbauth.client.id.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                            <td class="leftCol-med labelField"><fmt:message
                                    key='client.secret' />:</td>
                            <td>
                                <table>
                                    <tr>
                                        <td><input id="fbClientSecret"
                                            name="fbClientSecret" type="password"
                                            value="<%=fbClientSecret%>" /></td>
                                        <td style="vertical-align: middle;"><input
                                            type="checkbox"
                                            onchange="document.getElementById('fbClientSecret').type = this.checked ? 'text' : 'password'">
                                            Show password</td>
                                    </tr>
                                    <tr>
                                        <td colspan="2"><div class="sectionHelp">
                                                <fmt:message key='fbauth.client.secret.help' />
                                            </div></td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                </table>
            </div>
            </div>
            
            <h2 id="in_bound_provisioning_head"  class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="in.bound.provisioning.config"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="inBoundProvisioning">
            <table>
            <tr>
                     <td>
                            <label style="display:block">
                                <input type="radio" id="provision_disabled" name="provisioning" value="provision_disabled" <% if(!isProvisioningEnabled){ %> checked="checked" <% } %> />
                                No provisioning
                            </label>
                            <div>
                                <label>
                                    <input type="radio" id="provision_static" name="provisioning" value="provision_static" <% if(isProvisioningEnabled && provisioningUserStoreId != null){ %> checked="checked" <% } %>/>
                                    Always provision to User Store Domain
                                </label>
                                <select id="provision_static_dropdown" name="provision_static_dropdown" <%=provisionStaticDropdownDisabled%>>
                                    <%
                                        if(userStoreDomains != null && userStoreDomains.length > 0){
                                            for(String userStoreDomain : userStoreDomains){
                                                if(provisioningUserStoreId != null && userStoreDomain.equals(provisioningUserStoreId)){
                                    %>
                                                    <option selected="selected"><%=userStoreDomain%></option>
                                    <%
                                                } else {
                                    %>
                                                    <option><%=userStoreDomain%></option>
                                    <%
                                                }
                                            }
                                        }
                                    %>
                                </select>

                            </div>
                            <div>
                                <label>
                                    <input type="radio" id="provision_dynamic" name="provisioning" value="provision_dynamic" <% if(isProvisioningEnabled && provisioningUserStoreIdClaimURI != null){ %> checked="checked" <% } %> />
                                    Provision dynamically to the User Store identified by
                                </label>
                                <select id="provision_dynamic_dropdown" name="provision_dynamic_dropdown" <%=provisionDynamicDropdownDisabled%>></select>
                            </div>
                            <div class="sectionHelp">
                                <fmt:message key='provisioning.enabled.help'/>
                            </div>
                        </td>
                    </tr>
            </table>
            </div>
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            
            <!-- Outbound Provisioning UI -->
            <h2 id="out_bound_provisioning_head"  class="sectionSeperator trigger active" >
                <a href="#"><fmt:message key="out.bound.provisioning.config"/></a>
            </h2>
            
            
            <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="outBoundProv">
                
                <!-- Google Connector -->
                <h2 id="google_prov_head" class="sectionSeperator trigger active" style="background-color: beige;">
                    <a href="#"><fmt:message key="google.provisioning.connector"/></a>
                </h2>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="googleProvRow">
                    <table class="carbonFormTable">
                        <tr>
                            <td class="leftCol-med labelField">
                                <label for="googleProvEnabled"><fmt:message key='google.provisioning.enabled'/>:</label>
                            </td>
                            <td>
                                <div class="sectionCheckbox">
<!-- -->                                    <input id="googleProvEnabled" name="googleProvEnabled" type="checkbox" <%=googleProvEnabledChecked%> onclick="checkProvEnabled(this);"/>
                                    <span style="display:inline-block" class="sectionHelp">
                                        <fmt:message key='google.provisioning.enabled.help'/>
                                    </span>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField">
                                <label for="googleProvDefault"><fmt:message key='google.provisioning.default'/>:</label>
                            </td>
                            <td>
                                <div class="sectionCheckbox">
<!-- -->                                    <input id="googleProvDefault" name="googleProvDefault" type="checkbox" <%=googleProvDefaultChecked%> <%=googleProvDefaultDisabled%> onclick="checkProvDefault(this);"/>
                                    <span style="display:inline-block" class="sectionHelp">
                                        <fmt:message key='google.provisioning.default.help'/>
                                    </span>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.domain.name'/>:</td>
                            <td>
                                <input id="google_prov_domain_name" name="google_prov_domain_name" type="text" value="<%=googleDomainName%>"/>
                                <div class="sectionHelp">
                                    <fmt:message key='google.provisioning.domain.name.help'/>
                                </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.attribute.primary.email'/>:</td>
                            <td>                                
	                            <div>
                                    <select id="google_prov_email_claim_dropdown" name="google_prov_email_claim_dropdown">
                                        <option selected="selected">--- Select Claim URI ---</option>
                                        <% for(String idpClaimName : claimUris) { %>
                                            <option value="<%=idpClaimName%>"> <%=idpClaimName%></option>
                                        <% } %>
                                    </select>
                                    <!--a id="claimMappingAddLink" class="icon-link" style="background-image: url(images/add.gif);"><fmt:message key='button.add.claim.mapping' /></a-->
	                            </div>
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.attribute.primary.email.help'/>
	                            </div>
                            </td>
                        </tr>
  
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.attribute.given.name'/>:</td>
                            <td>                                
	                            <div>
	                                <label>
<!-- -->	                          Pick given name from Claim : 
	                                </label>
                                    <select id="google_prov_givenname_claim_dropdown" name="google_prov_givenname_claim_dropdown">
                                        <option selected="selected">--- Select Claim URI ---</option>
                                        <% for(String idpClaimName : claimUris) { %>
                                        <option value="<%=idpClaimName%>"> <%=idpClaimName%></option>
                                        <% } %>
                                    </select>
	                            </div>
	                            <div>
	                                <label>
	                                    Given name default value : 
	                                </label>
                                    <input id="google_prov_givenname" name="google_prov_givenname" type="text" value="<%=googleGivenNameDefaultValue%>"/>
	                            </div>
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.attribute.given.name.help'/>
	                            </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.attribute.family.name'/>:</td>
                            <td>                                
	                            <div>
	                                <label>
	                                    Pick family name from Claim : 
	                                </label>
                                    <select id="google_prov_familyname_claim_dropdown" name="google_prov_familyname_role_claim_dropdown">
                                        <option selected="selected">--- Select Claim URI ---</option>
                                        <% for(String idpClaimName : claimUris) { %>
                                        <option value="<%=idpClaimName%>"> <%=idpClaimName%></option>
                                        <% } %>
                                    </select>
	                            </div>
	                            <div>
	                                <label>
	                                    Family name default value : 
	                                </label>
                                    <input id="google_prov_familyname" name="google_prov_familyname" type="text" value="<%=googleFamilyNameDefaultValue%>"/>
	                            </div>
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.attribute.family.name.help'/>
	                            </div>
                            </td>
                        </tr>

                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.service.accont.email'/>:</td>
                            <td>                                
	                            <div>
                                    <input id="google_prov_service_acc_email" name="google_prov_service_acc_email" type="text" value="<%=googleProvServiceAccEmail%>"/>
	                            </div>
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.service.accont.email.help'/>
	                            </div>
                            </td>
                        </tr>
	                    <tr>
	                        <td class="leftCol-med labelField"><fmt:message key='google.provisioning.service.account.private.key'/>:</td>
	                        <td>
                                <input id="google_prov_private_key" name="google_prov_private_key" type="file" />
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.service.account.private.key.help'/>
	                            </div>
                                <div id="google_prov_privatekey_div">
	                                <% if(googleProvPrivateKeyData != null) { %>
<!-- -->	                                <a id="google_prov_privatekey_deletelink" class="icon-link" style="margin-left:0;background-image:url(images/delete.gif);"><fmt:message key='google.provisioning.service.account.private.key.delete'/></a>
	                                <div style="clear:both"></div>
<!--	                                    <table class="styledLeft">
	                                        <thead><tr><th><fmt:message key='issuerdn'/></th>
	                                            <th><fmt:message key='subjectdn'/></th>
	                                            <th><fmt:message key='notafter'/></th>
	                                            <th><fmt:message key='notbefore'/></th>
	                                            <th><fmt:message key='serialno'/></th>
	                                            <th><fmt:message key='version'/></th>
	                                        </tr></thead>
	                                        <tbody>
	                                            <tr><td><%=certData.getIssuerDN()%></td>
	                                                <td><%=certData.getSubjectDN()%></td>
	                                                <td><%=certData.getNotAfter()%></td>
	                                                <td><%=certData.getNotBefore()%></td>
	                                                <td><%=certData.getSerialNumber()%></td>
	                                                <td><%=certData.getVersion()%></td>
	                                            </tr>
	                                        </tbody>
	                                    </table>
 -->	                                <% } %>
	                            </div>
	                        </td>
	                    </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.admin.email'/>:</td>
                            <td>                                
	                            <div>
	                                <input id="google_prov_admin_email" name="google_prov_admin_email" type="text" value="<%=googleProvAdminEmail%>"/>
	                            </div>
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.admin.email.help'/>
	                            </div>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-med labelField"><fmt:message key='google.provisioning.application.name'/>:</td>
                            <td>                                
	                            <div>
                                    <input id="google_prov_application_name" name="google_prov_application_name" type="text" value="<%=googleProvApplicationName%>"/>
	                            </div>
	                            <div class="sectionHelp">
	                                <fmt:message key='google.provisioning.application.name.help'/>
	                            </div>
                            </td>
                        </tr>
                    </table>
                </div>
                
                <h2 id="sf_prov_head" class="sectionSeperator trigger active" style="background-color: beige;">
                    <a href="#"><fmt:message key="sf.provisioning.connector"/></a>
                </h2>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="sfProvRow">
                                 
                <table class="carbonFormTable">
                            <tr>
		                        <td  class="leftCol-med labelField">API version:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="sf-api-version" name="sf-api-version"
		                                   type="text" /></td>
		                    </tr>
		                    <tr>
		                        <td  class="leftCol-med labelField">Domain Name:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="sf-domain-name" name="sf-domain-name"
		                                   type="text" /></td>
		                    </tr>
                			<tr>
		                        <td  class="leftCol-med labelField">Client ID:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="sf-clientid" name="sf-clientid"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">CLient Secret:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="sf-client-secret" name="sf-client-secret"
		                                   type="password" /></td>
		                    </tr>
		                    <tr>
		                        <td  class="leftCol-med labelField">Username:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="sf-username" name="sf-username"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">Password:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="sf-password" name="sf-password"
		                                   type="password" /></td>
		                    </tr>
		                    
		                    <tr>
		                    <td colspan="2">
		                    <hr/>
		                     	<table>
		                     	    <tr><td><fmt:message key='help.sf.claim.mapping'/></td></tr>
									<tr>
										<td>
											<select id="idpClaimsList" name="idpClaimsList" style="float:left;">							
													<% for(String localClaimName : claimUris) { %>
																<option value="<%=localClaimName%>"> <%=localClaimName%></option>
													<% } %>
											</select>
											<a id="claimMappingAddLink" class="icon-link" style="background-image: url(images/add.gif);"><fmt:message key='button.add.claim.mapping' /></a>
											<div style="clear:both"/>
                            				
							</td>
							</tr>
					       </table>
		                    <table class="styledLeft" id="claimMappingAddTable" >
                              <thead><tr>
                              <th class="leftCol-med">Local Claim</th>
                              <th class="leftCol-big">Salesforce Attribute</th>
                              <th class="leftCol-mid">Default Value</th>
                              
                              <th>Actions</th></tr></thead>
                              <tbody>
                       
                              </tbody>
                             </table>
		                    
		                    </td>
		                    </tr>

				</table>
                
                </div>
                 
                 <h2 id="scim_prov_head" class="sectionSeperator trigger active" style="background-color: beige;">
                    <a href="#"><fmt:message key="scim.provisioning.connector"/></a>
                </h2>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="scimProvRow">
                 
                 <table class="carbonFormTable">
		                    <tr>
		                        <td  class="leftCol-med labelField">Username:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="scim-username" name="scim-username"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">Password:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="scim-password" name="scim-password"
		                                   type="password" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">User Endpoint:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="scim-user-ep" name="scim-user-ep"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">Group Endpoint:</td>
		                        <td><input class="text-box-big" id="scim-group-ep" name="scim-group-ep"
		                                   type="text" /></td>
		                    </tr>
				</table>
                 
                 </div>
                 
                 <h2 id="spml_prov_head" class="sectionSeperator trigger active" style="background-color: beige;">
                    <a href="#"><fmt:message key="spml.provisioning.connector"/></a>
                </h2>
                <div class="toggle_container sectionSub" style="margin-bottom:10px;display: none;" id="spmlProvRow">
                 
                         <table class="carbonFormTable">
		                    <tr>
		                        <td  class="leftCol-med labelField">Username:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="spml-username" name="spml-username"
		                                   type="text" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">Password:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="spml-password" name="spml-password"
		                                   type="password" /></td>
		                    </tr>
                            <tr>
		                        <td  class="leftCol-med labelField">SPML Endpoint:<span class="required">*</span></td>
		                        <td><input class="text-box-big" id="spml-ep" name="spml-ep"
		                                   type="text" /></td>
		                    </tr>
				</table>
                 </div>
                
            </div>
     
            
            
            <h2 id="role_permission_config_head"  class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="role.permission.config.head"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="roleConfig">
            <table>
               <tr>
                        <td class="leftCol-med labelField"><fmt:message key='roles'/>:</td>
                        <td>
                            <a id="roleAddLink" class="icon-link" style="margin-left:0;background-image:url(images/add.gif);"><fmt:message key='add.role'/></a>
                            <div style="clear:both"/>
                            <div class="sectionHelp">
                                <fmt:message key='roles.help'/>
                            </div>
                            <table class="styledLeft" id="roleAddTable" style="display:none" >
                                <thead><tr><th class="leftCol-big"><fmt:message key='idp.role'/></th><th><fmt:message key='actions'/></th></tr></thead>
                                <tbody>
                                <% if(roles != null && roles.length>0){ %>
                                    <script>
                                        $(jQuery('#roleAddTable')).toggle();
                                    </script>
                                    <% for(int i = 0; i < roles.length; i++){ %>
                                        <tr>
                                            <td><input type="text" value="<%=roles[i]%>" id="rolerowname_<%=i%>" name="rolerowname_<%=i%>"/></td>
                                            <td>
                                                <a title="<fmt:message key='delete.role'/>"
                                                   onclick="deleteRoleRow(this);return false;"
                                                   href="#"
                                                   class="icon-link"
                                                   style="background-image: url(images/delete.gif)">
                                                    <fmt:message key='delete'/>
                                                </a>
                                            </td>
                                        </tr>
                                    <% } %>
                                <% } %>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='role.mappings'/>:</td>
                        <td>
                            <input id="roleMappingFile" name="roleMappingFile" type="file" />
                            <div class="sectionHelp">
                                <fmt:message key='role.mappings.help'/>
                            </div>
                            <% if(roleMappings != null && roleMappings.length > 0){ %>
                                <div id="roleMappingDiv">
                                    <a id="roleMappingDeleteLink" class="icon-link" style="background-image:url(images/delete.gif);"><fmt:message key='role.mapping.delete'/></a>
                                    <table class="styledLeft">
                                        <thead><tr><th class="leftCol-big"><fmt:message key='idp.role'/></th><th><fmt:message key='user.store.id'/></th><th><fmt:message key='tenant.role'/></th></tr></thead>
                                        <tbody>
                                        <%
                                            for(RoleMapping roleMapping:roleMappings){
                                                    if(roleMapping.getLocalRole().getUserStoreId() != null){
                                        %>
                                                        <tr><td><%=roleMapping.getRemoteRole()%></td><td><%=roleMapping.getLocalRole().getUserStoreId()%></td><td><%=roleMapping.getLocalRole().getLocalRoleName()%></td></tr>
                                        <%
                                                    } else {
                                        %>
                                                        <tr><td><%=roleMapping.getRemoteRole()%></td><td>PRIMARY</td><td><%=roleMapping.getLocalRole().getLocalRoleName()%></td></tr>
                                        <%
                                                    }
                                            }
                                        %>
                                        </tbody>
                                    </table>
                                </div>
                            <% } %>
                        </td>
                    </tr>
            
            </table>
            </div>
            
            
            <h2 id="claim_config_head"  class="sectionSeperator trigger active">
                <a href="#"><fmt:message key="claim.config.head"/></a>
            </h2>
            <div class="toggle_container sectionSub" style="margin-bottom:10px;" id="claimConfig">
            <table>
            <tr>
                        <td class="leftCol-med labelField"><fmt:message key='claimURIs'/>:</td>
                        <td>
                            <a id="claimAddLink" class="icon-link" style="margin-left:0;background-image:url(images/add.gif);"><fmt:message key='add.claim'/></a>
                            <div style="clear:both"/>
                            <div class="sectionHelp">
                                <fmt:message key='claimURIs.help'/>
                            </div>
                            <table class="styledLeft" id="claimAddTable" style="display:none">
                                <thead><tr><th class="leftCol-big"><fmt:message key='idp.claim'/></th><th><fmt:message key='actions'/></th></tr></thead>
                                <tbody>
                                <% if(identityProviderClaims != null && identityProviderClaims.length>0){ %>
                                <script>
                                    $(jQuery('#claimAddTable')).toggle();
                                </script>
                                <% for(int i = 0; i < identityProviderClaims.length; i++){ %>
                                <tr>
                                    <td><input type="text" class="claimrow" value="<%=identityProviderClaims[i].getClaimUri()%>" id="claimrowid_<%=i%>" name="claimrowname_<%=i%>"/></td>
                                    <td>
                                        <a title="<fmt:message key='delete.claim'/>"
                                           onclick="deleteClaimRow(this);return false;"
                                           href="#"
                                           class="icon-link"
                                           style="background-image: url(images/delete.gif)">
                                            <fmt:message key='delete'/>
                                        </a>
                                    </td>
                                </tr>
                                <% } %>
                                <% } %>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='user.id.claim.uri'/>:</td>
                        <td>
                            <select id="user_id_claim_dropdown" name="user_id_claim_dropdown"></select>
                            <div class="sectionHelp">
                                <fmt:message key='user.id.claim.uri.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='role.claim.uri'/>:</td>
                        <td>
                            <select id="role_claim_dropdown" name="role_claim_dropdown"></select>
                            <div class="sectionHelp">
                                <fmt:message key='role.claim.uri.help'/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td class="leftCol-med labelField"><fmt:message key='claim.mappings'/>:</td>
                        <td>
                            <input id="cliamMappingFile" name="claimMappingFile" type="file" />
                            <div class="sectionHelp">
                                <fmt:message key='claim.mappings.help'/>
                            </div>
                            <% if(claimMappings != null && claimMappings.length > 0){ %>
                            <div id="claimMappingDiv">
                                <a id="claimMappingDeleteLink" class="icon-link" style="background-image:url(images/delete.gif);"><fmt:message key='claim.mapping.delete'/></a>
                                <table class="styledLeft">
                                    <thead><tr><th class="leftCol-big"><fmt:message key='idp.claim'/></th><th><fmt:message key='tenant.claim'/></th></tr></thead>
                                    <tbody>
                                    <%
                                        for(ClaimMapping claimMapping:claimMappings){

                                    %>
                                            <tr><td><%=claimMapping.getIdpClaim().getClaimUri()%></td><td><%=claimMapping.getLocalClaim().getClaimUri()%></td></tr>
                                    <%
                                        }
                                    %>
                                    </tbody>
                                </table>
                            </div>
                            <% } %>
                        </td>
                    </tr>
            
            </table>
            </div>

            <!-- sectionSub Div -->
            <div class="buttonRow">
                <% if(identityProvider != null){ %>
                    <input type="button" value="<fmt:message key='update'/>" onclick="idpMgtUpdate();"/>
                <% } else { %>
                    <input type="button" value="<fmt:message key='register'/>" onclick="idpMgtUpdate();"/>
                <% } %>
                <input type="button" value="<fmt:message key='cancel'/>" onclick="idpMgtCancel();"/>
            </div>
            </form>
        </div>
    </div>

</fmt:bundle>