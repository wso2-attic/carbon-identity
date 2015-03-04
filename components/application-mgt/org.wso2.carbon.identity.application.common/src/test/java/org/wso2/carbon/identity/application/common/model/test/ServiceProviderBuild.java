package org.wso2.carbon.identity.application.common.model.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.InboundProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.LocalAndOutboundAuthenticationConfig;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.OutboundProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.common.model.User;


public class ServiceProviderBuild {
	public static void main(String args[]) throws XMLStreamException, IOException{
		
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(new FileReader("/Users/prabath/svn/wso2/carbon/branches/platform/turing/components/identity/org.wso2.carbon.identity.application.common/4.2.0/src/test/java/org/wso2/carbon/identity/application/common/model/test/sp.xml"));
		String identityProviderString = "";
		String line = null;
		while ((line = reader.readLine()) != null) {
			identityProviderString += line.trim();
		}
		OMElement documentElementOM = AXIOMUtil.stringToOM(identityProviderString);
		ServiceProvider serviceProvider = ServiceProvider.build(documentElementOM);
		
		System.out.println("applicationID: " + serviceProvider.getApplicationID());
		System.out.println("applicationName: " + serviceProvider.getApplicationName());
		System.out.println("description: " + serviceProvider.getDescription());
		
		System.out.println("inboundAuthenticationConfig: ");
		InboundAuthenticationConfig inboundAuthenticationConfig = serviceProvider.getInboundAuthenticationConfig();
		System.out.println("\tinboundAuthenticationRequestConfigs: ");
		InboundAuthenticationRequestConfig[] inboundAuthenticationRequestConfigs = inboundAuthenticationConfig.getInboundAuthenticationRequestConfigs();
		for(int i=0; i<inboundAuthenticationRequestConfigs.length; i++){
			System.out.println("\t\tinboundAuthKey: " + inboundAuthenticationRequestConfigs[i].getInboundAuthKey());
			System.out.println("\t\tinboundAuthType: " + inboundAuthenticationRequestConfigs[i].getInboundAuthType());
			System.out.println("\t\tproperties: ");
			Property[] properties = inboundAuthenticationRequestConfigs[i].getProperties();
			for(int j=0; j<properties.length; j++){
				System.out.println("\t\t\tname: " + properties[j].getName());
				System.out.println("\t\t\tvalue: " + properties[j].getValue());
				System.out.println("\t\t\tdefaultValue: " + properties[j].getDefaultValue());
				System.out.println("\t\t\tdescription: " + properties[j].getDescription());
				System.out.println("\t\t\tdisplayName" + properties[j].getDisplayName());
				System.out.println();
			}
		}
		
		System.out.println("localAndOutBoundAuthenticationConfig: ");
		LocalAndOutboundAuthenticationConfig localAndOutBoundAuthenticationConfig = serviceProvider.getLocalAndOutBoundAuthenticationConfig();
		System.out.println("\tauthenticationType: " + localAndOutBoundAuthenticationConfig.getAuthenticationType());
		System.out.println("\tauthenticationStepForSubject: ");
		AuthenticationStep authenticationStepForSubject = localAndOutBoundAuthenticationConfig.getAuthenticationStepForSubject();
		System.out.println("\t\tstepOrder: " + authenticationStepForSubject.getStepOrder());
		System.out.println("\t\tlocalAuthenticatorConfigs: ");
		LocalAuthenticatorConfig[] localAuthenticatorConfigs = authenticationStepForSubject.getLocalAuthenticatorConfigs();
		for(int i=0; i<localAuthenticatorConfigs.length; i++){
			System.out.println("\t\t\tname: " + localAuthenticatorConfigs[i].getName());
			System.out.println("\t\t\tdisplayName: " + localAuthenticatorConfigs[i].getDisplayName());
			System.out.println("\t\t\tproperties: ");
			Property[] properties = localAuthenticatorConfigs[i].getProperties();
			for(int j=0; j<properties.length; j++){
				System.out.println("\t\t\t\tname: " + properties[j].getName());
				System.out.println("\t\t\t\tvalue: " + properties[j].getValue());
				System.out.println("\t\t\t\tdefaultValue: " + properties[j].getDefaultValue());
				System.out.println("\t\t\t\tdescription: " + properties[j].getDescription());
				System.out.println("\t\t\t\tdisplayName" + properties[j].getDisplayName());
				System.out.println();
			}
		}
		System.out.println("\tauthenticationStepForAttributes: ");
		AuthenticationStep authenticationStepForAttributes = localAndOutBoundAuthenticationConfig.getAuthenticationStepForAttributes();
		System.out.println("\t\tstepOrder: " + authenticationStepForAttributes.getStepOrder());
		System.out.println("\t\tlocalAuthenticatorConfigs: ");
		localAuthenticatorConfigs = authenticationStepForAttributes.getLocalAuthenticatorConfigs();
		for(int i=0; i<localAuthenticatorConfigs.length; i++){
			System.out.println("\t\t\tname: " + localAuthenticatorConfigs[i].getName());
			System.out.println("\t\t\tdisplayName: " + localAuthenticatorConfigs[i].getDisplayName());
			System.out.println("\t\t\tproperties: ");
			Property[] properties = localAuthenticatorConfigs[i].getProperties();
			for(int j=0; j<properties.length; j++){
				System.out.println("\t\t\t\tname: " + properties[j].getName());
				System.out.println("\t\t\t\tvalue: " + properties[j].getValue());
				System.out.println("\t\t\t\tdefaultValue: " + properties[j].getDefaultValue());
				System.out.println("\t\t\t\tdescription: " + properties[j].getDescription());
				System.out.println("\t\t\t\tdisplayName" + properties[j].getDisplayName());
				System.out.println();
			}
		}
		
		System.out.println("\tauthenticationSteps: ");
		AuthenticationStep[] authenticationSteps = localAndOutBoundAuthenticationConfig.getAuthenticationSteps();
		for(int k=0; k<authenticationSteps.length; k++){
			System.out.println("\t\tstepOrder: " + authenticationSteps[k].getStepOrder());
			System.out.println("\t\tlocalAuthenticatorConfigs: ");
			localAuthenticatorConfigs = authenticationSteps[k].getLocalAuthenticatorConfigs();
			for(int i=0; i<localAuthenticatorConfigs.length; i++){
				System.out.println("\t\t\tname: " + localAuthenticatorConfigs[i].getName());
				System.out.println("\t\t\tdisplayName: " + localAuthenticatorConfigs[i].getDisplayName());
				System.out.println("\t\t\tproperties: ");
				Property[] properties = localAuthenticatorConfigs[i].getProperties();
				for(int j=0; j<properties.length; j++){
					System.out.println("\t\t\t\tname: " + properties[j].getName());
					System.out.println("\t\t\t\tvalue: " + properties[j].getValue());
					System.out.println("\t\t\t\tdefaultValue: " + properties[j].getDefaultValue());
					System.out.println("\t\t\t\tdescription: " + properties[j].getDescription());
					System.out.println("\t\t\t\tdisplayName" + properties[j].getDisplayName());
					System.out.println();
				}
			}
		}
		
		System.out.println();
		
		System.out.println("requestPathAuthenticatorConfigs: ");
		RequestPathAuthenticatorConfig[] requestPathAuthenticatorConfigs = serviceProvider.getRequestPathAuthenticatorConfigs();
		for(int i=0; i<requestPathAuthenticatorConfigs.length;i++){
			System.out.println("\tdisplayName: "+requestPathAuthenticatorConfigs[i].getDisplayName());
			System.out.println("\tname: "+requestPathAuthenticatorConfigs[i].getName());
			System.out.println("\t\tproperties: ");
			Property[] properties = localAuthenticatorConfigs[i].getProperties();
			for(int j=0; j<properties.length; j++){
				System.out.println("\t\t\tname: " + properties[j].getName());
				System.out.println("\t\t\tvalue: " + properties[j].getValue());
				System.out.println("\t\t\tdefaultValue: " + properties[j].getDefaultValue());
				System.out.println("\t\t\tdescription: " + properties[j].getDescription());
				System.out.println("\t\t\tdisplayName" + properties[j].getDisplayName());
				System.out.println();
			}
		}
		
		System.out.println("inboundProvisioningConfig: ");
		InboundProvisioningConfig inboundProvisioningConfig = serviceProvider.getInboundProvisioningConfig();
		System.out.println("\tprovisioningUserStore"+inboundProvisioningConfig.getProvisioningUserStore());
		System.out.println("\tprovisioningEnabled"+inboundProvisioningConfig.isProvisioningEnabled());
		
		System.out.println("outboundProvisioningConfig: ");
		OutboundProvisioningConfig outboundProvisioningConfig = serviceProvider.getOutboundProvisioningConfig();
		System.out.println("\tprovisioningIdentityProviders: ");
		System.out.println("\tprovisionByRoleList: ");
		String[] provisionByRoleList = outboundProvisioningConfig.getProvisionByRoleList();
		for(int i=0; i<provisionByRoleList.length; i++){
			System.out.println("\t\t"+provisionByRoleList[i]);
		}
		
		
		System.out.println("claimConfig: ");
		ClaimConfig claimConfig = serviceProvider.getClaimConfig();
		System.out.println("\t"+ claimConfig.getRoleClaimURI());
		System.out.println("\t"+ claimConfig.getUserClaimURI());
		ClaimMapping[] claimMapping = claimConfig.getClaimMappings();
		System.out.println("\tclaimMapping: ");
		for(int i=0; i<claimMapping.length; i++){
			System.out.println("\t\tdefaultValue: "+ claimMapping[i].getDefaultValue());
			Claim localClaim = claimMapping[i].getLocalClaim();
			System.out.println("\t\tlocalClaim: ");
			System.out.println("\t\t\tclaimId: "+localClaim.getClaimId());
			System.out.println("\t\t\tclaimUri: "+localClaim.getClaimUri());
			Claim remoteClaim = claimMapping[i].getRemoteClaim();
			System.out.println("\t\tremoteClaim: ");
			System.out.println("\t\t\tclaimId: "+remoteClaim.getClaimId());
			System.out.println("\t\t\tclaimUri: "+remoteClaim.getClaimUri());
		}
		
		System.out.println("permissionAndRoleConfig: ");
		PermissionsAndRoleConfig permissionsAndRoleConfig = serviceProvider.getPermissionAndRoleConfig();
		System.out.println("\tpermissions:");
		ApplicationPermission[] permissions = permissionsAndRoleConfig.getPermissions();
		for(int i=0; i<permissions.length; i++){
			System.out.println("\t\t"+permissions[i].getValue());
		}
		System.out.println("\tideRoles:");
		String[] idepRoles = permissionsAndRoleConfig.getIdpRoles();
		for(int i=0; i<idepRoles.length; i++){
			System.out.println("\t\t"+idepRoles[i]);
		}
		RoleMapping[] roleMappings = permissionsAndRoleConfig.getRoleMappings();
		for(int i=0; i<roleMappings.length; i++){
			LocalRole localRole = roleMappings[i].getLocalRole();
			System.out.println("\t\tlocalRole: ");
			System.out.println("\t\t\tlocalRoleName: " + localRole.getLocalRoleName());
			System.out.println("\t\t\tuserStoreId: " + localRole.getUserStoreId());
		}
		
	}
}
