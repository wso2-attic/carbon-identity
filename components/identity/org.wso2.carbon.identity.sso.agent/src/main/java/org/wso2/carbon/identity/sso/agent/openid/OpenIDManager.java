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
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentSessionBean;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConfigs;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenIDManager {

    // Smart OpenID Consumer Manager
    private static ConsumerManager consumerManager = null;

    AttributesRequestor attributesRequestor = null;

    public OpenIDManager() throws SSOAgentException{
        consumerManager = getConsumerManagerInstance();
    }

    private ConsumerManager getConsumerManagerInstance() throws SSOAgentException{
        HttpFetcherFactory  httpFetcherFactory = new HttpFetcherFactory(loadSSLContext(),null);
        return new ConsumerManager(
                new RealmVerifierFactory(new YadisResolver(httpFetcherFactory)),
                new Discovery(), httpFetcherFactory);
    }

    public String doOpenIDLogin(HttpServletRequest request, HttpServletResponse response) throws SSOAgentException {

        String claimed_id = request.getParameter(SSOAgentConfigs.getClaimedIdParameterName());


        try {

            // Discovery on the user supplied ID
            List discoveries = consumerManager.discover(claimed_id);

            // Associate with the OP and share a secret
            DiscoveryInformation discovered = consumerManager.associate(discoveries);

            // Keeping necessary parameters to verify the AuthResponse
            SSOAgentSessionBean sessionBean = new SSOAgentSessionBean();
            sessionBean.setOpenIDSessionBean(sessionBean.new OpenIDSessionBean());
            sessionBean.getOpenIDSessionBean().setDiscoveryInformation(discovered); // set the discovery information
            request.getSession().setAttribute(SSOAgentConfigs.getSessionBeanName(), sessionBean);

            consumerManager.setImmediateAuth(true);
            AuthRequest authReq = consumerManager.authenticate(discovered, SSOAgentConfigs.getReturnTo());

            // Request subject attributes using Attribute Exchange extension specification
            if(SSOAgentConfigs.getAttributesRequestorImplClass() != null){

                if(attributesRequestor == null){
                    synchronized (this){
                        if(attributesRequestor == null){
                            attributesRequestor = (AttributesRequestor)Class.forName(SSOAgentConfigs.
                                    getAttributesRequestorImplClass()).newInstance();
                            attributesRequestor.init();
                        }
                    }
                }

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
        } catch (ClassNotFoundException e) {
            throw new SSOAgentException("Error while instantiating AttributeRequestorImplClass: " +
                    SSOAgentConfigs.getAttributesRequestorImplClass(), e);
        } catch (InstantiationException e) {
            throw new SSOAgentException("Error while instantiating AttributeRequestorImplClass: " +
                    SSOAgentConfigs.getAttributesRequestorImplClass(), e);
        } catch (IllegalAccessException e) {
            throw new SSOAgentException("Error while instantiating AttributeRequestorImplClass: " +
                    SSOAgentConfigs.getAttributesRequestorImplClass(), e);
        }
    }

    public void processOpenIDLoginResponse(HttpServletRequest request, HttpServletResponse response) throws SSOAgentException{

        try {
            // Getting all parameters in request including AuthResponse
            ParameterList authResponseParams = new ParameterList(request.getParameterMap());

            // Get previously saved session bean
            SSOAgentSessionBean ssoAgentSessionBean = (SSOAgentSessionBean)request.getSession(false).getAttribute(
                    SSOAgentConfigs.getSessionBeanName());
            if(ssoAgentSessionBean == null){
                throw new SSOAgentException("Error while verifying OpenID response. Cannot find valid session for user");
            }

            // Previously discovered information
            DiscoveryInformation discovered = ssoAgentSessionBean.getOpenIDSessionBean().getDiscoveryInformation();

            // Verify return-to, discoveries, nonce & signature
            // Signature will be verified using the shared secret
            VerificationResult verificationResult = consumerManager.verify(SSOAgentConfigs.getReturnTo(), authResponseParams, discovered);

            Identifier verified = verificationResult.getVerifiedId();

            // Identifier will be NULL if verification failed
            if (verified != null) {

                AuthSuccess authSuccess = (AuthSuccess) verificationResult.getAuthResponse();

                ssoAgentSessionBean.getOpenIDSessionBean().setClaimedId(authSuccess.getIdentity());

                // Get requested attributes using AX extension
                if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
                    Map<String,List<String>> attributesMap = new HashMap<String,List<String>>();
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
                    ssoAgentSessionBean.getOpenIDSessionBean().setOpenIdAttributes(attributesMap);
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

    private SSLContext loadSSLContext() throws SSOAgentException {
        KeyStore trustStore = null;
        try {
            trustStore = SSOAgentConfigs.getKeyStore();

            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            return sslContext;
        } catch (NoSuchAlgorithmException e) {
            throw new SSOAgentException("Error when reading keystore", e);
        } catch (KeyManagementException e) {
            throw new SSOAgentException("Error when reading keystore", e);
        } catch (KeyStoreException e) {
            throw new SSOAgentException("Error when reading keystore", e);
        }
    }

}
