/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.identity.certificateauthority.endpoint.scep;

public class ScepConstants {

    public static final String OP_PARAM = "operation";
    public static final String MESSAGE_PARAM = "message";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String CA_CAPS = "POSTPKIOperation\n" +
            "SHA-1";
    //Not included in java 1.6
    public static final int METHOD_NOT_ALLOWED = 405;

}
