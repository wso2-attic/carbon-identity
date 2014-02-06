package org.wso2.carbon.identity.application.authenticator.oidc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.amber.oauth2.client.OAuthClient;
import org.apache.amber.oauth2.client.URLConnectionClient;
import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.client.response.OAuthAuthzResponse;
import org.apache.amber.oauth2.client.response.OAuthClientResponse;
import org.apache.amber.oauth2.common.OAuth;
import org.apache.amber.oauth2.common.error.OAuthError;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.amber.oauth2.common.utils.JSONUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorStatus;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.ExternalIdPConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.ApplicationAuthenticationContext;
import org.wso2.carbon.ui.CarbonUIUtil;

public class OpenIDConnectAuthenticator extends AbstractApplicationAuthenticator {

	private static Log log = LogFactory.getLog(OpenIDConnectAuthenticator.class);
	private String authenticatedUser;
	
    public boolean canHandle(HttpServletRequest request) {   
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.canHandle()");
		}
    	
    	// From login page asking for OIDC login
		if (request.getParameter("loginType") != null
				&& OpenIDConnectAuthenticatorConstants.LOGIN_TYPE.equals(request.getParameter("loginType"))) {
			return true;
		}
    	
    	// Check commonauth got an OIDC response
    	if (request.getParameter(OpenIDConnectAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE) != null 
    			&& request.getParameter(OpenIDConnectAuthenticatorConstants.OAUTH2_PARAM_STATE) != null) {
        	return true;
        }
		// TODO : What if IdP failed?
//    	if (request.getParameter(OpenIDConnectAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE) != null 
//    			&& request.getParameter(OpenIDConnectAuthenticatorConstants.OAUTH2_PARAM_STATE) != null) {
//        	return true;
//        }

