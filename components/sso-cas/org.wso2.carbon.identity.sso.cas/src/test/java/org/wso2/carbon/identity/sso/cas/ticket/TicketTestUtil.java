package org.wso2.carbon.identity.sso.cas.ticket;

import org.wso2.carbon.identity.application.common.model.ServiceProvider;

public class TicketTestUtil {
	private static final String SERVICE_PROVIDER_NAME = "DummyServiceProvider";
	private static final String SERVICE_PROVIDER_URL = "https://dummy.url/endpoint";
	private static final String TEST_PRINCIPAL = "Test";
	private static final String TEST_SESSION_DATA_KEY = "012345678";
	
	public static ServiceProvider getServiceProvider() {
		ServiceProvider fakeServiceProvider = new ServiceProvider();
		
		fakeServiceProvider.setApplicationName(SERVICE_PROVIDER_NAME);
		
		return fakeServiceProvider;
	}
	
	public static String getServiceProviderUrl() {
		return SERVICE_PROVIDER_URL;
	}
	
	public static String getPrincipal() {
		return TEST_PRINCIPAL;
	}
	
	public static String getSessionDataKey() {
		return TEST_SESSION_DATA_KEY;
	}
	
	public static ServiceTicket buildServiceTicket() {
		ServiceTicket serviceTicket = 
				new ServiceTicket(
						getServiceProvider(),
						buildTicketGrantingTicket(getSessionDataKey(), getPrincipal()),
						getServiceProviderUrl(),
						false,
						false
				);
		
		return serviceTicket;
	}
	
	public static ServiceTicket buildServiceTicket(ServiceProvider serviceProvider, TicketGrantingTicket ticketGrantingTicket, String serviceProviderUrl, boolean proxyRequest, boolean samlLogin) {
		ServiceTicket serviceTicket = 
				new ServiceTicket(
						serviceProvider,
						ticketGrantingTicket,
						serviceProviderUrl,
						proxyRequest,
						samlLogin
				);
		
		return serviceTicket;
	}
	
	public static TicketGrantingTicket buildTicketGrantingTicket() {
		return new TicketGrantingTicket(getSessionDataKey(), getPrincipal());
	}
	
	public static TicketGrantingTicket buildTicketGrantingTicket(String sessionDataKey, String principal) {
		return new TicketGrantingTicket(sessionDataKey, principal);
	}

	public static TicketGrantingTicket buildProxyGrantingTicket() {
		return new TicketGrantingTicket(getSessionDataKey(), getPrincipal(), true);
	}
	
	public static TicketGrantingTicket buildProxyGrantingTicket(String sessionDataKey, String principal) {
		return new TicketGrantingTicket(sessionDataKey, principal, true);
	}
}
