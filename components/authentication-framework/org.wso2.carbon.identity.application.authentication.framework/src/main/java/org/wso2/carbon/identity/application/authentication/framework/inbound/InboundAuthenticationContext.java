package org.wso2.carbon.identity.application.authentication.framework.inbound;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class InboundAuthenticationContext implements Serializable {

	private static final long serialVersionUID = -3113147804821962230L;

	private InboundAuthenticationRequest authenticationRequest;
	private String tenantDomain;
	private Map<String, Object> properties = new HashMap<String, Object>();

	public InboundAuthenticationRequest getAuthenticationRequest() {
		return authenticationRequest;
	}

	public void setAuthenticationRequest(InboundAuthenticationRequest authenticationRequest) {
		this.authenticationRequest = authenticationRequest;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getTenantDomain() {
		return tenantDomain;
	}

	public void setTenantDomain(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}
}
