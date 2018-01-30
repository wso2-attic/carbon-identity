package org.wso2.carbon.identity.application.authenticator.samlsso.handler;

import com.hazelcast.util.StringUtil;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLObjectBuilder;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.impl.SAMLObjectContentReference;
import org.opensaml.saml2.core.Artifact;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.ws.message.encoder.MessageEncodingException;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.encryption.EncryptionConstants;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.impl.SignatureBuilder;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.application.authentication.framework.config.builder.FileBasedConfigurationBuilder;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authenticator.samlsso.exception.SAMLSSOException;
import org.wso2.carbon.identity.application.authenticator.samlsso.manager.DefaultSAML2SSOManager;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SSOConstants;
import org.wso2.carbon.identity.application.authenticator.samlsso.util.SoapMessageHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opensaml.xml.Configuration.getMarshallerFactory;


/**
 * Created by nl26303 on 10/26/16.
 * Handler for Handling SAML artifactResolve response
 */
public class ArtifactResolveHandler {


    private static Log log = LogFactory.getLog(DefaultSAML2SSOManager.class);

    //name of parameters defined in carbon.xml

    private static final String SECURITY_KEYSTORE_LOCATION = "Security.KeyStore.Location";
    private static final String SECURITY_KEYSTORE_TYPE = "Security.KeyStore.Type";

    private AuthenticatorConfig authenticatorConfig;
    private ServerConfiguration serverConfig;


    public ArtifactResolveHandler(String artifactResolveUrl, String artifactResolveIssuer) {
        this.authenticatorConfig =
                FileBasedConfigurationBuilder.getInstance().getAuthenticatorConfigMap()
                        .get(SSOConstants.AUTHENTICATOR_NAME);
        this.serverConfig = ServerConfiguration.getInstance();
    }

    /**
     * create a SAML artifactResolveObject based on given samlArt parameter. This SAML ArtifactResolveObject is wrapped into a soapRequest.
     * A two sided SSL connection is created and the soapRequest is send to an Artifact Resolve Endpoint.
     *
     * @param samlArt SAML Artifact reference needed to get the actual data.
     * @param artifactResolveUrl end url to send soaprequest
     * @param artifactResolveIssuer issuer that needs to be added to the soaprequest.
     * @return String representation of an ArtifactResponse if an ArtifactResponse is found within the returned soapResponse.
     * if the soapResponse doesnot contain a ArtifactResponse then a SAMLSSOException is thrown.
     */
    public String getSAMLArtifactResolveResponse(String samlArt, String artifactResolveUrl, String artifactResolveIssuer) throws SAMLSSOException {

        try {
            SSLSocketFactory sslSocketFactory = createSSLSocketFactory(serverConfig);
            SoapMessageHandler soapMessageHandler = new SoapMessageHandler(sslSocketFactory);
            ArtifactResolve artifactResolve = createArtifactResolveObject(samlArt, artifactResolveIssuer);
            Envelope envelope = soapMessageHandler.buildSOAPMessage(artifactResolve);
            Element envelopeElement = marshallMessage(envelope);

            if (log.isDebugEnabled()) {
                log.info("Writing SOAP Message:\n" + XMLHelper.prettyPrintXML(envelopeElement));
            }

            Proxy proxy = null;

            String url = null;
            if (authenticatorConfig != null) {

                if (!StringUtil.isNullOrEmpty(authenticatorConfig.getParameterMap().get(SSOConstants.ServerConfig.ARTIFACT_RESOLVE_PROXY_HOST))) {
                    String proxyHost = authenticatorConfig.getParameterMap().get(SSOConstants.ServerConfig.ARTIFACT_RESOLVE_PROXY_HOST);
                    String proxyPort = authenticatorConfig.getParameterMap().get(SSOConstants.ServerConfig.ARTIFACT_RESOLVE_PROXY_PORT);
                    InetSocketAddress proxyInet = new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort));
                    proxy = new Proxy(Proxy.Type.HTTP, proxyInet);
                }
            }
            //send the soap message

            String soapResponse = soapMessageHandler.sendSOAP(XMLHelper.nodeToString(envelopeElement), artifactResolveUrl, proxy);
            Pattern p = Pattern.compile("<samlp:ArtifactResponse.+</samlp:ArtifactResponse>", Pattern.DOTALL);
            Matcher m = p.matcher(soapResponse);

