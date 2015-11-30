/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.tokenprocessor;

import org.apache.commons.io.Charsets;
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

    @Override
    public String getPreprocessedClientId(String processedClientId) throws IdentityOAuth2Exception {
        return processedClientId;
    }

    @Override
    public String getProcessedClientId(String clientId) throws IdentityOAuth2Exception {
        return clientId;
    }

    @Override
    public String getPreprocessedClientSecret(String processedClientSecret) throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(processedClientSecret),
                    Charsets.UTF_8);
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving preprocessed client secret", e);
        }
    }

    @Override
    public String getProcessedClientSecret(String clientSecret) throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(clientSecret.getBytes(Charsets.UTF_8));
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving processed client secret", e);
        }
    }

    @Override
    public String getPreprocessedAuthzCode(String processedAuthzCode) throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(processedAuthzCode), Charsets
                    .UTF_8);
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving preprocessed authorization code", e);
        }
    }

    @Override
    public String getProcessedAuthzCode(String authzCode) throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(authzCode.getBytes(Charsets.UTF_8));
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving processed authorization code", e);
        }
    }

    @Override
    public String getPreprocessedAccessTokenIdentifier(String processedAccessTokenIdentifier)
            throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(
                    processedAccessTokenIdentifier), Charsets.UTF_8);
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving preprocessed access token identifier", e);
        }
    }

    @Override
    public String getProcessedAccessTokenIdentifier(String accessTokenIdentifier)
            throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(
                    accessTokenIdentifier.getBytes(Charsets.UTF_8));
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving processed access token identifier", e);
        }
    }

    @Override
    public String getPreprocessedRefreshToken(String processedRefreshToken)
            throws IdentityOAuth2Exception {
        try {
            return new String(CryptoUtil.getDefaultCryptoUtil().base64DecodeAndDecrypt(
                    processedRefreshToken), Charsets.UTF_8);
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving preprocessed refresh token", e);
        }
    }

    @Override
    public String getProcessedRefreshToken(String refreshToken)
            throws IdentityOAuth2Exception {
        try {
            return CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(
                    refreshToken.getBytes(Charsets.UTF_8));
        } catch (CryptoException e) {
            throw new IdentityOAuth2Exception("Error while retrieving processed refresh token", e);
        }
    }
}
