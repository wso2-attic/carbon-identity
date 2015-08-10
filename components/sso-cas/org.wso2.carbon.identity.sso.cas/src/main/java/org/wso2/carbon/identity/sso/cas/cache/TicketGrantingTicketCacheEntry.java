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
package org.wso2.carbon.identity.sso.cas.cache;

import org.wso2.carbon.identity.application.common.cache.CacheEntry;
import org.wso2.carbon.identity.sso.cas.ticket.TicketGrantingTicket;

public class TicketGrantingTicketCacheEntry extends CacheEntry {

	private static final long serialVersionUID = 3718131375421998556L;
	
	private TicketGrantingTicket ticketGrantingTicket;

    public TicketGrantingTicket getTicketGrantingTicket() {
        return ticketGrantingTicket;
    }

    public void setTicketGrantingTicket(TicketGrantingTicket ticketGrantingTicket) {
        this.ticketGrantingTicket = ticketGrantingTicket;
    }
}