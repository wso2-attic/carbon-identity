/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.ui;

import org.apache.axiom.util.UIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml2.core.impl.StatusMessageBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.identity.base.IdentityException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class ErrorResponseBuilder {

    private static Log log = LogFactory.getLog(ErrorResponseBuilder.class);

    //Do the bootstrap first
    static {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            log.error("Errors when bootstrapping the OpenSAML2 library", e);
        }
    }

    private ErrorResponseBuilder() {
    }

    public static String generateErrorneousResponse() {
        Response response = new ResponseBuilder().buildObject();
        response.setIssuer(getIssuer());
        response.setStatus(buildStatus());
        response.setVersion(SAMLVersion.VERSION_20);
        response.setID(UIDGenerator.generateUID());

        try {
            return encode(marshall(response));
        } catch (IdentityException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while encoding.", e);
            }
            return null;
        }
    }


    private static Status buildStatus() {

        Status stat = new StatusBuilder().buildObject();

        //Set the status code
        StatusCode statCode = new StatusCodeBuilder().buildObject();
        statCode.setValue("urn:oasis:names:tc:SAML:2.0:status:Responder");
        stat.setStatusCode(statCode);
        StatusMessage statMesssage = new StatusMessageBuilder().buildObject();
        statMesssage.setMessage("Error when processing the Authentication Request");
        stat.setStatusMessage(statMesssage);

        return stat;
    }

    private static String marshall(XMLObject xmlObject) throws org.wso2.carbon.identity.base.IdentityException {
        try {
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

            MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
            Element element = marshaller.marshall(xmlObject);

            ByteArrayOutputStream byteArrayOutputStrm = new ByteArrayOutputStream();
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl =
                    (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            return byteArrayOutputStrm.toString("UTF-8");
        } catch (Exception e) {
            log.error("Error Serializing the SAML Response");
            throw IdentityException.error("Error Serializing the SAML Response", e);
        }
    }

    private static Issuer getIssuer() {
        Issuer issuer = new IssuerBuilder().buildObject();
        issuer.setValue("WSO2 Identity Server");
        issuer.setFormat("urn:oasis:names:tc:SAML:2.0:nameid-format:entity");
        return issuer;
    }

    public static String encode(String authReq) {
        return Base64.encodeBytes(authReq.getBytes(StandardCharsets.UTF_8));
    }
}
