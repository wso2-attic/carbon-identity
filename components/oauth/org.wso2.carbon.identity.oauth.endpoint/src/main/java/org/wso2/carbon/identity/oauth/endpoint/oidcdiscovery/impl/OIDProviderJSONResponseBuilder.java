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
package org.wso2.carbon.identity.oauth.endpoint.oidcdiscovery.impl;

import org.apache.amber.oauth2.common.utils.JSONUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.wso2.carbon.identity.oauth.oidcdiscovery.OIDCDiscoveryEndPointException;
import org.wso2.carbon.identity.oauth.oidcdiscovery.OIDProviderConfigResponse;
import org.wso2.carbon.identity.oauth.oidcdiscovery.builders.OIDProviderResponseBuilder;

import java.util.Map;


public class OIDProviderJSONResponseBuilder implements OIDProviderResponseBuilder {
    private static final Log log = LogFactory.getLog(OIDProviderResponseBuilder.class);

    public String getOIDProviderConfigString(OIDProviderConfigResponse oidProviderConfigResponse) throws
            OIDCDiscoveryEndPointException {
        Map<String, Object> configs = oidProviderConfigResponse.getConfigMap();
        try {
            //TODO
            //Remove org.apache.amber.oauth2.common.utils.JSONUtils
            return JSONUtils.buildJSON(configs);

        } catch (JSONException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error while generating the response JSON", e);
            }
            throw new OIDCDiscoveryEndPointException(OIDCDiscoveryEndPointException.ERROR_CODE_JSON_EXCEPTION, "Error" +
                    " while generating the response JSON");
        }

    }

}
