package org.wso2.carbon.identity.tomcat.valve;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth2.OAuth2TokenValidationService;
import org.wso2.carbon.identity.oauth2.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

public class OAuthAccessTokenValidatorValve extends ValveBase{

    private static final Log log = LogFactory.getLog(OAuthAccessTokenValidatorValve.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_HEADER_KEYWORD = "Bearer";
    public static final String BEARER_TOKEN_TYPE = "bearer";
    public static final String OAUTH_VALIDATION_RESPONSE = "oauth.access.token.validation.response";


    // By Default No endpoint is protected
    private String endpoints = "";
    private ArrayList<String> protectedEndpoints;

    public OAuthAccessTokenValidatorValve(){
        // async support
        super(true);
    }


    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;

        protectedEndpoints = new ArrayList<>();
        // split the comma delimited string and add to the list of protected endpoints
        for (String endpoint : endpoints.split(",")){
            String temp = endpoint.trim();
            if (!temp.isEmpty()){
                if (temp.startsWith("/") || temp.startsWith("\\")) {
                    temp = temp.substring(1);
                }
                protectedEndpoints.add(temp);
            }
        }
    }


    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        if(log.isDebugEnabled()){
            log.debug("OAuth REST Endpoint protection valve is initialized");
        }
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {

        try {
            // check whether the requested endpoint is protected
            if (checkEndPointIsProtected(request.getRequestURI())) {
                if (log.isDebugEnabled()){
                    log.debug("Request to OAuth protected Endpoint validated : "
                            +request.getHost().getName()+request.getRequestURI());
                }

                // check for the Authorization Header
                if (checkForAuthorizationHeader(request)) {
                    // get the authorization header
                    String authzHeader = request.getHeader(AUTHORIZATION_HEADER).trim();
                    if (log.isDebugEnabled()) {
                        log.debug("Authorization Header : " + authzHeader);
                    }

                    // check whether Bearer keyword is there
                    if (authzHeader.startsWith(BEARER_HEADER_KEYWORD)) {
                        String accessToken = extractAccessToken(authzHeader);

                        OAuth2ClientApplicationDTO clientApplicationDTO = validateAccessToken(accessToken);

                        // set the OAuth access token validation response and the consumer key
                        // within the OAuth2ClientApplicationDTO as an attribute
                        request.setAttribute(OAUTH_VALIDATION_RESPONSE, clientApplicationDTO);

                        if (log.isDebugEnabled()) {
                            log.debug("OAuth access token validation completed for " + accessToken);
                        }

                    } else {
                        log.error("Bearer keyword not found in the header");
                    }

                } else {
                    log.error("Authorization Header not found in the request");
                }
            }else{
                // since the End Point is not protected by OAuth we simply pass it on to the next valve
//                if (log.isDebugEnabled()){
//                    log.debug("Requested Endpoint "+request.getHost().getName()+request.getRequestURI()
//                            +" is not protected, Passing on to the next valve");
//                }
            }
        }catch (Exception ex){
            log.error("Could not handle the request",ex);
        }finally {

            // invoke the next valve
            getNext().invoke(request, response);
        }

    }

    public void addProtectedEndpoints(String url){
        protectedEndpoints.add(url.trim());
    }



    private boolean checkEndPointIsProtected(String requestedURI){
        // substring to remove the preceding file path separators
        String URI = requestedURI.trim();

        if (requestedURI.startsWith("/")){
              URI = URI.substring(1);
        }

        if (requestedURI.endsWith("/")){
            URI = URI.substring(0,URI.length()-1);
        }

        return protectedEndpoints.contains(URI);
    }

    /**
     * Method to validate the access token and return the details about the token and the consumer
     * @param accessTokenIdentifier
     * @return
     */
    private OAuth2ClientApplicationDTO validateAccessToken(String accessTokenIdentifier){

        // create an OAuth token validating request DTO
        OAuth2TokenValidationRequestDTO oauthValidationRequest = new OAuth2TokenValidationRequestDTO();

        // create access token object to validate and populate it
        OAuth2TokenValidationRequestDTO.OAuth2AccessToken accessToken = oauthValidationRequest.new OAuth2AccessToken();
        accessToken.setTokenType(BEARER_TOKEN_TYPE);
        accessToken.setIdentifier(accessTokenIdentifier);

        // set the token to the validation request
        oauthValidationRequest.setAccessToken(accessToken);


        OAuth2TokenValidationService oauthValidationService = new OAuth2TokenValidationService();
        OAuth2ClientApplicationDTO oauthValidationResponse = oauthValidationService
                .findOAuthConsumerIfTokenIsValid(oauthValidationRequest);

        return oauthValidationResponse;
    }


    /**
     * Method to check whether the Authorization Header is present in the request
     * @param request
     * @return
     */
    private boolean checkForAuthorizationHeader(Request request){
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()){
            if(AUTHORIZATION_HEADER.equalsIgnoreCase(headers.nextElement().trim())){
                return true;
            }
        }
        return false;
    }


    /**
     * Method to extract the access token string from the Authorization header
     * @param authorizationHeader String Authorization header value
     * @return String returns the access token
     */
    private String extractAccessToken(String authorizationHeader){

        String accessToken = authorizationHeader.trim().substring(7);

        if (log.isDebugEnabled()){
            log.debug("Extracted Access Token from header : "+accessToken);
        }
        return accessToken;
    }
}
