/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.ui.openid;

import org.openid4java.message.AuthSuccess;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.pape.PapeMessage;
import org.openid4java.message.sreg.SRegMessage;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.relyingparty.ui.openid.extensions.OpenIDAttributeExchange;
import org.wso2.carbon.identity.relyingparty.ui.openid.extensions.OpenIDExtension;
import org.wso2.carbon.identity.relyingparty.ui.openid.extensions.OpenIDPape;
import org.wso2.carbon.identity.relyingparty.ui.openid.extensions.OpenIDSimpleReg;

public class OpenIDExtensionFactory {

	private static OpenIDExtensionFactory factory = new OpenIDExtensionFactory();

	/**
	 * Make the class singleton
	 */
	private OpenIDExtensionFactory() {
	}

	/**
	 * Returns a singleton instance of OpenIDExtensionFactory
	 * 
	 * @return An instance of OpenIDExtensionFactory
	 */
	public static OpenIDExtensionFactory getInstance() {
		return factory;
	}

	/**
	 * Create an instance of the OpenIDExtension based on the OpenID request type
	 * 
	 * @param type Type of the OpenID request
	 * @return Appropriate OpenIDExtension instance
	 */
	public OpenIDExtension getExtension(String type) {

		if (IdentityConstants.OpenId.ATTRIBUTE_EXCHANGE.equals(type)) {
			return new OpenIDAttributeExchange();
		} else if (IdentityConstants.OpenId.SIMPLE_REGISTRATION.equals(type)) {
			return new OpenIDSimpleReg();
		} else if (IdentityConstants.OpenId.PAPE.equals(type)) {
			return new OpenIDPape();
		} else {
			return null;
		}
	}

	/**
	 * Create an instance of the OpenIDExtension based on the OpenID extension type
	 * 
	 * @param alias Extension alias
	 * @param auth AuthSuccess instance
	 * @return Appropriate OpenIDExtension instance
	 */
	public OpenIDExtension getExtension(String alias, AuthSuccess auth) {

		if (alias.equals(AxMessage.OPENID_NS_AX)
				|| alias.equals(IdentityConstants.OpenId.ExchangeAttributes.NS_AX)) {
			return new OpenIDAttributeExchange(auth);
		} else if (alias.equals(IdentityConstants.OpenId.SimpleRegAttributes.NS_SREG)
				|| alias.equals(SRegMessage.OPENID_NS_SREG)
				|| alias.equals(IdentityConstants.OpenId.SimpleRegAttributes.NS_SREG_1)) {
			return new OpenIDSimpleReg(auth);
		} else if (alias.equals(PapeMessage.OPENID_NS_PAPE)) {
			return new OpenIDPape(auth);
		}

		return null;
	}
}