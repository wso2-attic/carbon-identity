/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth.tokenprocessor;

import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;

/**
 * <Code>PlainTextPersistenceProcessor</Code> stores keys and secrets
 * in plain text in the database.
 */

public class PlainTextPersistenceProcessor implements TokenPersistenceProcessor {

    public String getProcessedClientId(String clientId) throws IdentityOAuth2Exception {
        return clientId;
    }

    public String getPreprocessedClientId(String processedClientId) throws IdentityOAuth2Exception {
        return processedClientId;
    }

    public String getProcessedClientSecret(String clientSecret) throws IdentityOAuth2Exception {
        return clientSecret;
    }

    public String getPreprocessedClientSecret(String processedClientSecret) throws IdentityOAuth2Exception {
        return processedClientSecret;
    }

    public String getProcessedAuthzCode(String authzCode) throws IdentityOAuth2Exception {
        return authzCode;
    }

    public String getPreprocessedAuthzCode(String processedAuthzCode) throws IdentityOAuth2Exception {
        return processedAuthzCode;
    }

    public String getProcessedAccessTokenIdentifier(String accessTokenIdentifier) throws IdentityOAuth2Exception {
        return accessTokenIdentifier;
    }

    public String getPreprocessedAccessTokenIdentifier(String processedAccessTokenIdentifier)
            throws IdentityOAuth2Exception {
        return processedAccessTokenIdentifier;
    }

    public String getProcessedRefreshToken(String refreshToken) throws IdentityOAuth2Exception {
        return refreshToken;
    }

    public String getPreprocessedRefreshToken(String processedRefreshToken)
            throws IdentityOAuth2Exception {
        return processedRefreshToken;
    }
}
