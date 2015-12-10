package org.wso2.carbon.identity.application.authenticator.iwa;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;

public class Provider {
	
	static final Oid SPNEGO_OID = Provider.getOid();
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
	
	/**
     * Returns the {@link GSSCredential} the server uses for pre-authentication.
     * 
     * @param subject account server uses for pre-authentication
     * @return credential that allows server to authenticate clients
     * @throws PrivilegedActionException
     */
    static GSSCredential getServerCredential(final Subject subject)
        throws PrivilegedActionException {
        
        final PrivilegedExceptionAction<GSSCredential> action = 
            new PrivilegedExceptionAction<GSSCredential>() {
                public GSSCredential run() throws GSSException {
                    return MANAGER.createCredential(
                        null
                        , GSSCredential.INDEFINITE_LIFETIME
                        , Provider.SPNEGO_OID
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
