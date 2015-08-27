/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.webfinger.builders;

import org.wso2.carbon.identity.webfinger.URLNormalizer;
import org.wso2.carbon.identity.webfinger.WebFingerConstants;
import org.wso2.carbon.identity.webfinger.WebFingerEndpointException;
import org.wso2.carbon.identity.webfinger.WebFingerRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

public class DefaultWebFingerRequestBuilder implements WebFingerRequestBuilder {
    @Override
    public WebFingerRequest buildRequest(HttpServletRequest request) throws WebFingerEndpointException {
        WebFingerRequest webFingerRequest = new WebFingerRequest();
        List<String> parameters = Collections.list(request.getParameterNames());
        if (parameters.size() != 2 || !parameters.contains(WebFingerConstants.REL) || !parameters.contains
                (WebFingerConstants.RESOURCE)) {
            throw new WebFingerEndpointException(WebFingerConstants.ERROR_CODE_INVALID_REQUEST, "Bad Web " +
                    "Finger request.");
        }
        webFingerRequest.setServletRequest(request);
        String resource = request.getParameter(WebFingerConstants.RESOURCE);
        webFingerRequest.setRel(request.getParameter(WebFingerConstants.REL));
        webFingerRequest.setResource(resource);
        URLNormalizer.normalizeResource(webFingerRequest);
        return webFingerRequest;
    }



}
