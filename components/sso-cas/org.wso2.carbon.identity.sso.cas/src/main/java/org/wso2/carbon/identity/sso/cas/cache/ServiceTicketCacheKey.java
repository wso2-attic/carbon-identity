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

import org.wso2.carbon.identity.application.common.cache.CacheKey;

public class ServiceTicketCacheKey extends CacheKey {

	private static final long serialVersionUID = 5017134072656604372L;
	
	private String serviceTicketId;

    public ServiceTicketCacheKey(String serviceTicketId) {
        this.serviceTicketId = serviceTicketId;
    }

    public String getSessionDataKey() {
        return serviceTicketId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serviceTicketId == null) ? 0 : serviceTicketId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ServiceTicketCacheKey other = (ServiceTicketCacheKey) obj;
        if (serviceTicketId == null) {
            if (other.serviceTicketId != null)
                return false;
        } else if (!serviceTicketId.equals(other.serviceTicketId))
            return false;
        return true;
    }

}
