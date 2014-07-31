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

import javax.ws.rs.core.Response;

public class ResponseUtils {

    public static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(message)
                .build();
    }

    public static Response methodNotAllowed(String allowedTypes) {
        return Response.status(ScepConstants.METHOD_NOT_ALLOWED)
                .header("Allow", allowedTypes)
                .build();
    }

    public static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response serverError() {
        return Response.serverError().build();
    }
}
