package org.wso2.carbon.identity.application.common.model;

public class ProvisioningProperty {

	private String name;
	private String value;
	private String base64EncodedStringValue;
	private boolean isConfidential;
	private String defaultValue;

	public ProvisioningProperty() {

	}

	/**
	 * 
	 * @return
	 */
	public String getValue() {
		return value;
	}

	/**
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConfidential() {
		return isConfidential;
	}

	/**
	 * 
	 * @param isConfidential
	 */
	public void setConfidential(boolean isConfidential) {
		this.isConfidential = isConfidential;
	}

	/**
	 * 
	 * @return
	 */
	public String getBase64EncodedStringValue() {
		return base64EncodedStringValue;
	}

	/**
	 * 
	 * @param base64EncodedStringValue
	 */
	public void setBase64EncodedStringValue(String base64EncodedStringValue) {
		this.base64EncodedStringValue = base64EncodedStringValue;
	}

	/**
	 * 
	 * @return
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * 
	 * @param defaultValue
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

}
