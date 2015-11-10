/*
 * Copyright (c) 2005-2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.provider;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.rahas.RahasData;
import org.w3c.dom.Element;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.WSO2Constants;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Map;

public class IdentityProviderUtil {

    private static boolean intial;

    private IdentityProviderUtil() {
    }

    public static boolean isIntial() {
        return intial;
    }

    public static void setIntial(boolean intial) {
        IdentityProviderUtil.intial = intial;
    }

    public static OMElement createRequestedDisplayToken(OMElement parent, GenericIdentityProviderData data) {

        return createOMElement(parent, IdentityConstants.NS,
                               IdentityConstants.LocalNames.REQUESTED_DISPLAY_TOKEN, IdentityConstants.PREFIX);
    }

    public static OMElement createDisplayToken(OMElement parent, GenericIdentityProviderData data) {

        return createOMElement(parent, IdentityConstants.NS,
                               IdentityConstants.LocalNames.DISPLAY_TOKEN, IdentityConstants.PREFIX);
    }

    public static OMElement createDisplayClaim(OMElement parent, String displayTag,
                                               String displayValue, String uri) {
        OMElement claimElem = createOMElement(parent, IdentityConstants.NS,
                                              IdentityConstants.LocalNames.DISPLAY_CLAIM, IdentityConstants.PREFIX);

        claimElem.addAttribute("Uri", uri, null);

        OMElement tagElem = createOMElement(claimElem, IdentityConstants.NS,
                                            IdentityConstants.LocalNames.DISPLAY_TAG, IdentityConstants.PREFIX);
        tagElem.setText(displayTag);

        OMElement valElem = createOMElement(claimElem, IdentityConstants.NS,
                                            IdentityConstants.LocalNames.DISPLAY_VALUE, IdentityConstants.PREFIX);
        valElem.setText(displayValue);

        return claimElem;
    }

    public static OMElement createOpenIdToken(OMElement parent, GenericIdentityProviderData data) {

        return createOMElement(parent, IdentityConstants.OpenId.OPENID_URL,
                               IdentityConstants.LocalNames.OPEN_ID_TOKEN, IdentityConstants.OpenId.PREFIX);
    }

    private static OMElement createOMElement(OMElement parent, String ns, String ln, String prefix) {

        return parent.getOMFactory().createOMElement(new QName(ns, ln, prefix), parent);

    }

    /**
     * Obtain the applies to host name from the WS-Trust request.
     *
     * @param data Data from WS-Trust request.
     * @return applies to host name if found.
     * @throws IdentityProviderException
     */
    public static String getAppliesToHostName(RahasData data) throws IdentityProviderException {
        // If there's no applies to then we don't have to encrypt
        if (data.getAppliesToEpr() == null) {
            return null;
        }

        String relyingPartyURI = data.getAppliesToAddress();

        if (relyingPartyURI == null) {
            // Addressing policy not used in the policy
            relyingPartyURI = data.getAppliesToEpr().getText();
            if (relyingPartyURI == null) {
                throw new IdentityProviderException("cannotFindRelyingParty");
            }
        }

        URI uri = null;
        try {
            // To get the host name extracted
            uri = new URI(relyingPartyURI);
        } catch (URISyntaxException e) {
            throw new IdentityProviderException("Invalid Uril", e);
        }
        return uri.getHost();
    }
}
