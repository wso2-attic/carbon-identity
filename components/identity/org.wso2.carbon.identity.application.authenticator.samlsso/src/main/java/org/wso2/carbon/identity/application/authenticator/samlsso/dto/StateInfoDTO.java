package org.wso2.carbon.identity.application.authenticator.samlsso.dto;

import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStateInfo;

public class StateInfoDTO extends AuthenticatorStateInfo {
	
	private String sessionIndex;
	private String subject;

	public String getSessionIndex() {
		return sessionIndex;
	}

	public void setSessionIndex(String sessionIndex) {
		this.sessionIndex = sessionIndex;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
}
