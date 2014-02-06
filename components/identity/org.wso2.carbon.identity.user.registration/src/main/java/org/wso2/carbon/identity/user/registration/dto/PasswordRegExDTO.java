package org.wso2.carbon.identity.user.registration.dto;

public class PasswordRegExDTO {

	private String domainName;
	private String regEx;

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getRegEx() {
		return regEx;
	}

	public void setRegEx(String regEx) {
		this.regEx = regEx;
	}

}
