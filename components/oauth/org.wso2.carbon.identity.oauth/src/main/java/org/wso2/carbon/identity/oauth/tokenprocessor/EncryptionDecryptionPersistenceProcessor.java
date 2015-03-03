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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;

/**
 * An implementation of <Code>TokenPersistenceProcessor</Code>
 * which is used when storing encrypted tokens.
 */
public class EncryptionDecryptionPersistenceProcessor implements TokenPersistenceProcessor {

    protected Log log = LogFactory.getLog(EncryptionDecryptionPersistenceProcessor.class);

    public String getPreprocessedClientId(String processedClientId) throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(processedClientId));
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getProcessedClientId(String clientId) throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(clientId.getBytes());
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getPreprocessedClientSecret(String processedClientSecret) throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(processedClientSecret));
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getProcessedClientSecret(String clientSecret) throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(clientSecret.getBytes());
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getPreprocessedAuthzCode(String processedAuthzCode) throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(processedAuthzCode));
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getProcessedAuthzCode(String authzCode) throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(authzCode.getBytes());
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getPreprocessedAccessTokenIdentifier(String processedAccessTokenIdentifier)
            throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(
                    processedAccessTokenIdentifier));
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getProcessedAccessTokenIdentifier(String accessTokenIdentifier)
            throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(
                    accessTokenIdentifier.getBytes());
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getPreprocessedRefreshToken(String processedRefreshToken)
            throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(
                    processedRefreshToken));
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }

    public String getProcessedRefreshToken(String refreshToken)
            throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(
                    refreshToken.getBytes());
        } catch (CryptoException e) {
            log.debug(e.getMessage(), e);
            throw new IdentityOAuth2Exception(e.getMessage());
        }
    }
}
