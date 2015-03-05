package org.wso2.carbon.identity.oauth2.dto;

public class OAuth2ClientApplicationDTO {

    private String consumerKey;
    private OAuth2TokenValidationResponseDTO accessTokenValidationResponse;

    /**
     * @return
     */
    public String getConsumerKey() {
        return consumerKey;
    }

    /**
     * @param consumerKey
     */
    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    /**
     * @return
     */
    public OAuth2TokenValidationResponseDTO getAccessTokenValidationResponse() {
        return accessTokenValidationResponse;
    }

    /**
     * @param accessTokenValidationResponse
     */
    public void setAccessTokenValidationResponse(
            OAuth2TokenValidationResponseDTO accessTokenValidationResponse) {
        this.accessTokenValidationResponse = accessTokenValidationResponse;
    }

}
