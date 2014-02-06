/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authorization.core.extension;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * Default and Sample implementation of <code>PostAuthorizationExtension</code> interface
 * This module does the logging of authorization action
 */
public class CarbonPostAuthorizationExtension implements PostAuthorizationExtension {

    private static Log log = LogFactory.getLog(CarbonPostAuthorizationExtension.class);

    public void init(Properties properties) throws Exception {
        // nothing to implement
    }

    public void doPostAuthorization(String subject, String resource, String action,
                                    boolean authorized, String authorizationAction) {

        if(PostAuthorizationExtension.ADD.equals(authorizationAction)){
            if(authorized){
                log.info("Permit authorization for subject : " + subject +
                                        " and resource : " + resource + " and action : " + action);
            } else {
                log.info("Deny authorization for subject : " + subject +
                                        " and resource : " + resource + " and action : " + action);
            }
        }
    }
}
