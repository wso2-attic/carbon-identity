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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;


public class WebFingerProcessor {
    private static Log log = LogFactory.getLog(WebFingerProcessor.class);
    private static WebFingerProcessor webFingerProcessor = new WebFingerProcessor();
    private MessageContext context;
    private WebFingerProcessor() {
        context = new MessageContext();
        if (log.isDebugEnabled()) {
            log.debug("Initializing OIDCProcessor for OpenID connect discovery processor.");
        }
    }
    public static WebFingerProcessor getInstance() {
        return webFingerProcessor;
    }

    public void validateRequest(HttpServletRequest request, String resource, String rel) throws WebFingerEndPointException{
        this.context.setServletRequest(request);
        WebFingerRequest webFingerRequest = this.context.getRequest();
        webFingerRequest.setRel(rel);
        webFingerRequest.setResource(resource);
        WebFingerRequestValidator validator = new DefaultWebFingerRequestValidator();

        validator.validateRequest(this.context);


    }
    public WebFingerResponse getWebFingerResponse()throws WebFingerEndPointException{return new WebFingerResponse();}
    private OIDProviderIssuer getOIDProviderIssuer() throws WebFingerEndPointException{
        return new OIDProviderIssuer();
    }


    private void getSyntax(){}
}
