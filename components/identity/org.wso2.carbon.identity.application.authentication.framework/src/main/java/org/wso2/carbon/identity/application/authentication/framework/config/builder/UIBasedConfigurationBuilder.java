/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.application.authentication.framework.config.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.ApplicationConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.dto.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.internal.FrameworkServiceComponent;
import org.wso2.carbon.identity.application.common.model.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticator;
import org.wso2.carbon.identity.application.common.model.FederatedIdentityProvider;
import org.wso2.carbon.identity.application.common.model.LocalAuthenticator;
import org.wso2.carbon.identity.application.common.model.RequestPathAuthenticator;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationInfoProvider;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.idp.mgt.IdentityProviderManager;

public class UIBasedConfigurationBuilder {
	
	private static Log log = LogFactory.getLog(UIBasedConfigurationBuilder.class);
	
	private static volatile UIBasedConfigurationBuilder instance;
	
	public static UIBasedConfigurationBuilder getInstance() {
		if (instance == null) {
			synchronized (ConfigurationFacade.class) {
				if (instance == null) {
					instance = new UIBasedConfigurationBuilder();
				}
			}
		}
		
		return instance;
	}
	
	public SequenceConfig getSequence(String reqType, String clientId, String tenantDomain) throws FrameworkException {
		
		SequenceConfig sequenceConfig = null;
		ApplicationInfoProvider appInfo =  ApplicationInfoProvider.getInstance();
			
		try {
			ServiceProvider serviceProvider = appInfo.getServiceProviderByClienId(clientId, reqType, tenantDomain);
		    
			if (serviceProvider == null) {
				String errorMessage = "No Service Provider found for the Inbound clientID " + clientId + " of type " + reqType;
				log.error(errorMessage);
				throw new FrameworkException(errorMessage);
			}

			sequenceConfig = new SequenceConfig();
			sequenceConfig.setApplicationId(serviceProvider.getApplicationName());
			sequenceConfig.setApplicationConfig(new ApplicationConfig(serviceProvider));
			
			// setting request path authenticators
			if(serviceProvider.getRequestPathAuthenticators() != null && serviceProvider.getRequestPathAuthenticators().length > 0) {
								
				List<AuthenticatorConfig> requestPathAuthenticators = new ArrayList<AuthenticatorConfig>();
				RequestPathAuthenticator[] reqAuths = serviceProvider.getRequestPathAuthenticators();
								
				// for each request path authenticator
				for(RequestPathAuthenticator reqAuth : reqAuths) {
					AuthenticatorConfig authConfig = new AuthenticatorConfig();
					String authenticatorName = getAuthenticatorName(reqAuth.getName());
					authConfig.setName(authenticatorName);
					authConfig.setEnabled(true);
									
					// iterate through each system authentication config
					for (ApplicationAuthenticator appAuthenticator : FrameworkServiceComponent.authenticators) {	
										
						if (authenticatorName.equalsIgnoreCase(appAuthenticator.getAuthenticatorName())) {
							authConfig.setApplicationAuthenticator(appAuthenticator);
							break;
						}
					}
					requestPathAuthenticators.add(authConfig);
				}
				sequenceConfig.setReqPathAuthenticators(requestPathAuthenticators);
			}
					
            IdentityProviderManager idpManager = IdentityProviderManager.getInstance();
            AuthenticationStep[] authenticationSteps = serviceProvider
                    .getLocalAndOutBoundAuthenticationConfig().getAuthenticationSteps();
            int stepOrder = 0;

			if (authenticationSteps == null) {
				return sequenceConfig;
			}
			
			//for each configured step
			for (AuthenticationStep authenticationStep : authenticationSteps) {
				
				try {
					stepOrder = new Integer(authenticationStep.getStepOrder());
				} catch (NumberFormatException e) {
					stepOrder++;
				}
				
				//create a step configuration object
                StepConfig stepConfig = new StepConfig();
                stepConfig.setOrder(stepOrder);
                
                // loading Federated Authenticators
                FederatedIdentityProvider[] federatedIDPs = authenticationStep.getFederatedIdentityProviders();
                
				if (federatedIDPs != null) {
					// for each idp in the step
					for (FederatedIdentityProvider federatedIDP : federatedIDPs) {
						FederatedAuthenticator[] federatedAuthenticators = federatedIDP.getFederatedAuthenticators();

						// for each authenticator in the idp
						for (FederatedAuthenticator federatedAuthenticator : federatedAuthenticators) {
							
							String actualAuthenticatorName = getAuthenticatorName(federatedAuthenticator.getName());
							// assign it to the step
							loadStepAuthenticator(stepConfig, federatedIDP.getIdentityProviderName(), actualAuthenticatorName);
						}
					}
				}
				
				// load local authenticators
				LocalAuthenticator[] localAuthenticators = authenticationStep.getLocalAuthenticators();
				if(localAuthenticators != null) {
					// assign it to the step
					for(LocalAuthenticator localAuthenticator : localAuthenticators) {
						String actualAuthenticatorName = getAuthenticatorName(localAuthenticator.getName());
						loadStepAuthenticator(stepConfig, null, actualAuthenticatorName);
					}
				}
				
				sequenceConfig.getStepMap().put(stepOrder, stepConfig);
			}
		} catch (IdentityException e) {
			String msg = "While trying to retrieve the SP config";
			log.error(msg, e);
			throw new FrameworkException(msg, e);
		}
		
		return sequenceConfig;
	}
	
