/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.builders.signature;


import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.RequestAbstractType;
import org.opensaml.saml2.core.StatusResponseType;
import org.opensaml.xml.security.x509.X509Credential;
import org.wso2.carbon.identity.base.IdentityException;

/**
 * Interface to Sign and validate the signature in SAML assertion
 */

public interface SSOSigner {

    public void init() throws IdentityException;

    /**
     *  Validate the  Signature in the SAML Assertion
     *
     * @param request
     *          SAML Assertion, this could be either a SAML Request or a LogoutRequest
     * @param cred
     *          Signature signing credential
     * @param alias
     *          Certificate alias against which the signature is validated.
     * @return  true, if the signature is valid.
     * @throws IdentityException
     */

    public boolean doValidateXMLSignature(RequestAbstractType request,X509Credential cred, String alias) throws IdentityException;

    /**
     * Set the signature to the SAML assertion
     *
     * @param assertion
     *          SAML assertion to be signed
     * @param signatureAlgorithm
     *          Signature algorithm
     * @param cred
     *          Signature signing credential
     * @return  SAML Assertion with a signature
     * @throws IdentityException
     */
    public Assertion doSetSignature(Assertion assertion, String signatureAlgorithm,
                                  X509Credential cred) throws IdentityException;

    /**
     * Sign the Response
     *
     * @param response
     *          Response to be signed
     * @param signatureAlgorithm
     *          Signature algorithm
     * @param cred
     *          Signature signing credential
     * @return
     * @throws IdentityException
     */
    public StatusResponseType doSignResponse(StatusResponseType response, String signatureAlgorithm,
                                                   X509Credential cred) throws IdentityException;
}

