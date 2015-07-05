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

    // TODO ResponseBuilder
    // TODO ErrorResponseBuilder
}
