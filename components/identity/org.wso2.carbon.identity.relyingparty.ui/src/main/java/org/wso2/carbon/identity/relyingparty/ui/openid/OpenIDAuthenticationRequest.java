/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.relyingparty.ui.openid;

import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.base.IdentityException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

public class OpenIDAuthenticationRequest {

    private String returnUrl;
    private HttpServletResponse reponse;
    private HttpServletRequest request;
    private String openIDUrl;
    private List<String> requiredClaimURIs = new ArrayList<String>();
    private List<String> optionalClaimURIs = new ArrayList<String>();

    private List<OpenIDAxAttribute> requiredClaims = new ArrayList<OpenIDAxAttribute>();
    private List<OpenIDAxAttribute> optionalClaims = new ArrayList<OpenIDAxAttribute>();

    private String realm;
    private ArrayList<String> requestTypes = new ArrayList<String>();
    private ArrayList<AuthPolicyType> authTypes = new ArrayList<AuthPolicyType>();
    private int maxAuthAge;

    private boolean requestClaimsFromIdP = false;

    public OpenIDAuthenticationRequest(HttpServletRequest request, HttpServletResponse reponse) {
        super();
        this.reponse = reponse;
        this.request = request;
    }

    public String getOpenIDUrl() {
        return openIDUrl;
    }

    public void setOpenIDUrl(String openIDUrl) {
        this.openIDUrl = openIDUrl;
    }

    public ArrayList<String> getRequestTypes() {
        return requestTypes;
    }

    public HttpServletResponse getReponse() {
        return reponse;
    }

    public void setReponse(HttpServletResponse reponse) {
        this.reponse = reponse;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnurl) {
        this.returnUrl = returnurl;
    }

    public int getMaxAuthAge() {
        return maxAuthAge;
    }

    public void setMaxAuthAge(int maxAuthAge) {
        this.maxAuthAge = maxAuthAge;
    }

    public ArrayList<AuthPolicyType> getAuthTypes() {
        return authTypes;
    }

    public boolean getRequestClaimsFromIdP() {
        return requestClaimsFromIdP;
    }

    public void setRequestClaimsFromIdP(boolean requestClaimsFromIdP) {
        this.requestClaimsFromIdP = requestClaimsFromIdP;
    }

    /**
     * Add requested authentication policies
     *
     * @param policyType Requested policy type
     */
    public void addAuthPolicy(AuthPolicyType policyType) {
        if (!authTypes.contains(policyType)) {
            authTypes.add(policyType);
        }
    }

    /**
     * Indicate what sort of attributes being requested.
     *
     * @param requestType OpenIDRequestType
     */
    public void addRequestType(String requestType) {
        if (!requestTypes.contains(requestType)) {
            requestTypes.add(requestType);
        }
    }

    /**
     * Add required attributes for Simple Registration. Make sure you have already set
     * SIMPLE_REGISTRATION as an RequestType before calling this method.
     *
     * @param attribute SReg required attribute
     * @throws RelyingPartyException
     */
    public void addRequiredClaims(String attribute) throws IdentityException {
        addClaims(attribute, requiredClaimURIs);
    }

    /**
     * Add optional attributes for Simple Registration. Make sure you have already set
     * SIMPLE_REGISTRATION as an RequestType before calling this method.
     *
     * @param attribute SReg optional attribute
     * @throws RelyingPartyException
     */
    public void addOptionalClaims(String attribute) throws IdentityException {
        addClaims(attribute, optionalClaimURIs);
    }

    /**
     * Add required attributes for Attribute Exchange. Make sure you have already set
     * ATTRIBUTE_EXCHANGE as an RequestType before calling this method.
     *
     * @param attribute Name of the attribute
     * @param namespace Namespace of the attribute
     * @throws RelyingPartyException
     */
    public void addRequiredClaims(String attribute, String namespace) throws IdentityException {
        addClaims(attribute, namespace, requiredClaims);
    }

    /**
     * Add optional attributes for Attribute Exchange. Make sure you have already set
     * ATTRIBUTE_EXCHANGE as an RequestType before calling this method.
     *
     * @param attribute Name of the attribute
     * @param namespace Namespace of the attribute
     * @throws RelyingPartyException
     */
    public void addOptionalClaims(String attribute, String namespace) throws IdentityException {
        addClaims(attribute, namespace, optionalClaims);
    }

    /**
     * @param attribute
     * @param namespace
     * @param claims
     * @throws RelyingPartyException
     */
    private void addClaims(String attribute, String namespace, List<OpenIDAxAttribute> claims)
            throws IdentityException {

        OpenIDAxAttribute axAttribute = null;

        if (attribute == null || attribute.trim().length() == 0 || namespace == null
                || namespace.trim().length() == 0) {
            throw new IdentityException("invalidInputParams");
        }

        axAttribute = new OpenIDAxAttribute(attribute, namespace);

        for (Object element : claims) {
            if (element instanceof OpenIDAxAttribute) {
                OpenIDAxAttribute attr = (OpenIDAxAttribute) element;
                if (attr.getAttributeName().equalsIgnoreCase(attribute)
                        || attr.getNamespace().equalsIgnoreCase(namespace)) {
                    throw new IdentityException("duplicatedAttributes");
                }
            }
        }

        if (!requestTypes.contains(IdentityConstants.OpenId.ATTRIBUTE_EXCHANGE)) {
            requestTypes.add(IdentityConstants.OpenId.ATTRIBUTE_EXCHANGE);
        }

        claims.add(axAttribute);
    }

    /**
     * @param attribute
     * @param claims
     * @throws RelyingPartyException
     */
    private void addClaims(String attribute, List<String> claims) throws IdentityException {

        if (attribute == null || attribute.trim().length() == 0) {
            throw new IdentityException("invalidInputParams");
        }
        if (claims.contains(attribute)) {
            throw new IdentityException("duplicatedAttributes");
        }
        if (!requestTypes.contains(IdentityConstants.OpenId.SIMPLE_REGISTRATION)) {
            requestTypes.add(IdentityConstants.OpenId.SIMPLE_REGISTRATION);
        }

        claims.add(attribute);
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<String> getRequiredClaimURIs() {
        return requiredClaimURIs;
    }

    public List<String> getOptionalClaimURIs() {
        return optionalClaimURIs;
    }

    public List<OpenIDAxAttribute> getRequiredClaims() {
        return requiredClaims;
    }

    public List<OpenIDAxAttribute> getOptionalClaims() {
        return optionalClaims;
    }


}