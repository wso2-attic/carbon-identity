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
package org.wso2.carbon.identity.application.mgt.dao;

import org.wso2.carbon.identity.application.mgt.dto.TrustedIDPConfigDTO;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.idp.mgt.IdentityProviderMgtService;
import org.wso2.carbon.idp.mgt.dto.TrustedIdPDTO;
import org.wso2.carbon.idp.mgt.exception.IdentityProviderMgtException;

public class TrustedIDPConfigDAO {

	public TrustedIDPConfigDTO getIDPData(String idpID) throws IdentityException {
		IdentityProviderMgtService idpmgtService = new IdentityProviderMgtService();
		try {
			TrustedIdPDTO idp = idpmgtService.getIdPByName(idpID);

			TrustedIDPConfigDTO dto = new TrustedIDPConfigDTO();
			//dto.setEndpointsString(idp.getIdPUrl());
			dto.setIdpIdentifier(idp.getIdPName());
			dto.setTypesString("DEFAULT");

			return dto;

		} catch (IdentityProviderMgtException e) {
			throw new IdentityException("Error while reading IDP info", e);
		}
	}

	public TrustedIDPConfigDTO[] getAllIDPData() throws IdentityException {
		IdentityProviderMgtService idpmgtService = new IdentityProviderMgtService();
		try {

			TrustedIdPDTO[] idps = idpmgtService.getIdPs();
			if (idps != null) {
				TrustedIDPConfigDTO[] idpDTOs = new TrustedIDPConfigDTO[idps.length];
				int i = 0;
				for (TrustedIdPDTO idp : idps) {
					idpDTOs[i] = getIDPData(idp.getIdPName());
					i++;
				}
				return idpDTOs;
			}
		} catch (IdentityProviderMgtException e) {
			throw new IdentityException("Error while reading IDP info", e);
		}
		return null;
	}

}
