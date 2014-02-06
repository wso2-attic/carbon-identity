package org.wso2.carbon.identity.application.authentication.framework.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.idp.mgt.IdentityProviderMgtService;
import org.wso2.carbon.idp.mgt.dto.TrustedIdPDTO;
import org.wso2.carbon.idp.mgt.exception.IdentityProviderMgtException;

public class ConfigurationFacade {
	
	private static Log log = LogFactory.getLog(ConfigurationFacade.class);
	
	private static volatile ConfigurationFacade instance;
	
	public ConfigurationFacade() {
		//Read the default config from the files
		FileBasedConfigurationBuilder.getInstance().build();
	}
	
	public static ConfigurationFacade getInstance() {
		
		if (instance == null) {
			synchronized (ConfigurationFacade.class) {
				
				if (instance == null) {
					instance = new ConfigurationFacade();
				}
			}
		}
		
		return instance;
	}
	
	public SequenceConfig getSequenceConfig(String reqType, String relyingParty) {
		
		SequenceConfig sequenceConfig = null;
		
		if (relyingParty != null) {
			//Get SP config from SP Management component
			//Get IdPs from IdP Management component
			sequenceConfig = SPConfigurationBuilder.getInstance().getConfiguration(reqType, relyingParty);
			
			if (sequenceConfig == null || (sequenceConfig.getStepMap() == null || sequenceConfig.getStepMap().isEmpty())) {
				sequenceConfig = FileBasedConfigurationBuilder.getInstance().findSequenceByRelyingParty(relyingParty);
			}
		} 
		
		if (sequenceConfig == null) { 
			//load default config from file
			sequenceConfig = FileBasedConfigurationBuilder.getInstance().findSequenceByRelyingParty("default");
		}
		
		return sequenceConfig;
	}
	
	public ExternalIdPConfig getIdPConfig(int step, String authenticator, String idpName) {
		
		//TODO check whether ExternalIdPConfig already exists for the authenticator.
		//if yes get that config. Else call IdentityProviderMgtService and get
		
		ExternalIdPConfig externalIdPConfig = null;
		IdentityProviderMgtService idpMgtService = new IdentityProviderMgtService();
		TrustedIdPDTO trustedIdPDTO = null;
		
		try {
			trustedIdPDTO = idpMgtService.getIdPByName(idpName);
		} catch (IdentityProviderMgtException e) {
			log.error("Exception while calling IdentityProviderMgtService.getTenantIdP()", e);
		}
		
		if (trustedIdPDTO != null) {
			externalIdPConfig = new ExternalIdPConfig(trustedIdPDTO);
		}
		
		return externalIdPConfig;
	}
	
	public String getAuthenticationEndpointURL() {
		return FileBasedConfigurationBuilder.getInstance().getAuthenticationEndpointURL();
	}
}
