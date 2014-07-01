package org.wso2.carbon.identity.certificateauthority;

public class CertificateGenerationException extends Exception {

    public CertificateGenerationException() {
    }

    public CertificateGenerationException(String message) {
        super(message);
    }

    public CertificateGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CertificateGenerationException(Throwable cause) {
        super(cause);
    }
}
