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
package org.wso2.carbon.identity.sts.passive;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultSubCode;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rahas.TrustException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.application.common.model.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.application.common.model.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.sts.passive.internal.RegistryBasedTrustedServiceStore;
import org.wso2.carbon.identity.sts.passive.processors.RequestProcessor;

import javax.xml.namespace.QName;

public class PassiveSTSService {
    private static final Log log = LogFactory.getLog(PassiveSTSService.class);

    public ResponseToken getResponse(RequestToken request) throws Exception {

        if (request == null || request.getUserName() == null) {
            throw new Exception("Invalid request token. User credentials not provided");
        }

        RequestProcessor processor = null;
        ResponseToken responseToken = null;
        String soapfault = null;

        // Setting wreply url from sp config
        setReplyToURL(request);

        processor = RequestProcessorFactory.getInstance().getRequestProcessor(request.getAction());

        if (processor != null) {
            try {
                responseToken = processor.process(request);
            } catch (TrustException e) {
                soapfault = genFaultResponse(MessageContext.getCurrentMessageContext(), "Sender",
                        "InvalidRequest", e.getMessage(), "none").toStringWithConsume();
            }
        } else {
            soapfault = genFaultResponse(MessageContext.getCurrentMessageContext(), "Sender",
                    "InvalidRequest", "Invalid Request", "none").toStringWithConsume();
        }

        if (responseToken == null) {
            responseToken = new ResponseToken();
        }

        if (soapfault != null) {
            responseToken.setResults(soapfault);
        }

        responseToken.setAuthenticated(true);
        if (request.getReplyTo() != null) {
            responseToken.setReplyTo(request.getReplyTo());
        } else {
            responseToken.setReplyTo(request.getRealm());
        }

        if (responseToken.getReplyTo() == null) {
            throw new Exception("ReplyTo address not found");
        }

        responseToken.setContext(request.getContext());

        return responseToken;
    }

    private SOAPFault genFaultResponse(MessageContext messageCtx, String code, String subCode,
                                       String reason, String detail) {
        SOAPFactory soapFactory = null;
        if (messageCtx.isSOAP11()) {
            soapFactory = OMAbstractFactory.getSOAP11Factory();
            SOAPEnvelope message = soapFactory.getDefaultFaultEnvelope();
            SOAPFaultReason soapFaultReason = soapFactory.createSOAPFaultReason();
            soapFaultReason.setText(reason);
            message.getBody().getFault().setReason(soapFaultReason);
            SOAPFaultCode soapFaultCode = soapFactory.createSOAPFaultCode();
            QName qNameSubCode = new QName("http://wso2.org/passivests", subCode, "sts");
            soapFaultCode.setText(qNameSubCode);
            message.getBody().getFault().setCode(soapFaultCode);
            return message.getBody().getFault();
        } else {
            soapFactory = OMAbstractFactory.getSOAP12Factory();
            SOAPEnvelope message = soapFactory.getDefaultFaultEnvelope();
            SOAPFaultDetail soapFaultDetail = soapFactory.createSOAPFaultDetail();
            soapFaultDetail.setText(detail);
            message.getBody().getFault().setDetail(soapFaultDetail);
            SOAPFaultReason soapFaultReason = soapFactory.createSOAPFaultReason();
            SOAPFaultText soapFaultText = soapFactory.createSOAPFaultText();
            soapFaultText.setText(reason);
            soapFaultReason.addSOAPText(soapFaultText);
            message.getBody().getFault().setReason(soapFaultReason);
            SOAPFaultCode soapFaultCode = soapFactory.createSOAPFaultCode();
            SOAPFaultValue soapFaultValue = soapFactory.createSOAPFaultValue(soapFaultCode);
            soapFaultValue.setText(code);
            soapFaultCode.setValue(soapFaultValue);
            SOAPFaultSubCode soapFaultSubCode = soapFactory.createSOAPFaultSubCode(soapFaultCode);
            SOAPFaultValue soapFaultValueSub = soapFactory.createSOAPFaultValue(soapFaultSubCode);
            QName qNameSubCode = new QName("http://wso2.org/passivests", subCode, "sts");
            soapFaultValueSub.setText(qNameSubCode);
            soapFaultSubCode.setValue(soapFaultValueSub);
            soapFaultCode.setSubCode(soapFaultSubCode);
            message.getBody().getFault().setCode(soapFaultCode);
            return message.getBody().getFault();
        }
    }

