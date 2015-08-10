/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package org.wso2.carbon.identity.sso.cas.ticket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;

/**
 * CAS service tickets are associated to a service provider by a ticket-granting ticket and
 * can only be used once. In the case of a proxy request, the proxy ticket is associated to the
 * requested service provider.
 *
 */
public class ServiceTicket extends AbstractTicket {
	private static final long serialVersionUID = 3163737436365602898L;
	private static Log log = LogFactory.getLog(ServiceTicket.class);
	private TicketGrantingTicket ticketGrantingTicket;
	private ServiceProvider service;
	private String originalServiceProviderUrl;
	private boolean used = false;
	
	public ServiceTicket(ServiceProvider serviceProvider, TicketGrantingTicket parentTicket, String serviceProviderUrl, boolean proxyRequest, boolean samlLogin) {
		super( proxyRequest ? TicketConstants.PROXY_TICKET_PREFIX : TicketConstants.SERVICE_TICKET_PREFIX, proxyRequest);
		
		if( samlLogin) {
	        uniqueId = SAMLTicketIdGenerator.generate(serviceProviderUrl);
	        log.debug("updated ticket ID for SAML login: "+uniqueId);
		}
		
		originalServiceProviderUrl = serviceProviderUrl;
		
		ticketGrantingTicket = parentTicket;
		service = serviceProvider;
	}

	public TicketGrantingTicket getParentTicket() {
		return ticketGrantingTicket;
	}
	
	public ServiceProvider getService() {
		return service;
	}
	
	public String getOriginalUrl() {
		return originalServiceProviderUrl;
	}
	
	@Override
    public  boolean isExpired() {
        return used || super.isExpired();
    }
	
    @Override
    public void updateState() {
        super.updateState();
        used = true;
    }
}
