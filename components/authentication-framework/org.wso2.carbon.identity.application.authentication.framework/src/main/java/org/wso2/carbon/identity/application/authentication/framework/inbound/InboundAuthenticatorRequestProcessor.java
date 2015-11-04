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

import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public abstract class InboundAuthenticatorRequestProcessor {

	private static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	private boolean directResponseRequired = false;
	private String responseContentType;

	/**
	 * 
	 * @param req
	 * @param resp
	 * @throws org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException
	 */
	public abstract void process(HttpServletRequest req, HttpServletResponse resp) throws FrameworkException;

	/**
	 *
	 * @return
	 */
	public abstract String getType();

	/**
	 *
	 * @return
	 */
	public abstract String getSelfPath();

	/**
	 *
	 * @return
	 */
	public abstract String getIssuer();


    /**
     *
     * @return
     */
    public abstract boolean canHandle(HttpServletRequest req, HttpServletResponse resp) throws FrameworkException;

	/**
	 *
	 * @return
	 */
	public boolean isDirectResponseRequired() {
		return directResponseRequired;
	}

	/**
	 *
	 * @param req
	 * @param attrIdToXpathMap
	 * @return
	 */
	public Map<String, String[]> parse(HttpServletRequest req, Map<String, String> attrIdToXpathMap) {

		Map<String, String[]> mappedValues = new HashMap<String, String[]>();
		String contentType = req.getContentType();

		if (req.getHeader("Accept") != null) {
			responseContentType = req.getHeader("Accept");
		} else {
			responseContentType = contentType;
		}

		try {

			InputStream is = req.getInputStream();

			if (req.getContentType().equals("application/json")) {
				String inputStreamString = new Scanner(is, "UTF-8").useDelimiter("\\A").next();
				JSONObject json = new JSONObject(inputStreamString);
				is = new ByteArrayInputStream(XML.toString(json).getBytes());
				contentType = "application/xml";
			}

			if ("application/xml".equals(contentType) || "text/xml".equals(contentType)) {
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				Document xmlDocument = builder.parse(is);
				XPath xPath = XPathFactory.newInstance().newXPath();

				Iterator<Map.Entry<String, String>> iterator = attrIdToXpathMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, String> pair = iterator.next();
					mappedValues.put(pair.getKey(),
							new String[] { xPath.compile(pair.getValue()).evaluate(xmlDocument) });
				}

				directResponseRequired = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mappedValues;
	}

	/**
	 *
	 * @param req
	 * @param resp
	 * @param newParams
	 * @throws javax.servlet.ServletException
	 * @throws java.io.IOException
	 * @throws org.wso2.carbon.identity.application.common.IdentityApplicationManagementException
	 */
	protected void sendToFrameworkForAuthentication(HttpServletRequest req, HttpServletResponse resp,
			Map<String, String[]> newParams) throws ServletException,
			IOException, IdentityApplicationManagementException {

		String sessionDataKey = UUIDGenerator.generateUUID();


		AuthenticationRequest authenticationRequest = new AuthenticationRequest();

		Map<String, String[]> OldParams = req.getParameterMap();
		Iterator<Map.Entry<String, String[]>> iterator = OldParams.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, String[]> pair = iterator.next();
			newParams.put(pair.getKey(), pair.getValue());
		}

		newParams.put("sessionDataKey", new String[] { sessionDataKey });
		newParams.put("type", new String[] { getType() });

		authenticationRequest.appendRequestQueryParams(newParams);

		for (@SuppressWarnings("rawtypes")
		Enumeration e = req.getHeaderNames(); e.hasMoreElements();) {
			String headerName = e.nextElement().toString();
			authenticationRequest.addHeader(headerName, req.getHeader(headerName));
		}

		authenticationRequest.setRelyingParty(getIssuer());
		authenticationRequest.setType(getType());
		authenticationRequest.setCommonAuthCallerPath(URLEncoder.encode(getSelfPath(), "UTF-8"));

		AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry(authenticationRequest);
		FrameworkUtils.addAuthenticationRequestToCache(sessionDataKey, authRequest);
        String queryParams = "?sessionDataKey=" + sessionDataKey + "&" + "type" + "=" + getType();

		FrameworkUtils.setRequestPathCredentials(req);

		String commonAuthURL = CarbonUIUtil.getAdminConsoleURL(req);
		commonAuthURL = commonAuthURL.replace(getSelfPath() + "/carbon/", "/commonauth");

		if (isDirectResponseRequired()) {
			FrameworkUtils.getRequestCoordinator().handle(req, resp);
		} else {
			resp.sendRedirect(commonAuthURL + queryParams);
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getResponseContentType() {
		return responseContentType;
	}

	/**
	 * 
	 * @param responseContentType
	 */
	public void setResponseContentType(String responseContentType) {
		this.responseContentType = responseContentType;
	}
}
