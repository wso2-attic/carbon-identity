/*
 *  Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.base;

import org.apache.commons.collections.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used for creating checked exceptions that can be handled.
 */
public class IdentityException extends Exception {

    private static final long serialVersionUID = -3055060202931592637L;

    private List<ErrorInfo> errorInfoList = new ArrayList<>();

    public class ErrorInfo {

        private String contextId = null;
        private String errorCode  = null;
        private String errorDescription = null;
        private String userErrorDescription = null;
        private Throwable cause = null;
        private Map<String, Object> parameters = new HashMap<>();

        private ErrorInfo(String contextId, String errorCode, String userErrorDescription, String errorDescription,
                         Map<String,Object> parameters, Throwable cause) {

            this.contextId = contextId;
            this.errorCode = errorCode;
            this.userErrorDescription = userErrorDescription;
            this.errorDescription = errorDescription;
            if(MapUtils.isNotEmpty(parameters)){
                this.parameters = parameters;
            }
            this.cause = cause;
        }

        public String getContextId() {
            return contextId;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getUserErrorDescription() {
            return userErrorDescription;
        }

        public Throwable getCause() {
            return cause;
        }

        public void addParameter(String key, Object value) {
            this.parameters.put(key, value);
        }

        public Map<String,Object> getParameters() {
            return this.parameters;
        }

        public Object getParameter(String key) {
            return this.parameters.get(key);
        }
    }

    public IdentityException(String errorDescription) {
        super(errorDescription);
    }

    public IdentityException(String errorDescription, Throwable e) {
        super(errorDescription, e);
    }

    private IdentityException(String contextId, String errorCode, String userErrorDescription, String errorDescription,
                             Map<String,Object> parameters, Throwable cause) {
        super(errorDescription, cause);
    }

    public static IdentityException error(String contextId, String errorCode,
                                                            String userErrorDescription, String errorDescription,
                                                            Map<String,Object> parameters,Throwable cause) {
        IdentityException identityException = new IdentityException(contextId, errorCode, userErrorDescription,
                errorDescription, parameters, cause);
        identityException.addErrorInfo(contextId, errorCode, userErrorDescription, errorDescription, parameters,
                cause);
        return identityException;
    }

    public IdentityException addErrorInfo(ErrorInfo errorInfo) {
        this.errorInfoList.add(errorInfo);
        return this;
    }

    public IdentityException addErrorInfo(String contextId, String errorCode, String userErrorDescription,
                                     String errorDescription, Map<String,Object> parameters, Throwable cause) {

        this.errorInfoList.add(buildErrorInfo(contextId, errorCode, userErrorDescription, errorDescription,
                parameters, cause));
        return this;
    }

    public ErrorInfo buildErrorInfo(String contextId, String errorCode, String userErrorDescription,
                                    String errorDescription, Map<String,Object> parameters,
                                    Throwable cause){

        ErrorInfo errorInfo = new ErrorInfo(contextId, errorCode, userErrorDescription, errorDescription,
                parameters, cause);
        return errorInfo;
    }

    public List<ErrorInfo> getErrorInfoList() {
        return errorInfoList;
    }

    public String getCode() {

        StringBuilder builder = new StringBuilder();
        for(int i = this.errorInfoList.size() - 1; i >= 0; i--) {
            ErrorInfo info = this.errorInfoList.get(i);
            builder.append('[');
            builder.append(info.contextId);
            builder.append(':');
            builder.append(info.errorCode);
            builder.append(']');
        }
        return builder.toString();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(getCode());
        builder.append('\n');

        //append additional context information.
        for(int i = this.errorInfoList.size() - 1; i >= 0; i--) {
            ErrorInfo info = this.errorInfoList.get(i);
            builder.append('[');
            builder.append(info.contextId);
            builder.append(':');
            builder.append(info.errorCode);
            builder.append(']');
            builder.append(info.errorDescription);
            if(i > 0) {
                builder.append('\n');
            }
        }

        //append root causes and text from this exception first.
        if(getMessage() != null) {
            builder.append('\n');
            if(getCause() == null) {
                builder.append(getMessage());
            } else if(!getMessage().equals(getCause().toString())) {
                builder.append(getMessage());
            }
        }
        appendException(builder, getCause());
        return builder.toString();
    }

    private void appendException(StringBuilder builder, Throwable throwable) {
        if(throwable == null) {
            return;
        }
        appendException(builder, throwable.getCause());
        builder.append(throwable.toString());
        builder.append('\n');
    }
}
