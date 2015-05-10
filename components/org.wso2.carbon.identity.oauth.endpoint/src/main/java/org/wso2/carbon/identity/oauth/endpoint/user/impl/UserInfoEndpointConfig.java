/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.oauth.endpoint.user.impl;

import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoAccessTokenValidator;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoClaimRetriever;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoRequestValidator;
import org.wso2.carbon.identity.oauth.endpoint.user.UserInfoResponseBuilder;
import org.wso2.carbon.identity.oauth.endpoint.util.EndpointUtil;

/**
 * A singleton object halding configurations
 *
 */
public class UserInfoEndpointConfig {
	
	private static Log log = LogFactory.getLog(UserInfoEndpointConfig.class);
	private static UserInfoEndpointConfig config = new UserInfoEndpointConfig();
	private static UserInfoRequestValidator requestValidator = null;
	private static UserInfoAccessTokenValidator accessTokenValidator = null;
	private static UserInfoResponseBuilder responseBuilder = null;
	private static UserInfoClaimRetriever claimRetriever = null;
	
	private UserInfoEndpointConfig() {
		log.debug("Initializing the UserInfoEndpointConfig singlton");
        initUserInfoEndpointConfig();
	}
	
	private void initUserInfoEndpointConfig() {
	}
	
	public static UserInfoEndpointConfig getInstance() {
		return config;
	}
	
	public UserInfoRequestValidator getUserInfoRequestValidator() throws OAuthSystemException {
		if (requestValidator == null) {
			synchronized (UserInfoRequestValidator.class) {
				if (requestValidator == null) {
					try {
						String requestValidatorClassName = EndpointUtil.getUserInfoRequestValidator();
						Class requestValidatorClass =
						                              this.getClass().getClassLoader()
						                                  .loadClass(requestValidatorClassName);
						requestValidator = (UserInfoRequestValidator) requestValidatorClass.newInstance();
					} catch (ClassNotFoundException e) {
						log.error("Error while loading configuration", e);
					} catch (InstantiationException e) {
						log.error("Error while loading configuration", e);
					} catch (IllegalAccessException e) {
						log.error("Error while loading configuration", e);
					}
				}
			}
		}
		return requestValidator;
	}
	
	public UserInfoAccessTokenValidator getUserInfoAccessTokenValidator() {
		if (accessTokenValidator == null) {
			synchronized (UserInfoAccessTokenValidator.class) {
				if (accessTokenValidator == null) {
					try {
						String accessTokenValidatorClassName = EndpointUtil.getAccessTokenValidator();
						Class accessTokenValidatorClass =
						                                  this.getClass().getClassLoader()
						                                      .loadClass(accessTokenValidatorClassName);
						accessTokenValidator =
						                       (UserInfoAccessTokenValidator) accessTokenValidatorClass.newInstance();
					} catch (ClassNotFoundException e) {
						log.error("Error while loading configuration", e);
					} catch (InstantiationException e) {
						log.error("Error while loading configuration", e);
					} catch (IllegalAccessException e) {
						log.error("Error while loading configuration", e);
					}
				}
			}
		}
		return accessTokenValidator;
	}
	
	public UserInfoResponseBuilder getUserInfoResponseBuilder() {
		if (responseBuilder == null) {
			synchronized (UserInfoResponseBuilder.class) {
				if (responseBuilder == null) {
					try {
						String responseBilderClassName = EndpointUtil.getUserInfoResponseBuilder();
						Class responseBuilderClass =
						                             this.getClass().getClassLoader()
						                                 .loadClass(responseBilderClassName);
						responseBuilder = (UserInfoResponseBuilder) responseBuilderClass.newInstance();
					} catch (ClassNotFoundException e) {
						log.error("Error while loading configuration", e);
					} catch (InstantiationException e) {
						log.error("Error while loading configuration", e);
					} catch (IllegalAccessException e) {
						log.error("Error while loading configuration", e);
					}
				}
			}
		}
		return responseBuilder;
	}
	
	public UserInfoClaimRetriever getUserInfoClaimRetriever() {
		if (claimRetriever == null) {
			synchronized (UserInfoClaimRetriever.class) {
				if (claimRetriever == null) {
					try {
						String claimRetrieverClassName = EndpointUtil.getUserInfoClaimRetriever();
						Class claimRetrieverClass =
						                            this.getClass().getClassLoader()
						                                .loadClass(claimRetrieverClassName);
						claimRetriever = (UserInfoClaimRetriever) claimRetrieverClass.newInstance();
					} catch (ClassNotFoundException e) {
						log.error("Error while loading configuration", e);
					} catch (InstantiationException e) {
						log.error("Error while loading configuration", e);
					} catch (IllegalAccessException e) {
						log.error("Error while loading configuration", e);
					}
				}
			}
		}
		return claimRetriever;
	}

}
