/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.samples.entitlement.callback;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.wso2.carbon.identity.entitlement.mediator.EntitlementCallbackHandler;

public class EntitlementCallBack implements EntitlementCallbackHandler{
	
	private static final Log log = LogFactory.getLog(EntitlementCallBack.class);

	public String findOperationName(MessageContext synCtx) {
		log.info("EntitlementCallBack ::: findOperationName");
		return "echo";
	}

	public String findServiceName(MessageContext synCtx) {
		log.info("EntitlementCallBack ::: findServiceName");
		return "http://localhost:8280/services";
	}

	public String getUserName(SOAPEnvelope envelope) {
		log.info("EntitlementCallBack ::: getUserName");
		return "admin";
	}

}
