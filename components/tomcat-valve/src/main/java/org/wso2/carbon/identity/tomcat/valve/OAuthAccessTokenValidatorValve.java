package org.wso2.carbon.identity.tomcat.valve;


import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;
import java.io.IOException;

public class OAuthAccessTokenValidatorValve extends ValveBase{
    private static final Log log = LogFactory.getLog(OAuthAccessTokenValidatorValve.class);

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        System.out.println("Lovely Lovely valve");
        log.info("I hit the tomcat valve for OAuth protection");
    }
}
