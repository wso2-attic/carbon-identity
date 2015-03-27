package org.wso2.carbon.identity.oauth.common;

import org.apache.amber.oauth2.common.OAuth;
import org.apache.amber.oauth2.common.validators.AbstractValidator;

import javax.servlet.http.HttpServletRequest;

/**
 * Grant validator for JSON Web Tokens
 * For JWT Grant to be valid the required parameters are
 * grant_type and assertion
 */
public class JWTGrantValidator extends AbstractValidator<HttpServletRequest> {

    public JWTGrantValidator() {
        requiredParams.add(OAuth.OAUTH_GRANT_TYPE);
        requiredParams.add(OAuth.OAUTH_ASSERTION);
    }
}