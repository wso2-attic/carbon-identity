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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class AbstractTicket implements Serializable {
	private static final long serialVersionUID = -2915000694712403246L;
	private static Log log = LogFactory.getLog(AbstractTicket.class);
	protected long creationTime;
	protected long lastTimeUsed;
    protected String uniqueId;
    // Possibly make this timeout threshold configurable
    private static int TIMEOUT_THRESHOLD = 1 * 60000; // 1 minute in milliseconds
    private boolean proxyRequest = false;
	   
	public AbstractTicket(String ticketSeed, boolean proxyRequest) {
        uniqueId = TicketIdGenerator.generate(ticketSeed);
		creationTime = System.currentTimeMillis();
		this.proxyRequest = proxyRequest;
		updateState();
		
		log.debug("CAS ticket created(proxy="+proxyRequest+"): "+getId());
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
    public String getId() {
    	return uniqueId;
    }
    
    public boolean hasProxy() {
    	return proxyRequest;
    }
    
    public boolean isExpired() {
    	return getLastTimeUsed() < (System.currentTimeMillis() + TIMEOUT_THRESHOLD); 
    }
	
	public long getLastTimeUsed() {
		return lastTimeUsed;
	}
	
	public void updateState() {
		lastTimeUsed = System.currentTimeMillis();
	}
}
