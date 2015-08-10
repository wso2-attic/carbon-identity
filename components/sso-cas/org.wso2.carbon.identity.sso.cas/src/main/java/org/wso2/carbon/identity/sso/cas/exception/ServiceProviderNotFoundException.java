package org.wso2.carbon.identity.sso.cas.exception;

public class ServiceProviderNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 7338087587507461267L;
	private String serviceProvider;

	public ServiceProviderNotFoundException() {
        super();
    }

    public ServiceProviderNotFoundException(String serviceProvider) {
        super();
        this.serviceProvider = serviceProvider;
    }

    public ServiceProviderNotFoundException(String serviceProvider, Throwable cause) {
        super(cause);
        this.serviceProvider = serviceProvider;
    }
    
    public String getRequestedServiceProvider() {
    	return serviceProvider;
    }
}