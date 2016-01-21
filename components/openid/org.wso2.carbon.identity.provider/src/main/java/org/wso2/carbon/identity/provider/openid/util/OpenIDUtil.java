/*
 * Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.provider.openid.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openid4java.message.Parameter;
import org.openid4java.message.ParameterList;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.provider.IdentityProviderException;
import org.wso2.carbon.identity.provider.dto.OpenIDParameterDTO;
import org.wso2.carbon.identity.provider.openid.OpenIDConstants;
import org.wso2.carbon.identity.provider.openid.client.OpenIDAdminClient;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains OpenID related utility functions
 */
public class OpenIDUtil {

    private static final Set<Character> UNRESERVED_CHARACTERS = new HashSet<Character>();
    private static final Log log = LogFactory.getLog(OpenIDUtil.class);

    private OpenIDUtil() {
    }

    public static String getUserName(String rquestUrl) throws IdentityException {
        String caller = null;
        String path = null;
        URI uri = null;
        String contextPath = "/openid/";

        try {
            uri = new URI(rquestUrl);
            path = uri.getPath();
        } catch (URISyntaxException e) {
            throw IdentityException.error("Invalid OpenID", e);
        }

        caller = path.substring(path.indexOf(contextPath) + contextPath.length(), path.length());
        return caller;
    }

    /**
     * Generate OpenID for a given user.
     *
     * @param user User
     * @return Generated OpenID
     * @throws IdentityProviderException
     */
    public static String generateOpenID(String user, String openIDUserUrl) throws IdentityException {
        String openID = null;
        URI uri = null;
        URL url = null;

        String normalizedUser = normalizeUrlEncoding(user);
        openID = String.format(openIDUserUrl, normalizedUser);

        try {
            uri = new URI(openID);
        } catch (URISyntaxException e) {
            log.error("Invalid OpenID URL :" + openID, e);
            throw IdentityException.error("Invalid OpenID URL");
        }

        try {
            url = uri.normalize().toURL();
            if (url.getQuery() != null || url.getRef() != null) {
                log.error("Invalid user name for OpenID :" + openID);
                throw IdentityException.error("Invalid user name for OpenID");
            }
        } catch (MalformedURLException e) {
            log.error("Malformed OpenID URL :" + openID, e);
            throw IdentityException.error("Malformed OpenID URL");
        }

        openID = url.toString();

        if (log.isDebugEnabled()) {
            log.debug("OpenID generated successfully : " + openID);
        }

        return openID;
    }

