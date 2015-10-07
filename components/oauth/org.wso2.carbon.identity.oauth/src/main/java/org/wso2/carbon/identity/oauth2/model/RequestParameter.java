/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth2.model;

import java.io.Serializable;

/**
 * This class is used to store request parameters
 */
public class RequestParameter implements Serializable {

    private static final long serialVersionUID = 8094659952209974793L;

    private String key;
    private String[] values;

    /**
     * Instantiate a RequestParameter object for the given key and value
     *
     * @param key    parameter key
     * @param values parameter values
     */
    public RequestParameter(String key, String... values) {
        this.key = key;
        this.values = values;
    }

    /**
     * Returns the parameter key
     *
     * @return parameter key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the parameter value
     *
     * @return parameter value
     */
    public String[] getValue() {
        return values;
    }
}
