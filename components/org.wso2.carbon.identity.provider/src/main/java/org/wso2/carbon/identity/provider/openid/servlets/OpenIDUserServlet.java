/*
 * Copyright 2005-2008 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.provider.openid.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.provider.dto.OpenIDProviderInfoDTO;
import org.wso2.carbon.identity.provider.openid.client.OpenIDAdminClient;
import org.wso2.carbon.identity.provider.openid.handlers.OpenIDHandler;
import org.wso2.carbon.identity.provider.openid.util.OpenIDUtil;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class OpenIDUserServlet extends HttpServlet {

	private static final long serialVersionUID = -2306630174647697559L;
	private static Log log = LogFactory.getLog(OpenIDUserServlet.class);

	/**
	 * 
	 */
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String serverUrl = null;
		String user = null;
		String back = "";
		OpenIDAdminClient opClient = null;
		OpenIDProviderInfoDTO opInfo = null;
		String backendServerURL = null;
		ConfigurationContext configContext = null;
        String tenantDomain = null;
        String mainCSS = "";
        String placeHolder = "";
        
        if (req.getRequestURL().indexOf("/images")>0 || req.getRequestURL().indexOf("/styles/css")>0){
        	return;
        }
        
        if (req.getSession()
                .getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
            tenantDomain = (String) req.getSession().getAttribute(
                	MultitenantConstants.TENANT_DOMAIN);
        } else {
            // user is not logged in or just logged out, but still they are inside url own to the domain
            tenantDomain = (String) req
                    .getAttribute(MultitenantConstants.TENANT_DOMAIN);
        }
        if (tenantDomain != null) {
        String themeRoot = placeHolder+ "../../../../t/" + tenantDomain
                    + "/registry/resource"
                    + RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
                    + "/repository";
            mainCSS = themeRoot + "/theme/admin/main.css";
            if (req.getSession().getAttribute(
                   CarbonConstants.THEME_URL_RANDOM_SUFFIX_SESSION_KEY) != null) {
               // this random string is used to get the effect of the theme change, where-ever the
               // theme is changed, this session will be changed
               mainCSS += "?rsuffix=" + req.getSession().getAttribute(
                   CarbonConstants.THEME_URL_RANDOM_SUFFIX_SESSION_KEY);
            }
        } else {
            mainCSS = "../carbon/styles/css/main.css";
        }
        String errorBack = "";
        try {
			user = OpenIDUtil.getUserName(req.getRequestURL().toString());
			if (log.isDebugEnabled()) {
				log.debug("OpenID url hit for the user " + user);
			}
			
			if (user.indexOf("/")>0) {
				placeHolder = "../";
			}
			
		   errorBack= "<html><head><link media=\"all\" type=\"text/css\" rel=\"stylesheet\" href=\""+placeHolder+"../carbon/openid-provider/css/openid-provider.css\" /><link media=\"all\" type=\"text/css\" rel=\"stylesheet\" href=\""+placeHolder+"../carbon/admin/css/global.css\" />"
                    +"<link media=\"all\" type=\"text/css\" rel=\"stylesheet\" href=\""+mainCSS+"\">\n"
					+ "</head>" 
					+"<body>"
					+"<table cellspacing=\"0\" border=\"0\" id=\"main-table\">"
                        +"<tbody>"
                        +"<tr>"
                            +"<td colspan=\"2\" id=\"header\">"
                                +"<div id=\"header-div\">"
                                    +"<div class=\"right-logo\">Management Console</div>"
                                    +"<div class=\"left-logo\"><a class=\"header-home\" href=\""+placeHolder+"../carbon/admin/index.jsp\"><img height=\"32\" width=\"300\" src=\""+placeHolder+"../admin/images/1px.gif\"/></a>"
                                    +"</div>"
                                +"</div>"
                            +"</td>"
                        +"</tr>"
                        +"<tr>"
                            +"<td valign=\"top\" id=\"menu-panel\">"
                                +"<table cellspacing=\"0\" border=\"0\" id=\"menu-table\">"
                                    +"<tbody><tr><td id=\"region1\"><div id=\"menu\"><ul class=\"main\"><li class=\"menu-home\">&nbsp;</li></ul></div></td></tr></tbody>"
                                +"</table>"
                            +"</td>"
                            +"<td id=\"middle-content\">"
                            +"<div class=\"openid-box-top\"></div>"
                            +"<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"openid-box\">"
                                        +"<tr><td class=\"openid-box-08\"></td>"
                                        +"<td class=\"openid-box-back\" style=\"color:#c55825;\">"
                                            +"<img src=\""+placeHolder+"../carbon/openid-provider/images/openid-biguser-fishy.gif\" align=\"middle\" hspace=\"50\" />You are trying to do something fishy !!! "
                                        +"</td>"
                                        +"<td class=\"openid-box-04\"></td>"
                            +"</tr>"
                            +"<tr><td class=\"openid-box-07\"></td><td class=\"openid-box-06\"></td><td class=\"openid-box-05\"></td></tr>"
                            +"</table>"
                            +"</td>"
                        +"</tr>"
                        +"<tr>"
                            +"<td colspan=\"2\" id=\"footer\">"
                                +"<div id=\"footer-div\">"
                                    +"<div class=\"footer-content\">"
                                        +"<div class=\"copyright\">\u00a9 2008 - 2009 WSO2 Inc. All Rights Reserved.</div>"
                                    +"</div>"
                                    +"<img height=\"1\" width=\"985\" src=\"images/1px.gif\"/>"
                                +"</div>"
                            +"</td>"
                        +"</tr>"
                        +"</tbody>"
                    +"</table>"
                    +"</body></html>";
		
		} catch (IdentityException e) {
			log.error("Failed to retreive the user name corresponding to the provided OpenID", e);
			resp.setContentType("text/html");
			printOutput(resp,errorBack);
			return;
		}

		backendServerURL = CarbonUIUtil.getServerURL(getServletConfig().getServletContext(), req
				.getSession());
		configContext = (ConfigurationContext) getServletConfig().getServletContext().getAttribute(
				CarbonConstants.CONFIGURATION_CONTEXT);
		opClient = new OpenIDAdminClient(configContext, backendServerURL,null);
		try {
			opInfo = opClient.getOpenIDProviderInfo(user,req.getRequestURL().toString());
		} catch (java.lang.Exception e) {
			log.error("Failed to retreive the user name corresponding to the provided OpenID", e);
			resp.setContentType("text/html");
			printOutput(resp,errorBack);
			return;
		}

		serverUrl = opInfo.getOpenIDProviderServerUrl();

		// Keep OpenIDHandler instantiated.
		OpenIDHandler.getInstance(serverUrl);

	/*	if (!opInfo.getUserExist()) {

			log.info("OpenID url hit for the user who does not exist in the system " + user);

			resp.setContentType("text/html");
			back = errorBack;
		} else {*/
			resp.setContentType("text/html");
			back = "<html><head><link media=\"all\" type=\"text/css\" rel=\"stylesheet\" href=\""+placeHolder+"../carbon/openid-provider/css/openid-provider.css\" /><link media=\"all\" type=\"text/css\" rel=\"stylesheet\" href=\""+placeHolder+"../carbon/admin/css/global.css\" />"
                    +"<link media=\"all\" type=\"text/css\" rel=\"stylesheet\" href=\""+mainCSS+"\">\n"
                    + "<link rel='openid2.provider' href='" + serverUrl + "'/>\n"
					+ "<link rel='openid.server' href='" + serverUrl + "'/>\n" + "</head>" 
					+"<body>"
					+"<table cellspacing=\"0\" border=\"0\" id=\"main-table\">"
                        +"<tbody>"
                        +"<tr>"
                            +"<td colspan=\"2\" id=\"header\">"
                                +"<div id=\"header-div\">"
                                    +"<div class=\"right-logo\">Management Console</div>"
                                    +"<div class=\"left-logo\"><a class=\"header-home\" href=\""+placeHolder+"../carbon/admin/index.jsp\"><img height=\"32\" width=\"300\" src=\""+placeHolder+"../carbon/admin/images/1px.gif\"/></a>"
                                    +"</div>"
                                +"</div>"
                            +"</td>"
                        +"</tr>"
                        +"<tr>"
                            +"<td valign=\"top\" id=\"menu-panel\">"
                                +"<table cellspacing=\"0\" border=\"0\" id=\"menu-table\">"
                                    +"<tbody><tr><td id=\"region1\"><div id=\"menu\"><ul class=\"main\"><li class=\"menu-home\">&nbsp;</li></ul></div></td></tr></tbody>"
                                +"</table>"
                            +"</td>"
                            +"<td id=\"middle-content\">"
                            +"<div class=\"openid-box-top\"></div>"
                            +"<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"openid-box\">"
                                        +"<tr><td class=\"openid-box-08\"></td>"
                                        +"<td class=\"openid-box-back\">"
                                            +"<img src=\""+placeHolder+"../carbon/openid-provider/images/openid-biguser.gif\" align=\"middle\" hspace=\"50\" />This is the OpenID Url of user, <span class=\"openid-box-username\">"+user+"</span>"
                                        +"</td>"
                                        +"<td class=\"openid-box-04\"></td>"
                            +"</tr>"
                            +"<tr><td class=\"openid-box-07\"></td><td class=\"openid-box-06\"></td><td class=\"openid-box-05\"></td></tr>"
                            +"</table>"
                            +"</td>"
                        +"</tr>"
                        +"<tr>"
                            +"<td colspan=\"2\" id=\"footer\">"
                                +"<div id=\"footer-div\">"
                                    +"<div class=\"footer-content\">"
                                        +"<div class=\"copyright\">\u00a9 2008 - 2009 WSO2 Inc. All Rights Reserved.</div>"
                                    +"</div>"
                                    +"<img height=\"1\" width=\"985\" src=\"images/1px.gif\"/>"
                                +"</div>"
                            +"</td>"
                        +"</tr>"
                        +"</tbody>"
                    +"</table>"
                    +"</body></html>";
//		}
		
		printOutput(resp,back);
	}
	
	private void printOutput(HttpServletResponse resp,String output) throws IOException{		
		PrintWriter out = null;
		try {
			out = resp.getWriter();
			out.write(output);
			out.flush();
		} finally {
			if (out != null) {
				out.close();
			}
		}	
	}
}