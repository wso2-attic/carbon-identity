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
package org.wso2.carbon.identity.sts.mgt.dto;

import java.util.Arrays;

public class CardIssuerDTO {

    private String cardName;
    private int validPeriodInDays;
    private CardIssuerTokenDTO[] supportedTokenTypes;
    private boolean symmetricBinding;

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public int getValidPeriodInDays() {
        return validPeriodInDays;
    }

    public void setValidPeriodInDays(int validPeriodInDays) {
        this.validPeriodInDays = validPeriodInDays;
    }

    public CardIssuerTokenDTO[] getSupportedTokenTypes() {
        return Arrays.copyOf(supportedTokenTypes, supportedTokenTypes.length);
    }

    public void setSupportedTokenTypes(CardIssuerTokenDTO[] supportedTokenTypes) {
        this.supportedTokenTypes = Arrays.copyOf(supportedTokenTypes, supportedTokenTypes.length);
    }

    public boolean isSymmetricBinding() {
        return symmetricBinding;
    }

    public void setSymmetricBinding(boolean symmetricBinding) {
        this.symmetricBinding = symmetricBinding;
    }

}
