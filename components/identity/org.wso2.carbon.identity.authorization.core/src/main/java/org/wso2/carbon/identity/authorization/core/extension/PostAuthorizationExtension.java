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

import java.util.Properties;


/**
 * This extension provides to do post actions after persisting the authorization data with the server.
 * Such as extended logging, publisher to publish required data to another server.
 */
public interface PostAuthorizationExtension {

    /**
     * identify adding of authorization
     */
    public static final String ADD = "add";

    /**
     * identify removing of authorization
     */
    public static final String REMOVE = "remove";

    /**
     * identify retrieve of authorization 
     */
    public static final String GET = "get";

	/**
	 * Initializes the Post Authorization Extension
	 *
	 * @param properties properties, that need to initialize the module.
     * @throws Exception throws when initialization is failed
	 */
	public void init(Properties properties) throws Exception;

    /**
     * Does the post actions based on authorization data
     *
     * @param subject subject value
     * @param resource resource value
     * @param action action value
     * @param authorized whether authorized or not
     * @param authorizationAction whether this is authorization adding,removing or retrieve
     */
    public void doPostAuthorization(String subject, String resource,  String action,
                                    boolean authorized, String authorizationAction);

}
