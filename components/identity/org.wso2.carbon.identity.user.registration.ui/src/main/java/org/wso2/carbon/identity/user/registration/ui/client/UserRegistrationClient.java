/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.user.registration.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.user.registration.stub.UserRegistrationAdminServiceIdentityException;
import org.wso2.carbon.identity.user.registration.stub.UserRegistrationAdminServiceStub;
import org.wso2.carbon.identity.user.registration.stub.dto.PasswordRegExDTO;
import org.wso2.carbon.identity.user.registration.stub.dto.UserDTO;
import org.wso2.carbon.identity.user.registration.stub.dto.UserFieldDTO;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class UserRegistrationClient {

    private UserRegistrationAdminServiceStub stub;
    private static final Log log = LogFactory.getLog(UserRegistrationClient.class);

    public static String USER_REGISTRATION_CLIENT_NAME =
            "org.wso2.carbon.identity.user.registration.ui.client.UserRegistrationClient";

    /**
     * Instantiates UserRegistrationServiceClient
     * 
     * @param cookie For session management
     * @param backendServerURL URL of the back end server where UserRegistrationAdminService is
     *        running.
     * @param configCtx ConfigurationContext
     * @throws org.apache.axis2.AxisFault
     */
    public UserRegistrationClient(String cookie, String backendServerURL,
            ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "UserRegistrationAdminService";
        stub = new UserRegistrationAdminServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * 
     * @return
     * @throws AxisFault
     */
    public String getRequiredFieldsForRegistration() throws AxisFault {
        UserFieldDTO[] userFields = null;
        StringBuffer buffer = null;
        try {
            buffer = new StringBuffer();
            userFields = stub.readUserFieldsForUserRegistration(IdentityConstants.INFOCARD_DIALECT);
            buffer.append(IdentityConstants.CLAIM_PPID + " ");
            if (userFields != null) {
                for (int i = 0; i < userFields.length; i++) {
                    if (userFields[i].getRequired()) {
                        buffer.append(userFields[i].getClaimUri() + " ");
                    }
                }
                return buffer.toString();
            }
        } catch (Exception e) {
            handleException(
                    "Error ouccured while reading user registration configurations from the backend service",
                    e);
        }
        return "";
    }

    /**
     * 
     * @return
     * @throws AxisFault
     */
    public String getOptionalFieldsForRegistration() throws AxisFault {
        UserFieldDTO[] userFields = null;
        StringBuffer buffer = null;
        try {
            buffer = new StringBuffer();
            userFields = stub.readUserFieldsForUserRegistration(IdentityConstants.INFOCARD_DIALECT);
            if (userFields != null) {
                for (int i = 0; i < userFields.length; i++) {
                    if (!userFields[i].getRequired()) {
                        buffer.append(userFields[i].getClaimUri() + " ");
                    }
                }
                return buffer.toString();
            }
        } catch (Exception e) {
            handleException(
                    "Error ouccured while reading user registration configurations from the backend service",
                    e);
        }
        return IdentityConstants.CLAIM_PPID + " ";
    }

    /**
     * @param user UserDTO to be added to User Store
     * @throws AxisFault
     */
    public void addUser(UserDTO user) throws AxisFault {
        try {
            stub.addUser(user);
        } catch (Exception e) {
            handleException("Error ouccured while adding a user at the backend service", e);
        }

    }
    
    /**
     * 
     * @return
     * @throws AxisFault
     */
	public PasswordRegExDTO[] getPasswordRegularExpressions() throws AxisFault {
		try {
			return stub.getPasswordRegularExpressions();
		} catch (Exception e) {
			handleException(
					"Error ouccured while adding a user at the backend service",
					e);
		}
		return null;
	}


    /**
     * @return
     * @throws AxisFault
     */
    public boolean isAddUserEnabled() throws AxisFault {
        try {
            return stub.isAddUserEnabled();
        } catch (Exception e) {
            handleException(
                    "Error ouccured while reading user registration support from the backend service",
                    e);
        }
        return false;
    }

    /**
     * @return
     * @throws AxisFault
     */
    public boolean isAddUserWithOpenIDEnabled() throws AxisFault {
        try {
            return stub.isAddUserWithInfoCardEnabled();
        } catch (Exception e) {
            handleException(
                    "Error ouccured while reading inforcard user registration support from the backend service",
                    e);
        }
        return false;
    }

    /**
     * @return
     * @throws AxisFault
     */
    public boolean isAddUserWithInfoCardEnabled() throws AxisFault {
        try {
            return stub.isAddUserWithOpenIDEnabled();
        } catch (Exception e) {
            handleException(
                    "Error ouccured while reading openid user registration support from the backend service",
                    e);
        }
        return false;
    }

    /**
     * Logs and wraps the given exception.
     * 
     * @param msg Error message
     * @param e Exception
     * @throws AxisFault
     */
    private void handleException(String msg, Exception e) throws AxisFault {
       // log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    /**
     * Returns the UserFieldDTO[] of the given dialect.
     * 
     * @param dialect
     * @return
     * @throws AxisFault
     */
    public UserFieldDTO[] readUserFieldsForUserRegistration(String dialect) throws AxisFault {
        try {
            return stub.readUserFieldsForUserRegistration(dialect);
        } catch (Exception e) {
            handleException("Error retrieving UserFiledDTOs for the dialect" + dialect, e);
        }

        return null;
    }

    public UserFieldDTO[] getOrderedUserFields(UserFieldDTO[] userFields) throws Exception {
        Arrays.sort(userFields, new UserFieldComparator());
        return userFields;
    }

    class UserFieldComparator implements Comparator<UserFieldDTO> {

        public int compare(UserFieldDTO filed1, UserFieldDTO filed2) {
            if (filed1.getDisplayOrder() == 0) {
                filed1.setDisplayOrder(Integer.MAX_VALUE);
            }

            if (filed2.getDisplayOrder() == 0) {
                filed2.setDisplayOrder(Integer.MAX_VALUE);
            }

            if (filed1.getDisplayOrder() < filed2.getDisplayOrder()) {
                return -1;
            }
            if (filed1.getDisplayOrder() == filed2.getDisplayOrder()) {
                return 0;
            }
            if (filed1.getDisplayOrder() > filed2.getDisplayOrder()) {
                return 1;
            }
            return 0;
        }

    }

}
