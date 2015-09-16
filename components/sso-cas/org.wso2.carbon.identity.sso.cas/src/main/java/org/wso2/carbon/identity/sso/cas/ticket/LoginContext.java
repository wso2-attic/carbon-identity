package org.wso2.carbon.identity.sso.cas.ticket;

import java.io.Serializable;

/**
 * Context object for CAS login that holds the original request properties
 */
public class LoginContext implements Serializable {

	private static final long serialVersionUID = -6948514313722223441L;

	private String redirectUrl;
	private boolean samlLogin;
	private boolean loginComplete;
	private boolean forceLogin;
	private boolean passiveLogin;
	
	public boolean isSAMLLogin() {
		return samlLogin;
	}
	
	public boolean isLoginComplete() {
		return loginComplete;
	}
	
	public boolean isForcedLogin() {
		return forceLogin;
	}
	
	public boolean isPassiveLogin() {
		return passiveLogin;
	}
	
	public String getRedirectUrl() {
		return redirectUrl;
	}
	
	public void setForcedLogin(boolean value) {
		forceLogin = value;
	}
	
	public void setPassiveLogin(boolean value) {
		passiveLogin = value;
	}
	
	public void setLoginComplete(boolean value) {
		loginComplete = value;
	}
	
	public void setSAMLLogin(boolean value) {
		samlLogin = value;
	}
	
	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}
}