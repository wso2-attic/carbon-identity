/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.openidconnect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.openidconnect.as.messages.IDTokenBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;

import java.util.Iterator;
import java.util.List;

/**
 * Returns the claims of the SAML assertion
 */
public class SAMLAssertionClaimsCallback implements CustomClaimsCallbackHandler {

    Log log = LogFactory.getLog(SAMLAssertionClaimsCallback.class);

    @Override
    public void handleCustomClaims(IDTokenBuilder builder, OAuthTokenReqMessageContext requestMsgCtx) {
        // reading the token set in the same grant
        Assertion assertion = (Assertion) requestMsgCtx.getProperty(OAuthConstants.OAUTH_SAML2_ASSERTION);
        if (assertion != null) {
            List<AttributeStatement> list = assertion.getAttributeStatements();
            if (list.size() > 0) {
                Iterator<Attribute> attribIterator =
                        assertion.getAttributeStatements().get(0)
                                .getAttributes().iterator();
                while (attribIterator.hasNext()) {
                    Attribute attribute = attribIterator.next();
                    String value = attribute.getAttributeValues().get(0).getDOM().getTextContent();
                    builder.setClaim(attribute.getName(), value);
                    if (log.isDebugEnabled()) {
                        log.debug("Attribute: " + attribute.getName() + ", Value: " + value);
                    }
                }
            } else {
                log.debug("No AttributeStatement found! ");
            }
        }
    }

}
