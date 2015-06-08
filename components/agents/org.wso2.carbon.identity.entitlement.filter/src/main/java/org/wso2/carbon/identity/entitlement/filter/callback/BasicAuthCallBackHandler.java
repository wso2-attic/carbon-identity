/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.entitlement.filter.callback;

import org.apache.commons.codec.binary.Base64;
import org.wso2.carbon.identity.entitlement.filter.exception.EntitlementFilterException;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class BasicAuthCallBackHandler extends EntitlementFilterCallBackHandler {

    private static final String AUTHORIZATION = "Authorization";
    private static final String NULL_STRING = "null";

    public BasicAuthCallBackHandler(HttpServletRequest request) throws EntitlementFilterException {
        String authHeaderEn = null;
        if (!(request.getHeader(AUTHORIZATION) == null || NULL_STRING.equals(request.getHeader(AUTHORIZATION)))) {
            authHeaderEn = request.getHeader(AUTHORIZATION);
            String[] tempArr = authHeaderEn.split(" ");
            if (tempArr.length == 2) {
                String authHeaderDc = null;
                try {
                    authHeaderDc = new String(Base64.decodeBase64(tempArr[1].getBytes(Charset.forName("UTF-8"))),
                            "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new EntitlementFilterException("authentication codec error", e);
                }
                tempArr = authHeaderDc.split(":");
                if (tempArr.length == 2) {
                    setUserName(tempArr[0]);
                }
            }
        } else {
            throw new EntitlementFilterException("Unable to retrieve username from Authorization header");
        }
    }
}
