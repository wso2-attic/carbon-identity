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
 *
 */
package org.wso2.carbon.identity.application.authentication.framework.inbound;

import javax.servlet.http.Cookie;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class InboundAuthenticationResponse implements Serializable {

    private Map<String, String> responseHeaders = new HashMap<String, String>();
    private Map<String, Cookie> cookies = new HashMap<String, Cookie>();
    private Map<String, String> parameters = new HashMap<String, String>();
    private int statusCode;
    private String redirectURL;
    private InboundAuthenticationRequestProcessor inboundAuthenticationRequestProcessor;
    private InboundAuthenticationResponseBuilder inboundAuthenticationResponseBuilder;

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void addResponseHeader(String key, String values) {
        responseHeaders.put(key, values);
    }

    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    public void addCookie(String key, Cookie values) {
        cookies.put(key, values);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public InboundAuthenticationRequestProcessor getInboundAuthenticationRequestProcessor() {
        return inboundAuthenticationRequestProcessor;
    }

    public void setInboundAuthenticationRequestProcessor(
            InboundAuthenticationRequestProcessor inboundAuthenticationRequestProcessor) {
        this.inboundAuthenticationRequestProcessor = inboundAuthenticationRequestProcessor;
    }

    public InboundAuthenticationResponseBuilder getInboundAuthenticationResponseBuilder() {
        return inboundAuthenticationResponseBuilder;
    }

    public void setInboundAuthenticationResponseBuilder(
            InboundAuthenticationResponseBuilder inboundAuthenticationResponseBuilder) {
        this.inboundAuthenticationResponseBuilder = inboundAuthenticationResponseBuilder;
    }
}
