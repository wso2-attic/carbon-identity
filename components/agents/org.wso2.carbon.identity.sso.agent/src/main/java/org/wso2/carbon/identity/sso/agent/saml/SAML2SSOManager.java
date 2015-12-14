/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 *
 *
 */

package org.wso2.carbon.identity.sso.agent.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignature;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.common.Extensions;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Audience;
import org.opensaml.saml2.core.AudienceRestriction;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Conditions;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.SessionIndex;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.impl.AuthnContextClassRefBuilder;
import org.opensaml.saml2.core.impl.AuthnRequestBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.NameIDPolicyBuilder;
import org.opensaml.saml2.core.impl.RequestedAuthnContextBuilder;
import org.opensaml.saml2.core.impl.SessionIndexBuilder;
import org.opensaml.saml2.ecp.RelayState;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.encryption.EncryptedKey;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.identity.sso.agent.SSOAgentConstants;
import org.wso2.carbon.identity.sso.agent.SSOAgentDataHolder;
import org.wso2.carbon.identity.sso.agent.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.bean.LoggedInSessionBean;
import org.wso2.carbon.identity.sso.agent.bean.SSOAgentConfig;
import org.wso2.carbon.identity.sso.agent.util.SAMLSignatureValidator;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentUtils;
import org.apache.commons.collections.CollectionUtils;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * TODO: Need to have mechanism to map SP initiated SAML2 Request to SAML2 Responses and validate.
 * TODO: Still however IdP initiated SSO also should be possible through configuration
 */
public class SAML2SSOManager {

    private static final Log log = LogFactory.getLog(SAML2SSOManager.class);


    private static final Logger LOGGER = Logger.getLogger(SSOAgentConstants.LOGGER_NAME);
    private SSOAgentConfig ssoAgentConfig = null;

    public SAML2SSOManager(SSOAgentConfig ssoAgentConfig) throws SSOAgentException {

		/* Initializing the OpenSAML library, loading default configurations */
        this.ssoAgentConfig = ssoAgentConfig;
        //load custom Signature Validator Class
        String signerClassName = ssoAgentConfig.getSAML2().getSignatureValidatorImplClass();
        try {
            if (signerClassName != null) {
                SSOAgentDataHolder.getInstance().setSignatureValidator(Class.forName(signerClassName).newInstance());
            }
        } catch (ClassNotFoundException e) {
            throw new SSOAgentException("Error loading custom signature validator class", e);
        } catch (IllegalAccessException e) {
            throw new SSOAgentException("Error loading custom signature validator class", e);
        } catch (InstantiationException e) {
            throw new SSOAgentException("Error loading custom signature validator class", e);
        }
        SSOAgentUtils.doBootstrap();
    }

