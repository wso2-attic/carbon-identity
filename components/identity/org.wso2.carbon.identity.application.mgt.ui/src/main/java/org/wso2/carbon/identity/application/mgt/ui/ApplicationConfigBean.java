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
package org.wso2.carbon.identity.application.mgt.ui;

public class ApplicationConfigBean {

	private String applicationIdentifier = "";
	private String publicCertificate = null;
	private SAMLSSOAppConfig samlssoConfig = null;
	private OAuthOIDCAppConfig oauthoidcConfig = null;
	private TrustedIDPConfig[] trustedIdpConfig = null;
	private String[] identityproviders = null;
	private boolean isUpdating = false;

	public String getApplicationIdentifier() {
		return applicationIdentifier;
	}

	public void setApplicationIdentifier(String applicationIdentifier) {
		this.applicationIdentifier = applicationIdentifier;
	}

	public String getPublicCertificate() {
		return publicCertificate;
	}

	public void setPublicCertificate(String publicCertificate) {
		this.publicCertificate = publicCertificate;
	}

	public SAMLSSOAppConfig getSamlssoConfig() {
		return samlssoConfig;
	}

	public void setSamlssoConfig(SAMLSSOAppConfig samlssoConfig) {
		this.samlssoConfig = samlssoConfig;
	}

	public OAuthOIDCAppConfig getOauthoidcConfig() {
		return oauthoidcConfig;
	}

	public void setOauthoidcConfig(OAuthOIDCAppConfig oauthoidcConfig) {
		this.oauthoidcConfig = oauthoidcConfig;
	}

	public TrustedIDPConfig[] getTrustedIdpConfig() {
		return trustedIdpConfig;
	}

	public void setTrustedIdpConfig(TrustedIDPConfig[] trustedIdpConfig) {
		this.trustedIdpConfig = trustedIdpConfig;
	}

	public String[] getIdentityproviders() {
		return identityproviders;
	}

	public void setIdentityproviders(String[] identityproviders) {
		this.identityproviders = identityproviders;
	}

	public void addTrustedIDPConfig(TrustedIDPConfig idpConfig) {
		if (trustedIdpConfig == null) {
			trustedIdpConfig = new TrustedIDPConfig[1];
			trustedIdpConfig[0] = idpConfig;
		} else {
			TrustedIDPConfig[] newArray = new TrustedIDPConfig[trustedIdpConfig.length + 1];
			System.arraycopy(trustedIdpConfig, 0, newArray, 0, trustedIdpConfig.length);
			newArray[trustedIdpConfig.length] = idpConfig;
			trustedIdpConfig = newArray;
		}
	}
	
	public String getProtocolsString() {
		StringBuffer buff = new StringBuffer("");
		
		if(samlssoConfig != null) {
			buff.append("SAML ");
		}
		
		if(oauthoidcConfig != null) {
			buff.append("OAuth/OIDC ");
		}
		
		return buff.toString();
	}
	
	public String getTrustedIDPsString() {
		StringBuffer buff = new StringBuffer("");
		
		if(trustedIdpConfig != null && trustedIdpConfig.length > 0) {
			for(TrustedIDPConfig idpConf : trustedIdpConfig) {
				buff.append(idpConf.getIdpName() + " ");
			}
		}
		
		return buff.toString();
	}

	public boolean isUpdating() {
		return isUpdating;
	}

	public void setUpdating(boolean isUpdating) {
		this.isUpdating = isUpdating;
	}

}
