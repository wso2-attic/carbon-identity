package org.wso2.carbon.identity.application.authentication.framework.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.internal.ApplicationAuthenticationFrameworkServiceComponent;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.dto.ApplicationConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.AuthenticationStepConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.AuthenticatorConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.TrustedIDPConfigDTO;
import org.wso2.carbon.identity.base.IdentityException;

public class SPConfigurationBuilder {
	
	private static Log log = LogFactory.getLog(SPConfigurationBuilder.class);
	
	private static volatile SPConfigurationBuilder instance;
	
	public static SPConfigurationBuilder getInstance() {
		if (instance == null) {
			synchronized (ConfigurationFacade.class) {
				if (instance == null) {
					instance = new SPConfigurationBuilder();
				}
			}
		}
		
		return instance;
	}
	
	public SequenceConfig getConfiguration(String reqType, String sp) {
		
		SequenceConfig sequenceConfig = null;
		ApplicationManagementService appMgtService = new ApplicationManagementService();
		
		try {
			ApplicationConfigDTO appConfigDTO = appMgtService.getApplicationData(sp, reqType);
			
			if (appConfigDTO != null) {
				
				sequenceConfig = new SequenceConfig();
				AuthenticationStepConfigDTO[] authenticationStepConfigDTOs =  appConfigDTO.getAuthenticationSteps();
				int stepOrder = 0;
				
				for (AuthenticationStepConfigDTO authenticationStepConfigDTO : authenticationStepConfigDTOs) {
					
					StepConfig stepConfig = new StepConfig();
					
					try {
						stepOrder = new Integer(authenticationStepConfigDTO.getStepIdentifier());
					} catch (NumberFormatException e) {
						log.error("Exception while trying to convert the Step order to integer", e);
						stepOrder++;
					}
					
					stepConfig.setOrder(stepOrder);
					AuthenticatorConfigDTO[] authenticatorConfigDTOs = authenticationStepConfigDTO.getAuthenticators();
					
					for (AuthenticatorConfigDTO authenticatorConfigDTO : authenticatorConfigDTOs) {
						
						TrustedIDPConfigDTO[] trustedIDPConfigDTOs = authenticatorConfigDTO.getIdps();
						
						if (trustedIDPConfigDTOs == null || trustedIDPConfigDTOs.length == 0) {
							loadAuthenticatorsByIdP(stepConfig, "internal", "BasicAuthenticator");
						} else {
							for (TrustedIDPConfigDTO trustedIDPConfigDTO : trustedIDPConfigDTOs) {
								ExternalIdPConfig externalIdPConfig = ConfigurationFacade.getInstance().getIdPConfig(0, null, trustedIDPConfigDTO.getIdpIdentifier());
								
								if (externalIdPConfig.isSAML2SSOEnabled()) {
									loadAuthenticatorsByIdP(stepConfig, externalIdPConfig.getIdPName(), "SAMLSSOAuthenticator");
								} else if (externalIdPConfig.isOIDCEnabled()) {
									loadAuthenticatorsByIdP(stepConfig, externalIdPConfig.getIdPName(), "OpenIDConnectAuthenticator");
								}
							}
						}
						
//						AuthenticatorConfig authenticatorConfig = new AuthenticatorConfig();
//						String authenticatorName = authenticatorConfigDTO.getAuthnticatorIdentifier();
//						authenticatorConfig.setName(authenticatorName);
//						
//						for (ApplicationAuthenticator appAuthenticator : ApplicationAuthenticationFrameworkServiceComponent.authenticators) {
//							
//							if (authenticatorName.equalsIgnoreCase(appAuthenticator.getAuthenticatorName())) {
//								authenticatorConfig.setApplicationAuthenticator(appAuthenticator);
//								break;
//							}
//						}
//						
//						TrustedIDPConfigDTO[] trustedIDPConfigDTOs = authenticatorConfigDTO.getIdps();
//						
//						for (TrustedIDPConfigDTO trustedIDPConfigDTO : trustedIDPConfigDTOs) {
//							authenticatorConfig.getIdpList().add(trustedIDPConfigDTO.getIdpIdentifier());
//						}
//						
//						stepConfig.getAuthenticatorList().add(authenticatorConfig);
					}
					
					sequenceConfig.getStepMap().put(stepOrder, stepConfig);
				}
			}
		} catch (IdentityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sequenceConfig;
	}
	
	private void loadAuthenticatorsByIdP(StepConfig stepConfig, String idpName, String authenticatorName) {
		
		AuthenticatorConfig authenticatorConfig = null;
		
		//check if authenticator already exists
		for (AuthenticatorConfig authConfig : stepConfig.getAuthenticatorList()) {
			
			if (authenticatorName.equals(authConfig.getName())) {
				authenticatorConfig = authConfig;
			}
		}
		
		if (authenticatorConfig == null) {
			authenticatorConfig = new AuthenticatorConfig();
			authenticatorConfig.setName(authenticatorName);
			
			for (ApplicationAuthenticator appAuthenticator : ApplicationAuthenticationFrameworkServiceComponent.authenticators) {	
				
				if (authenticatorName.equalsIgnoreCase(appAuthenticator.getAuthenticatorName())) {
					authenticatorConfig.setApplicationAuthenticator(appAuthenticator);
					break;
				}
			}
			
			stepConfig.getAuthenticatorList().add(authenticatorConfig);
		}
		
		if (idpName != null) {
			authenticatorConfig.getIdpList().add(idpName);
		}
	}
}