    /**
     * Returns the redirection URL with the appended SAML2
     * Request message
     *
     * @param request SAML 2 request
     * @return redirectionUrl
     */
    public String buildRedirectRequest(HttpServletRequest request, boolean isLogout) throws SSOAgentException {

        RequestAbstractType requestMessage = null;
        if (!isLogout) {
            requestMessage = buildAuthnRequest(request);
        } else {
            LoggedInSessionBean sessionBean = (LoggedInSessionBean) request.getSession(false).
                    getAttribute(SSOAgentConstants.SESSION_BEAN_NAME);
            if (sessionBean != null) {
                requestMessage = buildLogoutRequest(sessionBean.getSAML2SSO().getSubjectId(),
                        sessionBean.getSAML2SSO().getSessionIndex());
            } else {
                throw new SSOAgentException("SLO Request can not be built. SSO Session is NULL");
            }
        }
        String idpUrl = null;

        String encodedRequestMessage = encodeRequestMessage(
                requestMessage, SAMLConstants.SAML2_REDIRECT_BINDING_URI);
        StringBuilder httpQueryString = new StringBuilder(
                SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_AUTH_REQ +
                        "=" + encodedRequestMessage);

        String relayState = ssoAgentConfig.getSAML2().getRelayState();
        if (relayState != null) {
            try {
                httpQueryString.append("&" + RelayState.DEFAULT_ELEMENT_LOCAL_NAME + "=" +
                        URLEncoder.encode(relayState, "UTF-8").trim());
            } catch (UnsupportedEncodingException e) {
                throw new SSOAgentException("Error occurred while URLEncoding " +
                        RelayState.DEFAULT_ELEMENT_LOCAL_NAME, e);
            }
        }

        if (ssoAgentConfig.getSAML2().isRequestSigned()) {
            SSOAgentUtils.addDeflateSignatureToHTTPQueryString(httpQueryString,
                    new X509CredentialImpl(ssoAgentConfig.getSAML2().getSSOAgentX509Credential()));
        }

        if (ssoAgentConfig.getQueryParams() != null && !ssoAgentConfig.getQueryParams().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String[]> entry : ssoAgentConfig.getQueryParams().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue().length > 0) {
                    for (String param : entry.getValue()) {
                        builder.append("&").append(entry.getKey()).append("=").append(param);
                    }
                }
            }
            httpQueryString.append(builder);
        }



        if (ssoAgentConfig.getSAML2().getIdPURL().indexOf("?") > -1) {
            idpUrl = ssoAgentConfig.getSAML2().getIdPURL().concat("&").concat(httpQueryString.toString());
        } else {
            idpUrl = ssoAgentConfig.getSAML2().getIdPURL().concat("?").concat(httpQueryString.toString());
        }
        return idpUrl;
    }

    /**
     * Handles the request for http post binding
     *
     * @param request  The HTTP request with SAML2 message
     * @param response The HTTP response
     * @param isLogout Whether the request is a logout request
     * @throws SSOAgentException
     */
    public String buildPostRequest(HttpServletRequest request, HttpServletResponse response,
                                   boolean isLogout) throws SSOAgentException {

        RequestAbstractType requestMessage = null;
        if (!isLogout) {
            requestMessage = buildAuthnRequest(request);
            if (ssoAgentConfig.getSAML2().isRequestSigned()) {
                requestMessage = SSOAgentUtils.setSignature((AuthnRequest) requestMessage,
                        XMLSignature.ALGO_ID_SIGNATURE_RSA,
                        new X509CredentialImpl(ssoAgentConfig.getSAML2().getSSOAgentX509Credential()));
            }

        } else {
            LoggedInSessionBean sessionBean = (LoggedInSessionBean) request.getSession(false).
                    getAttribute(SSOAgentConstants.SESSION_BEAN_NAME);
            if (sessionBean != null) {
                requestMessage = buildLogoutRequest(sessionBean.getSAML2SSO()
                        .getSubjectId(), sessionBean.getSAML2SSO().getSessionIndex());
                if (ssoAgentConfig.getSAML2().isRequestSigned()) {
                    requestMessage = SSOAgentUtils.setSignature((LogoutRequest) requestMessage,
                            XMLSignature.ALGO_ID_SIGNATURE_RSA,
                            new X509CredentialImpl(ssoAgentConfig.getSAML2().getSSOAgentX509Credential()));
                }
            } else {
                throw new SSOAgentException("SLO Request can not be built. SSO Session is null");
            }
        }
        String encodedRequestMessage = encodeRequestMessage(requestMessage, SAMLConstants.SAML2_POST_BINDING_URI);

        Map<String, String[]> paramsMap = new HashMap<String, String[]>();
        paramsMap.put(SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_AUTH_REQ,
                new String[]{encodedRequestMessage});
        if (ssoAgentConfig.getSAML2().getRelayState() != null) {
            paramsMap.put(RelayState.DEFAULT_ELEMENT_LOCAL_NAME,
                    new String[]{ssoAgentConfig.getSAML2().getRelayState()});
        }

        //Add any additional parameters defined
        if (ssoAgentConfig.getQueryParams() != null && !ssoAgentConfig.getQueryParams().isEmpty()) {
            paramsMap.putAll(ssoAgentConfig.getQueryParams());
        }

        StringBuilder htmlParams = new StringBuilder();
        for (Map.Entry<String, String[]> entry : paramsMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue().length > 0) {
                for (String param : entry.getValue()) {
                    htmlParams.append("<input type='hidden' name='").append(entry.getKey())
                            .append("' value='").append(param).append("'>\n");
                }
            }

        }
        String htmlPayload = ssoAgentConfig.getSAML2().getPostBindingRequestHTMLPayload();
        if (htmlPayload == null || !htmlPayload.contains("<!--$saml_params-->")) {
            htmlPayload = "<html>\n" +
                    "<body>\n" +
                    "<p>You are now redirected back to " + ssoAgentConfig.getSAML2().getIdPURL() + " \n" +
                    "If the redirection fails, please click the post button.</p>\n" +
                    "<form method='post' action='" + ssoAgentConfig.getSAML2().getIdPURL() + "'>\n" +
                    "<p>\n" +
                    htmlParams.toString() +
                    "<button type='submit'>POST</button>\n" +
                    "</p>\n" +
                    "</form>\n" +
                    "<script type='text/javascript'>\n" +
                    "document.forms[0].submit();\n" +
                    "</script>\n" +
                    "</body>\n" +
                    "</html>";
        } else {
            htmlPayload = htmlPayload.replace("<!--$saml_params-->",
                    htmlParams.toString());
        }
        return htmlPayload;

    }

    public void processResponse(HttpServletRequest request, HttpServletResponse response)
            throws SSOAgentException {

        String saml2SSOResponse = request.getParameter(SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_RESP);

        if (saml2SSOResponse != null) {
            String decodedResponse = new String(Base64.decode(saml2SSOResponse), Charset.forName("UTF-8"));
            XMLObject samlObject = SSOAgentUtils.unmarshall(decodedResponse);
            if (samlObject instanceof LogoutResponse) {
                //This is a SAML response for a single logout request from the SP
                doSLO(request);
            } else {
                processSSOResponse(request);
            }
            String relayState = request.getParameter(RelayState.DEFAULT_ELEMENT_LOCAL_NAME);

            if (relayState != null && !relayState.isEmpty() && !"null".equalsIgnoreCase(relayState)) { //additional
                // checks for incompetent IdPs
                ssoAgentConfig.getSAML2().setRelayState(relayState);
            }

        } else {
            throw new SSOAgentException("Invalid SAML2 Response. SAML2 Response can not be null.");
        }
    }

    /**
     * This method handles the logout requests from the IdP
     * Any request for the defined logout URL is handled here
     *
     * @param request
     * @throws javax.servlet.ServletException
     * @throws IOException
     */
    public void doSLO(HttpServletRequest request) throws SSOAgentException {

        XMLObject saml2Object = null;
        if (request.getParameter(SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_AUTH_REQ) != null) {
            saml2Object = SSOAgentUtils.unmarshall(new String(Base64.decode(request.getParameter(
                    SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_AUTH_REQ)), Charset.forName("UTF-8")));
        }
        if (saml2Object == null) {
            saml2Object = SSOAgentUtils.unmarshall(new String(Base64.decode(request.getParameter(
                    SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_RESP)), Charset.forName("UTF-8")));
        }
        if (saml2Object instanceof LogoutRequest) {
            LogoutRequest logoutRequest = (LogoutRequest) saml2Object;
            String sessionIndex = logoutRequest.getSessionIndexes().get(0).getSessionIndex();
            Set<HttpSession> sessions = SSOAgentSessionManager.invalidateAllSessions(sessionIndex);
            for (HttpSession session : sessions) {
                session.invalidate();
            }
        } else if (saml2Object instanceof LogoutResponse) {
            if (request.getSession(false) != null) {
                /**
                 * Not invalidating session explicitly since there may be other listeners
                 * still waiting to get triggered and at the end of the chain session needs to be
                 * invalidated by the system
                 */
                Set<HttpSession> sessions =
                        SSOAgentSessionManager.invalidateAllSessions(request.getSession(false));
                for (HttpSession session : sessions) {
                    try {
                        session.invalidate();
                    } catch (IllegalStateException ignore) {

                        if (log.isDebugEnabled()) {
                            log.debug("Ignoring exception : ", ignore);
                        }
                        //ignore
                        //session is already invalidated
                    }
                }
            }
        } else {
            throw new SSOAgentException("Invalid SAML2 Single Logout Request/Response");
        }
    }

    protected void processSSOResponse(HttpServletRequest request) throws SSOAgentException {

        LoggedInSessionBean sessionBean = new LoggedInSessionBean();
        sessionBean.setSAML2SSO(sessionBean.new SAML2SSO());

        String saml2ResponseString =
                new String(Base64.decode(request.getParameter(
                        SSOAgentConstants.SAML2SSO.HTTP_POST_PARAM_SAML2_RESP)), Charset.forName("UTF-8"));
        Response saml2Response = (Response) SSOAgentUtils.unmarshall(saml2ResponseString);
        sessionBean.getSAML2SSO().setResponseString(saml2ResponseString);
        sessionBean.getSAML2SSO().setSAMLResponse(saml2Response);

        Assertion assertion = null;
        if (ssoAgentConfig.getSAML2().isAssertionEncrypted()) {
            List<EncryptedAssertion> encryptedAssertions = saml2Response.getEncryptedAssertions();
            EncryptedAssertion encryptedAssertion = null;
            if (!CollectionUtils.isEmpty(encryptedAssertions)) {
                encryptedAssertion = encryptedAssertions.get(0);
                try {
                    assertion = getDecryptedAssertion(encryptedAssertion);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Assertion decryption failure : ", e);
                    }
                    throw new SSOAgentException("Unable to decrypt the SAML2 Assertion");
                }
            }
        } else {
            List<Assertion> assertions = saml2Response.getAssertions();
            if (assertions != null && !assertions.isEmpty()) {
                assertion = assertions.get(0);
            }
        }
        if (assertion == null) {
            if (isNoPassive(saml2Response)) {
                LOGGER.log(Level.FINE, "Cannot authenticate in passive mode");
                return;
            }
            throw new SSOAgentException("SAML2 Assertion not found in the Response");
        }

        String idPEntityIdValue = assertion.getIssuer().getValue();
        if (idPEntityIdValue == null || idPEntityIdValue.isEmpty()) {
            throw new SSOAgentException("SAML2 Response does not contain an Issuer value");
        } else if (!idPEntityIdValue.equals(ssoAgentConfig.getSAML2().getIdPEntityId())) {
            throw new SSOAgentException("SAML2 Response Issuer verification failed");
        }
        sessionBean.getSAML2SSO().setAssertion(assertion);
        // Cannot marshall SAML assertion here, before signature validation due to a weird issue in OpenSAML

        // Get the subject name from the Response Object and forward it to login_action.jsp
        String subject = null;
        if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {
            subject = assertion.getSubject().getNameID().getValue();
        }

        if (subject == null) {
            throw new SSOAgentException("SAML2 Response does not contain the name of the subject");
        }


        sessionBean.getSAML2SSO().setSubjectId(subject); // set the subject
        request.getSession().setAttribute(SSOAgentConstants.SESSION_BEAN_NAME, sessionBean);

        // validate audience restriction
        validateAudienceRestriction(assertion);

        // validate signature
        validateSignature(saml2Response, assertion);

        // Marshalling SAML2 assertion after signature validation due to a weird issue in OpenSAML
        sessionBean.getSAML2SSO().setAssertionString(marshall(assertion));

        ((LoggedInSessionBean) request.getSession().getAttribute(
                SSOAgentConstants.SESSION_BEAN_NAME)).getSAML2SSO().
                setSubjectAttributes(getAssertionStatements(assertion));

        //For removing the session when the single sign out request made by the SP itself
        if (ssoAgentConfig.getSAML2().isSLOEnabled()) {
            String sessionId = assertion.getAuthnStatements().get(0).getSessionIndex();
            if (sessionId == null) {
                throw new SSOAgentException("Single Logout is enabled but IdP Session ID not found in SAML2 Assertion");
            }
            ((LoggedInSessionBean) request.getSession().getAttribute(
                    SSOAgentConstants.SESSION_BEAN_NAME)).getSAML2SSO().setSessionIndex(sessionId);
            SSOAgentSessionManager.addAuthenticatedSession(request.getSession(false));
        }

        request.getSession().setAttribute(SSOAgentConstants.SESSION_BEAN_NAME, sessionBean);

    }

    protected LogoutRequest buildLogoutRequest(String user, String sessionIdx) throws SSOAgentException {

        LogoutRequest logoutReq = new LogoutRequestBuilder().buildObject();

        logoutReq.setID(SSOAgentUtils.createID());
        logoutReq.setDestination(ssoAgentConfig.getSAML2().getIdPURL());

        DateTime issueInstant = new DateTime();
        logoutReq.setIssueInstant(issueInstant);
        logoutReq.setNotOnOrAfter(new DateTime(issueInstant.getMillis() + 5 * 60 * 1000));

        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(ssoAgentConfig.getSAML2().getSPEntityId());
        logoutReq.setIssuer(issuer);

        NameID nameId = new NameIDBuilder().buildObject();
        nameId.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
        nameId.setValue(user);
        logoutReq.setNameID(nameId);

        SessionIndex sessionIndex = new SessionIndexBuilder().buildObject();
        sessionIndex.setSessionIndex(sessionIdx);
        logoutReq.getSessionIndexes().add(sessionIndex);

        logoutReq.setReason("Single Logout");

        return logoutReq;
    }

    protected AuthnRequest buildAuthnRequest(HttpServletRequest request) throws SSOAgentException {

        IssuerBuilder issuerBuilder = new IssuerBuilder();
        Issuer issuer =
                issuerBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
                        "Issuer", "samlp");
        issuer.setValue(ssoAgentConfig.getSAML2().getSPEntityId());

		/* NameIDPolicy */
        NameIDPolicyBuilder nameIdPolicyBuilder = new NameIDPolicyBuilder();
        NameIDPolicy nameIdPolicy = nameIdPolicyBuilder.buildObject();
        nameIdPolicy.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        nameIdPolicy.setSPNameQualifier("Issuer");
        nameIdPolicy.setAllowCreate(true);

		/* AuthnContextClass */
        AuthnContextClassRefBuilder authnContextClassRefBuilder = new AuthnContextClassRefBuilder();
        AuthnContextClassRef authnContextClassRef =
                authnContextClassRefBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:assertion",
                        "AuthnContextClassRef",
                        "saml");
        authnContextClassRef.setAuthnContextClassRef("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		/* AuthnContex */
        RequestedAuthnContextBuilder requestedAuthnContextBuilder =
                new RequestedAuthnContextBuilder();
        RequestedAuthnContext requestedAuthnContext = requestedAuthnContextBuilder.buildObject();
        requestedAuthnContext.setComparison(AuthnContextComparisonTypeEnumeration.EXACT);
        requestedAuthnContext.getAuthnContextClassRefs().add(authnContextClassRef);

        DateTime issueInstant = new DateTime();

		/* Creation of AuthRequestObject */
        AuthnRequestBuilder authRequestBuilder = new AuthnRequestBuilder();
        AuthnRequest authRequest =
                authRequestBuilder.buildObject("urn:oasis:names:tc:SAML:2.0:protocol",
                        "AuthnRequest", "samlp");

        authRequest.setForceAuthn(ssoAgentConfig.getSAML2().isForceAuthn());
        authRequest.setIsPassive(ssoAgentConfig.getSAML2().isPassiveAuthn());
        authRequest.setIssueInstant(issueInstant);
        authRequest.setProtocolBinding(ssoAgentConfig.getSAML2().getHttpBinding());
        authRequest.setAssertionConsumerServiceURL(ssoAgentConfig.getSAML2().getACSURL());
        authRequest.setIssuer(issuer);
        authRequest.setNameIDPolicy(nameIdPolicy);
        authRequest.setRequestedAuthnContext(requestedAuthnContext);
        authRequest.setID(SSOAgentUtils.createID());
        authRequest.setVersion(SAMLVersion.VERSION_20);
        authRequest.setDestination(ssoAgentConfig.getSAML2().getIdPURL());
        if (request.getAttribute(Extensions.LOCAL_NAME) != null) {
            authRequest.setExtensions((Extensions) request.getAttribute(Extensions.LOCAL_NAME));
        }

		/* Requesting Attributes. This Index value is registered in the IDP */
        if (ssoAgentConfig.getSAML2().getAttributeConsumingServiceIndex() != null &&
                ssoAgentConfig.getSAML2().getAttributeConsumingServiceIndex().trim().length() > 0) {
            authRequest.setAttributeConsumingServiceIndex(Integer.parseInt(
                    ssoAgentConfig.getSAML2().getAttributeConsumingServiceIndex()));
        }

        return authRequest;
    }

    protected String encodeRequestMessage(RequestAbstractType requestMessage, String binding)
            throws SSOAgentException {

        Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(requestMessage);
        Element authDOM = null;
        try {
            authDOM = marshaller.marshall(requestMessage);
            StringWriter rspWrt = new StringWriter();
            XMLHelper.writeNode(authDOM, rspWrt);
            if (SAMLConstants.SAML2_REDIRECT_BINDING_URI.equals(binding)) {
                //Compress the message, Base 64 encode and URL encode
                Deflater deflater = new Deflater(Deflater.DEFLATED, true);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream
                        (byteArrayOutputStream, deflater);
                deflaterOutputStream.write(rspWrt.toString().getBytes(Charset.forName("UTF-8")));
                deflaterOutputStream.close();
                String encodedRequestMessage = Base64.encodeBytes(byteArrayOutputStream
                        .toByteArray(), Base64.DONT_BREAK_LINES);
                return URLEncoder.encode(encodedRequestMessage, "UTF-8").trim();
            } else if (SAMLConstants.SAML2_POST_BINDING_URI.equals(binding)) {
                return Base64.encodeBytes(rspWrt.toString().getBytes(),
                        Base64.DONT_BREAK_LINES);
            } else {
                LOGGER.log(Level.FINE, "Unsupported SAML2 HTTP Binding. Defaulting to " +
                        SAMLConstants.SAML2_POST_BINDING_URI);
                return Base64.encodeBytes(rspWrt.toString().getBytes(),
                        Base64.DONT_BREAK_LINES);
            }
        } catch (MarshallingException e) {
            throw new SSOAgentException("Error occurred while encoding SAML2 request", e);
        } catch (UnsupportedEncodingException e) {
            throw new SSOAgentException("Error occurred while encoding SAML2 request", e);
        } catch (IOException e) {
            throw new SSOAgentException("Error occurred while encoding SAML2 request", e);
        }
    }


    /*
     * Process the response and returns the results
     */
    private Map<String, String> getAssertionStatements(Assertion assertion) {

        Map<String, String> results = new HashMap<String, String>();

        if (assertion != null && assertion.getAttributeStatements() != null) {

            List<AttributeStatement> attributeStatementList = assertion.getAttributeStatements();


            for (AttributeStatement statement : attributeStatementList) {
                List<Attribute> attributesList = statement.getAttributes();
                for (Attribute attribute : attributesList) {
                    Element value = attribute.getAttributeValues().get(0).getDOM();
                    String attributeValue = value.getTextContent();
                    results.put(attribute.getName(), attributeValue);
                }
            }

        }
        return results;
    }

    /**
     * Validate the AudienceRestriction of SAML2 Response
     *
     * @param assertion SAML2 Assertion
     * @return validity
     */
    protected void validateAudienceRestriction(Assertion assertion) throws SSOAgentException {

        if (assertion != null) {
            Conditions conditions = assertion.getConditions();
            if (conditions != null) {
                List<AudienceRestriction> audienceRestrictions = conditions.getAudienceRestrictions();
                if (audienceRestrictions != null && !audienceRestrictions.isEmpty()) {
                    boolean audienceFound = false;
                    for (AudienceRestriction audienceRestriction : audienceRestrictions) {
                        if (audienceRestriction.getAudiences() != null && !audienceRestriction.getAudiences().isEmpty()
                                ) {
                            for (Audience audience : audienceRestriction.getAudiences()) {
                                if (ssoAgentConfig.getSAML2().getSPEntityId().equals(audience.getAudienceURI())) {
                                    audienceFound = true;
                                    break;
                                }
                            }
                        }
                        if (audienceFound) {
                            break;
                        }
                    }
                    if (!audienceFound) {
                        throw new SSOAgentException("SAML2 Assertion Audience Restriction validation failed");
                    }
                } else {
                    throw new SSOAgentException("SAML2 Response doesn't contain AudienceRestrictions");
                }
            } else {
                throw new SSOAgentException("SAML2 Response doesn't contain Conditions");
            }
        }
    }


    /**
     * Validate the signature of a SAML2 Response and Assertion
     *
     * @param response SAML2 Response
     * @return true, if signature is valid.
     */
    protected void validateSignature(Response response, Assertion assertion) throws SSOAgentException {

        if (SSOAgentDataHolder.getInstance().getSignatureValidator() != null) {
            //Custom implemetation of signature validation
            SAMLSignatureValidator signatureValidatorUtility = (SAMLSignatureValidator) SSOAgentDataHolder
                    .getInstance().getSignatureValidator();
            signatureValidatorUtility.validateSignature(response, assertion, ssoAgentConfig);
        } else {
            //If custom implementation not found, Execute the default implementation
            if (ssoAgentConfig.getSAML2().isResponseSigned()) {
                if (response.getSignature() == null) {
                    throw new SSOAgentException("SAML2 Response signing is enabled, but signature element not found in SAML2 Response element");
                } else {
                    try {
                        SignatureValidator validator = new SignatureValidator(
                                new X509CredentialImpl(ssoAgentConfig.getSAML2().getSSOAgentX509Credential()));
                        validator.validate(response.getSignature());
                    } catch (ValidationException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Validation exception : ", e);
                        }
                        throw new SSOAgentException("Signature validation failed for SAML2 Response");
                    }
                }
            }
            if (ssoAgentConfig.getSAML2().isAssertionSigned()) {
                if (assertion.getSignature() == null) {
                    throw new SSOAgentException("SAML2 Assertion signing is enabled, but signature element not found in SAML2 Assertion element");
                } else {
                    try {
                        SignatureValidator validator = new SignatureValidator(
                                new X509CredentialImpl(ssoAgentConfig.getSAML2().getSSOAgentX509Credential()));
                        validator.validate(assertion.getSignature());
                    } catch (ValidationException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Validation exception : ", e);
                        }
                        throw new SSOAgentException("Signature validation failed for SAML2 Assertion");
                    }
                }
            }
        }
    }

    /**
     * Serialize the Auth. Request
     *
     * @param xmlObject
     * @return serialized auth. req
     */
    protected String marshall(XMLObject xmlObject) throws SSOAgentException {

        try {
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
            MarshallerFactory marshallerFactory =
                    org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
            Element element = marshaller.marshall(xmlObject);
            ByteArrayOutputStream byteArrayOutputStrm = new ByteArrayOutputStream();
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            return new String(byteArrayOutputStrm.toByteArray(), Charset.forName("UTF-8"));
        } catch (ClassNotFoundException e) {
            throw new SSOAgentException("Error in marshalling SAML2 Assertion", e);
        } catch (InstantiationException e) {
            throw new SSOAgentException("Error in marshalling SAML2 Assertion", e);
        } catch (MarshallingException e) {
            throw new SSOAgentException("Error in marshalling SAML2 Assertion", e);
        } catch (IllegalAccessException e) {
            throw new SSOAgentException("Error in marshalling SAML2 Assertion", e);
        }
    }

    /**
     * Get Decrypted Assertion
     *
     * @param encryptedAssertion
     * @return
     * @throws Exception
     */
    protected Assertion getDecryptedAssertion(EncryptedAssertion encryptedAssertion) throws SSOAgentException {

        try {
            KeyInfoCredentialResolver keyResolver = new StaticKeyInfoCredentialResolver(
                    new X509CredentialImpl(ssoAgentConfig.getSAML2().getSSOAgentX509Credential()));

            EncryptedKey key = encryptedAssertion.getEncryptedData().
                    getKeyInfo().getEncryptedKeys().get(0);
            Decrypter decrypter = new Decrypter(null, keyResolver, null);
            SecretKey dkey = (SecretKey) decrypter.decryptKey(key, encryptedAssertion.getEncryptedData().
                    getEncryptionMethod().getAlgorithm());
            Credential shared = SecurityHelper.getSimpleCredential(dkey);
            decrypter = new Decrypter(new StaticKeyInfoCredentialResolver(shared), null, null);
            decrypter.setRootInNewDocument(true);
            return decrypter.decrypt(encryptedAssertion);
        } catch (Exception e) {
            throw new SSOAgentException("Decrypted assertion error", e);

        }
    }

    protected boolean isNoPassive(Response response) {

        return response.getStatus() != null &&
                response.getStatus().getStatusCode() != null &&
                response.getStatus().getStatusCode().getValue().equals(StatusCode.RESPONDER_URI) &&
                response.getStatus().getStatusCode().getStatusCode() != null &&
                response.getStatus().getStatusCode().getStatusCode().getValue().equals(
                        StatusCode.NO_PASSIVE_URI);
    }

    public SSOAgentConfig getSsoAgentConfig() {
        return ssoAgentConfig;
    }
}
