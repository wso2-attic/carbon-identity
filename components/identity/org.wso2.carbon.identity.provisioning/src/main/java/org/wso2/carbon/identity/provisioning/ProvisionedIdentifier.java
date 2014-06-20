package org.wso2.carbon.identity.provisioning;

import java.io.Serializable;

public class ProvisionedIdentifier implements Serializable{

	/**
     * 
     */
    private static final long serialVersionUID = -6599321170580333389L;
    private String identifier;

	/**
	 * 
	 * @return
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * 
	 * @param identifier
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
