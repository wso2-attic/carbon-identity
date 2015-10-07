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

package org.wso2.carbon.identity.application.authenticator.openid.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.Parameter;
import org.openid4java.message.ax.FetchResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is introduced to overcome a bug in Yahoo Fetch Response
 */
public class YahooFetchResponse extends FetchResponse {

    private static Log log = LogFactory.getLog(YahooFetchResponse.class);

    protected FetchResponse fetchResponse = null;

    YahooFetchResponse(FetchResponse fetchResponse) {
        this.fetchResponse = fetchResponse;
    }

    public List<String> getAttributeValues(String alias){

        List<String> values = new ArrayList<String>();
        if("unlimited".equalsIgnoreCase(_parameters.getParameterValue("count." + alias))){
            values.add(getParameterValue("value." + alias));
        } else {
            values = super.getAttributeValues(alias);
        }
        return values;
    }

    public int getCount(String alias){

        if("unlimited".equalsIgnoreCase(_parameters.getParameterValue("count." + alias))){
            return 1;
        } else {
            return super.getCount(alias);
        }
    }

    protected boolean isValid() {

        Iterator it = _parameters.getParameters().iterator();
        while (it.hasNext()) {
            String paramName = ((Parameter) it.next()).getKey();

            if (!paramName.equals("mode") &&
                    !paramName.startsWith("type.") &&
                    !paramName.startsWith("count.") &&
                    !paramName.startsWith("value.") &&
                    !paramName.equals("update_url")) {
                log.warn("Invalid parameter name in AX payload: " + paramName);
                //return false;
            }
        }
        return checkAttributes();
    }

    private boolean checkAttributes() {
        List aliases = getAttributeAliases();

        Iterator it = aliases.iterator();
        while (it.hasNext()) {
            String alias = (String) it.next();

            if (!_parameters.hasParameter("type." + alias)) {
                log.warn("Type missing for attribute alias: " + alias);
                return false;
            }

            if (!_parameters.hasParameter("count." + alias)) {
                if (_parameters.hasParameterPrefix("value." + alias + ".")) {
                    log.warn("Count parameter not present for alias: " + alias
                            + "; value." + alias + ".[index] format is not allowed.");
                    return false;
                }
            }
            if (_parameters.hasParameter("value." + alias)) {
                if (_parameters.hasParameter("value." + alias)) {
                    if (!"unlimited".equalsIgnoreCase(_parameters.getParameter("count.email").getValue())) {
                        log.warn("Count parameter present for alias: " + alias
                                + "; should use value." + alias + ".[index] format.");
                        return false;
                    }

                }

                int count = getCount(alias);

                if (count < 0) {
                    log.warn("Invalid value for count." + alias + ": " + count);
                    return false;
                }

                for (int i = 1; i <= count; i++) {
                    if (!"unlimited".equalsIgnoreCase(_parameters.getParameter(
                            "count.email").getValue())
                            && !_parameters.hasParameter("value." + alias + "."
                            + Integer.toString(i))) {
                        log.warn("Value missing for alias: "
                                + alias + "." + Integer.toString(i));
                        return false;
                    }
                }
            }
        }

        return true;
    }

}
