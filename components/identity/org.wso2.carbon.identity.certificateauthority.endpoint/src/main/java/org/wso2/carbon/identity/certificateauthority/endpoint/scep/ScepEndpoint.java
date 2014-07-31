/*
* Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
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

package org.wso2.carbon.identity.certificateauthority.endpoint.scep;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCRLStore;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAbsentContent;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.jscep.asn1.IssuerAndSubject;
import org.jscep.message.*;
import org.jscep.transaction.FailInfo;
import org.jscep.transaction.MessageType;
import org.jscep.transaction.Nonce;
import org.jscep.transaction.TransactionId;
import org.jscep.transport.request.Operation;
import org.wso2.carbon.identity.certificateauthority.CRLService;
import org.wso2.carbon.identity.certificateauthority.CaException;
import org.wso2.carbon.identity.certificateauthority.ScepServices;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Path("/scep")
public class ScepEndpoint {

    private static final Log log = LogFactory.getLog(ScepEndpoint.class);

    private ScepServices scepServices = new ScepServices();

    @Path("/{tenantID}")
    public Response service(@Context HttpServletRequest request, @PathParam("tenantID") String tenant) {
        try {
            int tenantId = Integer.parseInt(tenant);
            Operation operation;

            //Check operation validity
            operation = getOperation(request);
            if (operation == null) {
                return ResponseUtils.badRequest("Missing 'operation' parameter.");
            }

            //check request type validity
            String reqMethod = request.getMethod();
            if (operation == Operation.PKI_OPERATION) {
                if (!reqMethod.equals(ScepConstants.POST) && !reqMethod.equals(ScepConstants.GET)) {
                    return ResponseUtils.methodNotAllowed(ScepConstants.GET + ", " + ScepConstants.POST);
                }
            } else {
                if (!reqMethod.equals(ScepConstants.GET)) {
                    return ResponseUtils.methodNotAllowed(ScepConstants.GET);
                }
            }

            switch (operation) {
                case GET_CA_CAPS:
                    return getCaCaps();
                case GET_CA_CERT:
                    return getCaCert(tenant);
                case GET_NEXT_CA_CERT:
                    return getNextCaCert(tenant);
                case PKI_OPERATION:
                    return pkiOperation(tenantId, request);
            }

        } catch (NumberFormatException e) {
            log.error("Invalid tenant id " + tenant);
            ResponseUtils.notFound();
        } catch (IllegalArgumentException e) {
            return ResponseUtils.badRequest("Invalid 'operation' parameter.");
        }
        return ResponseUtils.badRequest("Cannot handle the operation");

    }

    private Operation getOperation(final HttpServletRequest req) {
        String op = req.getParameter(ScepConstants.OP_PARAM);
        if (op == null) {
            return null;
        }
        return Operation.forName(req.getParameter(ScepConstants.OP_PARAM));
    }

    private byte[] getMessageBytes(final HttpServletRequest req) throws IOException {
        if (req.getMethod().equals(ScepConstants.POST)) {
            return IOUtils.toByteArray(req.getInputStream());
        } else {
            Operation op = getOperation(req);
            if (op == Operation.PKI_OPERATION) {
                String msg = req.getParameter(ScepConstants.MESSAGE_PARAM);
                if (msg.length() == 0) {
                    return new byte[0];
                }
                return Base64.decode(msg);
            } else {
                return new byte[0];
            }
        }
    }

    private Response getCaCaps() {
        return Response.ok()
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(ScepConstants.CA_CAPS)
                .build();
    }

    private Response getCaCert(String tenantId) {
        try {
            int tId = Integer.parseInt(tenantId);
            X509Certificate caCert = scepServices.getCaCert(tId);
            if (caCert != null) {
                Response.ok()
                        .type("application/x-x509-ca-cert")
                        .entity(caCert.getEncoded());
            }
        } catch (NumberFormatException e) {
            return ResponseUtils.notFound();
        } catch (CertificateEncodingException e) {
            return ResponseUtils.serverError();
        } catch (Exception e) {
            //occurs when invalid tenant id is given
            log.warn("Certificate for tenant "+tenantId+" is requested, but was not available");
            return ResponseUtils.notFound();
        }
        return ResponseUtils.notFound();
    }

    private Response getNextCaCert(String tenantId) {
        return getCaCert(tenantId);
    }

    private Response pkiOperation(int tenantId, HttpServletRequest request) {
        try {
            byte[] body = getMessageBytes(request);
            CMSSignedData sd = new CMSSignedData(body);

            Store reqStore = sd.getCertificates();
            Collection<X509CertificateHolder> reqCerts = reqStore.getMatches(null);

            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509CertificateHolder holder = reqCerts.iterator().next();
            ByteArrayInputStream bais = new ByteArrayInputStream(
                    holder.getEncoded());
            X509Certificate reqCert = (X509Certificate) factory.generateCertificate(bais);


            PkcsPkiEnvelopeDecoder envDecoder = new PkcsPkiEnvelopeDecoder(
                    getRecipient(tenantId), getRecipientKey(tenantId));
            PkiMessageDecoder decoder = new PkiMessageDecoder(reqCert,
                    envDecoder);
            PkiMessage<?> msg = decoder.decode(sd);
            MessageType msgType = msg.getMessageType();
            Object msgData = msg.getMessageData();

            Nonce senderNonce = Nonce.nextNonce();
            TransactionId transId = msg.getTransactionId();
            Nonce recipientNonce = msg.getSenderNonce();
            CertRep certRep;

            if (msgType == MessageType.GET_CERT) {
                final IssuerAndSerialNumber iasn = (IssuerAndSerialNumber) msgData;
                final X509Name principal = iasn.getName();
                final BigInteger serial = iasn.getSerialNumber().getValue();
                List<X509Certificate> issued = doGetCert(principal, serial);
                if (issued.size() == 0) {
                    certRep = new CertRep(transId, senderNonce,
                            recipientNonce, FailInfo.badCertId);
                } else {
                    CMSSignedData messageData = getMessageData(issued);

                    certRep = new CertRep(transId, senderNonce,
                            recipientNonce, messageData);
                }
            } else if (msgType == MessageType.GET_CERT_INITIAL) {
                final IssuerAndSubject ias = (IssuerAndSubject) msgData;
                final X500Name issuer = X500Name.getInstance(ias.getIssuer());
                final X500Name subject = X500Name.getInstance(ias.getSubject());

                try {
                    List<X509Certificate> issued = doGetCertInitial(tenantId, issuer,
                            subject, transId);

                    if (issued.size() == 0) {
                        certRep = new CertRep(transId, senderNonce,
                                recipientNonce);
                    } else {
                        CMSSignedData messageData = getMessageData(issued);

                        certRep = new CertRep(transId, senderNonce,
                                recipientNonce, messageData);
                    }
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            } else if (msgType == MessageType.GET_CRL) {
                final IssuerAndSerialNumber iasn = (IssuerAndSerialNumber) msgData;
                final X509Name issuer = iasn.getName();
                final BigInteger serialNumber = iasn.getSerialNumber()
                        .getValue();

                try {
                    CMSSignedData messageData = getMessageData(doGetCrl(tenantId, issuer, serialNumber));

                    certRep = new CertRep(transId, senderNonce, recipientNonce,
                            messageData);
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            } else if (msgType == MessageType.PKCS_REQ) {
                final PKCS10CertificationRequest certReq = (PKCS10CertificationRequest) msgData;

                try {
                    List<X509Certificate> issued = doEnrol(certReq, transId, tenantId);

                    if (issued.size() == 0) {
                        certRep = new CertRep(transId, senderNonce,
                                recipientNonce);
                    } else {
                        CMSSignedData messageData = getMessageData(issued);

                        certRep = new CertRep(transId, senderNonce,
                                recipientNonce, messageData);
                    }
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            } else {
                log.error("Unknown message for operation");
                return ResponseUtils.badRequest("Unknown message for operation");
            }

            PkcsPkiEnvelopeEncoder envEncoder = new PkcsPkiEnvelopeEncoder(
                    reqCert, "DESede");
            PkiMessageEncoder encoder = new PkiMessageEncoder(getSignerKey(tenantId),
                    getSigner(tenantId), envEncoder);
            CMSSignedData signedData = encoder.encode(certRep);

            return Response.ok()
                    .type("application/x-pki-message")
                    .entity(signedData.getEncoded())
                    .build();
        } catch (Exception e) {
            log.error(e);
            return ResponseUtils.serverError();
        }
    }

    private CMSSignedData getMessageData(X509CRL crl) throws CRLException, CMSException {
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        JcaCRLStore store;
        if (crl == null) {
            store = new JcaCRLStore(Collections.emptyList());
        } else {
            store = new JcaCRLStore(Collections.singleton(crl));
        }
        generator.addCertificates(store);
        return generator.generate(new CMSAbsentContent());
    }

    private X509CRL doGetCrl(int tenantId, X509Name issuer, BigInteger serialNumber)
            throws CertificateException, CaException {
        CRLService crlService = new CRLService();
        return crlService.getLatestX509Crl(tenantId);
    }

    private List<X509Certificate> doEnrol(PKCS10CertificationRequest certReq,
                                          TransactionId transId, int tenantId)
            throws CaException {
        scepServices.addCsr(certReq, transId.toString(), tenantId);
        return Collections.emptyList();
    }

    private X509Certificate[] getSignerCertificateChain(int tenantId) throws Exception {
        return new X509Certificate[]{scepServices.getCaCert(tenantId)};
    }

    private X509Certificate getSigner(int tenantId) throws Exception {
        return scepServices.getCaCert(tenantId);
    }

    private PrivateKey getSignerKey(int tenantId) throws Exception {
        return scepServices.getCaKey(tenantId);
    }

    private List<X509Certificate> doGetCertInitial(int tenantId, X500Name issuer, X500Name subject,
                                                   TransactionId transId) {
        try {
            X509Certificate certificate = scepServices.getCertificate(tenantId, transId.toString());
            if (certificate != null) {
                return Collections.singletonList(certificate);
            }
        } catch (CaException e) {
            log.error("Error retrieving certificate", e);
        }
        return Collections.emptyList();
    }

    private CMSSignedData getMessageData(final List<X509Certificate> certs)
            throws IOException, CMSException, GeneralSecurityException {
        CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
        JcaCertStore store = new JcaCertStore(certs);
        generator.addCertificates(store);
        return generator.generate(new CMSAbsentContent());
    }

    private X509Certificate getRecipient(int tenantId) throws Exception {
        return scepServices.getCaCert(tenantId);
    }

    private PrivateKey getRecipientKey(int tenantId) throws Exception {
        return scepServices.getCaKey(tenantId);
    }

    private List<X509Certificate> doGetCert(X509Name principal, BigInteger serial) {
        try {
            X509Certificate certificate = scepServices.getCertificate(serial.toString());
            if (certificate != null) {
                return Collections.singletonList(certificate);
            }
        } catch (CaException e) {
            log.error("Error retrieving the certificate");
        }
        return null;
    }
}
