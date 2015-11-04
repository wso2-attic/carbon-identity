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

import org.wso2.carbon.identity.application.authentication.framework.cache.AuthenticationRequestCacheEntry;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticationRequest;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public abstract class InboundAuthenticationRequestProcessor {

    /**
	 *
     * @param req  HTTP Request
     * @param resp HTTP Response
     * @throws org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException
	 */
    public abstract InboundAuthenticationResponse process(HttpServletRequest req, HttpServletResponse resp)
            throws FrameworkException;

    /**
     *
     * @return
     */
    public abstract String getName();

    /**
	 *
	 * @return
	 */
	public abstract String getSelfPath();

	/**
	 *
	 * @return
	 */
    public abstract String getRelyingPartyId();

    /**
     *
     * @return
     */
    public abstract int getPriority();

    /**
     *
     * @return
     */
    public abstract boolean canHandle(HttpServletRequest req, HttpServletResponse resp) throws FrameworkException;

	/**
	 *
	 * @return
	 */
    public abstract boolean isDirectResponseRequired();

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
            Map<String, String[]> newParams) throws AuthenticationFrameworkRuntimeException {

        try {
            String sessionDataKey = UUIDGenerator.generateUUID();

            AuthenticationRequest authenticationRequest = new AuthenticationRequest();

            Map<String, String[]> OldParams = req.getParameterMap();
            Iterator<Map.Entry<String, String[]>> iterator = OldParams.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, String[]> pair = iterator.next();
                newParams.put(pair.getKey(), pair.getValue());
            }

            newParams.put("sessionDataKey", new String[] { sessionDataKey });
            newParams.put("type", new String[] { getName() });

            authenticationRequest.appendRequestQueryParams(newParams);

            for (@SuppressWarnings("rawtypes")
                 Enumeration e = req.getHeaderNames(); e.hasMoreElements(); ) {
                String headerName = e.nextElement().toString();
                authenticationRequest.addHeader(headerName, req.getHeader(headerName));
            }

            authenticationRequest.setRelyingParty(getRelyingPartyId());
            authenticationRequest.setType(getName());
            authenticationRequest.setCommonAuthCallerPath(URLEncoder.encode(getSelfPath(), "UTF-8"));

            AuthenticationRequestCacheEntry authRequest = new AuthenticationRequestCacheEntry(authenticationRequest);
            FrameworkUtils.addAuthenticationRequestToCache(sessionDataKey, authRequest);
            String queryParams = "?sessionDataKey=" + sessionDataKey + "&" + "type" + "=" + URLEncoder.encode(getName(),
                    "UTF-8");

            String commonAuthURL = IdentityUtil.getServerURL(FrameworkConstants.COMMONAUTH, true);

            if (isDirectResponseRequired()) {
                FrameworkUtils.getRequestCoordinator().handle(req, resp);
            } else {
                resp.sendRedirect(commonAuthURL + queryParams);
            }
        } catch (IOException ex) {
            throw new AuthenticationFrameworkRuntimeException("Exception occurred while sending request to framework",
                    ex);
        }
    }

}
