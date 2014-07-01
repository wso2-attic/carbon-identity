package org.wso2.carbon.identity.certificateauthority.utils;

import org.bouncycastle.openssl.PEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;

public class CertificateUtils {

    public static String getEncodedCertificate(X509Certificate certificate) {
        try {
            StringWriter stringWriter = new StringWriter();
            PEMWriter writer = new PEMWriter(stringWriter);
            writer.writeObject(certificate);
            writer.close();
            return stringWriter.toString();
        } catch (IOException ignored) {
        }
        return null;
    }
}
