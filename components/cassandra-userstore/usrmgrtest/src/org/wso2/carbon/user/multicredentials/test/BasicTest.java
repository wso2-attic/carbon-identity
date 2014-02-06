package org.wso2.carbon.user.multicredentials.test;/*
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

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.user.profile.stub.UserProfileMgtServiceStub;
import org.wso2.carbon.identity.user.profile.stub.types.UserProfileDTO;
import org.wso2.carbon.user.cassandra.CFConstants;
import org.wso2.carbon.user.mgt.multiplecredentials.stub.types.Credential;
import org.wso2.carbon.user.mgt.multiplecredentials.stub.types.carbon.ClaimValue;
import org.wso2.carbon.user.mgt.stub.MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.MultipleCredentialsUserAdminStub;

import java.rmi.RemoteException;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class BasicTest {

    private MultipleCredentialsUserAdminStub multipleCredsAdminStub;
    private String backendServerURL;
    private String keyStoreLocation;
    private UserProfileMgtServiceStub userProfileMgtServiceStub;

//    public static void main(String[] args) {
//        BasicTest basicTest = new BasicTest();
//        basicTest.init();
//        try {
//            basicTest.CRDUser();
//        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (RemoteException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//
//    }

    @BeforeClass
    public void init() {
        backendServerURL = "https://localhost:9443/services/";
        keyStoreLocation = this.getClass().getResource("client-truststore.jks").getFile();
        initMultiCredsStub();
        initClaimMgtStub();
    }

    @Test
    public void CRDUser()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {
        try {
            multipleCredsAdminStub.deleteUser("Tharindu", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }
        Credential credential = new Credential();
        credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential.setIdentifier("Tharindu");
        credential.setSecret("admin123");
        // add user
        multipleCredsAdminStub.addUser(credential, null, null, null);

        // get user creds
        Credential[] credentials = multipleCredsAdminStub.getCredentials("Tharindu", CFConstants.DEFAULT_TYPE);
        for (Credential returnedCreds : credentials) {
            Assert.assertEquals(returnedCreds.getIdentifier(), credential.getIdentifier(),
                                "Returned creds not equal to actual creds");
            Assert.assertEquals(returnedCreds.getCredentialsType(), credential.getCredentialsType(),
                                "Returned creds not equal to actual creds");
        }

        // delete user
        multipleCredsAdminStub.deleteUser("Tharindu", CFConstants.DEFAULT_TYPE);
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedException = null;

        // get non-existing user creds
        try {
            multipleCredsAdminStub.getCredentials("Tharindu", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedException = e;
        } finally {
            assertNotNull(expectedException);
        }
    }

    @Test
    public void CRUDCredentials()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {
        try {
            multipleCredsAdminStub.deleteUser("Caressa", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }
        // add user with default creds
        Credential credential = new Credential();
        credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential.setIdentifier("Caressa");
        credential.setSecret("Very,very,secure password");
        multipleCredsAdminStub.addUser(credential, null, null, null);

        // add device creds
        Credential deviceCreds = new Credential();
        deviceCreds.setIdentifier("Aa:SSD:SDSA:SA:01");
        deviceCreds.setCredentialsType(CFConstants.DEVICE_TYPE);
        deviceCreds.setSecret("MAC_ID_PASSWORD");
        multipleCredsAdminStub.addCredential(credential.getIdentifier(), credential.getCredentialsType(), deviceCreds);

        // get creds with default
        Credential[] returnedCreds1 = multipleCredsAdminStub.getCredentials(credential.getIdentifier(), credential.getCredentialsType());
        assertTrue(returnedCreds1.length == 2, "returned creds does not contain both credentials");

        // get same creds with device
        Credential[] returnedCreds2 = multipleCredsAdminStub.getCredentials(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());

        // both should be equal to submitted creds
//        assertEquals(returnedCreds1, returnedCreds2, "Both creds not equal");
        for (Credential credential1 : returnedCreds1) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), credential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        for (Credential credential1 : returnedCreds2) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), credential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        // update default creds
        Credential updatedCredential = new Credential();
        updatedCredential.setIdentifier("Caressa2");
        updatedCredential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        updatedCredential.setSecret("JUNK_PW");
        multipleCredsAdminStub.updateCredential(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType(), updatedCredential);

        // use earlier creds - should not work
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedException = null;
        try {
            multipleCredsAdminStub.getCredentials(credential.getIdentifier(), credential.getCredentialsType());
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedException = e;
        } finally {
            assertNotNull(expectedException);
        }

        // get using new creds
        Credential[] updatedReturnedCreds1 = multipleCredsAdminStub.getCredentials(updatedCredential.getIdentifier(), updatedCredential.getCredentialsType());
        assertTrue(updatedReturnedCreds1.length == 2, "returned creds does not contain both credentials");

        // get using non - updated device creds
        Credential[] updatedReturnedCreds2 = multipleCredsAdminStub.getCredentials(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());

        // both should be equal
//        assertEquals(updatedReturnedCreds1, updatedReturnedCreds2, "Both creds not equal");
        for (Credential credential1 : updatedReturnedCreds2) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), updatedCredential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        for (Credential credential1 : updatedReturnedCreds1) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), updatedCredential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        // delete creds
        Credential nonExistentCredential = credential;

        // should throw exception
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedDeletingException = null;
        try {
            multipleCredsAdminStub.deleteCredential(nonExistentCredential.getIdentifier(), nonExistentCredential.getCredentialsType());
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedDeletingException = e;
        } finally {
            assertNotNull(expectedDeletingException);
        }

        // delete inserted creds
        multipleCredsAdminStub.deleteCredential(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());
        // get using new creds
        Credential[] returnedCredsAfterDelete = multipleCredsAdminStub.getCredentials(updatedCredential.getIdentifier(), updatedCredential.getCredentialsType());
        assertTrue(returnedCredsAfterDelete.length == 1, "returned creds should not contain deleted creds");

        // returned creds should be equal to the updated creds, i.e. the only creds left
        assertEquals(returnedCredsAfterDelete[0].getCredentialsType(), updatedCredential.getCredentialsType());
        assertEquals(returnedCredsAfterDelete[0].getIdentifier(), updatedCredential.getIdentifier());

        //  check if it's possible to get creds from the deleted creds
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedExceptionWhenGettingCredsAfterDeletion = null;
        try {
            multipleCredsAdminStub.getCredentials(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedExceptionWhenGettingCredsAfterDeletion = e;
        } finally {
            assertNotNull(expectedExceptionWhenGettingCredsAfterDeletion);
        }

        // check if it's possible to update deleted creds
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedExceptionWhenUpdatingCredsAfterDeletion = null;
        try {
            multipleCredsAdminStub.updateCredential(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType(), deviceCreds);
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedExceptionWhenUpdatingCredsAfterDeletion = e;
        } finally {
            assertNotNull(expectedExceptionWhenUpdatingCredsAfterDeletion);
        }
    }

    @Test
    public void authenticateWithAnyUser()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {
        // delete off user to init
        try {
            multipleCredsAdminStub.deleteUser("tharindu@wso2.com", "Email");
        } catch (RemoteException e) {
            // ignore

        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }

        Credential emailCred1 = new Credential();
        emailCred1.setIdentifier("tharindu@wso2.com");
        emailCred1.setSecret("VerySecurePassword");
        emailCred1.setCredentialsType("Email");

        multipleCredsAdminStub.addUser(emailCred1, null, null, null);
        // normal auth
        assertTrue(multipleCredsAdminStub.authenticate(emailCred1));

        Credential defaultCred2 = new Credential();
        defaultCred2.setIdentifier("tharindu");
        defaultCred2.setSecret("defaultpassword");
        defaultCred2.setCredentialsType("Default");

        multipleCredsAdminStub.addCredential(emailCred1.getIdentifier(), emailCred1.getCredentialsType(), defaultCred2);

        Credential deviceCred = new Credential();
        deviceCred.setIdentifier("AA_SS_SS_WA");
        deviceCred.setCredentialsType(CFConstants.DEVICE_TYPE);

        //normal auth
        assertTrue(multipleCredsAdminStub.authenticate(defaultCred2));

        // check for authenticate with any credential
        defaultCred2.setSecret(emailCred1.getSecret());
        assertTrue(multipleCredsAdminStub.authenticate(defaultCred2));

    }


    @Test
    public void CRUDclaims()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {

        // clean up users
        try {
            multipleCredsAdminStub.deleteUser("Caressa", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }
        try {
            multipleCredsAdminStub.deleteUser("Tharindu", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }

        // add user with default creds
        Credential credential = new Credential();
        credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential.setIdentifier("Caressa");
        credential.setSecret("Very,very,secure password");

        ClaimValue claimValue1 = new ClaimValue();
        String emailClaimURI = "http://wso2.org/email";
        claimValue1.setClaimURI(emailClaimURI);
        String emailClaimVal = "foo@bar.com";
        claimValue1.setValue(emailClaimVal);

        ClaimValue claimValue2 = new ClaimValue();
        String countryClaimURI = "http://wso2.org/country";
        claimValue2.setClaimURI(countryClaimURI);
        String countryClaimVal = "Sri Lanka";
        claimValue2.setValue(countryClaimVal);

        ClaimValue claimValue3 = new ClaimValue();
        String phoneClaimURI = "http://wso2.org/phome";
        claimValue3.setClaimURI(phoneClaimURI);
        String phoneClaimVal = "12334455";
        claimValue3.setValue(phoneClaimVal);

        ClaimValue[] claimValues = { claimValue1, claimValue2, claimValue3 };

        // add user with claims
        multipleCredsAdminStub.addUser(credential, null, claimValues, null);

        // retreive single claim and check equality
        String actualEmailClaimValue = multipleCredsAdminStub
                .getUserClaimValue(credential.getIdentifier(),
                                   credential.getCredentialsType(), emailClaimURI, null);
        assertEquals(actualEmailClaimValue, emailClaimVal);

        // retrieve 2 claims and check
        ClaimValue[] retrievedUserClaimVals = multipleCredsAdminStub
                .getUserClaimValues(credential.getIdentifier(),
                                    credential.getCredentialsType(),
                                    new String[]{countryClaimURI, phoneClaimURI}, null);
        assertTrue(retrievedUserClaimVals.length == 2);

        if (retrievedUserClaimVals[0].getClaimURI().equals(claimValue2.getClaimURI())) {
            assertEquals(retrievedUserClaimVals[0].getValue(), claimValue2.getValue());
            assertEquals(retrievedUserClaimVals[1].getValue(), claimValue3.getValue());
            assertEquals(retrievedUserClaimVals[1].getClaimURI(), claimValue3.getClaimURI());
        } else {
            assertEquals(retrievedUserClaimVals[0].getValue(), claimValue3.getValue());
            assertEquals(retrievedUserClaimVals[0].getClaimURI(), claimValue3.getClaimURI());
            assertEquals(retrievedUserClaimVals[1].getValue(), claimValue2.getValue());
            assertEquals(retrievedUserClaimVals[1].getClaimURI(), claimValue2.getClaimURI());
        }

        // retrieve all claims and check
        ClaimValue[] allUserClaimValues = multipleCredsAdminStub.getAllUserClaimValues(credential.getIdentifier(),
                                                                                       credential.getCredentialsType(), null);
        assertTrue(allUserClaimValues.length == 3);

        // delete single claim value
        multipleCredsAdminStub.deleteUserClaimValue(credential.getIdentifier(),
                                                    credential.getCredentialsType(), claimValue1.getClaimURI(), null);

        // retrieve and check after deletion
        ClaimValue[] retrievedAfterDeletionClaims = multipleCredsAdminStub.getAllUserClaimValues(credential.getIdentifier(),
                                                                                       credential.getCredentialsType(), null);

        assertTrue(retrievedAfterDeletionClaims.length == 2);

        if (retrievedAfterDeletionClaims[0].getClaimURI().equals(claimValue2.getClaimURI())) {
            assertEquals(retrievedAfterDeletionClaims[0].getValue(), claimValue2.getValue());
            assertEquals(retrievedAfterDeletionClaims[1].getValue(), claimValue3.getValue());
            assertEquals(retrievedAfterDeletionClaims[1].getClaimURI(), claimValue3.getClaimURI());
        } else {
            assertEquals(retrievedAfterDeletionClaims[0].getValue(), claimValue3.getValue());
            assertEquals(retrievedAfterDeletionClaims[0].getClaimURI(), claimValue3.getClaimURI());
            assertEquals(retrievedAfterDeletionClaims[1].getValue(), claimValue2.getValue());
            assertEquals(retrievedAfterDeletionClaims[1].getClaimURI(), claimValue2.getClaimURI());
        }

        // delete all claims
        multipleCredsAdminStub.deleteUserClaimValues(credential.getIdentifier(),
                                                     credential.getCredentialsType(),
                                                     new String[]{claimValue2.getClaimURI(),
                                                                  claimValue3.getClaimURI()}, null);

        // set claim values
        ClaimValue claimValue4 = new ClaimValue();
        String cityClaimURI = "http://wso2.org/city";
        claimValue4.setClaimURI(cityClaimURI);
        String claimCityVal = "Colombo";
        claimValue4.setValue(claimCityVal);

        multipleCredsAdminStub.setUserClaimValue(credential.getIdentifier(),
                                                 credential.getCredentialsType(),
                                                 cityClaimURI, claimCityVal, null);

        String actualCityClaimVal = multipleCredsAdminStub.getUserClaimValue(credential.getIdentifier(),
                                                                         credential.getCredentialsType(), cityClaimURI, null);

        assertEquals(actualCityClaimVal, claimCityVal);

        // add different user with claims
        Credential credential1 = new Credential();
        credential1.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential1.setIdentifier("Tharindu");
        credential1.setSecret("secure password");
        multipleCredsAdminStub.addUser(credential1, null, null, null);

        multipleCredsAdminStub.setUserClaimValues(credential1.getIdentifier(),
                                                  credential1.getCredentialsType(), claimValues, null);
        multipleCredsAdminStub.deleteUserClaimValue(credential1.getIdentifier(),
                                                    credential1.getCredentialsType(),
                                                    claimValue1.getClaimURI(),
                                                    null);

        // retrieve 2 claims and check
        ClaimValue[] retrievedDifferentUserClaimVals = multipleCredsAdminStub
                .getUserClaimValues(credential1.getIdentifier(),
                                    credential1.getCredentialsType(),
                                    new String[]{countryClaimURI, phoneClaimURI}, null);
        assertTrue(retrievedDifferentUserClaimVals.length == 2);

        if (retrievedDifferentUserClaimVals[0].getClaimURI().equals(claimValue2.getClaimURI())) {
            assertEquals(retrievedDifferentUserClaimVals[0].getValue(), claimValue2.getValue());
            assertEquals(retrievedDifferentUserClaimVals[1].getValue(), claimValue3.getValue());
            assertEquals(retrievedDifferentUserClaimVals[1].getClaimURI(), claimValue3.getClaimURI());
        } else {
            assertEquals(retrievedDifferentUserClaimVals[0].getValue(), claimValue3.getValue());
            assertEquals(retrievedDifferentUserClaimVals[0].getClaimURI(), claimValue3.getClaimURI());
            assertEquals(retrievedDifferentUserClaimVals[1].getValue(), claimValue2.getValue());
            assertEquals(retrievedDifferentUserClaimVals[1].getClaimURI(), claimValue2.getClaimURI());
        }
    }

    @Test
    public void AddUserId()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {
        try {
            multipleCredsAdminStub.deleteUser("Tharindu", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }
        Credential credential = new Credential();
        credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential.setIdentifier("Tharindu");
        credential.setSecret("admin123");
        // add user
        String userId = UUID.randomUUID().toString();
        multipleCredsAdminStub.addUserWithUserId(userId, credential, null, null, null);

        String returnedUserId = multipleCredsAdminStub.getUserId(credential);

        assertEquals(returnedUserId, userId, "user id is not equal");
    }


    private void initClaimMgtStub() {
        String keyStoreLoc = keyStoreLocation;
        System.setProperty("javax.net.ssl.trustStore", keyStoreLoc);
        System.setProperty("javax.net.ssl.keyStore", keyStoreLoc);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");

        /**
         * Axis2 configuration context
         */
        ConfigurationContext configContext;

        try {

            /**
             * Create a configuration context. A configuration context contains
             * information for
             * axis2 environment. This is needed to create an axis2 client
             */
            configContext =
                    ConfigurationContextFactory.createConfigurationContextFromFileSystem(null,
                                                                                         null);

            String serviceEndPoint = backendServerURL + "UserProfileMgtService";

            userProfileMgtServiceStub = new UserProfileMgtServiceStub(configContext, serviceEndPoint);
            ServiceClient client = userProfileMgtServiceStub._getServiceClient();
            Options option = client.getOptions();

            /**
             * setting basic auth headers for authentication for user admin
             */
            HttpTransportProperties.Authenticator auth =
                    new HttpTransportProperties.Authenticator();
            auth.setUsername("multipleCredentialUserStoreDomain/admin");
            auth.setPassword("admin");
            auth.setPreemptiveAuthentication(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
            option.setManageSession(true);
            option.setTimeOutInMilliSeconds(10 * 60 * 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void initMultiCredsStub() {

        String keyStoreLoc = keyStoreLocation;
        System.setProperty("javax.net.ssl.trustStore", keyStoreLoc);
        System.setProperty("javax.net.ssl.keyStore", keyStoreLoc);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");

        /**
         * Axis2 configuration context
         */
        ConfigurationContext configContext;

        try {

            /**
             * Create a configuration context. A configuration context contains
             * information for
             * axis2 environment. This is needed to create an axis2 client
             */
            configContext =
                    ConfigurationContextFactory.createConfigurationContextFromFileSystem(null,
                                                                                         null);

            String serviceEndPoint = backendServerURL + "MultipleCredentialsUserAdmin";

            multipleCredsAdminStub = new MultipleCredentialsUserAdminStub(configContext, serviceEndPoint);
            ServiceClient client = multipleCredsAdminStub._getServiceClient();
            Options option = client.getOptions();

            /**
             * setting basic auth headers for authentication for user admin
             */
            HttpTransportProperties.Authenticator auth =
                    new HttpTransportProperties.Authenticator();
            auth.setUsername("admin");
            auth.setPassword("admin");
            auth.setPreemptiveAuthentication(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
            option.setManageSession(true);
            option.setTimeOutInMilliSeconds(10 * 60 * 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
