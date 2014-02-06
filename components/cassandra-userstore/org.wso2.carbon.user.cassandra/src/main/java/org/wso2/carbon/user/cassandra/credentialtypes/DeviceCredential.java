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

import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.wso2.carbon.user.cassandra.CFConstants;
import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.CredentialDoesNotExistException;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialsException;
import org.wso2.carbon.user.core.multiplecredentials.UserDoesNotExistException;

/**
 * This class represents the device identifier credential. This can be used to
 * authenticate devices using their IMEI numbers.
 * 
 */
public class DeviceCredential extends AbstractCassandraCredential {

	@Override
	public void update(String userId, Credential newCredential) throws MultipleCredentialsException {
		// String existingUserId = getExistingUserId(identifier);

		// get device id to overwrite
		String oldDeviceId = getExistingCredentialId(userId);

		if (oldDeviceId == null) {
			throw new CredentialDoesNotExistException("Credential of type: " + credentialTypeName +
			                                          " does not exist for user id : " + userId);
		}

		Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

		String newDeviceId = newCredential.getIdentifier();
		// overright user id
		mutator.addInsertion(userId, CFConstants.USERS,
		                     HFactory.createColumn(credentialTypeName, newDeviceId));

		// delete old id off index
		mutator.addDeletion(createRowKeyForReverseLookup(oldDeviceId), CFConstants.USERNAME_INDEX,
		                    null, stringSerializer);

		// add new id to index
		mutator.addInsertion(createRowKeyForReverseLookup(newDeviceId), CFConstants.USERNAME_INDEX,
		                     HFactory.createColumn(CFConstants.USER_ID, userId));

		mutator.execute();
	}

	@Override
	public boolean authenticate(Credential credential) {
		// TODO check for active
		try {
			String userId = getExistingUserId(credential.getIdentifier());
			if (userId == null) {
				return false;
			}
		} catch (UserDoesNotExistException e) {
			return false;
		}
		return true;
	}

	@Override
	public Credential get(String identifier) throws MultipleCredentialsException {
		// String userId = getExistingUserId(identifier);
		ColumnSlice<String, String> credentialRow = getCredentialRow(identifier);

		if (credentialRow == null) {
			throw new CredentialDoesNotExistException("Credential of type: " + credentialTypeName +
			                                          " does not exist for identifier : " +
			                                          identifier);
		}

		Credential credential = new Credential();
		credential.setCredentialsType(getCredentialTypeName());
		credential.setIdentifier(identifier);
		return credential;

	}

	//@Override
	public boolean isNullSecretAllowed() {
		return true;
	}

}
