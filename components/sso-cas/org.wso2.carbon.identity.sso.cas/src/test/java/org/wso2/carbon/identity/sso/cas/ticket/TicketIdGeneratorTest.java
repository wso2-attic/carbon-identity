package org.wso2.carbon.identity.sso.cas.ticket;

import org.junit.Assert;
import org.junit.Test;

public class TicketIdGeneratorTest {
	
	@Test
	public void testStringPrefix() {
		final String prefix = "TEST";
				
		final String ticketId = TicketIdGenerator.generate(prefix);
		
		Assert.assertNotNull(ticketId);
		Assert.assertTrue(ticketId.startsWith(prefix));
	}
	
	@Test
	public void testNullPrefix() {
		final String prefix = null;
		final String expected = "UNKNOWN";
				
		final String ticketId = TicketIdGenerator.generate(prefix);
		
		Assert.assertNotNull(ticketId);
		Assert.assertTrue(ticketId.startsWith(expected));
	}
}
