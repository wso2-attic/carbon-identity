package org.wso2.carbon.identity.application.authenticator.samlsso.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.opensaml.common.SAMLObject;
import org.opensaml.ws.soap.common.SOAPObjectBuilder;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xml.XMLObjectBuilderFactory;

import static org.apache.commons.httpclient.methods.multipart.StringPart.DEFAULT_CHARSET;


/**
 * Created by nl26303 on 9/28/16.
 */
public class SoapMessageHandler {
    private static final Logger LOG = Logger.getLogger(SoapMessageHandler.class.getName());
    private static final String CONTENT_TYPE = "text/xml; charset=utf-8";


    private SSLSocketFactory sslSocketFactory = null;

    public SoapMessageHandler(SSLSocketFactory socketFactory) {
        sslSocketFactory = socketFactory;
    }

    /**
     * Build a SOAP Message. <br>
     *
     * @param samlMessage
     *            SAMLObject.
     * @return Envelope soap envelope
     */
    @SuppressWarnings("unchecked")
    public Envelope buildSOAPMessage(SAMLObject samlMessage)
    {
        XMLObjectBuilderFactory builderFactory = org.opensaml.xml.Configuration.getBuilderFactory();

        SOAPObjectBuilder<Envelope> envBuilder = (SOAPObjectBuilder<Envelope>) builderFactory
                .getBuilder(Envelope.DEFAULT_ELEMENT_NAME);
        Envelope envelope = envBuilder.buildObject();

        LOG.log(Level.INFO, "Adding SAML message to the SOAP message's body");
        SOAPObjectBuilder<Body> bodyBuilder = (SOAPObjectBuilder<Body>) builderFactory
                .getBuilder(Body.DEFAULT_ELEMENT_NAME);
        Body body = bodyBuilder.buildObject();
        body.getUnknownXMLObjects().add(samlMessage);
        envelope.setBody(body);
        return envelope;
    }

    /**
     * Send SOAP message. <br>
     *
     * @param sMessage
     *            String with message that needs to be send.
     * @param sUrl
     *            String with url to send message to.
     * @return the string
     *
     * @throws Exception
     *             If sending fails.
     */
    public String sendSOAP(String sMessage, String sUrl, Proxy proxy)
            throws Exception
    {
        StringBuilder sb = new StringBuilder();
        URL url;
        HttpURLConnection connection = null;
        HttpsURLConnection  sslconnection = null;

        url = new URL(sUrl);
        try {
            if ( sslSocketFactory != null ) {
                if (proxy != null) {
                    sslconnection = (HttpsURLConnection) url.openConnection(proxy);
                } else {
                    sslconnection = (HttpsURLConnection) url.openConnection();
                }
                sslconnection.setSSLSocketFactory(sslSocketFactory);
                connection = sslconnection;
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // enable sending to connection
            connection.setDoOutput(true);

            // set mime headers
            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            connection.setRequestProperty("Accept", CONTENT_TYPE);
            StringBuilder sbSOAPAction = new StringBuilder("\"");
            sbSOAPAction.append(sUrl).append("\"");
            connection.setRequestProperty("SOAPAction", sbSOAPAction.toString());
            LOG.log(Level.INFO, "Send: Url=" + sUrl + " ContentType=" + CONTENT_TYPE
                    + " Action=" + sbSOAPAction);

            connection.setRequestProperty("Pragma", "no-cache");
            connection.setRequestProperty("Cache-Control", "no-cache, no-store");
            // write message to output
            PrintStream osOutput = new PrintStream((connection).getOutputStream());
            osOutput.println(sMessage);
            osOutput.println("\r\n\r\n");
            osOutput.close();

            int iRetCode = connection.getResponseCode();
            switch (iRetCode) { // switch on HTTP response code
                case 200: // ok
                    LOG.log(Level.INFO, "Response OK: ContentType: "
                            + connection.getContentType());
                    // For now we assume utf-8 (default)
                    sb = new StringBuilder(stream2string(connection.getInputStream()));
                    break;
                case 500: // Internal server error
                    LOG.log(Level.WARNING, "No response from target host. Errorcode: "
                            + iRetCode);
                    break;
                default: // unknown error
                    StringBuilder builder = new StringBuilder("Invalid response from target host: \"");
                    builder.append(connection.getHeaderField(0));
                    builder.append(" \". Errorcode: " + iRetCode);
                    LOG.log(Level.WARNING, builder.toString());
                    break;
            }
        }
        catch (java.net.UnknownHostException eUH) { // target host unknown
            LOG.log(Level.WARNING, "Communication Error", eUH);
            throw new Exception("Error in Communication", eUH);
        }
        catch (java.io.IOException eIO) { // error while connecting,writing or reading
            StringBuilder builder = new StringBuilder("Could not open connection with host: \"");
            builder.append(url.toString());
            LOG.log(Level.WARNING, builder.toString(), eIO);
            throw new Exception("Error in Communication", eIO);
        }
        return sb.toString();
    }

    /**
     * Read bytes from inputstream till empty and convert to string based on supplied charset encoding.
     *
     * @param is
     *            The inputstream to read from.
     * @param enc
     *            The character encoding to use in conversion.
     * @return String containing the data from the inputstream
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @category utility method
     *
     */
    private static String stream2string(InputStream is, String enc)
            throws IOException
    {

        int xRead;
        byte[] ba = new byte[512];
        DataInputStream isInput = new DataInputStream(new BufferedInputStream(is));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // Retrieve message as bytes and put them in a string
        while ((xRead = isInput.read(ba)) != -1) {
            bos.write(ba, 0, xRead);
        }
        return bos.toString(enc);
    }

    /**
     * Stream2string.
     *
     * @param is
     *            the is
     * @return the string
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private static String stream2string(InputStream is)
            throws IOException
    {
        return stream2string(is, "UTF-8");
    }

}
