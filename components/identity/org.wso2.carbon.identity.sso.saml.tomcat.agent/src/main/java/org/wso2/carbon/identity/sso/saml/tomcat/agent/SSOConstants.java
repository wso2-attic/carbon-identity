/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.identity.sso.saml.tomcat.agent;

/**
 * 
 */
public class SSOConstants {
    
	public static final int SESSION_EXPIRE_TIME = 1000 * 60 * 60;
	public static final String HTTP_POST_PARAM_SAML2_AUTH_REQ = "SAMLRequest";
	public static final String HTTP_POST_PARAM_SAML2_RESP = "SAMLResponse";
	public static final String IDP_SESSION = "IdPSession";
	public static final String LAST_ACCESSED_TIME = "lastAccessed";
	public static final String AUTHENTICATED = "authenticated";
}
