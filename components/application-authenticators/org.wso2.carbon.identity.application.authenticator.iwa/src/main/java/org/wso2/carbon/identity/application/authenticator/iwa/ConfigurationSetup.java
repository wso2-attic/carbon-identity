package org.wso2.carbon.identity.application.authenticator.iwa;

public final class ConfigurationSetup {

	//file names
	public static void setSystemProperties(String prop1, String prop2) {

		System.setProperty("java.security.auth.login.config",prop1);
		System.setProperty("java.security.auth.krb5.conf",prop2);
		
	}
	
	
	
}
