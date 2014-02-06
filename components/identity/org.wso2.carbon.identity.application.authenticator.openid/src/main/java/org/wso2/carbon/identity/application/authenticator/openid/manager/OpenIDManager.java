package org.wso2.carbon.identity.application.authenticator.openid.manager;

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
import org.wso2.carbon.identity.application.authenticator.openid.exception.OpenIDException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.CarbonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenIDManager {

    // Smart OpenID Consumer Manager
    private static ConsumerManager consumerManager = new ConsumerManager();
    AttributesRequestor attributesRequestor = null;

    public String doOpenIDLogin(HttpServletRequest request, HttpServletResponse response, String contextIdentifier) throws OpenIDException {
    	
        String claimed_id = request.getParameter("claimed_id");

        try {

            // Discovery on the user supplied ID
            List discoveries = consumerManager.discover(claimed_id);

            // Associate with the OP and share a secret
            DiscoveryInformation discovered = consumerManager.associate(discoveries);

            // Keeping necessary parameters to verify the AuthResponse
            request.getSession().setAttribute("openid-disc", discovered);

            //consumerManager.setImmediateAuth(true);
            
            String returnToURL = CarbonUIUtil.getAdminConsoleURL(request);
            returnToURL = returnToURL.replace("commonauth/carbon/", "commonauth") + "?sessionDataKey=" + contextIdentifier;
            
            AuthRequest authReq = consumerManager.authenticate(discovered, returnToURL);

            // Request subject attributes using Attribute Exchange extension specification
//            if(SSOAgentConfigs.getAttributesRequestorImplClass() != null){
//
//                if(attributesRequestor == null){
//                    synchronized (this){
//                        if(attributesRequestor == null){
//                            attributesRequestor = (AttributesRequestor)Class.forName(SSOAgentConfigs.
//                                    getAttributesRequestorImplClass()).newInstance();
//                            attributesRequestor.init();
//                        }
//                    }
//                }
//
//                String[] requestedAttributes = attributesRequestor.getRequestedAttributes(claimed_id);
//
//                // Getting required attributes using FetchRequest
//                FetchRequest fetchRequest = FetchRequest.createFetchRequest();
//
//                for(String requestedAttribute:requestedAttributes){
//                    fetchRequest.addAttribute(requestedAttribute,
//                            attributesRequestor.getTypeURI(claimed_id, requestedAttribute),
//                            attributesRequestor.isRequired(claimed_id, requestedAttribute),
//                            attributesRequestor.getCount(claimed_id, requestedAttribute));
//                }
//
//                // Adding the AX extension to the AuthRequest message
//                authReq.addExtension(fetchRequest);
//            }

            // Returning OP Url
            return authReq.getDestinationUrl(true);

        } catch (YadisException e){
            if(e.getErrorCode() == 1796){
                throw new OpenIDException(e.getMessage(), e);
            }
            throw new OpenIDException("Error while creating FetchRequest", e);
        } catch (MessageException e) {
            throw new OpenIDException("Error while creating FetchRequest", e);
        } catch (DiscoveryException e) {
            throw new OpenIDException("Error while doing OpenID Discovery", e);
        } catch (ConsumerException e) {
            throw new OpenIDException("Error while doing OpenID Authentication", e);
        } /*catch (ClassNotFoundException e) {
            throw new OpenIDException("Error while instantiating AttributeRequestorImplClass: " +
                    SSOAgentConfigs.getAttributesRequestorImplClass(), e);
        } catch (InstantiationException e) {
            throw new OpenIDException("Error while instantiating AttributeRequestorImplClass: " +
                    SSOAgentConfigs.getAttributesRequestorImplClass(), e);
        } catch (IllegalAccessException e) {
            throw new OpenIDException("Error while instantiating AttributeRequestorImplClass: " +
                    SSOAgentConfigs.getAttributesRequestorImplClass(), e);
        }*/
    }

    public void processOpenIDLoginResponse(HttpServletRequest request, HttpServletResponse response, String contextIdentifier) throws OpenIDException{

        try {
            // Getting all parameters in request including AuthResponse
            ParameterList authResponseParams = new ParameterList(request.getParameterMap());

            // Previously discovered information
            DiscoveryInformation discovered = (DiscoveryInformation) request.getSession().getAttribute("openid-disc");

            String returnToURL = CarbonUIUtil.getAdminConsoleURL(request);
            returnToURL = returnToURL.replace("commonauth/carbon/", "commonauth") + "?sessionDataKey=" + contextIdentifier;
            
            // Verify return-to, discoveries, nonce & signature
            // Signature will be verified using the shared secret
            VerificationResult verificationResult = consumerManager.verify(returnToURL, authResponseParams, discovered);

            Identifier verified = verificationResult.getVerifiedId();

            // Identifier will be NULL if verification failed
            if (verified != null) {

                AuthSuccess authSuccess = (AuthSuccess) verificationResult.getAuthResponse();

                request.getSession().setAttribute("username", authSuccess.getIdentity());

                // Get requested attributes using AX extension
//                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
//                    Map<String,List> attributesMap = new HashMap<String,List>();
//                    String[] attrArray = attributesRequestor.getRequestedAttributes(authSuccess.getIdentity());
//                    FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
//                    for(String attr : attrArray){
//                        List attributeValues = fetchResp.getAttributeValuesByTypeUri(attributesRequestor.getTypeURI(authSuccess.getIdentity(), attr));
//                        if(attributeValues.get(0) instanceof String && ((String)attributeValues.get(0)).split(",").length > 1){
//                            String[] splitString = ((String)attributeValues.get(0)).split(",");
//                            for(String part : splitString){
//                                attributeValues.add(part);
//                            }
//                        }
//                        if(attributeValues.get(0) != null){
//                            attributesMap.put(attr,attributeValues);
//                        }
//                    }
//                    request.getSession().setAttribute(SSOAgentConfigs.getOpenIdAttributesMapName(), attributesMap);
//                }

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

}
