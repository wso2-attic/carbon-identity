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

package org.wso2.carbon.identity.application.mgt.ui.client;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.mgt.dto.xsd.ApplicationConfigDTO;
import org.wso2.carbon.identity.application.mgt.dto.xsd.TrustedIDPConfigDTO;
import org.wso2.carbon.identity.application.mgt.stub.ApplicationManagementServiceIdentityException;
import org.wso2.carbon.identity.application.mgt.stub.ApplicationManagementServiceStub;
import org.wso2.carbon.identity.application.mgt.ui.ApplicationConfigBean;
import org.wso2.carbon.identity.application.mgt.ui.TrustedIDPConfig;

public class ApplicationManagementServiceClient {

	ApplicationManagementServiceStub stub = null;

	Log log = LogFactory.getLog(ApplicationManagementServiceClient.class);

	/**
	 * Instantiates OAuthAdminClient
	 * 
	 * @param cookie
	 *            For session management
	 * @param backendServerURL
	 *            URL of the back end server where OAuthAdminService is running.
	 * @param configCtx
	 *            ConfigurationContext
	 * @throws org.apache.axis2.AxisFault
	 */
	public ApplicationManagementServiceClient(String cookie, String backendServerURL,
			ConfigurationContext configCtx) throws AxisFault {
		String serviceURL = backendServerURL + "ApplicationManagementService";
		stub = new ApplicationManagementServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
	}

	public void storeApplicationData(ApplicationConfigBean bean) throws Exception {

		ApplicationConfigDTO appConfigDTO = ClientUtil.getApplicationConfigDTO(bean);
		try {
			stub.storeApplicationData(appConfigDTO);
		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

	}

	public ApplicationConfigBean getApplicationData(String applicationID) throws Exception {
		try {
			ApplicationConfigDTO applicationDTO = stub.getApplicationDataFromID(applicationID);
			if (applicationDTO != null) {
				return ClientUtil.getApplicationConfigBean(applicationDTO);
			}
			return null;
		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}

	public ApplicationConfigBean[] getAllApplicationData() throws Exception {
		ApplicationConfigBean[] appConfigBean = null;
		try {
			ApplicationConfigDTO[] allAppData = stub.getAllApplicationData();
			if (allAppData != null && allAppData.length > 0) {
				appConfigBean = new ApplicationConfigBean[allAppData.length];
				int i = 0;
				for (ApplicationConfigDTO dto : allAppData) {
					appConfigBean[i++] = ClientUtil.getApplicationConfigBean(dto);
				}
			}
			return appConfigBean;

		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}

	public ApplicationConfigBean updateApplicationData(ApplicationConfigBean bean) throws Exception {
		ApplicationConfigDTO appConfigDTO = ClientUtil.getApplicationConfigDTO(bean);
		try {
			ApplicationConfigDTO applicationDTO = stub.updateApplication(appConfigDTO);
			return ClientUtil.getApplicationConfigBean(applicationDTO);
		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}
	}

	public void deleteApplication(String applicationID) throws Exception {
		try {
			stub.deleteApplication(applicationID);
		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

	}

	public String[] getIDPList() throws Exception {
		TrustedIDPConfigDTO[] idps = null;
		try {
			idps = stub.getAllIDPData();
		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

		if (idps != null) {
			String[] idpList = new String[idps.length];
			int i = 0;
			for (TrustedIDPConfigDTO dto : idps) {
				idpList[i] = dto.getIdpIdentifier();
				i++;
			}
			return idpList;
		}
		return null;
	}

	public TrustedIDPConfig getIDPData(String idpID) throws Exception {
		TrustedIDPConfigDTO idpconf = null;
		try {
			idpconf = stub.getIDPData(idpID);
		} catch (RemoteException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		} catch (ApplicationManagementServiceIdentityException e) {
			log.error(e.getMessage(), e);
			throw new Exception(e.getMessage());
		}

		TrustedIDPConfig idpconfig = new TrustedIDPConfig();

		if (idpconf != null) {
			idpconfig.setIdpName(idpconf.getIdpIdentifier());
			idpconfig.setEndpointUrl(idpconf.getEndpointsString());
			idpconfig.setProtocols(idpconf.getTypes());
		}
		return idpconfig;
	}

}
