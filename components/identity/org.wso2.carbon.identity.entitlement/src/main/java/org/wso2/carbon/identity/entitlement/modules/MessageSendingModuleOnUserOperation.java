/*
*
*   Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*   WSO2 Inc. licenses this file to you under the Apache License,
*   Version 2.0 (the "License"); you may not use this file except
*   in compliance with the License.
*   You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.wso2.carbon.identity.entitlement.modules;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.json.JSONObject;
import org.wso2.carbon.identity.entitlement.NotificationConstants;
import org.wso2.carbon.identity.entitlement.model.PEPEndpointInfo;


/**
 * REST message sending module using Http client library. XML and JSON play load can be used to send
 */
public class MessageSendingModuleOnUserOperation implements Runnable {

	private ThreadSafeClientConnManager connectionManager = null;

	private PEPEndpointInfo endpointInfo;

	private String subjectId;

	private String notificationType;

	private String eventType;

	//used for setting Basic authentication header
	private final String AUTHORIZATION_HEADER = "Authorization";

	private static Log log = LogFactory.getLog(MessageSendingModuleOnUserOperation.class);

	public MessageSendingModuleOnUserOperation(PEPEndpointInfo endpointInfo,
                                               String notificationType, String eventType) {

		this.endpointInfo = endpointInfo;
		this.notificationType = notificationType;
		this.eventType = eventType;
		connectionManager = new ThreadSafeClientConnManager();
	}

	@Override
	public void run() {

		if (endpointInfo != null) {

			HttpPost post = new HttpPost(endpointInfo.getEndpoint());
			HttpResponse response = null;
			try {

				HttpClient client = new DefaultHttpClient(connectionManager);

				StringEntity entity;
				if ("JSON".equalsIgnoreCase(notificationType)) {
					post.setHeader("Content-Type", "application/json");
                    post.setHeader(AUTHORIZATION_HEADER, getBase64EncodedBasicAuthHeader
                            (endpointInfo.getUsername(), endpointInfo.getPassword()));
					entity = new StringEntity(getJSONData());
				} else {
					post.setHeader("Content-Type", "application/xml");
					post.setHeader(AUTHORIZATION_HEADER, getBase64EncodedBasicAuthHeader
					  (endpointInfo.getUsername(), endpointInfo.getPassword()));
					entity = new StringEntity(getXMLData());
				}

				post.setEntity(entity);
				response = client.execute(post);
				log.info("Notification message has been posted to " + endpointInfo.getEndpoint());
			} catch (Exception e) {
				log.error("Message sending is failed", e);
			} finally {
				if (response != null) {
					log.debug(response.getStatusLine());
				}
				post.abort();
			}
		}
	}

	/**
	 * get the xml message which want to be sent to the external endpoint
	 * @return  the xml message to be sent to the external endpoint
	 */
	private String getXMLData() {

		String data = null;

		if (NotificationConstants.EVENT_TYPE_USER_REMOVE.equals(eventType)) {
			data = "<UserRemovalNotification><SubjectId>" + subjectId + "</SubjectId>";
			data = data + "</UserRemovalNotification> ";

		} else if (NotificationConstants.EVENT_TYPE_PROFILE_UPDATE.equals(eventType)) {
			data = "<ProfileUpdateNotification><SubjectId>" + subjectId + "</SubjectId>";
			data = data + "</ProfileUpdateNotification> ";
		}

		return data;
	}


	private String getJSONData() {

		try {
			JSONObject root = new JSONObject();
			JSONObject object = new JSONObject();

			if (NotificationConstants.EVENT_TYPE_USER_REMOVE.equals(eventType)) {
				object.put("SubjectId", subjectId);
				root.put("UserRemovalNotification", object);

			} else if (NotificationConstants.EVENT_TYPE_PROFILE_UPDATE.equals(eventType)) {

				object.put("SubjectId", subjectId);
				root.put("ProfileUpdateNotification", object);
			}

			return root.toString();

		} catch (Exception e) {
			log.error("Error while building JSON Message", e);
		}

		return "";
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}

	private String getBase64EncodedBasicAuthHeader(String userName, String password) {
		String concatenatedCredential = userName + ":" + password;
		byte[] byteValue = concatenatedCredential.getBytes();
		String encodedAuthHeader = Base64.encode(byteValue);
		encodedAuthHeader = "Basic " + encodedAuthHeader;
		return encodedAuthHeader;
	}

}

