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
package org.wso2.carbon.identity.sts.passive;

public class ResponseToken {

    // This required parameter specifies the result of the token issuance. This can take
    // the form of the <wsse:RequestSecurityTokenResponse> element, a SOAP
    // security token request response (that is, a <S:Envelope>) as detailed in WSTrust,
    // or a SOAP <S:Fault> element.
    private String results;

    // This optional parameter specifies the context information (if any) passed in with
    // the request. It should be noted that this parameter specifies the context
    // information (if any) passed in with the original request.
    private String context;

    // This parameter specifies a URL to which an HTTP GET can be issued. The result
    // is a document of type text/xml that contains the issuance result. This can either
    // be the <wsse:RequestSecurityTokenResopnse> element, a SOAP response, or a
    // SOAP <S:Fault> element.
    private String responsePointer;
    
    private String replyTo;
    
    private boolean authenticated; 
    
    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getResponsePointer() {
        return responsePointer;
    }

    public void setResponsePointer(String responsePointer) {
        this.responsePointer = responsePointer;
    }    
}
