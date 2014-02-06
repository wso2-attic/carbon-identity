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
package org.wso2.carbon.identity.application.mgt.dto;

public class TrustedIDPConfigDTO {

	private String idpIdentifier = null;
	private String[] types = null;
	private String[] endpoints = null;

	public String getIdpIdentifier() {
		return idpIdentifier;
	}

	public void setIdpIdentifier(String idpIdentifier) {
		this.idpIdentifier = idpIdentifier;
	}

	public String[] getTypes() {
		return types;
	}

	public void setTypes(String[] types) {
		this.types = types;
	}

	public String[] getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(String[] endpoints) {
		this.endpoints = endpoints;
	}

	public String getTypesString() {
		if (types != null && types.length > 0) {
			StringBuffer buff = new StringBuffer();
			for (String type : types) {
				buff.append(type + ",");
			}
			return buff.toString();
		}
		return null;
	}
	
	public void setTypesString(String typestring) {
		if(typestring != null) {
			types = typestring.split(",");
		}
	}
	
	public String getEndpointsString() {
		if (endpoints != null && endpoints.length > 0) {
			StringBuffer buff = new StringBuffer();
			for (String type : types) {
				buff.append(type + ",");
			}
			return buff.toString();
		}
		return null;
	}
	
	public void setEndpointsString(String endpointString) {
		if(endpointString != null) {
			endpoints = endpointString.split(",");
		}
	}

}
