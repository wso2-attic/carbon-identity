package org.wso2.carbon.user.cassandra;/*
 *   Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;

import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.jdbc.JDBCRealmConstants;
import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialsException;

public class Util {


    public static String createRowKeyForReverseLookup(String identifier, String credentialTypeName) {
        return credentialTypeName + "::" + identifier;
    }

    public static String createRowKeyForReverseLookup(Credential credential) {
        return createRowKeyForReverseLookup(credential.getIdentifier(), credential.getCredentialsType());
    }

    private static Serializer<String> stringSerializer = StringSerializer.get();

    public static String getExistingUserId(String credentialTypeName, String identifier,
                                           Keyspace keyspace) {

        identifier = createRowKeyForReverseLookup(identifier, credentialTypeName);
        ColumnQuery<String, String, String> usernameIndexQuery = HFactory
                .createColumnQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);

        usernameIndexQuery.setColumnFamily(CFConstants.USERNAME_INDEX).setKey(identifier)
                .setName(CFConstants.USER_ID);

        QueryResult<HColumn<String, String>> result = usernameIndexQuery.execute();

        HColumn<String, String> userIdCol = result.get();

        if (userIdCol == null) {
            return null;
        }

        return userIdCol.getValue();
    }

    public static ColumnSlice<String, String> getExistingCredentialsRow(String identifier,
                                                                        String credentialTypeName,
                                                                        Keyspace keyspace) {

        identifier = createRowKeyForReverseLookup(identifier, credentialTypeName);
        SliceQuery<String, String, String> credentialRowQuery = HFactory
                .createSliceQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);

        credentialRowQuery.setColumnFamily(CFConstants.USERNAME_INDEX).setKey(identifier)
                .setRange("", "", true, 100);


        return credentialRowQuery.execute().get();
    }

    public static String getSaltValue(String identifier,
                               String credentialTypeName,
                               Keyspace keyspace) {

        identifier = createRowKeyForReverseLookup(identifier, credentialTypeName);

        ColumnQuery<String, String, String> saltValQuery = HFactory
                .createColumnQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);

        saltValQuery.setColumnFamily(CFConstants.USERNAME_INDEX).setKey(identifier)
                .setName(CFConstants.SALT_VALUE);
        HColumn<String, String> saltValResult = saltValQuery.execute().get();

        if (saltValResult == null) {
            return null;
        }

        return saltValResult.getValue();
    }


    private static Log log = LogFactory.getLog(Util.class);

    private static RealmConfiguration realmConfig;

    public static RealmConfiguration getRealmConfig() {
        return realmConfig;
    }

    public static void setRealmConfig(RealmConfiguration realmConfig) {
        Util.realmConfig = realmConfig;
    }

    public static String preparePassword(String password, String saltValue) throws MultipleCredentialsException {
        try {
            String digestInput = password;
            if (saltValue != null) {
                digestInput = password + saltValue;
            }
            String digsestFunction = Util.getRealmConfig().getUserStoreProperties().get(
                    JDBCRealmConstants.DIGEST_FUNCTION);
            if (digsestFunction != null) {

                if (digsestFunction.
                        equals(UserCoreConstants.RealmConfig.PASSWORD_HASH_METHOD_PLAIN_TEXT)) {
                    return password;
                }

                MessageDigest dgst = MessageDigest.getInstance(digsestFunction);
                byte[] byteValue = dgst.digest(digestInput.getBytes());
                password = Base64.encode(byteValue);
            }
            return password;
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            throw new MultipleCredentialsException(e.getMessage(), e);
        }
    }

    protected static Random random = new Random();

    public static String getSaltValue() {
        String saltValue = null;
        if ("true".equals(realmConfig.getUserStoreProperties().get(
                JDBCRealmConstants.STORE_SALTED_PASSWORDS))) {
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            saltValue = Base64.encode(bytes);
        }
        return saltValue;
    }
}
