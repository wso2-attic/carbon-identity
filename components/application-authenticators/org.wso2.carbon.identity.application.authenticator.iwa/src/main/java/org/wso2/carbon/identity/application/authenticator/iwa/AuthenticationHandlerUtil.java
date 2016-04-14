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

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.wso2.carbon.user.core.service.RealmService;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.PrivilegedActionException;

/**
 * this class process the kerberos token and create server credentials
 */
public class AuthenticationHandlerUtil {

    private static transient GSSCredential serverCred;
    private static transient KerberosPrincipal serverPrincipal;

    public static void initialize() throws GSSException, PrivilegedActionException, LoginException {
        //todo if this not given fall back to default user name
        RealmService realmService = IWAServiceDataHolder.getRealmService();
        String username = realmService.getBootstrapRealmConfiguration().getUserStoreProperty(IWAConstants.SPN_NAME);
        String password = realmService.getBootstrapRealmConfiguration().getUserStoreProperty(IWAConstants.SPN_PASSWORD);
        CallbackHandler callbackHandler = IWAServiceDataHolder.getUsernamePasswordHandler(username, password);
        createSeverCredentials(callbackHandler);
        serverPrincipal = new KerberosPrincipal(serverCred.getName().toString());
    }

    /**
     * Process kerberos token and get user name
     *
     * @param gssToken kerberos token
     * @return username
     * @throws GSSException
     */
    public static String processToken(byte[] gssToken) throws GSSException {
        GSSContext context;
        context = IWAServiceDataHolder.MANAGER.createContext(serverCred);
        context.acceptSecContext(gssToken, 0, gssToken.length);
        String name = null;

        if (!context.isEstablished()) {
            return name;
        }
        name = context.getSrcName().toString();
        return name;
    }

    public static void setSystemProperties(String prop2) {
        //todo need to be removed
        System.setProperty("java.security.auth.krb5.conf", prop2);
    }

    /**
     * Create server credential using SPN
     *
     * @param callbackHandler username password callback handler
     * @throws PrivilegedActionException
     * @throws LoginException
     */
    private static void createSeverCredentials(CallbackHandler callbackHandler)
            throws PrivilegedActionException, LoginException {
        LoginContext loginContext = new LoginContext(IWAConstants.SERVER, callbackHandler);
        loginContext.login();
        serverCred = IWAServiceDataHolder.getServerCredential(loginContext.getSubject());
    }

    /**
     * Handle local host authentication request
     */
    //todo check logic of using system user insted of access user
    public static String doLocalhost() {
        final String username = System.getProperty(IWAConstants.USER_NAME);

        if (null == username || username.isEmpty()) {
            return serverPrincipal.getName() + '@' + serverPrincipal.getRealm();
        } else {
            return username + '@' + serverPrincipal.getRealm();
        }
    }
}
