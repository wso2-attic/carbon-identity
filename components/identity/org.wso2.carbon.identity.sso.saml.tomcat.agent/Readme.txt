WSO2 Identity Server Tomcat Agent For SAML2 SSO
===============================================

This component enables the web apps to use the single sign on facility 
for authenticating it's users with the help of an identity provider.

The filter in the component handle all the requests for the application
The unauthenticated requests are redirected to the Identity providers with
SAML authentication request embedded into the request.
After the authentication the attributes of the authenticated user is added as 
the session attributes so that they can be used to within the web application

And once the user sign out from the web application this session is invalidated
The logout can also be triggered by the Identity provider in case the user has
logout from any other SSO enabled application. In that case, 
requests made for the app after the user logout from other (SSO enabled) app
will be rejected and redirected to the login page.

How to configure
----------------
1. Please go through the project. If your existing web application,
    has authentication way,  you can integrate it with SAML SSO by implementing "Authenticator" interface.
    Default implementation can be found as "SimpleAuthenticator".  For testing, you can go with
    default implementation.

2. Build the project using maven 3,  Jar file can be found at under target directory
3. Stop the application already running
4. Copy the built jar file in to web app's lib folder
5. There are some dependency jar files. Please copy them also (still not finalized)
3. Edit the web.xml and add the following configurations to it. Refer the sample app for details

	<filter>
		<filter-name>AuthFilter</filter-name>
		<filter-class>org.wso2.carbon.identity.sso.saml.tomcat.agent.SSOAgentFilter</filter-class>
		<init-param>
			<!-- A unique identifier for this SAML 2.0 Service Provider application -->
			<param-name>Issuer</param-name>
			<param-value>SSOSampleApp</param-value>
		</init-param>
		<init-param>
			<!-- The URL of the SAML 2.0 Identity Provider -->
			<param-name>IdpUrl</param-name>
			<param-value>https://localhost:9443/samlsso</param-value>
		</init-param>
		<init-param>
			<!-- The URL of the SAML 2.0 Assertion Consumer (i.e. SSO Login url of the app in this case) -->
			<param-name>ConsumerUrl</param-name>
			<param-value>http://localhost:8080/IdentityMgtApp/ssologin</param-value>
		</init-param>
		<init-param>
			<!-- Identifier given for the Service Provider for SAML 2.0 attribute exchange -->
			<param-name>AttributeConsumingServiceIndex</param-name>
			<param-value>1239245949</param-value>
		</init-param>

		<init-param>
		    <!--
		    This is the url of the login page, the authentication requests will be forwarded
		    to <webapp_home_url>/<ssoLoginPage>. (e.g. localhost:8080/ssologin)
		     -->
			<param-name>SSOLoginPage</param-name>
			<param-value>ssologin</param-value>
		</init-param>
		<init-param>
		    <!-- This is the page to redirect the user after the authentication (i.e. user home page) -->
			<param-name>HomePage</param-name>
			<param-value>home.jsp</param-value>
		</init-param>
		<init-param>
		    <!-- This is the page to redirect user after the logout -->
			<param-name>LogoutPage</param-name>
			<param-value>logout.jsp</param-value>
		</init-param>

		<init-param>
		    <!-- This is the attribute which defining the attribute Id for the subject name identifier value of the SAML assertion -->
			<param-name>SubjectNameAttributeId</param-name>
			<param-value>userName</param-value>
		</init-param>
		<init-param>
		    <!-- This is the attribute which defining the trust store of the web application. This can be used to over write the default trust store-->
			<param-name>TrustStore</param-name>
			<param-value>/home/asela/security/client-truststore.jks</param-value>
		</init-param>
		<init-param>
		    <!-- This is the attribute which defining the trust store password of the web application-->
			<param-name>TrustStorePassword</param-name>
			<param-value>wso2carbon</param-value>
		</init-param>
		<init-param>
		    <!-- This is the attribute which defining the IDP public certificate alias name in defined trust store file -->
			<param-name>IDPCertAlias</param-name>
			<param-value>localhost</param-value>
		</init-param>
		<init-param>
		    <!-- This is the page to redirect user if there are any error in agent -->
			<param-name>ErrorPage</param-name>
			<param-value>sso-errors.jsp</param-value>
		</init-param>
		<init-param>
		    <!-- Identity server version. Whether it is 4 (4.X.X) or 3 (3.X.X) -->
		    <param-name>ServerVersion</param-name>
		    <param-value>3.2.3</param-value>
		</init-param>
	</filter>
	<filter-mapping>
		<filter-name>AuthFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

5. Add the details of the web application to the identity provider

In WSO2 Identity server, please configure following

a) Configure Issuer 
b) Configure ACS
c) Enable Response Signing.
d) Select Any attributes that are wanted to receive

6. Restart the web application
