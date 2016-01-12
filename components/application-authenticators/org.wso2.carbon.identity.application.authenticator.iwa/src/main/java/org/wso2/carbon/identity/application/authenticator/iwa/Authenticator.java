/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.application.authenticator.iwa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.wso2.carbon.user.core.service.RealmService;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.PrivilegedActionException;
import javax.security.auth.kerberos.KerberosPrincipal;

/** this class process the kerberos token and create server credentials*/
public class Authenticator {

	private transient LoginContext loginContext;
	private transient GSSCredential serverCred;
	private CallbackHandler callbackHandler;
	private final transient KerberosPrincipal serverPrincipal;
	private static Log log = LogFactory.getLog(Authenticator.class);

	public Authenticator() throws LoginException, PrivilegedActionException, GSSException {

		//todo if this not given fall back to default user name
		RealmService realmService = IWAServiceDataHolder.getRealmService();
		String username = realmService.getBootstrapRealmConfiguration().getUserStoreProperty("SPNName");
		String password = realmService.getBootstrapRealmConfiguration().getUserStoreProperty("SPNPassword");

		callbackHandler = IWAServiceDataHolder.getUsernamePasswordHandler(username, password);

		serviceLogin(callbackHandler);

		this.serverPrincipal = new KerberosPrincipal(this.serverCred.getName().toString());

	}

	/**
	 * Process kerberos token and get user name
	 *
	 * @param gssToken kerberos token
	 * @return username
	 * @throws GSSException
	 * */
	public String processToken(byte [] gssToken) throws GSSException {
		
		GSSContext context;

        context= IWAServiceDataHolder.MANAGER.createContext(this.serverCred);
        byte[] token = context.acceptSecContext(gssToken, 0, gssToken.length);

		String name=null;

        if(!context.isEstablished()) {
            return name;
        }

		name = context.getSrcName().toString();
		return name;
	}

    public static void setSystemProperties(String prop2) {

		System.setProperty("java.security.auth.krb5.conf",prop2);

    }

	/**
	 * Get server credential using SPN
	 *
	 * @param  callbackHandler
	 * @throws PrivilegedActionException
	 * @throws LoginException
	 * */
	private void serviceLogin(CallbackHandler callbackHandler) throws PrivilegedActionException, LoginException {

		this.loginContext = new LoginContext("Server", callbackHandler);
		this.loginContext.login();
		this.serverCred = IWAServiceDataHolder.getServerCredential(this.loginContext.getSubject());

	}

	/**
	 * Handle local host authentication request
	 * */
	public String doLocalhost() {
		final String username = System.getProperty("user.name");

		if (null == username || username.isEmpty()) {
			return this.serverPrincipal.getName() + '@' + this.serverPrincipal.getRealm();

		} else {
			return username + '@' + this.serverPrincipal.getRealm();
		}
	}
}
