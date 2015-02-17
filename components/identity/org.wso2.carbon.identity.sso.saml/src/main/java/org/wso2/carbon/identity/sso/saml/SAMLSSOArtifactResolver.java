package org.wso2.carbon.identity.sso.saml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Artifact;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.impl.ArtifactBuilder;
import org.opensaml.saml2.core.impl.ArtifactResolveBuilder;
import org.w3c.dom.Document;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.Registry;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SAMLSSOArtifactResolver {

    private static Log log = LogFactory.getLog(SAMLSSOArtifactResolver.class);
    /**
     * Get the artifact string and include the <ArtifactResolve> in a SOAP message
     * @param artifact
     */
    public String resolveArtifact(String artifact) throws IdentityException, SOAPException, IOException,
            SAXException, ParserConfigurationException, TransformerException, Base64DecodingException, NoSuchAlgorithmException {

        String artifactResolutionServiceUrl = getArtifactResolutionServiceUrl(artifact);
        RequestAbstractType artifactResolveMessage = buildArtifactResolve(artifact);
        SOAPMessage soapRequest = createSOAPMessage(artifactResolveMessage);
        SOAPMessage soapResponse = sendSOAPMessge(soapRequest, artifactResolutionServiceUrl);
        return processSoapResponse(soapResponse);
    }

    /**
     * Extract the SourceID from the Artifact
     * Get the Artifact Resolution Service URL for the SourceID
     * @param artifactString
     * @return
     */
    private String getArtifactResolutionServiceUrl(String artifactString)
            throws Base64DecodingException, IdentityException, NoSuchAlgorithmException {
        byte[] array = Base64.decode(artifactString);
        byte[] sourceID = new byte[20];
        System.arraycopy(array, 4, sourceID, 0, 20);
        SAMLSSOServiceProviderDO serviceProviderDO = getServiceProvider(sourceID);
        if(serviceProviderDO != null) {
            return serviceProviderDO.getArtifactResolutionService();
        }
        return null;
    }

    /**
     * Build the <ArtifactResolve>
     * @param artifactString
     * @return
     */
    private ArtifactResolve buildArtifactResolve(String artifactString) throws IdentityException {

        // ID
        String artifactResolveRandomId = Integer.toHexString(new Double(Math.random()).intValue());

        // Issue Instance
        DateTime issueInstant = new DateTime();

        // Issuer
        Issuer issuer = SAMLSSOUtil.getIssuer();

        // Artifact
        Artifact artifact = new ArtifactBuilder().buildObject();
        artifact.setArtifact(artifactString);

        // ArtifactResolve
        ArtifactResolve artifactResolve = new ArtifactResolveBuilder().buildObject();

        artifactResolve.setID(artifactResolveRandomId);
        artifactResolve.setVersion(SAMLVersion.VERSION_20);
        artifactResolve.setIssueInstant(issueInstant);
        artifactResolve.setIssuer(issuer);
        artifactResolve.setArtifact(artifact);
        return artifactResolve;
    }

    /**
     * Create SOAP message
     * @param artifactResolveMessage <ArtifactResolve> to be included
     * @return
     */
    private SOAPMessage createSOAPMessage(RequestAbstractType artifactResolveMessage)
            throws IdentityException, SOAPException, ParserConfigurationException, IOException, SAXException {

        String marshalledArtifactResolve = SAMLSSOUtil.marshall(artifactResolveMessage);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new ByteArrayInputStream(marshalledArtifactResolve.getBytes("utf-8"))));

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPBody body = soapMessage.getSOAPBody();
        body.addDocument(document);

        if(log.isDebugEnabled()) {
            log.debug(soapMessage);
        }
        return soapMessage;
    }

    /**
     * Send SOAP Message to Artifact Resolution Service
     * @param soapRequest
     */
    private SOAPMessage sendSOAPMessge(SOAPMessage soapRequest, String url) throws SOAPException {

        if(log.isDebugEnabled()) {
            log.debug("Sending soap message");
        }
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();
        SOAPMessage soapResponse = soapConnection.call(soapRequest, url);
        soapConnection.close();
        return soapResponse;
    }

    /**
     * Process SOAP Response message
     * Authentication response or Logout response
     * @param soapResponse
     */
    private String processSoapResponse(SOAPMessage soapResponse) throws SOAPException, TransformerException {

        if(log.isDebugEnabled()) {
            log.debug("Processing soap response");
        }

        SOAPBody soapBody = soapResponse.getSOAPBody();
        Document doc = soapBody.extractContentAsDocument();
        Source source = new DOMSource(doc);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);
        String soapBodyStr = stringWriter.getBuffer().toString();

        if(log.isDebugEnabled()) {
            log.debug(soapBodyStr);
        }

        return soapBodyStr;
    }

    /**
     * Get the Issuer from the sourceID of the Artifact
     * @param sourceID
     * @return
     * @throws IdentityException
     */
    private SAMLSSOServiceProviderDO getServiceProvider(byte[] sourceID)
            throws IdentityException, NoSuchAlgorithmException {

        SAMLSSOServiceProviderDO serviceProviderDO = null;
        IdentityPersistenceManager persistenceManager = IdentityPersistenceManager.getPersistanceManager();
        Registry registry = (Registry) PrivilegedCarbonContext.getThreadLocalCarbonContext().getRegistry(RegistryType.SYSTEM_CONFIGURATION);
        SAMLSSOServiceProviderDO[] serviceProviderDOs = persistenceManager.getServiceProviders(registry);
        for (SAMLSSOServiceProviderDO sp : serviceProviderDOs) {
            String issuerID = sp.getIssuer();
            MessageDigest sha1Digester = MessageDigest.getInstance("SHA-1");
            byte[] array = sha1Digester.digest(issuerID.getBytes());
            if(Arrays.equals(sourceID, array)) {
                serviceProviderDO = sp;
                return serviceProviderDO;
            }
        }
        return serviceProviderDO;
    }

}