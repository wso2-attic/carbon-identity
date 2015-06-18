/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.sts.mgt;

import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.ParameterDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.sts.mgt.dto.CardIssuerDTO;
import org.wso2.carbon.identity.sts.mgt.dto.CardIssuerTokenDTO;
import org.wso2.carbon.registry.core.Registry;

import java.util.ArrayList;
import java.util.List;

public class STSAdminService {

    public CardIssuerDTO readCardIssuerConfiguration() throws Exception {
        CardIssuerDTO dto = null;
        IdentityPersistenceManager dbAmin = null;
        ParameterDO param = null;
        List<CardIssuerTokenDTO> supportedTokens = null;
        String[] tokens = null;
        Registry registry = null;

        dbAmin = IdentityPersistenceManager.getPersistanceManager();
        dto = new CardIssuerDTO();
        registry = IdentityTenantUtil.getRegistry();

        supportedTokens = new ArrayList<>();
        param = dbAmin.getParameter(registry, IdentityConstants.PARAM_SUPPORTED_TOKEN_TYPES);

        if (param == null || param.getValue() == null) {
            addParameters(registry);
            param = dbAmin.getParameter(registry, IdentityConstants.PARAM_SUPPORTED_TOKEN_TYPES);
        }

        if (param != null && param.getValue() != null) {
            tokens = param.getValue().split(",");
            CardIssuerTokenDTO token = null;
            for (int i = 0; i < tokens.length; i++) {
                token = new CardIssuerTokenDTO();
                if (tokens[i].trim().length() > 0) {
                    token.setTokenType(getTokenType(tokens[i]));
                    token.setSupported(true);
                    supportedTokens.add(token);
                }
            }
        }

        param = dbAmin.getParameter(registry, IdentityConstants.PARAM_NOT_SUPPORTED_TOKEN_TYPES);
        if (param != null && param.getValue() != null) {
            tokens = param.getValue().split(",");
            CardIssuerTokenDTO token = null;
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].trim().length() > 0) {
                    token = new CardIssuerTokenDTO();
                    token.setTokenType(getTokenType(tokens[i]));
                    token.setSupported(false);
                    supportedTokens.add(token);
                }
            }
        }

        dto.setSupportedTokenTypes(supportedTokens.toArray(new CardIssuerTokenDTO[supportedTokens.size()]));

        param = dbAmin.getParameter(registry, IdentityConstants.PARAM_CARD_NAME);
        if (param != null && param.getValue() != null) {
            dto.setCardName(param.getValue());
        }

        param = dbAmin.getParameter(registry, IdentityConstants.PARAM_VALID_PERIOD);
        if (param != null && param.getValue() != null) {
            dto.setValidPeriodInDays(Integer.parseInt(param.getValue()));
        }

        param = dbAmin.getParameter(registry, IdentityConstants.PARAM_USE_SYMM_BINDING);
        if (param != null && param.getValue() != null) {
            if ("true".equals(param.getValue())) {
                dto.setSymmetricBinding(true);
            } else {
                dto.setSymmetricBinding(false);
            }
        }

        return dto;
    }

    public void updateCardIssueConfiguration(CardIssuerDTO issuer) throws Exception {
        IdentityPersistenceManager dbAmin = null;
        CardIssuerTokenDTO[] supportedTokens = null;
        StringBuilder supportedBuffer = null;
        StringBuilder notSupportedBuffer = null;

        dbAmin = IdentityPersistenceManager.getPersistanceManager();

        supportedTokens = issuer.getSupportedTokenTypes();
        supportedBuffer = new StringBuilder();
        notSupportedBuffer = new StringBuilder();

        for (int i = 0; i < supportedTokens.length; i++) {
            if (supportedTokens[i].isSupported()) {
                supportedBuffer.append(getUri(supportedTokens[i].getTokenType()));
            } else {
                notSupportedBuffer.append(getUri(supportedTokens[i].getTokenType()));
            }
        }

        Registry registry = null;
        registry = IdentityTenantUtil.getRegistry();

        dbAmin.createOrUpdateParameter(registry, IdentityConstants.PARAM_SUPPORTED_TOKEN_TYPES,
                                       supportedBuffer.toString());
        dbAmin.createOrUpdateParameter(IdentityTenantUtil.getRegistry(),
                                       IdentityConstants.PARAM_NOT_SUPPORTED_TOKEN_TYPES,
                                       notSupportedBuffer.toString());
        dbAmin.createOrUpdateParameter(registry, IdentityConstants.PARAM_CARD_NAME, issuer.getCardName());
        dbAmin.createOrUpdateParameter(registry, IdentityConstants.PARAM_VALID_PERIOD, String
                .valueOf(issuer.getValidPeriodInDays()));
        dbAmin.createOrUpdateParameter(registry, IdentityConstants.PARAM_USE_SYMM_BINDING, String
                .valueOf(issuer.isSymmetricBinding()));
    }

    private String getUri(String tokenType) {
        if (STSMgtConstants.TokenType.SAML10.equals(tokenType)) {
            return IdentityConstants.SAML10_URL + ",";
        } else if (STSMgtConstants.TokenType.SAML11.equals(tokenType)) {
            return IdentityConstants.SAML11_URL + ",";
        } else if (STSMgtConstants.TokenType.SAML20.equals(tokenType)) {
            return IdentityConstants.SAML20_URL + ",";
        } else if (STSMgtConstants.TokenType.OpenID.equals(tokenType)) {
            return IdentityConstants.OpenId.OPENID_URL + ",";
        } else {
            return tokenType + ",";
        }
    }

    private String getTokenType(String uri) {
        if (uri.equals(IdentityConstants.SAML10_URL)) {
            return STSMgtConstants.TokenType.SAML10;
        } else if (uri.equals(IdentityConstants.SAML11_URL)) {
            return STSMgtConstants.TokenType.SAML11;
        } else if (uri.equals(IdentityConstants.SAML20_URL)) {
            return STSMgtConstants.TokenType.SAML20;
        } else if (uri.equals(IdentityConstants.OpenId.OPENID_URL)) {
            return STSMgtConstants.TokenType.OpenID;
        } else {
            return uri;
        }
    }

    private void addParameters(Registry registry) throws IdentityException {
        IdentityPersistenceManager admin = IdentityPersistenceManager.getPersistanceManager();

        admin.createOrUpdateParameter(registry, IdentityConstants.PARAM_SUPPORTED_TOKEN_TYPES,
                                      IdentityConstants.SAML10_URL + "," + IdentityConstants.SAML11_URL + ","
                                      + IdentityConstants.SAML20_URL + "," + IdentityConstants.OpenId.OPENID_URL);

        admin.createOrUpdateParameter(registry, IdentityConstants.PARAM_CARD_NAME,
                                      IdentityConstants.PARAM_VALUE_CARD_NAME);
        admin.createOrUpdateParameter(registry, IdentityConstants.PARAM_VALID_PERIOD,
                                      IdentityConstants.PARAM_VALUE_VALID_PERIOD);
    }
}
