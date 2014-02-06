/*
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.user.cassandra.credentialtypes;

import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialsException;

/**
 * This class represents the phone number credential. Users can use their phone
 * numbers to register and login. Users much provide a secret.
 */
public class PhoneNumberCredential extends EmailCredential {

	@Override
	public boolean isNullSecretAllowed() {
		return false;
	}

	public void add(String userId, Credential credential) throws MultipleCredentialsException {
		//TODO : validate phone number
		super.add(userId, credential);
	}
}
