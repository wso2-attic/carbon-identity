package org.wso2.carbon.identity.application.authenticator.iwa;

import java.security.PrivilegedActionException;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

public class Authenticator {

	private final transient LoginContext loginContext;
	private final transient GSSCredential serverCred;
	
	public Authenticator() throws LoginException, PrivilegedActionException, GSSException {

		String username="Administrator";
		String password="Admin@123#";

        CallbackHandler callbackHandler = IWAServiceDataHolder.getUsernamePasswordHandler(username, password);
		
		this.loginContext = new LoginContext("spnego-server", callbackHandler);
		
		this.loginContext.login();

		this.serverCred = IWAServiceDataHolder.getServerCredential(this.loginContext.getSubject());

		System.out.println(serverCred.toString());

	}

	
	public String authenticateUser(byte [] gssToken) throws GSSException {
		
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

    public static void setSystemProperties(String prop1, String prop2) {

        System.setProperty("java.security.auth.login.config",prop1);
        System.setProperty("java.security.auth.krb5.conf",prop2);

    }
	
}