    /**
     * Add a trusted service to which tokens are issued with given claims.
     *
     * @param realmName    - this uniquely represents the trusted service
     * @param claimDialect - claim dialects uris
     * @param claims       - these comma separated default claims are issued when a request is done from
     *                     the given realm
     * @throws Exception - if fails to add trusted service
     */
    public void addTrustedService(String realmName, String claimDialect, String claims)
            throws Exception {
        RegistryBasedTrustedServiceStore registryBasedTrustedServiceStore = new RegistryBasedTrustedServiceStore();
        registryBasedTrustedServiceStore.addTrustedService(realmName, claimDialect, claims);

    }

    /**
     * Remove the given trusted service with realmName
     *
     * @param realmName - the realm of the service
     * @throws Exception
     */
    public void removeTrustedService(String realmName) throws Exception {
        RegistryBasedTrustedServiceStore registryBasedTrustedServiceStore = new RegistryBasedTrustedServiceStore();
        registryBasedTrustedServiceStore.removeTrustedService(realmName);
    }

    /**
     * Get all trusted services
     *
     * @return get default claims for all trusted services
     * @throws Exception
     */
    public ClaimDTO[] getAllTrustedServices() throws Exception {

        // if in-memory store is empty, load from registry
        RegistryBasedTrustedServiceStore registryBasedTrustedServiceStore = new RegistryBasedTrustedServiceStore();
        return registryBasedTrustedServiceStore.getAllTrustedServices();
    }

    /**
     * Get default claims for given trusted service
     *
     * @param realmName - trusted service realm name
     * @return - default claims for given trusted service
     * @throws Exception
     */
    public ClaimDTO getTrustedServiceClaims(String realmName) throws Exception {
        // check in registry if not found in in-memory store
        RegistryBasedTrustedServiceStore registryBasedTrustedServiceStore = new RegistryBasedTrustedServiceStore();
        return registryBasedTrustedServiceStore.getTrustedServiceClaims(realmName);
    }


    private void setReplyToURL(RequestToken request) {

        String wreply = request.getReplyTo();

//        if(wreply != null) {
//            log.debug("Request contains ReplyTo URL : " + wreply +
//                    ". Skip setting ReplyTo URL from Realm (Service Provider config)");
//            return;
//        }

        String realm = request.getRealm();
        if (realm == null) {
            log.debug("Request does not contains Realm. Skip setting ReplyTo URL from Realm (Service Provider config)");
            return;
        }
        ServiceProvider sp = null;
        try {
            String tenantDomain = request.getTenantDomain();
            if (tenantDomain ==null || tenantDomain.trim().length() == 0) {
                tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                request.setTenantDomain(tenantDomain);
            }
            if(log.isDebugEnabled()) {
                log.debug("Retrieving wreply url for : " + realm + " in tenant : " + tenantDomain);
            }
            sp = ApplicationManagementService.getInstance().
                    getServiceProviderByClientId(realm, "passivests", tenantDomain);
        } catch (IdentityApplicationManagementException e) {
            log.error("Error while retrieving Service Provider corresponding to Realm : " + realm +
                    ". Skip setting ReplyTo URL from Realm (Service Provider config)", e);
            return;
        }


        if(sp == null) {
            log.error("Cannot find Service Provider corresponding to Realm : " + realm +
                    ". Skip setting ReplyTo URL from Realm (Service Provider config)");
        }

        InboundAuthenticationRequestConfig[] inboundAuthenticationConfigs =
                sp.getInboundAuthenticationConfig().getInboundAuthenticationRequestConfigs();
        if(inboundAuthenticationConfigs != null) {
            for (int i = 0; i < inboundAuthenticationConfigs.length; i++) {
                if ("passivests".equalsIgnoreCase(inboundAuthenticationConfigs[i].getInboundAuthType())) {

                    // get wreply url from properties
                    Property[] properties = inboundAuthenticationConfigs[i].getProperties();
                    if (properties != null) {
                        for (int j = 0; j < properties.length; j++) {
                            if("passiveSTSWReply".equalsIgnoreCase(properties[j].getName())) {
                                wreply = properties[j].getValue();
                                if (wreply != null && !wreply.isEmpty()) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Setting ReplyTo URL : " + wreply + " for Realm : " + realm);
                                    }
                                    request.setReplyTo(wreply);
                                }
                                return;
                            }
                        }
                    }

                    if(log.isDebugEnabled()) {
                        log.debug("WReply URL does not specified for Realm : " + realm + " in Service Provider configs");
                    }
                    return;
                }
            }
        }

    }
}
