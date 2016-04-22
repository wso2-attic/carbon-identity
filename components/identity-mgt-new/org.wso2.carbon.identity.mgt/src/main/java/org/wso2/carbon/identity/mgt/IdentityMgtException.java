/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.mgt;

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

public class IdentityMgtException extends Exception {

    public static final int FAILURE = 0;
    public static final int FAILED_AUTHENTICATION = 1;
    public static final int FAILED_ENCRYPTION = 2;

    private static final Map<Integer, String> FAULT_CODE_MAP = new HashMap<>();

    static {

        FAULT_CODE_MAP.put(
                new Integer(IdentityMgtException.FAILURE),
                IdentityMgtConstants.ErrorMessage.FAILURE
        );
        FAULT_CODE_MAP.put(
                new Integer(FAILED_AUTHENTICATION),
                IdentityMgtConstants.ErrorMessage.FAILED_AUTHENTICATION
        );
        FAULT_CODE_MAP.put(
                new Integer(FAILED_ENCRYPTION),
                IdentityMgtConstants.ErrorMessage.FAILED_ENCRYPTION
        );
    }

    private int errorCode;

    IdentityMgtException(){
        super();
    }

    public IdentityMgtException(String message) {
        super(message);
    }

    public IdentityMgtException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentityMgtException(Throwable cause) {
        super(cause);
    }

    public IdentityMgtException(int errorCode){
        this(errorCode, null);
    }

    public IdentityMgtException(int errorCode, Object[] args){
        getMessage(errorCode, args);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    /**
     * get the error message.
     *
     * @param errorCode
     * @param args
     * @return the message belongs to the error code provided
     */
    private static String getMessage(int errorCode, Object[] args) {
        return FAULT_CODE_MAP.get(errorCode);
    }

}

