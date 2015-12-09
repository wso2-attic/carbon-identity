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
package org.wso2.carbon.identity.sts.passive.processors;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.RahasConstants;
import org.apache.rahas.RahasData;
import org.apache.rahas.TrustException;
import org.apache.rahas.impl.SAMLPassiveTokenIssuer;
import org.apache.rahas.impl.SAMLTokenIssuerConfig;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.wso2.carbon.identity.sts.passive.RequestToken;
import org.wso2.carbon.identity.sts.passive.ResponseToken;

import javax.xml.stream.XMLStreamException;
import java.util.Vector;

public class SigningRequestProcessor extends RequestProcessor {
    private static final Log log = LogFactory.getLog(SigningRequestProcessor.class);

    public ResponseToken process(RequestToken request) throws TrustException {

        MessageContext context = MessageContext.getCurrentMessageContext();
        SAMLPassiveTokenIssuer issuer = null;
        WSHandlerResult handlerResults = null;
        WSSecurityEngineResult engineResult = null;
        WSUsernameTokenPrincipal principal = null;
        Vector<WSSecurityEngineResult> wsResults = null;
        ResponseToken reponseToken = null;
        Vector<WSHandlerResult> handlerResultsVector = null;
        OMElement rstr = null;

        principal = new WSUsernameTokenPrincipal(request.getUserName(), false);

        engineResult = new WSSecurityEngineResult(WSConstants.UT, principal, null, null, null);

        wsResults = new Vector<WSSecurityEngineResult>();
        wsResults.add(engineResult);

        handlerResults = new WSHandlerResult("", wsResults);

        handlerResultsVector = new Vector<WSHandlerResult>();
        handlerResultsVector.add(handlerResults);

        MessageContext.getCurrentMessageContext().setProperty(WSHandlerConstants.RECV_RESULTS,
                handlerResultsVector);


        try {
            MessageContext.getCurrentMessageContext().setProperty(RahasConstants.PASSIVE_STS_RST,
                    getRST(request.getRealm(), request.getAttributes(), request.getDialect()));
        } catch (Exception e) {
            log.error("Failed to get RST element.", e);
            throw new TrustException("errorWhileProcessingSigninRequest", e);
        }

        SAMLTokenIssuerConfig samlTokenIssuerConfig = null;
        try {
            samlTokenIssuerConfig = getSAMLTokenIssuerConfig(MessageContext.getCurrentMessageContext()
                    .getAxisService(), true);
        } catch (Exception e) {
            log.error("Failed to get saml token issuer config.", e);
            throw new TrustException("errorWhileProcessingSigninRequest", e);
        }

        rahasData = new RahasData(context);
        issuer = new SAMLPassiveTokenIssuer();
        issuer.setAudienceRestrictionCondition(request.getRealm());
        issuer.setConfig(samlTokenIssuerConfig);

        rstr = issuer.issuePassiveRSTR(rahasData);
        reponseToken = new ResponseToken();
        if (rstr != null) {
            try {
                reponseToken.setResults(rstr.toStringWithConsume());
            } catch (XMLStreamException e) {
                log.error(e.getMessage(), e);
                throw new TrustException("errorWhileProcessingSigninRequest", e);
            }
        }
        return reponseToken;
    }

}
