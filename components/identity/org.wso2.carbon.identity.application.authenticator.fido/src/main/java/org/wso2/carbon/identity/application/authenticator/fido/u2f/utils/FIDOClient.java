/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
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

package org.wso2.carbon.identity.application.authenticator.fido.u2f.utils;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.identity.fido.u2f.service.stub.FIDOServiceStub;

import java.rmi.RemoteException;

/**
 * Created by ananthaneshan on 12/15/14.
 */
public class FIDOClient {

	private FIDOServiceStub stub;

	public FIDOClient(ConfigurationContext configCtx, String backendServerURL, String cookie) throws Exception{
		String serviceURL = backendServerURL + "FIDOService";
		stub = new FIDOServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options options = client.getOptions();
		options.setManageSession(true);
		options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

	}

	public FIDOClient() throws Exception{
		this.stub = new FIDOServiceStub();
	}

	/*public String startAuthentication(String username, String appID) throws Exception {
		try {
			return stub.startAuthentication(username, appID);
		} catch (RemoteException e) {
			String msg = "Cannot trigger startAuthentication"
			             + " . Backend service may be unavailable";
			throw new Exception(msg, e);
		}
	}*/

	/*public void finishAuthentication(String response, String username, String appID) throws Exception{
		try{
			 stub.finishAuthentication(response, username, appID);
		}catch (RemoteException e) {
			String msg = "Cannot trigger finishAuthentication"
			             + " . Backend service may be unavailable";
			throw new Exception(msg, e);
		}
	}*/

	public String startRegistration(String username, String appID) throws Exception {
		try {
			return stub.startRegistration(username, appID);
		} catch (RemoteException e) {
			String msg = "Cannot trigger startRegistration"
			             + " . Backend service may be unavailable";
			throw new Exception(msg, e);
		}
	}

	public String finishRegistration(String response, String username, String appID) throws Exception {
		try {
			return stub.finishRegistration(response, username, appID);
		} catch (RemoteException e) {
			e.printStackTrace();
			String msg = "Cannot trigger finishRegistration"
			             + " . Backend service may be unavailable";
			throw new Exception(msg, e);
		}
	}

}
