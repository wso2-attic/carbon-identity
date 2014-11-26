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
import org.wso2.carbon.identity.entitlement.NotificationConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Email sending module that uses Axis2 email sender.  which is responsible for sending email
 * on the action of change of user information. ie user operations.
 */
public class EmailSendingModuleOnUserOperation implements Runnable {

	private String emailAddress;

	private String subjectId;
	// salutation which wants to be used in the mail
	private String salutation;

	private String eventType;

	private static  final Log log = LogFactory.getLog(EmailSendingModuleOnUserOperation.class);

	public EmailSendingModuleOnUserOperation(String eventType) {
		this.eventType = eventType;
	}

	@Override
	public void run() {

		try {
			Map<String, String> headerMap = new HashMap<String, String>();
			PrivilegedCarbonContext.startTenantFlow();
			headerMap.put(MailConstants.MAIL_HEADER_SUBJECT, getEmailSubject());

			String requestMessage = getMessage();

			OMElement payload = OMAbstractFactory.getOMFactory().createOMElement(
			  BaseConstants.DEFAULT_TEXT_WRAPPER, null);
			payload.setText(requestMessage);
			ServiceClient serviceClient;
			ConfigurationContext configContext =
                    CarbonConfigurationContextFactory.getConfigurationContext();
			if (configContext != null) {
				serviceClient = new ServiceClient(configContext, null);
			} else {
				serviceClient = new ServiceClient();
			}
			//setting properties for email sending module
			Options options = new Options();
			options.setProperty(Constants.Configuration.ENABLE_REST, Constants.VALUE_TRUE);
			options.setProperty(MessageContext.TRANSPORT_HEADERS, headerMap);
			options.setProperty(MailConstants.TRANSPORT_MAIL_FORMAT,
			  MailConstants.TRANSPORT_FORMAT_TEXT);
			options.setTo(new EndpointReference("mailto:" + emailAddress));
			serviceClient.setOptions(options);
			serviceClient.fireAndForget(payload);
			log.info("User notification email has been sent to " + emailAddress);
		} catch (Exception e) {
			log.error("Email sending is failed for address " + emailAddress, e);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}

	}


	//to get the subject depending on the action took place
	private String getEmailSubject() {

		String subjectLine = "";

		if (NotificationConstants.EVENT_TYPE_USER_REMOVE.equals(eventType)) {
			subjectLine = "User removal notification";
		} else if (NotificationConstants.EVENT_TYPE_PROFILE_UPDATE.equals(eventType)) {
			subjectLine = "User profile update notification";
		}

		return subjectLine;
	}

	//to get the message which wants to be sent as an email
	private String getMessage() {

		StringBuilder message = new StringBuilder();

		message.append("Hi ");
		message.append(salutation);
		message.append(System.getProperty("line.separator"));
		message.append(System.getProperty("line.separator"));

		if (NotificationConstants.EVENT_TYPE_USER_REMOVE.equals(eventType)) {
			message.append("User with subject id: ");
			message.append(subjectId);
			message.append(" has been removed");
			message.append(System.getProperty("line.separator"));

		} else if (NotificationConstants.EVENT_TYPE_PROFILE_UPDATE.equals(eventType)) {
			message.append("User profile updated for user with subject id: ");
			message.append(subjectId);
			message.append(System.getProperty("line.separator"));
		}

		message.append(System.getProperty("line.separator"));
		message.append("Best Regards");
		message.append(System.getProperty("line.separator"));
		message.append("Identity Server");

		return message.toString();
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}

	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

}
