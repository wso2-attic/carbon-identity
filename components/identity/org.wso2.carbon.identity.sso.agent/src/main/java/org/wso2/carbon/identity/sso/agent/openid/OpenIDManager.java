package org.wso2.carbon.identity.sso.agent.openid;

import org.openid4java.association.AssociationException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.yadis.YadisException;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.server.RealmVerifierFactory;
import org.openid4java.util.HttpFetcherFactory;
import org.wso2.carbon.identity.sso.agent.SSOAgentConstants;
import org.wso2.carbon.identity.sso.agent.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.bean.LoggedInSessionBean;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentConfig;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenIDManager {

    // Smart OpenID Consumer Manager
    private static ConsumerManager consumerManager = null;
    private SSOAgentConfig ssoAgentConfig = null;
    AttributesRequestor attributesRequestor = null;

    public OpenIDManager(SSOAgentConfig ssoAgentConfig) throws SSOAgentException{
        consumerManager = getConsumerManagerInstance();
        this.ssoAgentConfig = ssoAgentConfig;
    }

    private ConsumerManager getConsumerManagerInstance() throws SSOAgentException {

        HttpFetcherFactory  httpFetcherFactory = null;
        try {
            httpFetcherFactory = new HttpFetcherFactory(SSLContext.getDefault(),null);
        } catch (NoSuchAlgorithmException e) {
            throw new SSOAgentException("Error while getting default SSL Context", e);
        }
        return new ConsumerManager(
                new RealmVerifierFactory(new YadisResolver(httpFetcherFactory)),
                new Discovery(), httpFetcherFactory);
    }

    public String doOpenIDLogin(HttpServletRequest request, HttpServletResponse response) throws SSOAgentException {

        String claimed_id = ssoAgentConfig.getOpenId().getClaimedId();

        try {

            if (ssoAgentConfig.getOpenId().isDumbModeEnabled()){
                // Switch the consumer manager to dumb mode
                consumerManager.setMaxAssocAttempts(0);
            }

            // Discovery on the user supplied ID
            List discoveries = consumerManager.discover(claimed_id);

            // Associate with the OP and share a secret
            DiscoveryInformation discovered = consumerManager.associate(discoveries);

            // Keeping necessary parameters to verify the AuthResponse
            LoggedInSessionBean sessionBean = new LoggedInSessionBean();
            sessionBean.setOpenId(sessionBean.new OpenID());
            sessionBean.getOpenId().setDiscoveryInformation(discovered); // set the discovery information
            request.getSession().setAttribute(SSOAgentConstants.SESSION_BEAN_NAME, sessionBean);

            consumerManager.setImmediateAuth(true);
            AuthRequest authReq = consumerManager.authenticate(discovered,
                    ssoAgentConfig.getOpenId().getReturnToURL());


            // Request subject attributes using Attribute Exchange extension specification if AttributeExchange is enabled
            if(ssoAgentConfig.getOpenId().isAttributeExchangeEnabled() &&
                    ssoAgentConfig.getOpenId().getAttributesRequestor() != null){

                attributesRequestor = ssoAgentConfig.getOpenId().getAttributesRequestor();
                attributesRequestor.init();

                String[] requestedAttributes = attributesRequestor.getRequestedAttributes(claimed_id);

                // Getting required attributes using FetchRequest
                FetchRequest fetchRequest = FetchRequest.createFetchRequest();

                for(String requestedAttribute:requestedAttributes){
                    fetchRequest.addAttribute(requestedAttribute,
                            attributesRequestor.getTypeURI(claimed_id, requestedAttribute),
                            attributesRequestor.isRequired(claimed_id, requestedAttribute),
                            attributesRequestor.getCount(claimed_id, requestedAttribute));
                }

                // Adding the AX extension to the AuthRequest message
                authReq.addExtension(fetchRequest);
            }

            // Returning OP Url
            return authReq.getDestinationUrl(true);

        } catch (YadisException e){
            if(e.getErrorCode() == 1796){
                throw new SSOAgentException(e.getMessage(), e);
            }
            throw new SSOAgentException("Error while creating FetchRequest", e);
        } catch (MessageException e) {
            throw new SSOAgentException("Error while creating FetchRequest", e);
        } catch (DiscoveryException e) {
            throw new SSOAgentException("Error while doing OpenID Discovery", e);
        } catch (ConsumerException e) {
            throw new SSOAgentException("Error while doing OpenID Authentication", e);
        }
    }

    public void processOpenIDLoginResponse(HttpServletRequest request, HttpServletResponse response) throws SSOAgentException{

        try {
            // Getting all parameters in request including AuthResponse
            ParameterList authResponseParams = new ParameterList(request.getParameterMap());

            // Get previously saved session bean
            LoggedInSessionBean loggedInSessionBean = (LoggedInSessionBean)request.getSession(false).
                    getAttribute(SSOAgentConstants.SESSION_BEAN_NAME);
            if(loggedInSessionBean == null){
                throw new SSOAgentException("Error while verifying OpenID response. " +
                        "Cannot find valid session for user");
            }

            // Previously discovered information
            DiscoveryInformation discovered = loggedInSessionBean.getOpenId().getDiscoveryInformation();

            // Verify return-to, discoveries, nonce & signature
            // Signature will be verified using the shared secret
            VerificationResult verificationResult = consumerManager.verify(
                    ssoAgentConfig.getOpenId().getReturnToURL(), authResponseParams, discovered);

            Identifier verified = verificationResult.getVerifiedId();

            // Identifier will be NULL if verification failed
            if (verified != null) {

                AuthSuccess authSuccess = (AuthSuccess) verificationResult.getAuthResponse();

                loggedInSessionBean.getOpenId().setClaimedId(authSuccess.getIdentity());

                // Get requested attributes using AX extension
                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                    Map<String,List<String>> attributesMap = new HashMap<String,List<String>>();
                    if (ssoAgentConfig.getOpenId().getAttributesRequestor() != null) {
                        attributesRequestor = ssoAgentConfig.getOpenId().getAttributesRequestor();
                        String[] attrArray = attributesRequestor.getRequestedAttributes(authSuccess.getIdentity());
                        FetchResponse fetchResp = (FetchResponse) authSuccess.getExtension(AxMessage.OPENID_NS_AX);
                        for(String attr : attrArray){
                            List attributeValues = fetchResp.getAttributeValuesByTypeUri(attributesRequestor.getTypeURI(authSuccess.getIdentity(), attr));
                            if(attributeValues.get(0) instanceof String && ((String)attributeValues.get(0)).split(",").length > 1){
                                String[] splitString = ((String)attributeValues.get(0)).split(",");
                                for(String part : splitString){
                                    attributeValues.add(part);
                                }
                            }
                            if(attributeValues.get(0) != null){
                                attributesMap.put(attr,attributeValues);
                            }
                        }
                    }
                    loggedInSessionBean.getOpenId().setSubjectAttributes(attributesMap);
                }

            } else {
                throw new SSOAgentException("OpenID verification failed");
            }

        } catch (AssociationException e) {
            throw new SSOAgentException("Error while verifying OpenID response", e);
        } catch (MessageException e) {
            throw new SSOAgentException("Error while verifying OpenID response", e);
        } catch (DiscoveryException e) {
            throw new SSOAgentException("Error while verifying OpenID response", e);
        }

    }

//    protected SSLContext loadSSLContext() throws SSOAgentException {
//
//        KeyStore trustStore = null;
//        try {
//
//            trustStore = SSOAgentConfig.getKeyStore();
//
//            TrustManagerFactory tmf = TrustManagerFactory
//                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
//
//            tmf.init(trustStore);
//
//            SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(null, tmf.getTrustManagers(), null);
//            return sslContext;
//        } catch (NoSuchAlgorithmException e) {
//            throw new SSOAgentException("Error when reading keystore", e);
//        } catch (KeyManagementException e) {
//            throw new SSOAgentException("Error when reading keystore", e);
//        } catch (KeyStoreException e) {
//            throw new SSOAgentException("Error when reading keystore", e);
//        }
//    }

}
