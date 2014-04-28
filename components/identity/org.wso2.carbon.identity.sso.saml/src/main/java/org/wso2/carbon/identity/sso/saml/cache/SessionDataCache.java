/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.sso.saml.cache;

public class SessionDataCache extends BaseCache<CacheKey, CacheEntry> {

    private static final String SESSION_DATA_CACHE_NAME = "SAMLSSOSessionDataCache";
    private static volatile SessionDataCache instance;

    private SessionDataCache(String cacheName) {
        super(cacheName);
    }
    
    private SessionDataCache(String cacheName, int timeout) {
        super(cacheName, timeout);
    }

    public static SessionDataCache getInstance(int timeout) {
    	if (instance == null) {
    		synchronized (SessionDataCache.class) {
    			
				if (instance == null) {
					instance = new SessionDataCache(SESSION_DATA_CACHE_NAME, timeout);
				}
			}
    	}
        return instance;
    }
}
