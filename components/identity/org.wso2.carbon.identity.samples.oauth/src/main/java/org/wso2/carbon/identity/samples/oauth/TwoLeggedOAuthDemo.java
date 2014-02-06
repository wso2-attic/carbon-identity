/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.samples.oauth;

import java.io.*;
import java.net.URL;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthParameters.OAuthType;

/**
 * This is a demo client which demonstrate the use of 2-legged-oauth. In this example we will talk to a service in
 * ESB with OAuth. We will be using Identity Server to manage users. In this example,
 * first a given user will be authenticated with the Identity Server
 * Then a secret is also shared with the Identity Server.
 *
 * Afterwards user will try to call a web service in ESB. The request first goes to ESB OAuth Mediator.
 * The request is signed by user, using the consumer secret. Then signed part is added to message
 * as an oauth header. The mediator will intercept the incoming message and will send it to IS. IS will validate the
 * signature of the request and will return to the OAuth mediator.
 * If validation is successful service invocation will continue, else service invocation is not allowed. 
 */
public class TwoLeggedOAuthDemo {

    // Identity server, service URL (Always use IP address)
    private static final String IDENTITY_SERVER = "https://127.0.0.1:9444/";

     // Identity server, Host Name (Always use IP address)
    private static final String IDENTITY_SERVER_HOST_NAME = "127.0.0.1";

    // ESB service URL (Always use IP address)
    private static final String ESB_SERVER = "http://127.0.0.1:8280/";

    /**
     * @param args
     */
    public static void main(String[] args) {

        //User invoking the service
        //This user has to be registered in the system.
        String USER_NAME = "admin";

        //User password
        String PASSWORD = "admin";

        // Consumer key given for this client
        String CONSUMER_KEY = null;
        
        //Consumer secret given for this client
        String CONSUMER_SECRET = null;

        //The client that is going to talk to IS (The actual service invoked is
        // https://localhost:9443/services/AuthenticationAdmin?wsdl)
        AuthenticationServiceClient client = null;

        //Axis2 client needs a configuration context
        ConfigurationContext configContext = null;

        String path = null;

        GDataRequest request = null;
        path = System.getProperty("user.dir");      

        try {

            //Create a configuration context. A configuration context contains information for a
            //axis2 environment. This is needed to create an axis2 client
            configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
                    null, null);

            /**
             * Call to https://localhost:9443/services/AuthenticationAdmin?wsdl uses HTTPS protocol.
             * Therefore we to validate the server certificate. The server certificate is looked up in the
             * trust store. Following code sets what trust-store to look for and its JKs password.
             * Note : The trust store should have server's certificate.
             */
            System.setProperty("javax.net.ssl.trustStore",   path + File.separator +"wso2carbon.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

            /**
             * Here we are authenticating the given user name and password with IS.
             * https://localhost:9443/services/AuthenticationAdmin?wsdl
             */
            client = new AuthenticationServiceClient(IDENTITY_SERVER + "services/", configContext);

            /**
             * Actual authentication call. If authentication is successful, 
             * User can register a consumer secret with IS.
             */
			if (client.authenticate(USER_NAME, PASSWORD, IDENTITY_SERVER_HOST_NAME)) {
				System.out.println("\n User-" + USER_NAME + " successfully authenticated.");
				
			    String[] registeredData = client.registerOAuthConsumer();
			    // if the registration was successful, consumer key and secrete will be returned
			    if(registeredData != null && registeredData.length == 2){
			    	CONSUMER_KEY = registeredData[0];
			    	CONSUMER_SECRET = registeredData[1];
			    	System.out.println("\n Consumer registerd successfully ! \n Key: " + CONSUMER_KEY + " \n Secret: "+CONSUMER_SECRET );
			    }
			} else {
			    System.out.println("Invalid credentials");
			    return;
			}
            // We are using Google oauth API to call the service
            GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();

            // Setting user name as consumer key
            oauthParameters.setOAuthConsumerKey(CONSUMER_KEY);
            // Setting above assigned consumer secret
            oauthParameters.setOAuthConsumerSecret(CONSUMER_SECRET);
            
            // setting 2-legged OAuth flag
            oauthParameters.setOAuthType(OAuthType.TWO_LEGGED_OAUTH);

            // We will be using HMAC-SHA1 signature. Google API has a class to do that
            OAuthHmacSha1Signer signer = new OAuthHmacSha1Signer();

            // Create a Google service. The name of the current application given here
            // Names are only for reference purpose
            GoogleService service = new GoogleService("oauthclient", "sampleapp");
            service.setOAuthCredentials(oauthParameters, signer);

            /**
             * We will be calling test service's echoString method. As parameter we are sending "Hello World"
             * The parameter name is "in".
             */
            String param = "WSO2";
            String baseString = ESB_SERVER + "services/OAuthTest/greet?name="+ param;

            /**
             * Invoking the request. And writing the response output.
             */
            URL feedUrl = new URL(baseString);
            request = service.createFeedRequest(feedUrl);
            request.execute();
            
            System.out.println(convertStreamToString(request.getResponseStream()));
            
        }  catch (AxisFault e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}
