/*
 *
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apacheds;

/**
 * An enum to represent supported password hashing mechanisms. Currently on MD 5 and SHA digests
 * are supported.
 */
public enum PasswordAlgorithm {

    PLAIN_TEXT("Plaintext"),
    MD5("MD5"),
    SHA("SHA");

    private String algorithmName;

    private PasswordAlgorithm(String algorithm) {
        this.algorithmName = algorithm;
    }

    public String getAlgorithmName() {
        return this.algorithmName;
    }

}
