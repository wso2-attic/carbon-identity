package org.wso2.carbon.identity.application.authenticator.iwa;

import java.security.PrivilegedActionException;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

public class Authenticator {

	private final transient LoginContext logginContext;
	private final transient GSSCredential serverCred;
	
	public Authenticator(String username, String password ) 
			throws LoginException, PrivilegedActionException, GSSException {

		CallbackHandler callbackHandler = Provider.getUsernamePasswordHandler(username, password);
		
		this.logginContext = new LoginContext("spnego-server", callbackHandler);
		
		this.logginContext.login();

		this.serverCred = Provider.getServerCredential(this.logginContext.getSubject());
		//new KerberosPrincipal(this.serverCred.getName().toString());
		//System.out.println(this.serverCred.getName().toString());
		
	}

	
	public String authnticateSp(byte [] gss) throws GSSException {
		
		GSSContext context = null;

			context= Provider.MANAGER.createContext(this.serverCred);
			byte[] token = context.acceptSecContext(gss, 0, gss.length);

		String name=null;
		if(!context.isEstablished()) {
			return name;
		}
		

		name = context.getSrcName().toString();
		return name;
	}
	
	
}
