package org.wso2.carbon.identity.sso.cas.ticket;

import org.junit.Assert;
import org.junit.Test;

public class SAMLTicketIdGeneratorTest {
	@Test
	public void testStringPrefix() {
		final String serviceProviderUrl = TicketTestUtil.getServiceProviderUrl();
				
		final String ticketId = SAMLTicketIdGenerator.generate(serviceProviderUrl);
		
		Assert.assertNotNull(ticketId);
		Assert.assertTrue(ticketId.startsWith("AAEMM"));
	}
	
	@Test(expected=IllegalStateException.class)
	public void testNullPrefix() {				
		SAMLTicketIdGenerator.generate(null);
	}
}
