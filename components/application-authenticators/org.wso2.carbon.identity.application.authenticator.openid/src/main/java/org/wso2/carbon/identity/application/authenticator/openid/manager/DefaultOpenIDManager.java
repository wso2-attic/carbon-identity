/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.openid.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.yadis.YadisException;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.wso2.carbon.identity.application.authentication.framework.config.builder.FileBasedConfigurationBuilder;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authenticator.openid.exception.OpenIDException;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.core.util.IdentityUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultOpenIDManager implements OpenIDManager {

    // Smart OpenID Consumer Manager
    private static ConsumerManager consumerManager = new ConsumerManager();

    private static Log log = LogFactory.getLog(DefaultOpenIDManager.class);

    @Override
    public String doOpenIDLogin(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) throws OpenIDException {

        String claimed_id = request.getParameter("claimed_id");

        if (claimed_id == null) {
            claimed_id = context.getAuthenticatorProperties().get(
                    IdentityApplicationConstants.Authenticator.OpenID.OPEN_ID_URL);
        }

        try {

            // Discovery on the user supplied ID
            List discoveries = consumerManager.discover(claimed_id);

            // Associate with the OP and share a secret
            DiscoveryInformation discovered = consumerManager.associate(discoveries);

            // Keeping necessary parameters to verify the AuthResponse
            request.getSession().setAttribute("openid-disc", discovered);


            String realm = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH, true);
            String returnToURL = realm + "?sessionDataKey=" + context.getContextIdentifier();

            AuthRequest authReq = consumerManager.authenticate(discovered, returnToURL);
            authReq.setRealm(realm);

            // Request subject attributes using Attribute Exchange extension specification
            AttributesRequestor attributesRequestor = getAttributeRequestor();

            String[] requestedAttributes = attributesRequestor.getRequestedAttributes(claimed_id);

            // Getting required attributes using FetchRequest
            FetchRequest fetchRequest = FetchRequest.createFetchRequest();

            for (String requestedAttribute : requestedAttributes) {

                fetchRequest.addAttribute(requestedAttribute,
                        attributesRequestor.getTypeURI(claimed_id, requestedAttribute),
                        attributesRequestor.isRequired(claimed_id, requestedAttribute),
                        attributesRequestor.getCount(claimed_id, requestedAttribute));
            }

            // Adding the AX extension to the AuthRequest message
            authReq.addExtension(fetchRequest);

            // Returning OP Url
            return authReq.getDestinationUrl(true);

        } catch (YadisException e) {
            if (e.getErrorCode() == 1796) {
                throw new OpenIDException(e.getMessage(), e);
            }
            throw new OpenIDException("Error while creating FetchRequest", e);
        } catch (MessageException e) {
            throw new OpenIDException("Error while creating FetchRequest", e);
        } catch (DiscoveryException e) {
            throw new OpenIDException("Error while doing OpenID Discovery", e);
        } catch (ConsumerException e) {
            throw new OpenIDException("Error while doing OpenID Authentication", e);
        }
    }

    @Override
    public void processOpenIDLoginResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationContext context) throws OpenIDException {

        String contextIdentifier = context.getContextIdentifier();

        try {
            // Getting all parameters in request including AuthResponse
            ParameterList authResponseParams = new ParameterList(request.getParameterMap());

            // Previously discovered information
            DiscoveryInformation discovered = (DiscoveryInformation) request.getSession().getAttribute("openid-disc");

            String returnToURL = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH, true) + "?sessionDataKey=" +
                                 contextIdentifier;

            // Verify return-to, discoveries, nonce & signature
            // Signature will be verified using the shared secret
            VerificationResult verificationResult = consumerManager.verify(returnToURL, authResponseParams, discovered);

            Identifier verified = verificationResult.getVerifiedId();

            // Identifier will be NULL if verification failed
            if (verified != null) {

                if (log.isDebugEnabled()) {
                    log.debug("OpenID Response verification successfull. Verified ID: " + verified.getIdentifier());
                }

                AuthSuccess authSuccess = (AuthSuccess) verificationResult.getAuthResponse();

                AttributesRequestor attributesRequestor = getAttributeRequestor();

                AuthenticatedUser authenticatedSubject = new AuthenticatedUser();

                // Get requested attributes using AX extension
                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {

                    Map<ClaimMapping, String> externalIDPClaims = new HashMap<ClaimMapping, String>();

                    String[] attrArray = attributesRequestor.getRequestedAttributes(authSuccess.getIdentity());
                    FetchResponse fetchResp;
                    try {
                        fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
                    } catch (MessageException e) {
                        //this is done because of response validation fails for yahoo response.
                        fetchResp = new YahooFetchResponse(authSuccess.getParameterMap());
                    }

                    for (String attr : attrArray) {
                        String claimUri = attributesRequestor.getTypeURI(authSuccess.getIdentity(), attr);
                        List attributeValues = fetchResp.getAttributeValuesByTypeUri(claimUri);

                        if (attributeValues.get(0) instanceof String && ((String) attributeValues.get(0)).split(",").length > 1) {
                            String[] splitString = ((String) attributeValues.get(0)).split(",");
                            for (String part : splitString) {
                                attributeValues.add(part);
                            }
                        }
                        if (attributeValues.get(0) != null) {
                            Claim claim = new Claim();
                            claim.setClaimUri(claimUri);
                            ClaimMapping claimMapping = new ClaimMapping();
                            claimMapping.setRemoteClaim(claim);
                            externalIDPClaims.put(claimMapping, getCommaSeperatedValue(attributeValues));
                        }
                    }

                    authenticatedSubject.setUserAttributes(externalIDPClaims);
                }

                authenticatedSubject.setAuthenticatedSubjectIdentifier(authSuccess.getClaimed());
                context.setSubject(authenticatedSubject);

            } else {
                throw new OpenIDException("OpenID verification failed");
            }

        } catch (AssociationException e) {
            throw new OpenIDException("Error while verifying OpenID response", e);
        } catch (MessageException e) {
            throw new OpenIDException("Error while verifying OpenID response", e);
        } catch (DiscoveryException e) {
            throw new OpenIDException("Error while verifying OpenID response", e);
        }

    }

    private AttributesRequestor getAttributeRequestor() {

        String attribRequestorClassName = FileBasedConfigurationBuilder.getInstance()
                .getAuthenticatorBean("OpenIDAuthenticator").getParameterMap()
                .get("AttributesRequestor");

        AttributesRequestor attribRequestor = null;

        if (attribRequestorClassName != null) {
            try {
                // Bundle class loader will cache the loaded class and returned
                // the already loaded instance, hence calling this method
                // multiple times doesn't cost.
                Class clazz = Thread.currentThread().getContextClassLoader()
                        .loadClass(attribRequestorClassName);
                attribRequestor = (AttributesRequestor) clazz.newInstance();

            } catch (ClassNotFoundException e) {
                log.error("Error while instantiating the OpenIDManager ", e);
            } catch (InstantiationException e) {
                log.error("Error while instantiating the OpenIDManager ", e);
            } catch (IllegalAccessException e) {
                log.error("Error while instantiating the OpenIDManager ", e);
            }
        } else {
            attribRequestor = new SampleAttributesRequestor();
        }
        if (attribRequestor != null) {
            attribRequestor.init();
        }

        return attribRequestor;
    }

    private String getCommaSeperatedValue(List<String> values) {
        StringBuilder returnValue = null;
        for (String value : values) {
            if (returnValue == null) {
                returnValue = new StringBuilder(value);
            } else {
                returnValue.append("," + value);
            }
        }
        return returnValue != null ? returnValue.toString() : null;
    }

}
