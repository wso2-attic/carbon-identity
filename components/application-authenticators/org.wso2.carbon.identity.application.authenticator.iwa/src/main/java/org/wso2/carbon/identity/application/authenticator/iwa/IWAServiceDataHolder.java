/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

//todo handle exceptions and logs
public class IWAServiceDataHolder {

    static final Oid SPNEGO_OID = IWAServiceDataHolder.getOid();
    static final GSSManager MANAGER = GSSManager.getInstance();

    public static CallbackHandler getUsernamePasswordHandler(final String username,
                                                             final String password) {

        final CallbackHandler handler = new CallbackHandler() {
            public void handle(final Callback[] callback) {
                for (int i=0; i<callback.length; i++) {
                    if (callback[i] instanceof NameCallback) {
                        final NameCallback nameCallback = (NameCallback) callback[i];
                        nameCallback.setName(username);
                    } else if (callback[i] instanceof PasswordCallback) {
                        final PasswordCallback passCallback = (PasswordCallback) callback[i];
                        passCallback.setPassword(password.toCharArray());
                    } else {
                        System.out.println("Unsupported Callback i=" + i + "; class="
                                + callback[i].getClass().getName());
                    }
                }
            }
        };

        return handler;

    }

    static GSSCredential getServerCredential(final Subject subject)
            throws PrivilegedActionException {

        final PrivilegedExceptionAction<GSSCredential> action =
                new PrivilegedExceptionAction<GSSCredential>() {
                    public GSSCredential run() throws GSSException {
                        return MANAGER.createCredential(
                                null
                                , GSSCredential.INDEFINITE_LIFETIME
                                , IWAServiceDataHolder.SPNEGO_OID
                                , GSSCredential.ACCEPT_ONLY);
                    }
                };
        return Subject.doAs(subject, action);
    }

    private static Oid getOid() {
        Oid oid = null;
        try {
            oid = new Oid("1.3.6.1.5.5.2");
        } catch (GSSException gsse) {
            System.out.println("Unable to create OID 1.3.6.1.5.5.2 !"+ gsse.toString());
        }
        return oid;
    }

}
