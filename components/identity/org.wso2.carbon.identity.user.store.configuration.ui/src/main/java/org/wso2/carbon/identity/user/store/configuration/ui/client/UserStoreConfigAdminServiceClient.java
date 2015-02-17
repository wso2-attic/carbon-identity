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
package org.wso2.carbon.identity.user.store.configuration.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.identity.user.store.configuration.stub.UserStoreConfigAdminServiceDataSourceException;
import org.wso2.carbon.identity.user.store.configuration.stub.UserStoreConfigAdminServiceStub;
import org.wso2.carbon.identity.user.store.configuration.stub.api.Properties;
import org.wso2.carbon.identity.user.store.configuration.stub.dto.PropertyDTO;
import org.wso2.carbon.identity.user.store.configuration.stub.dto.UserStoreDTO;
import org.wso2.carbon.ndatasource.common.DataSourceException;

import java.util.HashMap;
import java.util.Map;

public class UserStoreConfigAdminServiceClient {
    private UserStoreConfigAdminServiceStub stub;

    /**
     * Constructor UserStoreConfigAdminServiceClient
     *
     * @param cookie
     * @param backendServerURL
     * @param configContext
     */
    public UserStoreConfigAdminServiceClient(String cookie, String backendServerURL,
                                             ConfigurationContext configContext) throws AxisFault {
        String serviceURL = backendServerURL + "UserStoreConfigAdminService";
        stub = new UserStoreConfigAdminServiceStub(configContext, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * Get all the configured domains
     *
     * @throws Exception
     * @return: active domains
     */
    public UserStoreDTO[] getActiveDomains() throws Exception {
        return stub.getSecondaryRealmConfigurations();
    }

    /**
     * Get available user store implementations
     *
     * @return : available user store managers
     * @throws Exception
     */
    public String[] getAvailableUserStoreClasses() throws Exception {
        return stub.getAvailableUserStoreClasses();

    }

    /**
     * Get properties required for the given user store
     *
     * @param className : list of properties required by each user store manager
     * @return : list of properties(mandatory+optional)
     * @throws Exception
     */
    public Properties getUserStoreProperties(String className) throws Exception {
        return stub.getUserStoreManagerProperties(className);

    }

    /**
     * Get user store manager properties from secondary realm configurations
     *
     * @return Map of properties
     * @throws Exception
     */
    public Map<String, String> getUserStoreManagerPropertiesFromSecondaryRealmConfigurations(String domain) throws Exception {
        Map<String, String> propertyMap = new HashMap<String, String>();
        UserStoreDTO[] userStoreDTOs = getActiveDomains();
        for(UserStoreDTO userStoreDTO: userStoreDTOs){
            if(userStoreDTO.getDomainId().equals(domain)){
                PropertyDTO[] propertyDTOs = userStoreDTO.getProperties();
                for (PropertyDTO propertyDTO : propertyDTOs) {
                    propertyMap.put(propertyDTO.getName(), propertyDTO.getValue());
                }
                break;
            }
        }

        return propertyMap;
    }

    /**
     * Save configuration to file system
     *
     * @param userStoreDTO : representation of new user store to be persisted
     * @throws Exception
     */
    public void addUserStore(UserStoreDTO userStoreDTO) throws DataSourceException, Exception {

        try {
            stub.addUserStore(userStoreDTO);
        } catch (UserStoreConfigAdminServiceDataSourceException e) {
            if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
                throw new DataSourceException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
            }
            throw new DataSourceException(e.getMessage(), e);
        } catch (AxisFault e) {
            throw new DataSourceException(e.getMessage(), e);
        }
    }

    /**
     * Deletes a given list of user stores
     *
     * @param userStores : domain names of user stores to deleted
     * @throws Exception
     */
    public void deleteUserStoresSet(String[] userStores) throws Exception {
        stub.deleteUserStoresSet(userStores);
    }

    /**
     * Deletes a given user store
     *
     * @param userStore : domain name of the user store to deleted
     * @throws Exception
     */
    public void deleteUserStore(String userStore) throws Exception {
        stub.deleteUserStore(userStore);
    }

    /**
     * Toggle user store state (enable/disable)
     *
     * @param domain     : domain name of the user store to enable/dissable
     * @param isDisabled : set true to disable user store
     * @throws Exception
     */
    public void changeUserStoreState(String domain, String isDisabled) throws Exception {
        stub.changeUserStoreState(domain, Boolean.parseBoolean(isDisabled));
    }

    /**
     * Rename user store including property changes
     *
     * @param previousDomain Previous domain name of the user store
     * @param userStoreDTO   New properties of the user store
     * @throws Exception
     */
    public void updateUserStoreWithDomainName(String previousDomain, UserStoreDTO userStoreDTO) throws Exception {
        if (previousDomain != null && !"".equals(previousDomain) && !previousDomain.equalsIgnoreCase(userStoreDTO.getDomainId())) {
            try {
                stub.editUserStoreWithDomainName(previousDomain, userStoreDTO);
            } catch (UserStoreConfigAdminServiceDataSourceException e) {
                if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
                    throw new DataSourceException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
                }
                throw new DataSourceException(e.getMessage(), e);
            } catch (AxisFault e) {
                throw new DataSourceException(e.getMessage(), e);
            }
        } else {
            this.updateUserStore(userStoreDTO);
        }
    }

    /**
     * Update user store without changing the domain name
     *
     * @param userStoreDTO New properties of the user store
     * @throws Exception
     */
    public void updateUserStore(UserStoreDTO userStoreDTO) throws DataSourceException, Exception {

        try {
            stub.editUserStore(userStoreDTO);
        } catch (UserStoreConfigAdminServiceDataSourceException e) {
            if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
                throw new DataSourceException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
            }
            throw new DataSourceException(e.getMessage(), e);
        } catch (AxisFault e) {
            throw new DataSourceException(e.getMessage(), e);
        }
    }

    public boolean testRDBMSConnection(String domainName, String driverName, String connectionURL, String username, String connectionPassword) throws Exception {
        try {
            return stub.testRDBMSConnection(domainName, driverName, connectionURL, username, connectionPassword);
        } catch (UserStoreConfigAdminServiceDataSourceException e) {
            if (e.getFaultMessage().getDataSourceException().isErrorMessageSpecified()) {
                throw new DataSourceException(e.getFaultMessage().getDataSourceException().getErrorMessage(), e);
            }
            throw new DataSourceException(e.getMessage(), e);
        }
    }
}
