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
import org.wso2.carbon.identity.entitlement.model.PEPEndpointInfo;

import java.util.HashMap;
import java.util.Map;


/**
 * REST message sending module using Http client library. XML and JSON play load can be used to send
 *
 */
public class MessageSendingModuleOnPolicyUpdate  implements Runnable {

	public final String AUTHORIZATION_HEADER = "Authorization";

	private ThreadSafeClientConnManager connectionManager = null;

	private PEPEndpointInfo endpointInfo = null;

	private String targetId;

	private String byWhom;

	private String action;

	private String target;

	private String notificationType;

	private Map<String, String> messageTemplate = new HashMap<String, String>();

	private static final Log log = LogFactory.getLog(MessageSendingModuleOnPolicyUpdate.class);

	public MessageSendingModuleOnPolicyUpdate(PEPEndpointInfo endpointInfo, String notificationType) {

		this.endpointInfo=endpointInfo;
		this.notificationType = notificationType;
		connectionManager = new ThreadSafeClientConnManager();
	}

	@Override
	public void run() {
		 if(endpointInfo!=null){

			 HttpPost post = new HttpPost(endpointInfo.getEndpoint());
			 HttpResponse response=null;
			 try{

				 HttpClient client = new DefaultHttpClient(connectionManager);

				 StringEntity entity;
				 if("JSON".equalsIgnoreCase(notificationType)){
					 post.setHeader("Content-Type", "application/json");
					 post.setHeader(AUTHORIZATION_HEADER, getBase64EncodedBasicAuthHeader
                             (endpointInfo.getUsername(), endpointInfo.getPassword()));
					 entity = new StringEntity(getJSONData());
				 } else {
					 post.setHeader("Content-Type", "application/xml");
					 post.setHeader(AUTHORIZATION_HEADER,getBase64EncodedBasicAuthHeader
                             (endpointInfo.getUsername(), endpointInfo.getPassword()));
					 entity = new StringEntity(getXMLData());
				 }
				 log.debug("sending notification to endpoint :" + endpointInfo.getEndpoint());
				 post.setEntity(entity);
				 response= client.execute(post);
			 } catch (Exception e) {
				 log.error("Message sending is failed" , e);
			 } finally {
				 if(response != null){
					 log.debug(response.getStatusLine());
				 }
				 post.abort();
			 }
		 }
	}

	/**
	 *
	 * @return the xml message which wants to be sent to the external endpoint
	 */
	private String getXMLData(){

		String rootNodeName  = messageTemplate.get("root");
		String targetIdNodeName = messageTemplate.get("targetId");
		String targetNodeName = messageTemplate.get("target");
		String actionNodeName = messageTemplate.get("action");
		String userNodeName = messageTemplate.get("user");

		String data = "<"+rootNodeName+"><"+targetIdNodeName+">" + targetId + "</"+targetIdNodeName+">";
		if (target != null){
			data = data + "<"+targetNodeName+">" + target + "</"+targetNodeName+">";
		}

		if(action != null){
			data = data + "<"+actionNodeName+">" + action + "</"+actionNodeName+">";
		}

		if(byWhom != null){
			data = data + "<"+userNodeName+">" + byWhom + "</"+userNodeName+">";
		}
		data = data + "</"+rootNodeName+"> ";

		return data;
	}

	/**
	 * the json message which wants to be sent to the external endpoint
	 * @return the message which should be sent to external endpoint
	 */
	 private String getJSONData(){

		String rootNodeName  = messageTemplate.get("root");
		String targetIdNodeName = messageTemplate.get("targetId");
		String targetNodeName = messageTemplate.get("target");
		String actionNodeName = messageTemplate.get("action");
		String userNodeName = messageTemplate.get("user");

		try{
			JSONObject root = new JSONObject();

			JSONObject object = new JSONObject();
			object.put(targetIdNodeName, targetId);

			if (target != null){
				object.put(targetNodeName, target);
			}
			if(action != null){
				object.put(actionNodeName, action);
			}
			if(byWhom != null){
				object.put(userNodeName, byWhom);
			}

			root.put(rootNodeName, object);

			return root.toString() + " ";

		} catch (Exception e){
			log.error("Error while building JSON Message");
		}

		return "";
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public void setByWhom(String byWhom) {
		this.byWhom = byWhom;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setMessageTemplate(Map<String, String> messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	/**
	 * to get base64 encoded username and password
	 * @param userName username
	 * @param password password
	 * @return base64 encoded username:password
	 */
	private String getBase64EncodedBasicAuthHeader(String userName, String password) {
		String concatenatedCredential = userName + ":" + password;
		byte[] byteValue = concatenatedCredential.getBytes();
		String encodedAuthHeader = Base64.encode(byteValue);
		encodedAuthHeader = "Basic " + encodedAuthHeader;
		return encodedAuthHeader;
	}
}
