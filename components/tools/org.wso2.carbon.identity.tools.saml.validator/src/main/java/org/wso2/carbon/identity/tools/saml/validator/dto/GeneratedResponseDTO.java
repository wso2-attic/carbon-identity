/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.identity.tools.saml.validator.dto;

public class GeneratedResponseDTO {

    private boolean success;
    private String message;
    private String xmlResponse;
    private String encodedResponse;

    public GeneratedResponseDTO() {
    }

    public GeneratedResponseDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public GeneratedResponseDTO(boolean success, String message, String xmlResponse, String encodedResponse) {
        this.success = success;
        this.message = message;
        this.xmlResponse = xmlResponse;
        this.encodedResponse = encodedResponse;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getXmlResponse() {
        return xmlResponse;
    }

    public void setXmlResponse(String xmlResponse) {
        this.xmlResponse = xmlResponse;
    }

    public String getEncodedResponse() {
        return encodedResponse;
    }

    public void setEncodedResponse(String encodedResponse) {
        this.encodedResponse = encodedResponse;
    }
}
