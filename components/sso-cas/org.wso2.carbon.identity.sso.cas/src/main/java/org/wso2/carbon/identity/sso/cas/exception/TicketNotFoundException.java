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
package org.wso2.carbon.identity.sso.cas.exception;

public class TicketNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -4493414620968412238L;
	
	private String requestedTicket;

	public TicketNotFoundException() {
        super();
    }

    public TicketNotFoundException(String message, String ticket) {
        super(message);
        requestedTicket = ticket;
    }

    public TicketNotFoundException(String message, String ticket, Throwable cause) {
        super(message, cause);
        requestedTicket = ticket;
    }
    
    public String getRequestedTicket() {
    	return requestedTicket;
    }
}
