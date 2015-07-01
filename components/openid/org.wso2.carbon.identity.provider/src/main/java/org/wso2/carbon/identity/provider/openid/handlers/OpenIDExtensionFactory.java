/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid.handlers;

import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.pape.PapeMessage;
import org.openid4java.message.sreg.SRegMessage;
import org.wso2.carbon.identity.base.IdentityConstants.OpenId.ExchangeAttributes;
import org.wso2.carbon.identity.base.IdentityConstants.OpenId.SimpleRegAttributes;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.openid.extensions.OpenIDAttributeExchange;
import org.wso2.carbon.identity.provider.openid.extensions.OpenIDExtension;
import org.wso2.carbon.identity.provider.openid.extensions.OpenIDPape;
import org.wso2.carbon.identity.provider.openid.extensions.OpenIDSimpleReg;

public class OpenIDExtensionFactory {

    // Guaranteed to be thread safe
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
     * Create an instance of the OpenIDExtension based on the OpenID extension type
     *
     * @param request  OpenIDAuthenticationRequest instance
     * @return Appropriate OpenIDExtension instance
     * @throws IdentityException
     */
    public OpenIDExtension getExtension(OpenIDAuthenticationRequest request)
            throws IdentityException {

        if (request == null) {
            return null;
        }

        String alias = request.getExtensionAlias();

        if (AxMessage.OPENID_NS_AX.equals(alias) || ExchangeAttributes.NS_AX.equals(alias)) {
            return new OpenIDAttributeExchange(request);
        } else if (SimpleRegAttributes.NS_SREG.equals(alias) || SRegMessage.OPENID_NS_SREG.equals(alias) ||
                   SimpleRegAttributes.NS_SREG_1.equals(alias)) {
            return new OpenIDSimpleReg(request);
        } else if (PapeMessage.OPENID_NS_PAPE.equals(alias)) {
            return new OpenIDPape(request);
        }

        return null;
    }
}