	private void loadStepAuthenticator(StepConfig stepConfig, String idpName, String authenticatorName) {
		
		AuthenticatorConfig authenticatorConfig = null;
		
		//check if authenticator already exists
		for (AuthenticatorConfig authConfig : stepConfig.getAuthenticatorList()) {
			
			if (authenticatorName.equals(authConfig.getName())) {
				authenticatorConfig = authConfig;
				break;
			}
		}
		
		if (authenticatorConfig == null) {
			authenticatorConfig = new AuthenticatorConfig();
			authenticatorConfig.setName(authenticatorName);
			
			for (ApplicationAuthenticator appAuthenticator : FrameworkServiceComponent.authenticators) {	
				
				if (authenticatorName.equalsIgnoreCase(appAuthenticator.getAuthenticatorName())) {
					authenticatorConfig.setApplicationAuthenticator(appAuthenticator);
					break;
				}
			}
			
			stepConfig.getAuthenticatorList().add(authenticatorConfig);
		}
		
		if (idpName != null) {
			authenticatorConfig.getIdps().add(idpName);
		}
	}
	
	private String getAuthenticatorName(String authentication) {

		// map authenticator name with the actual authenticator name
		Map<String, String> nameMappings = ConfigurationFacade.getInstance()
				.getAuthenticatorNameMappings();
		String actualAuthenticatorName = nameMappings.get(authentication);
		// assign defaults if unable to find the mapping from the config file
		if (actualAuthenticatorName == null) {
			if (authentication.equalsIgnoreCase("samlsso")) {
				actualAuthenticatorName = "SAMLSSOAuthenticator";
			} else if (authentication.equalsIgnoreCase("openidconnect")) {
				actualAuthenticatorName = "OpenIDConnectAuthenticator";
			} else if (authentication.equalsIgnoreCase("openid")) {
				actualAuthenticatorName = "OpenIDAuthenticator";
			} else if (authentication.equalsIgnoreCase("passive-sts")) {
				actualAuthenticatorName = "PassiveSTSAuthenticator";
			} else if (authentication.equalsIgnoreCase("basic")) {
				actualAuthenticatorName = "BasicAuthenticator";
			} else if (authentication.equalsIgnoreCase("facebook")) {
				actualAuthenticatorName = "FacebookAuthenticator";
			} else if (authentication.equalsIgnoreCase("oauth-bearer")) {
				actualAuthenticatorName = "BasicAuthRequestPathAuthenticator";
			} else if (authentication.equalsIgnoreCase("basic-auth")) {
				actualAuthenticatorName = "OAuthRequestPathAuthenticator";
			}

		}
		return actualAuthenticatorName;
	}
}
