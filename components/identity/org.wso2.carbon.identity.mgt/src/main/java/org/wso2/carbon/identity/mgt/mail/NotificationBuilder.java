/*
 *  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.mgt.mail;

import java.util.Map;

public class NotificationBuilder {

	public static Notification createNotification(String notificationType, String template, NotificationData data) throws Exception{

		String subject = null;
		String body = null;
		String footer = null;
		Notification notificatoin = null;
		
		if("EMAIL".equals(notificationType)) {
			String[] contents = template.split("\\|");
			
			if(contents.length > 3) {
				throw new Exception("Contents must be 3 or less");
			}
			
			subject = contents[0];
			body = contents[1];
			footer = contents[2];
			
//			Replace all the tags in the NotificationData.
			Map<String, String> tagsData = data.getTagsData();
			subject = replaceTags(tagsData, subject);
			body = replaceTags(tagsData, body);
			footer = replaceTags(tagsData, footer);
			
			notificatoin = new EmailNotification();
			notificatoin.setSubject(subject);
			notificatoin.setBody(body);
			notificatoin.setFooter(footer);
			notificatoin.setSendFrom(data.getSendFrom());
			notificatoin.setSendTo(data.getSendTo());			
			
		}
		return notificatoin;
	}
	
	private static String replaceTags(Map<String,String> tagsData, String content) {
		
		for (String key : tagsData.keySet()) {
			content = content.replaceAll("\\{"+ key +"\\}", tagsData.get(key));
		}
		return content;
	}
}
