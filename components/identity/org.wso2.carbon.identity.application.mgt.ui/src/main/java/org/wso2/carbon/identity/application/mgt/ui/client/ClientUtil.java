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
package org.wso2.carbon.identity.application.mgt.ui.client;

import org.wso2.carbon.identity.application.mgt.dto.xsd.ApplicationConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.xsd.AuthenticationStepConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.xsd.AuthenticatorConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.xsd.ClientConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.xsd.TrustedIDPConfigDTO;
import org.wso2.carbon.identity.application.mgt.ui.ApplicationConfigBean;
import org.wso2.carbon.identity.application.mgt.ui.OAuthOIDCAppConfig;
import org.wso2.carbon.identity.application.mgt.ui.SAMLSSOAppConfig;
import org.wso2.carbon.identity.application.mgt.ui.TrustedIDPConfig;

public class ClientUtil {

	public static final String SAML_SSO_CLIENT = "samlsso";
	public static final String OAUTH_OIDC_CLIENT = "oauth2";

	public static ApplicationConfigBean getApplicationConfigBean(ApplicationConfigDTO dto) {
		ApplicationConfigBean bean = new ApplicationConfigBean();

		// set identifier
		bean.setApplicationIdentifier(dto.getApplicatIdentifier());

		// set OAuth and SAML configs
		ClientConfigDTO[] clientConfigDTOs = dto.getClientConfig();
		if (clientConfigDTOs != null && clientConfigDTOs.length > 0) {
			for (ClientConfigDTO clientConfigDTO : clientConfigDTOs) {
				if (SAML_SSO_CLIENT.equals(clientConfigDTO.getType())) {
					SAMLSSOAppConfig ssoConfig = new SAMLSSOAppConfig();
					ssoConfig.setAcsUrl(clientConfigDTO.getCallbackUrl());
					ssoConfig.setIssuer(clientConfigDTO.getClientID());
					ssoConfig.setConsumerIndex(clientConfigDTO.getClientSecrete());
					bean.setSamlssoConfig(ssoConfig);

				} else if (OAUTH_OIDC_CLIENT.equals(clientConfigDTO.getType())) {
					OAuthOIDCAppConfig oauthConfig = new OAuthOIDCAppConfig();
					oauthConfig.setCallbackUrl(clientConfigDTO.getCallbackUrl());
					oauthConfig.setClientID(clientConfigDTO.getClientID());
					oauthConfig.setClientSecret(clientConfigDTO.getClientSecrete());
					bean.setOauthoidcConfig(oauthConfig);
				}
			}
		}

		// set IDPs
		AuthenticationStepConfigDTO[] stepDTOs = dto.getAuthenticationSteps();
		if (stepDTOs != null && stepDTOs.length > 0) {

			for (AuthenticationStepConfigDTO stepDTO : stepDTOs) {

				AuthenticatorConfigDTO[] authenDTOs = stepDTO.getAuthenticators();
				if (authenDTOs != null && authenDTOs.length > 0) {

					for (AuthenticatorConfigDTO authenDTO : authenDTOs) {

						TrustedIDPConfigDTO[] idpDTOs = authenDTO.getIdps();

						if (idpDTOs != null && idpDTOs.length > 0) {

							TrustedIDPConfig[] idpConfigs = new TrustedIDPConfig[idpDTOs.length];
							int i = 0;
							for (TrustedIDPConfigDTO idpDTO : idpDTOs) {
								idpConfigs[i] = new TrustedIDPConfig();
								idpConfigs[i].setIdpName(idpDTO.getIdpIdentifier());
								idpConfigs[i].setEndpointUrl(idpDTO.getEndpointsString());
								idpConfigs[i].setProtocols(idpDTO.getTypes());
								i++;
							}

							bean.setTrustedIdpConfig(idpConfigs);
						}
					}
				}
			}
		}

		return bean;
	}

	public static ApplicationConfigDTO getApplicationConfigDTO(ApplicationConfigBean bean) {

		ApplicationConfigDTO appConfigDTO = new ApplicationConfigDTO();

		appConfigDTO.setApplicatIdentifier(bean.getApplicationIdentifier());

		// set clients info
		ClientConfigDTO samlConfig = null;
		if (bean.getSamlssoConfig() != null) {
			samlConfig = new ClientConfigDTO();
			samlConfig.setClientID(bean.getSamlssoConfig().getIssuer());
			samlConfig.setClientSecrete(bean.getSamlssoConfig().getConsumerIndex());
			samlConfig.setCallbackUrl(bean.getSamlssoConfig().getAcsUrl());
			samlConfig.setType(ClientUtil.SAML_SSO_CLIENT);
		}
		ClientConfigDTO oauthOIDCConfig = null;
		if (bean.getOauthoidcConfig() != null) {
			oauthOIDCConfig = new ClientConfigDTO();
			oauthOIDCConfig.setClientID(bean.getOauthoidcConfig().getClientID());
			oauthOIDCConfig.setClientSecrete(bean.getOauthoidcConfig().getClientSecret());
			oauthOIDCConfig.setCallbackUrl(bean.getOauthoidcConfig().getCallbackUrl());
			oauthOIDCConfig.setType(ClientUtil.OAUTH_OIDC_CLIENT);
		}
		ClientConfigDTO[] clientConfig = null;
		if (bean.getSamlssoConfig() != null && bean.getOauthoidcConfig() != null) {
			clientConfig = new ClientConfigDTO[2];
			clientConfig[0] = samlConfig;
			clientConfig[1] = oauthOIDCConfig;
		} else if (bean.getSamlssoConfig() != null) {
			clientConfig = new ClientConfigDTO[1];
			clientConfig[0] = samlConfig;
		} else if (bean.getOauthoidcConfig() != null) {
			clientConfig = new ClientConfigDTO[1];
			clientConfig[0] = oauthOIDCConfig;
		}
		appConfigDTO.setClientConfig(clientConfig);

		TrustedIDPConfigDTO[] idps = null;
		TrustedIDPConfig[] trustedIDPConfig = bean.getTrustedIdpConfig();
		if (trustedIDPConfig != null) {
			idps = new TrustedIDPConfigDTO[trustedIDPConfig.length];
			int i = 0;

			for (TrustedIDPConfig trustedIDP : trustedIDPConfig) {

				TrustedIDPConfigDTO idpConfig = new TrustedIDPConfigDTO();
				idpConfig.setIdpIdentifier(trustedIDP.getIdpName());
				idpConfig.setEndpointsString(trustedIDP.getEndpointUrl());
				idpConfig.setTypesString("DEFAULT");
				idps[i++] = idpConfig;
			}
		}

		// Authenticators
		AuthenticatorConfigDTO authenticator = new AuthenticatorConfigDTO();
		authenticator.setAuthnticatorIdentifier("DEFAULT");
		authenticator.setIdps(idps);

		AuthenticatorConfigDTO[] authnticators = new AuthenticatorConfigDTO[1];
		authnticators[0] = authenticator;

		// Steps
		AuthenticationStepConfigDTO authnStep = new AuthenticationStepConfigDTO();
		authnStep.setStepIdentifier("DEFAULT");
		authnStep.setAuthenticators(authnticators);

		// set stpes info
		AuthenticationStepConfigDTO[] authnSteps = new AuthenticationStepConfigDTO[1];
		authnSteps[0] = authnStep;

		appConfigDTO.setAuthenticationSteps(authnSteps);

		return appConfigDTO;
	}

}
