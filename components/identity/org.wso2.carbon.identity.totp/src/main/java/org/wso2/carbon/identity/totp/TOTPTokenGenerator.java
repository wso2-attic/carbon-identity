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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.totp;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.notification.mgt.NotificationManagementException;
import org.wso2.carbon.identity.notification.mgt.NotificationSender;
import org.wso2.carbon.identity.notification.mgt.bean.PublisherEvent;
import org.wso2.carbon.identity.totp.exception.TOTPException;
import org.wso2.carbon.identity.totp.internal.TOTPManagerComponent;
import org.wso2.carbon.identity.totp.util.TOTPUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


/**
 * TOTP Token generator class.
 */
public class TOTPTokenGenerator {

	private static Log log = LogFactory.getLog(TOTPTokenGenerator.class);
	private static volatile TOTPTokenGenerator instance;
	private final String eventName = "TOTPOperation";
	private final String usernameLabel = "username";
	private final String operationLabel = "operation";
	private final String tokenLabel = "token";

	private TOTPTokenGenerator() {
	}

	;

	/**
	 * Singleton method to get instance of TOTPTokenGenerator.
	 *
	 * @return instance of TOTPTokenGenerator
	 */
	public static TOTPTokenGenerator getInstance() {
		if (instance == null) {
			synchronized (TOTPTokenGenerator.class) {
				if (instance == null) {
					instance = new TOTPTokenGenerator();
				}
			}
		}
		return instance;
	}

	/**
	 * Generate TOTP token for a locally stored user.
	 *
	 * @param username username of the user
	 * @return TOTP token as a String
	 * @throws TOTPException
	 */
	public String generateTOTPTokenLocal(String username)
			throws TOTPException {

		long token = 0;
		try {
			int tenantId = IdentityUtil.getTenantIdOFUser(username);
			UserRealm userRealm = TOTPManagerComponent.getRealmService().getTenantUserRealm(tenantId);
			if (userRealm != null) {
				UserStoreManager userStoreManager = userRealm.getUserStoreManager();
				String secretKey = userStoreManager.getUserClaimValue(MultitenantUtils.getTenantAwareUsername
						(username), Constants.SECRET_KEY, null);

				byte[] secretkey;
				String encoding = "Base32";
				try {
					encoding = TOTPUtil.getEncodingMethod();
				} catch (IdentityApplicationManagementException e) {
					log.error("Error when fetching the encoding method");
				}

				if ("Base32".equals(encoding)) {
					Base32 codec32 = new Base32();
					secretkey = codec32.decode(secretKey);
				} else {
					Base64 code64 = new Base64();
					secretkey = code64.decode(secretKey);
				}
				try {
					token = getCode(secretkey, getTimeIndex());
					sendNotification("TOTP token Generator",username,Long.toString(token));
					if (log.isDebugEnabled()) {
						log.debug("Token is sent to via email to the user : " + username);
					}

				} catch (NoSuchAlgorithmException e) {
					throw new TOTPException("TOTPTokenGenerator can't find the configured hashing algorithm", e);
				} catch (InvalidKeyException e) {
					throw new TOTPException("Secret key is not valid", e);
				}
			} else {
				throw new TOTPException("Cannot find the user realm for the given tenant domain : " + CarbonContext
						.getThreadLocalCarbonContext().getTenantDomain());
			}
		} catch (IdentityException e) {
			throw new TOTPException("TOTPTokenGenerator failed while trying to get the tenant ID of the user " +
			                        username, e);
		} catch (UserStoreException e) {
			throw new TOTPException("TOTPTokenGenerator failed while trying to access userRealm of the user : " +
			                        username, e);
		}
		return Long.toString(token);
	}

	/**
	 * Generate TOTP token for a given Secretkey
	 *
	 * @param secretKey Secret key
	 * @return TOTP token as a string
	 * @throws TOTPException
	 */
	public String generateTOTPToken(String secretKey) throws TOTPException {

		long token = 0;

		byte[] secretkey;
		String encoding = "Base32";
		try {
			encoding = TOTPUtil.getEncodingMethod();
		} catch (IdentityApplicationManagementException e) {
			throw new TOTPException("Error when fetching the encoding method",e);
		}

		if ("Base32".equals(encoding)) {
			Base32 codec32 = new Base32();
			secretkey = codec32.decode(secretKey);
		} else {
			Base64 code64 = new Base64();
			secretkey = code64.decode(secretKey);
		}

		try {
			token = getCode(secretkey, getTimeIndex());
		} catch (NoSuchAlgorithmException e) {
			throw new TOTPException("TOTPTokenGenerator can't find the configured hashing algorithm", e);
		} catch (InvalidKeyException e) {
			throw new TOTPException("Secret key is not valid", e);
		}
		return Long.toString(token);
	}

	/**
	 * Create the TOTP token for a given secret key and time index
	 *
	 * @param secret    Secret key
	 * @param timeIndex Number of Time elapse from the unix epoch time
	 * @return TOTP token value as a long
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeyException
	 */
	private long getCode(byte[] secret, long timeIndex)
			throws NoSuchAlgorithmException, InvalidKeyException {
		//One line between
		SecretKeySpec signKey = new SecretKeySpec(secret, "HmacSHA1");
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(timeIndex);
		byte[] timeBytes = buffer.array();
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(timeBytes);
		int offset = hash[19] & 0xf;
		long truncatedHash = hash[offset] & 0x7f;
		for (int i = 1; i < 4; i++) {
			truncatedHash <<= 8;
			truncatedHash |= hash[offset + i] & 0xff;
		}
		truncatedHash %= 1000000;
		return truncatedHash;
	}

	/**
	 * Get Time steps from unix epoch time.
	 *
	 * @return
	 */
	private static long getTimeIndex() {
		return System.currentTimeMillis() / 1000 / 30;
	}


	private void sendNotification(String operation, String username, String token) {
		NotificationSender notificationSender = TOTPManagerComponent.getNotificationSender();
		if (notificationSender != null) {
			try {
				PublisherEvent event = new PublisherEvent(eventName);
				event.addEventProperty(operationLabel, operation);
				event.addEventProperty(usernameLabel, username);
				event.addEventProperty(tokenLabel,token);
				if (log.isDebugEnabled()) {
					log.debug("Invoking notification sender");
				}
				notificationSender.invoke(event);
			} catch (NotificationManagementException e) {
				log.error("Error while sending notifications on user operations", e);
			}
		} else {
			log.error("No registered notification sender found. Notification sending aborted");
		}
	}
}
