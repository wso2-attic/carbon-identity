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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.mail.MailConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Email sending module on policy update that uses Axis2 email sender. This Module will send emails
 * according to a particular format on change of policies
 */
public class EmailSendingModuleOnPolicyUpdate implements Runnable {

	private String userName;

	private String emailAddress;
	//changed entity Id of the user
	private String targetId;
	//person who sends the email
	private String byWhom;
	// action on policy ex- UPDATE
	private String action;

	private String target;

	private String emailSubject;

	private String emailTemplate;

	private static final Log log = LogFactory.getLog(EmailSendingModuleOnPolicyUpdate.class);

	@Override
	public void run() {

		try {
			Map<String, String> headerMap = new HashMap<String, String>();
			PrivilegedCarbonContext.startTenantFlow();

			if (emailSubject == null || emailSubject.trim().length() == 0) {
				emailSubject = "WSO2 Notification";
			}
			headerMap.put(MailConstants.MAIL_HEADER_SUBJECT, emailSubject);

			String requestMessage = getMessage();

			OMElement payload = OMAbstractFactory.getOMFactory().createOMElement(
			  BaseConstants.DEFAULT_TEXT_WRAPPER, null);
			payload.setText(requestMessage);
			ServiceClient serviceClient;
			ConfigurationContext configContext = CarbonConfigurationContextFactory.getConfigurationContext();

			if (configContext != null) {
				serviceClient = new ServiceClient(configContext, null);
			} else {
				serviceClient = new ServiceClient();
			}
			// setting properties for email sending module
			Options options = new Options();
			options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
			options.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
			options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT,
			  MailConstants.TRANSPORT_FORMAT_TEXT);
			options.setTo(new EndpointReference("mailto:" + emailAddress));
			serviceClient.setOptions(options);
			serviceClient.fireAndForget(payload);
			log.info("Policy notification email has been sent to " + emailAddress);
		} catch (Exception e) {
			log.error("Email sending is failed for address " + emailAddress, e);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}

	}

	/**
	 * get the string message which should be sent. A default message is draft here and these
	 * variables will be overwritten from configurations
	 * @return The message which should be sent to the external endpoint
	 */
	private String getMessage() {

		if (emailTemplate == null || emailTemplate.trim().length() == 0) {
			emailTemplate =
			  "Hi {username}\n\n" +

			  "Target has been changed.\n\n" +

			  "Target Id : {targetId}\n" +
			  "Action : {action}\n" +
			  "By User : {byWhom}\n" +
			  "Target : {target}\n\n" +

			  "Best Regards\n" +
			  "http://soasecurity.org";
		}

		Map<String, String> map = new HashMap<String, String>();
		map.put("username", userName);
		map.put("targetId", targetId);
		map.put("action", action);
		map.put("byWhom", byWhom);
		map.put("target", target);

		return replacePlaceHolders(emailTemplate, map);
	}

	/**
	 * Replace the {user-parameters} in the config file with the respective
	 * values
	 *
	 * @param text           the initial text
	 * @param userParameters mapping of the key and its value
	 * @return the final text to be sent in the email
	 */
	private static String replacePlaceHolders(String text, Map<String, String> userParameters) {
		if (userParameters != null) {
			for (Map.Entry<String, String> entry : userParameters.entrySet()) {
				String key = entry.getKey();
				if (key != null && entry.getValue() != null) {
					text = text.replaceAll("\\{" + key + "\\}", entry.getValue());
				}
			}
		}
		return text;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public void setByWhom(String byWhom) {
		this.byWhom = byWhom;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public void setEmailTemplate(String emailTemplate) {
		this.emailTemplate = emailTemplate;
	}
}
