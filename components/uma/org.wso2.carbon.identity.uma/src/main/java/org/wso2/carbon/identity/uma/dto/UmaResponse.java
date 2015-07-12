/*
 *
 *  *
 *  * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *  * /
 *
 */

package org.wso2.carbon.identity.uma.dto;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UmaResponse {

    protected int responseStatus;

    protected String body;

    protected Map<String, String> headers = new HashMap<>();

    protected UmaResponse(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getHeader(String name) {
        return (String)this.headers.get(name);
    }

    public Map<String, String> getHeaders() {
        return this.headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getResponseStatus() {
        return this.responseStatus;
    }

    public void addHeader(String name, String header) {
        this.headers.put(name, header);
    }


    /**
     *  Class for building UMA API Responses
     */
    public static class UmaResponseBuilder{

        protected Map<String, Object> parameters = new HashMap();
        protected int responseCode;


        public UmaResponseBuilder(int responseCode) {
            this.responseCode = responseCode;
        }

        public UmaResponse.UmaResponseBuilder setScope(String value) {
            this.parameters.put("scope", value);
            return this;
        }

        public UmaResponse.UmaResponseBuilder setParam(String key, String value) {
            this.parameters.put(key, value);
            return this;
        }

        public UmaResponse buildJSONResponse() throws IOException {
            UmaResponse umaResponse = new UmaResponse(this.responseCode);

            // get the parameters from the map and build the json string for parameters
            String body = new ObjectMapper().writeValueAsString(this.parameters);
            umaResponse.setBody(body);

            // create the response and set the body
            return umaResponse;
        }

    }

    /**
     *  UmaErrorResponseBuilder class
     */
    public static class UmaErrorResponseBuilder extends UmaResponse.UmaResponseBuilder {
        public UmaErrorResponseBuilder(int responseCode) {
            super(responseCode);
        }

        //TODO define an exception and have a constructor based on it
//        public UmaErrorResponseBuilder error(OAuthProblemException ex) {
//            this.parameters.put("error", ex.getError());
//            this.parameters.put("error_description", ex.getDescription());
//            this.parameters.put("error_uri", ex.getUri());
//            this.parameters.put("state", ex.getState());
//            return this;
//        }

        public UmaErrorResponseBuilder setError(String error) {
            this.parameters.put("error", error);
            return this;
        }

        public UmaErrorResponseBuilder setErrorDescription(String desc) {
            this.parameters.put("error_description", desc);
            return this;
        }

        public UmaErrorResponseBuilder setErrorUri(String state) {
            this.parameters.put("error_uri", state);
            return this;
        }

        public UmaErrorResponseBuilder setState(String state) {
            this.parameters.put("state", state);
            return this;
        }


    }

}