    /**
     * @param text
     * @return
     */
    private static String normalizeUrlEncoding(String text) {

        if (text == null) {
            return null;
        }

        int len = text.length();
        StringBuilder normalized = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char current = text.charAt(i);
            if (current == '%' && i < len - 2) {
                String percentCode = text.substring(i, i + 3).toUpperCase();
                try {
                    String str = URLDecoder.decode(percentCode, "ISO-8859-1");
                    char chr = str.charAt(0);
                    if (UNRESERVED_CHARACTERS.contains(Character.valueOf(chr))) {
                        normalized.append(chr);
                    } else {
                        normalized.append(percentCode);
                    }
                } catch (UnsupportedEncodingException e) {
                    normalized.append(percentCode);
                }
                i += 2;
            } else {
                normalized.append(current);
            }
        }
        return normalized.toString();
    }

    /**
     * Normalize the provided relying party URL
     *
     * @param rpUrl Relying party URL to be normalized
     * @return Normalized relying party URL
     * @throws IdentityException
     */
    public static String getRelyingPartyUrl(String rpUrl) throws IdentityException {
        URI uri = null;
        URL url = null;

        try {
            uri = new URI(rpUrl);
        } catch (URISyntaxException e) {
            log.error("Invalid relying party URL :" + rpUrl, e);
            throw IdentityException.error("Invalid relying party URL");
        }

        try {
            url = uri.normalize().toURL();
            url = new URL(url.getProtocol().toLowerCase(), url.getHost().toLowerCase(), url.getPort(), url.getPath());
            return url.toString();
        } catch (MalformedURLException e) {
            log.error("Malformed relying party URL :" + rpUrl, e);
            throw IdentityException.error("Malformed relying party URL");
        }
    }

    public static OpenIDParameterDTO[] getOpenIDAuthRequest(ParameterList request) {
        OpenIDParameterDTO[] params = null;
        List list = null;

        list = request.getParameters();
        params = new OpenIDParameterDTO[list.size()];
        int i = 0;
        for (Object object : list) {
            Parameter param = (Parameter) object;
            OpenIDParameterDTO openIDParameterDTO = new OpenIDParameterDTO();
            openIDParameterDTO.setName(param.getKey());
            openIDParameterDTO.setValue(param.getValue());
            params[i++] = openIDParameterDTO;
        }
        return params;
    }

    public static OpenIDParameterDTO[] getOpenIDAuthRequest(HttpServletRequest request) {
        List<OpenIDParameterDTO> params = null;
        params = getOpenIDAuthRequestAsList(request);
        return params.toArray(new OpenIDParameterDTO[params.size()]);
    }

    public static List<OpenIDParameterDTO> getOpenIDAuthRequestAsList(HttpServletRequest request) {
        Map parameterMap = null;
        Iterator keysIter = null;
        List<OpenIDParameterDTO> params = null;
        OpenIDParameterDTO param = null;

        parameterMap = request.getParameterMap();
        keysIter = parameterMap.keySet().iterator();
        params = new ArrayList<OpenIDParameterDTO>();
        while (keysIter.hasNext()) {
            String name = (String) keysIter.next();
            Object v = parameterMap.get(name);

            String value;
            if (v instanceof String[]) {
                String[] values = (String[]) v;
                if (values.length > 1 && name.startsWith("openid.")) {
                    throw new IllegalArgumentException("Multiple parameters with the same name: " + values);
                }

                value = values.length > 0 ? values[0] : null;
            } else if (v instanceof String) {
                value = (String) v;
            } else {
                value = "";
            }

            param = new OpenIDParameterDTO();
            param.setName(name);
            param.setValue(value);
            params.add(param);
        }

        return params;
    }

    /**
     * Find the OpenID corresponding to the given user name.
     *
     * @param userName User name
     * @return OpenID corresponding the given user name.
     * @throws IdentityException
     */
    public static String getOpenID(String userName) throws IdentityException {
        return generateOpenID(userName, "");
    }

    public static String getFronEndUrl(String openId, HttpServletRequest request, String relativeUrl) {
        String tenant = MultitenantUtils.getDomainNameFromOpenId(openId);
        if (getHostName().equals(tenant)) {
            tenant = null;
        }

        String frontEndUrl = IdentityUtil.getServerURL("/carbon/", false, true) + relativeUrl;

        if (tenant != null && tenant.trim().length() > 0) {
            return frontEndUrl.replace("/carbon/", "/t/" + tenant + "/carbon/");
        }

        return frontEndUrl;
    }

    /**
     * Returns an instance of <code>OpenIDAdminClient</code>.
     * Only one instance of this will be created for a session.
     * This method is used to reuse the same client within a session.
     *
     * @param session
     * @return {@link OpenIDAdminClient}
     * @throws AxisFault
     */
    public static OpenIDAdminClient getOpenIDAdminClient(HttpSession session) throws AxisFault {
        OpenIDAdminClient client =
                (OpenIDAdminClient) session.getAttribute(OpenIDConstants.SessionAttribute.OPENID_ADMIN_CLIENT);
        if (client == null) { // a session timeout or the fist request
            String serverURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
            ConfigurationContext configContext = (ConfigurationContext) session.getServletContext().getAttribute(
                    CarbonConstants.CONFIGURATION_CONTEXT);
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
            client = new OpenIDAdminClient(configContext, serverURL, cookie);
            session.setAttribute(OpenIDConstants.SessionAttribute.OPENID_ADMIN_CLIENT, client);
        }
        return client;
    }

    private static String getHostName() {
        ServerConfiguration serverConfig = ServerConfiguration.getInstance();
        if (serverConfig.getFirstProperty("HostName") != null) {
            return MultitenantUtils.getDomainNameFromOpenId(serverConfig.getFirstProperty("HostName"));
        } else {
            return "localhost";
        }
    }

    public static String getLoginPageQueryParams(ParameterList params) throws IdentityException {
        String queryParams = null;
        try {
            String realm = params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_REALM) != null ?
                           URLEncoder.encode(params.getParameterValue(
                                   OpenIDConstants.OpenIDRequestParameters.OPENID_REALM),
                                             StandardCharsets.UTF_8.name()) : "";
            String returnTo =
                    params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_RETURN_TO) != null ?
                    URLEncoder.encode(
                            params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_RETURN_TO),
                            StandardCharsets.UTF_8.name()) : "";
            String claimedId =
                    params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_CLAIMED_ID) != null ?
                    URLEncoder.encode(
                            params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_CLAIMED_ID),
                            StandardCharsets.UTF_8.name()) : "";
            String identity =
                    params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_IDENTITY) != null ?
                    URLEncoder.encode(params.getParameterValue(OpenIDConstants.OpenIDRequestParameters.OPENID_IDENTITY),
                                      StandardCharsets.UTF_8.name()) : "";

            queryParams = "?" + OpenIDConstants.OpenIDRequestParameters.OPENID_REALM + "=" + realm
                          + "&" + OpenIDConstants.OpenIDRequestParameters.OPENID_RETURN_TO + "=" + returnTo
                          + "&" + OpenIDConstants.OpenIDRequestParameters.OPENID_CLAIMED_ID + "=" + claimedId
                          + "&" + OpenIDConstants.OpenIDRequestParameters.OPENID_IDENTITY + "=" + identity;
        } catch (UnsupportedEncodingException e) {
            log.error("UTF-8 encoding is not supported", e);
            throw IdentityException.error("UTF-8 encoding is not supported");
        }

        String username = null;
        if (params.getParameterValue(OpenIDConstants.OpenIDRequestParameters
                                             .OPENID_IDENTITY) != null) {
            username = OpenIDUtil.getUserName(params.getParameterValue(OpenIDConstants.OpenIDRequestParameters
                                                                               .OPENID_IDENTITY));
            queryParams = queryParams + "&" + OpenIDConstants.RequestParameter.USERNAME + "=" + username;
        }

        return queryParams;
    }

    public static Cookie getCookie(String name, HttpServletRequest req) {
        Cookie[] reqCookies = req.getCookies();
        if (reqCookies != null) {
            for (Cookie cookie : reqCookies) {
                if (cookie.getName().equalsIgnoreCase(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public static void setCookie(String name, String value, int expires, String path, boolean secure,
                                 HttpServletResponse resp) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(expires);
        cookie.setPath(path);
        cookie.setSecure(secure);
        resp.addCookie(cookie);
    }

    public static void deleteCookie(String name, String path, HttpServletRequest request) {
        Cookie[] reqCookies = request.getCookies();
        if (reqCookies != null) {
            for (Cookie cookie : reqCookies) {
                if (cookie.getName().equals(name) && cookie.getPath() != null
                    && cookie.getPath().equals(path)) {
                    cookie.setMaxAge(0);
                    break;
                }
            }
        }
    }
}