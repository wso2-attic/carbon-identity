package org.wso2.carbon.identity.thrift.authentication.client;

import org.apache.log4j.Logger;
import org.wso2.carbon.identity.thrift.authentication.client.exception.ThriftAuthenticationException;
import org.wso2.carbon.identity.thrift.authentication.client.internal.util.ThriftAuthenticationClientConstants;

public class AuthTest {

    static Logger log = Logger.getLogger(AuthTest.class);
    static String host = "10.100.1.144";

    public static void main(String[] args) throws InterruptedException, ThriftAuthenticationException {

        KeyStoreUtil.setTrustStoreParams();
        Thread.sleep(2000);

        sslLogin();
        httpsLogin();
        wrongSslLogin();
        wrongHttpsLogin();

        Thread.sleep(3000);
    }

    public static void sslLogin() throws ThriftAuthenticationException {
        ThriftAuthenticationClient authenticationClient = ThriftAuthenticationClientFactory.constructAgentAuthenticator();
        String sessionId = authenticationClient.authenticate("admin", "admin", ThriftAuthenticationClient.Protocol.HTTPS, host, 9443);
        if (sessionId == null) {
            log.error("sessionId==null for sslLogin");
        }
        log.info("sslLogin session ID: " + sessionId);
    }

    public static void httpsLogin() throws ThriftAuthenticationException {
        ThriftAuthenticationClient authenticationClient = ThriftAuthenticationClientFactory.constructAgentAuthenticator();
        String sessionId = authenticationClient.authenticate("admin", "admin", ThriftAuthenticationClient.Protocol.SSL, host, 10711);

        if (sessionId == null) {
            log.error("sessionId==null for httpsLogin");
        }
        log.info("httpsLogin session ID: " + sessionId);
    }

    public static void wrongSslLogin() {
        ThriftAuthenticationClient authenticationClient = ThriftAuthenticationClientFactory.constructAgentAuthenticator();
        String sessionId = null;
        try {
            sessionId = authenticationClient.authenticate("admin", "bar", ThriftAuthenticationClient.Protocol.SSL, host, 10711);
            log.error("sessionId!=null for wrongSslLogin");

        } catch (ThriftAuthenticationException e) {
            log.info("wrongSslLogin no session ID");
        }


    }

    public static void wrongHttpsLogin() {
        ThriftAuthenticationClient authenticationClient = ThriftAuthenticationClientFactory.constructAgentAuthenticator();
        String sessionId = null;
        try {
            sessionId = authenticationClient.authenticate("foo", "bar", ThriftAuthenticationClient.Protocol.HTTPS, host, 9443);
            log.error("sessionId!=null for wrongHttpsLogin");

        } catch (ThriftAuthenticationException e) {
            log.info("wrongHttpsLogin no session ID");
        }

    }

}