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
package org.wso2.carbon.identity.application.authentication.framework.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.json.XML;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCache;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationResultCacheKey;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationResult;
import org.wso2.carbon.identity.application.common.cache.CacheEntry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Map;


public abstract class InboundAuthenticatorResponseBuilder {

	private static Log log = LogFactory.getLog(InboundAuthenticatorResponseBuilder.class);
	private boolean directResponseRequired = false;
	private String contentType;
	private String XML_ELEMENT_TEMPLATE = "<#elementName>#elementValue</#elementName>";


	public String parse(Map<String, String> attributeMap, String parentElement) throws FrameworkException {

		try {

			// build XML first;
			StringBuffer xmlElements = new StringBuffer();
			Iterator<Map.Entry<String, String>> iterator = attributeMap.entrySet().iterator();
			String xml;

			xmlElements.append("<" + parentElement + ">");

			while (iterator.hasNext()) {
				Map.Entry<String, String> pair = iterator.next();
				String xmlFragment = XML_ELEMENT_TEMPLATE.replaceAll("#elementName", pair.getKey());
				xmlElements.append(xmlFragment.replaceAll("#elementValue", pair.getValue()));
			}

			xmlElements.append("</" + parentElement + ">");

			xml = xmlElements.toString();

			if ("application/xml".equals(contentType) || "text/xml".equals(contentType)) {
				return xml;
			} else if (contentType.equals("application/json")) {
				JSONObject json = XML.toJSONObject(xml);
				return json.toString();
			}

			throw new FrameworkException("Unsupported content type expected");

		} catch (Exception e) {
			throw new FrameworkException("Error while generating response.", e);
		}

	}

	/**
	 * 
	 * @param sessionIdentifier
	 */
	public abstract void buildResponse(HttpServletRequest req, HttpServletResponse resp, String sessionIdentifier);


    public abstract boolean canHandle(HttpServletRequest req, HttpServletResponse resp);

	/**
	 * 
	 * @return
	 */
	public boolean isDirectResponseRequired() {
		return directResponseRequired;
	}

	/**
	 * 
	 * @param directResponseRequired
	 */
	public void setDirectResponseRequired(boolean directResponseRequired) {
		this.directResponseRequired = directResponseRequired;
	}

	/**
	 * 
	 * @param sessionDataKey
	 * @return
	 */
	protected AuthenticationResult getAuthenticationResultFromCache(String sessionDataKey) {

		AuthenticationResultCacheKey authResultCacheKey = new AuthenticationResultCacheKey(sessionDataKey);
		CacheEntry cacheEntry = AuthenticationResultCache.getInstance().getValueFromCache(authResultCacheKey);
		AuthenticationResult authResult = null;

		if (cacheEntry != null) {
			AuthenticationResultCacheEntry authResultCacheEntry = (AuthenticationResultCacheEntry) cacheEntry;
			authResult = authResultCacheEntry.getResult();
		} else {
			log.error("Cannot find AuthenticationResult from the cache");
		}

		return authResult;
	}

	/**
	 * 
	 * @return
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * 
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

}
