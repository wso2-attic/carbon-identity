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
package org.wso2.carbon.identity.entitlement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.entitlement.common.EntitlementConstants;
import org.wso2.carbon.identity.entitlement.dto.StatusHolder;
import org.wso2.carbon.identity.entitlement.model.PEPEndpointInfo;
import org.wso2.carbon.identity.entitlement.modules.EmailSendingModuleOnPolicyUpdate;
import org.wso2.carbon.identity.entitlement.modules.MessageSendingModuleOnPolicyUpdate;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This an extension for <code>PAPStatusDataHandler</code> module that can use to send policy status as notifications.
 * This module supports Email and REST notifications.
 */
public class EntitlementNotificationExtension implements PAPStatusDataHandler {

	private String roleName;

	private String notificationType;

	private PEPEndpointInfo endpointInfo;

	private String emailTemplate;

	private String emailSubject;

	private String emailAddress;

	private boolean pdpUpdate = true;

	private boolean papUpdate = false;

	private List<String> pdpActions = new ArrayList<String>();

	private static ExecutorService threadPool = Executors.newFixedThreadPool(5);

	private static final Log log = LogFactory.getLog(EntitlementNotificationExtension.class);


	/**
	 * at the initialization a property map which carries relevant properties to this extension will
	 * be passed and class variables will be set from those properties.
	 * @param properties properties
	 */
	@Override
	public void init(Properties properties) {
		// read the user group that email would send
		roleName = properties.getProperty("roleName");

		// notification type whether it is email or message
		notificationType = properties.getProperty("notificationType");

		// end point url that message is send.
		String pepEndpointInfo = properties.getProperty("targetUrl");
		if (pepEndpointInfo != null && !"".equalsIgnoreCase(pepEndpointInfo.trim())) {
			endpointInfo = new PEPEndpointInfo();
			String[] endpointInfoArray = pepEndpointInfo.split(";");
			endpointInfo.setEndpoint(endpointInfoArray[0]);
			endpointInfo.setUsername(endpointInfoArray[1]);
			endpointInfo.setPassword(endpointInfoArray[2]);
		}

		String emailTemplateFile = properties.getProperty("emailTemplateFile");

		emailAddress = properties.getProperty("emailAddress");
		emailSubject = properties.getProperty("emailSubject");
		if (emailSubject == null || emailSubject.trim().length() == 0) {
			emailSubject = "XACML Policy Notification";
		}

		// ignore back end SSL certificate verification
		String ignoreVerification = properties.getProperty("ignoreServerVerification");

		// only pdp policy updates
		if (properties.getProperty("pdpNotification") != null) {
			pdpUpdate = Boolean.parseBoolean(properties.getProperty("pdpNotification"));
		}

		// only pap policy updates
		papUpdate = Boolean.parseBoolean(properties.getProperty("papNotification"));

		// pdp action
		String pdpActionUpdate = properties.getProperty("pdpNotificationAction");
		if (pdpActionUpdate != null) {
			String[] pdpActionUpdates = pdpActionUpdate.split(";");
			for (String update : pdpActionUpdates) {
				pdpActions.add(update.trim());
			}
		}

		if (Boolean.parseBoolean(ignoreVerification)) {
			// set the trust store as a system property for communication over TLS.
			try {
				// Get SSL context
				SSLContext sc = SSLContext.getInstance("SSL");

				// Create empty HostnameVerifier
				HostnameVerifier hv = new HostnameVerifier() {
					public boolean verify(String urlHostName, SSLSession session) {
						return true;
					}
				};
				HttpsURLConnection.setDefaultHostnameVerifier(hv);

				// Create a trust manager that does not validate certificate chains
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
					                               String authType) {
					}

					public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
					                               String authType) {
					}
				} };

				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				SSLContext.setDefault(sc);
			} catch (Exception e) {
				log.error(e);
			}
		}

		//load email template
		loadEmailTemplate(emailTemplateFile);
	}

	@Override
	public void handle(String about, String key, List<StatusHolder> statusHolder) throws EntitlementException {

		if (!about.equals(EntitlementConstants.Status.ABOUT_POLICY)) {
			return;
		}

		if (statusHolder != null) {
			for (StatusHolder holder : statusHolder) {
				handle(about, holder);
			}
		}

	}

	/**
	 * handler will decide the process depending on the status in status holder
	 * @param about indicates what is related with this admin status action
	 * @param statusHolder <code>StatusHolder</code>
	 * @throws EntitlementException
	 */
	@Override
	public void handle(String about, StatusHolder statusHolder) throws EntitlementException {

		if (!EntitlementConstants.Status.ABOUT_POLICY.equals(about)) {
			return;
		}

		String action = null;
		String typeOfAction = statusHolder.getType();

		//If papUpdate notifications are enabled through entitlement.properties
		if (papUpdate) {
			if (EntitlementConstants.StatusTypes.UPDATE_POLICY.equals(typeOfAction)) {
				action = "UPDATE";
			} else if (EntitlementConstants.StatusTypes.DELETE_POLICY.equals(typeOfAction)) {
				action = "DELETE";
			} else if (EntitlementConstants.StatusTypes.ADD_POLICY.equals(typeOfAction)) {
				action = "CREATE";
			}
		}

		//if pdpUpdate properties are enabled through entitlement.properties
		if (pdpUpdate) {
			if (EntitlementConstants.StatusTypes.PUBLISH_POLICY.equals(typeOfAction)) {
				action = statusHolder.getTargetAction();
			}

			if (action == null || (pdpActions.size() > 0 && !pdpActions.contains(action))) {
				return;
			}

			if (EntitlementConstants.PolicyPublish.ACTION_CREATE.equals(action) ||
			    EntitlementConstants.PolicyPublish.ACTION_UPDATE.equals(action)) {
				action = "UPDATE";
			}
		}

		if (action == null) {
			return;
		}

		//If the notification type is EMAIL, email sending modules will be registered
		if ("EMAIL".equalsIgnoreCase(notificationType)) {
			UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();
			try {

				if (emailAddress == null || emailAddress.trim().length() == 0) {
					String[] userNames = userRealm.getUserStoreManager().getUserListOfRole(roleName);

					for (String userName : userNames) {
						String email = null;
						try {
							email = userRealm.getUserStoreManager().getUserClaimValue(userName, "email", null);
						} catch (Exception e) {
							//ignore to be loop
						}
						if (email != null) {
							EmailSendingModuleOnPolicyUpdate module = new EmailSendingModuleOnPolicyUpdate();
							module.setEmailAddress(email);
							module.setUserName(userName);
							module.setTargetId(statusHolder.getKey());
							module.setByWhom(statusHolder.getUser());
							module.setTarget(statusHolder.getMessage());
							module.setAction(action);
							module.setEmailTemplate(emailTemplate);
							module.setEmailSubject(emailSubject);
							threadPool.submit(module);
						}

					}
				} else {
					EmailSendingModuleOnPolicyUpdate module = new EmailSendingModuleOnPolicyUpdate();
					module.setEmailAddress(emailAddress);
					module.setTargetId(statusHolder.getKey());
					module.setByWhom(statusHolder.getUser());
					module.setTarget(statusHolder.getMessage());
					module.setAction(action);
					module.setEmailTemplate(emailTemplate);
					module.setEmailSubject(emailSubject);
					threadPool.submit(module);
				}
			} catch (UserStoreException e) {
				log.error(e);
			}

		//if the notification type is XML or JSON
		} else if ("XML".equalsIgnoreCase(notificationType) || "JSON".equalsIgnoreCase(notificationType)) {

			Map<String, String> messageTemplate = new HashMap<String, String>();
			messageTemplate.put("root", "PolicyNotification");
			messageTemplate.put("targetId", "PolicyId");
			messageTemplate.put("target", "Policy");
			messageTemplate.put("action", "Action");
			messageTemplate.put("user", "User");

			MessageSendingModuleOnPolicyUpdate module
			  = new MessageSendingModuleOnPolicyUpdate(endpointInfo, notificationType);
			module.setTargetId(statusHolder.getKey());
			module.setByWhom(statusHolder.getUser());
			module.setTarget(statusHolder.getMessage());
			module.setMessageTemplate(messageTemplate);
			module.setAction(action);
			threadPool.submit(module);
		}
	}


	//load the email template given in the configuration file, If not present
	//a default template will be used
	private void loadEmailTemplate(String emailTemplateFile) {

		if (emailTemplateFile != null && emailTemplateFile.trim().length() > 0) {
			BufferedReader bufferedReader = null;
			InputStreamReader streamReader = null;
			try {
				streamReader = new InputStreamReader(new FileInputStream(emailTemplateFile), "UTF-8");
				bufferedReader = new BufferedReader(streamReader);
				StringBuilder stringBuilder = new StringBuilder();
				String line = bufferedReader.readLine();

				while (line != null) {
					stringBuilder.append(line);
					stringBuilder.append(System.getProperty("line.separator"));
					line = bufferedReader.readLine();
				}
				emailTemplate = stringBuilder.toString();
			} catch (Exception e) {
				// ignore
				log.error(e);
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (IOException e) {
						log.error(e);
					}
				}
				if (streamReader != null) {
					try {
						streamReader.close();
					} catch (IOException e) {
						log.error(e);
					}
				}
			}
		}

		if (emailTemplate == null) {
			emailTemplate =
			  "Hi {username}\n\n" +

			  "XACML PDP policy store has been changed.\n\n" +

			  "Policy Id : {targetId}\n" +
			  "Action : {action}\n" +
			  "By User : {byWhom}\n" +

			  "Best Regards\n" +
			  "http://xacmlinfo.org";
		}
	}

	@Override
	public StatusHolder[] getStatusData(String s, String s1, String s2, String s3) throws EntitlementException {
		return new StatusHolder[0];
	}
}
