/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.webfinger;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class WebFingerProcessor {
    private static Log log = LogFactory.getLog(WebFingerProcessor.class);
    private static WebFingerProcessor webFingerProcessor = new WebFingerProcessor();
    private MessageContext context;

    private WebFingerProcessor() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing OIDCProcessor for OpenID connect discovery processor.");
        }
    }

    public static WebFingerProcessor getInstance() {
        return webFingerProcessor;
    }

    public void validateRequest(HttpServletRequest request) throws
            WebFingerEndPointException {
        this.context = new MessageContext();
        WebFingerRequestValidator validator = new DefaultWebFingerRequestValidator();
        WebFingerRequest webFingerRequest = this.context.getRequest();
        List<String> parameters = Collections.list(request.getParameterNames());

        if (parameters.size() != 2 || !parameters.contains(WebFingerConstants.REL) || !parameters.contains
                (WebFingerConstants.RESOURCE)) {
            throw new WebFingerEndPointException(WebFingerEndPointException.ERROR_CODE_INVALID_REQUEST, "Bad Web " +
                    "Finger request.");
        }

        String resource = request.getParameter(WebFingerConstants.RESOURCE);
        webFingerRequest.setRel(request.getParameter(WebFingerConstants.REL));
        webFingerRequest.setResource(resource);
        URLNormalizer.normalizeResource(webFingerRequest);
        webFingerRequest.setServletRequest(request);
        validator.validateRequest(webFingerRequest);

    }

    public WebFingerResponse getWebFingerResponse() throws WebFingerEndPointException, ServerConfigurationException {
        if(this.context == null){
            throw new WebFingerEndPointException(WebFingerEndPointException.ERROR_CODE_INVALID_REQUEST, "Error in " +
                    "processing the request. Bad request parameters.");
        }
        WebFingerRequest request = this.context.getRequest();
        WebFingerResponse response = this.context.getResponse();
        IdentityConfigParser configParser = IdentityConfigParser.getInstance();
        OMElement oidcElement = configParser.getConfigElement(WebFingerConstants.CONFIG_WEBFINGER_TAG);
        if (oidcElement == null) {
            throw new WebFingerEndPointException(WebFingerEndPointException
                    .ERROR_CODE_NO_WEBFINGER_CONFIG, "No WebFinger settings set at the server.");
        }
        OMElement subjectElement = null;
        String userInfo = request.getUserInfo();
        if (userInfo == null || userInfo.isEmpty()) {
            userInfo = WebFingerConstants.CONFIG_DEFAULT_SUBJECT;
        }
        Iterator<OMElement> configurations = oidcElement.getChildrenWithName(getQNameWithIdentityNS
                (WebFingerConstants.CONFIG__WEBFINGER_CONFIG));
        while (configurations.hasNext()) {
            OMElement configuration = configurations.next();
            String subject = configuration.getAttributeValue(new QName(WebFingerConstants.USERINFO));
            if (subject.equals(userInfo)) {
                response.setSubject(request.getResource());
                subjectElement = configuration;
                break;
            }
        }
        //Check whether to give a default OIDCProvider Issuer
        if (subjectElement == null) {
            throw new WebFingerEndPointException(WebFingerEndPointException.ERROR_CODE_INVALID_RESOURCE, "No " +
                    "OpenID Provider Issuer for the provided resource.");
        }
        setLinksInResponse(request.getRel(), response, subjectElement);

        return response;
    }

    public int handleError(WebFingerEndPointException error) {
        if (log.isDebugEnabled()) {
            log.debug(error);
        }
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
    private void setLinksInResponse(String rel, WebFingerResponse response, OMElement subjectElement) {
        Iterator<OMElement> linkSet = subjectElement.getChildrenWithName(getQNameWithIdentityNS(WebFingerConstants
                .CONFIG_LINK));
        while(linkSet.hasNext()){
            OMElement link = linkSet.next();
            String attributeRel = link.getAttributeValue(new QName(WebFingerConstants.REL));
            if(attributeRel.equals(rel)){
                response.addLink(rel,link.getText());
            }
        }
    }

    private QName getQNameWithIdentityNS(String localPart) {
        return new QName(IdentityConfigParser.IDENTITY_DEFAULT_NAMESPACE, localPart);
    }
}
