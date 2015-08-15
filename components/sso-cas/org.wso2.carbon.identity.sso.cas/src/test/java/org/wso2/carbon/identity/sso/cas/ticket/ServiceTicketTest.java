package org.wso2.carbon.identity.sso.cas.ticket;

import org.junit.Assert;
import org.junit.Test;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;

public class ServiceTicketTest {
	@Test
	public void testNullServiceProvider() {
		final ServiceProvider serviceProvider = null;		
		final TicketGrantingTicket ticketGrantingTicket = TicketTestUtil.buildTicketGrantingTicket();
		final String serviceProviderUrl = TicketTestUtil.getServiceProviderUrl();
		final boolean isProxyRequest = false;
		final boolean isSamlLogin = false;
		
		final ServiceTicket ticket = TicketTestUtil.buildServiceTicket(serviceProvider, ticketGrantingTicket, serviceProviderUrl, isProxyRequest, isSamlLogin);
		
		Assert.assertNotNull(ticket);
		Assert.assertNull(ticket.getService());
		Assert.assertFalse(ticket.hasProxy());
		Assert.assertEquals(ticketGrantingTicket, ticket.getParentTicket());
		Assert.assertEquals(serviceProviderUrl, ticket.getOriginalUrl());
	}
	
	@Test
	public void testNullTicketGrantingTicket() {
		final ServiceProvider serviceProvider = TicketTestUtil.getServiceProvider();		
		final TicketGrantingTicket ticketGrantingTicket = null;
		final String serviceProviderUrl = TicketTestUtil.getServiceProviderUrl();
		final boolean isProxyRequest = false;
		final boolean isSamlLogin = false;
		
		final ServiceTicket ticket = TicketTestUtil.buildServiceTicket(serviceProvider, ticketGrantingTicket, serviceProviderUrl, isProxyRequest, isSamlLogin);
		
		Assert.assertNotNull(ticket);
		Assert.assertNull(ticket.getParentTicket());
		Assert.assertFalse(ticket.hasProxy());
		Assert.assertEquals(serviceProvider, ticket.getService());
		Assert.assertEquals(serviceProviderUrl, ticket.getOriginalUrl());
	}
	
	@Test
	public void testNullServiceProviderUrl() {
		final ServiceProvider serviceProvider = TicketTestUtil.getServiceProvider();		
		final TicketGrantingTicket ticketGrantingTicket = TicketTestUtil.buildTicketGrantingTicket();
		final String serviceProviderUrl = null;
		final boolean isProxyRequest = false;
		final boolean isSamlLogin = false;
		
		final ServiceTicket ticket = TicketTestUtil.buildServiceTicket(serviceProvider, ticketGrantingTicket, serviceProviderUrl, isProxyRequest, isSamlLogin);
		
		Assert.assertNotNull(ticket);
		Assert.assertNull(ticket.getOriginalUrl());
		Assert.assertFalse(ticket.hasProxy());
		Assert.assertEquals(ticketGrantingTicket, ticket.getParentTicket());
		Assert.assertEquals(serviceProvider, ticket.getService());
	}
	
	@Test
	public void testProxyRequest() {
		final ServiceProvider serviceProvider = TicketTestUtil.getServiceProvider();		
		final TicketGrantingTicket ticketGrantingTicket = TicketTestUtil.buildProxyGrantingTicket();
		final String serviceProviderUrl = TicketTestUtil.getServiceProviderUrl();
		final boolean isProxyRequest = true;
		final boolean isSamlLogin = false;
		
		final ServiceTicket ticket = TicketTestUtil.buildServiceTicket(serviceProvider, ticketGrantingTicket, serviceProviderUrl, isProxyRequest, isSamlLogin);
		
		Assert.assertNotNull(ticket);
		Assert.assertTrue(ticket.hasProxy());
		Assert.assertEquals(ticketGrantingTicket, ticket.getParentTicket());
		Assert.assertEquals(serviceProvider, ticket.getService());
		Assert.assertEquals(serviceProviderUrl, ticket.getOriginalUrl());
	}
	
	@Test
	public void testSamlLogin() {
		final ServiceProvider serviceProvider = TicketTestUtil.getServiceProvider();		
		final TicketGrantingTicket ticketGrantingTicket = TicketTestUtil.buildProxyGrantingTicket();
		final String serviceProviderUrl = TicketTestUtil.getServiceProviderUrl();
		final boolean isProxyRequest = false;
		final boolean isSamlLogin = true;
		
		final ServiceTicket ticket = TicketTestUtil.buildServiceTicket(serviceProvider, ticketGrantingTicket, serviceProviderUrl, isProxyRequest, isSamlLogin);
		
		Assert.assertNotNull(ticket);
		Assert.assertNotNull(ticket.getId());
		Assert.assertFalse(ticket.getId().startsWith("UNKNOWN"));
		Assert.assertFalse(ticket.getId().startsWith(TicketConstants.TICKET_GRANTING_TICKET_PREFIX));
		Assert.assertFalse(ticket.getId().startsWith(TicketConstants.PROXY_GRANTING_TICKET_PREFIX));
		Assert.assertFalse(ticket.hasProxy());
		Assert.assertEquals(ticketGrantingTicket, ticket.getParentTicket());
		Assert.assertEquals(serviceProvider, ticket.getService());
		Assert.assertEquals(serviceProviderUrl, ticket.getOriginalUrl());
	}
}
