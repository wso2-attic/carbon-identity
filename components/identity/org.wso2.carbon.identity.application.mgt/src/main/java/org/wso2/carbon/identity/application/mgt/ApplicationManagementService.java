/*
 *Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.carbon.identity.application.mgt;

import org.wso2.carbon.identity.application.mgt.dao.ApplicationConfigDAO;
import org.wso2.carbon.identity.application.mgt.dao.TrustedIDPConfigDAO;
import org.wso2.carbon.identity.application.mgt.dto.ApplicationConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.TrustedIDPConfigDTO;
import org.wso2.carbon.identity.base.IdentityException;

public class ApplicationManagementService {

	public void storeApplicationData(ApplicationConfigDTO appConfiDto) throws IdentityException {
		ApplicationConfigDAO configDAO = new ApplicationConfigDAO();
		configDAO.storeApplicationData(appConfiDto);
	}

	public ApplicationConfigDTO getApplicationData(String clientId, String type) throws IdentityException {
		ApplicationConfigDAO configDAO = new ApplicationConfigDAO();
		return configDAO.getApplicationData(clientId, type);
	}

    public ApplicationConfigDTO getApplicationDataFromID(String appID) throws IdentityException {
        ApplicationConfigDAO configDAO = new ApplicationConfigDAO();
        return configDAO.getApplicationDataFromID(appID);
    }

	public ApplicationConfigDTO[] getAllApplicationData() throws IdentityException {
		ApplicationConfigDAO configDAO = new ApplicationConfigDAO();
		return configDAO.getAllApplicationData();
	}
	
	public ApplicationConfigDTO updateApplication(ApplicationConfigDTO appConfiDto) throws IdentityException {
		ApplicationConfigDAO configDAO = new ApplicationConfigDAO();
		return configDAO.updateApplicationData(appConfiDto);
	}

    public void deleteApplication(String appID) throws IdentityException {
        ApplicationConfigDAO configDAO = new ApplicationConfigDAO();
        configDAO.deleteApplication(appID, false);
    }

	public TrustedIDPConfigDTO getIDPData(String idpID) throws IdentityException {
		TrustedIDPConfigDAO idpdao = new TrustedIDPConfigDAO();
		return idpdao.getIDPData(idpID);
	}

	public TrustedIDPConfigDTO[] getAllIDPData() throws IdentityException {
		TrustedIDPConfigDAO idpdao = new TrustedIDPConfigDAO();
		return idpdao.getAllIDPData();
	}
	
    
}
