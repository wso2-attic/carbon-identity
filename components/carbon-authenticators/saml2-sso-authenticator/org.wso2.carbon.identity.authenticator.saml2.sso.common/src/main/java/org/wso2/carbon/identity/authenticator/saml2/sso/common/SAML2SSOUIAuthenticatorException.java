/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.authenticator.saml2.sso.common;

/**
 * This class is used for denote the exceptions thrown from the SAML2 based SSO Authenticator module
 */
public class SAML2SSOUIAuthenticatorException extends Exception {

    public SAML2SSOUIAuthenticatorException() {
        super();
    }

    public SAML2SSOUIAuthenticatorException(String message) {
        super(message);
    }

    public SAML2SSOUIAuthenticatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
