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

public class RequestToken {

    // This required parameter specifies the action to be performed. By including the
    // action, URIs can be overloaded to perform multiple functions.
    private String action;

    // This optional parameter is the URL to which responses are directed.
    private String replyTo;

    // This optional parameter is the URL for the resource accessed.
    private String responseTo;

    // This optional parameter is an opaque context value that MUST be returned with
    // the issued token if it is passed in the request.
    private String context;

    // This optional parameter is the URL for the policy which can be obtained using an
    // HTTP GET and identifies the policy to be used related to the action specified in
    // "wa", but MAY have a broader scope than just the "wa". Refer to WS-Policy and
    // WS-Trust for details on policy and trust. This attribute is only used to reference
    // policy documents.
    private String policy;

    // This optional parameter indicates the current time at the recipient for ensuring
    // freshness. This parameter is the string encoding of time using the XML Schema
    // date-time time using UTC notation.
    private String currentTimeAtRecepient;

    // This optional parameter is the URI of the requesting realm. This should be
    // specified if it isn't obvious from the request (e.g. the wreply parameter). ). The
    // wtrealm SHOULD be a security realm of the resource in which nobody (except the
    // resource or authorized delegates) can control URLs.
    private String realm;

    // This optional parameter specifies a token request using either a
    // <wsse:RequestSecurityToken> element or a full request message as described
    // in WS-Trust. If this parameter is not specified, it is assumed that the responding
    // service knows the correct type of token to return.
    private String request;

    // This optional parameter specifies a URL for where to find the request (wreq
    // parameter).
    private String requestPointer;

    // This required parameter specifies the attribute request. The syntax is specific to
    // the attribute store being used and is not mandated by this specification. This
    // attribute is only present on the request.
    private String attributes;
    
    //This required parameter specifies the pseudonym request and either contains a
    //SOAP envelope or an attribute request, such as <wsse:GetPseudonym>. This
    //attribute is only present on the request.
    private String pseudo;

    // The user name used to login to the passive STS
    private String userName;

    // The password used to login to the passive STS
    private String password;

    private String dialect;
    
    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getResponseTo() {
        return responseTo;
    }

    public void setResponseTo(String responseTo) {
        this.responseTo = responseTo;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getCurrentTimeAtRecepient() {
        return currentTimeAtRecepient;
    }

    public void setCurrentTimeAtRecepient(String currentTimeAtRecepient) {
        this.currentTimeAtRecepient = currentTimeAtRecepient;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRequestPointer() {
        return requestPointer;
    }

    public void setRequestPointer(String requestPointer) {
        this.requestPointer = requestPointer;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }
}
