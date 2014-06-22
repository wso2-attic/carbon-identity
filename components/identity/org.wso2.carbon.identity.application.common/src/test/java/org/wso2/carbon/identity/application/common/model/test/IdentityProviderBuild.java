package org.wso2.carbon.identity.application.common.model.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.wso2.carbon.identity.application.common.model.ApplicationPermission;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.JustInTimeProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.LocalRole;
import org.wso2.carbon.identity.application.common.model.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ProvisioningConnectorConfig;
import org.wso2.carbon.identity.application.common.model.RoleMapping;

public class IdentityProviderBuild {
	public static void main(String args[]) throws IOException, XMLStreamException {

		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(new FileReader("identityprovider-config/IdentityProvider.xml"));
		String identityProviderString = "";
		String line = null;
		while ((line = reader.readLine()) != null) {
			identityProviderString += line.trim();
		}
		OMElement documentElementOM = AXIOMUtil.stringToOM(identityProviderString);
		
		IdentityProvider identityProvider = IdentityProvider.build(documentElementOM);
		System.out.println("identityProviderName: "+identityProvider.getIdentityProviderName());
		System.out.println("identityProviderDescription: "+identityProvider.getIdentityProviderDescription());
		System.out.println("alias: "+identityProvider.getAlias());
		System.out.println("primary: "+identityProvider.isPrimary());
		System.out.println("federationHub: "+identityProvider.isFederationHub());
		System.out.println("homeRealmId: "+identityProvider.getHomeRealmId());
		FederatedAuthenticatorConfig[] federatedAuthenticatorConfigs = identityProvider.getFederatedAuthenticatorConfigs();
		System.out.println("federatedAuthenticatorConfigs:");
		for(int i=0; i<federatedAuthenticatorConfigs.length; i++){
			System.out.println("\tname: "+federatedAuthenticatorConfigs[i].getName());
			System.out.println("\tdisplayName: "+federatedAuthenticatorConfigs[i].getDisplayName());
			System.out.println("\tenabled: "+federatedAuthenticatorConfigs[i].isEnabled());
			
			Property[] properties = federatedAuthenticatorConfigs[i].getProperties();
			System.out.println("\tproperties: ");
			for(int j=0; j<properties.length; j++){
				System.out.println("\t\tname: " + properties[j].getName());
				System.out.println("\t\tvalue: " + properties[j].getValue());
				System.out.println("\t\tconfidential: " + properties[j].isConfidential());
				System.out.println("\t\tdefaultValue: " + properties[j].getDefaultValue());
				System.out.println("\t\tdisplayName: " + properties[j].getDisplayName());
				System.out.println("\t\trequired: " + properties[j].isRequired());
				System.out.println("\t\tdescription: " + properties[j].getDescription());
				System.out.println();
			}
			
		}
		
		FederatedAuthenticatorConfig defaultAuthenticatorConfig = identityProvider.getDefaultAuthenticatorConfig();
		System.out.println("defaultAuthenticatorConfig: ");
		System.out.println("\tname: "+defaultAuthenticatorConfig.getName());
		System.out.println("\tdisplayName: "+defaultAuthenticatorConfig.getDisplayName());
		System.out.println("\tenabled: "+defaultAuthenticatorConfig.isEnabled());
		
		Property[] properties = defaultAuthenticatorConfig.getProperties();
		System.out.println("\tproperties: ");
		for(int j=0; j<properties.length; j++){
			System.out.println("\t\tname: " + properties[j].getName());
			System.out.println("\t\tvalue: " + properties[j].getValue());
			System.out.println("\t\tconfidential: " + properties[j].isConfidential());
			System.out.println("\t\tdefaultValue: " + properties[j].getDefaultValue());
			System.out.println("\t\tdisplayName: " + properties[j].getDisplayName());
			System.out.println("\t\trequired: " + properties[j].isRequired());
			System.out.println("\t\tdescription: " + properties[j].getDescription());
			System.out.println();
		}
		
		ProvisioningConnectorConfig[] provisioningConnectorConfigs = identityProvider.getProvisioningConnectorConfigs();
		System.out.println("provisioningConnectorConfigs: ");
		for(int i=0; i<federatedAuthenticatorConfigs.length; i++){
			System.out.println("\tname: "+provisioningConnectorConfigs[i].getName());
			properties = provisioningConnectorConfigs[i].getProvisioningProperties();
			System.out.println("\tgetProvisioningProperties: ");
			for(int j=0; j<properties.length; j++){
				System.out.println("\t\tname: " + properties[j].getName());
				System.out.println("\t\tvalue: " + properties[j].getValue());
				System.out.println("\t\tconfidential: " + properties[j].isConfidential());
				System.out.println("\t\tdefaultValue: " + properties[j].getDefaultValue());
				System.out.println("\t\tdisplayName: " + properties[j].getDisplayName());
				System.out.println("\t\trequired: " + properties[j].isRequired());
				System.out.println("\t\tdescriptions: " + properties[j].getDescription());
				System.out.println();
			}
			
		}
		
		ProvisioningConnectorConfig defaultProvisioningConnectorConfig = identityProvider.getDefaultProvisioningConnectorConfig();
		System.out.println("defaultProvisioningConnectorConfig: ");
		System.out.println("\tname: "+defaultProvisioningConnectorConfig.getName());
		properties = defaultProvisioningConnectorConfig.getProvisioningProperties();
		System.out.println("\tprovisioningProperties: ");
		for(int j=0; j<properties.length; j++){
			System.out.println("\t\tname: " + properties[j].getName());
			System.out.println("\t\tvalue: " + properties[j].getValue());
			System.out.println("\t\tconfidential: " + properties[j].isConfidential());
			System.out.println("\t\tdefaultValue: " + properties[j].getDefaultValue());
			System.out.println("\t\tdisplayName: " + properties[j].getDisplayName());
			System.out.println("\t\trequired: " + properties[j].isRequired());
			System.out.println("\t\tdescription: " + properties[j].getDescription());
			System.out.println();
		}
		
		ClaimConfig claimConfig = identityProvider.getClaimConfig();
		System.out.println("claimConfig: ");
		System.out.println("\troleClaimURI: " + claimConfig.getRoleClaimURI());
		System.out.println("\tuserClaimURI: " + claimConfig.getUserClaimURI());
		System.out.println("\tlocalClaimDialect: " + claimConfig.isLocalClaimDialect());
		Claim[] idpClaims = claimConfig.getIdpClaims();
		
		System.out.println("\tidpClaims: ");
		
		for(int i=0; i<idpClaims.length; i++){
			System.out.println("\t\tclaimId: "+idpClaims[i].getClaimId());
			System.out.println("\t\tclaimURI: "+idpClaims[i].getClaimUri());
			System.out.println();
		}
		
		System.out.println("certificate: "+identityProvider.getCertificate());
		
		PermissionsAndRoleConfig permissionAndRoleConfig = identityProvider.getPermissionAndRoleConfig();
		System.out.println("permissionAndRoleConfig: ");
		ApplicationPermission[] permissions = permissionAndRoleConfig.getPermissions();
		System.out.println("\tpermissions: ");
		for(int i=0; i<permissions.length; i++){
			System.out.println("\t\tvalue: "+permissions[i].getValue());
			System.out.println();
		}		
	    RoleMapping[] roleMappings = permissionAndRoleConfig.getRoleMappings();
	    System.out.println("\troleMappings: ");
	    for(int i=0; i<roleMappings.length; i++){
			System.out.println("\t\tremoteRole: "+roleMappings[i].getRemoteRole());
			LocalRole localRole = roleMappings[i].getLocalRole();
			System.out.println("\t\tlocalRole: ");
			System.out.println("\t\t\tlocalRoleName: "+localRole.getLocalRoleName());
			System.out.println("\t\t\tuserStoreId: "+localRole.getUserStoreId());
			System.out.println();
		}
	    String[] idpRoles = permissionAndRoleConfig.getIdpRoles();
	    System.out.println("\t\tidpRoles: ");
	    for(int i=0; i<idpRoles.length;i++){
	    	System.out.println("\t\t\t" + idpRoles[i]);
	    }
		
	    System.out.println("justInTimeProvisioningConfig: ");
		JustInTimeProvisioningConfig justInTimeProvisioningConfig = identityProvider.getJustInTimeProvisioningConfig();
		System.out.println("\tuserStoreClaimUri: " + justInTimeProvisioningConfig.getUserStoreClaimUri());
		System.out.println("\tprovisioningUserStore: " + justInTimeProvisioningConfig.getProvisioningUserStore());
		System.out.println("\tprovisioningEnabled: " + justInTimeProvisioningConfig.isProvisioningEnabled());
	}
}