            if (m.find()) {
                return m.group(0);
            } else {
                throw new SAMLSSOException("No valid SoapResponse");
            }
        } catch (Exception e) {
            throw new SAMLSSOException(e.getMessage(), e);
        }

    }

    /**
     * Create SAML ArtifactResolve Object via builderfactory and sign the returned object
     *
     * @param sReceivedArtifact object reference to actual data
     * @param artifactResolveIssuer name of issuer that needs to be added to ArtifactResolveObject.
     * @return SAML ArtifactResolve Object.
     */
    private ArtifactResolve createArtifactResolveObject(String sReceivedArtifact, String artifactResolveIssuer) {

        //get the builder factory
        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

        SAMLObjectBuilder<Artifact> artifactBuilder = (SAMLObjectBuilder<Artifact>) builderFactory.getBuilder(Artifact.DEFAULT_ELEMENT_NAME);
        Artifact artifact = artifactBuilder.buildObject();
        artifact.setArtifact(sReceivedArtifact);

        SAMLObjectBuilder<ArtifactResolve> artifactResolveBuilder = (SAMLObjectBuilder<ArtifactResolve>) builderFactory.getBuilder(ArtifactResolve.DEFAULT_ELEMENT_NAME);
        ArtifactResolve artifactResolve = artifactResolveBuilder.buildObject();
        artifactResolve.setVersion(SAMLVersion.VERSION_20);
        artifactResolve.setID(UUID.randomUUID().toString());
        artifactResolve.setIssueInstant(new DateTime());

        SAMLObjectBuilder<Issuer> issuerBuilder = (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);



        Issuer issuer = issuerBuilder.buildObject();
        issuer.setValue(artifactResolveIssuer);
        artifactResolve.setIssuer(issuer);

        artifactResolve.setArtifact(artifact);

        try {
            artifactResolve = (ArtifactResolve) signSamlObject(serverConfig, artifactResolve);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return artifactResolve;
    }

    /**
     * @param serverConfig serverConfig Object
     *                     contains keystore properties as defined in carbon.xml
     * @param obj          Object that needs to be signed
     */
    private SignableSAMLObject signSamlObject(ServerConfiguration serverConfig, SignableSAMLObject obj)
            throws Exception {

        String keyfile = "";
        String keyfilePw = "";
        String keyfileType = "";
        String keyAlias = "";

        if (serverConfig != null) {
            keyfile = serverConfig.getFirstProperty(SECURITY_KEYSTORE_LOCATION);
            keyfilePw = serverConfig.getFirstProperty(SSOConstants.ServerConfig.KEY_PASSWORD);
            keyfileType = serverConfig.getFirstProperty(SECURITY_KEYSTORE_TYPE);
            keyAlias = serverConfig.getFirstProperty(SSOConstants.ServerConfig.KEY_ALIAS);
        }

        if (!obj.isSigned()) {


            Signature signature = new SignatureBuilder().buildObject();
            String signingAlgo;
            //read properties from carbon.xml
            KeyStore ks = loadKeystoreFromResource(keyfile, keyfilePw, keyfileType);
            PrivateKey privKey = (PrivateKey) ks.getKey(keyAlias, keyfilePw.toCharArray());
            if ("RSA".equalsIgnoreCase(privKey.getAlgorithm())) {
                signingAlgo = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256;
            } else {
                signingAlgo = SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA1;
            }

            BasicCredential credential = new BasicCredential();
            credential.setPrivateKey(privKey);
            signature.setSigningCredential(credential);
            signature.setSignatureAlgorithm(signingAlgo);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            obj.setSignature(signature);

            SAMLObjectContentReference contentReference = new SAMLObjectContentReference(obj);
            contentReference.setDigestAlgorithm(EncryptionConstants.ALGO_ID_DIGEST_SHA256);
            signature.getContentReferences().clear();  // must be done after setSignature() (it adds a default to the list)
            signature.getContentReferences().add(contentReference);


            try {
                getMarshallerFactory().getMarshaller(obj).marshall(obj);
            } catch (MarshallingException e) {
                log.error("Cannot marshall object for signature", e);
                throw new Exception("Something went wrong during marshalling", e);
            }
            try {
                Signer.signObject(signature);
            } catch (SignatureException e) {
                log.error("Cannot sign the object", e);
                throw new Exception("Something went wrong setting signature", e);
            }
        } else
            log.info("Object already signed!");

        return obj;
    }

    /*
     *
	 */

    /**
     * @param serverConfig serverConfig Object that contains properties that are configured in carbon.xml
     * @throws GeneralSecurityException
     * @throws IOException              Override this method if you need a non-default sslsocketfactory
     *                                  or set  the security property "ssl.SocketFactory.provider"
     * @return sslsocketfactory or null if creation failed
     */
    private SSLSocketFactory createSSLSocketFactory(final ServerConfiguration serverConfig) throws GeneralSecurityException, IOException {
		    /*
		     * Set up a key manager for client authentication
		     * if asked by the server.  Use the implementation's
		     * default TrustStore and secureRandom routines.
		     */
        SSLSocketFactory factory = null;
        SSLContext ctx;
        KeyManagerFactory kmf;
        KeyStore ks;

        String keyfile = "";
        String keyfilePw = "";
        String keyfileType = "";

        if (serverConfig != null) {
            keyfile = serverConfig.getFirstProperty(SECURITY_KEYSTORE_LOCATION);
            keyfilePw = serverConfig.getFirstProperty(SSOConstants.ServerConfig.KEY_PASSWORD);
            keyfileType = serverConfig.getFirstProperty(SECURITY_KEYSTORE_TYPE);
        }

        char[] kspassphrase = keyfilePw.toCharArray();

        ctx = SSLContext.getInstance("TLS");
        kmf = KeyManagerFactory.getInstance("SunX509");
        ks = loadKeystoreFromResource(keyfile, keyfilePw, keyfileType);

        kmf.init(ks, kspassphrase);

        ctx.init(kmf.getKeyManagers(), null, null);

        factory = ctx.getSocketFactory();

        return factory;

    }

    private KeyStore loadKeystoreFromResource(String resource, String password, String type) {
        try (InputStream is = Files.newInputStream(Paths.get(resource))) {
            KeyStore keystore = KeyStore.getInstance(type);
            keystore.load(is, password.toCharArray());
            return keystore;
        } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Helper method that marshalls the given message.
     *
     * @param message message the marshall and serialize
     * @return marshalled message
     * @throws MessageEncodingException thrown if the give message can not be marshalled into its DOM representation
     */
    private Element marshallMessage(XMLObject message)
            throws MessageEncodingException {

        try {
            Marshaller marshaller = getMarshallerFactory().getMarshaller(message);
            if (marshaller == null) {
                log.info("Unable to marshall message, no marshaller registered for message object: "
                        + message.getElementQName());
                throw new RuntimeException("No marshaller registered for message object: " + message.getElementQName());
            }
            Element messageElem = marshaller.marshall(message);

            return messageElem;
        } catch (MarshallingException e) {
            throw new MessageEncodingException("Encountered error marshalling message into its DOM representation", e);
        }
    }

}
