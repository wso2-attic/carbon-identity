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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

import org.wso2.carbon.user.cassandra.CFConstants;
import org.wso2.carbon.user.cassandra.Util;
import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.CredentialDoesNotExistException;
import org.wso2.carbon.user.core.multiplecredentials.CredentialProperty;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialsException;

/**
 * This class represents the email type credential. Users can register and get
 * authenticated using their email address. This credential requires a secret.
 * 
 */
public class EmailCredential extends AbstractCassandraCredential {

	@Override
	public void add(String userId, Credential credential) throws MultipleCredentialsException {
		// create digest of password and salt

		if (credential.getSecret() != null) {
			String saltValue = Util.getSaltValue();
			String preparedPassword = Util.preparePassword(credential.getSecret(), saltValue);

			credential.setSecret(preparedPassword);

			List<CredentialProperty> credentialPropertyList;
			if (credential.getCredentialProperties() == null) {
				credentialPropertyList = new ArrayList<CredentialProperty>();
				CredentialProperty credentialProperty = new CredentialProperty();
				credentialProperty.setKey(CFConstants.SALT_VALUE);
				credentialProperty.setValue(saltValue);
				credentialPropertyList.add(credentialProperty);

			} else {
				credentialPropertyList = Arrays.asList(credential.getCredentialProperties());
				for (int i = 0; i < credentialPropertyList.size(); i++) {
					CredentialProperty credentialProperty = credentialPropertyList.get(i);
					if (CFConstants.SALT_VALUE.equals(credentialProperty.getKey())) {
						credentialProperty.setValue(saltValue);
						credentialPropertyList.add(credentialProperty);
					}
				}
			}

			credential.setCredentialProperties(credentialPropertyList.toArray(new CredentialProperty[credentialPropertyList.size()]));
		}
		super.add(userId, credential); // To change body of overridden methods
									   // use File | Settings | File Templates.
	}

	@Override
	public void update(String userId, Credential credential) throws MultipleCredentialsException {
		// get device id to overwrite
		String oldDeviceId = getExistingCredentialId(userId);

		if (oldDeviceId == null) {
			throw new CredentialDoesNotExistException("Credential of type: " + credentialTypeName +
			                                          " does not exist for user id : " + userId);
		}

		Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

		String identifier = credential.getIdentifier();
		// overright user id
		mutator.addInsertion(userId, CFConstants.USERS,
		                     HFactory.createColumn(credentialTypeName, identifier));

		// delete old id off index
		mutator.addDeletion(createRowKeyForReverseLookup(oldDeviceId), CFConstants.USERNAME_INDEX,
		                    null, stringSerializer);

		// add new id, secret properties to index
		// add identifier - also functions as reverse look up
		mutator.addInsertion(createRowKeyForReverseLookup(identifier), CFConstants.USERNAME_INDEX,
		                     HFactory.createColumn(CFConstants.USER_ID, userId));

		// add identifier
		mutator.addInsertion(createRowKeyForReverseLookup(identifier), CFConstants.USERNAME_INDEX,
		                     HFactory.createColumn(CFConstants.SECRET, credential.getSecret()));

		// add cred properties
		if (credential.getCredentialProperties() != null) {
			for (CredentialProperty credentialProperty : credential.getCredentialProperties()) {
				if (CFConstants.USER_ID.equals(credentialProperty.getKey()) ||
				    CFConstants.SECRET.equals(credentialProperty.getKey()) ||
				    CFConstants.IS_ACTIVE.equals(credentialProperty.getKey())) {
					throw new ReservedPropertiesUsedException("The reserved property " +
					                                          credentialProperty.getKey() +
					                                          " is used.");
				}
				mutator.addInsertion(createRowKeyForReverseLookup(identifier),
				                     CFConstants.USERNAME_INDEX,
				                     HFactory.createColumn(credentialProperty.getKey(),
				                                           credential.getSecret()));
			}
		}

		mutator.execute();

	}

	@Override
	public boolean authenticate(Credential credential) throws MultipleCredentialsException {
		if (credential.getSecret() == null) {
			return false;
		}
		ColumnSlice<String, String> credentialRow = getCredentialRow(credential.getIdentifier());

		HColumn<String, String> saltValueCol =
		                                       credentialRow.getColumnByName(CFConstants.SALT_VALUE);
		String saltVal = null;
		if (saltValueCol != null) {
			saltVal = saltValueCol.getValue();
		}

		HColumn<String, String> secretCol = credentialRow.getColumnByName(CFConstants.SECRET);
		if (secretCol == null) {
			return false;
		}
		String preparedPasswordHash = Util.preparePassword(credential.getSecret(), saltVal);

		if (secretCol.getValue().equals(preparedPasswordHash)) {
			return true;
		}

		return false;
	}

	@Override
	public Credential get(String identifier) throws MultipleCredentialsException {
		ColumnSlice<String, String> credentialRow = getCredentialRow(identifier);
		if (credentialRow == null) {
			throw new CredentialDoesNotExistException("Credential of type: " + credentialTypeName +
			                                          " does not exist for identifier : " +
			                                          identifier);
		}

		Credential credential = new Credential();
		credential.setIdentifier(identifier);
		credential.setSecret(credentialRow.getColumnByName(CFConstants.SECRET).getValue());
		credential.setCredentialsType(credentialTypeName);
		for (HColumn<String, String> credProperty : credentialRow.getColumns()) {
			if (CFConstants.SECRET.equals(credProperty.getValue())) {
				continue;
			}
			CredentialProperty credentialProperty = new CredentialProperty();
			credentialProperty.setKey(credProperty.getName());
			credentialProperty.setKey(credProperty.getValue());
		}
		return credential;
	}

	//@Override
    public boolean isNullSecretAllowed() {
	    return false;
    }
}
