package org.wso2.carbon.identity.sso.saml.dto;

public class SAMLSSOSessionDTO {

    private String httpQueryString;
    private String destination;
    private String relayState;
    private String requestMessageString;
    private String issuer;
    private String requestID;
    private String subject;
    private String relyingPartySessionId;
    private String assertionConsumerURL;
    private String customLoginPage;
    private boolean isIdPInitSSO;
    private SAMLSSOReqValidationResponseDTO validationRespDTO;
    private String sessionId;

    public String getHttpQueryString() {
        return httpQueryString;
    }

    public void setHttpQueryString(String httpQueryString) {
        this.httpQueryString = httpQueryString;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    public String getRequestMessageString() {
        return requestMessageString;
    }

    public void setRequestMessageString(String requestMessageString) {
        this.requestMessageString = requestMessageString;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getRequestID() {
        return requestID;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRelyingPartySessionId() {
        return relyingPartySessionId;
    }

    public void setRelyingPartySessionId(String relyingPartySessionId) {
        this.relyingPartySessionId = relyingPartySessionId;
    }

    public String getAssertionConsumerURL() {
        return assertionConsumerURL;
    }

    public void setAssertionConsumerURL(String assertionConsumerURL) {
        this.assertionConsumerURL = assertionConsumerURL;
    }

    public String getCustomLoginPage() {
        return customLoginPage;
    }

    public void setCustomLoginPage(String customLoginPage) {
        this.customLoginPage = customLoginPage;
    }

    public boolean isIdPInitSSO() {
        return isIdPInitSSO;
    }

    public void setIdPInitSSO(boolean isIdPInitSSO) {
        this.isIdPInitSSO = isIdPInitSSO;
    }

	public SAMLSSOReqValidationResponseDTO getValidationRespDTO() {
		return validationRespDTO;
	}

	public void setValidationRespDTO(
			SAMLSSOReqValidationResponseDTO validationRespDTO) {
		this.validationRespDTO = validationRespDTO;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
}
