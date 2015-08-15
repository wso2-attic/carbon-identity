package org.wso2.carbon.identity.sso.cas.ticket;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.identity.sso.cas.util.CASSSOUtil;

@RunWith(PowerMockRunner.class)
public class TicketGrantingTicketTest {
	
	@Test
	public void testNullSessionDataKey() {
		final String sessionDataKey = null;
		final String principal = TicketTestUtil.getPrincipal();
		
		final TicketGrantingTicket ticket = TicketTestUtil.buildTicketGrantingTicket(sessionDataKey, principal);
		
		Assert.assertNotNull(ticket);
		Assert.assertNull(ticket.getSessionDataKey());
	}
	
	@Test
	public void testEquals() {
		final TicketGrantingTicket ticket = TicketTestUtil.buildTicketGrantingTicket();
		
		// Idempotence
		Assert.assertTrue(ticket.equals(ticket));
	}
	
	@Test
	public void testNotEquals() {		
		final TicketGrantingTicket ticket = TicketTestUtil.buildProxyGrantingTicket();
		
		// Different ID
		final TicketGrantingTicket comparison = TicketTestUtil.buildProxyGrantingTicket();
		
		Assert.assertFalse(ticket.equals(comparison));		
	}
	
	@Test
	public void testNullPrincipal() {
		final String sessionDataKey = TicketTestUtil.getSessionDataKey();
		final String principal = null;
		
		final TicketGrantingTicket ticket = TicketTestUtil.buildTicketGrantingTicket(sessionDataKey, principal);
		
		Assert.assertNotNull(ticket);
		Assert.assertNull(ticket.getPrincipal());
	}
	
	@Test
	public void testNonProxyRequest() {	
		final TicketGrantingTicket ticket = TicketTestUtil.buildTicketGrantingTicket();
		
		Assert.assertNotNull(ticket);
		Assert.assertTrue(ticket.getId().startsWith(TicketConstants.TICKET_GRANTING_TICKET_PREFIX));
	}
	
	@Test
	public void testProxyRequest() {
		final TicketGrantingTicket ticket = TicketTestUtil.buildProxyGrantingTicket();
		
		Assert.assertNotNull(ticket);
		Assert.assertTrue(ticket.getId().startsWith(TicketConstants.PROXY_GRANTING_TICKET_PREFIX));
	}
	
	@Test
	public void testExpiration() {
		final TicketGrantingTicket ticket = TicketTestUtil.buildTicketGrantingTicket();
		
		Assert.assertNotNull(ticket);
		Assert.assertFalse(ticket.isExpired());
		
		ticket.expire();
		
		Assert.assertTrue(ticket.isExpired());
	}
	
	@PrepareForTest({ CASSSOUtil.class })
	@Test
	public void testGrantServiceTicket() throws Exception {
		final boolean isSamlLogin = false;
		
		final TicketGrantingTicket ticket = TicketTestUtil.buildTicketGrantingTicket();
	
		Assert.assertNotNull(ticket);
		
		PowerMockito.mockStatic(CASSSOUtil.class);

		PowerMockito.doNothing().when(CASSSOUtil.class, "storeServiceTicket", TicketTestUtil.buildServiceTicket());
		
		final ServiceTicket serviceTicket = ticket.grantServiceTicket(
				TicketTestUtil.getServiceProvider(), 
				TicketTestUtil.getServiceProviderUrl(), 
				isSamlLogin);
	
		Assert.assertNotNull(serviceTicket);
		Assert.assertTrue(serviceTicket.getOriginalUrl().equals(TicketTestUtil.getServiceProviderUrl()));
		Assert.assertFalse(serviceTicket.hasProxy());
		Assert.assertEquals(ticket, serviceTicket.getParentTicket());
	}
	
	@PrepareForTest({ CASSSOUtil.class })
	@Test
	public void testGrantProxyTicket() throws Exception {
		final boolean isSamlLogin = false;
		
		final TicketGrantingTicket ticket = TicketTestUtil.buildProxyGrantingTicket();
	
		Assert.assertNotNull(ticket);
		
		PowerMockito.mockStatic(CASSSOUtil.class);

		PowerMockito.doNothing().when(CASSSOUtil.class, "storeServiceTicket", TicketTestUtil.buildServiceTicket());
		
		final ServiceTicket proxyTicket = ticket.grantServiceTicket(
				TicketTestUtil.getServiceProvider(), 
				TicketTestUtil.getServiceProviderUrl(), 
				isSamlLogin);
	
		Assert.assertNotNull(proxyTicket);
		Assert.assertTrue(proxyTicket.getOriginalUrl().equals(TicketTestUtil.getServiceProviderUrl()));
		Assert.assertTrue(proxyTicket.hasProxy());
		Assert.assertEquals(ticket, proxyTicket.getParentTicket());
	}
}
