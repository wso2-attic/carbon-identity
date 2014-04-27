package org.wso2.carbon.identity.application.authentication.framework.context;

import org.wso2.carbon.identity.application.authentication.framework.config.dto.SequenceConfig;

public class SessionContext {
	
	String authenticatedUser;
	SequenceConfig authenticatedSequence;
	
	public String getAuthenticatedUser() {
		return authenticatedUser;
	}
	
	public void setAuthenticatedUser(String authenticatedUser) {
		this.authenticatedUser = authenticatedUser;
	}
	
	public SequenceConfig getAuthenticatedSequence() {
		return authenticatedSequence;
	}
	
	public void setAuthenticatedSequence(SequenceConfig authenticatedSequence) {
		this.authenticatedSequence = authenticatedSequence;
	}
}
