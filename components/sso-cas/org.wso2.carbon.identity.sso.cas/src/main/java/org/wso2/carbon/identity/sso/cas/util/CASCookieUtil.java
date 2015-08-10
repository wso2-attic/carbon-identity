/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package org.wso2.carbon.identity.sso.cas.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.sso.cas.config.CASConfiguration;

public class CASCookieUtil {
	private static final String CAS_COOKIE_NAME = "CASTGC";
	
	public static String getSessionDataKey(HttpServletRequest req) {
		Cookie authCookie = FrameworkUtils.getAuthCookie(req);
		String storedSessionDataKey = null;
		
		if( authCookie != null ) {
			storedSessionDataKey = authCookie.getValue();
		}
		
		return storedSessionDataKey;
	}
	
	public static String getTicketGrantingTicketId(HttpServletRequest req) {
		String ticketGrantingTicketId = null;
		Cookie ticketGrantingCookie = CASCookieUtil
				.getTicketGrantingCookie(req);

		if (ticketGrantingCookie != null) {
			ticketGrantingTicketId = ticketGrantingCookie.getValue();
		}
		
		return ticketGrantingTicketId;
	}
	
   public static Cookie getTicketGrantingCookie(HttpServletRequest req) {
       Cookie[] cookies = req.getCookies();
       if (cookies != null) {
           for (Cookie cookie : cookies) {
               if (cookie.getName().equals(CASCookieUtil.CAS_COOKIE_NAME)) {
                   return cookie;
               }
           }
       }
       return null;
   }
   
   public static void storeTicketGrantingCookie(String sessionId, HttpServletRequest req, HttpServletResponse resp,
                                     int sessionTimeout) {
      Cookie ticketGrantingCookie = getTicketGrantingCookie(req);
      if (ticketGrantingCookie == null) {
          ticketGrantingCookie = new Cookie(CASCookieUtil.CAS_COOKIE_NAME, sessionId);
      }
      
      ticketGrantingCookie.setPath( CASConfiguration.getBasePath() );
      ticketGrantingCookie.setSecure(true);
      resp.addCookie(ticketGrantingCookie);
  } 
   
   public static void removeTicketGrantingCookie(HttpServletRequest req, HttpServletResponse resp) {
		
 		Cookie[] cookies = req.getCookies();
       if (cookies != null) {
 			for (Cookie cookie : cookies) {
 				if (cookie.getName().equals(CASCookieUtil.CAS_COOKIE_NAME)) {
 					cookie.setMaxAge(0);
 					cookie.setValue("");
 					cookie.setPath( CASConfiguration.getBasePath() );
 					cookie.setSecure(true);
 					resp.addCookie(cookie);
 					break;
 				}
 			}
       }
 	}
   
   public static Cookie getTempCookie(HttpServletRequest req, String name) {
       Cookie[] cookies = req.getCookies();
       if (cookies != null) {
           for (Cookie cookie : cookies) {
               if (cookie.getName().equals(name)) {
                   return cookie;
               }
           }
       }
       return null;
   }
   
   public static void storeTempCookie(String name, String sessionId, HttpServletRequest req, HttpServletResponse resp) {
	Cookie ticketGrantingCookie = getTempCookie(req, name);
		if (ticketGrantingCookie == null) {
		ticketGrantingCookie = new Cookie(name, sessionId);
	}
	
	resp.addCookie(ticketGrantingCookie);
   } 
}
