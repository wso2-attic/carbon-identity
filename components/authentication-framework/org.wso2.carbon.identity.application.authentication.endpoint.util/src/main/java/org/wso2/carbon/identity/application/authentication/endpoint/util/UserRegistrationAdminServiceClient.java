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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.endpoint.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.identity.user.registration.stub.UserRegistrationAdminServiceException;
import org.wso2.carbon.identity.user.registration.stub.UserRegistrationAdminServiceStub;
import org.wso2.carbon.identity.user.registration.stub.dto.UserDTO;
import org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;

public class UserRegistrationAdminServiceClient {

    private UserRegistrationAdminServiceStub stub;
    private Properties prop;
    private static final String USER_REGISTRATION_SERVICE = "/services/UserRegistrationAdminService" +
            ".UserRegistrationAdminServiceHttpsSoap11Endpoint/";

    public UserRegistrationAdminServiceClient() throws AxisFault {

        StringBuilder builder = new StringBuilder();
        String serviceURL = builder.append(Constants.HTTPS_URL).append(TenantDataManager.getPropertyValue(Constants
                .HOST)).append(Constants.COLON) .append(TenantDataManager.getPropertyValue(Constants.PORT)).append
                (USER_REGISTRATION_SERVICE).toString();
        stub = new UserRegistrationAdminServiceStub(serviceURL);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
    }

    public void addUser (Map<String,String> registrationProperties) throws RemoteException, UserRegistrationAdminServiceException {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserName(registrationProperties.get("reg-username"));
        userDTO.setPassword(registrationProperties.get("reg-password"));
        UserFieldDTO[] claims = new UserFieldDTO[3];
        int i = 0;
        for(Map.Entry<String, String> claim: registrationProperties.entrySet()) {
            UserFieldDTO userFieldDTO = new UserFieldDTO();
            if (claim.getKey().equals("reg-first-name")) {
                userFieldDTO.setClaimUri("http://wso2.org/claims/givenname");
                userFieldDTO.setFieldName("First Name");
                userFieldDTO.setFieldValue(claim.getValue());
                claims[i] = userFieldDTO;
                i++;
            } else if (claim.getKey().equals("reg-last-name")) {
                userFieldDTO.setClaimUri("http://wso2.org/claims/lastname");
                userFieldDTO.setFieldName("Last Name");
                userFieldDTO.setFieldValue(claim.getValue());
                claims[i] = userFieldDTO;
                i++;
            } else if (claim.getKey().equals("reg-email")) {
                userFieldDTO.setClaimUri("http://wso2.org/claims/emailaddress");
                userFieldDTO.setFieldName("Email Address");
                userFieldDTO.setFieldValue(claim.getValue());
                claims[i] = userFieldDTO;
                i++;
            }
        }
        userDTO.setUserFields(claims);
        stub.addUser(userDTO);
    }

}
