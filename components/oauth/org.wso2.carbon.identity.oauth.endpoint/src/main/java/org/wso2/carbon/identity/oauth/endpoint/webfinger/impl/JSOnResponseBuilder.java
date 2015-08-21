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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.webfinger.impl;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.wso2.carbon.identity.webfinger.*;
import org.wso2.carbon.identity.webfinger.builders.ResponseBuilder;

import java.util.ArrayList;
import java.util.List;

public class JSOnResponseBuilder implements ResponseBuilder {
    private static final Log log = LogFactory.getLog(JSOnResponseBuilder.class);
    @Override
    public String getOIDProviderIssuerString(WebFingerResponse webFingerResponse) throws WebFingerEndPointException {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(WebFingerConstants.SUBJECT, webFingerResponse.getSubject());

            List<JSONObject> jsonList = new ArrayList<JSONObject>();
            for(WebLink webLink : webFingerResponse.getLinks()){
                JSONObject jsonLink = new JSONObject();
                jsonLink.put(WebFingerConstants.REL,webLink.getRel());
                jsonLink.put(WebFingerConstants.HREF,webLink.getHref());
                jsonList.add(jsonLink);
            }
            jsonObject.put(WebFingerConstants.LINKS,jsonList);
            return jsonObject.toString();

        } catch (JSONException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while generating the response JSON", e);
            }
            throw new WebFingerEndPointException(WebFingerConstants.ERROR_CODE_JSON_EXCEPTION, "Error" +
                    " while generating the response JSON");
        }
    }
}
