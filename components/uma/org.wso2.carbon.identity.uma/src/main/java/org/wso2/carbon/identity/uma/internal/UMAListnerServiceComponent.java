/*
 *
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * /
 */

package org.wso2.carbon.identity.uma.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.uma.claims.handlers.ClaimsTokenHandler;
import org.wso2.carbon.identity.uma.claims.token.ClaimsToken;
import org.wso2.carbon.identity.uma.rpt.issuer.RptIssuer;
import org.wso2.carbon.identity.uma.userconsent.handlers.UserConsentHandler;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * @scr.component name="org.wso2.carbon.identity.uma.handlers.listener" immediate="true"
 * @scr.reference name="claims.token.handler.service"
 * interface="org.wso2.carbon.identity.uma.claims.handlers.ClaimsTokenHandler"
 * cardinality="0..n" policy="dynamic"
 * bind="setClaimsTokenHandler"
 * unbind="unsetClaimsTokenHandler"
 * @scr.reference name="user.consent.handler.service"
 * interface="org.wso2.carbon.identity.uma.userconsent.handlers.UserConsentHandler"
 * cardinality="0..n" policy="dynamic"
 * bind="setUserConsentHandler"
 * unbind="unsetUserConsentHandler"
 */
public class UMAListnerServiceComponent {

    private static Log log = LogFactory.getLog(UMAListnerServiceComponent.class);

    private static Map<Integer, ClaimsTokenHandler> claimsTokenHandlers;
    private static Map<Integer, UserConsentHandler> userConsentHandlers;
    private static Map<String, ClaimsToken> claimTokens;
    private static Map<Integer, RptIssuer> rptIssuers;

    private static Collection<ClaimsTokenHandler> claimsTokenHandlerCollection;
    private static Collection<UserConsentHandler> userConsentHandlerCollection;
    private static Collection<ClaimsToken> claimsTokenCollection;
    private static Collection<RptIssuer> rptIssuerCollection;

    protected static synchronized void setClaimsTokenHandler
            (ClaimsTokenHandler claimsTokenHandler){
        claimsTokenHandlerCollection = null;
        if (claimsTokenHandlers == null){
            claimsTokenHandlers = new TreeMap<>();
        }
        claimsTokenHandlers.put(claimsTokenHandler.getPriority(),claimsTokenHandler);

    }

    protected static synchronized void unsetClaimsTokenHandler
            (ClaimsTokenHandler claimsTokenHandler){
        if (claimsTokenHandler != null && claimsTokenHandlers != null){
            claimsTokenHandlers.remove(claimsTokenHandler.getPriority());
            claimsTokenHandlerCollection = null;
        }

    }

    protected static synchronized void setUserConsentHandler
            (UserConsentHandler userConsentHandler){
        userConsentHandlerCollection= null;
        if (userConsentHandlers == null){
            userConsentHandlers = new TreeMap<>();
        }
        userConsentHandlers.put(userConsentHandler.getPriority(), userConsentHandler);

    }

    protected static synchronized void unsetUserConsentHandler
            (UserConsentHandler userConsentHandler){
        if (userConsentHandler != null && userConsentHandlers != null){
            userConsentHandlers.remove(userConsentHandler.getPriority());
            userConsentHandlerCollection = null;
        }

    }

    protected static synchronized void setClaimTokens
            (ClaimsToken claimTokens){

    }

    protected static synchronized void unsetClaimTokens
            (ClaimsToken claimTokens){

    }

    protected static synchronized void setRptIssuers
            (RptIssuer rptIssuer){

    }

    protected static synchronized void unsetRptIssuers
            (RptIssuer rptIssuer){

    }

    public static Collection<ClaimsTokenHandler> getClaimsTokenHandlers() {
        return null;
    }

    public static Collection<UserConsentHandler> getUserConsentHandlers() {
        return null;
    }

    public static Collection<ClaimsToken> getClaimTokens() {
        return claimTokens.values();
    }

    public static Collection<RptIssuer> getRptIssuers() {
        return rptIssuers.values();
    }
}