        return false;
    }

	@Override
	public AuthenticatorStatus authenticate(HttpServletRequest request,
			HttpServletResponse response,
			ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.authenticate()");
		}
    	
		
    	
    	// From login page asking for OIDC login
    	if (request.getParameter("loginType") != null 
    			&& OpenIDConnectAuthenticatorConstants.LOGIN_TYPE.equals(request.getParameter("loginType"))) {
    		sendInitialRequest(request, response, null);
    		return AuthenticatorStatus.CONTINUE;
    	}
    	
    	try {
        	ExternalIdPConfig externalIdPConfig = getIdPConfigs(request, context);
    		String clientId = externalIdPConfig.getClientId();
    		String clientSecret = externalIdPConfig.getClientSecret();
    		String tokenEndPoint = externalIdPConfig.getTokenEndpointUrl();
			
			String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
			callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");

    		OAuthAuthzResponse authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
    		String code = authzResponse.getCode();
			
			OAuthClientRequest accessRequest;
			try {
				accessRequest = OAuthClientRequest
						.tokenLocation(tokenEndPoint)
						.setGrantType(GrantType.AUTHORIZATION_CODE)
						.setClientId(clientId).setClientSecret(clientSecret)
						.setRedirectURI(callbackurl)
						.setCode(code)
						.buildBodyMessage();
			} catch (OAuthSystemException e) {
	        	if (log.isDebugEnabled()) {
	    			log.debug("Exception while building request for request access token", e);
	    		}
		        return AuthenticatorStatus.FAIL;
			}

			// create OAuth client that uses custom http client under the hood
			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			OAuthClientResponse oAuthResponse;
			try {
				oAuthResponse = oAuthClient.accessToken(accessRequest);
			} catch (OAuthSystemException e) {
	        	if (log.isDebugEnabled()) {
	    			log.debug("Exception while requesting access token", e);
	    		}
		        return AuthenticatorStatus.FAIL;
			}

			// TODO : return access token and id token to framework
			String accessToken = oAuthResponse.getParam(OpenIDConnectAuthenticatorConstants.ACCESS_TOKEN);
			String idToken = oAuthResponse.getParam(OpenIDConnectAuthenticatorConstants.ID_TOKEN);

            if (accessToken != null && idToken != null) { 
    				String base64Body = idToken.split("\\.")[1];
    				byte[] decoded = Base64.decodeBase64(base64Body.getBytes());
    				String json = new String(decoded);
    				Map<String, Object> jsonObject = JSONUtils.parseJSON(json);
    				authenticatedUser = (String) jsonObject.get("sub");
    		        request.getSession().setAttribute("username", authenticatedUser); // set the subject
    		    	return AuthenticatorStatus.PASS;
            }
			
        } catch (OAuthProblemException e) {
        	if (log.isDebugEnabled()) {
    			log.debug("Exception while processing OpenID Connect response", e);
    		}
	    } catch (JSONException e) {
        	if (log.isDebugEnabled()) {
    			log.debug("Exception while parsing idToken", e);
    		}
	    }
        return AuthenticatorStatus.FAIL;
	}

	@Override
	public AuthenticatorStatus logout(HttpServletRequest request,
			HttpServletResponse response,
			ApplicationAuthenticationContext context) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.logout()");
		}
		// TODO : Add logic
		return AuthenticatorStatus.PASS;
	}

	@Override
	public void sendInitialRequest(HttpServletRequest request,
			HttpServletResponse response,
			ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.sendInitialRequest()");
		}
        
		try {
        	ExternalIdPConfig externalIdPConfig = getIdPConfigs(request, context);
    		String clientId = externalIdPConfig.getClientId();
    		String authorizationEP = externalIdPConfig.getAuthzEndpointUrl();
			
//			String callbackurl = "https://localhost:9444/commonauth";
			String callbackurl = CarbonUIUtil.getAdminConsoleURL(request);
			callbackurl = callbackurl.replace("commonauth/carbon/", "commonauth");
			
			String state = context.getContextIdentifier();

			OAuthClientRequest authzRequest = OAuthClientRequest
		            .authorizationLocation(authorizationEP)
		            .setClientId(clientId)
		            .setRedirectURI(callbackurl)
		            .setResponseType(OpenIDConnectAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE)
		            .setScope(OpenIDConnectAuthenticatorConstants.OAUTH_OIDC_SCOPE)
		            .setState(state)
		            .buildQueryMessage();
		    response.sendRedirect(authzRequest.getLocationUri());
        } catch (IOException e) {
        	log.error("Exception while sending to the login page", e);
        } catch (OAuthSystemException e) {
        	log.error("Exception while building authorization code request", e);
		}
		return;
	}

	@Override
	public String getAuthenticatedSubject(HttpServletRequest request) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getAuthenticatedSubject()");
		}
    	
		return authenticatedUser;
	}

	@Override
	public String getContextIdentifier(HttpServletRequest request) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getContextIdentifier()");
		}
    	
		return request.getParameter(OpenIDConnectAuthenticatorConstants.OAUTH2_PARAM_STATE);
	}
    
	@Override
    public String getAuthenticatorName() {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getAuthenticatorName()");
		}
    	
	    return OpenIDConnectAuthenticatorConstants.AUTHENTICATOR_NAME;
	}

	@Override
	public String getResponseAttributes(HttpServletRequest request) {
		
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getResponseAttributes()");
		}
		return null;
	}

    
    public ExternalIdPConfig getIdPConfigs(HttpServletRequest request, ApplicationAuthenticationContext context) {
    	
    	if (log.isTraceEnabled()) {
			log.trace("Inside OpenIDConnectAuthenticator.getIdPConfigs()");
		}
    	
//    	String hrdIdP = null;
//    	
//    	if (request.getSession().getAttribute("federated-idp-domain") != null) {
//    		hrdIdP = (String)request.getSession().getAttribute("federated-idp-domain");
//    	} else {
//    		hrdIdP = request.getParameter("fid");
//    	}
    	
//    	if (hrdIdP != null && !hrdIdP.equals("null") && !hrdIdP.isEmpty()) {
//    		request.getSession().setAttribute("federated-idp-domain", hrdIdP);
//    		return ExternalIdPConfigurationReader.getInstance().getIdPConfigs(hrdIdP);
//    	} else {
//    		String defaultIdP = getAuthenticatorConfig().getParameterMap().get(OpenIDConnectAuthenticatorConstants.AuthenticatorConfParams.DEFAULT_IDP_CONFIG);
//    		return ConfigurationFacade.getInstance().getIdPConfig(context.getCurrentStep(), getAuthenticatorName(), defaultIdP);
    		
    		return ConfigurationFacade.getInstance().getIdPConfig(context.getCurrentStep(), getAuthenticatorName(), context.getExternalIdP());
//    	}
    }
}
