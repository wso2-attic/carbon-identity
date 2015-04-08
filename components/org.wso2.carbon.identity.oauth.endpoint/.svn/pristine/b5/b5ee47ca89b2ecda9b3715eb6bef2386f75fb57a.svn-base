/**
 * 
 */
package org.wso2.carbon.identity.oauth.endpoint.user;

import javax.servlet.http.HttpServletRequest;

/**
 * Validates the User Info request against the specification
 * 
 */
public interface UserInfoRequestValidator {

	/**
	 * Validates the UserInfo request and return the access token if validation
	 * succeed. If validation failed, an {@code UserInfoEndpointException} will be
	 * thrown. Scope validation must be done at the access token validation
	 * @param request
	 * @return
	 * @throws UserInfoEndpointException
	 */
	public String validateRequest(HttpServletRequest request) throws UserInfoEndpointException;

}
